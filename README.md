# VillageSat — Backend Fintech Enterprise

Plateforme backend fintech cloud-native, conçue pour la banque digitale, mobile money, microfinance, paiements et super-app financière en Afrique et à l'international.

## Stack technique

| Couche | Technologie |
|--------|-------------|
| Backend | Java 21 + Spring Boot 3.4 (architecture hexagonale) |
| Auth | Keycloak + OAuth2/OIDC + JWT |
| Base de données | PostgreSQL 16 (ACID, partitionnement) |
| Cache | Redis 7 |
| Messaging | Apache Kafka |
| API Gateway | Kong + Spring Cloud Gateway |
| Reverse Proxy | NGINX (TLS 1.3) |
| Containers | Docker + Kubernetes |
| IaC | Terraform |
| Monitoring | Prometheus + Grafana |
| Logs | ELK Stack |
| Secrets | HashiCorp Vault |
| Object Storage | MinIO / AWS S3 |
| CI/CD | GitHub Actions |
| Sécurité deps | Trivy + Snyk |

## Microservices

| Service | Port | Responsabilité |
|---------|------|----------------|
| `api-gateway` | 8080 | Routage, rate limiting, JWT validation |
| `auth-service` | 8081 | OAuth2, MFA, sessions, RBAC |
| `user-service` | 8082 | Profils, KYC niveau 1, préférences |
| `wallet-service` | 8083 | Wallets multi-devises, soldes |
| `transaction-service` | 8084 | Transferts, idempotence, ledger |
| `payment-service` | 8085 | Paiements marchands, QR Code |
| `notification-service` | 8086 | SMS, email, push (WebSocket) |
| `compliance-service` | 8087 | KYC/KYB, AML, scoring risque |
| `fraud-service` | 8088 | Détection fraude temps réel |
| `audit-service` | 8089 | Audit trail immuable |
| `reporting-service` | 8090 | Relevés PDF, rapports réglementaires |
| `admin-service` | 8091 | Back-office, gel compte, blacklist |
| `mobile-money-integration` | 8092 | M-Pesa, Orange Money, MTN MoMo |
| `banking-integration` | 8093 | SWIFT, ACH, banques partenaires |

## Démarrage rapide (local)

```bash
# Prérequis : Docker, Java 21, Maven 3.9+
docker compose up -d

# Compiler tous les modules
mvn compile -B

# Wallet service (profil dev = auth mock + pas de Keycloak requis)
cd services/wallet-service && mvn spring-boot:run

# User service
cd services/user-service && mvn spring-boot:run

# Compliance service (KYC)
cd services/compliance-service && mvn spring-boot:run

# Transaction service (autre terminal)
cd services/transaction-service && mvn spring-boot:run

# Fraud service (scoring)
cd services/fraud-service && mvn spring-boot:run
```

### Profils Spring

| Profil | Usage |
|--------|-------|
| `dev` (défaut) | Auth mock, MFA simulé, token interne `dev-internal-token` |
| `prod` | Keycloak JWT, mTLS Istio, Vault secrets |

### Tests de charge k6

```bash
k6 run tests/load/k6-transfer-load.js
```

Voir [`tests/load/README.md`](tests/load/README.md).

## Documentation

| Document | Description |
|----------|-------------|
| [Architecture globale](docs/architecture.md) | Vue d'ensemble, diagrammes, flux |
| [Sécurité](docs/security.md) | Zero Trust, chiffrement, RBAC, MFA |
| [Conformité](docs/compliance.md) | PCI-DSS, RGPD, AML, KYC |
| [API Reference](docs/api-reference.md) | Endpoints REST complets |
| [Base de données](docs/database.md) | Schéma, index, partitionnement |
| [DevSecOps](docs/devsecops.md) | CI/CD, monitoring, DR |
| [Montée en charge](docs/scaling-plan.md) | Capacity planning millions d'users |
| [Production cloud](docs/production-plan.md) | Déploiement multi-région Afrique |

## Principes architecturaux

1. **Sécurité d'abord** — Chaque endpoint est authentifié, autorisé, audité
2. **Idempotence** — Toute opération financière est idempotente via `Idempotency-Key`
3. **Immutabilité du ledger** — Append-only, double-entry bookkeeping
4. **Event-driven** — Kafka pour découplage et résilience
5. **Zero Trust** — mTLS inter-services, pas de confiance réseau implicite
6. **Conformité by design** — Audit trail, rétention, pseudonymisation RGPD

## Licence

Propriétaire — VillageSat © 2026
# villages-backend
