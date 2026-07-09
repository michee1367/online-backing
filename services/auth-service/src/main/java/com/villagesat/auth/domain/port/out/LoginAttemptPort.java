package com.villagesat.auth.domain.port.out;

public interface LoginAttemptPort {

    void recordSuccess(String email);

    void recordFailure(String email, String reason);

    boolean isLocked(String email);

    long remainingLockSeconds(String email);
}
