package com.ram.notificationservice.dto;

import com.ram.notificationservice.model.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationRequest {

    @NotBlank(message = "userId cannot be empty")
    private String userId;

    @NotBlank(message = "title cannot be empty")
    private String title;

    @NotBlank(message = "message cannot be empty")
    private String message;

    @NotNull(message = "type is required - DEAL_ALERT, RE_ENGAGEMENT, ORDER_UPDATE")
    private NotificationType type;
}
