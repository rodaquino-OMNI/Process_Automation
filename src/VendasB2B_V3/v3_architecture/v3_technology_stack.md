# V3 Technology Stack Specification
**AUSTA B2B Sales Automation Platform - Version 3**

**Document Purpose**: Define all technology choices with architectural rationale

**Date**: 2025-12-08
**Status**: APPROVED - Implementation Ready
**Version**: 1.0.0

---

## EXECUTIVE SUMMARY

The V3 technology stack is designed for **enterprise-grade reliability, scalability, and maintainability**. It combines proven open-source technologies (Camunda, PostgreSQL, Kubernetes) with modern development practices (microservices, API-first, cloud-native).

**Core Principles**:
1. **Openness**: No vendor lock-in, open-source first
2. **Scalability**: Horizontal scaling via Kubernetes
3. **Resilience**: Circuit breakers, retries, compensation
4. **Observability**: Metrics, logs, traces (OpenTelemetry)
5. **Security**: Zero-trust, encryption at rest/transit, RBAC

---

## ARCHITECTURE LAYERS

### Layer 1: Process Orchestration
### Layer 2: Integration & Services
### Layer 3: Data Persistence
### Layer 4: Infrastructure & Runtime
### Layer 5: Monitoring & Observability
### Layer 6: Security & Compliance

---

## LAYER 1: PROCESS ORCHESTRATION

### Camunda BPM Platform 7.x

**Version**: 7.19+ (latest stable)
**License**: Apache 2.0
**Deployment**: Self-hosted (Kubernetes)

#### Rationale
- **BPMN 2.0 Standard**: Industry-standard process notation
- **DMN Support**: Business rule automation
- **Mature Ecosystem**: 10+ years of production use
- **Scalability**: Proven at 10,000+ concurrent instances
- **Extensibility**: Java/Spring Boot delegate pattern
- **Community**: Active community, enterprise support available

#### Components

**Camunda Engine**:
- Process execution engine (BPMN, DMN, CMMN)
- Job executor (async tasks, timers, retries)
- History service (audit trail)
- External task client (worker pattern)

**Camunda Tasklist**:
- User task interface
- Form rendering (embedded HTML5 forms)
- Task assignment and completion
- Filtering and sorting

**Camunda Cockpit**:
- Process monitoring dashboard
- Instance inspection (variable viewer)
- Incident management
- Performance analytics

**Camunda Admin**:
- User/group management
- Authorization (RBAC)
- Tenant management (multi-tenancy)
- System health monitoring

#### Configuration

```yaml
# application.yml
camunda:
  bpm:
    admin-user:
      id: admin
      password: ${CAMUNDA_ADMIN_PASSWORD}

    database:
      type: postgres
      schema-update: false  # Managed via Flyway migrations

    job-execution:
      enabled: true
      deployment-aware: true
      max-jobs-per-acquisition: 10
      wait-time-in-millis: 5000
      backoff-time-in-millis: 0
      max-backoff: 10000
      lock-time-in-millis: 300000  # 5 minutes

    metrics:
      enabled: true
      reporter-activate: true

    history-level: FULL  # Audit trail requirement

    auto-deployment-enabled: false  # Controlled deployments
```

#### Deployment Artifacts

**BPMN Files** (14 total):
1. Main orchestrator: `main-orchestrator-v3.bpmn` (6250 lines XML)
2-14. Subprocesses: 13 files (avg 400 lines each)

**DMN Files** (8 total):
1. `decision_meddic_score.dmn`
2. `decision_lead_fit_score.dmn`
3. `decision_pricing_calculation.dmn`
4. `decision_approval_level.dmn`
5. `decision_needs_analysis.dmn`
6. `decision_kpi_analysis.dmn`
7. `decision_expansion_opportunities.dmn`
8. `decision_risk_assessment.dmn`

**Forms** (50+ HTML5 forms):
- Lead intake form
- Discovery meeting form
- Proposal configuration form
- Approval forms (4 tiers)
- Negotiation tracking form
- Contract review form
- Implementation planning form
- Onboarding data form
- Digital services setup form
- Monitoring checkin forms
- Expansion proposal form
- (40+ additional forms in subprocesses)

---

## LAYER 2: INTEGRATION & SERVICES

