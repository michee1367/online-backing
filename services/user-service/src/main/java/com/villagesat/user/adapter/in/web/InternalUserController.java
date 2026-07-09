package com.villagesat.user.adapter.in.web;

import com.villagesat.user.domain.model.UserWithProfile;
import com.villagesat.user.domain.port.in.UserUseCase;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * API interne pour les autres microservices (mTLS + X-Internal-Service-Token).
 */
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserUseCase userUseCase;

    public InternalUserController(UserUseCase userUseCase) {
        this.userUseCase = userUseCase;
    }

    @GetMapping("/{userId}")
    public UserController.UserProfileResponse getUser(@PathVariable UUID userId) {
        UserWithProfile data = userUseCase.getCurrentUser(userId);
        return UserController.UserProfileResponse.from(data);
    }

    @PostMapping("/provision")
    public void provision(@RequestBody ProvisionRequest request) {
        userUseCase.provisionFromRegistration(new UserUseCase.ProvisionUserCommand(
                request.userId(), request.email(), request.phone(),
                request.firstName(), request.lastName(), request.countryCode(),
                request.kycLevel(), request.status()));
    }

    public record ProvisionRequest(
            UUID userId,
            String email,
            String phone,
            String firstName,
            String lastName,
            String countryCode,
            int kycLevel,
            String status
    ) {}
}
