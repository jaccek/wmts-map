# Compose Multiplatform architecture

- **MUST** keep composables focused on rendering UI and forwarding user intent.
- **MUST NOT** put business rules, persistence operations or HTTP calls directly in composables.
- **SHOULD** use unidirectional data flow: state flows toward UI; user actions flow toward state owner.
- **SHOULD** separate screen-level state orchestration from reusable visual components.
- **SHOULD** reuse the project's design system rather than recreating common controls.
- **MUST** keep composables side-effect-safe; use effect APIs only for effects tied to composition/lifecycle.
- **SHOULD** make state ownership and lifetime explicit.
- **MAY** use a ViewModel or another state holder according to the project's established architecture. Do not introduce a second state-management pattern without approval.
