package com.jimm0063.magi.document.utilities.modules.diagnostics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;

import com.jimm0063.magi.document.utilities.core.ModuleRequest;
import com.jimm0063.magi.document.utilities.core.ModuleResponse;
import com.jimm0063.magi.document.utilities.core.TextModule;

/**
 * Helpers to check the setup from a Shortcut.
 *
 * <ul>
 *   <li>{@code ping}: echoes the text back</li>
 *   <li>{@code notes-test}: a tiny HTML document with one embedded base64 PNG, to check whether
 *   "Make Rich Text from HTML" + "Create Note" keeps images</li>
 * </ul>
 */
@Component
public class DiagnosticsModule implements TextModule {

    private final String notesTestHtml = buildNotesTestHtml();

    @Override
    public String id() {
        return "diagnostics";
    }

    @Override
    public String description() {
        return "Checks for your Shortcuts setup (ping, Apple Notes image test)";
    }

    @Override
    public List<String> actions() {
        return List.of("ping", "notes-test");
    }

    @Override
    public ModuleResponse execute(String action, ModuleRequest request) {
        if ("ping".equals(action)) {
            return ModuleResponse.ok(id(), action, request.text().isEmpty() ? "pong" : request.text());
        }
        return ModuleResponse.ok(id(), action, notesTestHtml);
    }

    private static String buildNotesTestHtml() {
        BufferedImage image = new BufferedImage(240, 80, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 240, 80);
        g.setColor(new Color(0x3b82f6));
        g.fillRoundRect(10, 20, 60, 40, 12, 12);
        g.setColor(new Color(0x10b981));
        g.fillRoundRect(170, 20, 60, 40, 12, 12);
        g.setColor(new Color(0x334155));
        g.fillRect(70, 38, 90, 4);
        g.fillPolygon(new int[] {160, 170, 160}, new int[] {32, 40, 48}, 3);
        g.dispose();

        ByteArrayOutputStream png = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", png);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String src = "data:image/png;base64," + Base64.getEncoder().encodeToString(png.toByteArray());
        return "<!DOCTYPE html>\n<html><head><meta charset=\"utf-8\"><title>Notes image test</title></head>"
                + "<body style=\"font-family:-apple-system,Helvetica,Arial,sans-serif;\">"
                + "<h1>Notes image test</h1>"
                + "<p>If you can see a <b>blue box connected to a green box</b> below, embedded images work.</p>"
                + "<p><img src=\"" + src + "\" width=\"240\" alt=\"Test diagram\"></p>"
                + "<p><code style=\"font-family:Menlo,monospace;\">inline code</code> and a list:</p>"
                + "<ul><li>one</li><li>two</li></ul>"
                + "</body></html>\n";
    }
}
