package com.dms.service.impl;

import com.dms.dto.response.DocumentResponse;
import com.dms.entity.Department;
import com.dms.entity.Document;
import com.dms.entity.RoleName;
import com.dms.entity.User;
import com.dms.exception.BadRequestException;
import com.dms.exception.ResourceNotFoundException;
import com.dms.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Advanced multi-criteria document search. Implemented separately from
 * {@code DocumentServiceImpl} so existing search behaviour is untouched.
 *
 * <p>Filters supported (any combination, all optional):
 * <ul>
 *   <li>name      – matches title, original file name, file name</li>
 *   <li>tag       – substring match on the comma-separated tags column</li>
 *   <li>department – owner's department</li>
 *   <li>ownerId   – exact owner match</li>
 *   <li>ownerEmail – owner email substring match</li>
 *   <li>dateFrom  – ISO-8601 date or date-time, inclusive lower bound</li>
 *   <li>dateTo    – ISO-8601 date or date-time, inclusive upper bound</li>
 * </ul>
 *
 * <p>Visibility rules mirror the existing search: ADMIN sees everything,
 * other users see documents they own, public documents, and documents
 * shared with them via {@code DocumentPermission}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentAdvancedSearchService {

    @PersistenceContext
    private EntityManager em;

    private final UserRepository userRepository;
    private final DocumentServiceImpl documentService;

    public Page<DocumentResponse> search(String userEmail,
                                         String name,
                                         String tag,
                                         String department,
                                         Long ownerId,
                                         String ownerEmail,
                                         String dateFrom,
                                         String dateTo,
                                         int page,
                                         int size) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Department dept = parseDepartment(department);
        LocalDateTime from = parseDateLower(dateFrom);
        LocalDateTime to = parseDateUpper(dateTo);

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Document> cq = cb.createQuery(Document.class);
        Root<Document> doc = cq.from(Document.class);
        Join<Object, Object> owner = doc.join("owner", JoinType.LEFT);
        cq.select(doc).distinct(true);

        List<Predicate> preds = buildPredicates(cb, doc, owner, user,
                name, tag, dept, ownerId, ownerEmail, from, to);
        cq.where(preds.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(doc.get("createdAt")));

        Pageable pageable = PageRequest.of(page, size);
        List<Document> results = em.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        // Count
        CriteriaQuery<Long> count = cb.createQuery(Long.class);
        Root<Document> docCount = count.from(Document.class);
        Join<Object, Object> ownerCount = docCount.join("owner", JoinType.LEFT);
        count.select(cb.countDistinct(docCount));
        List<Predicate> countPreds = buildPredicates(cb, docCount, ownerCount, user,
                name, tag, dept, ownerId, ownerEmail, from, to);
        count.where(countPreds.toArray(new Predicate[0]));
        Long total = em.createQuery(count).getSingleResult();

        List<DocumentResponse> mapped = results.stream()
                .map(documentService::toResponse)
                .toList();
        return new PageImpl<>(mapped, pageable, total == null ? 0L : total);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb,
                                            Root<Document> doc,
                                            Join<Object, Object> owner,
                                            User caller,
                                            String name,
                                            String tag,
                                            Department dept,
                                            Long ownerId,
                                            String ownerEmail,
                                            LocalDateTime from,
                                            LocalDateTime to) {
        List<Predicate> p = new ArrayList<>();

        // Hide soft-deleted by default
        p.add(cb.notEqual(doc.get("status"), Document.DocumentStatus.DELETED));

        // Visibility: admin sees all; others see owned, public, or permission-granted
        if (!isAdmin(caller)) {
            Subquery<Long> permSub = cb.createQuery().subquery(Long.class);
            Root<com.dms.entity.DocumentPermission> permRoot =
                    permSub.from(com.dms.entity.DocumentPermission.class);
            permSub.select(permRoot.get("document").get("id"));
            permSub.where(cb.equal(permRoot.get("user").get("id"), caller.getId()));

            Predicate ownPred = cb.equal(doc.get("owner").get("id"), caller.getId());
            Predicate publicPred = cb.isTrue(doc.get("isPublic"));
            Predicate sharedPred = doc.get("id").in(permSub);
            p.add(cb.or(ownPred, publicPred, sharedPred));
        }

        if (name != null && !name.isBlank()) {
            String pat = "%" + name.toLowerCase() + "%";
            p.add(cb.or(
                    cb.like(cb.lower(doc.get("title")), pat),
                    cb.like(cb.lower(doc.get("originalFileName")), pat),
                    cb.like(cb.lower(doc.get("fileName")), pat)
            ));
        }

        if (tag != null && !tag.isBlank()) {
            p.add(cb.like(cb.lower(doc.get("tags")), "%" + tag.toLowerCase() + "%"));
        }

        if (dept != null) {
            p.add(cb.equal(owner.get("department"), dept));
        }

        if (ownerId != null) {
            p.add(cb.equal(owner.get("id"), ownerId));
        }

        if (ownerEmail != null && !ownerEmail.isBlank()) {
            p.add(cb.like(cb.lower(owner.get("email")),
                    "%" + ownerEmail.toLowerCase() + "%"));
        }

        if (from != null) {
            p.add(cb.greaterThanOrEqualTo(doc.get("createdAt"), from));
        }
        if (to != null) {
            p.add(cb.lessThanOrEqualTo(doc.get("createdAt"), to));
        }

        return p;
    }

    private boolean isAdmin(User user) {
        return user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
    }

    private Department parseDepartment(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Department.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(
                    "Invalid department. Allowed: HR, ACCOUNT, ENGINEERING, SALES, OPERATIONS, OTHER");
        }
    }

    private LocalDateTime parseDateLower(String s) {
        if (s == null || s.isBlank()) return null;
        return parseEither(s).toLocalDate().atStartOfDay();
    }

    private LocalDateTime parseDateUpper(String s) {
        if (s == null || s.isBlank()) return null;
        return parseEither(s).toLocalDate().atTime(23, 59, 59);
    }

    private LocalDateTime parseEither(String s) {
        try {
            return LocalDateTime.parse(s);
        } catch (DateTimeParseException ignored) { }
        try {
            return LocalDate.parse(s).atStartOfDay();
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("Invalid date '" + s + "'. Use yyyy-MM-dd or ISO date-time.");
        }
    }
}
