# Kotlin conventions

- **MUST** follow the official Kotlin coding conventions unless the project has an approved formatter configuration that refines them.
- **SHOULD** prefer `val` and immutable data.
- **SHOULD** use data classes for value-like data where their generated semantics are appropriate.
- **SHOULD** use sealed types for finite, meaningful state/error alternatives.
- **MUST** make nullability explicit; avoid `!!` in production code.
- **SHOULD** use named arguments when they materially improve call-site clarity.
- **SHOULD** keep functions focused and avoid excessive nesting.
- **SHOULD** prefer standard-library operations when they are clear and efficient.
- **MUST NOT** introduce clever DSLs, scope-function chains or extension APIs that obscure control flow.
- **MAY** use extension functions for cohesive operations that naturally read as operations on the receiver.

Do not apply style-only rewrites to unrelated code.
