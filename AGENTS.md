# Project instructions

This project follows the pinned Company AI Engineering Standards release.

## Project overview

- Product: WMTS Map
- Main technologies: Compose Multiplatform
- Supported targets: Android, iOS
- Dependency injection: Koin
- Design system: Material Design 3

Fill these fields with verified project facts. Do not guess.

## Repository map

Project contains 2 submodules. One is a wmts-map for reusable library. The second one is example. It shows wmts-map possibilities.

## Company standards

Read `company-standards/README.md` to find applicable standards.

Always read:
- `company-standards/engineering/general.md`
- `company-standards/engineering/architecture.md`
- `company-standards/engineering/error-handling.md`
- `company-standards/engineering/security.md`

Also read the technology standards relevant to the files being changed.

## Project-specific rules

There is no project-specific standards.

## Build and validation commands

Replace the placeholders below with commands verified in this repository:

- Compile shared/Android: `./gradlew :example:assembleDebug`
- Compile iOS framework: `./gradlew :example:linkDebugFrameworkIosSimulatorArm64`
- Unit tests: `./gradlew :wmts-map:iosSimulatorArm64Test` (common tests executed on the iOS simulator target; `:example` has no tests)
- Formatting check: Not configured
- Static analysis: Not configured

Do not claim a command passed unless it was executed successfully.

## Workflow

1. Inspect the existing implementation and relevant tests.
2. Identify affected modules and applicable standards.
3. For non-trivial work, propose a concise plan before editing.
4. Implement the smallest complete change.
5. Add or update tests.
6. Run the relevant project validation commands.
7. Report changed files, checks run, results and known limitations.

## Safety

- Do not commit secrets or production data.
- Do not perform destructive database changes without explicit approval.
- Do not add production dependencies without justification and approval.
- Do not make unrelated refactors.
- Ask before changing public APIs, authentication, encryption or data-retention behavior.
