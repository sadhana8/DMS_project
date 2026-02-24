package com.dms.dao;

import com.dms.models.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    // 🔍 Search documents by category
    List<Document> findByCategory(String category);

    // 🔍 Search documents by uploader name
    List<Document> findByUploadedBy(String uploadedBy);

    // 🔍 Search documents by file type (e.g., application/pdf)
    List<Document> findByFileType(String fileType);

    // 🔍 Search documents by keyword in title
    List<Document> findByTitleContainingIgnoreCase(String keyword);

    // 📊 Count total documents uploaded by each user
    @Query("SELECT d.uploadedBy, COUNT(d) FROM Document d GROUP BY d.uploadedBy")
    List<Object[]> countDocumentsByUploader();

    // 📊 Count total documents by category
    @Query("SELECT d.category, COUNT(d) FROM Document d GROUP BY d.category")
    List<Object[]> countDocumentsByCategory();

    // 📏 Average file size by user (optional analytics)
    @Query("SELECT d.uploadedBy, AVG(d.fileSize) FROM Document d GROUP BY d.uploadedBy")
    List<Object[]> averageFileSizeByUploader();

    // 💾 Total storage (file size) used by each user
    @Query("SELECT d.uploadedBy, SUM(d.fileSize) FROM Document d GROUP BY d.uploadedBy")
    List<Object[]> totalFileSizeByUploader();
}