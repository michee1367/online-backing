package com.villagesat.user.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.user.domain.model.User;
import com.villagesat.user.domain.port.out.KeycloakSyncPort;
import com.villagesat.user.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.villagesat.user.support.UserTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycApprovedKafkaConsumerTest {

    @Mock
    UserRepository userRepository;

    @Mock
    KeycloakSyncPort keycloakSync;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    KycApprovedKafkaConsumer consumer;

    @Test
    void onKycEvent_approved_updatesKycLevel() {
        User user = activeUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        consumer.onKycEvent(kycApprovedEvent(2));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().kycLevel()).isEqualTo(2);

        verify(keycloakSync).updateKycLevel(USER_ID, 2);
    }

    @Test
    void onKycEvent_pendingUser_activatesOnKycApproval() {
        User user = pendingUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        consumer.onKycEvent(kycApprovedEvent(1));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(User.UserStatus.ACTIVE);
        assertThat(captor.getValue().kycLevel()).isEqualTo(1);

        verify(keycloakSync).updateStatus(USER_ID, "ACTIVE");
        verify(keycloakSync).updateKycLevel(USER_ID, 1);
    }

    @Test
    void onKycEvent_userNotFound_noSave() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        consumer.onKycEvent(kycApprovedEvent(1));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(keycloakSync);
    }

    @Test
    void onKycEvent_otherEventType_ignored() {
        String message = """
                {"eventType":"kyc.rejected","payload":{"userId":"%s","level":1}}
                """.formatted(USER_ID);

        consumer.onKycEvent(message);

        verifyNoInteractions(userRepository);
        verifyNoInteractions(keycloakSync);
    }

    @Test
    void onKycEvent_invalidJson_doesNotThrow() {
        consumer.onKycEvent("not-json");

        verifyNoInteractions(userRepository);
        verifyNoInteractions(keycloakSync);
    }
}
