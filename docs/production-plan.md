# Plan de production cloud — VillageSat

## 1. Choix cloud & régions

| Provider | Région | Rôle | Justification |
|----------|--------|------|---------------|
| AWS | af-south-1 (Cape Town) | Primary Afrique | Latence, souveraineté données |
| AWS | eu-west-1 (Dublin) | EU + DR | RGPD, DR cross-region |
| AWS | us-east-1 | DR Global | Backup tertiaire |

Alternative multi-cloud : Primary AWS + DR Azure (eu-west) pour résilience vendor.

## 2. Architecture production

```
                    ┌─────────────────┐
                    │   Cloudflare    │
                    │  WAF + DDoS     │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │  Route 53       │
                    │  GeoDNS         │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
     ┌────────▼───┐  ┌──────▼─────┐  ┌────▼───────┐
     │ af-south-1 │  │ eu-west-1  │  │ us-east-1  │
     │ EKS Primary│  │ EKS DR     │  │ S3 Backup  │
     └────────────┘  └────────────┘  └────────────┘
```

## 3. Infrastructure Terraform

Modules :
- `modules/vpc` — VPC, subnets public/private, NAT
- `modules/eks` — Cluster Kubernetes, node groups
- `modules/rds` — PostgreSQL (alternative managed) ou EC2 Patroni
- `modules/elasticache` — Redis
- `modules/msk` — Managed Kafka
- `modules/vault` — HashiCorp Vault HA
- `modules/monitoring` — Prometheus, Grafana

Environnements : `infrastructure/terraform/environments/{dev,staging,prod-af,prod-eu}`

## 4. Checklist go-live

### Sécurité
- [ ] Penetration test externe validé
- [ ] TLS certificates (Let's Encrypt / ACM)
- [ ] Vault initialized, unseal auto via KMS
- [ ] mTLS Istio configuré
- [ ] WAF rules actives
- [ ] Secrets rotation testée

### Opérations
- [ ] Monitoring dashboards + alerting testés
- [ ] Runbooks DR documentés et testés
- [ ] Backup restore testé (< RTO)
- [ ] On-call rotation configurée
- [ ] Status page (Statuspage.io)

### Conformité
- [ ] DPO nommé
- [ ] Registre traitements RGPD
- [ ] AML rules configurées
- [ ] KYC provider intégré (sandbox → prod)
- [ ] Legal : CGU, politique confidentialité

### Performance
- [ ] Load test validé (2x capacity target)
- [ ] HPA testé
- [ ] Failover AZ testé
- [ ] CDN configuré

## 5. SLA & support

| Tier | Disponibilité | Support |
|------|--------------|---------|
| Standard | 99.9% | Email, 24h response |
| Business | 99.95% | Priority, 4h response |
| Enterprise | 99.99% | Dedicated, 1h response |

Maintenance window : Dimanche 02:00-04:00 UTC (rolling updates).

## 6. Coûts estimés (production initiale)

| Composant | Coût mensuel (USD) |
|-----------|-------------------|
| EKS cluster (6 nodes) | ~$1 200 |
| PostgreSQL HA (3 nodes) | ~$800 |
| Redis Cluster | ~$300 |
| MSK Kafka | ~$500 |
| S3 + backup | ~$200 |
| Cloudflare Pro | ~$200 |
| Monitoring (Grafana Cloud) | ~$100 |
| **Total estimé** | **~$3 300/mois** |

Scale An 1 (1M users) : ~$8 000-12 000/mois.

## 7. Bonnes pratiques bancaires

1. **Segregation of duties** — Aucun dev n'a accès prod DB
2. **Four-eyes principle** — Opérations admin > seuil require 2 approvers
3. **Immutable infrastructure** — Pas de SSH prod, GitOps only
4. **Change management** — Tout deploy tracé, rollback < 5 min
5. **Reconciliation** — Batch nightly : ledger vs external providers
6. **Incident response** — Playbook 24/7, post-mortem blameless
7. **Vendor management** — Due diligence partenaires (mobile money, KYC)
8. **Business continuity** — DR test trimestriel documenté

## 8. Roadmap technique

| Phase | Durée | Livrables |
|-------|-------|-----------|
| **MVP** | 3 mois | Auth, Wallet, P2P transfer, KYC L1 |
| **Beta** | +2 mois | Mobile money, QR payment, fraud basic |
| **GA Afrique** | +2 mois | Full compliance, multi-région, cards |
| **Scale** | +6 mois | International, banking integration, ML fraud |
