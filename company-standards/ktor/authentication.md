# Ktor authentication

- **MUST** keep tokens and credentials out of source, logs and ordinary persistent storage.
- **MUST** use the approved secure-storage abstraction for platform credentials.
- **MUST** define refresh concurrency and token invalidation behavior.
- **SHOULD** avoid multiple simultaneous refresh requests for the same credential state.
- **MUST** prevent infinite refresh/retry loops.
- **MUST** handle expired, revoked and invalid credentials distinctly where the API supports it.
- **MUST** never disable TLS verification to resolve authentication or certificate errors.

Company decision required: token storage, refresh strategy, logout semantics and authentication plugin configuration.
