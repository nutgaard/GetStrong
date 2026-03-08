# ADR-002: Adopt single-activity Jetpack Compose navigation

Status: accepted

## Context
The app needs a placeholder screen now and will later require fast, low-friction flows for workout planning and workout execution. A single-activity model aligns well with modern Android navigation and avoids fragment-heavy setup.

## Decision
Use a single activity hosting a Jetpack Compose UI tree and a Navigation Compose graph. Each destination or major flow is represented by UI state plus a ViewModel. Navigation remains a presentation concern and does not cross into domain logic.

## Consequences
- UI iteration is fast and consistent with modern Android guidance.
- There is less fragment and XML overhead in the base project.
- The team must keep composables stateless where practical and avoid embedding business rules in UI code.
- Typed or well-structured route conventions should be maintained to avoid fragile navigation coupling.