### 2.1 Application Framework

**Spring Boot 3.x**

**Version**: 3.2+ (with Java 21)
**License**: Apache 2.0

#### Rationale
- **Camunda Integration**: Official Spring Boot Starter
- **Dependency Injection**: Simplified delegate development
- **Auto-Configuration**: Minimal boilerplate
- **Production-Ready**: Actuator endpoints (health, metrics)
- **Ecosystem**: Vast library ecosystem

#### Key Dependencies

```xml
<!-- pom.xml -->
<dependencies>
  <!-- Camunda Spring Boot Starter -->
  <dependency>
    <groupId>org.camunda.bpm.springboot</groupId>
    <artifactId>camunda-bpm-spring-boot-starter-webapp</artifactId>
    <version>7.19.0</version>
  </dependency>

  <!-- Spring Boot Starter Web -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>

  <!-- Spring Boot Starter Data JPA -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
  </dependency>

  <!-- Spring Boot Starter Security -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
  </dependency>

  <!-- Spring Boot Actuator -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
  </dependency>
</dependencies>
```

### 2.2 External Service Integration

#### Integration Pattern: External Task Workers

**Camunda External Task Client**:
- **Pattern**: Poll-based workers (long polling)
- **Concurrency**: Configurable thread pools per topic
- **Retry**: Configurable retry strategies (R3/PT5M default)
- **Scalability**: Horizontal scaling (multiple worker instances)

#### 7 External Systems

**1. CRM System (Salesforce / HubSpot)**

**Integration Type**: REST API
**Authentication**: OAuth2
**Operations**: 15 service tasks

| Operation | Topic | Purpose | SLA |
|-----------|-------|---------|-----|
| Create Opportunity | `crm-create-opportunity` | Lead → Opportunity conversion | <2s |
| Update Opportunity | `crm-update-opportunity` | Status changes | <2s |
| Get Opportunity | `crm-get-opportunity` | Data retrieval | <1s |
| Create Contact | `crm-create-contact` | Contact management | <2s |
| Update Contact | `crm-update-contact` | Contact updates | <2s |
| Log Activity | `crm-log-activity` | Activity tracking | <1s |
| Create Task | `crm-create-task` | Follow-up tasks | <1s |
| Update Deal Stage | `crm-update-stage` | Pipeline progression | <2s |
| Mark Closed Won | `crm-close-won` | Deal closure | <2s |
| Mark Closed Lost | `crm-close-lost` | Loss analysis | <2s |
| Create Note | `crm-create-note` | Documentation | <1s |
| Attach Document | `crm-attach-document` | File attachment | <3s |
| Get Account | `crm-get-account` | Account data | <1s |
| Update Account | `crm-update-account` | Account updates | <2s |
| Log Expansion | `crm-log-expansion` | Expansion tracking | <2s |

**Worker Configuration**:
```yaml
# application-crm-worker.yml
camunda:
  external-task:
    workers:
      crm:
        topics:
          - crm-create-opportunity
          - crm-update-opportunity
          # ... (13 more topics)
        max-tasks: 10
        lock-duration: 60000  # 1 minute
        retry-timeout: 5000   # 5 seconds
```

**2. ERP System (TOTVS / SAP)**

**Integration Type**: SOAP + REST hybrid
**Authentication**: API Key + Certificate
**Operations**: 8 service tasks

| Operation | Topic | Purpose | SLA |
|-----------|-------|---------|-----|
| Create Customer | `erp-create-customer` | Master data creation | <5s |
| Update Customer | `erp-update-customer` | Master data update | <5s |
| Create Contract | `erp-create-contract` | Contract setup | <5s |
| Update Contract | `erp-update-contract` | Contract changes | <5s |
| Create Billing | `erp-create-billing` | Billing setup | <5s |
| Post Revenue | `erp-post-revenue` | Revenue recognition | <5s |
| Generate Invoice | `erp-generate-invoice` | Invoicing | <10s |
| Check Credit | `erp-check-credit` | Credit limit check | <3s |

**3. ANS (Brazilian Health Insurance Regulator)**

**Integration Type**: SOAP Web Service (XML)
**Authentication**: ICP-Brasil digital certificate
**Operations**: 2 service tasks (MANDATORY)

