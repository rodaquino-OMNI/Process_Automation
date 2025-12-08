# V3 C4 Architecture Diagrams
**AUSTA B2B Sales Automation Platform - Version 3**

**Document Purpose**: Visualize system architecture using C4 model (Context, Container, Component, Code)

**Date**: 2025-12-08
**Status**: APPROVED - Implementation Ready
**Version**: 1.0.0

---

## ABOUT C4 MODEL

The C4 model provides a hierarchical set of architectural diagrams:
- **Level 1 - Context**: System in its environment (users, external systems)
- **Level 2 - Container**: High-level technology choices (applications, databases)
- **Level 3 - Component**: Internal structure of containers (modules, classes)
- **Level 4 - Code**: Class diagrams (optional, usually UML)

---

## LEVEL 1: SYSTEM CONTEXT DIAGRAM

**Purpose**: Show how the AUSTA B2B Sales Automation System fits into its environment

```mermaid
C4Context
    title System Context - AUSTA B2B Sales Automation Platform V3

    Person(salesRep, "Sales Representative", "Executes qualification, discovery, proposal tasks")
    Person(salesManager, "Sales Manager", "Approves proposals, reviews progress")
    Person(director, "Commercial Director", "Approves high-value deals, strategic decisions")
    Person(executive, "C-Level Executive", "Approves mega deals, oversees performance")
    Person(customerSuccess, "Customer Success Manager", "Monitors accounts, identifies expansion")
    Person(operations, "Operations Team", "Implements solutions, onboards beneficiaries")
    Person(legal, "Legal Counsel", "Reviews contracts, ensures compliance")

    System(austaSystem, "AUSTA B2B Sales Automation System", "Automates end-to-end B2B sales lifecycle: lead → expansion")

    Person_Ext(client, "Client (B2B Company)", "Receives proposals, signs contracts, uses health insurance")

    System_Ext(crmSystem, "CRM System", "Salesforce/HubSpot - Opportunity management")
    System_Ext(erpSystem, "ERP System", "TOTVS/SAP - Financial integration, billing")
    System_Ext(ansSystem, "ANS System", "Brazilian health insurance regulator - Beneficiary registration")
    System_Ext(digitalServices, "Digital Services Platform", "User provisioning, mobile app, telemedicine")
    System_Ext(documentMgmt, "Document Management", "Alfresco/SharePoint - Proposal/contract storage")
    System_Ext(eSignature, "E-Signature Platform", "DocuSign/Clicksign - Contract signing")
    System_Ext(communications, "Communications Platform", "Email (SendGrid), SMS (Twilio), WhatsApp")

    Rel(salesRep, austaSystem, "Executes tasks, enters data", "HTTPS/Web UI")
    Rel(salesManager, austaSystem, "Reviews, approves", "HTTPS/Web UI")
    Rel(director, austaSystem, "Approves", "HTTPS/Web UI")
    Rel(executive, austaSystem, "Approves, monitors", "HTTPS/Web UI")
    Rel(customerSuccess, austaSystem, "Monitors, expands", "HTTPS/Web UI")
    Rel(operations, austaSystem, "Implements, onboards", "HTTPS/Web UI")
    Rel(legal, austaSystem, "Reviews contracts", "HTTPS/Web UI")

    Rel(austaSystem, client, "Sends proposals, requests signatures", "Email, SMS, WhatsApp")
    Rel(client, austaSystem, "Responds to proposals", "Email")

    Rel(austaSystem, crmSystem, "Creates/updates opportunities", "REST API, OAuth2")
    Rel(austaSystem, erpSystem, "Creates customers, posts revenue", "SOAP/REST, API Key")
    Rel(austaSystem, ansSystem, "Registers beneficiaries", "SOAP, ICP-Brasil Certificate")
    Rel(austaSystem, digitalServices, "Provisions accounts, activates services", "REST API, OAuth2")
    Rel(austaSystem, documentMgmt, "Stores proposals/contracts", "REST API, API Key")
    Rel(austaSystem, eSignature, "Requests signatures", "REST API + Webhooks")
    Rel(austaSystem, communications, "Sends notifications", "SMTP, REST API")

    UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="2")
```

