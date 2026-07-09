package com.villagesat.mobilemoney.adapter.in.web;

import com.villagesat.mobilemoney.adapter.in.web.dto.CallbackRequest;
import com.villagesat.mobilemoney.adapter.in.web.dto.TransactionResponse;
import com.villagesat.mobilemoney.domain.port.in.CallbackCommand;
import com.villagesat.mobilemoney.domain.port.in.MobileMoneyUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/mobile-money")
public class MobileMoneyCallbackController {

    private final MobileMoneyUseCase mobileMoneyUseCase;

    public MobileMoneyCallbackController(MobileMoneyUseCase mobileMoneyUseCase) {
        this.mobileMoneyUseCase = mobileMoneyUseCase;
    }

    @PostMapping("/callback")
    public ResponseEntity<TransactionResponse> handleCallback(@Valid @RequestBody CallbackRequest request) {
        var command = new CallbackCommand(
                request.externalRef(), request.providerRef(),
                request.status(), request.failedReason()
        );
        var tx = mobileMoneyUseCase.handleCallback(command);
        return ResponseEntity.ok(TransactionResponse.from(tx));
    }
}
