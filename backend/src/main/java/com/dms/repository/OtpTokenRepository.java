package com.dms.repository;

import com.dms.entity.OtpToken;
import com.dms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    Optional<OtpToken> findTopByUserAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(
            User user, OtpToken.Purpose purpose);

    Optional<OtpToken> findByCodeAndUserAndPurposeAndIsUsedFalse(
            String code, User user, OtpToken.Purpose purpose);

    @Modifying
    @Query("UPDATE OtpToken o SET o.isUsed = true WHERE o.user.id = :userId AND o.purpose = :purpose")
    void invalidateAllForUser(@Param("userId") Long userId,
                              @Param("purpose") OtpToken.Purpose purpose);

    @Modifying
    @Query("DELETE FROM OtpToken o WHERE o.expiresAt < :cutoff")
    void deleteExpired(@Param("cutoff") LocalDateTime cutoff);
}
