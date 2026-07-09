# Architecture globale — VillageSat Fintech

## 1. Vue d'ensemble

VillageSat adopte une **architecture microservices event-driven** avec séparation stricte des domaines métier (Domain-Driven Design). Chaque service possède sa propre base de données (pattern Database-per-Service) avec synchronisation via événements Kafka.

### Diagramme haut niveau

```mermaid
flowchart TB
    subgraph clients [Clients]
        MOB[App Mobile]
        WEB[Web App]
        MER[Marchands / POS]
        PART[Partenaires API]
    end

    subgraph edge [Edge Layer]
        NGINX[NGINX TLS 1.3]
        KONG[Kong API Gateway]
        WAF[WAF / DDoS Shield]
    end

    subgraph auth [Identity]
        KC[Keycloak OIDC]
    end

    subgraph core [Core Services]
        USR[User Service]
        WAL[Wallet Service]
        TXN[Transaction Service]
        PAY[Payment Service]
        NTF[Notification Service]
    end

    subgraph compliance_layer [Compliance & Risk]
        KYC[Compliance/KYC]
        FRD[Fraud Detection]
        AUD[Audit Service]
    end

    subgraph integration [Integrations]
        MM[Mobile Money]
        BNK[Banking Integration]
    end

    subgraph support [Support]
        ADM[Admin Service]
        RPT[Reporting Service]
    end

    subgraph infra [Infrastructure]
        PG[(PostgreSQL Cluster)]
        RD[(Redis Cluster)]
        KF{{Apache Kafka}}
        VLT[HashiCorp Vault]
        S3[MinIO / S3]
        ELK[ELK Stack]
        PROM[Prometheus + Grafana]
    end

    MOB & WEB & MER & PART --> WAF --> NGINX --> KONG
    KONG --> KC
    KONG --> USR & WAL & TXN & PAY & NTF & KYC & ADM & RPT
    USR & WAL & TXN & PAY --> KF
    KYC & FRD & AUD --> KF
    TXN & PAY --> MM & BNK
    core --> PG & RD
    compliance_layer --> PG
    core & compliance_layer --> VLT
    RPT --> S3
    core --> ELK & PROM
```

## 2. Architecture hexagonale (par service)

Chaque microservice suit le pattern **Ports & Adapters** :

```
services/wallet-service/
├── domain/           # Entités, value objects, règles métier pures
│   ├── model/
│   ├── port/in/      # Use cases (interfaces)
│   └── port/out/     # Repositories, messaging (interfaces)
├── application/      # Services applicatifs, orchestration
│   └── service/
├── adapter/
│   ├── in/web/       # REST controllers, WebSocket
│   ├── in/messaging/ # Kafka consumers
│   └── out/
│       ├── persistence/  # JPA repositories
│       ├── messaging/    # Kafka producers
│       └── external/     # Clients HTTP partenaires
└── config/           # Spring configuration, sécurité
```

**Règle d'or** : le domaine ne dépend d'aucun framework. Les adapters implémentent les ports.

## 3. Diagramme des microservices et communication

```mermaid
flowchart LR
    subgraph sync [Sync REST - gRPC interne]
        GW[API Gateway] -->|REST| SVC[Services]
    end

    subgraph async [Async Kafka Topics]
        T1[user.created]
        T2[kyc.approved]
        T3[wallet.credited]
        T4[transaction.completed]
        T5[fraud.alert]
        T6[audit.event]
        T7[notification.request]
    end

    USR2[User] -->|publish| T1
    T1 -->|subscribe| WAL2[Wallet]
    KYC2[Compliance] -->|publish| T2
    T2 -->|subscribe| WAL2
    TXN2[Transaction] -->|publish| T3 & T4
    T4 -->|subscribe| NTF2[Notification]
    T4 -->|subscribe| AUD2[Audit]
    FRD2[Fraud] -->|publish| T5
    T5 -->|subscribe| ADM2[Admin]
```

### Topics Kafka principaux

| Topic | Producteur | Consommateurs | Rétention |
|-------|-----------|---------------|-----------|
| `user.events` | user-service | wallet, compliance, audit | 90j |
| `kyc.events` | compliance-service | wallet, transaction, admin | 7 ans |
| `wallet.events` | wallet-service | transaction, reporting, audit | 7 ans |
| `transaction.events` | transaction-service | payment, fraud, notification, audit | 7 ans |
| `payment.events` | payment-service | transaction, reporting | 7 ans |
| `fraud.alerts` | fraud-service | admin, compliance, notification | 7 ans |
| `audit.events` | all services | audit-service | 10 ans |

## 4. Flux d'authentification (OAuth2 + OIDC + MFA)

