# Ktor HTTP client

- **MUST** centralize `HttpClient` construction and shared configuration.
- **MUST** configure explicit connect, request and socket timeouts appropriate to the product.
- **MUST** install only required plugins and document non-default behavior.
- **SHOULD** inject the client or a narrow API abstraction for testability.
- **MUST** close client resources when their owning scope ends.
- **MUST NOT** create a new client for every request.
- **SHOULD** configure expect-success behavior intentionally and handle non-2xx responses consistently.
- **MUST** avoid logging credentials and sensitive payloads.
- **SHOULD** use Ktor's multiplatform engine strategy consistently with the project's targets.

Company decision required: approved engines, timeout values, retry policy, proxy behavior and client lifetime.
