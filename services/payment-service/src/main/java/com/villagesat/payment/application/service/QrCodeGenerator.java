package com.villagesat.payment.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Component
public class QrCodeGenerator {

    private static final long QR_EXPIRY_MINUTES = 15;

    private final ObjectMapper objectMapper;

    public QrCodeGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String generate(UUID merchantId, String merchantCode, BigDecimal amount,
                           String currency, String reference) {
        try {
            Map<String, Object> payload = Map.of(
                    "merchantId", merchantId.toString(),
                    "merchantCode", merchantCode,
                    "amount", amount.toPlainString(),
                    "currency", currency,
                    "reference", reference,
                    "expiresAt", Instant.now().plus(QR_EXPIRY_MINUTES, ChronoUnit.MINUTES).toString()
            );
            String json = objectMapper.writeValueAsString(payload);
            return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to generate QR code payload", e);
        }
    }

    public Instant defaultExpiry() {
        return Instant.now().plus(QR_EXPIRY_MINUTES, ChronoUnit.MINUTES);
    }
}
