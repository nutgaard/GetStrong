# ADR-003: Use Hilt as the dependency injection mechanism

Status: accepted

## Context
The chosen architecture requires predictable construction of repositories, use cases, and ViewModels. Hand-wired dependency creation would become repetitive and fragile as more planning, execution, and persistence features are added.

## Decision
Use Hilt as the single dependency injection mechanism for the application.

- Bind repository interfaces to data-layer implementations in `di`.
- Construct ViewModels through Hilt.
- Keep DI annotations and module definitions out of `domain`.

Hilt is the only approved DI pattern for MVP. Do not introduce service locators, manual singleton registries, or a second DI framework.

## Consequences
- Dependency ownership becomes explicit, testable, and consistent.
- The build gains annotation-processing and configuration overhead.
- Wiring stays centralized in `di` instead of spreading constructor assembly logic through the app.
- Domain code remains unaware of the DI framework.
