package com.dms.controller;

import com.dms.models.Document;
import com.dms.services.DocumentService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentService documentService;

    @Autowired
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // 📤 Upload document
    @PostMapping("/upload")
    public ResponseEntity<Document> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("uploadedBy") String uploadedBy,
            @RequestParam("category") String category
    ) throws IOException {

        Document savedDoc = documentService.uploadDocument(
                file, title, description, uploadedBy, category);

        return new ResponseEntity<>(savedDoc, HttpStatus.CREATED);
    }

    // 📜 Get all documents
    @GetMapping
    public List<Document> getAllDocuments() {
        return documentService.getAllDocuments();
    }

    // 🔍 Get document by ID
    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocumentById(@PathVariable Long id) {

        Document doc = documentService.getDocument(id);

        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(doc);
    }

    // ⬇️ Download file
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) throws IOException {

        Document doc = documentService.getDocument(id);

        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] data = documentService.downloadFile(id);

        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getFileName() + "\"")
                .body(resource);
    }

    // ✏️ Update metadata
    @PutMapping("/{id}")
    public ResponseEntity<Document> updateDocumentMetadata(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String description) {

        Document updated = documentService.updateMetadata(id, title, description);

        if (updated == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(updated);
    }

    // 🚫 Deprecate document
    @PutMapping("/deprecate/{id}")
    public ResponseEntity<String> deprecateDocument(@PathVariable Long id) {

        boolean success = documentService.deprecateDocument(id);

        if (!success) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok("Document deprecated successfully");
    }

    // 🔍 Filter by category
    @GetMapping("/filter/category")
    public List<Document> filterByCategory(@RequestParam String category) {
        return documentService.getByCategory(category);
    }

    // 🔍 Filter by uploader
    @GetMapping("/filter/uploader")
    public List<Document> filterByUploader(@RequestParam String name) {
        return documentService.getByUploader(name);
    }

    // 📊 Analytics
    @GetMapping("/analytics/uploader")
    public List<String> getStatsByUploader() {
        return documentService.getDocumentStatsByUploader();
    }

    @GetMapping("/analytics/category")
    public List<String> getStatsByCategory() {
        return documentService.getDocumentStatsByCategory();
    }

    @GetMapping("/analytics/storage")
    public List<String> getTotalStorageByUploader() {
        return documentService.getTotalFileSizeByUploader();
    }
}
