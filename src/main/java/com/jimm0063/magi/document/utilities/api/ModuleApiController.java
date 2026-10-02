package com.jimm0063.magi.document.utilities.api;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.jimm0063.magi.document.utilities.core.ModuleException;
import com.jimm0063.magi.document.utilities.core.ModuleRegistry;
import com.jimm0063.magi.document.utilities.core.ModuleResponse;
import com.jimm0063.magi.document.utilities.core.TextModule;

/**
 * Generic entry point: {@code POST /api/v1/{module}/{action}} (GET also works, handy for testing).
 *
 * <p>Errors are returned as HTTP 200 with {@code success: false}, so a Shortcut needs a single
 * {@code If success} check. Add {@code ?raw=true} to receive only {@code result} as plain text.
 */
@RestController
@RequestMapping("/api/v1")
public class ModuleApiController {

    private static final Logger log = LoggerFactory.getLogger(ModuleApiController.class);

    private final ModuleRegistry registry;
    private final RequestReader requestReader;

    public ModuleApiController(ModuleRegistry registry, RequestReader requestReader) {
        this.registry = registry;
        this.requestReader = requestReader;
    }

    @GetMapping("/modules")
    public ModuleResponse modules() {
        List<String> items = registry.all().stream()
                .flatMap(module -> module.actions().stream().map(action -> module.id() + "/" + action))
                .toList();
        String result = String.join("\n", registry.all().stream()
                .map(module -> module.id() + ": " + module.description())
                .toList());
        return ModuleResponse.ok("core", "modules", result).items(items);
    }

    @GetMapping("/{module}")
    public ModuleResponse actions(@PathVariable String module) {
        return registry.find(module)
                .map(found -> ModuleResponse.ok(found.id(), "actions", found.description()).items(found.actions()))
                .orElseGet(() -> unknownModule(module, "actions"));
    }

    @RequestMapping(value = "/{module}/{action}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> execute(@PathVariable String module, @PathVariable String action,
                                     HttpServletRequest request) {
        ModuleResponse response = run(module, action, request);
        if (Boolean.parseBoolean(request.getParameter("raw"))) {
            String body = response.success() ? response.result() : "ERROR: " + response.get("message");
            return ResponseEntity.ok().contentType(new MediaType(MediaType.TEXT_PLAIN, java.nio.charset.StandardCharsets.UTF_8)).body(body);
        }
        return ResponseEntity.ok(response);
    }

    private ModuleResponse run(String moduleId, String action, HttpServletRequest request) {
        TextModule module = registry.find(moduleId).orElse(null);
        if (module == null) {
            return unknownModule(moduleId, action);
        }
        if (!module.actions().contains(action)) {
            return ModuleResponse.error(moduleId, action,
                    "Unknown action '" + action + "'. Available: " + String.join(", ", module.actions()));
        }
        try {
            return module.execute(action, requestReader.read(request));
        } catch (ModuleException e) {
            return ModuleResponse.error(moduleId, action, e.getMessage());
        } catch (IOException | RuntimeException e) {
            log.error("Module {}/{} failed", moduleId, action, e);
            return ModuleResponse.error(moduleId, action, "Unexpected error: " + e.getMessage());
        }
    }

    private ModuleResponse unknownModule(String module, String action) {
        return ModuleResponse.error(module, action, "Unknown module '" + module + "'");
    }
}
