# Payment Service Design

This document describes the **payment flow** with `inline code` and a [link](https://example.com).

## Architecture

```mermaid
flowchart LR
    Client[iPhone Shortcut] -->|POST markdown| API(Spring Boot)
    API --> Renderer{Mermaid?}
    Renderer -->|yes| Chromium[(Headless Chromium)]
    Renderer -->|no| HTML[HTML output]
```

## Sequence

```mermaid
sequenceDiagram
    participant U as User
    participant S as Service
    U->>S: Send document
    S-->>U: Rendered HTML
```

## Broken diagram

```mermaid
flowchart LR
    A -->-->--> ((( B
```

## Details

| Component | Purpose |
|-----------|---------|
| API       | Receives text |
| Renderer  | Draws diagrams |

- [x] Parse markdown
- [ ] Render PDF

```java
System.out.println("regular code stays as code");
```

> Note: everything stays in memory.
