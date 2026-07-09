package com.villagesat.gateway.fallback;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping
    public Mono<Map<String, Object>> fallback(ServerWebExchange exchange) {
        return Mono.just(Map.of(
                "error", "SERVICE_UNAVAILABLE",
                "message", "Le service est temporairement indisponible. Réessayez dans quelques instants.",
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.SERVICE_UNAVAILABLE.value()
        ));
    }
}
