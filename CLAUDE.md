# DoSafe — Project Context for Claude

## What this project is

DoSafe is a **portfolio backend project** built by a junior developer (Miguel Damasco) with no formal work experience. Its purpose is to demonstrate backend skills to potential employers. The application processes identity documents (DNI, passport, driver's license), extracts their expiration dates using AWS Textract OCR, and stores them securely in S3.

**This is a backend-only project.** The `dosafe-frontend/` directory exists but is empty — the UI has not been implemented yet.

---

## Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4.0.2 / Spring 7 / Java 17 |
| Build | Maven |
| Security | Spring Security + JWT (JJWT 0.13.0) |
| Database | MySQL 8.x + JPA/Hibernate |
| Cloud | AWS SDK v2 — S3, SQS, Textract, SNS |
| PDF processing | Apache PDFBox 2.0.30 |
| Logging | SLF4J + Logback |
| Boilerplate reduction | Lombok |

---

## Running locally

**Prerequisites:** MySQL on `localhost:3306`, AWS env vars set.

```bash
cd dosafe-backend/DoSafe
mvn clean spring-boot:run
```

App starts on `http://localhost:8080`.

**Required environment variables:**
```
AWS_S3_BUCKET=dosafe-documents
AWS_REGION=us-east-2
AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...
```

**Database:** `application.properties` uses `root/area51` on `localhost:3306/dosafe`. Credentials are hardcoded — known issue, not yet moved to env vars.

**Stop the app on Windows:**
```powershell
Stop-Process -Id (Get-NetTCPConnection -LocalPort 8080 -State Listen).OwningProcess -Force
```

**View logs in real time (Windows):**
```powershell
Get-Content "dosafe-backend\DoSafe\logs\dosafe.log" -Wait -Tail 50
```

---

## Architecture

Layered architecture with clean separation:

```
Controller  →  Service  →  Domain  →  Infrastructure/Repository  →  MySQL
                                  ↘  AWS (S3, SQS, Textract)
```

### Module map

```
src/main/java/com/miguel_damasco/DoSafe/
├── common/
│   ├── apiResponse/       # ApiResponse<T> sealed interface, ApiResponses factory, Meta, ErrorInfo
│   ├── correlationId/     # CorrelationIdHolder (MDC wrapper), CorrelationIdFilter (OncePerRequestFilter)
│   └── exception/         # DoSafeException base, specific exceptions, GlobalExceptionHandler
├── config/                # AwsConfig, SecurityConfig, JacksonConfig
├── document/
│   ├── controller/        # DocumentController → POST /document/upload
│   ├── service/           # DocumentUploadService, ProcessTextractResultService
│   ├── domain/            # DocumentModel (JPA), DocumentId, DocumentTypeEnum, DocumentStatus
│   │   ├── clasification/ # DocumentClasifier interface, GeneralDocumentClasifier
│   │   └── extraction/    # ExpirationDateExtractor (Strategy), per-type implementations, Selector
│   ├── dto/               # Request/response DTOs, DocumentResponseMapper
│   ├── infraestructure/
│   │   ├── conversion/    # DocumentConverter interface, PdfDocumentConverter (PDFBox)
│   │   └── ocr/
│   │       ├── textract/  # TextractClientAdapter, TextractNotificationMessage
│   │       ├── sqs/       # TextractQueueConsumer (@Scheduled every 5s)
│   │       └── sns/       # SnsNotification DTO
│   └── repository/        # DocumentRepository (Spring Data JPA)
├── user/
│   ├── controller/        # AuthenticationController → POST /authentication/login, /register
│   ├── service/           # UserService (login, register, JWT generation)
│   ├── domain/            # UserModel (JPA), RoleEnum (USER, ADMIN)
│   ├── dto/               # LoginRequestDTO, RegisterRequestDTO, LoginResponseDTO, RegisterResponseDTO
│   └── repository/        # UserRepository
├── security/
│   ├── jwt/               # JwtUtil (JJWT), JwtAuthenticationFilter
│   ├── refresh/           # RefreshTokenModel, RefreshTokenService, RefreshTokenRepository
│   ├── CustomUserDetailsService.java
│   └── MyUserDetails.java
└── storage/
    ├── DocumentStorage.java        # Interface
    └── S3DocumentStorage.java      # Implementation — S3 upload, presigned URLs, delete
```

---

## Key conventions

### Dependency injection
Always constructor injection. Lombok `@RequiredArgsConstructor` is used throughout. `@Autowired` is not used.

### API responses
Every controller returns `ResponseEntity<ApiResponse<T>>`. Never return raw objects.

```java
// Success
ApiResponses.success(data, 200, "Login successfully!")
ApiResponses.success(data, 201, "Resource created successfully!")

// Error (handled by GlobalExceptionHandler — do NOT build error responses in controllers)
ApiResponses.error(errorCode, details, statusCode, httpReason)
```

`ApiResponse<T>` is a sealed interface with two permits: `Success<T>` and `Error` (both Java records).

### Exception handling
Throw domain exceptions from services, never handle HTTP status codes in services.

**Custom exceptions live in `common/exception/`:**
- `DoSafeException` — base class, carries `HttpStatus` + `errorCode`
- `UserAlreadyExistsException` → 409 `USER_ALREADY_EXISTS`
- `UserNotFoundException` → 404 `USER_NOT_FOUND`
- `DocumentProcessingException` → 422 `DOCUMENT_PROCESSING_ERROR`

`GlobalExceptionHandler` (`@RestControllerAdvice`) centralizes all error responses. When adding a new feature that can fail, create a specific exception extending `DoSafeException` — do not throw `RuntimeException` directly.

**Error response format:**
```json
{
  "error": { "code": "USER_ALREADY_EXISTS", "details": "Username 'x' is already taken" },
  "meta": { "success": false, "statusCode": 409, "message": "Conflict", "timestamp": "..." }
}
```

### Observability / CorrelationId
Every HTTP request gets a `correlationId` (UUID) injected into the SLF4J MDC by `CorrelationIdFilter`. It appears in every log line as `[correlationId]`.

- If the request carries `X-Correlation-ID` header, that value is reused (for end-to-end tracing).
- The correlationId is returned to the client in the `X-Correlation-ID` response header.
- For async processing: the correlationId is stored as S3 object metadata (`correlation-id`) and as Textract's `jobTag`, then recovered in `TextractQueueConsumer` when the SQS notification arrives.
- Use `MDC.remove("correlationId")` (via `CorrelationIdHolder.clear()`) — never `MDC.clear()`, which would wipe other MDC entries from other components.

### Logging
- Use `@Slf4j` (Lombok) on every class that logs.
- Never use `System.out.println()`. Use `log.info()`, `log.warn()`, `log.error()`.
- Log format: `%d{HH:mm:ss} %-5level [%X{correlationId}] %logger - %msg%n`
- File logs: `logs/dosafe.log` with daily rotation, 30-day retention.
- Package `com.miguel_damasco.DoSafe` logs at DEBUG. Everything else at WARN.

### Naming
- Method parameters: `p` prefix (e.g., `pRequest`, `pUserId`).
- Services: named after their action (e.g., `DocumentUploadService`, `ProcessTextractResultService`).
- DTOs: suffix `RequestDTO` / `ResponseDTO`.
- Enums: suffix `Enum` (e.g., `DocumentTypeEnum`, `RoleEnum`).

---

## Document processing flow

### Synchronous (HTTP request)
```
POST /document/upload (JWT required)
  → DocumentUploadService.upload()
      → find user by username
      → DocumentFactory.create() → status: PROCESSING
      → save to DB
      → PdfDocumentConverter.convertToPdf()
      → S3DocumentStorage.upload() → key: users/{userId}/{documentId}.pdf
      → save S3 key to DB
      → return DocumentUploadResponseDTO
```

### Asynchronous (SQS polling every 5 seconds)
```
TextractQueueConsumer.pollQueue()
  → deserialize SNS wrapper → TextractNotificationMessage
  → if status != SUCCEEDED → skip
  → recover correlationId from jobTag → set in MDC
  → extract documentId from S3 key
  → ProcessTextractResultService.execute()
      → TextractClientAdapter.getLines() → OCR text lines
      → GeneralDocumentClasifier.classify() → DocumentTypeEnum
      → ExpirationDateExtractorSelector → pick correct extractor
      → ExpirationDateExtractor.extract() → LocalDate
      → document.markProcessed(type, expirationDate) → status: PROCESSED
      → save to DB
```

---

## Security

- JWT stateless authentication via `JwtAuthenticationFilter` (runs before `UsernamePasswordAuthenticationFilter`).
- Public endpoints: `POST /authentication/login`, `POST /authentication/register`, `GET /document/poll`.
- All other endpoints require a valid JWT Bearer token.
- Passwords: BCrypt strength 10.
- CSRF disabled (JWT-based API, no cookies).
- Session policy: STATELESS.
- Refresh token mechanism implemented (`RefreshTokenModel`, `RefreshTokenService`).

---

## AWS infrastructure

- **Region:** `us-east-2`
- **S3 bucket:** `dosafe-documents` (env var `AWS_S3_BUCKET`)
- **S3 key pattern:** `users/{userId}/{documentId}.pdf`
- **SQS queue:** `https://sqs.us-east-2.amazonaws.com/565393042861/document-text-extracted-queue`
- **Credentials:** `DefaultCredentialsProvider` (reads from env vars or `~/.aws/credentials`)
- **Flow:** Textract → SNS → SQS → `TextractQueueConsumer`

---

## Known issues / pending work

- `System.out.println()` calls remain in `DocumentController` and `S3DocumentStorage` — replace with `log.info()`.
- Database credentials (`root/area51`) are hardcoded in `application.properties` — move to env vars.
- SQS queue URL is hardcoded in `application.properties` — move to env var.
- No tests exist yet — highest priority gap for portfolio credibility.
- No Swagger/OpenAPI documentation.
- No frontend implemented.
- No retry mechanism for failed Textract jobs (status stays FAILED).
- `ExpirationDateExtractor` only fully implemented for `IDENTITY_CARD`; passport and other types are partial.

---

## Design patterns in use

| Pattern | Where |
|---|---|
| Strategy | `ExpirationDateExtractor` — one implementation per document type, selected by `ExpirationDateExtractorSelector` |
| Factory | `DocumentFactory` — creates `DocumentModel` with defaults |
| Adapter | `TextractClientAdapter`, `S3DocumentStorage` — wrap AWS SDK |
| Repository | Spring Data JPA (`DocumentRepository`, `UserRepository`) |
| Sealed classes | `ApiResponse<T>` — `Success<T>` and `Error` as records |
| Value Object | `DocumentId` wrapping `UUID` |
| Template Method | `OncePerRequestFilter` base for `CorrelationIdFilter` and `JwtAuthenticationFilter` |
