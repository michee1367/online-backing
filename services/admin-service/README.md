# Admin Service

Back-office administration pour les opérateurs VillageSat.

## Port
8089

## Fonctionnalités
- Gestion des actions sur les comptes (freeze, unfreeze, close, reset MFA, force KYC review)
- Gestion de la blacklist (utilisateurs, téléphones, emails, appareils)
- Configuration système

## Endpoints
| Méthode | Path | Rôle |
|---------|------|------|
| POST | /api/v1/admin/actions | ADMIN |
| GET | /api/v1/admin/actions?userId= | ADMIN |
| POST | /api/v1/admin/blacklist | ADMIN |
| DELETE | /api/v1/admin/blacklist/{id} | ADMIN |
| GET | /api/v1/admin/blacklist | ADMIN |
| GET | /api/v1/admin/config | ADMIN |
| PUT | /api/v1/admin/config/{key} | ADMIN |

## Stack
- Java 21, Spring Boot 3.4
- PostgreSQL (schema: admin)
- Kafka (producer)
- OAuth2/JWT (Keycloak)
- Flyway migrations

## Build & Run
```bash
mvn clean package -DskipTests
java -jar target/admin-service-1.0.0-SNAPSHOT.jar
```
