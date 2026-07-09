package com.villagesat.mobilemoney.adapter.in.web.dto;

import com.villagesat.mobilemoney.domain.model.ProviderConfig;

public record ProviderResponse(
        String provider,
        String apiUrl,
        boolean active
) {
    public static ProviderResponse from(ProviderConfig config) {
        return new ProviderResponse(config.getProvider().name(), config.getApiUrl(), config.isActive());
    }
}
