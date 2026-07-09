package com.villagesat.wallet.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.wallet.domain.port.in.WalletKycUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static com.villagesat.wallet.support.WalletTestFixtures.USER_ID;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class KycApprovedWalletKafkaConsumerTest {

    @Mock
    WalletKycUseCase walletKycUseCase;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    KycApprovedWalletKafkaConsumer consumer;

    @Test
    void onKycEvent_approved_appliesLimits() {
        String message = """
                {
                  "eventType": "kyc.approved",
                  "payload": {
                    "userId": "%s",
                    "level": 2,
                    "submissionId": "sub-1",
                    "status": "APPROVED",
                    "riskScore": 0.98
                  }
                }
                """.formatted(USER_ID);

        consumer.onKycEvent(message);

        verify(walletKycUseCase).applyKycLimits(USER_ID, 2);
    }

    @Test
    void onKycEvent_otherEventType_ignored() {
        consumer.onKycEvent("{\"eventType\":\"kyc.rejected\",\"payload\":{}}");

        verifyNoInteractions(walletKycUseCase);
    }

    @Test
    void onKycEvent_invalidJson_doesNotThrow() {
        consumer.onKycEvent("not-json");

        verifyNoInteractions(walletKycUseCase);
    }
}
