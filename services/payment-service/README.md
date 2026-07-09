# Payment Service

Service de paiements marchands pour VillageSat — QR Code, checkout et gestion des marchands.

## Architecture

Architecture hexagonale (ports & adapters) :

```
com.villagesat.payment
├── domain/
│   ├── model/          # Records Java 21 : Merchant, Payment, enums
│   └── port/
│       ├── in/         # Use cases : MerchantUseCase, PaymentUseCase
│       └── out/        # Repositories, EventPublisher
├── application/
│   └── service/        # MerchantService, PaymentService, QrCodeGenerator
├── adapter/
│   ├── in/web/         # REST controllers, DTOs, ExceptionHandler
│   └── out/
│       ├── persistence/  # JPA entities, repos, mapper
│       └── messaging/    # Kafka event publisher
└── config/             # JpaConfig, KafkaConfig
```

## Endpoints

| Méthode | URL | Description |
|---------|-----|-------------|
| POST | `/api/v1/payments/merchants` | Enregistrer un marchand |
| GET | `/api/v1/payments/merchants/me` | Lister mes marchands |
| POST | `/api/v1/payments/initiate` | Initier un paiement |
| POST | `/api/v1/payments/{reference}/confirm` | Confirmer un paiement |
| GET | `/api/v1/payments/{reference}` | Consulter un paiement |
| POST | `/api/v1/payments/{reference}/refund` | Rembourser un paiement |
| POST | `/api/v1/payments/qr/generate` | Générer un QR code paiement |

## Configuration

| Variable | Défaut | Description |
|----------|--------|-------------|
| `DB_HOST` | localhost | Hôte PostgreSQL |
| `DB_PORT` | 5432 | Port PostgreSQL |
| `DB_NAME` | villagesat | Base de données |
| `DB_USER` | villagesat | Utilisateur DB |
| `DB_PASSWORD` | villagesat_secret | Mot de passe DB |
| `KAFKA_BOOTSTRAP` | localhost:9092 | Serveurs Kafka |
| `KEYCLOAK_ISSUER` | http://localhost:8180/realms/villagesat | Issuer JWT |

## Lancement

```bash
mvn spring-boot:run -pl services/payment-service
```

Port : **8086**

## Docker

```bash
docker build -f services/payment-service/Dockerfile -t villagesat/payment-service .
docker run -p 8086:8086 villagesat/payment-service
```
