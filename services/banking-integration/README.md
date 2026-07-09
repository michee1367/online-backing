# Banking Integration

Virements bancaires (SWIFT, ACH, SEPA, LOCAL) et gestion des comptes bancaires liés.

## Fonctionnalités

- Liaison de comptes bancaires externes
- Vérification des comptes liés
- Virements entrants/sortants via SWIFT, ACH, SEPA ou local
- Suivi des virements et statuts

## API

| Méthode | Chemin | Description |
|---------|--------|-------------|
| POST | `/api/v1/banking/accounts/link` | Lier un compte bancaire |
| GET | `/api/v1/banking/accounts` | Lister mes comptes liés |
| DELETE | `/api/v1/banking/accounts/{id}` | Supprimer un compte lié |
| POST | `/api/v1/banking/transfers/initiate` | Initier un virement |
| GET | `/api/v1/banking/transfers` | Lister mes virements |
| GET | `/api/v1/banking/transfers/{id}` | Détail virement |

Port : **8093**

## Types de virements

| Type | Description |
|------|-------------|
| SWIFT | Virement international |
| ACH | Automated Clearing House |
| SEPA | Espace européen de paiements |
| LOCAL | Virement local |

## Architecture

```
domain/
  model/        → LinkedBankAccount, BankTransfer, enums
  port/in/      → BankingUseCase, commands
  port/out/     → repositories, gateway, event publisher
application/
  service/      → BankingService
adapter/
  in/web/       → controllers REST
  out/persistence/ → JPA entities, repositories
  out/gateway/  → SimulatedBankGateway
  out/messaging/ → Kafka event publisher
```

## Tests

```bash
mvn -pl services/banking-integration -am test
```
