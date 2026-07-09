# Base de données — VillageSat

## 1. Stratégie

- **Database-per-Service** : chaque microservice possède son schéma PostgreSQL dédié
- **ACID** : transactions avec isolation `READ COMMITTED` minimum, `SERIALIZABLE` pour ledger
- **Partitionnement** : tables transactionnelles partitionnées par mois
- **Réplication** : Patroni (1 primary + 2 sync replicas)
- **Backup** : pgBackRest, full daily + WAL continuous, RPO=0

## 2. Schémas par service

| Schéma | Service | Tables principales |
|--------|---------|-------------------|
| `auth` | auth-service | sessions, mfa_secrets, device_tokens |
| `users` | user-service | users, profiles, preferences |
| `wallets` | wallet-service | wallets, balances, ledger_entries |
| `transactions` | transaction-service | transactions, transfers, fees |
| `payments` | payment-service | payments, qr_codes, merchants |
| `compliance` | compliance-service | kyc_submissions, kyb, screenings |
| `fraud` | fraud-service | rules, scores, alerts |
| `audit` | audit-service | audit_log (append-only) |
| `admin` | admin-service | blacklist, account_actions |

## 3. Modèle relationnel (core)

```
users.users ──1:N──► wallets.wallets ──1:N──► wallets.ledger_entries
                         │
                         └──1:N──► transactions.transactions
                                       │
                                       ├──N:1── transactions.fees
                                       └──1:1── payments.payments (optional)

compliance.kyc_submissions ──N:1── users.users
audit.audit_log (standalone, all entities)
```

## 4. Index optimisés

```sql
-- Transactions : recherche par wallet + date (couverture partielle)
CREATE INDEX idx_txn_source_wallet_created
  ON transactions.transactions (source_wallet_id, created_at DESC)
  WHERE status != 'FAILED';

-- Ledger : balance calculation
CREATE INDEX idx_ledger_wallet_entry
  ON wallets.ledger_entries (wallet_id, entry_sequence DESC);

-- Audit : recherche réglementaire
CREATE INDEX idx_audit_actor_created
  ON audit.audit_log (actor_id, created_at DESC);
```

## 5. Partitionnement

Tables partitionnées par `RANGE (created_at)` :
- `transactions.transactions` — mensuel
- `wallets.ledger_entries` — mensuel
- `audit.audit_log` — trimestriel

Rétention : transactions 7 ans, audit 10 ans. Archivage S3 après expiration partition.

## 6. Double-entry ledger

Chaque mouvement financier génère 2 entrées ledger :

| Entry | Wallet | Type | Amount |
|-------|--------|------|--------|
| 1 | Source | DEBIT | -100.00 |
| 2 | Destination | CREDIT | +100.00 |

Invariant : `SUM(debits) = SUM(credits)` pour chaque transaction_id.

## 7. Sauvegarde & DR

| Type | Fréquence | Rétention | Stockage |
|------|-----------|-----------|----------|
| Full backup | Daily 02:00 UTC | 30 jours | S3 cross-region |
| WAL archive | Continuous | 7 jours | S3 |
| Logical dump | Weekly | 90 jours | S3 encrypted |

Test restore : mensuel automatisé (staging).

## 8. Migration

Flyway par service : `services/{service}/src/main/resources/db/migration/`

Schéma initial global : [`database/schema/V1__init_schema.sql`](../database/schema/V1__init_schema.sql)
