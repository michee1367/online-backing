# Notification Service

Service de notifications multi-canal pour VillageSat. Consomme des événements Kafka et envoie des notifications par SMS, email et push.

## Architecture

Architecture hexagonale (ports & adapters) :

| Couche | Package | Rôle |
|--------|---------|------|
| Domain | `domain/model/` | Entités domaine (Notification, NotificationTemplate) |
| Ports IN | `domain/port/in/` | Interface use-case (NotificationUseCase) |
| Ports OUT | `domain/port/out/` | Ports sortants (Repository, Gateways) |
| Application | `application/service/` | NotificationService, TemplateResolver |
| Adapter IN Web | `adapter/in/web/` | Controller REST |
| Adapter IN Messaging | `adapter/in/messaging/` | Consumers Kafka |
| Adapter OUT Persistence | `adapter/out/persistence/` | JPA entities, repos, mappers |
| Adapter OUT Gateway | `adapter/out/gateway/` | Gateways simulés (SMS, Email, Push) |
| Config | `config/` | JpaConfig, KafkaConfig |

## API REST

| Méthode | Endpoint | Rôle | Auth |
|---------|----------|------|------|
| POST | `/api/v1/notifications/send` | Envoi manuel | ADMIN |
| GET | `/api/v1/notifications` | Mes notifications | CUSTOMER |
| GET | `/api/v1/notifications/{id}` | Détail notification | CUSTOMER |

## Événements Kafka consommés

| Topic | Événement | Action |
|-------|-----------|--------|
| `transaction.events` | `transaction.completed` | SMS sender + receiver |
| `kyc.events` | `kyc.approved` / `kyc.rejected` | Email utilisateur |
| `wallet.events` | `wallet.created` | SMS bienvenue |

## Templates

Les templates de notification sont définis en dur dans `TemplateResolver` avec des placeholders `{{variable}}`.

## Lancement

```bash
# Prérequis : PostgreSQL + Kafka
mvn spring-boot:run -pl services/notification-service
```

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

Port : **8085**
