# Coroutines and Flow standards

## Structured concurrency

- **MUST** use structured concurrency and lifecycle-appropriate scopes.
- **MUST NOT** use `GlobalScope` in application code.
- **MUST** preserve cancellation: do not swallow `CancellationException`; if catching broad exceptions, rethrow cancellation.
- **MUST** ensure launched work has a clear owner and lifetime.
- **SHOULD** inject dispatchers when deterministic tests or dispatcher choice is important.
- **SHOULD** avoid switching dispatchers without a reason; use `withContext` around blocking or dispatcher-specific work.

## Flow

- Use `StateFlow` for observable state with a current value.
- Use `SharedFlow` or another explicit event mechanism for broadcast events when its replay/buffer semantics fit.
- Do not treat a `SharedFlow` as a durable event queue.
- **MUST** collect UI flows with lifecycle-aware APIs on Android where applicable.
- **SHOULD** keep flow transformations close to the layer that owns their meaning.
- **MUST** define error behavior for upstream failures; a failed flow should not accidentally terminate a long-lived UI state pipeline without a deliberate recovery strategy.

## Dispatchers and blocking work

- Do not perform blocking I/O on a UI thread.
- Use the dispatcher appropriate to the operation and platform.
- Do not wrap every suspend call in `withContext(Dispatchers.IO)`; suspend APIs may already manage their execution context.

## Testing

- Use deterministic test dispatchers and a shared test scheduler where applicable.
- Avoid real delays and timing-dependent assertions.
- Test cancellation and failure behavior when they are important to correctness.

## Company decision required

Define the approved UI event pattern, dispatcher provider abstraction (if any), and test libraries for each project.
