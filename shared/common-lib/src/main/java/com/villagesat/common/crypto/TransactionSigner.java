package com.villagesat.common.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Signature HMAC-SHA256 pour transactions sensibles.
 * Protection anti-replay via timestamp + nonce inclus dans le message signé.
 */
public final class TransactionSigner {

    private static final String HMAC_ALGO = "HmacSHA256";

    private TransactionSigner() {}

    public static String sign(String payload, String timestamp, String nonce, byte[] secretKey) {
        String message = payload + "|" + timestamp + "|" + nonce;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secretKey, HMAC_ALGO));
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new SignatureException("Failed to sign transaction", e);
        }
    }

    public static boolean verify(String payload, String timestamp, String nonce,
                                  String signature, byte[] secretKey) {
        String expected = sign(payload, timestamp, nonce, secretKey);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    public static class SignatureException extends RuntimeException {
        public SignatureException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
