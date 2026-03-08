# ADR-003: Use Hilt as the dependency injection mechanism

Status: accepted

## Context
The base architecture requires verifiable dependency injection, ViewModel wiring, and scalable construction of repositories and use cases. A standardized Android DI approach reduces custom bootstrapping.

## Decision
Use Hilt as the application dependency injection framework. Bind repository interfaces to data-layer implementations in the DI layer, inject ViewModels through Hilt, and keep the domain layer unaware of the DI framework.

## Consequences
- Dependency ownership becomes explicit and test-friendly.
- The project incurs annotation-processing and build configuration overhead.
- Service-locator style access patterns should be avoided because Hilt is the single DI mechanism.
- Framework-specific DI annotations remain outside the domain layer.
