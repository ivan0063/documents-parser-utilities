package com.jimm0063.magi.document.utilities.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Download;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;

/**
 * End-to-end tests of the Thymeleaf UI, driven by a real browser.
 */
class WebUiE2E extends E2ESupport {

    private static Playwright playwright;
    private static Browser browser;

    private BrowserContext context;
    private Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @AfterAll
    static void closeBrowser() {
        playwright.close();
    }

    @BeforeEach
    void openPage() {
        context = browser.newContext(new Browser.NewContextOptions().setAcceptDownloads(true));
        context.grantPermissions(List.of("clipboard-read", "clipboard-write"), new BrowserContext.GrantPermissionsOptions()
                .setOrigin(url("")));
        page = context.newPage();
    }

    @AfterEach
    void closePage() {
        context.close();
    }

    @Test
    void homeListsModulesAndLinksToMarkdown() {
        page.navigate(url("/"));

        assertThat(page.locator("[data-module=markdown]")).isVisible();
        assertThat(page.locator("[data-module=diagnostics]")).isVisible();

        page.locator("[data-module=markdown] h2 a").click();
        page.waitForURL(url("/markdown"));
        assertThat(page.locator("#text")).isVisible();
    }

    @Test
    void pastedMarkdownIsRenderedWithDiagramImages() {
        renderPasted(sample("technical-doc.md"));

        assertThat(page).hasTitle("Payment Service Design");
        assertThat(page.locator("#document h1")).hasText("Payment Service Design");
        assertThat(page.locator("#document img.mermaid-diagram")).hasCount(2);
        assertThat(page.locator("#document .diagram-error")).hasCount(1);
        assertThat(page.locator("#diagram-warning")).hasText("1 of 3 diagram(s) could not be rendered");

        // The images actually decode and have a real size.
        Object widths = page.evaluate("() => [...document.querySelectorAll('img.mermaid-diagram')]"
                + ".map(img => img.complete ? img.naturalWidth : 0)");
        assertThat((List<?>) widths).allSatisfy(w -> assertThat(((Number) w).intValue()).isGreaterThan(100));
    }

    @Test
    void uploadedFileIsRendered(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("upload.md");
        Files.writeString(file, "# Uploaded\n\n```mermaid\nflowchart LR\n  X --> Y\n```\n");

        page.navigate(url("/markdown"));
        page.setInputFiles("#file", file);
        page.click("#render-button");
        page.waitForURL("**/view/**");

        assertThat(page.locator("#document h1")).hasText("Uploaded");
        assertThat(page.locator("#document img.mermaid-diagram")).hasCount(1);
        assertThat(page.locator("#diagram-warning")).hasCount(0);
    }

    @Test
    void downloadPdfButtonDownloadsAPdf() throws IOException {
        renderPasted(sample("technical-doc.md"));

        Download download = page.waitForDownload(() -> page.click("#download-pdf"));

        assertThat(download.suggestedFilename()).isEqualTo("Payment Service Design.pdf");
        assertThat(isPdf(Files.readAllBytes(download.path()))).isTrue();
    }

    @Test
    void copyForNotesPutsHtmlWithImagesOnTheClipboard() {
        renderPasted("# Clip\n\n```mermaid\nflowchart LR\n  A --> B\n```\n");

        page.click("#copy-notes");

        assertThat(page.locator("#toast")).hasText("Copied! Paste it into Apple Notes.");
        String clipboardHtml = (String) page.evaluate("async () => {"
                + " const items = await navigator.clipboard.read();"
                + " return await (await items[0].getType('text/html')).text(); }");
        assertThat(clipboardHtml).contains("<h1>Clip</h1>").contains("data:image/png;base64,");
    }

    @Test
    void emptySubmissionShowsAnError() {
        page.navigate(url("/markdown"));
        page.click("#render-button");

        assertThat(page.locator("#error")).hasText("Text is empty");
    }

    @Test
    void unknownDocumentShowsExpiredPage() {
        Response response = page.navigate(url("/view/expired-id"));

        assertThat(response.status()).isEqualTo(404);
        assertThat(page.locator("#expired-message")).isVisible();
    }

    private void renderPasted(String markdown) {
        page.navigate(url("/markdown"));
        page.fill("#text", markdown);
        page.click("#render-button");
        page.waitForURL("**/view/**");
    }
}
