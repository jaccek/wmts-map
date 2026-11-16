# Kotlin coding style

## Naming and structure

- Use `UpperCamelCase` for types and `lowerCamelCase` for functions/properties.
- Use descriptive names; avoid unexplained abbreviations.
- Keep files cohesive. Multiple declarations are acceptable when they form one small, related unit.
- Keep visibility as restrictive as practical (`private`/`internal` before `public`).

## API design

- Prefer small APIs with explicit inputs and outputs.
- Avoid exposing mutable collections or mutable state.
- Avoid boolean parameters when an enum or distinct function communicates intent better.
- Avoid default arguments that conceal materially different behavior.

## Scope functions

Use `let`, `run`, `with`, `apply` and `also` only when they improve readability. Avoid deeply nested scope functions or ambiguous `it` references.

## Exceptions

Do not use exceptions for ordinary branching or expected domain outcomes. Do not catch `Throwable` unless implementing a carefully justified process boundary.
