package com.villagesat.user.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.user.config.UserTestKafkaConfig;
import com.villagesat.user.domain.port.in.UserUseCase;
import com.villagesat.user.domain.port.out.KeycloakSyncPort;
import com.villagesat.user.support.UserTestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles({"test", "dev"})
@Import(UserTestKafkaConfig.class)
class UserIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("villagesat_user_test")
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
    UserUseCase userUseCase;

    @MockitoBean
    KeycloakSyncPort keycloakSync;

    // ── Provisioning via useCase → GET /api/v1/users/me ───────────

    @Test
    void provisionAndGetCurrentUser() throws Exception {
        userUseCase.provisionFromRegistration(new UserUseCase.ProvisionUserCommand(
                UserTestFixtures.USER_ID, "alice@example.com", "+243810000001",
                "Alice", "Mutombo", "CD", 0, "ACTIVE"
        ));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", devJwt(UserTestFixtures.USER_ID.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(UserTestFixtures.USER_ID.toString()))
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.lastName").value("Mutombo"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    // ── GET /api/v1/users/me (user not found) ─────────────────────

    @Test
    void getMe_userNotProvisioned_returns404() throws Exception {
        String randomId = java.util.UUID.randomUUID().toString();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", devJwt(randomId)))
                .andExpect(status().isNotFound());
    }

    // ── PATCH /api/v1/users/me → profile update ───────────────────

    @Test
    void updateProfile() throws Exception {
        java.util.UUID userId = java.util.UUID.randomUUID();
        userUseCase.provisionFromRegistration(new UserUseCase.ProvisionUserCommand(
                userId, "bob@example.com", "+243810000002",
                "Bob", "Kasongo", "CD", 0, "ACTIVE"
        ));

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", devJwt(userId.toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Robert",
                                  "addressCity": "Lubumbashi",
                                  "preferredLanguage": "en"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Robert"))
                .andExpect(jsonPath("$.addressCity").value("Lubumbashi"))
                .andExpect(jsonPath("$.preferredLanguage").value("en"));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", devJwt(userId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Robert"))
                .andExpect(jsonPath("$.addressCity").value("Lubumbashi"));
    }

    // ── GET /api/v1/users/me/data-export → 202 ───────────────────

    @Test
    void requestDataExport_returns202() throws Exception {
        java.util.UUID userId = java.util.UUID.randomUUID();
        userUseCase.provisionFromRegistration(new UserUseCase.ProvisionUserCommand(
                userId, "export@example.com", null,
                "Export", "Test", "CD", 0, "ACTIVE"
        ));

        mockMvc.perform(get("/api/v1/users/me/data-export")
                        .header("Authorization", devJwt(userId.toString())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.exportId").isNotEmpty());
    }

    // ── Internal endpoint: GET /internal/users/{userId} ───────────

    @Test
    void internalGetUser() throws Exception {
        java.util.UUID userId = java.util.UUID.randomUUID();
        userUseCase.provisionFromRegistration(new UserUseCase.ProvisionUserCommand(
                userId, "internal@example.com", "+243810000003",
                "Internal", "User", "CD", 0, "ACTIVE"
        ));

        mockMvc.perform(get("/internal/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.firstName").value("Internal"));
    }

    // ── Internal endpoint: POST /internal/users/provision ─────────

    @Test
    void internalProvision() throws Exception {
        java.util.UUID userId = java.util.UUID.randomUUID();

        mockMvc.perform(post("/internal/users/provision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "email": "provision@example.com",
                                  "phone": "+243810000004",
                                  "firstName": "Provisioned",
                                  "lastName": "User",
                                  "countryCode": "CD",
                                  "kycLevel": 0,
                                  "status": "PENDING_VERIFICATION"
                                }
                                """.formatted(userId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/internal/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Provisioned"))
                .andExpect(jsonPath("$.email").value("provision@example.com"));
    }

    /**
     * Generates a dev-profile HS256 JWT for the given subject.
     * Matches the DevSecurityConfig secret key.
     */
    private static String devJwt(String subject) {
        try {
            String secret = "villagesat-dev-secret-key-32bytes!!";
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(
                    secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));

            String header = java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());
            long now = System.currentTimeMillis() / 1000;
            String payload = java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(("{\"sub\":\"%s\",\"iat\":%d,\"exp\":%d,"
                            + "\"realm_access\":{\"roles\":[\"CUSTOMER\"]}}")
                            .formatted(subject, now, now + 3600)
                            .getBytes());

            String sigInput = header + "." + payload;
            String signature = java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(sigInput.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

            return "Bearer " + sigInput + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate dev JWT", e);
        }
    }
}
