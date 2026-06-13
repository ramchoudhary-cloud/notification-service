package com.ram.notificationservice.dto;

import com.ram.notificationservice.model.NotificationStatus;
import com.ram.notificationservice.model.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private String userId;
    private String title;
    private String message;
    private NotificationType type;
    private NotificationStatus status;
    private int retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}
