
package com.dms.controller;

import com.dms.models.Document;
import com.dms.services.DocumentService;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;
import com.dms.dao.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.io.IOException;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

  private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // POST /api/documents/upload
    @PostMapping("/upload")
    public ResponseEntity<Document> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description
    ) throws IOException {
        Document savedDoc = documentService.uploadDocument(file, title, description);
        return new ResponseEntity<>(savedDoc, HttpStatus.CREATED);
    }

    // GET /api/documents
    @GetMapping
    public List<Document> getAllDocuments() {
        return documentService.getAllDocuments();
    }

    // GET /api/documents/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocumentById(@PathVariable Long id) {
        Document doc = documentService.getDocument(id);
        return (doc != null) ? ResponseEntity.ok(doc) : ResponseEntity.notFound().build();
    }

    // GET /api/documents/download/{id}
    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id) throws IOException {
        Document doc = documentService.getDocument(id);
        if (doc == null) return ResponseEntity.notFound().build();

        byte[] data = documentService.downloadFile(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                .body(data);
    }

    // PUT /api/documents/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Document> updateDocumentMetadata(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String description
    ) {
        Document updated = documentService.updateMetadata(id, title, description);
        return (updated != null) ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    // PUT /api/documents/deprecate/{id}
    @PutMapping("/deprecate/{id}")
    public ResponseEntity<String> deprecateDocument(@PathVariable Long id) {
        boolean success = documentService.deprecateDocument(id);
        return success ? ResponseEntity.ok("Document deprecated successfully")
                       : ResponseEntity.notFound().build();
    }
}
