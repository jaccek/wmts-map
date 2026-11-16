---
name: code-review
description: Review a Kotlin or Kotlin Multiplatform change for correctness, architecture, security, concurrency, persistence, testing and company standards. Use for reviewing diffs or proposed implementations.
---

# Code review

## Review approach
1. Read project `AGENTS.md`, pinned standards version and applicable standards.
2. Inspect the full diff and relevant surrounding code.
3. Focus on defects and risks introduced by the change, not unrelated legacy issues.
4. Verify claims against actual code and tests.

## Review dimensions
- Correctness and edge cases.
- Architecture and dependency direction.
- Kotlin API clarity and null safety.
- Coroutine ownership, cancellation and thread safety.
- Compose state, side effects and lifecycle.
- KMP source-set placement and platform compatibility.
- Ktor serialization, timeouts, error mapping and credential safety.
- Room schema compatibility, migration safety and data preservation.
- Test coverage and deterministic behavior.
- Security, logging and sensitive-data handling.
- Unnecessary dependencies or unrelated changes.

## Findings
Report findings first, ordered by severity. For each finding include:
- severity (Critical/High/Medium/Low);
- file and line/range;
- concrete failure scenario;
- why it violates a requirement or creates a defect;
- a practical remediation.

Do not report speculative style preferences as defects. If no findings are identified, state the scope reviewed and any validation not performed. A code review is not a substitute for running tests.
