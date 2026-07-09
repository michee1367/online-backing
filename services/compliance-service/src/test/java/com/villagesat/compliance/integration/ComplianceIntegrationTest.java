package com.villagesat.compliance.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.compliance.config.ComplianceTestKafkaConfig;
import com.villagesat.compliance.domain.model.Screening;
import com.villagesat.compliance.domain.port.out.AmlScreeningPort;
import com.villagesat.compliance.domain.port.out.IdentityVerificationPort;
import com.villagesat.compliance.domain.port.out.IdentityVerificationPort.VerificationResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles({"test", "dev"})
@Import(ComplianceTestKafkaConfig.class)
class ComplianceIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("villagesat_compliance_test")
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

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    AmlScreeningPort amlScreeningPort;

    @MockBean
    IdentityVerificationPort identityVerificationPort;

    static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    static final UUID REVIEWER_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");

    @Test
    void submitKyc_returnsAccepted_andCreatesSubmission() throws Exception {
        stubClearScreenings();
        when(identityVerificationPort.verify(any()))
                .thenReturn(new VerificationResult(new BigDecimal("0.80"), "sim-ref", true));

        MvcResult result = mockMvc.perform(post("/api/v1/kyc/submit")
                        .with(jwt().jwt(jwt -> jwt
                                .subject(USER_ID.toString())
                                .claim("realm_access", Map.of("roles", List.of("CUSTOMER")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "level": 1,
                                    "documentType": "NATIONAL_ID",
                                    "documentNumber": "DOC-123456",
                                    "documentFrontUrl": "front-key",
                                    "documentBackUrl": "back-key",
                                    "selfieUrl": "selfie-key"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.submissionId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.targetLevel").value(1))
                .andExpect(jsonPath("$.riskScore").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("submissionId").asText()).isNotBlank();
    }

    @Test
    void submitKyc_autoApprove_returnsOk() throws Exception {
        UUID autoApproveUser = UUID.randomUUID();
        stubClearScreenings();
        when(identityVerificationPort.verify(any()))
                .thenReturn(new VerificationResult(new BigDecimal("0.97"), "sim-ref", true));

        mockMvc.perform(post("/api/v1/kyc/submit")
                        .with(jwt().jwt(jwt -> jwt
                                .subject(autoApproveUser.toString())
                                .claim("realm_access", Map.of("roles", List.of("CUSTOMER")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "level": 1,
                                    "documentType": "PASSPORT",
                                    "documentNumber": "PASS-789",
                                    "documentFrontUrl": "front",
                                    "selfieUrl": "selfie"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.message").value("KYC approuvé automatiquement"));

        verify(kafkaTemplate).send(eq("kyc.events"), eq(autoApproveUser.toString()), anyString());
    }

    @Test
    void getStatus_afterSubmission_returnsCurrentStatus() throws Exception {
        UUID statusUser = UUID.randomUUID();
        stubClearScreenings();
        when(identityVerificationPort.verify(any()))
                .thenReturn(new VerificationResult(new BigDecimal("0.80"), "sim-ref", true));

        mockMvc.perform(post("/api/v1/kyc/submit")
                        .with(jwt().jwt(jwt -> jwt
                                .subject(statusUser.toString())
                                .claim("realm_access", Map.of("roles", List.of("CUSTOMER")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "level": 1,
                                    "documentType": "NATIONAL_ID",
                                    "documentNumber": "DOC-STATUS",
                                    "documentFrontUrl": "front",
                                    "selfieUrl": "selfie"
                                }
                                """))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/v1/kyc/status")
                        .with(jwt().jwt(jwt -> jwt
                                .subject(statusUser.toString())
                                .claim("realm_access", Map.of("roles", List.of("CUSTOMER"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.level").value(0))
                .andExpect(jsonPath("$.limits").isNotEmpty())
                .andExpect(jsonPath("$.limits.dailyTransfer").value(50));
    }

    @Test
    void getStatus_noSubmission_returnsNotSubmitted() throws Exception {
        UUID newUser = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/kyc/status")
                        .with(jwt().jwt(jwt -> jwt
                                .subject(newUser.toString())
                                .claim("realm_access", Map.of("roles", List.of("CUSTOMER"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_SUBMITTED"))
                .andExpect(jsonPath("$.level").value(0))
                .andExpect(jsonPath("$.limits.dailyTransfer").value(50));
    }

    @Test
    void listPendingReviews_asAdmin_returnsList() throws Exception {
        UUID reviewUser = UUID.randomUUID();
        stubClearScreenings();
        when(identityVerificationPort.verify(any()))
                .thenReturn(new VerificationResult(new BigDecimal("0.80"), "sim-ref", true));

        mockMvc.perform(post("/api/v1/kyc/submit")
                        .with(jwt().jwt(jwt -> jwt
                                .subject(reviewUser.toString())
                                .claim("realm_access", Map.of("roles", List.of("MERCHANT")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "level": 2,
                                    "documentType": "PASSPORT",
                                    "documentNumber": "DOC-REVIEW",
                                    "documentFrontUrl": "front",
                                    "selfieUrl": "selfie"
                                }
                                """))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/v1/compliance/kyc/pending")
                        .with(jwt().jwt(jwt -> jwt
                                .subject(REVIEWER_ID.toString())
                                .claim("realm_access", Map.of("roles", List.of("ADMIN"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void approveKyc_publishesKafkaEvent() throws Exception {
        UUID approveUser = UUID.randomUUID();
        stubClearScreenings();
        when(identityVerificationPort.verify(any()))
                .thenReturn(new VerificationResult(new BigDecimal("0.80"), "sim-ref", true));

        MvcResult submitResult = mockMvc.perform(post("/api/v1/kyc/submit")
                        .with(jwt().jwt(jwt -> jwt
                                .subject(approveUser.toString())
                                .claim("realm_access", Map.of("roles", List.of("CUSTOMER")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "level": 1,
                                    "documentType": "NATIONAL_ID",
                                    "documentNumber": "DOC-APPROVE",
                                    "documentFrontUrl": "front",
                                    "selfieUrl": "selfie"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andReturn();

        String submissionId = objectMapper.readTree(
                submitResult.getResponse().getContentAsString()).path("submissionId").asText();

        mockMvc.perform(post("/api/v1/compliance/kyc/{id}/approve", submissionId)
                        .with(jwt().jwt(jwt -> jwt
                                .subject(REVIEWER_ID.toString())
                                .claim("realm_access", Map.of("roles", List.of("COMPLIANCE_OFFICER")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"notes": "Documents valides, approuvé"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.submissionId").value(submissionId));

        verify(kafkaTemplate).send(eq("kyc.events"), eq(approveUser.toString()), contains("kyc.approved"));
    }

    @Test
    void submitKyc_sanctionsMatch_returnsConflict() throws Exception {
        UUID sanctionUser = UUID.randomUUID();
        Instant now = Instant.now();
        when(amlScreeningPort.screenUser(eq(sanctionUser), isNull(), anyString()))
                .thenReturn(List.of(
                        new Screening(UUID.randomUUID(), sanctionUser, null,
                                Screening.ScreeningType.PEP, Screening.ScreeningResult.CLEAR,
                                "test", Map.of(), now),
                        new Screening(UUID.randomUUID(), sanctionUser, null,
                                Screening.ScreeningType.SANCTIONS, Screening.ScreeningResult.MATCH,
                                "test", Map.of(), now),
                        new Screening(UUID.randomUUID(), sanctionUser, null,
                                Screening.ScreeningType.ADVERSE_MEDIA, Screening.ScreeningResult.CLEAR,
                                "test", Map.of(), now)
                ));

        mockMvc.perform(post("/api/v1/kyc/submit")
                        .with(jwt().jwt(jwt -> jwt
                                .subject(sanctionUser.toString())
                                .claim("realm_access", Map.of("roles", List.of("CUSTOMER")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "level": 1,
                                    "documentType": "NATIONAL_ID",
                                    "documentNumber": "SANCTION_TEST_DOC",
                                    "documentFrontUrl": "front",
                                    "selfieUrl": "selfie"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("KYC_SANCTIONS_MATCH"));
    }

    @Test
    void submitKyc_invalidLevel_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/kyc/submit")
                        .with(jwt().jwt(jwt -> jwt
                                .subject(UUID.randomUUID().toString())
                                .claim("realm_access", Map.of("roles", List.of("CUSTOMER")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "level": 5,
                                    "documentType": "NATIONAL_ID",
                                    "documentNumber": "DOC-INVALID",
                                    "documentFrontUrl": "front",
                                    "selfieUrl": "selfie"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private void stubClearScreenings() {
        Instant now = Instant.now();
        when(amlScreeningPort.screenUser(any(UUID.class), isNull(), anyString()))
                .thenReturn(List.of(
                        new Screening(UUID.randomUUID(), USER_ID, null,
                                Screening.ScreeningType.PEP, Screening.ScreeningResult.CLEAR,
                                "test", Map.of(), now),
                        new Screening(UUID.randomUUID(), USER_ID, null,
                                Screening.ScreeningType.SANCTIONS, Screening.ScreeningResult.CLEAR,
                                "test", Map.of(), now),
                        new Screening(UUID.randomUUID(), USER_ID, null,
                                Screening.ScreeningType.ADVERSE_MEDIA, Screening.ScreeningResult.CLEAR,
                                "test", Map.of(), now)
                ));
    }
}
