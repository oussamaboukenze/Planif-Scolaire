package com.planifscolaire.planning.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

@Component
public class EventCodec {
    private final ObjectMapper objectMapper;

    public EventCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Événement non sérialisable", exception);
        }
    }

    public <T> T read(String message, Class<T> type) {
        try {
            return objectMapper.readValue(message, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Événement invalide", exception);
        }
    }
}
