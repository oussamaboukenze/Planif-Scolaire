package com.planifscolaire.presences.event;

import com.planifscolaire.presences.entite.OutboxEvent;
import com.planifscolaire.presences.repository.OutboxRepository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "app.events.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {
    private final OutboxRepository repository;
    private final KafkaTemplate<String, String> kafka;

    public OutboxPublisher(OutboxRepository repository, KafkaTemplate<String, String> kafka) {
        this.repository = repository;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${app.outbox.delay:1000}")
    @Transactional
    public void publish() {
        for (OutboxEvent event : repository.findTop100ByOrderByIdAsc()) {
            try {
                kafka.send(event.getTopic(), event.getMessageKey(), event.getPayload())
                        .get(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Publication interrompue", e);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Kafka indisponible : événement conservé pour réessai", e);
            }
            repository.delete(event);
        }
    }
}
