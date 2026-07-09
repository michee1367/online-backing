package com.villagesat.auth.domain.port.in;

import com.villagesat.auth.domain.model.AuthTokens;
import com.villagesat.auth.domain.model.MfaSetup;

import java.util.UUID;

public interface MfaUseCase {

    MfaSetup initiateSetup(UUID userId, String email);

    void confirmSetup(UUID userId, String code);

    AuthTokens verifyLoginMfa(VerifyMfaCommand command);

    boolean isMfaEnabled(UUID userId);

    record VerifyMfaCommand(UUID sessionId, String method, String code, String refreshToken) {}
}
