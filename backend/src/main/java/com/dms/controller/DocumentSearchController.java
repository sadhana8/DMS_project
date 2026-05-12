package com.dms.controller;

import com.dms.dto.response.DocumentResponse;
import com.dms.service.impl.DocumentAdvancedSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Multi-criteria document search. Lives at {@code /document-search/advanced}
 * (rather than under {@code /documents}) so it does not collide with or
 * shadow the existing {@code /documents/search} endpoint on
 * {@code DocumentController}.
 *
 * <p>All parameters are optional; if none are supplied the call returns
 * the same listing as the default document list (subject to visibility).
 *
 * <pre>
 * GET /document-search/advanced
 *     ?name=salary
 *     &amp;tag=april
 *     &amp;department=HR
 *     &amp;ownerId=12
 *     &amp;ownerEmail=ram@
 *     &amp;dateFrom=2025-04-01
 *     &amp;dateTo=2025-04-30
 *     &amp;page=0&amp;size=20
 * </pre>
 */
@RestController
@RequestMapping("/document-search")
@RequiredArgsConstructor
public class DocumentSearchController {

    private final DocumentAdvancedSearchService searchService;

    @GetMapping("/advanced")
    public ResponseEntity<Page<DocumentResponse>> advancedSearch(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) String ownerEmail,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(searchService.search(
                ud.getUsername(), name, tag, department,
                ownerId, ownerEmail, dateFrom, dateTo,
                page, size));
    }
}
