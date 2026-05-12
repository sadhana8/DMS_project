/*! DocVault Local CDN — healthcheck asset
    GET /cdn/js/healthcheck.js should return this file verbatim.
    Any HTML page can <script src="/cdn/js/healthcheck.js"></script> to confirm
    the local CDN pipeline is serving files correctly. */
(function () {
    if (typeof window !== 'undefined') {
        window.__DOCVAULT_CDN__ = { version: '1.0', ok: true };
        if (console && console.info) {
            console.info('[DocVault CDN] healthcheck loaded — local CDN is serving assets.');
        }
    }
})();
