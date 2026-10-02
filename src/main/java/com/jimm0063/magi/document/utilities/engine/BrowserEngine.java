package com.jimm0063.magi.document.utilities.engine;

import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

import com.jimm0063.magi.document.utilities.config.MagiProperties;

/**
 * Owns one headless Chromium for the whole application.
 *
 * <p>Playwright objects must only be used from the thread that created them, so every task runs on a
 * single dedicated thread. Each task gets a fresh, isolated {@link BrowserContext} that is closed
 * afterwards. Requests are therefore processed one at a time, which is fine for a personal service.
 */
@Component
public class BrowserEngine implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(BrowserEngine.class);

    private final MagiProperties.Browser settings;
    private final ExecutorService thread = Executors.newSingleThreadExecutor(runnable -> {
        Thread t = new Thread(runnable, "browser-engine");
        t.setDaemon(true);
        return t;
    });

    private Playwright playwright;
    private Browser browser;

    public BrowserEngine(MagiProperties properties) {
        this.settings = properties.browser();
    }

    /**
     * Runs {@code task} with a new browser context (viewport 1600x1000, configured device scale factor).
     */
    public <T> T execute(Function<BrowserContext, T> task) {
        Future<T> future = thread.submit(() -> {
            ensureStarted();
            Browser.NewContextOptions options = new Browser.NewContextOptions()
                    .setViewportSize(1600, 1000)
                    .setDeviceScaleFactor(settings.deviceScaleFactor());
            try (BrowserContext context = browser.newContext(options)) {
                context.setDefaultTimeout(settings.timeout().toMillis());
                return task.apply(context);
            }
        });
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BrowserEngineException("Interrupted while waiting for the browser", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new BrowserEngineException("Browser task failed: " + cause.getMessage(), cause);
        }
    }

    public double deviceScaleFactor() {
        return settings.deviceScaleFactor();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        if (settings.warmup()) {
            thread.submit(() -> {
                try {
                    ensureStarted();
                } catch (RuntimeException e) {
                    log.warn("Browser warm-up failed; it will be retried on first use", e);
                }
            });
        }
    }

    private void ensureStarted() {
        if (browser != null && browser.isConnected()) {
            return;
        }
        closeQuietly();
        log.info("Starting headless Chromium");
        playwright = Playwright.create();
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(true);
        if (settings.executablePath() != null && !settings.executablePath().isBlank()) {
            options.setExecutablePath(Paths.get(settings.executablePath()));
        }
        browser = playwright.chromium().launch(options);
        log.info("Chromium {} ready", browser.version());
    }

    private void closeQuietly() {
        try {
            if (playwright != null) {
                playwright.close();
            }
        } catch (RuntimeException e) {
            log.debug("Error closing Playwright", e);
        }
        playwright = null;
        browser = null;
    }

    @Override
    public void destroy() throws Exception {
        thread.submit(this::closeQuietly).get();
        thread.shutdown();
    }
}
