package com.villagesat.admin.adapter.in.web;

import com.villagesat.admin.domain.model.AccountAction;
import com.villagesat.admin.domain.model.BlacklistEntry;
import com.villagesat.admin.domain.model.SystemConfig;
import com.villagesat.admin.domain.port.in.AdminUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminUseCase adminUseCase;

    public AdminController(AdminUseCase adminUseCase) {
        this.adminUseCase = adminUseCase;
    }

    @PostMapping("/actions")
    public ResponseEntity<AccountAction> performAction(@Valid @RequestBody AccountAction action) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUseCase.performAccountAction(action));
    }

    @GetMapping("/actions")
    public ResponseEntity<List<AccountAction>> getActions(@RequestParam UUID userId) {
        return ResponseEntity.ok(adminUseCase.getAccountActions(userId));
    }

    @PostMapping("/blacklist")
    public ResponseEntity<BlacklistEntry> addToBlacklist(@Valid @RequestBody BlacklistEntry entry) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUseCase.addToBlacklist(entry));
    }

    @DeleteMapping("/blacklist/{id}")
    public ResponseEntity<Void> removeFromBlacklist(@PathVariable UUID id) {
        adminUseCase.removeFromBlacklist(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/blacklist")
    public ResponseEntity<List<BlacklistEntry>> getBlacklist() {
        return ResponseEntity.ok(adminUseCase.getBlacklist());
    }

    @GetMapping("/config")
    public ResponseEntity<List<SystemConfig>> getConfig() {
        return ResponseEntity.ok(adminUseCase.getSystemConfig());
    }

    @PutMapping("/config/{key}")
    public ResponseEntity<SystemConfig> updateConfig(@PathVariable String key, @RequestBody SystemConfig config) {
        return ResponseEntity.ok(adminUseCase.updateSystemConfig(key, config.getValue(), config.getUpdatedBy()));
    }
}
