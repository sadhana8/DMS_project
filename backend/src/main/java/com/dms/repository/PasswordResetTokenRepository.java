package com.dms.repository;

import com.dms.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link PasswordResetToken} entities.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Finds a reset token by its opaque string value. Used to validate a token
     * submitted via the reset-password form.
     *
     * @param token the UUID token string from the reset link
     * @return an {@link Optional} containing the token entity, or empty if not
     * found
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Deletes all reset tokens belonging to a specific user. Called before
     * issuing a new token to prevent accumulation of stale entries.
     *
     * @param userId the ID of the user whose tokens should be removed
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.user.id = :userId")
    void deleteByUserId(Long userId);
}