**Key Insights**:
- **7 user personas** (internal stakeholders with different roles)
- **1 external persona** (client companies)
- **7 external systems** (integrations critical for process execution)
- **Authentication**: HTTPS + OAuth2 for all external APIs
- **Compliance**: ANS integration is regulatory requirement (ICP-Brasil certificate)

---

## LEVEL 2: CONTAINER DIAGRAM

**Purpose**: Show high-level technology choices and how containers communicate

```mermaid
C4Container
    title Container Diagram - AUSTA B2B Sales Automation Platform V3

    Person(user, "System User", "Sales team, managers, operations")
    Person_Ext(client, "Client", "B2B company decision maker")

    System_Boundary(c1, "AUSTA B2B Sales Automation System") {
        Container(webUI, "Web Application", "React.js, Nginx", "User interface for task execution, form submission")
        Container(apiGateway, "API Gateway", "Kong Gateway", "API routing, rate limiting, JWT validation")

        Container(camundaEngine, "Camunda BPM Engine", "Java 21, Spring Boot 3", "Process orchestration (13 subprocesses), user task management")

        ContainerDb(postgres, "Process Database", "PostgreSQL 14", "Stores process instances, tasks, variables, audit trail")

        Container(externalWorkers, "External Task Workers", "Java 21, Spring Boot 3", "Service task execution (CRM, ERP, ANS, etc.)")

        Container(kafka, "Message Queue", "Apache Kafka", "Event streaming, async communication, notifications")

        ContainerDb(redis, "Cache", "Redis", "Session storage, API response caching")

        Container(prometheus, "Metrics Collector", "Prometheus", "Scrapes metrics from all services")
        Container(grafana, "Dashboards", "Grafana", "Visualizes metrics, process KPIs")
        Container(elk, "Log Aggregator", "Elasticsearch + Logstash + Kibana", "Centralized logging, log search")
        Container(jaeger, "Distributed Tracing", "Jaeger", "Request tracing across services")
    }

    System_Ext(crmSystem, "CRM System", "Salesforce/HubSpot")
    System_Ext(erpSystem, "ERP System", "TOTVS/SAP")
    System_Ext(ansSystem, "ANS System", "Brazilian regulator")
    System_Ext(digitalServices, "Digital Services", "User provisioning platform")
    System_Ext(documentMgmt, "Document Management", "Alfresco/SharePoint")
    System_Ext(eSignature, "E-Signature", "DocuSign/Clicksign")
    System_Ext(communications, "Communications", "SendGrid, Twilio")

    Rel(user, webUI, "Uses", "HTTPS")
    Rel(webUI, apiGateway, "API calls", "HTTPS/JSON")
    Rel(apiGateway, camundaEngine, "Routes to", "HTTP/REST")

    Rel(camundaEngine, postgres, "Reads/writes", "JDBC, SQL")
    Rel(camundaEngine, externalWorkers, "Fetches external tasks", "REST API")
    Rel(camundaEngine, kafka, "Publishes events", "Kafka Protocol")

    Rel(externalWorkers, crmSystem, "Integrates", "REST, OAuth2")
    Rel(externalWorkers, erpSystem, "Integrates", "SOAP/REST")
    Rel(externalWorkers, ansSystem, "Integrates", "SOAP, Certificate")
    Rel(externalWorkers, digitalServices, "Integrates", "REST, OAuth2")
    Rel(externalWorkers, documentMgmt, "Integrates", "REST, API Key")
    Rel(externalWorkers, eSignature, "Integrates", "REST, Webhooks")
    Rel(externalWorkers, communications, "Integrates", "SMTP, REST")

    Rel(kafka, communications, "Triggers notifications", "Consumer")

    Rel(camundaEngine, prometheus, "Exposes metrics", "/actuator/prometheus")
    Rel(externalWorkers, prometheus, "Exposes metrics", "/actuator/prometheus")
    Rel(prometheus, grafana, "Feeds data", "PromQL")

    Rel(camundaEngine, elk, "Ships logs", "Logstash")
    Rel(externalWorkers, elk, "Ships logs", "Logstash")

    Rel(camundaEngine, jaeger, "Sends traces", "OpenTelemetry")
    Rel(externalWorkers, jaeger, "Sends traces", "OpenTelemetry")

    Rel(apiGateway, redis, "Caches responses", "Redis Protocol")

    UpdateLayoutConfig($c4ShapeInRow="4", $c4BoundaryInRow="3")
```