```mermaid
sequenceDiagram
    participant C as Client App
    participant GW as API Gateway
    participant KC as Keycloak
    participant AUTH as Auth Service
    participant USR as User Service

    C->>GW: POST /auth/login (email, password)
    GW->>KC: Resource Owner Password / Auth Code
    KC->>KC: Validate credentials + anti-bruteforce
    KC-->>GW: Access Token (JWT) + Refresh Token
    GW-->>C: Tokens (HttpOnly cookie option)

    Note over C,KC: MFA requis pour opérations sensibles
    C->>GW: POST /auth/mfa/verify (TOTP code)
    GW->>AUTH: Verify TOTP via Vault-stored secret
    AUTH-->>GW: MFA claim added to session
    GW-->>C: Elevated token (scope: transactions:write)

    C->>GW: GET /api/v1/wallets (Bearer JWT)
    GW->>GW: Validate JWT signature, expiry, rate limit
    GW->>GW: Extract roles (RBAC)
    GW->>USR: Forward with X-User-Id, X-Roles headers
    USR-->>C: 200 OK
```

### RBAC — Rôles et permissions

| Rôle | Permissions |
|------|-------------|
| `CUSTOMER` | wallet:read, transaction:read, transfer:own |
| `MERCHANT` | + payment:accept, qr:generate |
| `AGENT` | + cash:in/out (mobile money agent) |
| `COMPLIANCE_OFFICER` | kyc:review, aml:investigate |
| `FRAUD_ANALYST` | fraud:review, account:freeze |
| `ADMIN` | * (scoped par tenant) |
| `SUPER_ADMIN` | system:* |

Les permissions sont stockées dans Keycloak et propagées via JWT claims (`realm_access.roles`, `resource_access`).

## 5. Flux de transaction (transfert P2P)

```mermaid
sequenceDiagram
    participant C as Client
    participant GW as Gateway
    participant TXN as Transaction Service
    participant FRD as Fraud Service
    participant WAL as Wallet Service
    participant KFK as Kafka
    participant NTF as Notification
    participant AUD as Audit

    C->>GW: POST /transactions/transfer<br/>Idempotency-Key: uuid
    GW->>GW: JWT + RBAC + rate limit
    GW->>TXN: Forward request

    TXN->>TXN: Validate idempotency key (Redis)
    TXN->>FRD: POST /fraud/score (sync, timeout 200ms)
    FRD-->>TXN: score=12, action=ALLOW

    TXN->>WAL: POST /internal/debit (mTLS)
    WAL->>WAL: SELECT FOR UPDATE (pessimistic lock)
    WAL->>WAL: Check balance + limits
    WAL-->>TXN: Debit OK, new balance

    TXN->>WAL: POST /internal/credit
    WAL-->>TXN: Credit OK

    TXN->>TXN: Persist transaction (ACID)
    TXN->>KFK: Publish transaction.completed
    KFK->>NTF: Send push notification
    KFK->>AUD: Immutable audit log

    TXN-->>C: 201 Created {transactionId, status: COMPLETED}
```

### Garanties transactionnelles

- **Saga orchestrée** pour transferts cross-service avec compensation
- **Outbox pattern** : événements Kafka écrits dans la même transaction DB
- **Idempotency-Key** : header obligatoire, TTL Redis 24h
- **Double-entry ledger** : chaque mouvement = débit + crédit
- **Signature HMAC-SHA256** des transactions > seuil configurable

## 6. Zero Trust Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Service Mesh (Istio)                  │
│  ┌─────────┐  mTLS   ┌─────────┐  mTLS   ┌─────────┐   │
│  │ Service │◄──────►│ Service │◄──────►│ Service │   │
│  │    A    │         │    B    │         │    C    │   │
│  └─────────┘         └─────────┘         └─────────┘   │
│       ▲ Policy enforcement (OPA)                        │
└───────┼─────────────────────────────────────────────────┘
        │ JWT + mTLS
   ┌────┴────┐
   │ Gateway │
   └─────────┘
```

- **mTLS** entre tous les services (certificats Vault PKI)
- **Network Policies** Kubernetes : deny-all par défaut
- **OPA/Gatekeeper** : policies admission (no privileged containers)
- **Secrets** : jamais en env vars, toujours via Vault Agent Injector

## 7. Haute disponibilité

| Composant | Stratégie | SLA cible |
|-----------|-----------|-----------|
| API Gateway | 3+ replicas, HPA | 99.99% |
| Services core | 3+ replicas par AZ | 99.95% |
| PostgreSQL | Patroni HA, sync replica | 99.99% |
| Redis | Sentinel / Cluster mode | 99.9% |
| Kafka | 3 brokers, RF=3, min ISR=2 | 99.95% |
| Keycloak | 2+ replicas, sticky sessions | 99.9% |

**RPO** : 0 (sync replication) | **RTO** : < 15 minutes (failover automatique)

## 8. Multi-région Afrique

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  af-south-1  │     │  eu-west-1   │     │  us-east-1   │
│  (Cape Town) │     │  (Dublin)    │     │  (Virginia)  │
│  PRIMARY CD  │     │  DR + EU     │     │  DR Global   │
└──────┬───────┘     └──────┬───────┘     └──────┬───────┘
       │                    │                    │
       └────────────────────┼────────────────────┘
                    Global Load Balancer
                    (GeoDNS + health checks)
```

Données utilisateurs africains hébergées en **af-south-1** (conformité souveraineté des données).

## 9. Références internes

- [Sécurité](security.md)
- [API Reference](api-reference.md)
- [Base de données](database.md)
- [DevSecOps](devsecops.md)