| Operation | Topic | Purpose | SLA | Compliance |
|-----------|-------|---------|-----|------------|
| Register Beneficiaries | `ans-register-beneficiaries` | Beneficiary registration | 72 hours | MANDATORY |
| Report Plan Changes | `ans-report-plan-changes` | Plan modifications | 30 days | MANDATORY |

**ANS Submission Format**:
```xml
<!-- ANS XML Standard (simplified) -->
<ansRegistro xmlns="http://www.ans.gov.br/schema">
  <cabecalho>
    <operadoraRegistro>123456</operadoraRegistro>
    <dataEnvio>2025-12-08T10:00:00Z</dataEnvio>
    <assinaturaDigital>ICP-Brasil Certificate</assinaturaDigital>
  </cabecalho>
  <beneficiarios>
    <beneficiario>
      <cpf>123.456.789-00</cpf>
      <nome>João Silva</nome>
      <dataNascimento>1980-01-01</dataNascimento>
      <plano>Referência</plano>
      <dataInclusao>2025-12-15</dataInclusao>
    </beneficiario>
    <!-- ... more beneficiaries -->
  </beneficiarios>
</ansRegistro>
```

**4-7. Other Systems** (similar patterns):
- **Digital Services Platform**: REST API (OAuth2), 7 operations
- **Document Management**: REST API (API key), 4 operations
- **E-Signature**: REST API + Webhooks (DocuSign/Clicksign), 3 operations
- **Communications**: SMTP + SMS API + WhatsApp Business API, 10+ operations

### 2.3 API Layer

**REST API Framework**: Spring Boot + Spring Web MVC

**API Design**:
- RESTful principles (resources, HTTP verbs)
- JSON payload (snake_case naming)
- Versioning: URL-based (/api/v3/)
- Pagination: Offset-based (limit/offset params)
- Filtering: Query params
- Sorting: sort_by/sort_order params

**API Security**:
- OAuth2 + JWT tokens
- API rate limiting (100 req/min per client)
- IP whitelisting (for external systems)
- CORS configuration (frontend origins)

**API Documentation**: OpenAPI 3.0 (Swagger UI)

```yaml
# Sample API Specification (OpenAPI 3.0)
openapi: 3.0.0
info:
  title: AUSTA B2B Sales API
  version: 3.0.0
  description: REST API for B2B sales process orchestration

paths:
  /api/v3/processes/{processInstanceId}:
    get:
      summary: Get process instance details
      parameters:
        - name: processInstanceId
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          description: Process instance found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProcessInstance'

components:
  schemas:
    ProcessInstance:
      type: object
      properties:
        id:
          type: string
        businessKey:
          type: string
        processDefinitionId:
          type: string
        startTime:
          type: string
          format: date-time
        endTime:
          type: string
          format: date-time
        variables:
          type: object
```

### 2.4 Message Queue (Asynchronous Communication)

**Apache Kafka**

**Version**: 3.5+
**License**: Apache 2.0

#### Rationale
- **High Throughput**: 1M+ messages/second
- **Durability**: Message persistence (configurable retention)
- **Scalability**: Horizontal partitioning
- **Event Sourcing**: Replay capability
- **Integration**: Spring Kafka support

#### Use Cases

1. **Process Event Streaming**:
   - Process started
   - Phase completed
   - SLA breached
   - Error occurred
   - Deal closed

2. **External System Events**:
   - CRM opportunity updated (webhook)
   - Contract signed (DocuSign webhook)
   - Payment received (ERP event)
   - Beneficiary added (ANS confirmation)

3. **Notification Delivery**:
   - Email queue
   - SMS queue
   - Push notification queue
   - WhatsApp message queue

**Kafka Topics**:
```
austa.process.events (process lifecycle events)
austa.sla.breaches (SLA monitoring)
austa.errors (error tracking)
austa.notifications.email
austa.notifications.sms
austa.notifications.push
austa.crm.webhooks (CRM events)
austa.erp.events (ERP events)
austa.signature.webhooks (E-signature events)
```

