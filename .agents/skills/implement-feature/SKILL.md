---
name: implement-feature
description: Implement a feature in an existing Kotlin or Kotlin Multiplatform project while following company architecture and testing standards. Use for end-to-end feature work spanning presentation, domain or data layers.
---

# Implement feature

## Before editing
1. Read the project's `AGENTS.md` and `company-standards/standards.lock.json`.
2. Read `company-standards/engineering/general.md` and the relevant architecture, technology and security standards.
3. Inspect the module graph and at least one nearby feature implemented in the project.
4. Identify the user-visible behavior, affected layers, platform targets and acceptance criteria.
5. For non-trivial work, present a short plan and list assumptions before editing.

## Implementation
1. Keep the change within the smallest set of modules that owns the capability.
2. Follow existing project patterns unless they conflict with an explicit company MUST.
3. Keep business rules out of composables and platform framework classes.
4. Reuse existing repositories, components, DI, navigation and error abstractions.
5. Do not add dependencies or public API changes without justification and required approval.
6. Implement loading, empty, success and failure behavior where relevant.
7. Add tests for business rules, mappings and important state transitions.

## Validation
- Run the narrowest relevant tests first.
- Run the project's documented formatting, static-analysis and compilation tasks.
- Validate affected Android/iOS targets where the environment permits.
- Report skipped checks and why they could not run.

## Completion report
Summarize behavior, changed modules/files, standards applied, tests/checks and known risks. Never claim unexecuted validation passed.
