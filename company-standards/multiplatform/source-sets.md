# Kotlin Multiplatform source-set placement

Use the narrowest source set that can own the implementation.

| Source set | Suitable content |
|---|---|
| `commonMain` | Pure Kotlin logic, shared models, interfaces, portable libraries |
| `commonTest` | Platform-independent tests |
| `androidMain` | Android framework APIs and Android-specific implementations |
| `androidUnitTest` / `androidInstrumentedTest` | Android JVM or device tests, according to project setup |
| `iosMain` | Apple platform implementations shared by configured iOS targets |
| `iosTest` | iOS-specific tests where configured |

- **MUST** verify actual source-set names in the project's Gradle configuration; hierarchies vary.
- **MUST NOT** add platform dependencies to common source sets.
- **SHOULD** avoid duplicating common logic in platform source sets.
- **SHOULD** keep platform adapters thin and delegate policy to shared code.