**Kafka Configuration**:
```yaml
# application-kafka.yml
spring:
  kafka:
    bootstrap-servers: kafka:9092
    consumer:
      group-id: austa-sales-automation
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

---

## LAYER 3: DATA PERSISTENCE

### 3.1 Relational Database

**PostgreSQL 14+**

**Version**: 14.x or 15.x
**License**: PostgreSQL License (open source)

#### Rationale
- **ACID Compliance**: Strong consistency guarantees
- **JSON Support**: Native JSON/JSONB data types (flexible schema)
- **Performance**: Advanced indexing (B-tree, GiST, GIN)
- **Scalability**: Replication (streaming, logical)
- **Extensions**: PostGIS, pg_cron, pg_stat_statements
- **Camunda Support**: Officially supported by Camunda

#### Database Schema

**Camunda Tables** (created by Camunda engine):
- `ACT_RE_*`: Repository (process definitions, DMN tables)
- `ACT_RU_*`: Runtime (active process instances, tasks, variables)
- `ACT_HI_*`: History (audit trail, completed processes)
- `ACT_ID_*`: Identity (users, groups, memberships)
- `ACT_GE_*`: General (byte arrays, properties)

**Application Tables**:
```sql
-- Custom business data tables

-- Client companies
CREATE TABLE companies (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(255) NOT NULL,
  industry VARCHAR(100),
  company_size INTEGER,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

-- Contracts
CREATE TABLE contracts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  company_id UUID NOT NULL REFERENCES companies(id),
  contract_value NUMERIC(15,2) NOT NULL,
  plan_type VARCHAR(50),
  start_date DATE NOT NULL,
  end_date DATE,
  status VARCHAR(50) DEFAULT 'active',
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

-- Beneficiaries
CREATE TABLE beneficiaries (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  contract_id UUID NOT NULL REFERENCES contracts(id),
  cpf VARCHAR(14) UNIQUE NOT NULL,
  name VARCHAR(255) NOT NULL,
  birth_date DATE NOT NULL,
  relationship VARCHAR(50), -- titular|dependent
  health_card_number VARCHAR(50) UNIQUE,
  ans_registration_id VARCHAR(100),
  created_at TIMESTAMP DEFAULT NOW()
);

-- Process metrics (for analytics)
CREATE TABLE process_metrics (
  id BIGSERIAL PRIMARY KEY,
  process_instance_id VARCHAR(64) NOT NULL,
  process_definition_key VARCHAR(255),
  start_time TIMESTAMP NOT NULL,
  end_time TIMESTAMP,
  duration_ms BIGINT,
  outcome VARCHAR(50), -- won|lost|error
  score_meddic INTEGER,
  opportunity_value NUMERIC(15,2),
  final_value NUMERIC(15,2),
  created_at TIMESTAMP DEFAULT NOW()
);

-- Audit log
CREATE TABLE audit_log (
  id BIGSERIAL PRIMARY KEY,
  process_instance_id VARCHAR(64),
  phase VARCHAR(50),
  action VARCHAR(255),
  user_id VARCHAR(100),
  details JSONB, -- Flexible schema for action-specific data
  created_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_companies_name ON companies(name);
CREATE INDEX idx_contracts_company_id ON contracts(company_id);
CREATE INDEX idx_contracts_status ON contracts(status);
CREATE INDEX idx_beneficiaries_contract_id ON beneficiaries(contract_id);
CREATE INDEX idx_beneficiaries_cpf ON beneficiaries(cpf);
CREATE INDEX idx_process_metrics_definition_key ON process_metrics(process_definition_key);
CREATE INDEX idx_process_metrics_outcome ON process_metrics(outcome);
CREATE INDEX idx_audit_log_process_instance_id ON audit_log(process_instance_id);
CREATE INDEX idx_audit_log_phase ON audit_log(phase);
```

#### Database Configuration

```yaml
# application-database.yml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/austa_sales
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQL10Dialect
    show-sql: false
    hibernate:
      ddl-auto: none  # Managed via Flyway
    properties:
      hibernate:
        format_sql: true
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

#### High Availability

**Replication**:
- **Master-Slave Replication** (streaming replication)
- **Read Replicas**: 2 read-only replicas for reporting queries
- **Failover**: Automatic failover (Patroni + etcd)

**Backup Strategy**:
- **Full Backup**: Daily at 2 AM (pg_basebackup)
- **WAL Archiving**: Continuous (Point-in-Time Recovery)
- **Retention**: 30 days
- **Storage**: AWS S3 or Azure Blob

---

## LAYER 4: INFRASTRUCTURE & RUNTIME

### 4.1 Container Orchestration

**Kubernetes (K8s)**

**Version**: 1.28+
**Managed Service**: AWS EKS, Azure AKS, or Google GKE

#### Rationale
- **Auto-Scaling**: Horizontal Pod Autoscaler (HPA)
- **Self-Healing**: Automatic pod restarts
- **Service Discovery**: DNS-based service naming
- **Load Balancing**: Built-in load balancer
- **Rolling Updates**: Zero-downtime deployments
- **Resource Management**: CPU/memory limits, quotas

#### Deployment Architecture

```yaml
# Kubernetes Deployment Manifest (Camunda Engine)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: camunda-engine
  namespace: austa-sales
spec:
  replicas: 3  # 3 engine instances for HA
  selector:
    matchLabels:
      app: camunda-engine
  template:
    metadata:
      labels:
        app: camunda-engine
    spec:
      containers:
      - name: camunda
        image: camunda/camunda-bpm-platform:7.19.0-jdk21
        ports:
        - containerPort: 8080
        env:
        - name: DB_DRIVER
          value: org.postgresql.Driver
        - name: DB_URL
          value: jdbc:postgresql://postgres:5432/austa_sales
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: postgres-credentials
              key: username
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: postgres-credentials
              key: password
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /engine-rest/engine
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /engine-rest/engine
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: camunda-engine
  namespace: austa-sales
spec:
  selector:
    app: camunda-engine
  ports:
  - protocol: TCP
    port: 8080
    targetPort: 8080
  type: ClusterIP
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: camunda-engine-hpa
  namespace: austa-sales
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: camunda-engine
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

#### Infrastructure Components

**Deployed Services** (Kubernetes):
1. **Camunda Engine** (3-10 replicas, HPA-enabled)
2. **External Task Workers** (5-20 replicas per worker type, HPA-enabled)
3. **API Gateway** (Kong/Nginx Ingress, 2-5 replicas)
4. **PostgreSQL** (StatefulSet, 1 master + 2 replicas)
5. **Kafka** (StatefulSet, 3 brokers)
6. **Redis** (StatefulSet, 1 master + 2 replicas) - for caching
7. **Prometheus** (Deployment, 1 replica) - metrics
8. **Grafana** (Deployment, 1 replica) - dashboards

**Resource Allocation** (per environment):

| Environment | vCPU | Memory | Storage | Nodes |
|-------------|------|--------|---------|-------|
| **Development** | 8 | 16 GB | 100 GB | 2 |
| **Staging** | 16 | 32 GB | 500 GB | 3 |
| **Production** | 64 | 128 GB | 2 TB | 5+ |

### 4.2 API Gateway

**Kong Gateway** (or Nginx Ingress)

**Version**: 3.4+
**License**: Apache 2.0

#### Rationale
- **Routing**: Path-based routing to services
- **Authentication**: OAuth2, JWT validation
- **Rate Limiting**: Request throttling per client
- **Caching**: Response caching (Redis backend)
- **Logging**: Centralized access logs
- **Plugins**: Extensible (Lua scripts)

#### Configuration

```yaml
# Kong Ingress Example
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: austa-sales-api
  namespace: austa-sales
  annotations:
    konghq.com/plugins: rate-limiting, cors, jwt
spec:
  ingressClassName: kong
  rules:
  - host: api.austa.com.br
    http:
      paths:
      - path: /api/v3/
        pathType: Prefix
        backend:
          service:
            name: camunda-engine
            port:
              number: 8080
---
# Rate Limiting Plugin
apiVersion: configuration.konghq.com/v1
kind: KongPlugin
metadata:
  name: rate-limiting
  namespace: austa-sales
plugin: rate-limiting
config:
  minute: 100
  policy: redis
  redis_host: redis
  redis_port: 6379
---
# JWT Plugin
apiVersion: configuration.konghq.com/v1
kind: KongPlugin
metadata:
  name: jwt
  namespace: austa-sales
plugin: jwt
config:
  key_claim_name: kid
  secret_is_base64: false
```

---

## LAYER 5: MONITORING & OBSERVABILITY

### 5.1 Metrics & Alerting

**Prometheus + Grafana**

**Prometheus Version**: 2.45+
**Grafana Version**: 10.0+

#### Rationale
- **Pull-Based Metrics**: Prometheus scrapes metrics from services
- **Time-Series Database**: Efficient storage for metrics
- **PromQL**: Powerful query language
- **Alerting**: Alert rules with notification channels
- **Grafana Dashboards**: Rich visualization

#### Metrics Collected

**Camunda Engine Metrics**:
```
# Process instance metrics
camunda_process_instances_started_total{process_definition_key}
camunda_process_instances_completed_total{process_definition_key}
camunda_process_instances_active{process_definition_key}

# User task metrics
camunda_user_tasks_created_total
camunda_user_tasks_completed_total
camunda_user_tasks_active

# Job metrics
camunda_jobs_failed_total
camunda_jobs_successful_total
camunda_jobs_execution_time_seconds

# Incident metrics
camunda_incidents_open{incident_type}
camunda_incidents_resolved{incident_type}
```

**Application Metrics** (Spring Boot Actuator + Micrometer):
```
# JVM metrics
jvm_memory_used_bytes{area}
jvm_gc_pause_seconds{action,cause}
jvm_threads_live

# HTTP metrics
http_server_requests_seconds{method,status,uri}
http_client_requests_seconds{method,status,uri}

# Database metrics
hikaricp_connections_active
hikaricp_connections_pending
hikaricp_connections_timeout_total

# Kafka metrics
kafka_consumer_fetch_manager_records_consumed_total{topic}
kafka_producer_record_send_total{topic}
```

**Business Metrics** (custom):
```
# Process outcomes
austa_process_outcome_total{outcome} # won|lost|error

# Cycle time
austa_process_duration_days{process_definition_key}

# Phase duration
austa_phase_duration_seconds{phase}

# MEDDIC scores
austa_meddic_score{qualification_level}

# Revenue
austa_revenue_total{contract_type}
austa_expansion_revenue_total
```

#### Alert Rules

```yaml
# prometheus-alerts.yml
groups:
- name: CamundaEngine
  interval: 30s
  rules:
  - alert: HighProcessErrorRate
    expr: rate(camunda_process_instances_completed_total{outcome="error"}[5m]) > 0.05
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "High process error rate detected"
      description: "Error rate is {{ $value | humanizePercentage }} over the last 5 minutes"

  - alert: SLABreachRateHigh
    expr: rate(austa_sla_breach_total[1h]) > 0.1
    for: 10m
    labels:
      severity: warning
    annotations:
      summary: "High SLA breach rate"
      description: "SLA breach rate is {{ $value | humanizePercentage }} over the last hour"

  - alert: DatabaseConnectionPoolExhausted
    expr: hikaricp_connections_pending > 5
    for: 2m
    labels:
      severity: critical
    annotations:
      summary: "Database connection pool exhausted"
      description: "{{ $value }} connections are pending in the pool"

  - alert: JobExecutionFailureRateHigh
    expr: rate(camunda_jobs_failed_total[5m]) / rate(camunda_jobs_successful_total[5m]) > 0.1
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "High job execution failure rate"
      description: "Job failure rate is {{ $value | humanizePercentage }}"
```

#### Grafana Dashboards

**Dashboard 1: Process Overview**
- Total process instances (started, completed, active)
- Conversion funnel (qualified → closed won)
- Average cycle time (days)
- Process outcome distribution (won vs. lost)

**Dashboard 2: Phase Performance**
- Time spent per phase (bar chart)
- SLA compliance per phase (heatmap)
- Active tasks per phase (table)
- Error rate per phase (time series)

**Dashboard 3: Resource Utilization**
- JVM memory usage (time series)
- CPU usage (time series)
- Database connections (gauge)
- Kafka lag (time series)

**Dashboard 4: Business KPIs**
- Revenue generated (counter)
- Expansion revenue (counter)
- Average MEDDIC score (gauge)
- NPS score (gauge)
- Account health distribution (pie chart)

### 5.2 Logging

**Elastic Stack (ELK)**

**Elasticsearch Version**: 8.x
**Logstash Version**: 8.x
**Kibana Version**: 8.x

#### Rationale
- **Centralized Logging**: All application logs in one place
- **Full-Text Search**: Quickly find logs by keyword
- **Log Aggregation**: Combine logs from multiple services
- **Visualization**: Kibana dashboards
- **Alerting**: Elasticsearch alerting rules

#### Log Format

**Structured Logging** (JSON format):
```json
{
  "timestamp": "2025-12-08T10:00:00.123Z",
  "level": "INFO",
  "logger": "com.austa.sales.delegate.ProcessInitializationDelegate",
  "thread": "http-nio-8080-exec-1",
  "message": "Process initialized: OpportunityId=123e4567, AssignedRep=john.doe, Priority=high",
  "context": {
    "processInstanceId": "process-123",
    "businessKey": "LEAD-2025-001",
    "phase": "initialization",
    "userId": "admin"
  },
  "trace": {
    "traceId": "abc123",
    "spanId": "def456"
  }
}
```

#### Log Levels

- **ERROR**: Unrecoverable errors (external service failure, database error)
- **WARN**: Recoverable errors (retry triggered, SLA breach)
- **INFO**: Process milestones (process started, phase completed)
- **DEBUG**: Detailed execution (variable changes, gateway decisions) - disabled in production
- **TRACE**: Very detailed (every activity start/end) - disabled in production

### 5.3 Distributed Tracing

**OpenTelemetry + Jaeger**

**OpenTelemetry Version**: 1.x
**Jaeger Version**: 1.50+

#### Rationale
- **Request Tracing**: Trace requests across microservices
- **Performance Profiling**: Identify slow operations
- **Dependency Analysis**: Understand service dependencies
- **Root Cause Analysis**: Trace errors back to source

#### Trace Example

```
Trace: Process Lead (LEAD-2025-001)
├─ Span: POST /api/v3/processes (150ms)
│  ├─ Span: Create CRM Opportunity (80ms)
│  │  └─ Span: HTTP POST api.salesforce.com (75ms)
│  ├─ Span: Assign Sales Team (20ms)
│  └─ Span: Start Process Instance (50ms)
├─ Span: Qualification Phase (7 days)
│  ├─ Span: First Contact (2 days)
│  ├─ Span: Deep Research (1 day)
│  │  └─ Span: Company Data API (500ms)
│  ├─ Span: Discovery Meeting (4 days)
│  └─ Span: MEDDIC Calculation (100ms)
│     └─ Span: DMN Evaluation (80ms)
└─ ...
```

---

## LAYER 6: SECURITY & COMPLIANCE

### 6.1 Authentication & Authorization

**OAuth2 + JWT**

**Identity Provider**: Keycloak or Auth0
**Token Format**: JWT (JSON Web Token)

#### Rationale
- **Stateless**: No server-side session storage
- **Standardized**: OAuth2 industry standard
- **Scalable**: JWT can be validated without database lookup
- **Fine-Grained**: Role-based access control (RBAC)

#### User Roles

| Role | Permissions | Assigned To |
|------|-------------|-------------|
| **Sales Representative** | Execute qualification, discovery, proposal tasks | Sales team |
| **Sales Manager** | Execute manager-level approvals, review proposals | Sales management |
| **Commercial Director** | Execute director-level approvals, strategic decisions | Commercial directors |
| **CEO/Board** | Execute C-level approvals, emergency overrides | Executive board |
| **Customer Success** | Execute monitoring, expansion tasks | Customer success team |
| **Operations** | Execute implementation, onboarding tasks | Operations team |
| **Legal Counsel** | Execute contract review tasks | Legal department |
| **System Admin** | Full access, user management | IT administrators |

#### Authorization Configuration

```yaml
# application-security.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://auth.austa.com.br/realms/sales
          jwk-set-uri: https://auth.austa.com.br/realms/sales/protocol/openid-connect/certs

camunda:
  bpm:
    authorization:
      enabled: true
      enable-custom-password-policy: true
    admin-user:
      id: admin
      password: ${CAMUNDA_ADMIN_PASSWORD}
      groups:
        - camunda-admin
```

### 6.2 Data Encryption

**Encryption at Rest**:
- **Database**: PostgreSQL pgcrypto extension (AES-256)
- **File Storage**: AWS S3 server-side encryption (SSE-S3)
- **Backup**: Encrypted backups (GPG encryption)

**Encryption in Transit**:
- **HTTPS**: TLS 1.3 (Let's Encrypt certificates)
- **API Calls**: All external API calls over HTTPS
- **Database Connections**: SSL-enabled PostgreSQL connections

### 6.3 Compliance

**LGPD (Lei Geral de Proteção de Dados - Brazilian GDPR)**:
- **Data Minimization**: Collect only necessary PII
- **Consent Management**: Explicit consent for data processing
- **Right to Erasure**: Data deletion upon request
- **Data Portability**: Export user data in standard format
- **Data Breach Notification**: 72-hour notification requirement

**ANS Compliance** (Brazilian Health Insurance):
- **Beneficiary Registration**: 72-hour deadline (enforced by process)
- **Digital Signature**: ICP-Brasil certificate (enforced by integration)
- **Plan Code Validation**: ANS-approved plan codes (enforced by DMN rules)
- **Audit Trail**: Full history of ANS submissions (audit_log table)

---

## DEPLOYMENT STRATEGY

### Environment Tiers

**Development**:
- Local Docker Compose
- H2 in-memory database (for rapid testing)
- Mock external services (WireMock)
- Single-node Kafka

**Staging**:
- Kubernetes cluster (3 nodes)
- PostgreSQL (1 master + 1 replica)
- Sandboxed external services (test accounts)
- 3-broker Kafka cluster

**Production**:
- Kubernetes cluster (5+ nodes, multi-AZ)
- PostgreSQL (1 master + 2 replicas, automated failover)
- Real external services (production accounts)
- 5-broker Kafka cluster (multi-AZ)
- CDN (CloudFront/Cloudflare) for static assets

### CI/CD Pipeline

**Tools**: GitHub Actions + ArgoCD

**Pipeline Stages**:
1. **Build**: Maven build, run unit tests
2. **Test**: Integration tests, BPMN validation
3. **Scan**: Security scanning (Snyk, Trivy)
4. **Package**: Docker image build
5. **Push**: Push to container registry
6. **Deploy (Staging)**: ArgoCD auto-sync
7. **Smoke Tests**: Basic health checks
8. **Deploy (Production)**: Manual approval → ArgoCD sync
9. **Monitor**: Watch metrics for 1 hour

**Deployment Strategy**: Blue-Green Deployment
- Deploy new version to "green" environment
- Run smoke tests on green
- Switch traffic from blue to green (instant cutover)
- Keep blue as rollback option for 24 hours

---

## COST ESTIMATION (Monthly)

**Infrastructure** (AWS pricing example):

| Component | Specification | Cost (USD) |
|-----------|---------------|-----------|
| **EKS Cluster** | 5 m5.xlarge nodes | $730 |
| **RDS PostgreSQL** | db.r5.xlarge (HA) | $680 |
| **ElastiCache Redis** | cache.m5.large | $123 |
| **Managed Kafka (MSK)** | 3 kafka.m5.large | $810 |
| **S3 Storage** | 2 TB (backups, docs) | $46 |
| **Data Transfer** | 5 TB outbound | $450 |
| **CloudWatch Logs** | 100 GB ingestion | $51 |
| **Route 53** | Hosted zone + queries | $10 |
| **ACM Certificates** | Free | $0 |
| **Total Infrastructure** | | **$2,900/month** |

**Third-Party SaaS**:

| Service | Plan | Cost (USD) |
|---------|------|-----------|
| **Salesforce/HubSpot** | Enterprise (100 users) | $3,000 |
| **DocuSign** | Business Pro (50 envelopes/mo) | $600 |
| **Twilio** | SMS + WhatsApp | $500 |
| **SendGrid** | Email (100K emails/mo) | $90 |
| **Datadog** | APM + Logs (optional) | $800 |
| **Total SaaS** | | **$4,990/month** |

**Grand Total**: ~$7,890/month (~R$ 39,450/month at 5:1 exchange rate)

---

**Document Version**: 1.0.0
**Last Updated**: 2025-12-08
**Status**: APPROVED - Implementation Ready
**Next Review**: Quarterly (technology evaluation)

---

*Technology choices will be reviewed quarterly to incorporate new releases and industry best practices.*
