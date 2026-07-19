# Copilot instructions for MyStockManager

- This is a static web app. The runtime is a plain browser app with `index.html`, CSS in `css/`, and vanilla JS in `js/`.
- No `package.json`, no bundler, no automated test framework were found. Edit the source directly and validate changes in the browser or a local static server.

## Architecture
- Entry point: `index.html` loads the app shell and scripts in a strict order.
- Main controller: `js/app.js` boots the app, initializes `Modal`, `ItemForm`, and `LoginView`, opens `IndexedDB` (`js/core/db.js`), and manages the main view switcher.
- Module pattern: most code uses IIFEs and global singletons such as `App`, `Auth`, `DB`, `ItemModel`, `StorageModel`, `ShoppingModel`, `PrefsModel`, and view objects like `AllItemsView`.
- UI views are in `js/views/` and each view exposes `init()` and `render()`.

## Data & persistence
- Persistent storage is IndexedDB via `js/core/db.js` with object stores: `users`, `storages`, `items`, `shopping_list`, and `preferences`.
- Authentication is local only: `js/core/auth.js` stores session metadata in `sessionStorage` and uses `js/core/crypto.js` for password hashing and key management.
- User preferences are in IndexedDB and theme/language are also cached in `localStorage` (`msm_theme`, `msm_lang`).

## Key patterns
- Views use DOM element IDs and class names directly. Example: `App.navigateTo()` toggles `view-expiring`, `view-all`, `view-shopping`, `view-storages`.
- UI text uses translations from `js/i18n.js` with `i18n.t(key)` and `i18n.setLang(...)`.
- `ItemModel` contains app-specific helpers like `getExpiring()`, `sort()`, `duplicate()`, and `moveToStorage()`.
- `StorageModel.remove()` rejects deletion if the storage still contains items.
- Service worker in `sw.js` is cache-first for app assets and network-only for TheMealDB API.

## External integration
- Recipes use TheMealDB REST API: `https://www.themealdb.com/api/json/v1/1`.
- Ad banner behavior is in `js/components/ad-banner.js` and premium handling is partially maintained in `js/core/auth.js` + `js/models/prefs.model.js`.

## Important project conventions
- Preserve the plain-vanilla JS style. Do not introduce build tooling unless explicitly asked.
- Prefer adding new behavior via existing module patterns and view controllers rather than rewriting the whole app.
- Keep translations in `js/i18n.js`; do not hardcode UI strings when a translation key already exists.
- Many helpers are global singletons. Search for `Auth`, `DB`, `ItemModel`, `StorageModel`, `ShoppingModel`, `PrefsModel`, `Modal`, and `Toast` when tracing logic.
- `index.html` controls the runtime script inclusion order. Only scripts loaded there are part of the active app.

## Running / debugging
- Run this app from a static server. Example: `python -m http.server 8000` from the repo root or use VS Code Live Server.
- Debug in browser DevTools. Inspect IndexedDB under `mystockmanager` to verify schema and data.

> Note: there are some legacy or unused files under `js/` such as `js/recipes.js`, `js/shopping.js`, `js/items.js`, `js/alerts.js`, and `js/storage.js` that are not directly included by `index.html`.
