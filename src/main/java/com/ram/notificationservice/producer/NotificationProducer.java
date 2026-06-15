package com.ram.notificationservice.producer;

import com.ram.notificationservice.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Value("${kafka.topic.notifications}")
    private String topic;

    /**
     * Publishes notification event to Kafka.
     * Using userId as message key so all notifications for same user
     * go to same partition - guarantees ordering per user.
     *
     * Note: send() is async. Added callback to log failures.
     * Initially missed this and had silent failures in dev.
     */
    public void publish(NotificationEvent event) {
        CompletableFuture<SendResult<String, NotificationEvent>> future =
                kafkaTemplate.send(topic, event.getUserId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish notification event for userId={}, notifId={}: {}",
                        event.getUserId(), event.getNotificationId(), ex.getMessage());
            } else {
                log.debug("Published notifId={} to partition={}",
                        event.getNotificationId(),
                        result.getRecordMetadata().partition());
            }
        });
    }
}
