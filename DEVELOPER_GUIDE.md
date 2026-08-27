# Developer Guide — Worker Portal

A complete record of the architecture, implementation decisions, and features
built during the development of the Worker Portal project.

---

## Table of Contents

1. [Overview](#overview)
2. [Technology Stack](#technology-stack)
3. [Architecture](#architecture)
4. [Original Application](#original-application)
5. [Apache Camel Integration](#apache-camel-integration)
6. [SOAP Message Translator — How It Works](#soap-message-translator--how-it-works)
7. [Authentication](#authentication)
8. [Filtering Workers](#filtering-workers)
9. [Project Layout](#project-layout)
10. [Build & Deployment](#build--deployment)
11. [Key Implementation Details](#key-implementation-details)
12. [Testing the SOAP Endpoint](#testing-the-soap-endpoint)
13. [Troubleshooting](#troubleshooting)

---

## Overview

Worker Portal is a Jakarta EE 11 web application deployed on Payara 7 with
PostgreSQL. It manages a list of workers (employees) with full CRUD operations.

The application exposes three interfaces:

- **Web UI** — JSP + Servlets at `/worker-portal/`
- **JSON REST API** — JAX-RS at `/api/workers`
- **SOAP/XML Translator** — Apache Camel-based endpoint at `/api/soap/translate`

All three interfaces share the same business logic layer (`WorkerService`).

---

## Technology Stack

| Component | Version | Purpose |
|---|---|---|
| Java | 17 | Language runtime |
| Jakarta EE | 11.0.0 | Servlets 6.1, JPA 3.2, JAX-RS, JAX-WS, CDI |
| Payara | 7.2026.6 | Application server |
| PostgreSQL | — | Database |
| EclipseLink | (via Payara) | JPA provider |
| Apache Camel | 4.22.0 | SOAP message translation middleware |
| SLF4J + Logback | 2.0.16 / 1.5.18 | Logging |
| Maven | — | Build tool |

### Why Apache Camel 4.22.0?

- Latest stable release with `jakarta.*` namespace support
- Payara 7 runs Jakarta EE 11 (not javax), so older Camel versions won't work
- `camel-cdi` was removed in Camel 4.x — we bootstrap manually via CDI events
- `camel-core` + `camel-direct` is all we need (no HTTP, no Spring)

---

## Architecture

```
External Client
      │
      │  POST /api/soap/translate?operation=getAllWorkers
      │  Content-Type: application/xml
      │  Authorization: Basic <base64>
      │
      ▼
┌─────────────────────────────────────────────┐
│  ApiAuthFilter (Basic Auth)                 │  ← HTTP-level authentication
│  Checks app_user table in PostgreSQL        │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│  SoapTranslatorResource (JAX-RS)            │  ← Entry point
│  @Path("/soap")                             │
│  Extracts operation + id query params       │
│  Sends raw SOAP XML to Camel via            │
│  ProducerTemplate.requestBodyAndHeaders()   │
└─────────────────┬───────────────────────────┘
                  │  direct:soap-translate
                  ▼
┌─────────────────────────────────────────────┐
│  Camel Route (SoapMessageTranslatorRoute)   │
│                                             │
│  1. SoapHeaderAuthProcessor                 │  ← WS-Security auth
│     Extracts wsse:Username + wsse:Password  │
│     from <soap:Header>                      │
│     Validates against app_user table        │
│     Sets authFailed=true if invalid         │
│                                             │
│  2. Choice (authFailed?)                    │
│     ├─ YES → return SOAP Fault              │
│     └─ NO  → continue                       │
│                                             │
│  3. WorkerRequestProcessor                  │  ← Business logic
│     Parses SOAP body                        │
│     Calls WorkerService methods             │
│     Returns Map<String, Object> result      │
│                                             │
│  4. WorkerResponseProcessor                 │  ← Response building
│     Converts Map result to SOAP XML         │
│     Wraps in SOAP Envelope                  │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
          SOAP XML Response
```

---

## Original Application

Before the Camel integration, the application had:

### JPA Entities

- `Worker` — id, firstName, lastName, dateOfBirth (LocalDate), role
- `User` — id, username, passwordHash (PBKDF2)

### Data Access

- `WorkerDAO` — CRUD operations + search with JPQL `LIKE` filtering
- `UserDAO` — findByUsername for authentication
- `JpaUtil` — EntityManagerFactory singleton

### Service Layer

- `WorkerService` — validation, orchestration, delegates to WorkerDAO

### Web Layer

- `LoginServlet`, `LogoutServlet`, `WorkersServlet`, `WorkerFormServlet`, `WorkerDeleteServlet`
- `AuthFilter` — session-based auth for web UI
- JSP views: login.jsp, workers.jsp, worker-form.jsp

### REST API

- `WorkerResource` — `@Path("/workers")` with GET/POST/PUT/DELETE
- `ApiAuthFilter` — HTTP Basic Auth for all `/api/**`
- `BasicAuthValidator` — validates against app_user table

### Original SOAP Service (removed)

- `WorkerSoapService` — JAX-WS `@WebService` with `@WebMethod` operations
- `WorkerSoapDTO` — JAXB data transfer objects
- `SoapAuthFilter` — servlet filter for `/WorkerSoapService`
- Registered in `web.xml` as a servlet mapping

**This original SOAP service was replaced by the Apache Camel translator.**

---

## Apache Camel Integration

### Why Camel Instead of JAX-WS?

The original JAX-WS SOAP service worked but had limitations:
- Tightly coupled to the specific XML schema of the WSDL
- Difficult to handle varying/incoming XML formats from external clients
- No easy way to add middleware (auth, transformation, routing)

Apache Camel provides:
- A flexible message processing pipeline
- Easy XML parsing and transformation
- Processors that can be composed and extended
- A clean separation between transport (HTTP/JAX-RS) and processing (Camel)

### Dependencies Added to `pom.xml`

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.apache.camel</groupId>
            <artifactId>camel-bom</artifactId>
            <version>4.22.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>org.apache.camel</groupId>
        <artifactId>camel-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.camel</groupId>
        <artifactId>camel-direct</artifactId>
    </dependency>
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>2.0.16</version>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.5.18</version>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

### CDI Bootstrap (no `camel-cdi`)

Camel 4.x removed `camel-cdi`. The `CamelContext` is started and stopped
manually using CDI lifecycle events:

```java
@ApplicationScoped
public class CamelBootstrap {

    private CamelContext camelContext;

    @Inject
    private SoapMessageTranslatorRoute soapRoute;

    public void onStartup(@Observes @Initialized(ApplicationScoped.class) Object init) {
        camelContext = new DefaultCamelContext();
        camelContext.addRoutes(soapRoute);
        camelContext.start();
    }

    public void onShutdown(@Observes @Destroyed(ApplicationScoped.class) Object init) {
        if (camelContext != null) camelContext.stop();
    }

    public CamelContext getCamelContext() { return camelContext; }
}
```

### CDI Gotcha: `RouteBuilder` Must Be `@Dependent`

`RouteBuilder` is an abstract class. Annotating it with `@ApplicationScoped`
causes `WELD-001410` (non-proxyable bean). The fix is to use `@Dependent`:

```java
@Dependent  // NOT @ApplicationScoped
public class SoapMessageTranslatorRoute extends RouteBuilder { ... }
```

The same applies to `SoapTranslatorResource` — it extends JAX-RS, which handles
its own scoping, so `@Dependent` is used.

### Only One `@ApplicationPath` Per WAR

Payara only allows one `@ApplicationPath` per WAR. The existing `RestApplication`
at `/api` is reused. The SOAP translator lives at `/api/soap/translate` under
the same application path.

---

## SOAP Message Translator — How It Works

### Endpoint

The soap-translator is a **separate WAR** (different context root) from worker-portal:

```
POST http://localhost:8090/soap-translator/api/soap/translate
     ?operation=<operation>
     [&id=<id>]
Content-Type: application/xml
```

### Request Flow

1. Client sends SOAP XML to `/soap-translator/api/soap/translate`
2. `SoapTranslatorResource` extracts `operation` and `id` query parameters
3. Raw SOAP XML + headers are sent to Camel via `ProducerTemplate`
4. Camel route processes the message through three stages:
   - **Auth** → **Request processing** → **Response building**
5. Final SOAP XML is returned to the client

### EJB Remote Communication (cross-WAR)

soap-translator does **not** call worker-portal over REST/HTTP. Instead
`WorkerRequestProcessor` injects `WorkerServiceRemote` via `@EJB`:

```java
@EJB(lookup = "java:global/worker-portal/WorkerService!com.company.workerportal.service.WorkerServiceRemote")
private WorkerServiceRemote workerService;
```

This is a direct in-JVM EJB Remote call over the Payara shared ORB.

### Shared Interfaces JAR (avoiding classloader conflicts)

`WorkerDTO`, `WorkerServiceRemote` and `AuthRequest` must live in **exactly one**
classloader so that the CORBA/IIOP types serialized by worker-portal deserialize
correctly inside soap-translator (and vice-versa). If both WARs package their own
copy of `com.company.workerportal.service.*`, Payara splits the package across
two webapp classloaders and you get:

```
ClassCastException: com.company.workerportal.model.Worker cannot be cast to
com.company.workerportal.model.Worker (loaders WebappClassLoader @3e5608af vs @20080ed4)
```

**Solution:** build `shared-interfaces.jar` containing only `WorkerDTO`,
`WorkerServiceRemote` and `AuthRequest`, install it to Maven local repo, and put
the JAR in the **Payara domain library**:

```
C:\payara7\glassfish\domains\domain1\lib\shared-interfaces.jar
```

Both WARs declare it as a `provided` dependency (compile-time only, not bundled):

```xml
<dependency>
    <groupId>com.company</groupId>
    <artifactId>shared-interfaces</artifactId>
    <version>1.0</version>
    <scope>provided</scope>
</dependency>
```

The **domain classloader** (parent of both webapp classloaders) loads the single
copy, so there is no package duplication.

**To rebuild the JAR** (e.g. after changing `WorkerDTO`/`WorkerServiceRemote`/
`AuthRequest`):

```
# copy the three .java files into a staging dir, then:
javac -cp <jakartaee-api.jar> -d <staging> <staging>/com/company/workerportal/service/Worker*.java <staging>/com/company/workerportal/service/AuthRequest.java
jar cf shared-interfaces.jar -C <staging> com

# install to local Maven + copy into Payara, then RESTART Payara
mvn install:install-file -DgroupId=com.company -DartifactId=shared-interfaces \
  -Dversion=1.0 -Dpackaging=jar -Dfile=shared-interfaces.jar -DgeneratePom=true
cp shared-interfaces.jar C:\payara7\glassfish\domains\domain1\lib\
```

**Important:** after adding/updating the JAR in `domain1/lib/`, the Payara domain
must be **restarted**, and worker-portal must be (re)deployed **before**
soap-translator so the EJB lookup target is present.

### Supported Operations

| Operation | Query Params | SOAP Body Content |
|---|---|---|
| `getAllWorkers` | — | Optional: `<searchTerm>`, `<dateFrom>`, `<dateTo>` |
| `getWorkerById` | `id=1` | — |
| `addWorker` | — | `<firstName>`, `<lastName>`, `<dateOfBirth>`, `<role>` |
| `updateWorker` | `id=1` | `<firstName>`, `<lastName>`, `<dateOfBirth>`, `<role>` |
| `deleteWorker` | `id=1` | — |

### Request Processing (WorkerRequestProcessor)

Parses the SOAP XML body using `DocumentBuilderFactory` (namespace-aware).
Extracts operation-specific data:

- **getAllWorkers**: optional `<searchTerm>`, `<dateFrom>`, `<dateTo>` elements
- **getWorkerById**: `id` from query param or `<id>` in body
- **addWorker/updateWorker**: `<firstName>`, `<lastName>`, `<dateOfBirth>`, `<role>`
- **deleteWorker**: `id` from query param or `<id>` in body

Calls `WorkerServiceRemote` methods (EJB Remote) and stores results in a
`Map<String, Object>`. Note that cross-WAR, only `WorkerDTO` objects are
exchanged — the JPA `Worker` entity (with `LocalDate`) is never serialized
over IIOP because `LocalDate` is not reliably serializable over GIOP/IIOP.

### Response Building (WorkerResponseProcessor)

Converts the `Map<String, Object>` result into SOAP XML. Each operation has
a dedicated method that builds the appropriate response structure:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <getAllWorkersResponse>
      <success>true</success>
      <workers>
        <worker>
          <id>1</id>
          <firstName>Youssef</firstName>
          <lastName>Allani</lastName>
          <dateOfBirth>1990-05-15</dateOfBirth>
          <role>Developer</role>
        </worker>
      </workers>
    </getAllWorkersResponse>
  </soap:Body>
</soap:Envelope>
```

---

## Authentication

The SOAP translator requires **both** authentication methods to pass:

### 1. HTTP Basic Auth (ApiAuthFilter)

Applied to all `/api/**` endpoints including `/soap/**`.

```
Authorization: Basic YWRtaW46YWRtaW4xMjM=
```

Validates against the `app_user` table using `BasicAuthValidator`:
- Decodes Base64 credentials
- Looks up user by username via `UserDAO`
- Verifies password hash via `PasswordUtil.verify()` (PBKDF2)

Returns HTTP 401 if invalid.

### 2. WS-Security UsernameToken (SoapHeaderAuthProcessor)

Credentials must be inside the SOAP envelope header:

```xml
<soap:Header>
  <wsse:Security
    xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
    <wsse:UsernameToken>
      <wsse:Username>admin</wsse:Username>
      <wsse:Password>admin123</wsse:Password>
    </wsse:UsernameToken>
  </wsse:Security>
</soap:Header>
```

The username/password are forwarded to the remote EJB (`WorkerService`
via `AuthRequest`), which re-validates them against the `app_user` table
(`PasswordUtil.verify`) and then applies role-based authorization (see below).
Returns a SOAP Fault if invalid or unauthorized.

### 3. Role-Based Authorization

Each `app_user` has a `role` column. Role is enforced inside the EJB layer
(`WorkerService.checkAllowed`) because soap-translator has no direct DB access.

| Role | Allowed SOAP operations |
|---|---|
| `ADMIN` | All operations (`getAllWorkers`, `searchWorkers`, `getWorkerById`, `getDistinctRoles`, `validate`, `addWorker`, `updateWorker`, `deleteWorker`) |
| `VIEWER` | Read-only only: `getAllWorkers`, `searchWorkers`, `getWorkerById`, `getDistinctRoles`, `validate` |

How it works:

- `WorkerRequestProcessor` builds an `AuthRequest` from the WS-Security
  `Username`/`Password` headers captured by `SoapHeaderAuthProcessor` and passes
  it as the first argument of every `WorkerServiceRemote` call.
- Each `WorkerService` method calls `checkAllowed(caller, operation)`, which:
  - Resolves the user by username (`UserDAO.findByUsername`)
  - Verifies the password (`PasswordUtil.verify`)
  - Denies mutating operations for non-`ADMIN` roles with
    `SecurityException("Access denied ...")`
- `WorkerResponseProcessor` turns any error/denial into an explicit
  `<Fault>` in the SOAP body.

Internal Web/REST front-controllers (which already authenticate via session or
Basic Auth) pass a `null` caller and are therefore **not** subject to the SOAP
role restriction - the Web UI and REST API behave exactly as before.

Create a new user with a role (hash generated by `GenerateHashTool`):

```sql
INSERT INTO app_user (username, password_hash, role)
VALUES ('viewer', '<paste-hash>', 'VIEWER');
```

### Why Both?

- Basic Auth protects the HTTP endpoint (standard for REST/JAX-RS)
- WS-Security in the SOAP header ensures the message itself is authenticated
- An external client that only has the SOAP envelope (e.g., forwarding XML)
  must still include valid credentials inside the message

### Auth Failure Responses

| Scenario | Response |
|---|---|
| Missing/invalid Basic Auth | HTTP 401 Unauthorized |
| Valid Basic Auth + missing WS-Security | SOAP Fault: `WS-Security credentials missing` |
| Valid Basic Auth + wrong WS-Security password | SOAP Fault: `Invalid credentials` |
| Both invalid | HTTP 401 (Basic Auth fails first) |
| Valid credentials but operation not allowed for role | SOAP Fault: `Access denied: operation '<op>' not permitted for role '<role>'` |

---

## Filtering Workers

### getAllWorkers with Filters

The `getAllWorkers` operation supports optional filters passed inside the SOAP body:

```xml
<soap:Body>
  <getAllWorkers>
    <searchTerm>y</searchTerm>
    <dateFrom>1985-01-01</dateFrom>
    <dateTo>1995-12-31</dateTo>
  </getAllWorkers>
</soap:Body>
```

All filters are optional and can be combined:

| Filter | JPQL Condition | Example |
|---|---|---|
| `<searchTerm>y</searchTerm>` | `LOWER(firstName) LIKE '%y%' OR LOWER(lastName) LIKE '%y%'` | Matches "Youssef", "Gary", "Lyon" |
| `<dateFrom>1990-01-01</dateFrom>` | `dateOfBirth >= :dateFrom` | Born from 1990 onwards |
| `<dateTo>1995-12-31</dateTo>` | `dateOfBirth <= :dateTo` | Born up to 1995 |

Without any filter elements (or with an empty `<getAllWorkers/>`), all workers
are returned — same behavior as before.

### How Filtering Flows Through the Code

1. `WorkerRequestProcessor` extracts `<searchTerm>`, `<dateFrom>`, `<dateTo>` from SOAP XML
2. If any filter is present, calls `WorkerService.searchWorkers(term, null, null, false, dateFrom, dateTo)`
3. `WorkerService` delegates to `WorkerDAO.search()` which builds a dynamic JPQL query
4. If no filters, calls `WorkerService.getAllWorkers()` (simple `findAll()`)

---

## Project Layout

The system now consists of **two separate WARs** plus a **shared interfaces JAR**:

```
worker-portal/                          (Git branch: main)
│
├── src/main/java/com/company/workerportal/
│   ├── model/
│   │   ├── Worker.java              JPA entity (id, firstName, lastName, dateOfBirth, role)
│   │   └── User.java                JPA entity (id, username, passwordHash)
│   ├── dao/
│   │   ├── WorkerDAO.java           CRUD + search with dynamic JPQL
│   │   ├── UserDAO.java             findByUsername
│   │   └── JpaUtil.java             EntityManagerFactory singleton
│   ├── service/
│   │   └── WorkerService.java       @Stateless @Remote EJB bean, business logic/validation
│   ├── security/
│   │   ├── PasswordUtil.java        PBKDF2 hash/verify
│   │   ├── BasicAuthValidator.java  Validates HTTP Basic Auth against DB
│   │   └── GenerateHashTool.java    CLI tool to generate password hashes
│   ├── rest/
│   │   ├── RestApplication.java     @ApplicationPath("/api")
│   │   ├── WorkerResource.java      JSON REST API (/api/workers)
│   │   ├── ApiAuthFilter.java       Basic Auth for all /api/** endpoints
│   │   └── ErrorMessage.java        Simple JSON error body
│   └── web/
│       ├── LoginServlet.java, LogoutServlet.java
│       ├── WorkersServlet.java, WorkerFormServlet.java, WorkerDeleteServlet.java
│       └── AuthFilter.java          Session-based auth for web UI
│
├── src/main/webapp/                 login.jsp, workers.jsp, worker-form.jsp, css/style.css
├── src/main/resources/META-INF/persistence.xml
└── sql/schema.sql                   Database schema + sample data

soap-translator/                     (Git branch: soap-translator — standalone Camel SOAP middleware)
│
└── src/main/java/com/company/soaptranslator/
    ├── camel/
    │   ├── CamelBootstrap.java      CDI lifecycle for CamelContext
    │   ├── route/
    │   │   └── SoapMessageTranslatorRoute.java    Main Camel route
    │   └── processor/
    │       ├── SoapHeaderAuthProcessor.java        WS-Security auth from SOAP header
    │       ├── WorkerRequestProcessor.java         SOAP XML → WorkerServiceRemote calls
    │       └── WorkerResponseProcessor.java        WorkerDTO results → SOAP XML
    └── rest/
        ├── RestApplication.java     @ApplicationPath("/api")
        └── SoapTranslatorResource.java   SOAP translator entry point
```

**Shared interfaces JAR** (deployed to Payara `domain1/lib/`, `provided` scope):

```
com.company.workerportal.service.AuthRequest
com.company.workerportal.service.WorkerDTO
com.company.workerportal.service.WorkerServiceRemote
```

These three classes live ONLY in `shared-interfaces.jar`, never inside either WAR.

---

## Build & Deployment

### Prerequisites

- Payara 7 with the domain **running** (`asadmin start-domain domain1`).
- `shared-interfaces.jar` present in `C:\payara7\glassfish\domains\domain1\lib\`.
- local Maven repo has `com.company:shared-interfaces:1.0` installed
  (required for compiling the WARs, because both declare it `provided`).

### Routine redeploy (most code changes)

For changes that stay inside **one** WAR (servlets, REST, DAO, processors, route,
validation logic, etc.) do exactly what you did before — build and force-deploy:

```powershell
# worker-portal
mvn clean package
asadmin deploy --force target/worker-portal.war

# soap-translator
mvn clean package
asadmin deploy --force target/soap-translator.war
```

**Deploy order rule:** always deploy **worker-portal FIRST** (it provides the EJB
that soap-translator looks up), then soap-translator. If both are being redeployed,
do them in this order.

### Shared interfaces JAR change

If you modify `WorkerDTO`, `WorkerServiceRemote` or `AuthRequest` (a **signature**
change), the procedure is heavier because those classes are cached by the domain
classloader:

```powershell
# 1. Stage the three source files, then compile + jar them
#    (the .java files normally live in worker-portal's
#    src/main/java/com/company/workerportal/service/)
javac -cp <path-to-jakartaee-api.jar> -d <staging> <staging>/com/company/workerportal/service/Worker*.java <staging>/com/company/workerportal/service/AuthRequest.java
jar cf shared-interfaces.jar -C <staging> com

# 2. Install to local Maven repo (for compile-time deps)
mvn install:install-file -DgroupId=com.company -DartifactId=shared-interfaces `
  -Dversion=1.0 -Dpackaging=jar -Dfile=shared-interfaces.jar -DgeneratePom=true

# 3. Copy into the Payara domain library (replace the old file)
copy shared-interfaces.jar C:\payara7\glassfish\domains\domain1\lib\

# 4. Restart the domain (required for the classloader to pick up the new JAR)
asadmin restart-domain domain1
# wait until it finishes coming back up

# 5. Rebuild + redeploy BOTH apps, worker-portal first
mvn clean package            # in worker-portal
asadmin deploy --force target/worker-portal.war
mvn clean package            # in soap-translator
asadmin deploy --force target/soap-translator.war
```

> The `javac`/`jar` commands above are on one line each (the `\`` line
> continuation is only for PowerShell readability).

### Which procedure to use — decision table

| What you changed | Procedure |
|---|---|
| worker-portal only (servlets, REST, DAO, security, validation) | Build + deploy worker-portal only. soap-translator is unaffected. |
| soap-translator only (processors, route, forwarding existing params like `role`/`sort`) | Build + deploy soap-translator only. |
| `WorkerDTO`, `WorkerServiceRemote` or `AuthRequest` signature | Full shared-JAR procedure above (rebuild → install → copy → restart → redeploy both). |

**Design tip:** keep `WorkerServiceRemote`, `WorkerDTO` and `AuthRequest` stable. To
add filtering, prefer forwarding parameters the EJB already accepts (`role`, `sort`,
`desc`) from soap-translator only — that stays a one-WAR change.

---

## Key Implementation Details

### Camel Route Processing

The route uses Camel's `choice()` to short-circuit on auth failure:

```java
from("direct:soap-translate")
    .process(soapHeaderAuthProcessor)
    .choice()
        .when(header("authFailed").isEqualTo(true))
            // Return fault, skip business logic
        .otherwise()
            .process(workerRequestProcessor)
            .process(workerResponseProcessor)
    .end();
```

### XML Parsing

Both `SoapHeaderAuthProcessor` and `WorkerRequestProcessor` parse the same
SOAP XML independently (Camel processors receive the same exchange body).
`DocumentBuilderFactory` with `setNamespaceAware(true)` is used throughout.

### Error Handling

The route has a global `onException(Exception.class)` handler that catches
any unexpected exception and returns a generic Server fault:

```xml
<Fault>
  <faultcode>Server</faultcode>
  <faultstring>Internal processing error</faultstring>
</Fault>
```

### Password Storage

Passwords are stored as `<saltHex>:<hashHex>` using PBKDF2WithHmacSHA256
with 120,000 iterations and a 256-bit key. The same format is used for
both Basic Auth and WS-Security validation (they share the `app_user` table).

---

## Testing the SOAP Endpoint

### Postman Setup

1. **Method**: POST
2. **URL**: `http://localhost:8090/soap-translator/api/soap/translate?operation=getAllWorkers`
3. **Auth tab**: Basic Auth → username: `admin`, password: `admin123`
4. **Body tab**: raw → XML

### Example: Get All Workers

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
               xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
  <soap:Header>
    <wsse:Security>
      <wsse:UsernameToken>
        <wsse:Username>admin</wsse:Username>
        <wsse:Password>admin123</wsse:Password>
      </wsse:UsernameToken>
    </wsse:Security>
  </soap:Header>
  <soap:Body>
    <getAllWorkers/>
  </soap:Body>
</soap:Envelope>
```

### Example: Search Workers

```xml
<soap:Body>
  <getAllWorkers>
    <searchTerm>y</searchTerm>
    <dateFrom>1985-01-01</dateFrom>
    <dateTo>1995-12-31</dateTo>
  </getAllWorkers>
</soap:Body>
```

### Example: Get Worker by ID

```xml
<soap:Body>
  <getWorkerById>
    <id>1</id>
  </getWorkerById>
</soap:Body>
```

### Example: Add Worker

```xml
<soap:Body>
  <addWorker>
    <firstName>Jane</firstName>
    <lastName>Doe</lastName>
    <dateOfBirth>1992-03-20</dateOfBirth>
    <role>Engineer</role>
  </addWorker>
</soap:Body>
```

### Example: Update Worker

```xml
<soap:Body>
  <updateWorker>
    <id>1</id>
    <firstName>Jane</firstName>
    <lastName>Smith</lastName>
    <dateOfBirth>1992-03-20</dateOfBirth>
    <role>Senior Engineer</role>
  </updateWorker>
</soap:Body>
```

### Example: Delete Worker

```xml
<soap:Body>
  <deleteWorker>
    <id>1</id>
  </deleteWorker>
</soap:Body>
```

---

## Troubleshooting

### `WELD-001410: Non-proxyable bean`

Cause: A `RouteBuilder` subclass annotated with `@ApplicationScoped`.
Fix: Use `@Dependent` instead.

### `No @ApplicationPath found`

Cause: Multiple `@ApplicationPath` annotations in the same WAR.
Fix: Only one allowed — the SOAP translator is under the existing `/api` path.

### Camel context fails to start

Check `C:\payara7\glassfish\domains\domain1\logs\server.log` for:
- `ClassNotFoundException` — missing camel-core or camel-direct in WAR
- `WELD` errors — check beans.xml exists and has `<annotated/>` discovery mode

### WS-Security auth fails with valid credentials

The `SoapHeaderAuthProcessor` uses `getElementsByTagName` which searches
all elements regardless of namespace prefix. Ensure the SOAP envelope
includes the `wsse` namespace declaration:

```xml
xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
```

### 401 on all /api/** endpoints

The `ApiAuthFilter` applies to every path under `/api`. Make sure the
`Authorization` header is set correctly. The filter returns a plain JSON
string on 401, not an XML entity, to avoid serialization errors.
