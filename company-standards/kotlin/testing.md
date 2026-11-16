# Kotlin testing standards

- **MUST** test business rules and non-trivial transformations.
- **SHOULD** test observable behavior rather than private implementation details.
- **MUST** cover important success, failure, boundary and cancellation cases.
- **SHOULD** keep tests deterministic and independent of wall-clock timing, network and machine-specific state.
- **SHOULD** use descriptive test names that state condition and expected outcome.
- **MUST NOT** weaken production code solely to satisfy a brittle test.
- **SHOULD** use fakes for simple deterministic collaborators and mocks where interaction verification is genuinely needed.
- **MUST** ensure tests clean up resources and do not depend on execution order.

The project must document the exact Gradle test tasks and platform coverage.
