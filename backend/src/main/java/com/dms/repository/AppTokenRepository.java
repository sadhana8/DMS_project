package com.dms.repository;

import com.dms.entity.AppToken;
import com.dms.entity.AppToken.TokenType;
import com.dms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AppTokenRepository extends JpaRepository<AppToken, Long> {

    Optional<AppToken> findByTokenValueAndTokenType(String tokenValue, TokenType type);

    Optional<AppToken> findByTokenValue(String tokenValue);

    // ── Refresh token helpers ────────────────────────────────────────
    @Modifying @Transactional
    @Query("UPDATE AppToken t SET t.isRevoked = true WHERE t.user = :user AND t.tokenType = 'REFRESH'")
    void revokeAllRefreshTokensForUser(@Param("user") User user);

    /** Convenience overload used by UserServiceImpl and ResignationScheduler (no User object available). */
    @Modifying @Transactional
    @Query("UPDATE AppToken t SET t.isRevoked = true WHERE t.user.id = :userId AND t.tokenType = 'REFRESH'")
    void revokeAllRefreshTokensByUserId(@Param("userId") Long userId);

    // ── OTP helpers ──────────────────────────────────────────────────
    @Query("SELECT t FROM AppToken t WHERE t.user = :user AND t.tokenType = :type AND t.isUsed = false AND t.expiresAt > :now ORDER BY t.createdAt DESC")
    Optional<AppToken> findActiveOtp(@Param("user") User user,
                                     @Param("type") TokenType type,
                                     @Param("now") LocalDateTime now);

    // ── Cleanup ──────────────────────────────────────────────────────
    @Modifying @Transactional
    @Query("DELETE FROM AppToken t WHERE t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);

    @Modifying @Transactional
    @Query("DELETE FROM AppToken t WHERE t.user = :user AND t.tokenType = :type")
    void deleteByUserAndType(@Param("user") User user, @Param("type") TokenType type);
}
