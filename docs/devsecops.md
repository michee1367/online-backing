# DevSecOps — VillageSat

## 1. Pipeline CI/CD (GitHub Actions)

```
┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐
│  Lint   │──►│  Test   │──►│  Scan   │──►│  Build  │──►│ Deploy  │
│  SAST   │   │  Unit   │   │  Trivy  │   │  Docker │   │  K8s    │
│         │   │  Integ  │   │  Snyk   │   │  Cosign │   │ B/G     │
└─────────┘   └─────────┘   └─────────┘   └─────────┘   └─────────┘
```

### Stages

| Stage | Outils | Gate |
|-------|--------|------|
| **Lint** | Checkstyle, SpotBugs, ESLint | Block on error |
| **SAST** | Semgrep, CodeQL | Block critical |
| **Test** | JUnit 5, Testcontainers, JaCoCo | Coverage > 80% |
| **Scan deps** | Trivy, Snyk | Block critical CVE |
| **Scan secrets** | Gitleaks, TruffleHog | Block any leak |
| **Build** | Maven, Docker multi-stage | - |
| **Sign** | Cosign (Sigstore) | Required for deploy |
| **Deploy staging** | Helm → staging cluster | Auto on main |
| **Deploy prod** | Helm → prod (blue/green) | Manual approval |

Pipeline : [`.github/workflows/ci-cd.yml`](../.github/workflows/ci-cd.yml)

## 2. Déploiement Blue/Green

```yaml
# Rolling update avec maxUnavailable=0, maxSurge=1
# Blue/Green via Argo Rollouts ou flag Helm
strategy:
  blueGreen:
  activeService: wallet-service-active
  previewService: wallet-service-preview
  autoPromotionEnabled: false  # Manual promote after smoke tests
  scaleDownDelaySeconds: 300
```

Smoke tests post-deploy : health check + transaction test synthétique.

## 3. Monitoring (Prometheus + Grafana)

### Métriques clés (SLIs)

| SLI | Target SLO |
|-----|------------|
| API latency p99 | < 500ms |
| Transaction success rate | > 99.9% |
| Auth success rate | > 99.5% |
| Error rate 5xx | < 0.1% |

### Dashboards Grafana

- **Overview** : RPS, latency, error rate par service
- **Transactions** : TPS, success/fail, fraud blocks
- **Infrastructure** : CPU, memory, pod restarts
- **Security** : failed logins, rate limit hits, fraud alerts

### Alerting (PagerDuty/Opsgenie)

| Alert | Severity | Condition |
|-------|----------|-----------|
| HighErrorRate | P1 | 5xx > 1% for 5min |
| TransactionFailureSpike | P1 | fail rate > 5% |
| DatabaseReplicationLag | P2 | lag > 30s |
| FraudAlertBurst | P2 | > 100 alerts/hour |
| CertificateExpiry | P3 | < 14 days |

## 4. Logs (ELK)

```
Filebeat (sidecar) → Logstash → Elasticsearch → Kibana
```

- Retention hot : 30 jours
- Retention cold (S3) : 1 an
- Index pattern : `villagesat-{service}-{date}`

## 5. Disaster Recovery

| Scénario | RTO | RPO | Procédure |
|----------|-----|-----|-----------|
| AZ failure | 5 min | 0 | Auto failover Patroni/K8s |
| Region failure | 15 min | 0 | DNS failover → eu-west-1 |
| Data corruption | 1 hour | 1 hour | Point-in-time recovery |
| Ransomware | 4 hours | 24 hours | Restore from immutable backup |

Runbook : `docs/runbooks/disaster-recovery.md`

## 6. Backup automatique

- **PostgreSQL** : pgBackRest → S3 (cross-region replication)
- **Redis** : RDB snapshots every 6h + AOF
- **Kafka** : MirrorMaker 2 cross-region
- **Vault** : Raft snapshot daily
- **MinIO/S3** : Versioning + Object Lock

## 7. Environnements

| Env | Cluster | Usage |
|-----|---------|-------|
| `dev` | local (docker-compose) | Développement |
| `staging` | K8s staging | QA, penetration test |
| `prod-af` | K8s af-south-1 | Production Afrique |
| `prod-eu` | K8s eu-west-1 | Production EU + DR |
