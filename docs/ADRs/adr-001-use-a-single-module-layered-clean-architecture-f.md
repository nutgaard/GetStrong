# ADR-001: Use a single-module layered clean architecture for MVP

Status: accepted

## Context
The app needs to move quickly through MVP while still preserving clear boundaries between UI, business logic, and storage. The current scope does not justify a multi-module Gradle build, but it does require architecture that can absorb planning, execution, progression, and persistence features without collapsing into a single Android-centric code layer.

## Decision
Use a single Android `app` module for MVP and enforce a layered architecture inside that module.

- `ui` owns Compose screens, navigation, ViewModels, and presentation state.
- `domain` owns framework-independent business models, use cases, and repository contracts.
- `data` owns local persistence, data sources, mapping, and repository implementations.
- `di` owns dependency wiring only.

Use MVVM in the presentation layer. Keep `domain` free of Android framework types, Room entities, and DI annotations. Treat repository interfaces as domain contracts and repository implementations as data-layer details.

## Consequences
- Project setup, build time, and refactoring cost stay low during MVP.
- Architectural boundaries are enforced by conventions, reviews, and ADRs rather than Gradle module isolation.
- Future extraction into feature modules or library modules remains possible because contracts are established early.
- Direct shortcuts such as `ui -> data` or Android framework code in `domain` are architectural violations.
