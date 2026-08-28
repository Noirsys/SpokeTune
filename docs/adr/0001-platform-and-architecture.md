# 0001: Android platform and application architecture
Status: Accepted
Date: 2026-08-28
Owners: SpokeTune maintainers

## Context and evidence
SpokeTune needs low-latency microphone access, reliable offline storage, and an accessible wheel-scanning flow. AndroidX now defaults new libraries to API 23, covering approximately 99% of active Play devices. The development machine currently has API 34 installed.

## Options considered
- API 21, maximizing reach at higher compatibility and test cost.
- API 23, matching the AndroidX default support floor.
- API 26+, simplifying platform behavior while excluding more working shop devices.

## Decision
Use a single-activity Jetpack Compose Android app with `minSdk 23`, `targetSdk 34`, and four dependency-directed modules: `app`, `core:domain`, `core:data`, and `signal`. Domain and signal processing remain pure Kotlin. UI uses unidirectional state and talks to data through repositories. Raise target/compile SDK only with compatibility testing.

## Consequences and risks
API 23 devices remain in scope and require lifecycle/audio testing. Target SDK 34 is intentionally conservative for the installed toolchain and must be raised before Play distribution if policy requires it. No cloud, account, or network permission is introduced.

## Validation / reversal trigger
Revisit if field research shows meaningful target-device loss, a required dependency raises its floor, or Play policy requires a newer target.

## Related task IDs
P0-020, P0-021, P0-022, P1-001