**Key Insights**:
- **Separation of Concerns**: Camunda Engine focuses on orchestration, External Workers handle integrations
- **Async Communication**: Kafka decouples notification delivery from process execution
- **Caching Layer**: Redis reduces load on Camunda Engine and external APIs
- **Observability Stack**: Prometheus + Grafana + ELK + Jaeger provide full visibility
- **API Gateway**: Centralized entry point for authentication, rate limiting, routing

---

## LEVEL 3: COMPONENT DIAGRAM (Camunda Engine Container)

**Purpose**: Show internal components of the Camunda BPM Engine container

```mermaid
C4Component
    title Component Diagram - Camunda BPM Engine Container

    Container_Boundary(c1, "Camunda BPM Engine") {
        Component(restApi, "REST API", "Spring Web MVC", "Exposes process engine operations via REST")

        Component(processEngine, "Process Engine", "Camunda Core", "BPMN process execution, state management")
        Component(jobExecutor, "Job Executor", "Async Threads", "Executes timers, async tasks, retries")
        Component(taskService, "Task Service", "Camunda API", "User task lifecycle management")
        Component(runtimeService, "Runtime Service", "Camunda API", "Process instance operations")
        Component(historyService, "History Service", "Camunda API", "Audit trail, completed processes")
        Component(repositoryService, "Repository Service", "Camunda API", "Process definition deployment")

        Component(dmnEngine, "DMN Engine", "Camunda DMN", "Business rule evaluation (8 decision tables)")

        Component(initDelegate, "Process Initialization Delegate", "Java, Spring Bean", "CRM integration, team assignment")
        Component(slaDelegate, "SLA Notification Delegate", "Java, Spring Bean", "SLA breach notifications")
        Component(errorDelegate, "Error Handling Delegate", "Java, Spring Bean", "Centralized error logging")
        Component(dealWonDelegate, "Deal Won Notification Delegate", "Java, Spring Bean", "Victory notifications")
        Component(dealLostDelegate, "Deal Lost Notification Delegate", "Java, Spring Bean", "Loss analysis notifications")

        ComponentDb(processDB, "Process Database", "PostgreSQL", "ACT_* tables (runtime, history, identity)")
    }

    Container_Ext(externalWorkers, "External Task Workers", "7 worker types for CRM, ERP, ANS, etc.")
    Container_Ext(kafka, "Message Queue", "Event streaming")
    Container_Ext(prometheus, "Metrics Collector", "Scrapes /actuator/prometheus")

    Rel(restApi, processEngine, "Uses", "Java API")
    Rel(restApi, taskService, "Uses", "Java API")
    Rel(restApi, runtimeService, "Uses", "Java API")
    Rel(restApi, historyService, "Uses", "Java API")
    Rel(restApi, repositoryService, "Uses", "Java API")

    Rel(processEngine, jobExecutor, "Schedules jobs", "Internal queue")
    Rel(processEngine, dmnEngine, "Evaluates decisions", "DMN API")

    Rel(processEngine, initDelegate, "Executes", "Delegate Expression")
    Rel(processEngine, slaDelegate, "Executes", "Execution Listener")
    Rel(processEngine, errorDelegate, "Executes", "Delegate Expression")
    Rel(processEngine, dealWonDelegate, "Executes", "Execution Listener")
    Rel(processEngine, dealLostDelegate, "Executes", "Execution Listener")

    Rel(processEngine, processDB, "Persists state", "JDBC/Hibernate")
    Rel(taskService, processDB, "CRUD tasks", "JDBC")
    Rel(runtimeService, processDB, "CRUD instances", "JDBC")
    Rel(historyService, processDB, "Queries history", "JDBC")
    Rel(repositoryService, processDB, "Deploys definitions", "JDBC")

    Rel(jobExecutor, externalWorkers, "Fetches external tasks", "REST API")

    Rel(initDelegate, kafka, "Publishes events", "Kafka Producer")
    Rel(slaDelegate, kafka, "Publishes alerts", "Kafka Producer")

    Rel(processEngine, prometheus, "Exposes metrics", "Micrometer")

    UpdateLayoutConfig($c4ShapeInRow="4", $c4BoundaryInRow="2")
```

