package com.planifscolaire.gateway.configuration;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfiguration {
    @Bean
    KeyResolver clientKeyResolver() {
        return exchange -> Mono.justOrEmpty(
                exchange.getRequest().getHeaders().getFirst("X-Client-Id"))
                .filter(clientId -> !clientId.isBlank())
                .switchIfEmpty(Mono.justOrEmpty(exchange.getRequest().getRemoteAddress())
                        .map(address -> address.getAddress() == null
                                ? address.getHostString()
                                : address.getAddress().getHostAddress()))
                .defaultIfEmpty("anonymous");
    }
}