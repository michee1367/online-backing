# Auth Service — VillageSat

Service d'authentification central : Keycloak, sessions, MFA TOTP, anti-bruteforce.

## Endpoints

| Méthode | Path | Auth | Description |
|---------|------|------|-------------|
| POST | `/api/v1/auth/register` | Public | Inscription |
| POST | `/api/v1/auth/login` | Public | Connexion |
| POST | `/api/v1/auth/refresh` | Public | Refresh token |
| POST | `/api/v1/auth/logout` | JWT | Déconnexion + blacklist |
| POST | `/api/v1/auth/mfa/setup` | JWT | Générer secret TOTP |
| POST | `/api/v1/auth/mfa/confirm` | JWT | Activer MFA |
| POST | `/api/v1/auth/mfa/verify` | Public | Valider MFA post-login |

## Démarrage

```bash
docker compose up -d postgres redis keycloak
cd services/auth-service && mvn spring-boot:run
```

Swagger : http://localhost:8081/swagger-ui.html

## Flux MFA

1. `POST /auth/mfa/setup` (authentifié) → secret + QR + backup codes
2. `POST /auth/mfa/confirm` → active MFA
3. `POST /auth/login` → `mfaRequired: true` + `sessionId`
4. `POST /auth/mfa/verify` → `{ sessionId, method, code, refreshToken }` → nouveaux tokens avec `mfa_verified=true`

## Configuration

| Variable | Défaut | Description |
|----------|--------|-------------|
| `KEYCLOAK_URL` | http://localhost:8180 | URL Keycloak |
| `KEYCLOAK_ADMIN_SECRET` | service-client-secret... | Secret client admin |
| `AUTH_ENCRYPTION_KEY` | (dev key) | Clé AES-256 MFA secrets |
| `SPRING_PROFILES_ACTIVE` | dev | Profil Spring |

## Utilisateur démo Keycloak

- Email : `demo.customer@villagesat.com`
- Mot de passe : `SecureP@ss123!`
