package com.dms.repository;

import com.dms.entity.TwoFactorSetting;
import com.dms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TwoFactorSettingRepository extends JpaRepository<TwoFactorSetting, Long> {
    Optional<TwoFactorSetting> findByUser(User user);
    Optional<TwoFactorSetting> findByUserId(Long userId);
}
