# Fraud Service

Service de détection de fraude en temps réel pour la plateforme VillageSat.

## Architecture

Architecture hexagonale (Ports & Adapters) :

```
com.villagesat.fraud
├── domain/
│   ├── model/          # Entités domaine pures (records)
│   └── port/
│       ├── in/         # Use cases (FraudScoringUseCase, FraudAlertUseCase)
│       └── out/        # Ports sortants (Repository, EventPublisher, TransactionHistoryPort)
├── application/
│   ├── service/        # Services applicatifs
│   └── rules/          # Moteur de règles composable
├── adapter/
│   ├── in/
│   │   ├── web/        # Controllers REST
│   │   └── messaging/  # Consumers Kafka
│   └── out/
│       ├── persistence/ # JPA adapters
│       ├── messaging/   # Kafka publisher
│       └── stub/        # Stubs pour dépendances externes
└── config/             # Configuration Spring
```

## Règles de scoring

| Règle | Condition | Points |
|-------|-----------|--------|
| AmountThreshold | Montant > 10 000 | +30 |
| VelocityRule | > 5 transactions/heure | +25 |
| LargeTransfer | Montant > 50 000 | +40 |
| NewAccount | Compte < 7 jours | +15 |
| CrossBorder | Devise ≠ CDF | +10 |
| UnusualHour | Transaction 01:00-05:00 UTC | +10 |

**Actions selon le score :**
- 0-29 → ALLOW
- 30-59 → REVIEW
- 60-79 → STEP_UP_MFA
- 80-100 → BLOCK

## Endpoints

### Interne (service-to-service)
- `POST /internal/fraud/score` — Scoring en temps réel

### API (authentifié, rôle FRAUD_ANALYST)
- `GET /api/v1/fraud/alerts?status=OPEN` — Lister les alertes
- `POST /api/v1/fraud/alerts/{id}/resolve` — Résoudre une alerte

### Kafka
- **Consomme** : `transaction.events` (analyse post-hoc)
- **Produit** : `fraud.events` (alertes BLOCK)

## Configuration

| Variable | Description | Défaut |
|----------|-------------|--------|
| `DB_HOST` | Hôte PostgreSQL | `localhost` |
| `DB_PORT` | Port PostgreSQL | `5432` |
| `DB_NAME` | Nom de la base | `villagesat` |
| `KAFKA_BOOTSTRAP` | Serveurs Kafka | `localhost:9092` |
| `KEYCLOAK_ISSUER` | Issuer URI Keycloak | `http://localhost:8180/realms/villagesat` |
| `INTERNAL_SERVICE_TOKEN` | Token inter-services | `dev-internal-token` |

## Lancer

```bash
# Dev local
mvn spring-boot:run -pl services/fraud-service

# Docker
docker build -f services/fraud-service/Dockerfile -t villagesat/fraud-service .
docker run -p 8088:8088 villagesat/fraud-service
```

## Tests

```bash
mvn test -pl services/fraud-service
```
