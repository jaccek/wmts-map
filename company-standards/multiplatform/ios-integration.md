# iOS and Swift integration

- **MUST** design exported Kotlin APIs with Swift ergonomics in mind.
- **SHOULD** expose small, stable facades rather than internal implementation graphs.
- **SHOULD** avoid unnecessarily complex generic, nested or overloaded APIs at the Kotlin/Native boundary.
- **MUST** define how suspend functions and Flow are consumed from Swift using the project's approved interop approach.
- **MUST** document ownership and lifecycle of callbacks, delegates and native resources.
- **SHOULD** avoid retaining Swift objects indefinitely through Kotlin callbacks.
- **MUST** verify changes using the project's Xcode/iOS build path when available.

Company decision required: choose and document the supported Flow-to-Swift bridge, async API conventions, framework packaging and minimum iOS version.
