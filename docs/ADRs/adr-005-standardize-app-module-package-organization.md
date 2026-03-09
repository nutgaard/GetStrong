# ADR-005: Standardize app-module package organization and ownership

Status: accepted

## Context
ADR-001 defines the layered architecture, but the developer still needs a concrete package map for milestone 1. Without a canonical structure, code will drift toward file-by-file convenience instead of layer ownership.

## Decision
Standardize package ownership inside the single `app` module as follows:

- `ui`: feature screens, navigation, UI state, design system, and ViewModels
- `domain`: business models, use cases, repository interfaces, and domain rules
- `data`: Room entities, DAOs, local data sources, mappers, repository implementations, and seed import code
- `di`: Hilt modules, bindings, and provider configuration

Inside `ui`, organize by feature flow rather than by widget type. The current flows are `home`, `programs`, `history`, `progress`, `settings`, `workoutEditor`, `exerciseDetail`, `activeWorkout`, and `summary`, with shared routing code under `ui/navigation`.

This ADR refines ADR-001. It does not change the layered model; it makes that model concrete enough for implementation.

## Consequences
- The developer gets a stable package map for `M1`.
- Feature code can grow incrementally without dissolving the layer boundaries.
- Review can apply a clear rule: `ui` does not depend on storage models, and `domain` does not depend on Android or DI details.
- Future extraction into multiple modules remains possible because ownership is explicit.
