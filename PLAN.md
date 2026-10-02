# Magi Document Utilities — Plan

A Spring Boot service on the local network that processes text for **Apple Shortcuts** (REST API) and the **browser** (Thymeleaf UI). It is organized as pluggable **modules**. The first module is **markdown**.

## 1. Decisions

| Topic | Decision |
|---|---|
| Build | Maven, Java 21, latest stable Spring Boot |
| Base package | `com.jimm0063.magi.document.utilities` |
| Front end | Thymeleaf (server-rendered, same app) |
| Hosting | Debian machine on the local network, listening on `0.0.0.0:8080` |
| Persistence | **None.** Everything is generated in memory; nothing is written to disk |
| Markdown parser | flexmark-java (tables, task lists, TOC, strikethrough extensions) |
| Mermaid + PDF engine | Playwright for Java (headless Chromium) running a **bundled** `mermaid.min.js` (works offline, no CDN) |
| Diagram images | PNG, embedded as base64 `data:` URIs |
| Security | Optional API key header `X-API-Key` (off by default on the LAN) |

## 2. API response contract (Shortcuts-friendly)

Rules:
1. **Flat.** One level, no nested objects.
2. Values are strings, numbers or booleans. Lists are **plain lists of strings** only.
3. The same base fields on every response, success or error.
4. Errors are returned as HTTP 200 with `success: false`, so one `If` check in Shortcuts covers every case.
5. `?raw=true` returns only `result` as plain text.

Base fields (always present):

| Field | Type | Meaning |
|---|---|---|
| `success` | boolean | did it work |
| `module` | string | e.g. `markdown` |
| `action` | string | e.g. `render` |
| `result` | string | main output |
| `message` | string | `OK` or a human-readable error |

Optional fields stay top-level: `items` (list of strings), `count`, `url`, `pdfUrl`, `diagramCount`, `failedDiagrams`, ...

Requests accept either a `text/plain` body (the raw text) or flat JSON `{ "text": "...", "<option>": "..." }`.

## 3. Markdown module: `render`

Use case: AI-generated technical documents with embedded ```` ```mermaid ```` blocks. Output is a document with the diagrams rendered as images, to paste into Apple Notes or download as a PDF.

Pipeline:
1. Parse the Markdown with flexmark.
2. Collect every fenced code block whose info string is `mermaid`.
3. Render each one in headless Chromium with mermaid.js, then screenshot the SVG to a PNG (device scale factor 2 for sharpness).
4. Replace each block with `<img src="data:image/png;base64,...">`.
5. If a diagram fails, keep the code block and add a visible warning. Count it in `failedDiagrams`; the rest of the document still renders.
6. Build two HTML variants:
   - **Notes HTML**: simple tags and **inline styles only** (no `<style>`/classes), because Shortcuts' *Make Rich Text from HTML* ignores most CSS.
   - **Viewer HTML**: a fully styled page for the browser and for PDF printing.
7. Store the result in an **in-memory cache** (Caffeine, TTL 30 min, max ~50 documents). It is needed so `url`/`pdfUrl` work. Nothing goes to disk, and everything is lost on restart.

### Endpoints

| Method | Path | Returns |
|---|---|---|
| GET | `/api/v1/modules` | list of modules/actions (`items`) |
| POST | `/api/v1/markdown/render` | JSON (below). `?raw=true` gives the Notes HTML only. `?format=pdf` gives the PDF file directly |
| GET | `/view/{id}` | viewer page in the browser (with a "Copy for Notes" button) |
| GET | `/view/{id}/pdf` | PDF download |
| GET | `/view/{id}/notes` | Notes HTML of a rendered document |
| POST | `/api/v1/markdown/diagrams` | each diagram as a base64 PNG in `items` |
| GET | `/api/v1/diagnostics/notes-test` | tiny HTML with one base64 PNG, to check the Shortcuts → Notes flow |

Example response:
```json
{
  "success": true,
  "module": "markdown",
  "action": "render",
  "result": "<html>...Notes-friendly HTML with base64 images...</html>",
  "url": "http://<host>:8080/view/a1b2c3",
  "pdfUrl": "http://<host>:8080/view/a1b2c3/pdf",
  "diagramCount": 3,
  "failedDiagrams": 0,
  "message": "OK"
}
```

### Shortcut recipes (to document and test)
- **Straight to Notes:** Get file → Get Contents of URL (POST, `?raw=true`) → Make Rich Text from HTML → Create Note. *Whether Notes keeps the base64 images is unverified; check it with `notes-test` first.*
- **Via Safari:** POST → Get Dictionary Value `url` → Open URL → tap "Copy for Notes" → paste.
- **PDF:** POST with `?format=pdf` → Save File, or add it to a note.

## 4. Web UI (Thymeleaf)

- `/`: home page listing the modules
- `/markdown`: paste text or upload `.md` → **Render** → preview with diagrams, plus **Copy for Apple Notes** (Clipboard API, `text/html`) and **Download PDF**

## 5. Package layout

```
com.jimm0063.magi.document.utilities
├── core        TextModule, ModuleRegistry, ModuleResponse, ModuleRequest, error handling
├── api         REST controllers
├── web         Thymeleaf controllers
├── engine      BrowserEngine (Playwright lifecycle, single browser, serialized page use)
├── store       DocumentStore (in-memory Caffeine cache)
└── modules
    └── markdown  MarkdownModule, MermaidRenderer, HtmlDocumentBuilder, PdfRenderer
```

Adding a module means adding a `@Component` that implements `TextModule`; the registry and `/api/v1/modules` pick it up automatically.

## 6. Phases

Status: phases 0–7 are implemented in the first version, with unit and end-to-end tests (see README). The Notes image route (phase 6) still needs to be checked on a real iPhone.

0. **Skeleton:** Maven project, core contract (`ModuleResponse`, `TextModule`, registry), error handler, `/api/v1/modules`, Thymeleaf layout
1. **Markdown → HTML** without diagrams (flexmark, both HTML variants)
2. **Mermaid rendering** with Playwright + bundled mermaid.js, base64 PNG embedding, failure handling
3. **PDF** via Chromium `page.pdf()` (A4, print backgrounds)
4. **In-memory store** + `/view/{id}` and `/view/{id}/pdf`
5. **Web UI** `/markdown` page with copy/download
6. **Notes test endpoint** + Shortcut recipes in the README
7. **Debian deployment:** Dockerfile on the official Playwright Java base image (includes Chromium and system deps); alternative: jar + systemd + `playwright install-deps`

## 7. Testing

- Unit tests: Mermaid block extraction, HTML builders, response contract
- Integration: `@SpringBootTest` render of a sample `.md` with valid and broken diagrams (needs Chromium)
- Web: `@WebMvcTest` for controllers
