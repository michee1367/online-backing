package com.villagesat.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.auth.adapter.in.web.AuthController.LoginRequest;
import com.villagesat.auth.adapter.in.web.AuthController.RefreshRequest;
import com.villagesat.auth.adapter.in.web.AuthController.RegisterRequest;
import com.villagesat.auth.config.AuthTestKafkaConfig;
import com.villagesat.auth.config.AuthTestRedisConfig;
import com.villagesat.auth.domain.model.AuthTokens;
import com.villagesat.auth.domain.port.out.IdentityProviderPort;
import com.villagesat.auth.domain.port.out.LoginAttemptPort;
import com.villagesat.auth.domain.port.out.TokenBlacklistPort;
import com.villagesat.auth.domain.port.out.UserRegistrationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles({"test", "dev"})
@Import({AuthTestKafkaConfig.class, AuthTestRedisConfig.class})
@DisplayName("Auth API — tests d'intégration")
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("villagesat_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.schemas", () -> "auth");
        registry.add("spring.flyway.default-schema", () -> "auth");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "auth");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IdentityProviderPort identityProvider;

    @MockitoBean
    private UserRegistrationEventPublisher eventPublisher;

    @MockitoBean
    private LoginAttemptPort loginAttemptPort;

    @MockitoBean
    private TokenBlacklistPort tokenBlacklist;

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @BeforeEach
    void setUp() {
        when(loginAttemptPort.isLocked(anyString())).thenReturn(false);
        doNothing().when(loginAttemptPort).recordSuccess(anyString());
        doNothing().when(loginAttemptPort).recordFailure(anyString(), anyString());
        doNothing().when(tokenBlacklist).blacklist(anyString(), anyLong());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register → 201 Created")
    void register_returns201() throws Exception {
        when(identityProvider.registerUser(any())).thenReturn(USER_ID);
        doNothing().when(identityProvider).assignRealmRole(USER_ID, "CUSTOMER");
        doNothing().when(identityProvider).setUserAttribute(eq(USER_ID), anyString(), anyString());
        doNothing().when(eventPublisher).publishUserRegistered(any(), any());

        RegisterRequest request = new RegisterRequest(
                "integ@test.com", "+33612345678", "P@ssword1234!",
                "Jean", "Dupont", "FR", true);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value("integ@test.com"))
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login → 200 avec tokens")
    void login_returns200WithTokens() throws Exception {
        String accessToken = buildFakeJwt(USER_ID, "jti-1", Instant.now().plusSeconds(3600));
        String refreshToken = "test-refresh-token";
        AuthTokens tokens = new AuthTokens(accessToken, refreshToken, 3600, "Bearer");

        when(identityProvider.authenticate("login@test.com", "P@ssword1234!")).thenReturn(tokens);

        LoginRequest request = new LoginRequest("login@test.com", "P@ssword1234!", "device-fp");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(accessToken))
                .andExpect(jsonPath("$.refreshToken").value(refreshToken))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.mfaRequired").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh → 200 avec nouveau token")
    void refresh_returns200WithNewToken() throws Exception {
        String accessToken = buildFakeJwt(USER_ID, "jti-1", Instant.now().plusSeconds(3600));
        String refreshToken = "initial-refresh";
        AuthTokens loginTokens = new AuthTokens(accessToken, refreshToken, 3600, "Bearer");

        when(identityProvider.authenticate("refresh@test.com", "P@ssword1234!")).thenReturn(loginTokens);

        LoginRequest loginRequest = new LoginRequest("refresh@test.com", "P@ssword1234!", "device-fp");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        String newAccessToken = buildFakeJwt(USER_ID, "jti-2", Instant.now().plusSeconds(3600));
        AuthTokens refreshedTokens = new AuthTokens(newAccessToken, "new-refresh", 3600, "Bearer");
        when(identityProvider.refreshToken(refreshToken)).thenReturn(refreshedTokens);

        RefreshRequest refreshRequest = new RefreshRequest(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(newAccessToken))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout → 204 No Content")
    void logout_returns204() throws Exception {
        doNothing().when(identityProvider).revokeRefreshToken(anyString());
        doNothing().when(identityProvider).setUserAttribute(any(UUID.class), anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register — validation échoue sans email")
    void register_validation_missingEmail() throws Exception {
        String invalidBody = """
                {"phone":"+33612345678","password":"P@ssword1234!","firstName":"Jean","lastName":"Dupont","countryCode":"FR","acceptedTerms":true}
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    private static String buildFakeJwt(UUID sub, String jti, Instant exp) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"sub\":\"" + sub + "\",\"jti\":\"" + jti
                        + "\",\"exp\":" + exp.getEpochSecond()
                        + ",\"email\":\"user@test.com\"}").getBytes());
        return header + "." + payload + ".signature";
    }
}
