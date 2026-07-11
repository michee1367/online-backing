package com.villagesat.transaction.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.transaction.adapter.out.wallet.WalletClient;
import com.villagesat.transaction.config.TransactionTestKafkaConfig;
import com.villagesat.transaction.domain.port.out.FraudScoringPort;
import com.villagesat.transaction.domain.port.out.TransactionRepository;
import com.villagesat.transaction.support.TransactionTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles({"test", "dev"})
@Import(TransactionTestKafkaConfig.class)
class TransferIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("villagesat_txn_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TransactionRepository transactionRepository;

    @MockBean
    WalletClient walletClient;

    @MockBean
    FraudScoringPort fraudScoringPort;

    @BeforeEach
    void setUp() {
        reset(walletClient, fraudScoringPort);
        when(fraudScoringPort.score(any())).thenReturn(
                new FraudScoringPort.FraudResult(5, FraudScoringPort.FraudAction.ALLOW));
        when(walletClient.getWallet(TransactionTestFixtures.SOURCE_WALLET))
                .thenReturn(new WalletClient.WalletResponse(TransactionTestFixtures.SOURCE_WALLET, "CDF"));
        when(walletClient.getWallet(TransactionTestFixtures.DEST_WALLET))
                .thenReturn(new WalletClient.WalletResponse(TransactionTestFixtures.DEST_WALLET, "CDF"));
    }

    @Test
    void transfer_success_persistsCompletedTransaction() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceWalletId": "%s",
                                  "destinationWalletId": "%s",
                                  "amount": "5000.0000",
                                  "currency": "CDF",
                                  "description": "integration-test"
                                }
                                """.formatted(TransactionTestFixtures.SOURCE_WALLET, TransactionTestFixtures.DEST_WALLET)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.fee").value("100.0000"))
                .andExpect(jsonPath("$.totalDebited").value("5100.0000"));

        verify(walletClient).debit(eq(TransactionTestFixtures.SOURCE_WALLET), any(UUID.class), any(), anyString());
        verify(walletClient).credit(eq(TransactionTestFixtures.DEST_WALLET), any(UUID.class), any(), anyString());

        assertThat(transactionRepository.findByIdempotencyKey(idempotencyKey))
                .isPresent()
                .get()
                .satisfies(tx -> assertThat(tx.status().name()).isEqualTo("COMPLETED"));
    }

    @Test
    void transfer_idempotentReplay_returnsCachedResultWithoutDoubleDebit() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();
        String body = """
                {
                  "sourceWalletId": "%s",
                  "destinationWalletId": "%s",
                  "amount": "1000.0000",
                  "currency": "CDF"
                }
                """.formatted(TransactionTestFixtures.SOURCE_WALLET, TransactionTestFixtures.DEST_WALLET);

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        clearInvocations(walletClient);

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verifyNoInteractions(walletClient);
    }

    @Test
    void transfer_fraudBlocked_returns403() throws Exception {
        when(fraudScoringPort.score(any())).thenReturn(
                new FraudScoringPort.FraudResult(98, FraudScoringPort.FraudAction.BLOCK));

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceWalletId": "%s",
                                  "destinationWalletId": "%s",
                                  "amount": "1000.0000",
                                  "currency": "CDF"
                                }
                                """.formatted(TransactionTestFixtures.SOURCE_WALLET, TransactionTestFixtures.DEST_WALLET)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FRAUD_BLOCKED"));

        verifyNoInteractions(walletClient);
    }

    @Test
    void transfer_walletFailure_persistsFailedTransaction() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();
        doThrow(new RuntimeException("limit exceeded"))
                .when(walletClient).debit(any(), any(), any(), any());

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceWalletId": "%s",
                                  "destinationWalletId": "%s",
                                  "amount": "2000.0000",
                                  "currency": "CDF"
                                }
                                """.formatted(TransactionTestFixtures.SOURCE_WALLET, TransactionTestFixtures.DEST_WALLET)))
                .andExpect(status().is5xxServerError());

        assertThat(transactionRepository.findByIdempotencyKey(idempotencyKey))
                .isPresent()
                .get()
                .satisfies(tx -> {
                    assertThat(tx.status().name()).isEqualTo("FAILED");
                    assertThat(tx.failedReason()).contains("limit exceeded");
                });

        verify(walletClient, never()).credit(any(), any(), any(), any());
    }

    @Test
    void transfer_currencyMismatch_returns400() throws Exception {
        when(walletClient.getWallet(TransactionTestFixtures.DEST_WALLET))
                .thenReturn(new WalletClient.WalletResponse(TransactionTestFixtures.DEST_WALLET, "USD"));

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceWalletId": "%s",
                                  "destinationWalletId": "%s",
                                  "amount": "1000.0000",
                                  "currency": "CDF"
                                }
                                """.formatted(TransactionTestFixtures.SOURCE_WALLET, TransactionTestFixtures.DEST_WALLET)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CURRENCY_MISMATCH"));

        verify(walletClient, never()).debit(any(), any(), any(), any());
        verify(walletClient, never()).credit(any(), any(), any(), any());
    }
}
