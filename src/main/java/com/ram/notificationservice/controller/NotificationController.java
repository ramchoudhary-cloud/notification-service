package com.ram.notificationservice.controller;

import com.ram.notificationservice.dto.NotificationRequest;
import com.ram.notificationservice.dto.NotificationResponse;
import com.ram.notificationservice.model.NotificationStatus;
import com.ram.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * POST /api/v1/notifications
     * Authenticated users can send notifications.
     * Returns 202 Accepted - delivery is async via Kafka.
     */
    @PostMapping
    public ResponseEntity<NotificationResponse> send(@Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(notificationService.send(request));
    }

    /**
     * GET /api/v1/notifications/{id}
     * Fetch a single notification by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.getById(id));
    }

    /**
     * GET /api/v1/notifications/user/{userId}
     * Fetch all notifications for a user. Results are Redis cached.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResponse>> getByUser(@PathVariable String userId) {
        return ResponseEntity.ok(notificationService.getByUser(userId));
    }

    /**
     * GET /api/v1/notifications/status/{status}
     * Admin only - see all notifications by delivery status.
     * Useful for monitoring FAILED ones.
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<NotificationResponse>> getByStatus(@PathVariable NotificationStatus status) {
        return ResponseEntity.ok(notificationService.getByStatus(status));
    }
}
