# Structure du projet VillageSat Backend

```
villagesat-backend/
├── README.md
├── pom.xml                          # Parent Maven multi-module
├── docker-compose.yml               # Stack locale (PG, Redis, Kafka, Keycloak, Vault...)
│
├── docs/                            # Documentation technique
│   ├── architecture.md              # Architecture globale + diagrammes Mermaid
│   ├── security.md                  # Stratégie Zero Trust, chiffrement, RBAC
│   ├── compliance.md                # PCI-DSS, RGPD, AML, KYC
│   ├── api-reference.md             # Endpoints REST documentés
│   ├── database.md                  # Stratégie DB, partitionnement
│   ├── devsecops.md                 # CI/CD, monitoring, DR
│   ├── scaling-plan.md              # Capacity planning
│   ├── production-plan.md           # Go-live cloud Afrique
│   └── openapi/
│       └── villagesat-api.yaml      # Spec OpenAPI 3.1
│
├── database/
│   └── schema/
│       └── V1__init_schema.sql      # Schéma PostgreSQL complet
│
├── shared/
│   └── common-lib/                  # Lib partagée (security, idempotency, crypto)
│       └── src/main/java/com/villagesat/common/
│           ├── crypto/              # TransactionSigner HMAC-SHA256
│           ├── error/               # ApiError standard
│           ├── idempotency/         # IdempotencyService (Redis)
│           └── security/            # SecurityUtils JWT
│
├── services/                        # Microservices (hexagonal architecture)
│   ├── api-gateway/                 # Kong + Spring Cloud Gateway
│   ├── auth-service/                # OAuth2, MFA, sessions
│   ├── user-service/                # Profils, préférences
│   ├── wallet-service/              # ★ Wallets, ledger, balances
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src/main/java/com/villagesat/wallet/
│   │       ├── domain/              # Entités pures (Wallet, Balance)
│   │       │   ├── model/
│   │       │   └── port/
│   │       │       ├── in/          # Use cases
│   │       │       └── out/         # Repository interfaces
│   │       ├── application/         # Services applicatifs
│   │       │   └── service/
│   │       ├── adapter/
│   │       │   ├── in/web/          # REST controllers
│   │       │   ├── in/messaging/    # Kafka consumers
│   │       │   └── out/
│   │       │       ├── persistence/ # JPA
│   │       │       └── messaging/   # Kafka producers
│   │       └── config/
│   │
│   ├── transaction-service/         # ★ Transferts, idempotence, saga
│   ├── payment-service/             # Paiements marchands, QR Code
│   ├── notification-service/        # SMS, email, push, WebSocket
│   ├── compliance-service/          # KYC/KYB, AML screening
│   ├── fraud-service/               # Scoring fraude temps réel
│   ├── audit-service/               # Audit trail immuable
│   ├── reporting-service/           # Relevés PDF, rapports
│   ├── admin-service/               # Back-office, blacklist
│   ├── mobile-money-integration/    # M-Pesa, Orange Money, MTN
│   └── banking-integration/         # SWIFT, ACH, banques
│
├── infrastructure/
│   ├── docker/
│   │   └── prometheus/
│   ├── kubernetes/
│   │   ├── wallet-service.yaml      # Deployment, HPA, NetworkPolicy
│   │   └── helm/                    # Charts Helm par service
│   └── terraform/
│       ├── modules/
│       │   ├── vpc/
│       │   ├── eks/
│       │   ├── rds/
│       │   ├── elasticache/
│       │   └── msk/
│       └── environments/
│           ├── dev/
│           ├── staging/
│           ├── prod-af/             # Afrique du Sud (primary)
│           └── prod-eu/             # Dublin (DR + EU)
│
└── .github/
    └── workflows/
        └── ci-cd.yml                # Lint, SAST, test, Trivy, Cosign, deploy
```

## Pattern hexagonal par service

Chaque microservice suit la même structure :

| Couche | Responsabilité | Dépendances |
|--------|---------------|-------------|
| `domain/model` | Entités, value objects, règles métier | Aucune |
| `domain/port/in` | Interfaces use cases | domain only |
| `domain/port/out` | Interfaces repositories, events | domain only |
| `application/service` | Orchestration, transactions | domain ports |
| `adapter/in/web` | REST, validation, sécurité | application |
| `adapter/out/persistence` | JPA, Redis | application |
| `adapter/out/messaging` | Kafka producers/consumers | application |
| `config` | Spring Boot, OAuth2, OpenTelemetry | adapters |

## Conventions de nommage

- Packages : `com.villagesat.{service}.{layer}`
- Events Kafka : `{domain}.{action}` (ex: `wallet.created`)
- Tables DB : `{schema}.{entity}` (ex: `wallets.ledger_entries`)
- API paths : `/api/v1/{resource}`
- Internal API : `/internal/{resource}` (mTLS only)
