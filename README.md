# Magi Document Utilities

A small Spring Boot service for your local network. It processes text for **Apple Shortcuts** (REST API) and for the **browser** (Thymeleaf UI). Features are pluggable **modules**.

**First module, `markdown`:** send in an AI-generated technical document with ```` ```mermaid ```` blocks and get back a finished document with the diagrams as real images:

- **HTML for Apple Notes** (images embedded as base64 PNG, inline styles only)
- **a web page** to open in Safari, with a *Copy for Apple Notes* button
- **a PDF**

Nothing is saved to disk. Rendered documents stay in memory for 30 minutes, then they're gone.

## Run

### Docker (recommended on Debian)

```bash
docker compose up -d --build
# open http://<debian-ip>:8080
```

The image is based on the official Playwright Java image, which already includes Chromium, its system libraries and fonts.

### Without Docker

Needs Java 21 and Maven:

```bash
mvn package -DskipTests
# one time: install Chromium + its Debian system dependencies
mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install --with-deps chromium"
java -jar target/document-utilities-0.1.0-SNAPSHOT.jar
```

### Configuration (`application.yml` / environment variables)

| Setting | Env var | Default | Meaning |
|---|---|---|---|
| `magi.api-key` | `MAGI_API_KEY` | empty | If set, `/api/**` requires the header `X-API-Key` |
| `magi.store.ttl` | | `30m` | How long rendered documents stay in memory |
| `magi.browser.executable-path` | `MAGI_BROWSER_EXECUTABLE_PATH` | empty | Use a specific Chromium binary |
| `magi.mermaid.theme` | | `default` | Mermaid theme: `default`, `neutral`, `forest`, `base`, `dark` |

## API

Every response is **flat JSON**, so a Shortcut reads any field with a single *Get Dictionary Value*:

```json
{
  "success": true,
  "module": "markdown",
  "action": "render",
  "result": "<!DOCTYPE html>…",
  "message": "OK",
  "title": "Payment Service Design",
  "url": "http://192.168.1.50:8080/view/oGsJwQTpaY3X",
  "pdfUrl": "http://192.168.1.50:8080/view/oGsJwQTpaY3X/pdf",
  "notesUrl": "http://192.168.1.50:8080/view/oGsJwQTpaY3X/notes",
  "diagramCount": 3,
  "failedDiagrams": 0
}
```

- `success`, `module`, `action`, `result` and `message` are **always** present.
- Errors still return HTTP 200, with `success: false` and the reason in `message`.
- Lists are plain lists of strings in `items`, with their length in `count`.
- `?raw=true` returns only `result` as plain text.

**Request body:** any of these work:
- raw text (`text/plain`, `text/markdown`, a file)
- JSON `{"text": "...", "theme": "forest"}`
- a form with a `text` field or a `file`

| Endpoint | Description |
|---|---|
| `GET /api/v1/modules` | All `module/action` pairs in `items` |
| `POST /api/v1/markdown/render` | Markdown → Notes HTML in `result`, plus `url`, `pdfUrl`, `notesUrl` |
| `POST /api/v1/markdown/render?raw=true` | Only the Notes HTML |
| `POST /api/v1/markdown/render?format=pdf` | The PDF file directly |
| `POST /api/v1/markdown/diagrams` | Each diagram as a base64 PNG in `items` |
| `GET /api/v1/diagnostics/notes-test` | Tiny HTML with one embedded image, to test Notes |
| `GET /api/v1/diagnostics/ping` | Returns `pong` |

If a diagram has a syntax error, it stays as code with a visible warning, and it's counted in `failedDiagrams`. The rest of the document still renders.

## Apple Shortcuts recipes

Replace `http://magi.local:8080` with your Debian machine's address.

### 1. Markdown file → Apple Note (direct)
1. **Receive** files from the Share Sheet (or **Select File**)
2. **Get Contents of URL**: `http://magi.local:8080/api/v1/markdown/render?raw=true`, Method **POST**, Request Body **File** → *Shortcut Input*
3. **Make Rich Text from HTML** ← *Contents of URL*
4. **Create Note** with *Rich Text*

> Run `GET /api/v1/diagnostics/notes-test` through steps 3–4 first. If the note shows the blue-and-green test image, embedded images survive this route on your iOS version. If not, use recipe 2 or 3.

### 2. Via Safari (copy & paste)
1. **Get Contents of URL**: `…/api/v1/markdown/render`, POST, Request Body **File**
2. **Get Dictionary Value** `url`
3. **Open URLs**, then tap **Copy for Apple Notes** and paste into a note

### 3. PDF
1. **Get Contents of URL**: `…/api/v1/markdown/render?format=pdf`, POST, Request Body **File**
2. **Save File** (or **Share**, or add it to a note)

### Error handling in a Shortcut
**Get Dictionary Value** `success` → **If** *is false* → **Show Alert** with **Get Dictionary Value** `message`.

## Web UI

- `/`: list of modules
- `/markdown`: paste or upload Markdown, choose a diagram theme, **Render**
- `/view/{id}`: the rendered document with **Copy for Apple Notes** and **Download PDF**

## Development

```bash
mvn test     # unit tests (fast, no browser)
mvn verify   # unit + end-to-end tests (real HTTP server, real Chromium, browser-driven UI)
```

The end-to-end suites in `src/test/java/.../e2e` are the safety net for future changes:

| Suite | What it covers |
|---|---|
| `ApiE2E` | Every API endpoint and body format, raw/PDF modes, the flat-JSON contract, real Mermaid rendering, error cases |
| `WebUiE2E` | The Thymeleaf UI in a real browser: paste, upload, render, PDF download, clipboard copy, expired page |
| `ApiKeyE2E` | API-key protection |

GitHub Actions (`.github/workflows/ci.yml`) runs `mvn verify` on every push and pull request.

### Adding a module

Create a Spring `@Component` that implements `TextModule` (`id`, `description`, `actions`, `execute`), and return `ModuleResponse.ok(...)`. It is then available automatically at `/api/v1/{id}/{action}` and listed on the home page. `ModuleResponse.with(...)` only accepts flat values, so the Shortcuts contract can't be broken by accident. Optionally return a `webPath()` and add a Thymeleaf page.

### Upgrading Playwright

When changing `<playwright.version>` in `pom.xml`, change the `Dockerfile` base image tag (`mcr.microsoft.com/playwright/java:v<version>-noble`) to match.
