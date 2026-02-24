package com.dms.models;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "document")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 📄 Basic Metadata
    private String title;
    private String description;

    // 📁 File Metadata
    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "upload_time")
    private LocalDateTime uploadTime = LocalDateTime.now();

    @Column(name = "is_deprecated")
    private boolean deprecated = false;

    // 🧍 Uploader Metadata
    @Column(name = "uploaded_by")
    private String uploadedBy;

    // 📏 File Size (in bytes)
    @Column(name = "file_size")
    private Long fileSize;

    // 🏷️ Category or Type
    @Column(name = "category")
    private String category;
}
