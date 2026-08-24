package com.agrawalpulse.common.notification;

// Thin port over whatever fan-out mechanism a given environment uses (SNS/SES/EventBridge in
// AWS, a no-op in local). Business modules depend only on this interface so they never branch
// on Spring profile themselves.
public interface NotificationPublisher {

    void publish(String topic, String message);
}
