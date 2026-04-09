package com.dms.repository;

import com.dms.entity.DeprecationStatus;
import com.dms.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User} entities.
 *
 * <p>
 * All standard list/search queries filter on
 * {@code deprecation_status = 'ACTIVE'} so deprecated users never appear in
 * normal results. Separate admin queries expose deprecated records.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds an active (non-deprecated) user by e-mail. Used by
     * {@link com.dms.security.CustomUserDetailsService}.
     *
     * @param email the e-mail address to search for
     * @return an {@link Optional} containing the active user, or empty if not
     * found
     */
    Optional<User> findByEmailAndDeprecationStatus(String email, DeprecationStatus status);

    /**
     * Finds any user by e-mail regardless of deprecation status. Used
     * internally for deprecation and restore operations.
     *
     * @param email the e-mail address to search for
     * @return an {@link Optional} containing the user (active or deprecated)
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds any user by username regardless of deprecation status.
     *
     * @param username the username to search for
     * @return an {@link Optional} containing the user
     */
    Optional<User> findByUsername(String username);

    /**
     * Returns {@code true} if any user (any status) has this e-mail.
     */
    boolean existsByEmail(String email);

    /**
     * Returns {@code true} if any user (any status) has this username.
     */
    boolean existsByUsername(String username);

    /**
     * Full-text search across active users only.
     *
     * @param query the search term (partial match, case-insensitive)
     * @param pageable pagination and sorting
     * @return a page of matching active users
     */
    @Query("SELECT u FROM User u WHERE u.deprecationStatus = 'ACTIVE' AND ("
            + "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR "
            + "LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :query, '%')) OR "
            + "LOWER(u.email)     LIKE LOWER(CONCAT('%', :query, '%')) OR "
            + "LOWER(u.username)  LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<User> searchActiveUsers(@Param("query") String query, Pageable pageable);

    /**
     * Returns all active users, paginated.
     *
     * @param pageable pagination and sorting
     * @return page of active users
     */
    Page<User> findByDeprecationStatus(DeprecationStatus status, Pageable pageable);

    /**
     * Returns all deprecated users (admin view).
     *
     * @param pageable pagination and sorting
     * @return page of deprecated users
     */
    @Query("SELECT u FROM User u WHERE u.deprecationStatus = 'DEPRECATED' ORDER BY u.deprecatedAt DESC")
    Page<User> findAllDeprecated(Pageable pageable);

    /**
     * Count of users with {@code deprecationStatus = ACTIVE} and
     * {@code isActive = true}.
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.deprecationStatus = 'ACTIVE' AND u.isActive = true")
    long countActiveUsers();
}
