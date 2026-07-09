# API Gateway

Point d'entrée unique pour tous les clients (mobile, web, marchands).

## Stack

- **Spring Cloud Gateway** (WebFlux)
- **Redis** rate limiting (token bucket)
- **Resilience4j** circuit breaker
- **OAuth2/JWT** Keycloak

## Routes

| Path | Service cible | Port |
|------|--------------|------|
| `/api/v1/auth/**` | auth-service | 8081 |
| `/api/v1/users/**` | user-service | 8082 |
| `/api/v1/wallets/**` | wallet-service | 8083 |
| `/api/v1/transactions/**` | transaction-service | 8084 |
| `/api/v1/notifications/**` | notification-service | 8085 |
| `/api/v1/payments/**` | payment-service | 8086 |
| `/api/v1/kyc/**` | compliance-service | 8087 |
| `/api/v1/fraud/**` | fraud-service | 8088 |
| `/api/v1/admin/**` | admin-service | 8089 |
| `/api/v1/reports/**` | reporting-service | 8090 |

## Rate Limiting

- 20 requêtes/seconde par utilisateur (burst 40)
- Clé : userId JWT ou IP si non authentifié
- Stocké dans Redis

## Circuit Breaker

- Fenêtre glissante : 10 requêtes
- Seuil de failure : 50%
- Temps d'attente (open) : 10s
- Fallback : message « service temporairement indisponible »

## Filtres globaux

1. **RequestIdFilter** : injecte un `X-Request-Id` (traçabilité)
2. **LoggingFilter** : log méthode + path + status + durée
3. **CORS** : autorisé pour tous les origins

## Sécurité

- `/api/v1/auth/**` : public (inscription, login)
- `/internal/**` : bloqué (inter-service uniquement via mesh)
- Tout le reste : JWT obligatoire

Port : **8080**
