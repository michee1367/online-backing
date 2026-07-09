# Tests — VillageSat Backend

## Tests unitaires & intégration (Maven)

```bash
# Tout le monorepo (CI)
mvn verify

# Un service spécifique
mvn -pl services/wallet-service -am test
mvn -pl services/transaction-service -am test
mvn -pl services/auth-service -am test
mvn -pl services/user-service -am test
mvn -pl services/compliance-service -am test

# Un test précis
mvn -pl services/wallet-service test -Dtest=WalletKycIntegrationTest
mvn -pl services/transaction-service test -Dtest=TransferIntegrationTest
```

**Prérequis intégration** : Docker (Testcontainers PostgreSQL + Redis).

## Couverture par service

### wallet-service

| Type | Classes |
|------|---------|
| Unitaire | `WalletKycLimitsTest`, `WalletKycLevelTest`, `TransactionLimitServiceTest`, `WalletKycLimitServiceTest`, `WalletServiceTest`, `KycApprovedWalletKafkaConsumerTest` |
| Intégration | `WalletKycIntegrationTest`, `KycKafkaConsumerIntegrationTest` |

### transaction-service

| Type | Classes |
|------|---------|
| Unitaire | `FeeCalculatorTest`, `TransactionTest`, `TransferServiceTest` |
| Intégration | `TransferIntegrationTest` (PostgreSQL + Redis Testcontainers, wallet/fraud mockés) |

### auth-service

| Type | Classes |
|------|---------|
| Unitaire | `AuthServiceTest`, `MfaServiceTest`, `JwtClaimsParserTest` |
| Intégration | `AuthIntegrationTest` (MockMvc + Testcontainers) |

### user-service

| Type | Classes |
|------|---------|
| Unitaire | `UserServiceTest`, `UserCreatedKafkaConsumerTest`, `KycApprovedKafkaConsumerTest` |
| Intégration | `UserIntegrationTest` |

### compliance-service

| Type | Classes |
|------|---------|
| Unitaire | `KycServiceTest`, `KycReviewServiceTest`, `KycLimitsTest` |
| Intégration | `ComplianceIntegrationTest` |

### fraud-service

| Type | Classes |
|------|---------|
| Unitaire | `FraudScoringServiceTest`, `RuleEngineTest` |

## Tests de charge (k6)

Voir [load/README.md](load/README.md).
