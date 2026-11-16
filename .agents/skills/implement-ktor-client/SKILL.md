---
name: implement-ktor-client
description: Implement or modify Ktor HTTP client integration, serialization, authentication or API error mapping according to company standards.
---

# Implement Ktor client integration

## Analyze
1. Read Ktor client, serialization, error-handling and authentication standards relevant to the task.
2. Inspect the existing shared `HttpClient` configuration and API service conventions.
3. Identify the API contract, DTOs, domain mapping and error abstraction.
4. Check whether the required Ktor plugins and dependencies already exist.

## Implement
- Reuse the centralized client; do not create a client per request.
- Keep endpoint definitions and transport DTOs at the data/infrastructure boundary.
- Configure request parameters and serialization consistently with the existing client.
- Map HTTP, transport, timeout and deserialization failures into the project's established error model.
- Preserve coroutine cancellation.
- Never log tokens, credentials or sensitive payloads.
- Do not weaken TLS verification.
- Avoid retries unless the operation is safe and the project's retry policy allows them.

## Tests
Use Ktor MockEngine or the project's established test transport to cover:
- successful response and mapping;
- representative 4xx and 5xx responses;
- network/timeout failures;
- malformed or incomplete payloads;
- cancellation where relevant.

## Completion
Report API contract assumptions, changed files, tests and any unverified backend behavior.
