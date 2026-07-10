package com.villagesat.payment.domain.port.out;

public class WalletBusinessException extends RuntimeException {
    public WalletBusinessException(String message) { super(message); }
}
