package com.dms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/**
 * Returns metadata about the local CDN.
 * Files are served statically at /cdn/** by WebMvcConfig.
 */
@RestController
@RequestMapping("/cdn")
public class CdnController {

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
            "basePath", "/cdn",
            "description", "Local CDN — drop files in src/main/resources/static/cdn/ to serve them offline",
            "folders", Map.of(
                "css",   "Stylesheets (inter.css, jetbrains-mono.css)",
                "js",    "JavaScript libraries (axios.min.js, recharts.min.js, etc.)",
                "fonts", "Web fonts (.woff2, .woff)",
                "images","Images and icons"
            ),
            "usage", "Reference as /cdn/css/inter.css or /cdn/js/axios.min.js from any HTML/JSX"
        ));
    }
}
