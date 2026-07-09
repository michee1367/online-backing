package com.villagesat.common.error;

import java.time.Instant;
import java.util.UUID;

public record ApiError(
        String code,
        String message,
        UUID requestId,
        Instant timestamp
) {}
