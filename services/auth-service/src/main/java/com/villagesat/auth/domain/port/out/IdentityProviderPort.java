package com.villagesat.auth.domain.port.out;

import com.villagesat.auth.domain.model.AuthTokens;
import com.villagesat.auth.domain.port.in.AuthUseCase;

import java.util.Map;
import java.util.UUID;

public interface IdentityProviderPort {

    UUID registerUser(AuthUseCase.RegisterCommand command);

    AuthTokens authenticate(String email, String password);

    AuthTokens refreshToken(String refreshToken);

    void revokeRefreshToken(String refreshToken);

    void setUserAttribute(UUID userId, String attribute, String value);

    void assignRealmRole(UUID userId, String roleName);

    UUID findUserIdByEmail(String email);
}
