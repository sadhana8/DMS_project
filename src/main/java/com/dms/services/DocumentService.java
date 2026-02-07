package com.dms.services;

import com.dms.models.Document;
import com.dms.dao.DocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    // Upload new document
    public Document uploadDocument(MultipartFile file, String title, String description) throws IOException {
        Path path = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(path);

        String fileName = file.getOriginalFilename();
        Path target = path.resolve(fileName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        Document doc = new Document();
        doc.setTitle(title);
        doc.setDescription(description);
        doc.setFileName(fileName);
        doc.setFileType(file.getContentType());
        doc.setFilePath(target.toString());
        doc.setUploadTime(LocalDateTime.now());

        return documentRepository.save(doc);
    }

    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    public Document getDocument(Long id) {
        return documentRepository.findById(id).orElse(null);
    }

    public Document updateMetadata(Long id, String title, String description) {
        Document doc = getDocument(id);
        if (doc != null) {
            doc.setTitle(title);
            doc.setDescription(description);
            return documentRepository.save(doc);
        }
        return null;
    }

    public boolean deprecateDocument(Long id) {
        Document doc = getDocument(id);
        if (doc != null) {
            doc.setDeprecated(true);
            documentRepository.save(doc);
            return true;
        }
        return false;
    }

    public byte[] downloadFile(Long id) throws IOException {
        Document doc = getDocument(id);
        if (doc == null) throw new RuntimeException("Document not found");
        Path path = Paths.get(doc.getFilePath());
        return Files.readAllBytes(path);
    }
}
