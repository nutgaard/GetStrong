# ADR-001: Use a single-module layered clean architecture for MVP

Status: accepted

## Context
The project needs to move quickly on a P0 setup task while still preserving a clean separation between UI, business rules, and storage. The acceptance criteria require domain, data, and UI structure, but the current scope does not justify a large multi-module Gradle layout.

## Decision
Adopt a single Android app module for MVP with explicit package boundaries for ui, domain, data, and di. Use MVVM in presentation, use cases and repository interfaces in domain, and repository implementations plus local data sources in data. Keep domain pure Kotlin and framework-independent.

## Consequences
- Project setup and build times stay simpler during MVP.
- Architectural boundaries rely on discipline and review rather than Gradle-enforced isolation.
- Future extraction into feature or library modules remains possible because contracts are defined early.
- Cross-layer imports must be treated as governance violations if they bypass the intended architecture.
