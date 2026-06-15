package com.ram.notificationservice.consumer;

import com.ram.notificationservice.dto.NotificationEvent;
import com.ram.notificationservice.model.NotificationStatus;
import com.ram.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;

    @KafkaListener(
        topics = "${kafka.topic.notifications}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(NotificationEvent event) {
        log.info("Received event from Kafka: notifId={}, userId={}, type={}",
                event.getNotificationId(), event.getUserId(), event.getType());

        notificationRepository.findById(event.getNotificationId()).ifPresentOrElse(notification -> {
            try {
                // simulate actual delivery - in real system this calls FCM/APNs/SMS gateway
                deliver(event);

                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                notificationRepository.save(notification);

                log.info("notifId={} delivered successfully", event.getNotificationId());

            } catch (Exception e) {
                log.error("Delivery failed for notifId={}: {}", event.getNotificationId(), e.getMessage());

                // track retry count so we can stop retrying after N attempts
                notification.setRetryCount(notification.getRetryCount() + 1);
                notification.setStatus(NotificationStatus.FAILED);
                notificationRepository.save(notification);

                // TODO: publish to a dead letter topic if retryCount > 3
                // haven't implemented DLQ yet but that's the next step
            }
        }, () -> log.warn("Notification {} not found in DB - skipped", event.getNotificationId()));
    }

    private void deliver(NotificationEvent event) {
        // placeholder for actual push/SMS/email delivery
        log.debug("Delivering {} to userId={}: {}",
                event.getType(), event.getUserId(), event.getTitle());
    }
}
