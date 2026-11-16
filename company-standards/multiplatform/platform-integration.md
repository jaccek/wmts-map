# Android and iOS platform integration

- **MUST** isolate platform APIs behind clear boundaries.
- **SHOULD** expose capability-oriented interfaces to shared code rather than platform framework objects.
- **MUST** define lifecycle, threading and resource ownership for platform callbacks and native resources.
- **MUST** handle permission denial, unavailable hardware/services and platform errors explicitly.
- **SHOULD** use platform-native APIs when they provide materially better integration, accessibility or performance.
- **SHOULD** keep platform-specific UI native only when product or platform requirements justify it; shared Compose UI is also valid.
- **MUST** test platform adapters on their target platform when behavior depends on native APIs.

Do not assume Android and iOS have identical lifecycle, background execution, storage, permission or networking behavior.
