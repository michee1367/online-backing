# Bonnes pratiques bancaires — VillageSat

## 1. Principes fondamentaux

### Double-entry bookkeeping
Toute opération financière produit au minimum une écriture DEBIT et une écriture CREDIT. L'invariant `SUM(debits) = SUM(credits)` est vérifié en continu par un job de réconciliation.

### Immutabilité du ledger
Les écritures comptables ne sont jamais modifiées ni supprimées. Les corrections passent par des écritures de contrepassation (reversal).

### Idempotence obligatoire
Toute API mutative (POST, PUT, PATCH) qui touche des fonds exige un header `Idempotency-Key`. Un rejeu retourne la même réponse sans effet de bord.

### ACID pour les opérations financières
- **Atomicité** : débit + crédit dans la même transaction DB ou saga avec compensation
- **Cohérence** : contraintes CHECK, FK, triggers d'audit
- **Isolation** : `SELECT FOR UPDATE` (pessimistic lock) sur les balances
- **Durabilité** : sync replication PostgreSQL, WAL archiving

## 2. Gestion des risques

| Risque | Mitigation |
|--------|------------|
| Fraude | Scoring temps réel, step-up MFA, gel automatique |
| Blanchiment | KYC progressif, transaction monitoring, STR |
| Erreur opérationnelle | Four-eyes admin, audit trail, reversal workflow |
| Cyberattaque | Zero Trust, WAF, rate limiting, encryption |
| Perte de données | RPO=0, backup immuable, DR testé trimestriellement |

## 3. Limites et contrôles

- Limites KYC-progressives (L0→L3)
- Limites journalières/mensuelles par wallet
- Seuils de déclaration réglementaire (configurable par pays)
- Velocity checks (max N transactions/heure)
- Géofencing pour opérations cross-border

## 4. Réconciliation

```
Nightly batch:
  1. Internal ledger balance vs wallet balances
  2. Platform wallet (fees collected) vs fee table
  3. External provider statements vs transaction records
  4. Unmatched items → reconciliation queue → manual review
```

## 5. Gestion des incidents

1. **Detection** : alerting Prometheus → PagerDuty
2. **Triage** : classification P1-P4, runbook
3. **Containment** : gel comptes affectés si fraude
4. **Resolution** : fix + deploy blue/green
5. **Post-mortem** : blameless, actions correctives

## 6. Séparation des responsabilités

- Développeurs : pas d'accès prod DB
- Ops : deploy via GitOps (ArgoCD), pas de SSH
- Compliance : accès read-only audit logs
- Admin financier : opérations gel/dégel avec four-eyes

## 7. Standards de référence

- **ISO 27001** : SMSI
- **PCI-DSS v4.0** : paiements par carte
- **PSD2/PSD3** : services de paiement EU
- **FATF Recommendations** : AML/CFT
- **BCEAO/BEAC** : réglementation UEMOA/CEMAC
