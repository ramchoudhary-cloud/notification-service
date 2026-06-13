package com.ram.notificationservice.dto;

import com.ram.notificationservice.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// this goes over the wire via Kafka so needs no-args constructor for Jackson
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private Long notificationId;
    private String userId;
    private String title;
    private String message;
    private NotificationType type;
}
