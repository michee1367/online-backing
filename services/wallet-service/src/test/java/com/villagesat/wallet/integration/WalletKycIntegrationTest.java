package com.villagesat.wallet.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.wallet.config.WalletTestKafkaConfig;
import com.villagesat.wallet.support.WalletTestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles({"test", "dev"})
@Import(WalletTestKafkaConfig.class)
class WalletKycIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("villagesat_wallet_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void walletLifecycle_kycLimitsAndDebitEnforcement() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currency":"CDF","type":"PERSONAL","label":"Test"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kycLevel").value(0))
                .andExpect(jsonPath("$.dailyLimit").value("200000"))
                .andExpect(jsonPath("$.monthlyLimit").value("200"))
                .andReturn();

        UUID walletId = UUID.fromString(
                objectMapper.readTree(createResult.getResponse().getContentAsString())
                        .path("walletId").asText());

        credit(walletId, "1000.0000");

        debit(walletId, "30.0000", status().isOk());
        debit(walletId, "25.0000", status().isUnprocessableEntity());

        mockMvc.perform(post("/internal/wallets/users/{userId}/kyc-limits", WalletTestFixtures.USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kycLevel\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletsUpdated").value(1));

        mockMvc.perform(get("/api/v1/wallets/{walletId}", walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycLevel").value(1))
                .andExpect(jsonPath("$.dailyLimit").value("500"));

        debit(walletId, "100.0000", status().isOk());
    }

    @Test
    void applyKycLimits_internalEndpoint_upgradesToL2() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currency":"USD","type":"PERSONAL","label":"USD"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        UUID walletId = UUID.fromString(
                objectMapper.readTree(createResult.getResponse().getContentAsString())
                        .path("walletId").asText());

        mockMvc.perform(post("/internal/wallets/users/{userId}/kyc-limits", WalletTestFixtures.USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kycLevel\":2}"))
                .andExpect(status().isOk());

        JsonNode wallet = objectMapper.readTree(
                mockMvc.perform(get("/api/v1/wallets/{walletId}", walletId))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());

        assertThat(wallet.path("kycLevel").asInt()).isEqualTo(2);
        assertThat(wallet.path("dailyLimit").asText()).isEqualTo("5000");
    }

    private void credit(UUID walletId, String amount) throws Exception {
        mockMvc.perform(post("/internal/wallets/{walletId}/credit", walletId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionId": "%s",
                                  "amount": "%s",
                                  "description": "test-credit"
                                }
                                """.formatted(UUID.randomUUID(), amount)))
                .andExpect(status().isOk());
    }

    private void debit(UUID walletId, String amount,
                       org.springframework.test.web.servlet.ResultMatcher expectedStatus) throws Exception {
        mockMvc.perform(post("/internal/wallets/{walletId}/debit", walletId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionId": "%s",
                                  "amount": "%s",
                                  "description": "test-debit"
                                }
                                """.formatted(UUID.randomUUID(), amount)))
                .andExpect(expectedStatus);
    }
}
