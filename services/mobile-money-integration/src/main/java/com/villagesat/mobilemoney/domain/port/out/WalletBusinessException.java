package com.villagesat.mobilemoney.domain.port.out;

public class WalletBusinessException extends RuntimeException {
    public WalletBusinessException(String message) { super(message); }
}
