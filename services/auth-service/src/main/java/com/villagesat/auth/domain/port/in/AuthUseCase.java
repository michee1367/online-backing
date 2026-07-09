package com.villagesat.auth.domain.port.in;

import com.villagesat.auth.domain.model.AuthTokens;
import com.villagesat.auth.domain.model.LoginResult;
import com.villagesat.auth.domain.model.RegisteredUser;

import java.util.UUID;

public interface AuthUseCase {

    RegisteredUser register(RegisterCommand command);

    LoginResult login(LoginCommand command);

    AuthTokens refresh(RefreshCommand command);

    void logout(LogoutCommand command);

    record RegisterCommand(
            String email,
            String phone,
            String password,
            String firstName,
            String lastName,
            String countryCode,
            boolean acceptedTerms
    ) {}

    record LoginCommand(
            String email,
            String password,
            String deviceFingerprint,
            String ipAddress,
            String userAgent
    ) {}

    record RefreshCommand(String refreshToken, String ipAddress) {}

    record LogoutCommand(String accessToken, String refreshToken, UUID userId) {}
}
