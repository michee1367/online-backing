package com.villagesat.mobilemoney.domain.model;

import java.time.Instant;

public class ProviderConfig {

    private MobileMoneyProvider provider;
    private String apiUrl;
    private String apiKeyEncrypted;
    private String merchantId;
    private String callbackUrl;
    private boolean active;
    private Instant updatedAt;

    public ProviderConfig() {
    }

    public MobileMoneyProvider getProvider() {
        return provider;
    }

    public void setProvider(MobileMoneyProvider provider) {
        this.provider = provider;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiKeyEncrypted() {
        return apiKeyEncrypted;
    }

    public void setApiKeyEncrypted(String apiKeyEncrypted) {
        this.apiKeyEncrypted = apiKeyEncrypted;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
