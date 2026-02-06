
package com.dms.controller;

import com.dms.models.Document;
import com.dms.dao.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    @Autowired
    private DocumentRepository repo;

  
    @GetMapping
    public List<Document> getAll() {        
        return repo.findAll();
    }

    @PostMapping
    public Document create(@RequestBody Document document) {
        return repo.save(document);
    }
}
