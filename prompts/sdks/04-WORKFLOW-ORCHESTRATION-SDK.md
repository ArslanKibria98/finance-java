# ⏱️ Prompt 04: Workflow Orchestration SDK Implementation

**Objective**: Implement the `workflow-orchestration-sdk` for Temporal.io workflow orchestration and SAGA patterns.

**Prerequisites**:
- ✅ Prompt 00-03 complete

---

## 📚 Reference Documents

**IMPORTANT**: Read these documents thoroughly before proceeding.

### Master Blueprint
- `/var/www/docs/islamic-financing/master-blueprint/01_ARCHITECTURE_PRINCIPLES.md`
  - Section: Orchestration-First (Not Choreography)

- `/var/www/docs/islamic-financing/master-blueprint/06_ORCHESTRATION_PATTERNS.md`
  - Temporal workflow definitions
  - SAGA compensation patterns
  - Long-running workflow patterns
  - Human-in-the-loop signals

- `/var/www/docs/islamic-financing/master-blueprint/02_TECHNOLOGY_STACK.md`
  - Temporal.io 1.32.1 configuration

---

## 🎯 Implementation Requirements

### Technologies
- **Temporal Java SDK**: 1.32.1
- **Temporal Spring Boot Starter**: 1.32.1

### What to Implement

#### 1. Temporal Configuration (in `config/`)
- `TemporalConfig` - Client connection to Temporal server (localhost:7233)
- `WorkerConfig` - Worker factory, register task queues
- `DataConverterConfig` - Custom serialization for domain objects

#### 2. Activity Base Classes (in `activity/`)
- `BaseActivity` - Base class with logging, heartbeat, context access
- `ActivityRetryPolicy` - Standard retry policies:
  - `defaultRetry()` - 5 attempts, exponential backoff
  - `aggressiveRetry()` - 10 attempts for critical ops
  - `conservativeRetry()` - 3 attempts for expensive ops
  - `externalApiRetry()` - 7 attempts for API calls

#### 3. SAGA Pattern (in `saga/`)
- `SagaWorkflow` - Base interface for SAGA workflows
- `SagaStep` - Forward action + compensation action
- `CompensationAction` - Interface for rollback logic
- `SagaOrchestrator` - Execute steps sequentially, compensate on failure
- Example: `LoanDisbursementSaga` with 3 steps:
  1. Reserve funds in wallet
  2. Update loan status
  3. Transfer funds to customer

#### 4. Workflow Versioning (in `versioning/`)
- `WorkflowVersion` - Utilities for version checks
- `VersionedWorkflow` - Support backward compatibility
- Use `Workflow.getVersion()` for gradual rollout

#### 5. Common Utilities (in `common/`)
- `TaskQueue` - Standard task queue names for all services
- `WorkflowIdGenerator` - Generate unique workflow IDs
- `WorkflowContext` - Workflow metadata and tenant context
- `TemporalMetrics` - Prometheus metrics for workflows

#### 6. Exception Handling (in `exception/`)
- `WorkflowExecutionException`
- `ActivityFailureException`
- `CompensationFailureException`

---

## 🧪 Testing Requirements

- Use `TestWorkflowEnvironment` from Temporal SDK
- Test SAGA compensation on step failure
- Test workflow versioning scenarios
- Test activity retry policies
- Test long-running workflows with signals

---

## ✅ Success Criteria

- [ ] Temporal client connects to server
- [ ] Workers register and start successfully
- [ ] Activities execute with retry policies
- [ ] SAGA compensates on any step failure
- [ ] Workflow versioning supports multiple versions running concurrently
- [ ] All workflows are deterministic (no random/time calls in workflow code)
- [ ] Tests pass: `mvn test -pl shared-libraries/workflow-orchestration-sdk`
- [ ] Build succeeds: `mvn clean install -pl shared-libraries/workflow-orchestration-sdk`

---

## 🔄 Next Step

After completing this SDK, proceed to:
- **Prompt 05**: `lms-adapter-sdk` implementation
