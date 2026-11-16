# Ktor error handling

- **MUST** distinguish transport failures, timeout/cancellation, HTTP status failures and deserialization failures.
- **MUST** map Ktor-specific exceptions into the application's agreed error abstraction at the data boundary.
- **MUST NOT** expose raw server response bodies to users.
- **SHOULD** parse structured API error payloads when the backend contract defines them.
- **SHOULD** preserve status code and safe diagnostic metadata for logging/telemetry.
- **MUST** rethrow cancellation rather than mapping it to a generic network error.
- **SHOULD** test representative 2xx, 4xx, 5xx, timeout, offline and malformed-payload cases using a mock engine.
