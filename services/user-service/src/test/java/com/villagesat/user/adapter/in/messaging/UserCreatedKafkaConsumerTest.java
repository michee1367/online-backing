package com.villagesat.user.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.user.domain.port.in.UserUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.villagesat.user.support.UserTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserCreatedKafkaConsumerTest {

    @Mock
    UserUseCase userUseCase;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    UserCreatedKafkaConsumer consumer;

    @Test
    void onUserEvent_userCreated_provisionsUser() {
        consumer.onUserEvent(userCreatedEvent());

        ArgumentCaptor<UserUseCase.ProvisionUserCommand> captor =
                ArgumentCaptor.forClass(UserUseCase.ProvisionUserCommand.class);
        verify(userUseCase).provisionFromRegistration(captor.capture());

        UserUseCase.ProvisionUserCommand cmd = captor.getValue();
        assertThat(cmd.userId()).isEqualTo(USER_ID);
        assertThat(cmd.email()).isEqualTo("alice@example.com");
        assertThat(cmd.firstName()).isEqualTo("Alice");
        assertThat(cmd.lastName()).isEqualTo("Mutombo");
        assertThat(cmd.countryCode()).isEqualTo("CD");
        assertThat(cmd.kycLevel()).isZero();
        assertThat(cmd.status()).isEqualTo("PENDING_VERIFICATION");
    }

    @Test
    void onUserEvent_otherEventType_ignored() {
        String message = """
                {
                  "eventType": "user.updated",
                  "payload": {"userId": "%s"}
                }
                """.formatted(USER_ID);

        consumer.onUserEvent(message);

        verifyNoInteractions(userUseCase);
    }

    @Test
    void onUserEvent_invalidJson_doesNotThrow() {
        consumer.onUserEvent("not-json");

        verifyNoInteractions(userUseCase);
    }

    @Test
    void onUserEvent_missingPayloadFields_usesDefaults() {
        String message = """
                {
                  "eventType": "user.created",
                  "payload": {
                    "userId": "%s",
                    "email": "test@example.com",
                    "firstName": "Test",
                    "lastName": "User"
                  }
                }
                """.formatted(USER_ID);

        consumer.onUserEvent(message);

        ArgumentCaptor<UserUseCase.ProvisionUserCommand> captor =
                ArgumentCaptor.forClass(UserUseCase.ProvisionUserCommand.class);
        verify(userUseCase).provisionFromRegistration(captor.capture());

        UserUseCase.ProvisionUserCommand cmd = captor.getValue();
        assertThat(cmd.countryCode()).isEqualTo("CD");
        assertThat(cmd.kycLevel()).isZero();
        assertThat(cmd.status()).isEqualTo("PENDING_VERIFICATION");
    }
}
