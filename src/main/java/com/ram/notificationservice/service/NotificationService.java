package com.ram.notificationservice.service;

import com.ram.notificationservice.dto.NotificationEvent;
import com.ram.notificationservice.dto.NotificationRequest;
import com.ram.notificationservice.dto.NotificationResponse;
import com.ram.notificationservice.exception.NotificationNotFoundException;
import com.ram.notificationservice.model.Notification;
import com.ram.notificationservice.model.NotificationStatus;
import com.ram.notificationservice.producer.NotificationProducer;
import com.ram.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationProducer notificationProducer;

    /**
     * Main flow:
     * 1. Persist to DB with PENDING status first (important - if we publish to Kafka first
     *    and the app crashes, we lose track of the notification)
     * 2. Publish to Kafka for async delivery
     * 3. Consumer updates status to SENT/FAILED
     *
     * Cache is evicted on new notifications so getByUser returns fresh data.
     */
    @Transactional
    @CacheEvict(value = "userNotifications", key = "#request.userId")
    public NotificationResponse send(NotificationRequest request) {
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .title(request.getTitle())
                .message(request.getMessage())
                .type(request.getType())
                .status(NotificationStatus.PENDING)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Saved notification id={} for userId={}", saved.getId(), saved.getUserId());

        NotificationEvent event = NotificationEvent.builder()
                .notificationId(saved.getId())
                .userId(saved.getUserId())
                .title(saved.getTitle())
                .message(saved.getMessage())
                .type(saved.getType())
                .build();

        notificationProducer.publish(event);
        return toResponse(saved);
    }

    /**
     * Cached by userId - avoids hitting DB on every poll.
     * Cache evicts when new notification is sent to same user.
     *
     * Tradeoff: status might be slightly stale (Redis TTL = 1hr).
     * Acceptable for this use case since this is just a history view.
     */
    @Cacheable(value = "userNotifications", key = "#userId")
    public List<NotificationResponse> getByUser(String userId) {
        log.debug("Cache miss - fetching from DB for userId={}", userId);
        return notificationRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // no cache here - status queries are admin-level and need real-time data
    public List<NotificationResponse> getByStatus(NotificationStatus status) {
        return notificationRepository.findByStatus(status)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public NotificationResponse getById(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));
        return toResponse(notification);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .status(n.getStatus())
                .retryCount(n.getRetryCount())
                .createdAt(n.getCreatedAt())
                .sentAt(n.getSentAt())
                .build();
    }
}
