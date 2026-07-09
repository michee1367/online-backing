package com.villagesat.common.config;

import com.villagesat.common.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("securityUtils")
public class SecurityExpressionService {

    @Value("${villagesat.security.transfer-mfa-required:true}")
    private boolean transferMfaRequired;

    public boolean isMfaVerified() {
        try {
            return SecurityUtils.isMfaVerified();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /** MFA pour transferts P2P — désactivable via villagesat.security.transfer-mfa-required */
    public boolean isTransferMfaVerified() {
        if (!transferMfaRequired) {
            return true;
        }
        return isMfaVerified();
    }
}