**Key Insights**:
- **Core Services**: 6 Camunda services (Process, Task, Runtime, History, Repository, DMN)
- **Delegates**: 5 custom Java delegates for critical integration points
- **Async Execution**: Job Executor handles timers, retries, and external task polling
- **DMN Integration**: Business rules externalized to 8 DMN decision tables
- **Event Publishing**: Delegates publish events to Kafka for notification delivery

---

## LEVEL 3: COMPONENT DIAGRAM (External Task Workers Container)

**Purpose**: Show internal components of the External Task Workers container

```mermaid
C4Component
    title Component Diagram - External Task Workers Container

    Container_Boundary(c1, "External Task Workers") {
        Component(crmWorker, "CRM Worker", "Java, Camunda External Task Client", "15 topics: create-opportunity, update-opportunity, close-won, etc.")
        Component(erpWorker, "ERP Worker", "Java, Camunda External Task Client", "8 topics: create-customer, post-revenue, generate-invoice, etc.")
        Component(ansWorker, "ANS Worker", "Java, Camunda External Task Client", "2 topics: register-beneficiaries, report-plan-changes")
        Component(digitalWorker, "Digital Services Worker", "Java, Camunda External Task Client", "7 topics: create-accounts, configure-app, activate-portal, etc.")
        Component(documentWorker, "Document Worker", "Java, Camunda External Task Client", "4 topics: generate-proposal, generate-contract, store-document, etc.")
        Component(signatureWorker, "E-Signature Worker", "Java, Camunda External Task Client", "3 topics: request-signature, validate-signature, retrieve-signed, etc.")
        Component(commWorker, "Communications Worker", "Java, Camunda External Task Client", "10+ topics: send-email, send-sms, send-whatsapp, etc.")

        Component(crmClient, "CRM Client", "REST Client, Feign", "Salesforce/HubSpot API client")
        Component(erpClient, "ERP Client", "SOAP/REST Client", "TOTVS/SAP API client")
        Component(ansClient, "ANS Client", "SOAP Client", "ANS XML submission client with ICP-Brasil certificate")
        Component(digitalClient, "Digital Services Client", "REST Client", "Digital platform API client")
        Component(documentClient, "Document Management Client", "REST Client", "Alfresco/SharePoint API client")
        Component(signatureClient, "E-Signature Client", "REST Client + Webhook Receiver", "DocuSign/Clicksign API client")
        Component(emailClient, "Email Client", "SMTP Client", "SendGrid SMTP client")
        Component(smsClient, "SMS Client", "REST Client", "Twilio API client")
        Component(whatsappClient, "WhatsApp Client", "REST Client", "WhatsApp Business API client")

        Component(retryHandler, "Retry Handler", "Java, Custom Logic", "Exponential backoff, circuit breaker")
        Component(errorReporter, "Error Reporter", "Java, Kafka Producer", "Publishes errors to Kafka")
    }

    System_Ext(crmSystem, "CRM System", "Salesforce/HubSpot")
    System_Ext(erpSystem, "ERP System", "TOTVS/SAP")
    System_Ext(ansSystem, "ANS System", "Brazilian regulator")
    System_Ext(digitalServices, "Digital Services", "User provisioning")
    System_Ext(documentMgmt, "Document Management", "Alfresco/SharePoint")
    System_Ext(eSignature, "E-Signature", "DocuSign/Clicksign")
    Container_Ext(kafka, "Message Queue", "Error events, notifications")
    Container_Ext(camundaEngine, "Camunda Engine", "Fetches external tasks")

    Rel(crmWorker, camundaEngine, "Polls for tasks", "REST API")
    Rel(erpWorker, camundaEngine, "Polls for tasks", "REST API")
    Rel(ansWorker, camundaEngine, "Polls for tasks", "REST API")
    Rel(digitalWorker, camundaEngine, "Polls for tasks", "REST API")
    Rel(documentWorker, camundaEngine, "Polls for tasks", "REST API")
    Rel(signatureWorker, camundaEngine, "Polls for tasks", "REST API")
    Rel(commWorker, camundaEngine, "Polls for tasks", "REST API")

    Rel(crmWorker, crmClient, "Uses", "Method calls")
    Rel(erpWorker, erpClient, "Uses", "Method calls")
    Rel(ansWorker, ansClient, "Uses", "Method calls")
    Rel(digitalWorker, digitalClient, "Uses", "Method calls")
    Rel(documentWorker, documentClient, "Uses", "Method calls")
    Rel(signatureWorker, signatureClient, "Uses", "Method calls")
    Rel(commWorker, emailClient, "Uses", "Method calls")
    Rel(commWorker, smsClient, "Uses", "Method calls")
    Rel(commWorker, whatsappClient, "Uses", "Method calls")

    Rel(crmClient, crmSystem, "Calls API", "HTTPS, OAuth2")
    Rel(erpClient, erpSystem, "Calls API", "SOAP/REST")
    Rel(ansClient, ansSystem, "Submits XML", "SOAP, Certificate")
    Rel(digitalClient, digitalServices, "Calls API", "HTTPS, OAuth2")
    Rel(documentClient, documentMgmt, "Calls API", "HTTPS, API Key")
    Rel(signatureClient, eSignature, "Calls API + Receives webhooks", "HTTPS")
    Rel(emailClient, communications, "Sends email", "SMTP")
    Rel(smsClient, communications, "Sends SMS", "REST API")
    Rel(whatsappClient, communications, "Sends WhatsApp", "REST API")

    Rel(crmWorker, retryHandler, "Uses", "Method calls")
    Rel(erpWorker, retryHandler, "Uses", "Method calls")
    Rel(ansWorker, retryHandler, "Uses", "Method calls")

    Rel(crmWorker, errorReporter, "Reports failures", "Method calls")
    Rel(erpWorker, errorReporter, "Reports failures", "Method calls")
    Rel(ansWorker, errorReporter, "Reports failures", "Method calls")

    Rel(errorReporter, kafka, "Publishes errors", "Kafka Producer")

    UpdateLayoutConfig($c4ShapeInRow="4", $c4BoundaryInRow="2")
```

