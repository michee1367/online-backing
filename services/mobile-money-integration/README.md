# Mobile Money Integration

Intégration avec les providers mobile money africains : M-Pesa, Orange Money, MTN MoMo, Airtel Money.

## Fonctionnalités

- Dépôts depuis mobile money vers wallet VillageSat
- Retraits depuis wallet vers mobile money
- Callback webhook pour confirmation asynchrone des providers
- Liste des providers actifs
- Suivi des transactions

## API

| Méthode | Chemin | Description |
|---------|--------|-------------|
| POST | `/api/v1/mobile-money/deposit` | Initier un dépôt |
| POST | `/api/v1/mobile-money/withdraw` | Initier un retrait |
| GET | `/api/v1/mobile-money/transactions` | Lister mes transactions |
| GET | `/api/v1/mobile-money/transactions/{id}` | Détail transaction |
| POST | `/internal/mobile-money/callback` | Webhook provider |
| GET | `/api/v1/mobile-money/providers` | Providers actifs |

Port : **8092**

## Providers supportés

| Provider | Sandbox URL |
|----------|-------------|
| M-Pesa | sandbox.safaricom.co.ke |
| Orange Money | api.orange.com/orange-money-webpay |
| MTN MoMo | sandbox.momodeveloper.mtn.com |
| Airtel Money | openapi.airtel.africa |

## Architecture

```
domain/
  model/        → MobileMoneyTransaction, ProviderConfig, enums
  port/in/      → MobileMoneyUseCase, commands
  port/out/     → repositories, gateway, event publisher
application/
  service/      → MobileMoneyService
adapter/
  in/web/       → controllers REST
  out/persistence/ → JPA entities, repositories
  out/gateway/  → SimulatedMobileMoneyGateway
  out/messaging/ → Kafka event publisher
  out/wallet/   → SimulatedWalletCreditAdapter
```

## Tests

```bash
mvn -pl services/mobile-money-integration -am test
```
