// Content-hash chunk filenames to prevent CDN (Cloudflare) stale cache
// after redeployment. Without this, `551.js` from build A may be served
// by CDN while `composeApp.js` and `.wasm` are from build B, causing
// Wasm instantiation failure (infinite retry loop).
config.output = config.output || {};
config.output.chunkFilename = '[id].[contenthash:8].js';
