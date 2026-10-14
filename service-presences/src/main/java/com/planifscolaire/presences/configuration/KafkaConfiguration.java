package com.planifscolaire.presences.configuration;

import com.planifscolaire.presences.event.Events;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfiguration {
    @Bean
    NewTopic scheduledTopic() {
        return TopicBuilder.name(Events.SCHEDULED).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic cancelledTopic() {
        return TopicBuilder.name(Events.CANCELLED).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic absenceTopic() {
        return TopicBuilder.name(Events.ABSENCE).partitions(1).replicas(1).build();
    }

    @Bean
    DefaultErrorHandler kafkaErrorHandler() {
        return new DefaultErrorHandler(new FixedBackOff(2000, FixedBackOff.UNLIMITED_ATTEMPTS));
    }
}
