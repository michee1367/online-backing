# API Reference — VillageSat v1

Base URL : `https://api.villagesat.com/api/v1`

Headers communs :
```http
Authorization: Bearer {access_token}
Content-Type: application/json
X-Request-Id: {uuid}
Idempotency-Key: {uuid}  # Obligatoire pour POST transaction/payment
Accept-Language: fr|en
```

## Auth Service

### POST /auth/register
Création de compte utilisateur.

```json
// Request
{
  "email": "user@example.com",
  "phone": "+243812345678",
  "password": "SecureP@ss123!",
  "firstName": "Jean",
  "lastName": "Kabongo",
  "countryCode": "CD",
  "acceptedTerms": true
}

// Response 201
{
  "userId": "usr_01HXYZ...",
  "email": "user@example.com",
  "kycLevel": 0,
  "status": "PENDING_VERIFICATION",
  "verificationSent": true
}
```

### POST /auth/login
```json
// Request
{ "email": "user@example.com", "password": "SecureP@ss123!" }

// Response 200
{
  "accessToken": "eyJhbG...",
  "refreshToken": "eyJhbG...",
  "expiresIn": 900,
  "tokenType": "Bearer",
  "mfaRequired": true,
  "mfaMethods": ["TOTP"]
}
```

### POST /auth/mfa/verify
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "method": "TOTP",
  "code": "123456",
  "refreshToken": "eyJhbG..."
}
// Response: nouveaux tokens avec mfa_verified=true
```

### POST /auth/refresh
```json
{ "refreshToken": "eyJhbG..." }
```

### POST /auth/logout
Révoque refresh token et blacklist access token.

---

## User Service

### GET /users/me
Profil utilisateur courant.

### PATCH /users/me
```json
{ "firstName": "Jean", "preferredLanguage": "fr", "timezone": "Africa/Kinshasa" }
```

### GET /users/me/data-export
Export RGPD (async, lien S3 signé).

---

## Wallet Service

### POST /wallets
Créer un wallet multi-devise.

```json
// Request
{ "currency": "CDF", "type": "PERSONAL", "label": "Compte principal" }

// Response 201
{
  "walletId": "wal_01HXYZ...",
  "accountNumber": "VS-****-7890",
  "currency": "CDF",
  "balance": "0.00",
  "availableBalance": "0.00",
  "status": "ACTIVE",
  "limits": {
    "dailyTransfer": "500.00",
    "monthlyTransfer": "5000.00"
  }
}
```

### GET /wallets
Liste des wallets de l'utilisateur.

### GET /wallets/{walletId}
Détail + solde.

### GET /wallets/{walletId}/balance
```json
{
  "balance": "150000.00",
  "availableBalance": "145000.00",
  "pendingBalance": "5000.00",
  "currency": "CDF",
  "lastUpdated": "2026-05-24T10:30:00Z"
}
```

### POST /wallets/{walletId}/deposit
```json
{ "amount": "50000.00", "source": "MOBILE_MONEY", "provider": "MPESA", "phoneNumber": "+243..." }
```

### POST /wallets/{walletId}/withdraw
```json
{ "amount": "25000.00", "destination": "MOBILE_MONEY", "provider": "ORANGE_MONEY", "phoneNumber": "+243..." }
```

---

## Transaction Service

### POST /transactions/transfer
Transfert interne P2P.

```json
// Request
{
  "sourceWalletId": "wal_01...",
  "destinationWalletId": "wal_02...",
  "amount": "10000.00",
  "currency": "CDF",
  "description": "Remboursement déjeuner",
  "category": "P2P"
}

