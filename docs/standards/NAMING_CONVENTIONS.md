# Naming Conventions - Single Source of Truth

> ALL agents and commands MUST follow these naming conventions. Do NOT define naming rules inline in commands - reference this document instead.

## Java Class Naming

| Type | Pattern | Example | Location |
|------|---------|---------|----------|
| Aggregate Root | `{Name}Aggregate` | `LoanAggregate` | `domain/model/` |
| Domain Entity | `{Name}Entity` | `RepaymentEntity` | `domain/model/` |
| Value Object ID | `{Name}Id` | `LoanId` | `domain/model/` |
| Status Enum | `{Name}Status` | `LoanStatus` | `domain/model/` |
| Domain Event | `{Aggregate}{PastTense}` | `LoanDisbursed` | `domain/model/` (record) |
| Domain Service | `{Name}DomainService` | `CreditDomainService` | `domain/service/` |
| Input Port | `{Verb}{Name}UseCase` | `ManageLoanUseCase` | `domain/port/in/` |
| Output Port | `{Name}Repository` | `LoanRepository` | `domain/port/out/` |
| Event Publisher | `EventPublisher` | `EventPublisher` | `domain/port/out/` |
| Use Case Impl | `{Verb}{Name}UseCaseImpl` | `ManageLoanUseCaseImpl` | `application/usecase/` |
| DTO | `{Name}Dto` | `LoanDto` | `application/dto/` |
| Mapper | `{Name}Mapper` | `LoanMapper` | `application/mapper/` |
| JPA Entity | `{Name}JpaEntity` | `LoanJpaEntity` | `infrastructure/persistence/entity/` |
| JPA Repository | `Jpa{Name}Repository` | `JpaLoanRepository` | `infrastructure/persistence/repository/` |
| Repo Impl | `{Name}RepositoryImpl` | `LoanRepositoryImpl` | `infrastructure/persistence/repository/` |
| Persistence Mapper | `{Name}PersistenceMapper` | `LoanPersistenceMapper` | `infrastructure/persistence/mapper/` |
| Kafka Publisher | `Kafka{Name}Publisher` | `KafkaLoanPublisher` | `infrastructure/messaging/` |
| Controller | `{Name}Controller` | `LoanController` | `adapter/rest/controller/` |
| Request | `{Verb}{Name}Request` | `CreateLoanRequest` | `adapter/rest/request/` |
| Response | `{Name}Response` | `LoanResponse` | `adapter/rest/response/` |
| Temporal Activity | `{Name}Activity` | `DisbursementActivity` | `adapter/temporal/activity/` |
| Activity Impl | `{Name}ActivityImpl` | `DisbursementActivityImpl` | `adapter/temporal/activity/` |
| Temporal Workflow | `{Name}Workflow` | `LoanOriginationWorkflow` | shared activity-api SDK |
| Workflow Impl | `{Name}WorkflowImpl` | `LoanOriginationWorkflowImpl` | service workflow module |

## Package Naming
```
com.ksa.financing.{servicename}         # Service root (no hyphens)
com.ksa.financing.{servicename}.domain  # Domain layer
com.ksa.financing.domain                # domain-core-sdk
com.ksa.financing.infra                 # foundational-infra-sdk
com.ksa.islamic.messaging              # messaging-event-sdk
com.ksa.islamic.workflow               # workflow-orchestration-sdk
com.ksa.financing.lms                   # lms-adapter-sdk
com.ksa.financing.compliance            # compliance-localization-sdk
```

## Database Naming

| Type | Pattern | Example |
|------|---------|---------|
| Database | `{service}_db` | `lending_db` |
| Table | `snake_case` (plural) | `loan_applications` |
| Column | `snake_case` | `created_at` |
| Primary Key | `id` | `id UUID PRIMARY KEY` |
| Foreign Key | `{referenced_table}_id` | `customer_id` |
| Index | `idx_{table}_{columns}` | `idx_loans_tenant_status` |
| Enum Type | `{name}_status` | `loan_status` |
| Migration | `V{n}__{description}.sql` | `V1__initial_schema.sql` |
| Tenant Schema | `tenant_{code}_{service}` | `tenant_001_lending` |

## Kafka Naming

| Type | Pattern | Example |
|------|---------|---------|
| Topic | `financing.{aggregate}.{event-past-tense}` | `financing.loan.disbursed` |
| Dead Letter | `{topic}.DLT` | `financing.loan.disbursed.DLT` |
| Consumer Group | `${spring.application.name}` | `lending-service` |
| Avro Schema | `{AggregateEvent}.avsc` | `LoanDisbursed.avsc` |

## Temporal Naming

| Type | Pattern | Example |
|------|---------|---------|
| Task Queue | `${spring.application.name}-queue` | `lending-service-queue` |
| Workflow ID | `{workflow}-{entity-id}` | `loan-origination-uuid` |
| Activity Interface | `{Name}Activity` | `LoanActivity` |
| Activity API SDK | `{service}-activity-api` | `lending-activity-api` |

## REST API Naming

| Type | Pattern | Example |
|------|---------|---------|
| Base Path | `/api/v{n}/{resource}` | `/api/v1/loans` |
| Create | `POST /api/v1/{resource}` | `POST /api/v1/loans` |
| Read | `GET /api/v1/{resource}/{id}` | `GET /api/v1/loans/{id}` |
| Update | `PUT /api/v1/{resource}/{id}` | `PUT /api/v1/loans/{id}` |
| Delete | `DELETE /api/v1/{resource}/{id}` | `DELETE /api/v1/loans/{id}` |
| List | `GET /api/v1/{resource}` | `GET /api/v1/loans` |
| Action | `POST /api/v1/{resource}/{id}/{action}` | `POST /api/v1/loans/{id}/disburse` |

## File/Directory Naming

| Type | Pattern | Example |
|------|---------|---------|
| Service directory | `kebab-case` | `lending-service` |
| Config files | `kebab-case` | `application-dev.yml` |
| Shell scripts | `kebab-case` | `start-infra.sh` |
| Documentation | `UPPER_SNAKE_CASE.md` or `kebab-case.md` | `ENVIRONMENT_CONFIG.md` |