**Key Insights**:
- **7 Worker Types**: One per external system category
- **60+ External Task Topics**: Granular topic design for specific operations
- **API Clients**: Abstraction layer for external system APIs
- **Retry Logic**: Exponential backoff, circuit breaker pattern
- **Error Reporting**: Failures published to Kafka for alerting

---

## LEVEL 4: CODE DIAGRAM (Example: CRM Worker Class Structure)

**Purpose**: Show detailed class structure for one worker (optional, usually UML)

```mermaid
classDiagram
    class CrmWorker {
        -ExternalTaskClient taskClient
        -CrmApiClient crmClient
        -RetryHandler retryHandler
        -ErrorReporter errorReporter
        +start() void
        +registerHandlers() void
        -handleCreateOpportunity(ExternalTask, ExternalTaskService) void
        -handleUpdateOpportunity(ExternalTask, ExternalTaskService) void
        -handleCloseWon(ExternalTask, ExternalTaskService) void
    }

    class ExternalTaskClient {
        +baseUrl(String) ExternalTaskClient
        +maxTasks(int) ExternalTaskClient
        +lockDuration(long) ExternalTaskClient
        +asyncResponseTimeout(long) ExternalTaskClient
        +subscribe(String) TopicSubscriptionBuilder
        +build() ExternalTaskClient
    }

    class ExternalTask {
        +getId() String
        +getTopicName() String
        +getVariable(String) Object
        +getAllVariables() Map~String, Object~
        +getBusinessKey() String
        +getProcessInstanceId() String
    }

    class ExternalTaskService {
        +complete(ExternalTask) void
        +complete(ExternalTask, Map~String, Object~) void
        +handleFailure(ExternalTask, String, String, int, long) void
        +handleBpmnError(ExternalTask, String, String) void
    }

    class CrmApiClient {
        -RestTemplate restTemplate
        -String baseUrl
        -String accessToken
        +createOpportunity(OpportunityRequest) OpportunityResponse
        +updateOpportunity(String, OpportunityUpdate) OpportunityResponse
        +closeOpportunity(String, String) OpportunityResponse
        +getOpportunity(String) OpportunityResponse
    }

    class RetryHandler {
        -int maxRetries
        -long backoffMs
        +execute(Callable~T~) T
        +shouldRetry(Exception) boolean
        +calculateBackoff(int) long
    }

    class ErrorReporter {
        -KafkaTemplate kafkaTemplate
        -String errorTopic
        +reportError(String, Exception, Map~String, Object~) void
    }

    CrmWorker --> ExternalTaskClient : uses
    CrmWorker --> CrmApiClient : uses
    CrmWorker --> RetryHandler : uses
    CrmWorker --> ErrorReporter : uses
    CrmWorker ..> ExternalTask : handles
    CrmWorker ..> ExternalTaskService : calls
    CrmApiClient --> RestTemplate : uses
    ErrorReporter --> KafkaTemplate : uses
```

