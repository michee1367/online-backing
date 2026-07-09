package com.villagesat.banking.domain.port.out;

import com.villagesat.banking.domain.model.BankTransfer;

public interface BankGatewayPort {

    TransferResponse initiateSwiftTransfer(BankTransfer transfer);

    TransferStatusResponse checkTransferStatus(String externalRef);

    record TransferResponse(
            boolean success,
            String externalRef,
            String errorMessage
    ) {
    }

    record TransferStatusResponse(
            String status,
            String externalRef
    ) {
    }
}
