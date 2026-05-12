package com.dms.repository;

import com.dms.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u WHERE "
            + "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR "
            + "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR "
            + "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR "
            + "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<User> searchUsers(String query, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    long countActiveUsers();

    /**
     * Paged list of deprecated (inactive) users — admin only.
     */
    Page<User> findByIsActiveFalse(Pageable pageable);

    /**
     * Lightweight list used for the audit-trail user filter (aka "employees").
     */
    @Query("SELECT u FROM User u ORDER BY u.firstName ASC, u.lastName ASC")
    java.util.List<User> findAllForDirectory();

    /**
     * Count of users created since a given timestamp.
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
    long countCreatedSince(@Param("since") java.time.LocalDateTime since);

    /**
     * All active users that hold a specific role (e.g. ROLE_ADMIN) — used to
     * fan-out notifications.
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :role AND u.isActive = true")
    java.util.List<User> findAllActiveByRole(@Param("role") com.dms.entity.RoleName role);

    /**
     * Resigned users whose effective date has passed but who are still active.
     * Used by the resignation scheduler to revoke access.
     */
    @Query("SELECT u FROM User u WHERE u.resignationEffectiveDate IS NOT NULL " +
           "AND u.resignationEffectiveDate <= :now AND u.isActive = true")
    java.util.List<User> findResignedAndDue(@Param("now") java.time.LocalDateTime now);
}
