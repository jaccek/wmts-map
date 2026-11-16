# Compose navigation

- **MUST** use the project's approved navigation library and established route model.
- **SHOULD** keep navigation decisions outside reusable leaf composables.
- **SHOULD** pass stable identifiers or small navigation arguments rather than entire domain graphs.
- **MUST** consider back-stack behavior, process recreation and deep links where applicable.
- **MUST** avoid placing secrets or sensitive personal data in routes.
- **SHOULD** test critical navigation flows and argument handling.

Company decision required: document the navigation library, route serialization strategy and Android/iOS integration.
