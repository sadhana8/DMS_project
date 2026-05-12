package com.dms.repository;

import com.dms.entity.Notification;
import com.dms.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    List<NotificationSetting> findByUserId(Long userId);
    Optional<NotificationSetting> findByUserIdAndNotificationType(Long userId, Notification.NotificationType type);
    void deleteByUserId(Long userId);
}
