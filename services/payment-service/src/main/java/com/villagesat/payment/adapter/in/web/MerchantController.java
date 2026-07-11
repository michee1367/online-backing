package com.villagesat.payment.adapter.in.web;

import com.villagesat.common.security.SecurityUtils;
//import com.villagesat.payment.adapter.in.web.MerchantController.RegisterMerchantRequest.MerchantResponse;
import com.villagesat.payment.domain.model.Merchant;
import com.villagesat.payment.domain.port.in.MerchantUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments/merchants")
@Tag(name = "Merchants", description = "Gestion des marchands")
public class MerchantController {

    private final MerchantUseCase merchantUseCase;

    public MerchantController(MerchantUseCase merchantUseCase) {
        this.merchantUseCase = merchantUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "S'inscrire comme marchand")
    public ResponseEntity<MerchantResponse> register(@Valid @RequestBody RegisterMerchantRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Merchant merchant = merchantUseCase.registerMerchant(new MerchantUseCase.RegisterMerchantCommand(
                userId,
                request.businessName(),
                request.businessType(),
                request.contactEmail(),
                request.contactPhone(),
                request.callbackUrl(),
                request.listProductsUrl()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(MerchantResponse.from(merchant));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Détails de son marchand")
    public ResponseEntity<List<MerchantResponse>> getMyMerchants() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<MerchantResponse> merchants = merchantUseCase.listByUserId(userId).stream()
                .map(MerchantResponse::from)
                .toList();
        return ResponseEntity.ok(merchants);
    }

    public record RegisterMerchantRequest(
            @NotBlank @Size(max = 200) String businessName,
            @Size(max = 50) String businessType,
            @Email @Size(max = 255) String contactEmail,
            @Size(max = 20) String contactPhone,
            @Size(max = 500) String callbackUrl,            
            @Size(max = 500) String listProductsUrl

    ) {}

    public record MerchantResponse(
            UUID merchantId,
            String merchantCode,
            String businessName,
            String businessType,
            String status,
            String contactEmail,
            String contactPhone,
            String callbackUrl,
            String listProductsList,
            String commissionRate
    ) {
        static MerchantResponse from(Merchant m) {
            return new MerchantResponse(
                    m.id(),
                    m.merchantCode(),
                    m.businessName(),
                    m.businessType(),
                    m.status().name(),
                    m.contactEmail(),
                    m.contactPhone(),
                    m.callbackUrl(),
                    m.listProductsUrl(),
                    m.commissionRate().toPlainString()
            );
        }
    }
}
