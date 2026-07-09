# Compliance Service — VillageSat

Service de conformité : KYC multi-niveaux, screening AML, publication Kafka `kyc.approved`.

## Endpoints

| Méthode | Path | Rôle | Description |
|---------|------|------|-------------|
| POST | `/api/v1/kyc/submit` | CUSTOMER | Soumettre documents KYC |
| GET | `/api/v1/kyc/status` | CUSTOMER | Statut + plafonds |
| GET | `/api/v1/compliance/kyc/pending` | COMPLIANCE_OFFICER | File d'attente revue |
| POST | `/api/v1/compliance/kyc/{id}/approve` | COMPLIANCE_OFFICER | Approbation manuelle |
| POST | `/api/v1/compliance/kyc/{id}/reject` | COMPLIANCE_OFFICER | Rejet manuel |

## Workflow KYC

```
POST /kyc/submit
    → Screening AML (PEP, SANCTIONS, ADVERSE_MEDIA)
    → Vérification identité simulée (score 0-1)
    → score ≥ 0.95 → APPROVED → Kafka kyc.approved
    → score ≥ 0.70 → IN_REVIEW (revue manuelle)
    → score < 0.40 → REJECTED
```

## Événements Kafka (`kyc.events`)

| Event | Consommateurs |
|-------|---------------|
| `kyc.submitted` | audit-service (futur) |
| `kyc.approved` | **user-service** (kycLevel + ACTIVE), **wallet-service** (plafonds) |
| `kyc.rejected` | notification-service (futur) |

Payload `kyc.approved` :
```json
{
  "eventType": "kyc.approved",
  "payload": {
    "userId": "uuid",
    "level": 1,
    "submissionId": "uuid",
    "status": "APPROVED",
    "riskScore": 0.97
  }
}
```

## Niveaux KYC

| Niveau | Plafond journalier | International | Cartes |
|--------|-------------------|---------------|--------|
| L0 | 50 USD | Non | Non |
| L1 | 500 USD | Non | Non |
| L2 | 5 000 USD | Oui | Oui |
| L3 | Illimité* | Oui | Oui |

## Test sanctions (dev)

Document contenant `SANCTION_TEST` ou `OFAC_BLOCKED` → rejet automatique.

## Démarrage

```bash
docker compose up -d postgres kafka
cd services/compliance-service && mvn spring-boot:run
```

Port : **8087** | Swagger : http://localhost:8087/swagger-ui.html
