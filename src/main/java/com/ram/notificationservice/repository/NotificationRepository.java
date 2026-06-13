package com.ram.notificationservice.repository;

import com.ram.notificationservice.model.Notification;
import com.ram.notificationservice.model.NotificationStatus;
import com.ram.notificationservice.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserId(String userId);

    List<Notification> findByStatus(NotificationStatus status);

    List<Notification> findByUserIdAndType(String userId, NotificationType type);

    // needed for retry logic - fetch failed ones with low retry count
    List<Notification> findByStatusAndRetryCountLessThan(NotificationStatus status, int maxRetries);
}
