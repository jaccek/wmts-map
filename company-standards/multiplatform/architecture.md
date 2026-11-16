# Kotlin Multiplatform architecture

## Shared code

- **SHOULD** place platform-independent domain rules, models, validation, orchestration and reusable data logic in shared source sets.
- **MUST NOT** force platform-specific behavior into common code through fragile workarounds.
- **SHOULD** keep platform APIs behind narrow interfaces when shared policy needs them.
- **MUST** keep platform-specific dependencies out of `commonMain`.
- **SHOULD** keep public shared APIs simple for both Kotlin and Swift consumers.

## Platform implementations

- Put platform-specific implementations in the narrowest appropriate source set.
- Prefer dependency injection or explicit factory boundaries when selecting platform implementations.
- Use `expect`/`actual` only when a compile-time platform declaration is the clearest solution; do not use it as a default service locator.
- Avoid leaking Android framework or Apple framework types into shared domain APIs.

## Build and compatibility

- **MUST** consider all configured targets when changing shared public APIs.
- **MUST** report which targets were actually compiled and tested.
- **SHOULD** keep target-specific work isolated so one unavailable platform does not obscure validation of others.
