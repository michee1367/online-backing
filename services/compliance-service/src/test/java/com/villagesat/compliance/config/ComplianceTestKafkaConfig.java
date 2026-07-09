package com.villagesat.compliance.config;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class ComplianceTestKafkaConfig {

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    KafkaTemplate<String, String> kafkaTemplate() {
        KafkaTemplate<String, String> template = mock(KafkaTemplate.class);
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("kyc.events", 0), 0, 0, 0, 0, 0);
        SendResult<String, String> result = new SendResult<>(
                new ProducerRecord<>("kyc.events", "key", "value"), metadata);
        when(template.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(result));
        when(template.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(result));
        return template;
    }
}