**Key Insights**:
- **Worker Pattern**: Poll-based external task execution
- **API Abstraction**: CrmApiClient encapsulates REST calls
- **Error Handling**: RetryHandler + ErrorReporter for resilience
- **Camunda Integration**: ExternalTaskClient + ExternalTaskService

---

## DEPLOYMENT DIAGRAM (Kubernetes)

**Purpose**: Show how containers are deployed in production environment

```mermaid
C4Deployment
    title Deployment Diagram - AUSTA B2B Sales Automation Platform V3 (Production)

    Deployment_Node(aws, "AWS Cloud", "Amazon Web Services") {
        Deployment_Node(eks, "EKS Cluster", "Kubernetes 1.28+") {
            Deployment_Node(webPod, "Web Application Pods", "2-5 replicas") {
                Container(webUI, "Web Application", "React.js + Nginx", "Static assets, SPA")
            }

            Deployment_Node(apiPod, "API Gateway Pods", "2-5 replicas") {
                Container(apiGateway, "API Gateway", "Kong Gateway", "Rate limiting, JWT validation")
            }

            Deployment_Node(enginePod, "Camunda Engine Pods", "3-10 replicas, HPA-enabled") {
                Container(camundaEngine, "Camunda BPM Engine", "Java 21, Spring Boot 3", "Process orchestration")
            }

            Deployment_Node(workerPod1, "CRM Worker Pods", "5-20 replicas, HPA-enabled") {
                Container(crmWorker, "CRM Worker", "Java 21, Spring Boot 3", "CRM integrations")
            }

            Deployment_Node(workerPod2, "ERP Worker Pods", "3-10 replicas, HPA-enabled") {
                Container(erpWorker, "ERP Worker", "Java 21, Spring Boot 3", "ERP integrations")
            }

            Deployment_Node(workerPod3, "ANS Worker Pods", "2-5 replicas, HPA-enabled") {
                Container(ansWorker, "ANS Worker", "Java 21, Spring Boot 3", "ANS compliance")
            }

            Deployment_Node(monitoringPod, "Monitoring Pods", "1-2 replicas") {
                Container(prometheus, "Prometheus", "Prometheus 2.45+", "Metrics collection")
                Container(grafana, "Grafana", "Grafana 10.0+", "Dashboards")
            }

            Deployment_Node(kafkaPod, "Kafka Pods", "5 brokers, StatefulSet, multi-AZ") {
                Container(kafka, "Apache Kafka", "Kafka 3.5+", "Event streaming")
            }
        }

        Deployment_Node(rds, "RDS", "Managed PostgreSQL") {
            ContainerDb(postgres, "PostgreSQL", "PostgreSQL 14, db.r5.xlarge, Multi-AZ", "Process database")
        }

        Deployment_Node(elasticache, "ElastiCache", "Managed Redis") {
            ContainerDb(redis, "Redis", "Redis 7, cache.m5.large", "Session cache, API cache")
        }

        Deployment_Node(s3, "S3", "Object Storage") {
            ContainerDb(s3Bucket, "Document Storage", "S3 Bucket, 2 TB", "Proposals, contracts, backups")
        }

        Deployment_Node(cloudfront, "CloudFront", "CDN") {
            Container(cdn, "CDN", "CloudFront distribution", "Static asset delivery")
        }
    }

    Rel(cloudfront, webPod, "Forwards requests", "HTTPS")
    Rel(webPod, apiPod, "API calls", "HTTPS")
    Rel(apiPod, enginePod, "Routes requests", "HTTP")
    Rel(enginePod, postgres, "Persists state", "JDBC/SSL")
    Rel(enginePod, redis, "Caches data", "Redis Protocol")
    Rel(enginePod, workerPod1, "Fetches tasks", "REST API")
    Rel(enginePod, workerPod2, "Fetches tasks", "REST API")
    Rel(enginePod, workerPod3, "Fetches tasks", "REST API")
    Rel(enginePod, kafkaPod, "Publishes events", "Kafka Protocol")
    Rel(workerPod1, s3Bucket, "Stores documents", "AWS SDK")
    Rel(prometheus, enginePod, "Scrapes metrics", "/actuator/prometheus")
    Rel(prometheus, workerPod1, "Scrapes metrics", "/actuator/prometheus")
    Rel(grafana, prometheus, "Queries metrics", "PromQL")

    UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="2")
```

