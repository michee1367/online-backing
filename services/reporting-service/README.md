# Reporting Service

Génération de relevés et rapports pour les clients VillageSat.

## Port
8090

## Fonctionnalités
- Demande de génération de rapports (relevé de compte, historique transactions, résumé KYC)
- Suivi du statut de génération
- Téléchargement des rapports générés
- Formats supportés : PDF, CSV, JSON

## Endpoints
| Méthode | Path | Rôle |
|---------|------|------|
| POST | /api/v1/reports/request | CUSTOMER |
| GET | /api/v1/reports | Authentifié (mes rapports) |
| GET | /api/v1/reports/{id} | Authentifié |
| GET | /api/v1/reports/{id}/download | Authentifié |

## Stack
- Java 21, Spring Boot 3.4
- PostgreSQL (schema: reporting)
- Kafka (producer)
- OAuth2/JWT (Keycloak)
- Flyway migrations

## Build & Run
```bash
mvn clean package -DskipTests
java -jar target/reporting-service-1.0.0-SNAPSHOT.jar
```