// Response 201
{
  "transactionId": "txn_01HXYZ...",
  "status": "COMPLETED",
  "amount": "10000.00",
  "fee": "100.00",
  "totalDebited": "10100.00",
  "sourceBalance": "139900.00",
  "completedAt": "2026-05-24T10:31:00Z"
}
```

### POST /transactions/external-transfer
Transfert externe (mobile money, banque).

```json
{
  "sourceWalletId": "wal_01...",
  "destination": {
    "type": "MOBILE_MONEY",
    "provider": "MTN_MOMO",
    "phoneNumber": "+243987654321",
    "recipientName": "Marie Tshisekedi"
  },
  "amount": "50000.00",
  "currency": "CDF"
}
```

### GET /transactions
Query: `?walletId=&status=&from=&to=&page=0&size=20`

### GET /transactions/{transactionId}

---

## Payment Service

### POST /payments/merchant
Paiement marchand.

```json
{
  "walletId": "wal_01...",
  "merchantId": "mer_01...",
  "amount": "15000.00",
  "currency": "CDF",
  "orderReference": "ORD-2026-001"
}
```

### POST /payments/qr/generate
Génération QR Code (marchand).

```json
// Request
{ "merchantId": "mer_01...", "amount": "5000.00", "currency": "CDF", "expiresInSeconds": 300 }

// Response
{
  "qrCodeId": "qr_01...",
  "qrPayload": "villagesat://pay?qr=abc123",
  "qrImageUrl": "https://cdn.villagesat.com/qr/abc123.png",
  "expiresAt": "2026-05-24T10:35:00Z"
}
```

### POST /payments/qr/scan
Paiement via scan QR.

```json
{ "walletId": "wal_01...", "qrPayload": "villagesat://pay?qr=abc123" }
```

---

## Compliance Service

### POST /kyc/submit
```json
{
  "level": 1,
  "documentType": "NATIONAL_ID",
  "documentNumber": "ID123456",
  "documentFrontUrl": "s3://kyc-docs/...",
  "selfieUrl": "s3://kyc-docs/..."
}
```

### GET /kyc/status
```json
{ "level": 1, "status": "APPROVED", "approvedAt": "...", "limits": {...} }
```

### POST /kyb/submit
Soumission KYB entreprise (L3).

---

## Admin Service

### POST /admin/accounts/{userId}/freeze
```json
{ "reason": "SUSPICIOUS_ACTIVITY", "notes": "AML alert #4521" }
```

### POST /admin/blacklist
```json
{ "type": "PHONE", "value": "+243...", "reason": "FRAUD_CONFIRMED" }
```

---

## Notification Service

### WebSocket /ws/notifications
```javascript
// Connect with JWT
ws = new WebSocket('wss://api.villagesat.com/ws/notifications?token=eyJ...')

// Message
{ "type": "TRANSACTION_COMPLETED", "data": { "transactionId": "...", "amount": "..." } }
```

### GET /notifications
Historique notifications.

---

## Codes d'erreur standard

| Code | HTTP | Description |
|------|------|-------------|
| `AUTH_INVALID_CREDENTIALS` | 401 | Login échoué |
| `AUTH_MFA_REQUIRED` | 403 | MFA step-up requis |
| `AUTH_TOKEN_EXPIRED` | 401 | JWT expiré |
| `WALLET_INSUFFICIENT_FUNDS` | 422 | Solde insuffisant |
| `TXN_LIMIT_EXCEEDED` | 422 | Limite journalière dépassée |
| `TXN_DUPLICATE` | 409 | Idempotency key déjà utilisée |
| `FRAUD_BLOCKED` | 403 | Transaction bloquée par fraude |
| `KYC_LEVEL_INSUFFICIENT` | 403 | Niveau KYC insuffisant |
| `ACCOUNT_FROZEN` | 403 | Compte gelé |
| `RATE_LIMIT_EXCEEDED` | 429 | Trop de requêtes |

Format erreur :
```json
{
  "error": {
    "code": "WALLET_INSUFFICIENT_FUNDS",
    "message": "Solde insuffisant pour effectuer cette opération",
    "requestId": "req_01HXYZ...",
    "timestamp": "2026-05-24T10:30:00Z"
  }
}
```

## OpenAPI

Spec complète : [`/docs/openapi/villagesat-api.yaml`](../docs/openapi/villagesat-api.yaml)

Swagger UI local : `http://localhost:8080/swagger-ui.html`
