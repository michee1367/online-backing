# Transaction Service

Transferts P2P internes : scoring fraude, frais, idempotence Redis, orchestration wallet-service.

## Flux transfert

1. Vérification `Idempotency-Key` (Redis)
2. Scoring fraude synchrone (`fraud-service`, fallback ALLOW)
3. Calcul frais (1 %, minimum 100)
4. Débit source (montant + frais) via wallet-service
5. Crédit destination
6. Persistance + événement Kafka `transaction.completed`

## API

| Méthode | Chemin | Headers |
|---------|--------|---------|
| POST | `/api/v1/transactions/transfer` | `Idempotency-Key`, JWT + MFA |

Port : **8084**

## Tests

```bash
mvn -pl services/transaction-service -am test
```

| Type | Classes |
|------|---------|
| Unitaire | `FeeCalculatorTest`, `TransactionTest`, `TransferServiceTest` |
| Intégration | `TransferIntegrationTest` (PostgreSQL + Redis Testcontainers) |

Détails : [tests/README.md](../../tests/README.md).

## Erreurs

| Code | HTTP |
|------|------|
| `FRAUD_BLOCKED` | 403 |
| `TXN_DUPLICATE` | 409 |
