# Local CDN

Files in this folder are served at `/cdn/**` by the Spring Boot backend.
Use them to replace external CDN URLs and work fully offline.

## Endpoints
- `GET /cdn/css/inter.css`          — Inter font CSS
- `GET /cdn/fonts/*.woff2`          — Font files (add manually, see below)
- `GET /cdn/js/*.min.js`            — JS libraries (add manually)
- `GET /api/cdn/info`               — Manifest of all available assets

## How to add fonts (offline mode)
1. Download from https://github.com/rsms/inter/releases
2. Copy `.woff2` files → `src/main/resources/static/cdn/fonts/`
3. In `index.html` replace Google Fonts link with:
   `<link rel="stylesheet" href="http://localhost:8080/cdn/css/inter.css">`

## How to add JS libraries
1. Download minified JS from https://unpkg.com/<package>/dist/<file>.min.js
2. Save in `src/main/resources/static/cdn/js/`
3. Reference as `/cdn/js/your-lib.min.js`

## Cache
All CDN files cached 7 days: `Cache-Control: public, max-age=604800`
