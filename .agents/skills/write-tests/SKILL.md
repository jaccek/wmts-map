---
name: write-tests
description: Create or improve deterministic Kotlin tests for domain logic, ViewModels, Flow, Ktor, Room or multiplatform behavior.
---

# Write tests

## Analyze
1. Read testing strategy and the relevant technology testing standard.
2. Inspect nearby tests and the project's test framework/conventions.
3. Identify externally observable behavior and edge cases.
4. Avoid testing private implementation details.

## Test design
- Cover success, failure and important boundary conditions.
- Keep tests isolated, deterministic and independent of live services.
- Use fakes for simple collaborators; use mocks when interaction verification adds value.
- For coroutine tests, use the shared test scheduler and avoid real delays.
- For Ktor, use the project's mock engine.
- For Room, use isolated databases and migration tests.
- For KMP, place tests in the narrowest suitable source set.

## Quality
- Use descriptive names.
- Keep each test focused.
- Do not weaken production code to satisfy brittle tests.
- Ensure resources are cleaned up.

## Completion
List scenarios covered, test tasks executed and any platform coverage gaps.