**Key Insights**:
- **Kubernetes Orchestration**: Auto-scaling, self-healing, service discovery
- **Horizontal Scaling**: 3-10 engine pods, 5-20 worker pods (HPA-enabled)
- **Managed Services**: RDS PostgreSQL (Multi-AZ), ElastiCache Redis, S3
- **CDN**: CloudFront for static asset delivery (reduced latency)
- **Multi-AZ Deployment**: High availability across availability zones

---

## DATA FLOW DIAGRAMS

### Data Flow 1: Happy Path (Lead to Closed Won)

```mermaid
sequenceDiagram
    autonumber
    actor User as Sales Representative
    participant UI as Web Application
    participant API as API Gateway
    participant Engine as Camunda Engine
    participant DB as PostgreSQL
    participant Worker as CRM Worker
    participant CRM as CRM System

    User->>UI: Submit Lead Form
    UI->>API: POST /api/v3/processes/start
    API->>Engine: Start Process Instance
    Engine->>DB: Insert ACT_RU_EXECUTION
    Engine->>Worker: Fetch External Task (crm-create-opportunity)
    Worker->>CRM: POST /opportunities
    CRM-->>Worker: OpportunityId: 12345
    Worker->>Engine: Complete External Task
    Engine->>DB: Update Variables (opportunityId)
    Engine->>User: Assign User Task (Qualification)
    User->>UI: Complete Qualification Task
    UI->>API: POST /api/v3/tasks/{taskId}/complete
    API->>Engine: Complete Task
    Engine->>DB: Update ACT_HI_TASKINST
    Note over Engine: MEDDIC Score = 8 (qualified)
    Engine->>Engine: Evaluate Gateway (isQualified == true)
    Engine->>Engine: Start Discovery Phase
    Note over User,CRM: ... (11 more phases)
    Engine->>Worker: Fetch External Task (crm-close-won)
    Worker->>CRM: PATCH /opportunities/12345 {status: 'closed_won'}
    CRM-->>Worker: Success
    Worker->>Engine: Complete External Task
    Engine->>DB: Insert ACT_HI_PROCINST (outcome: 'won')
    Engine->>User: Process Completed (Deal Won)
```

### Data Flow 2: Error Handling and Compensation

```mermaid
sequenceDiagram
    autonumber
    participant Engine as Camunda Engine
    participant Worker as ANS Worker
    participant ANS as ANS System
    participant ErrorHandler as Error Handler Delegate
    participant Kafka as Message Queue
    participant Compensation as Compensation Tasks

    Engine->>Worker: Fetch External Task (ans-register-beneficiaries)
    Worker->>ANS: SOAP Request (beneficiary registration)
    ANS-->>Worker: Error: Invalid CPF format
    Worker->>Engine: Handle Failure (retries remaining: 2)
    Engine->>Worker: Fetch External Task (retry attempt)
    Worker->>ANS: SOAP Request (retry)
    ANS-->>Worker: Error: Invalid CPF format (persistent error)
    Worker->>Engine: Handle BPMN Error (ONBD_ERROR)
    Engine->>Engine: Trigger Error Boundary Event
    Engine->>ErrorHandler: Execute Error Handler Delegate
    ErrorHandler->>Kafka: Publish Error Event {phase: 'onboarding', error: 'ANS_ERROR'}
    ErrorHandler->>Engine: Error Logged
    Engine->>Compensation: Trigger Compensation Handlers (reverse order)
    Note over Compensation: 1. Compensate Digital Services (disable accounts)
    Note over Compensation: 2. Compensate Onboarding (cancel ANS submission)
    Note over Compensation: 3. Compensate Execution (rollback integrations)
    Note over Compensation: ... (8 more compensation tasks)
    Compensation-->>Engine: All Compensations Complete
    Engine->>Engine: End Process (ProcessError)
```

