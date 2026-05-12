package com.dms.repository;

import com.dms.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :uid " +
           "AND (:type IS NULL OR n.type = :type) " +
           "AND (:isRead IS NULL OR n.isRead = :isRead) " +
           "AND (:from IS NULL OR n.createdAt >= :from) " +
           "AND (:to   IS NULL OR n.createdAt <= :to) " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> findFiltered(@Param("uid") Long uid,
                                    @Param("type") Notification.NotificationType type,
                                    @Param("isRead") Boolean isRead,
                                    @Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to,
                                    Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipient.id = :uid AND n.isRead = false")
    long countUnread(@Param("uid") Long uid);

    @Modifying @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = :uid AND n.isRead = false")
    void markAllRead(@Param("uid") Long uid);

    @Modifying @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.recipient.id = :uid")
    void markOneRead(@Param("id") Long id, @Param("uid") Long uid);

    @Modifying @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.createdAt < :cutoff")
    void deleteOldRead(@Param("cutoff") LocalDateTime cutoff);
}
