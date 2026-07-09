# Audit Service

Trail d'audit immuable — consomme des événements Kafka et persiste un log.

## Port
8091

## Fonctionnalités
- Consommation de tous les événements Kafka (user, wallet, transaction, kyc)
- Persistance immuable dans une table partitionnée par date
- Recherche avancée par critères

## Endpoints
| Méthode | Path | Rôle |
|---------|------|------|
| GET | /api/v1/audit/search?entityType=&entityId=&from=&to= | ADMIN, COMPLIANCE_OFFICER |
| GET | /api/v1/audit/{id} | ADMIN, COMPLIANCE_OFFICER |

## Kafka Topics écoutés
- user.events
- wallet.events
- transaction.events
- kyc.events

## Stack
- Java 21, Spring Boot 3.4
- PostgreSQL (schema: audit, table partitionnée)
- Kafka (consumer)
- OAuth2/JWT (Keycloak)
- Flyway migrations

## Build & Run
```bash
mvn clean package -DskipTests
java -jar target/audit-service-1.0.0-SNAPSHOT.jar
```
