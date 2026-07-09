# Wallet Service

Gestion des wallets multi-devises, ledger double-entry, plafonds KYC et opérations internes (débit/crédit).

## Fonctionnalités

- Création / liste des wallets par utilisateur
- Soldes avec verrouillage pessimiste (`REPEATABLE_READ`)
- Ledger append-only (DEBIT / CREDIT)
- **Plafonds KYC** alignés sur `compliance-service` (L0–L3)
- Validation des plafonds journaliers / mensuels à chaque débit
- Kafka : provisioning wallet à l'inscription, mise à jour plafonds à l'approbation KYC

## Plafonds par niveau KYC

| Niveau | Plafond journalier | Plafond mensuel |
|--------|-------------------|-----------------|
| L0 | 200 000 | 2 000 000 |
| L1 | 500 | 5 000 |
| L2 | 5 000 | 50 000 |
| L3 | 999 999 | 9 999 999 |

## Flux Kafka

```
auth-service          → user.events   → user.created
wallet-service        → crée wallet CDF/USD (L0)

compliance-service    → kyc.events    → kyc.approved
wallet-service        → met à jour plafonds de tous les wallets actifs
user-service          → met à jour profil + Keycloak (en parallèle)

wallet-service        → wallet.events → wallet.limits.updated
```

Payload `kyc.approved` (consommé) :

```json
{
  "eventType": "kyc.approved",
  "payload": {
    "userId": "...",
    "level": 1,
    "submissionId": "...",
    "status": "APPROVED",
    "riskScore": 0.98
  }
}
```

## API

| Méthode | Chemin | Description |
|---------|--------|-------------|
| POST | `/api/v1/wallets` | Créer un wallet (plafonds L0) |
| GET | `/api/v1/wallets` | Lister ses wallets |
| GET | `/api/v1/wallets/{id}` | Détail + plafonds KYC |
| GET | `/api/v1/wallets/{id}/balance` | Solde |
| GET | `/api/v1/transactions?walletId=` | Historique ledger |
| POST | `/api/v1/transactions/transfer` | Transfert P2P (relai → transaction-service) |
| POST | `/internal/wallets/{id}/debit` | Débit (transaction-service) |
| POST | `/internal/wallets/{id}/credit` | Crédit |
| POST | `/internal/wallets/users/{userId}/kyc-limits` | Sync manuelle plafonds |

Port : **8083**

## Tests

```bash
mvn -pl services/wallet-service -am test
```

Détails : [tests/README.md](../../tests/README.md).

## Erreurs métier

| Code | HTTP | Description |
|------|------|-------------|
| `TRANSACTION_LIMIT_EXCEEDED` | 422 | Plafond journalier ou mensuel dépassé |
| `WALLET_INSUFFICIENT_FUNDS` | 422 | Solde insuffisant |
| `ACCOUNT_FROZEN` | 403 | Wallet gelé |
