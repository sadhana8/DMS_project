package com.dms.services;

import com.dms.models.Document;
import com.dms.dao.DocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentService {

    // 💾 This gets the upload folder path from application.properties
    @Value("${file.upload-dir}")
    private String uploadDir;

    private final DocumentRepository documentRepository;

    // 🧱 This connects the service to your database through the repository
    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    // 📤 This method uploads a file and saves all its metadata in the database
    public Document uploadDocument(MultipartFile file, String title, String description,
            String uploadedBy, String category) throws IOException {

        // 🗂️ Create (if missing) and prepare the folder to save the file
        Path path = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(path);

        // 📎 Get the file name
        String fileName = file.getOriginalFilename();

        // 📍 Decide where the file will be saved
        Path target = path.resolve(fileName);

        String fileUrl = "http://localhost:8081/uploads/" + fileName;

        // 🧾 Create a new document object to store metadata
        Document doc = new Document();
        doc.setTitle(title);
        doc.setDescription(description);
        doc.setFileName(fileName);
        doc.setFileType(file.getContentType());
        doc.setFilePath(target.toString());
        doc.setFilePath(fileUrl);
        doc.setUploadTime(LocalDateTime.now());
        doc.setDeprecated(false);

        // 🆕 Save the new metadata fields
        doc.setUploadedBy(uploadedBy);      // who uploaded
        doc.setFileSize(file.getSize());    // file size in bytes
        doc.setCategory(category);          // file category like "Report"

        // 💽 Save everything to the database and return the saved record
        return documentRepository.save(doc);
    }

    // 📜 Get all documents
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    // 🔍 Get one document by ID
    public Document getDocument(Long id) {
        return documentRepository.findById(id).orElse(null);
    }

    // ✏️ Update title and description
    public Document updateMetadata(Long id, String title, String description) {
        Document doc = getDocument(id);
        if (doc != null) {
            doc.setTitle(title);
            doc.setDescription(description);
            return documentRepository.save(doc);
        }
        return null;
    }

    // 🚫 Mark a document as deprecated (old or outdated)
    public boolean deprecateDocument(Long id) {
        Document doc = getDocument(id);
        if (doc != null) {
            doc.setDeprecated(true);
            documentRepository.save(doc);
            return true;
        }
        return false;
    }

    // ⬇️ Download file content as bytes
    public byte[] downloadFile(Long id) throws IOException {
        Document doc = getDocument(id);
        if (doc == null) {
            throw new RuntimeException("Document not found");
        }
        Path path = Paths.get(doc.getFilePath());
        return Files.readAllBytes(path);
    }

    // 📊 Count how many documents each person uploaded
    public List<String> getDocumentStatsByUploader() {
        List<Object[]> results = documentRepository.countDocumentsByUploader();
        List<String> stats = new ArrayList<>();
        for (Object[] row : results) {
            String uploadedBy = (String) row[0];
            Long count = (Long) row[1];
            stats.add(uploadedBy + " uploaded " + count + " documents");
        }
        return stats;
    }

    // 📊 Count how many documents are in each category
    public List<String> getDocumentStatsByCategory() {
        List<Object[]> results = documentRepository.countDocumentsByCategory();
        List<String> stats = new ArrayList<>();
        for (Object[] row : results) {
            String category = (String) row[0];
            Long count = (Long) row[1];
            stats.add(category + ": " + count + " documents");
        }
        return stats;
    }

    // 📊 Show total storage used by each uploader
    public List<String> getTotalFileSizeByUploader() {
        List<Object[]> results = documentRepository.totalFileSizeByUploader();
        List<String> stats = new ArrayList<>();
        for (Object[] row : results) {
            String uploader = (String) row[0];
            Long totalSize = (Long) row[1];
            stats.add(uploader + " used " + (totalSize / 1024) + " KB storage");
        }
        return stats;
    }
}
