package com.villagesat.auth.domain.model;

import java.util.List;
import java.util.UUID;

public record LoginResult(
        AuthTokens tokens,
        boolean mfaRequired,
        List<String> mfaMethods,
        UUID sessionId
) {}
