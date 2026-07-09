package com.villagesat.auth.domain.port.out;

import com.villagesat.auth.domain.model.RegisteredUser;
import com.villagesat.auth.domain.port.in.AuthUseCase;

public interface UserRegistrationEventPublisher {

    void publishUserRegistered(RegisteredUser user, AuthUseCase.RegisterCommand command);
}
