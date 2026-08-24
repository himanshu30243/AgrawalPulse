package com.agrawalpulse.common.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("local")
public class LocalNoOpNotificationPublisher implements NotificationPublisher {

    @Override
    public void publish(String topic, String message) {
        log.info("[local-noop-notification] topic={} message={}", topic, message);
    }
}
