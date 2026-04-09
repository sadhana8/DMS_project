package com.dms.repository;

import com.dms.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link RefreshToken} entities.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Finds a refresh token by its opaque string value. Used when the client
     * submits a refresh request to obtain a new access token.
     *
     * @param token the UUID refresh-token string supplied by the client
     * @return an {@link Optional} containing the token entity, or empty if not
     * found
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Revokes all active refresh tokens for a given user in a single UPDATE
     * statement.
     *
     * <p>
     * Called in three scenarios:
     * <ul>
     * <li>User logs out → all sessions on all devices are invalidated.</li>
     * <li>User changes password → security best-practice to force
     * re-login.</li>
     * <li>Admin resets a user's password → same forced re-login.</li>
     * </ul>
     *
     * @param userId the ID of the user whose tokens should be revoked
     */
    @Modifying
    @Query("UPDATE RefreshToken t SET t.isRevoked = true WHERE t.user.id = :userId")
    void revokeAllByUserId(Long userId);
}
