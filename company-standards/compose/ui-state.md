# Compose UI state

- **SHOULD** represent screen state as an immutable model.
- **SHOULD** expose observable state from a screen state holder (commonly `StateFlow`).
- **SHOULD** model mutually exclusive states with sealed types when that improves correctness; use data properties for independent dimensions.
- **MUST** distinguish persistent UI state from one-time effects such as navigation or transient messages.
- **MUST** define behavior for loading, empty, content and error states where relevant.
- **SHOULD** hoist state to the lowest common owner that needs to coordinate it.
- **SHOULD** keep reusable composables stateless where practical by accepting values and callbacks.
- **MUST** avoid storing derived state when it can be safely derived from source state.
- **MUST** avoid launching work directly during recomposition.

Company decision required: standardize the event delivery pattern and lifecycle-aware collection API for each target.
