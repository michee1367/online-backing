# Stratégie de sécurité — VillageSat

## 1. Principes Zero Trust

1. **Never trust, always verify** — Chaque requête est authentifiée et autorisée
2. **Least privilege** — RBAC granulaire, accès minimal par défaut
3. **Assume breach** — Segmentation réseau, détection continue
4. **Verify explicitly** — mTLS inter-services, validation JWT à chaque hop

## 2. Authentification & autorisation

### OAuth2 / OIDC (Keycloak)

- **Authorization Code + PKCE** pour apps mobile/web
- **Client Credentials** pour communication service-to-service
- **Refresh token rotation** avec détection de réutilisation (revocation cascade)
- **JWT** : RS256, expiry 15min (access), 7j (refresh)
- Claims : `sub`, `roles`, `tenant_id`, `mfa_verified`, `kyc_level`

### MFA / 2FA

| Méthode | Usage |
|---------|-------|
| TOTP (Google Authenticator) | Opérations sensibles, login step-up |
| SMS OTP | Fallback (limité, rate limited) |
| Push notification | App mobile (preferred) |
| Biométrie | Device binding côté client |

Step-up auth requis pour : transferts > seuil, changement mot de passe, ajout bénéficiaire.

### RBAC

```
Permission format: {resource}:{action}:{scope}
Exemples:
  wallet:read:own
  transaction:transfer:own
  account:freeze:any
  kyc:review:assigned
```

Implémentation : `@PreAuthorize("hasPermission(#walletId, 'Wallet', 'read')")` avec custom `PermissionEvaluator`.

## 3. Chiffrement

| Donnée | Au repos | En transit | Algorithme |
|--------|----------|------------|------------|
| PII (nom, email) | AES-256-GCM | TLS 1.3 | Vault Transit Engine |
| Numéros compte | AES-256-GCM + tokenization | TLS 1.3 | Application-level |
| Mots de passe | bcrypt (cost 12) | TLS 1.3 | Keycloak |
| Clés API | Vault KV v2 | mTLS | Rotation 90j |
| Audit logs | AES-256 + WORM storage | TLS 1.3 | S3 Object Lock |

### Rotation des clés

- **Automatique** via Vault : rotation every 90 days
- **Envelope encryption** : DEK par enregistrement, KEK dans Vault
- **Grace period** : anciennes clés valides 30j pour déchiffrement

## 4. Protections applicatives

### Rate limiting (Kong + Redis)

| Endpoint | Limite | Fenêtre |
|----------|--------|---------|
| `/auth/login` | 5 req | 15 min / IP |
| `/auth/mfa/*` | 3 req | 5 min / user |
| `/transactions/*` | 30 req | 1 min / user |
| `/payments/qr` | 60 req | 1 min / merchant |
| Global API | 1000 req | 1 min / API key |

### Anti-bruteforce

- Compte verrouillé après 5 échecs (exponential backoff)
- CAPTCHA après 3 échecs
- Alerting fraud-service sur patterns anormaux
- IP reputation (blocklist intégrée)

### Idempotence

```http
POST /api/v1/transactions/transfer
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
Content-Type: application/json
Authorization: Bearer {jwt}
```

Redis : `idempotency:{key}` → response hash, TTL 86400s. Rejeu = même réponse 200/201.

### Signature des transactions sensibles

Transactions > 500 USD : signature HMAC-SHA256 côté client avec clé device-bound.

```
X-Transaction-Signature: HMAC-SHA256(body + timestamp + nonce, deviceSecret)
X-Transaction-Timestamp: 1716566400
X-Transaction-Nonce: uuid
```

Serveur : rejet si timestamp > 5 min ou nonce déjà vu (anti-replay).

## 5. Protection contre les attaques

| Attaque | Mitigation |
|---------|------------|
| **SQL Injection** | JPA parameterized queries, jamais de concat SQL |
| **XSS** | Content-Security-Policy, output encoding, pas de HTML user-generated |
| **CSRF** | SameSite=Strict cookies, CSRF token pour sessions cookie-based |
| **SSRF** | Allowlist URLs externes, pas de redirect user-controlled |
| **Replay** | Nonce + timestamp + idempotency key |
| **DDoS** | WAF (Cloudflare/AWS Shield), rate limiting, auto-scaling |
| **MITM** | TLS 1.3, certificate pinning mobile |
| **Supply chain** | Trivy/Snyk CI, SBOM, signed images (Cosign) |

## 6. Journalisation sécurisée

### Règles

- **Jamais** logger : mots de passe, tokens, PAN, CVV, clés privées
- **Pseudonymiser** : user ID hashé dans logs non-audit
- **Structured logging** : JSON avec `traceId`, `spanId`, `userId`, `action`
- **Intégrité** : audit events signés (Ed25519) et stockés en WORM

### Audit trail immuable

```
audit.events → Kafka → audit-service → PostgreSQL (append-only)
                                      → S3 Object Lock (7-10 ans)
```

Chaque entrée : `{timestamp, actor, action, resource, before, after, ip, signature}`.

## 7. Secure session management

- Access token : 15 min, stateless JWT
- Refresh token : HttpOnly, Secure, SameSite=Strict cookie
- Session invalidation : blacklist Redis on logout/compromise
- Device fingerprinting : alerte si nouveau device
- Concurrent session limit : 3 devices max

## 8. Détection de fraude

Pipeline temps réel (fraud-service) :

1. **Rules engine** : montant, fréquence, géolocalisation, device
2. **ML scoring** : modèle gradient boosting (features transactionnelles)
3. **Actions** : ALLOW | REVIEW | BLOCK | STEP_UP_MFA
4. **Feedback loop** : analystes marquent faux positifs → retraining

## 9. Checklist sécurité production

- [ ] TLS 1.3 only, HSTS enabled
- [ ] mTLS inter-services via Istio
- [ ] Vault unsealed avec auto-unseal (cloud KMS)
- [ ] Network policies deny-all default
- [ ] Pod Security Standards : restricted
- [ ] Secrets scan dans CI (gitleaks)
- [ ] Dependency scan (Trivy, Snyk)
- [ ] Penetration test trimestriel
- [ ] SOC 2 Type II roadmap
