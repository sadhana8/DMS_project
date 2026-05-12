package com.dms.controller;

import com.dms.entity.Role;
import com.dms.entity.RoleName;
import com.dms.exception.BadRequestException;
import com.dms.exception.ResourceNotFoundException;
import com.dms.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Role management endpoints. The system uses a fixed set of organizational
 * roles defined by {@link RoleName} (ADMIN, HR, ACCOUNT, EMPLOYEE). This
 * controller exposes:
 *
 * <ul>
 *   <li>{@code GET /roles} – list all roles in the system</li>
 *   <li>{@code GET /roles/{id}} – get a single role</li>
 *   <li>{@code GET /roles/permissions} – the full role/permission matrix</li>
 *   <li>{@code GET /roles/{name}/permissions} – permissions granted to one role</li>
 * </ul>
 *
 * <p>Role-to-user assignment is performed via {@code PUT /users/{id}/roles}
 * on the existing {@code UserController}; this controller is read-only over
 * the role catalog itself.
 */
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleRepository roleRepository;

    /**
     * Static permission matrix derived from the requirements:
     *
     * <pre>
     * Feature           Admin   HR        Finance(ACCOUNT)   Employee
     * Upload docs       yes     yes       yes                yes
     * Delete docs       yes     limited   limited            no
     * Manage users      yes     no        no                 no
     * View salary docs  yes     no        yes                no
     * View own docs     yes     yes       yes                yes
     * </pre>
     */
    private static final Map<RoleName, Map<String, String>> PERMISSIONS = new LinkedHashMap<>() {{
        put(RoleName.ROLE_ADMIN, Map.of(
                "upload_docs", "yes",
                "delete_docs", "yes",
                "manage_users", "yes",
                "view_salary_docs", "yes",
                "view_own_docs", "yes",
                "manage_roles", "yes",
                "approve_workflows", "yes",
                "view_audit_logs", "yes"
        ));
        put(RoleName.ROLE_HR, Map.of(
                "upload_docs", "yes",
                "delete_docs", "limited",
                "manage_users", "no",
                "view_salary_docs", "no",
                "view_own_docs", "yes",
                "manage_roles", "no",
                "approve_workflows", "yes",
                "view_audit_logs", "no"
        ));
        put(RoleName.ROLE_ACCOUNT, Map.of(
                "upload_docs", "yes",
                "delete_docs", "limited",
                "manage_users", "no",
                "view_salary_docs", "yes",
                "view_own_docs", "yes",
                "manage_roles", "no",
                "approve_workflows", "no",
                "view_audit_logs", "no"
        ));
        put(RoleName.ROLE_EMPLOYEE, Map.of(
                "upload_docs", "yes",
                "delete_docs", "no",
                "manage_users", "no",
                "view_salary_docs", "no",
                "view_own_docs", "yes",
                "manage_roles", "no",
                "approve_workflows", "no",
                "view_audit_logs", "no"
        ));
        put(RoleName.ROLE_MANAGER, Map.of(
                "upload_docs", "yes",
                "delete_docs", "limited",
                "manage_users", "no",
                "view_salary_docs", "no",
                "view_own_docs", "yes",
                "manage_roles", "no",
                "approve_workflows", "yes",
                "view_audit_logs", "no"
        ));
        put(RoleName.ROLE_FINANCE, Map.of(
                "upload_docs", "yes",
                "delete_docs", "limited",
                "manage_users", "no",
                "view_salary_docs", "yes",
                "view_own_docs", "yes",
                "manage_roles", "no",
                "approve_workflows", "no",
                "view_audit_logs", "no"
        ));
        put(RoleName.ROLE_LEGAL, Map.of(
                "upload_docs", "yes",
                "delete_docs", "limited",
                "manage_users", "no",
                "view_salary_docs", "no",
                "view_own_docs", "yes",
                "manage_roles", "no",
                "approve_workflows", "no",
                "view_audit_logs", "no"
        ));
        put(RoleName.ROLE_REVIEWER, Map.of(
                "upload_docs", "no",
                "delete_docs", "no",
                "manage_users", "no",
                "view_salary_docs", "no",
                "view_own_docs", "yes",
                "manage_roles", "no",
                "approve_workflows", "yes",
                "view_audit_logs", "no"
        ));
    }};

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listRoles() {
        List<Role> roles = roleRepository.findAll();
        List<Map<String, Object>> body = new ArrayList<>();
        for (Role r : roles) {
            body.add(toMap(r));
        }
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getRole(@PathVariable Long id) {
        Role r = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        return ResponseEntity.ok(toMap(r));
    }

    /** Full role/permission matrix. */
    @GetMapping("/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Map<String, String>>> permissionsMatrix() {
        Map<String, Map<String, String>> out = new LinkedHashMap<>();
        for (Map.Entry<RoleName, Map<String, String>> e : PERMISSIONS.entrySet()) {
            out.put(e.getKey().name(), e.getValue());
        }
        return ResponseEntity.ok(out);
    }

    /** Permissions for a single role; {name} accepts ROLE_ADMIN or ADMIN. */
    @GetMapping("/{name}/permissions")
    public ResponseEntity<Map<String, String>> permissionsForRole(@PathVariable String name) {
        RoleName rn = parseRoleName(name);
        Map<String, String> perms = PERMISSIONS.get(rn);
        if (perms == null) throw new ResourceNotFoundException("No permissions defined for " + rn);
        return ResponseEntity.ok(perms);
    }

    private Map<String, Object> toMap(Role r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("name", r.getName().name());
        m.put("displayName", switch (r.getName()) {
            case ROLE_ADMIN    -> "Administrator";
            case ROLE_HR       -> "Human Resources";
            case ROLE_ACCOUNT  -> "Finance / Accounts";
            case ROLE_EMPLOYEE -> "Employee";
            case ROLE_MANAGER  -> "Department Manager";
            case ROLE_FINANCE  -> "Finance Team";
            case ROLE_LEGAL    -> "Legal Team";
            case ROLE_REVIEWER -> "Reviewer";
        });
        m.put("permissions", PERMISSIONS.getOrDefault(r.getName(), Map.of()));
        return m;
    }

    private RoleName parseRoleName(String s) {
        if (s == null) throw new BadRequestException("Role name required");
        String upper = s.trim().toUpperCase();
        if (!upper.startsWith("ROLE_")) upper = "ROLE_" + upper;
        try {
            return RoleName.valueOf(upper);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(
                    "Unknown role. Allowed: ROLE_ADMIN, ROLE_HR, ROLE_ACCOUNT, ROLE_EMPLOYEE, ROLE_MANAGER, ROLE_FINANCE, ROLE_LEGAL, ROLE_REVIEWER");
        }
    }
}
