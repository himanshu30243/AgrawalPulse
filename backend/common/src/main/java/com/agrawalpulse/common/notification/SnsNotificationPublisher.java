package com.agrawalpulse.common.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Slf4j
@Component
@Profile("!local")
public class SnsNotificationPublisher implements NotificationPublisher {

    private final SnsClient snsClient;
    private final String defaultTopicArn;

    public SnsNotificationPublisher(@Value("${agrawalpulse.aws.region:ap-south-1}") String region,
                                     @Value("${agrawalpulse.aws.sns-topic-arn:}") String defaultTopicArn) {
        this.snsClient = SnsClient.builder().region(Region.of(region)).build();
        this.defaultTopicArn = defaultTopicArn;
    }

    @Override
    public void publish(String topic, String message) {
        String topicArn = topic != null && topic.startsWith("arn:") ? topic : defaultTopicArn;
        if (topicArn == null || topicArn.isBlank()) {
            log.warn("No SNS topic ARN configured; dropping notification: {}", message);
            return;
        }
        snsClient.publish(PublishRequest.builder()
                .topicArn(topicArn)
                .message(message)
                .build());
    }
}
