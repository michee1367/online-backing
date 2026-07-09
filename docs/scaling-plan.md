# Plan de montée en charge — VillageSat

## 1. Objectifs capacity

| Horizon | Utilisateurs | TPS peak | Transactions/jour |
|---------|-------------|----------|---------------------|
| Lancement | 100K | 50 | 500K |
| An 1 | 1M | 500 | 5M |
| An 3 | 5M | 2 500 | 25M |
| An 5 | 10M+ | 5 000 | 50M+ |

## 2. Dimensionnement initial (production)

### Kubernetes cluster (af-south-1)

| Node pool | Instance type | Count | Usage |
|-----------|--------------|-------|-------|
| General | m6i.xlarge (4vCPU, 16GB) | 6-20 (HPA) | Services app |
| Database | r6i.2xlarge | 3 | PostgreSQL Patroni |
| Cache | r6i.large | 3 | Redis Cluster |
| Kafka | m6i.2xlarge | 3 | Kafka brokers |

### HPA (Horizontal Pod Autoscaler)

```yaml
metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Pods
    pods:
      metric:
        name: http_requests_per_second
      target:
        type: AverageValue
        averageValue: "100"
```

## 3. Stratégies de scaling

### Application layer
- **Stateless services** : scale horizontal illimité
- **Connection pooling** : PgBouncer (max 200 conn/service)
- **Cache** : Redis pour soldes (TTL 30s) + invalidation event-driven
- **Read replicas** : requêtes GET sur replicas PostgreSQL

### Database layer
- **Partitionnement** : pruning automatique partitions anciennes
- **Index** : monitoring pg_stat_user_indexes, reindex mensuel
- **Sharding futur** : par tenant_id si > 10M users (Citus)

### Kafka
- Partitions : `transaction.events` = 24 partitions (scale consumers)
- Compression : lz4
- Batch size optimisé pour throughput

## 4. Load testing

| Outil | Scénario | Fréquence |
|-------|----------|-----------|
| k6 | API load test | Pre-release |
| Gatling | Transaction stress test | Monthly |
| Chaos Mesh | Pod kill, network partition | Quarterly |

Targets :
- p99 latency < 500ms @ 2000 RPS
- 0 transaction loss under failover
- Recovery < 30s after pod kill

## 5. CDN & Edge

- **Cloudflare** : DDoS protection, WAF, CDN static assets
- **GeoDNS** : routing Afrique → af-south-1
- **API caching** : Kong proxy-cache pour endpoints read-only (catalogues, taux de change)

## 6. Cost optimization

- Spot instances pour workers non-critiques (reporting batch)
- Reserved instances pour database (1 an)
- S3 Intelligent-Tiering pour archives audit
- Auto-scaling down off-peak (nuit UTC+1)

## 7. Bottlenecks anticipés & mitigations

| Bottleneck | Signal | Mitigation |
|------------|--------|------------|
| DB write contention | Lock waits | Partition ledger, async outbox |
| Fraud scoring latency | p99 > 200ms | Pre-compute profiles, cache rules |
| Kafka lag | Consumer lag > 10K | Scale consumers, increase partitions |
| Keycloak | Login latency | Dedicated cluster, cache tokens |
