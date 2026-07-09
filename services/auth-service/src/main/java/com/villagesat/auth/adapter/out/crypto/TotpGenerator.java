package com.villagesat.auth.adapter.out.crypto;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec; // <-- AJOUT de l'import
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Component
public class TotpGenerator {

    private static final int SECRET_BYTES = 20;

    private final TimeBasedOneTimePasswordGenerator totp;
    private final Base32 base32 = new Base32();
    private final SecureRandom secureRandom = new SecureRandom();

    public TotpGenerator() {
        try {
            this.totp = new TimeBasedOneTimePasswordGenerator(Duration.ofSeconds(30), 6);
        } catch (Exception e) { // <-- Ajusté ici selon l'exception Quarkus/Java moderne
            throw new IllegalStateException("Failed to init TOTP", e);
        }
    }

    public String generateSecret() {
        byte[] buffer = new byte[SECRET_BYTES];
        secureRandom.nextBytes(buffer);
        return base32.encodeToString(buffer).replace("=", "");
    }

    public boolean verify(String base32Secret, String code) {
        try {
            byte[] decoded = base32.decode(normalizeSecret(base32Secret));
            
            // CORRECTION : Conversion du byte[] en SecretKey reconnue par la bibliothèque
            SecretKeySpec secretKey = new SecretKeySpec(decoded, "RAW");
            
            int expected = totp.generateOneTimePassword(secretKey, Instant.now());
            int provided = Integer.parseInt(code.trim());
            return expected == provided;
        } catch (Exception e) {
            return false;
        }
    }

    public String buildQrUri(String issuer, String account, String secret) {
        return "otpauth://totp/"
                + urlEncode(issuer) + ":" + urlEncode(account)
                + "?secret=" + normalizeSecret(secret)
                + "&issuer=" + urlEncode(issuer)
                + "&algorithm=SHA1&digits=6&period=30";
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String normalizeSecret(String secret) {
        return secret.replace(" ", "").toUpperCase();
    }
}
