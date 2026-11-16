---
name: create-compose-screen
description: Create or modify a Compose Multiplatform screen following company UI architecture, state management, design-system reuse and testing standards. Use for new screens or significant screen changes.
---

# Create Compose screen

## Analyze
1. Read the Compose architecture, UI state, components and navigation standards.
2. Inspect an existing screen with similar complexity.
3. Identify the screen state owner, route, design-system components and platform-specific requirements.
4. Confirm whether the screen belongs in shared or platform-specific UI.

## Design
- Define the screen's observable state and user actions before building the UI.
- Reuse existing design-system tokens and components.
- Keep state immutable and derive display values where practical.
- Distinguish durable screen state from one-time effects.

## Implement
1. Implement or extend the state holder/ViewModel using the project's established pattern.
2. Expose state through the project's approved observable mechanism.
3. Implement a composable that renders state and forwards callbacks.
4. Cover loading, empty, content and error states as applicable.
5. Integrate navigation through the project's approved route mechanism.
6. Keep business logic, database access and HTTP calls outside composables.
7. Provide accessibility semantics and meaningful labels.

## Test and validate
- Test state transitions and business decisions.
- Add UI tests for critical interactions where the project supports them.
- Compile the affected targets and run relevant checks.

Do not introduce a new navigation or state-management framework as part of a screen task without approval.
