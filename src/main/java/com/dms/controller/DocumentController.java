package com.dms.controller;

import com.dms.models.Document;
import com.dms.services.DocumentService;
import com.dms.dao.DocumentRepository;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*") // allows frontend or Postman to access this API
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentRepository documentRepository;

    @Autowired
    public DocumentController(DocumentService documentService, DocumentRepository documentRepository) {
        this.documentService = documentService;
        this.documentRepository = documentRepository;
    }

    // 📤 Upload Document with Metadata
    @PostMapping("/upload")
    public ResponseEntity<Document> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("uploadedBy") String uploadedBy,
            @RequestParam("category") String category
    ) throws IOException {
        Document savedDoc = documentService.uploadDocument(file, title, description, uploadedBy, category);
        return new ResponseEntity<>(savedDoc, HttpStatus.CREATED);
    }

    // 📜 Get All Documents
    @GetMapping
    public List<Document> getAllDocuments() {
        return documentService.getAllDocuments();
    }

    // 🔍 Get Document by ID
    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocumentById(@PathVariable Long id) {
        Document doc = documentService.getDocument(id);
        return (doc != null) ? ResponseEntity.ok(doc) : ResponseEntity.notFound().build();
    }

    // ⬇️ Download a Document
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) throws IOException {
        Document doc = documentService.getDocument(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        Path path = Paths.get(doc.getFilePath());
        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                .header("X-Uploaded-By", doc.getUploadedBy())
                .header("X-File-Size", String.valueOf(doc.getFileSize()))
                .header("X-Category", doc.getCategory())
                .body(resource);
    }

    // ✏️ Update Document Metadata
    @PutMapping("/{id}")
    public ResponseEntity<Document> updateDocumentMetadata(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String description
    ) {
        Document updated = documentService.updateMetadata(id, title, description);
        return (updated != null) ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    // 🚫 Deprecate Document (mark as outdated)
    @PutMapping("/deprecate/{id}")
    public ResponseEntity<String> deprecateDocument(@PathVariable Long id) {
        boolean success = documentService.deprecateDocument(id);
        return success
                ? ResponseEntity.ok("Document deprecated successfully")
                : ResponseEntity.notFound().build();
    }

    // 🔍 Filter by Category
    @GetMapping("/filter/category")
    public List<Document> filterByCategory(@RequestParam String category) {
        return documentRepository.findByCategory(category);
    }

    // 🔍 Filter by Uploader
    @GetMapping("/filter/uploader")
    public List<Document> filterByUploader(@RequestParam String name) {
        return documentRepository.findByUploadedBy(name);
    }

    // 📊 Analytics: Documents per Uploader
    @GetMapping("/analytics/uploader")
    public List<String> getStatsByUploader() {
        return documentService.getDocumentStatsByUploader();
    }

    // 📊 Analytics: Documents per Category
    @GetMapping("/analytics/category")
    public List<String> getStatsByCategory() {
        return documentService.getDocumentStatsByCategory();
    }

    // 📊 Analytics: Total Storage per Uploader
    @GetMapping("/analytics/storage")
    public List<String> getTotalStorageByUploader() {
        return documentService.getTotalFileSizeByUploader();
    }
}
