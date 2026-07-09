# Conformité réglementaire — VillageSat

## 1. Cadre réglementaire

| Réglementation | Applicabilité | Mesures clés |
|----------------|---------------|--------------|
| **PCI-DSS v4.0** | Cartes virtuelles, paiements | Tokenization, scope reduction, SAQ-A |
| **RGPD** | Utilisateurs UE | Consentement, droit à l'oubli, DPO |
| **AML/CFT** | Tous marchés | KYC, transaction monitoring, STR |
| **KYC/KYB** | Onboarding | Niveaux progressifs, vérification identité |
| **BCEAO/BEAC** | UEMOA/CEMAC | Reporting réglementaire, plafonds |
| **POPIA** | Afrique du Sud | Protection données personnelles |

## 2. KYC — Niveaux progressifs

| Niveau | Documents | Plafond journalier | Fonctionnalités |
|--------|-----------|-------------------|-----------------|
| L0 — Basic | Email + phone OTP | 50 USD | Wallet, receive only |
| L1 — Standard | ID national + selfie | 500 USD | Transferts P2P |
| L2 — Enhanced | Proof of address | 5 000 USD | International, cards |
| L3 — Business | KYB docs + UBO | Illimité (review) | Merchant, API |

### Workflow KYC multi-niveaux

```
Soumission → OCR/Verification (Onfido/Jumio) → Compliance review
    → Auto-approve (score > 0.95) | Manual review | Reject
    → Event kyc.approved → Wallet limits updated
```

## 3. AML — Anti-Money Laundering

### Transaction Monitoring Rules

- Structuring detection (smurfing) : multiples tx juste sous seuil
- Velocity : > N transactions / heure
- Geographic risk : pays liste grise/noire FATF
- PEP screening : bases Dow Jones / World-Check
- Sanctions screening : OFAC, EU, UN lists (temps réel)

### Suspicious Transaction Report (STR)

Workflow : `fraud.alert` → compliance officer review → STR filing → account freeze if confirmed.

## 4. PCI-DSS Scope Reduction

- **Pas de stockage PAN** — Tokenization via processeur (Stripe/Flutterwave)
- **Scope CDE minimal** — Seul payment-service en scope
- **Network segmentation** — CDE isolé, firewall rules strictes
- **Quarterly ASV scans** + annual penetration test

## 5. RGPD

| Droit | Implémentation |
|-------|----------------|
| Accès | `GET /api/v1/users/me/data-export` |
| Rectification | `PATCH /api/v1/users/me` |
| Effacement | Anonymisation (pas suppression ledger — obligation légale) |
| Portabilité | Export JSON/CSV |
| Opposition | Opt-out marketing, profiling |

- **DPO** désigné
- **Registre des traitements** maintenu
- **DPIA** pour scoring fraude et profiling
- **Rétention** : PII 5 ans post-clôture compte, transactions 7-10 ans

## 6. Journalisation réglementaire

Tout événement réglementaire est capturé dans `audit.regulatory_events` :

- Ouverture/fermeture compte
- Modification KYC
- Transaction > seuil déclaratif
- Gel/dégel compte
- STR soumis
- Accès données par admin

Conservation : **10 ans**, stockage WORM (S3 Object Lock Compliance mode).

## 7. Conservation des preuves

```
┌─────────────┐    ┌──────────────┐    ┌─────────────┐
│ Transaction │───►│ Audit Service│───►│ S3 WORM     │
│ + KYC docs  │    │ (signé)      │    │ 10 ans      │
└─────────────┘    └──────────────┘    └─────────────┘
```

Documents KYC chiffrés AES-256, accès loggé, rétention légale respectée.

## 8. Traçabilité complète

Chaque opération financière est traçable de bout en bout :

`Client Request → Gateway (requestId) → Service (traceId) → DB (transactionId) → Kafka (eventId) → Audit (auditId)`

Corrélation via **OpenTelemetry** : traceId propagé dans tous les headers.
