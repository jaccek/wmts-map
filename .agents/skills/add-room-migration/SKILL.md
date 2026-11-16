---
name: add-room-migration
description: Add or modify a Room database schema migration safely, preserving user data and following company migration and testing standards.
---

# Add Room migration

## Safety first
1. Read Room database and migration standards.
2. Inspect current entities, database version, exported schemas and existing migration tests.
3. Determine the exact old and new schema and supported upgrade paths.
4. Explain data-loss risks and the migration plan before making a risky change.
5. Never use destructive migration as a shortcut.

## Implement
1. Update entities/indices/relations as required.
2. Increment the database version according to project conventions.
3. Implement a migration from each supported prior version.
4. Preserve existing data and define defaults/nullability deliberately.
5. Update exported schema artifacts.
6. Register the migration in the database builder.
7. Update affected DAO/repository mappings if required.

## Test
- Test fresh database creation.
- Test each supported migration path.
- Verify existing rows and values survive.
- Verify new columns, indices, constraints and relations.
- Test relevant DAO behavior after migration.

## Completion
List source and target schema versions, migration paths tested, data-preservation assertions and checks not run. Do not claim a migration is safe without tests.
