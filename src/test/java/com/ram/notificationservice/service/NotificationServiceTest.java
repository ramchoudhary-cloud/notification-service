package com.ram.notificationservice.service;

import com.ram.notificationservice.dto.NotificationRequest;
import com.ram.notificationservice.dto.NotificationResponse;
import com.ram.notificationservice.exception.NotificationNotFoundException;
import com.ram.notificationservice.model.Notification;
import com.ram.notificationservice.model.NotificationStatus;
import com.ram.notificationservice.model.NotificationType;
import com.ram.notificationservice.producer.NotificationProducer;
import com.ram.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationProducer notificationProducer;

    @InjectMocks
    private NotificationService notificationService;

    private Notification sampleNotification;

    @BeforeEach
    void setUp() {
        sampleNotification = Notification.builder()
                .id(1L)
                .userId("user123")
                .title("Weekend Deals!")
                .message("Up to 60% off this weekend.")
                .type(NotificationType.DEAL_ALERT)
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void send_shouldPersistNotificationAndPublishToKafka() {
        NotificationRequest request = new NotificationRequest();
        request.setUserId("user123");
        request.setTitle("Weekend Deals!");
        request.setMessage("Up to 60% off this weekend.");
        request.setType(NotificationType.DEAL_ALERT);

        when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);
        doNothing().when(notificationProducer).publish(any());

        NotificationResponse response = notificationService.send(request);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo("user123");
        assertThat(response.getStatus()).isEqualTo(NotificationStatus.PENDING);

        // both should be called exactly once
        verify(notificationRepository, times(1)).save(any());
        verify(notificationProducer, times(1)).publish(any());
    }

    @Test
    void send_shouldPublishToKafkaAfterSave_notBefore() {
        // this ordering matters - if Kafka publish fails after save, we still have the record
        // if we saved after publish, a crash would leave orphan Kafka events
        NotificationRequest request = new NotificationRequest();
        request.setUserId("user123");
        request.setTitle("Test");
        request.setMessage("Test message");
        request.setType(NotificationType.RE_ENGAGEMENT);

        when(notificationRepository.save(any())).thenReturn(sampleNotification);

        notificationService.send(request);

        var inOrder = inOrder(notificationRepository, notificationProducer);
        inOrder.verify(notificationRepository).save(any());
        inOrder.verify(notificationProducer).publish(any());
    }

    @Test
    void getByUser_shouldReturnAllNotificationsForUser() {
        Notification second = Notification.builder()
                .id(2L)
                .userId("user123")
                .title("Come back!")
                .message("We miss you.")
                .type(NotificationType.RE_ENGAGEMENT)
                .status(NotificationStatus.SENT)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findByUserId("user123"))
                .thenReturn(List.of(sampleNotification, second));

        List<NotificationResponse> results = notificationService.getByUser("user123");

        assertThat(results).hasSize(2);
        assertThat(results).extracting(NotificationResponse::getUserId)
                .containsOnly("user123");
    }

    @Test
    void getByUser_shouldReturnEmptyList_whenUserHasNoNotifications() {
        when(notificationRepository.findByUserId("unknownUser")).thenReturn(List.of());

        List<NotificationResponse> results = notificationService.getByUser("unknownUser");

        assertThat(results).isEmpty();
        verify(notificationRepository).findByUserId("unknownUser");
    }

    @Test
    void getById_shouldThrowException_whenNotificationNotFound() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.getById(99L))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getById_shouldReturnNotification_whenExists() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(sampleNotification));

        NotificationResponse response = notificationService.getById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Weekend Deals!");
    }

    @Test
    void getByStatus_shouldReturnOnlyMatchingStatus() {
        when(notificationRepository.findByStatus(NotificationStatus.FAILED))
                .thenReturn(List.of());

        List<NotificationResponse> results = notificationService.getByStatus(NotificationStatus.FAILED);

        assertThat(results).isEmpty();
        verify(notificationRepository).findByStatus(NotificationStatus.FAILED);
    }
}
