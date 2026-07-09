# Microservices VillageSat

Chaque service est un module Maven indépendant avec architecture hexagonale.

| Service | Port | Statut | Description |
|---------|------|--------|-------------|
| [api-gateway](api-gateway/) | 8080 | **Implémenté** | Spring Cloud Gateway, rate limiting Redis, circuit breaker, JWT |
| [auth-service](auth-service/) | 8081 | **Implémenté** | Keycloak, MFA TOTP, sessions, anti-bruteforce |
| [user-service](user-service/) | 8082 | **Implémenté** | Profils, sync Keycloak, Kafka user.created / kyc.approved |
| [wallet-service](wallet-service/) | 8083 | **Implémenté** | Wallets, ledger, plafonds KYC, Kafka kyc.approved |
| [transaction-service](transaction-service/) | 8084 | **Implémenté** | Transferts P2P, idempotence Redis, fraud check |
| [notification-service](notification-service/) | 8085 | **Implémenté** | SMS, email, push, templates, Kafka consumers |
| [payment-service](payment-service/) | 8086 | **Implémenté** | Paiements marchands, QR Code, checkout |
| [compliance-service](compliance-service/) | 8087 | **Implémenté** | KYC multi-niveaux, AML, Kafka kyc.approved |
| [fraud-service](fraud-service/) | 8088 | **Implémenté** | Scoring fraude temps réel, règles composables, alertes |
| [admin-service](admin-service/) | 8089 | **Implémenté** | Back-office, blacklist, actions comptes |
| [reporting-service](reporting-service/) | 8090 | **Implémenté** | Relevés PDF/CSV, rapports |
| [audit-service](audit-service/) | 8091 | **Implémenté** | Audit trail immuable, Kafka multi-topic |
| [mobile-money-integration](mobile-money-integration/) | 8092 | **Implémenté** | M-Pesa, Orange Money, MTN MoMo, Airtel Money |
| [banking-integration](banking-integration/) | 8093 | **Implémenté** | SWIFT, virements bancaires, comptes liés |

## Flux événementiel

```
auth-service → user.events → user-service + wallet-service + audit-service
compliance-service → kyc.events → user-service + wallet-service + notification-service + audit-service
wallet-service → wallet.events → notification-service + audit-service
transaction-service → transaction.events → notification-service + fraud-service + audit-service
```

Pour ajouter un nouveau service :
1. Copier la structure de `wallet-service/`
2. Ajouter le module dans `pom.xml` parent
3. Créer le Dockerfile et le manifest Kubernetes
4. Ajouter au pipeline CI/CD matrix
