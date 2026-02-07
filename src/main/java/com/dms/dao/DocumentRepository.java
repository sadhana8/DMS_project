package com.dms.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.dms.models.Document;

public interface DocumentRepository extends JpaRepository<Document, Long> {}
