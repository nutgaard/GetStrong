# ADR-002: Adopt single-activity Jetpack Compose navigation

Status: accepted

## Context
The product requires fast transitions between workout planning, active workout execution, and summary flows. A fragment-heavy approach would add lifecycle and XML overhead without solving a real MVP problem.

## Decision
Use a single Android activity as the app entry point and host the UI entirely with Jetpack Compose. Manage screen transitions with a single Navigation Compose graph.

Navigation is a presentation concern. Screens and routes live in the `ui` layer, while domain logic remains navigation-agnostic. Each major destination may have its own ViewModel, but route changes must not leak into `domain` contracts.

## Consequences
- UI iteration remains fast and aligned with modern Android guidance.
- Fragment and XML complexity are avoided during MVP.
- The team must keep composables as presentation components and avoid embedding business rules in the UI layer.
- Route conventions need to stay structured and stable so navigation does not become stringly coupled across the app.
