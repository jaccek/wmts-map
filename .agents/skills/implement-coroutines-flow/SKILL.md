---
name: implement-coroutines-flow
description: Implement asynchronous Kotlin code using structured concurrency, Flow, StateFlow, cancellation and deterministic tests according to company standards.
---

# Implement Coroutines and Flow

## Analyze
1. Read the company Coroutines standard.
2. Identify the owner and lifetime of the work.
3. Determine whether the API is one-shot (`suspend`) or observable (`Flow`).
4. Inspect existing dispatcher, scope and event conventions.

## Implement
- Use structured concurrency and a lifecycle-appropriate owner.
- Never introduce `GlobalScope`.
- Preserve cancellation and rethrow `CancellationException` when catching broad exceptions.
- Use `StateFlow` for state with a current value.
- Use `SharedFlow` only when its replay and buffering semantics match the event requirement.
- Do not block the main thread.
- Do not switch dispatchers without a reason.
- Inject dispatchers when needed for deterministic testing.
- Define upstream error and recovery behavior explicitly.
- Avoid turning transient events into state or state into lossy events without a deliberate design.

## Test
Use the project's test dispatcher and scheduler. Avoid real delays. Cover success, failure, cancellation and state transitions that matter.

## Completion
Explain scope ownership, cancellation behavior, Flow semantics and tests executed.
