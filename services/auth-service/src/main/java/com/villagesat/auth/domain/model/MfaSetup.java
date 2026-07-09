package com.villagesat.auth.domain.model;

import java.util.List;

public record MfaSetup(
        String secret,
        String qrCodeUri,
        List<String> backupCodes
) {}
