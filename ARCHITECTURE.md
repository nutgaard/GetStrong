# Architecture

This file is maintained by the architect agent.

## Principles
- Keep decisions high-level and stable.

## Module Boundaries
- 

## Dependency Rules
- 

## Implementation Notes
-

## Task T1: Android project setup and base architecture

### Principles
- Prefer a simple MVP architecture with strong boundaries over early modular complexity.
- Keep domain logic for progression, warmups, failure detection, and deloading pure, deterministic, and independently testable.
- Use offline-only, local-first assumptions throughout the stack; do not introduce network concepts or dependencies.
- Adopt state-driven UI with unidirectional flow: UI renders state, ViewModels orchestrate, use cases execute business rules.
- Define stable contracts at layer boundaries via repository interfaces and explicit model mapping.
- Optimize for future feature growth in workout planning, execution, and summaries without locking the team into premature Gradle module fragmentation.

### Module Constraints
- Use a single Android app Gradle module for MVP delivery; enforce separation through package boundaries and interfaces rather than multiple feature modules at this stage.
- Maintain top-level package boundaries for ui, domain, data, and di; optionally add a small core/common package only for stable cross-cutting abstractions.
- domain must contain business entities, value objects, repository interfaces, and use cases only; it must remain pure Kotlin and free of Android framework types.
- data must contain local data sources, persistence models, mappers, and repository implementations; it is the only layer allowed to know storage details.
- ui must contain screens, ViewModels, navigation setup, and presentation-specific state/models; it must not embed progression or warmup calculation rules.
- di must act only as the composition root for wiring dependencies; it must not become a secondary business layer.
- Feature subpackages should reflect the PRD domains such as planning, catalog, execution, progression, and summary, but those features must still honor ui/domain/data separation.
- Do not introduce remote, sync, or account-related packages because they are outside MVP scope and conflict with offline-only assumptions.

### Dependency Rules
- ui may depend on domain and approved framework/common abstractions, but never directly on data implementations.
- data may depend on domain contracts and local persistence libraries, but never on ui.
- domain must not depend on AndroidX, Compose, Hilt, Room, or any other framework-specific APIs.
- Repository interfaces belong in domain; repository implementations belong in data; DI binds implementations to interfaces.
- Navigation concerns and route definitions stay in ui/navigation; domain and data remain navigation-agnostic.
- Framework models, database entities, and UI state models must be mapped at boundaries rather than reused across layers.
- Use coroutines and Flow as the default async boundary mechanism where streaming or observation is needed; keep synchronous pure logic pure.
- No network libraries, HTTP clients, remote DTOs, or online fallback paths are allowed in this baseline architecture.
- Unit tests should primarily target domain use cases and pure calculators on the JVM; presentation tests should validate ViewModel orchestration rather than business rule internals.

### Required ADRs
- ADR-001
- ADR-002
- ADR-003
- ADR-004

### Risks
- A single-module MVP can suffer boundary erosion if package rules are not consistently enforced.
- If business logic leaks into ViewModels or composables, progression and warmup behavior will become hard to validate and reuse.
- Choosing Compose, Navigation Compose, and Hilt improves velocity but adds build tooling complexity and annotation-processing overhead.
- Workout execution, timers, and active-session lifecycle handling are inherently stateful and can become fragile if state ownership is not centralized early.
- Local persistence schema for workouts, summaries, exercise catalog, and progression history will likely evolve, so migration discipline will matter soon after setup.
- Premature overengineering into many modules would slow MVP delivery, but deferring module boundaries too long could increase refactor cost later.

### Notes
Recommended baseline: single-activity Android app using Jetpack Compose for UI, Navigation Compose for screen flow, MVVM for presentation, domain use cases plus repository interfaces for business orchestration, and Hilt for dependency injection. Keep the app in one Gradle module for now, but make ui/domain/data boundaries explicit from day one. Treat progression rules, warmup generation, failure counting, and deload calculations as pure domain logic. Treat repositories as local-first abstractions suitable for Room-backed persistence when persistence work begins. This setup satisfies the placeholder-screen, navigation, DI, and testability goals without committing to unnecessary complexity.
