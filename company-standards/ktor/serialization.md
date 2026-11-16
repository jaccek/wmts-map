# Ktor serialization

- **MUST** use the project's approved serialization library and shared configuration.
- **SHOULD** configure unknown-key handling, default values, null encoding and naming conventions deliberately.
- **MUST** distinguish API DTOs from domain models when API contracts are unstable or differ from domain meaning.
- **MUST** handle nullable, missing and defaulted fields according to the API contract.
- **SHOULD** keep serialization annotations at transport boundaries rather than spreading them through domain models.
- **MUST** add tests for custom serializers and important contract edge cases.