---

## SECURITY ARCHITECTURE DIAGRAM

```mermaid
graph TB
    subgraph Internet
        Client[Client Browser]
        MaliciousActor[Malicious Actor]
    end

    subgraph "Security Perimeter"
        WAF[Web Application Firewall]
        CDN[CloudFront CDN]
    end

    subgraph "API Gateway Layer"
        Kong[Kong Gateway]
        RateLimit[Rate Limiter]
        JWTValidator[JWT Validator]
    end

    subgraph "Application Layer"
        CamundaEngine[Camunda Engine]
        ExternalWorkers[External Task Workers]
    end

    subgraph "Data Layer"
        PostgreSQL[PostgreSQL<br/>Encrypted at Rest]
        Redis[Redis<br/>Session Storage]
    end

    subgraph "Identity Provider"
        Keycloak[Keycloak/Auth0<br/>OAuth2 + JWT]
    end

    Client -->|HTTPS| WAF
    MaliciousActor -->|Blocked| WAF
    WAF -->|DDoS Protection| CDN
    CDN -->|TLS 1.3| Kong
    Kong --> RateLimit
    RateLimit -->|100 req/min| JWTValidator
    JWTValidator -->|Validate Token| Keycloak
    Keycloak -->|Valid JWT| JWTValidator
    JWTValidator -->|Authorized| CamundaEngine
    CamundaEngine -->|JDBC/SSL| PostgreSQL
    CamundaEngine -->|TLS| ExternalWorkers
    ExternalWorkers -->|OAuth2| ExternalAPIs[External APIs<br/>CRM, ERP, ANS]

    style WAF fill:#ff6b6b
    style CDN fill:#4ecdc4
    style Kong fill:#95e1d3
    style Keycloak fill:#feca57
    style PostgreSQL fill:#ee5a6f
```

**Security Layers**:
1. **Perimeter**: WAF (DDoS protection, SQL injection blocking)
2. **CDN**: CloudFront (geo-blocking, origin shielding)
3. **Gateway**: Kong (rate limiting, JWT validation, IP whitelisting)
4. **Application**: RBAC (role-based access control)
5. **Data**: Encryption at rest (AES-256), encryption in transit (TLS 1.3)
6. **Identity**: OAuth2 + JWT (stateless authentication)

---

## SUMMARY OF DIAGRAMS

**5 C4 Diagrams Created**:
1. **Level 1 - Context**: 7 user personas, 7 external systems
2. **Level 2 - Container**: 11 containers (Camunda, PostgreSQL, Kafka, Redis, Prometheus, Grafana, ELK, Jaeger, Web UI, API Gateway, External Workers)
3. **Level 3 - Component (Camunda Engine)**: 13 components (6 services, 5 delegates, 1 DMN engine, 1 job executor)
4. **Level 3 - Component (External Workers)**: 7 workers, 9 API clients, 2 utilities
5. **Level 4 - Code**: CRM Worker class structure (9 classes)

**Additional Diagrams**:
- **Deployment Diagram**: Kubernetes architecture with AWS managed services
- **Data Flow Diagram (Happy Path)**: 12-step sequence diagram
- **Data Flow Diagram (Error Handling)**: 10-step sequence diagram with compensation
- **Security Architecture**: 6-layer security model

---

**Document Version**: 1.0.0
**Last Updated**: 2025-12-08
**Status**: APPROVED - Implementation Ready
**Diagram Format**: Mermaid (text-based, version-controllable)

---

*These diagrams will be maintained alongside code changes to reflect the current architecture.*
