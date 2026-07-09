# Tests de charge k6 — VillageSat

## Prérequis

```bash
brew install k6
docker compose up -d
# Démarrer wallet-service et transaction-service (profil dev)
```

## Exécution

```bash
k6 run tests/load/k6-transfer-load.js

# Variables d'environnement
WALLET_URL=http://localhost:8083 \
TXN_URL=http://localhost:8084 \
INTERNAL_TOKEN=dev-internal-token \
k6 run tests/load/k6-transfer-load.js
```

## Seuils (SLO)

| Métrique | Cible |
|----------|-------|
| p99 latency | < 500 ms |
| Error rate | < 1% |
| Checks pass | > 99% |

## Scénario

1. **Setup** : création wallets + crédit initial via API interne
2. **Load** : transferts P2P avec `Idempotency-Key` unique
3. **Vérification** : lecture solde après chaque transfert
