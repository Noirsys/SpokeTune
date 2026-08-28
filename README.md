# SpokeTune

SpokeTune is an experimental Android utility for measuring the pitch of plucked bicycle
spokes and comparing relative tension consistency on each side of a wheel.

The repository now contains a buildable walking-alpha implementation with:

- a safety/privacy-first Compose flow for creating a wheel and starting a pass;
- just-in-time microphone permission and bounded, ephemeral PCM capture;
- an Android-independent YIN/CMNDF pitch analyzer with synthetic tests;
- stable spoke numbering, side-isolated median/MAD comparisons, and traversal tests;
- versioned, strictly validated JSON import/export models; and
- a generated debug APK at `app/build/outputs/apk/debug/app-debug.apk` after a build.

Build with `gradlew.bat --no-daemon --max-workers=1 test assembleDebug` (the low
worker count keeps the build reliable on memory-constrained development PCs).

This is a **walking alpha, not a safety-validated release**. Its real-spoke pitch
range and confidence thresholds still require the fixture corpus, device matrix,
qualified wheel-builder review, accessibility audit, and other MVP gates below.
Start with:

- [`docs/PRODUCT_REQUIREMENTS.md`](docs/PRODUCT_REQUIREMENTS.md) for the product
  scope, users, safety boundaries, and MVP acceptance criteria.
- [`IMPLEMENTATION.md`](IMPLEMENTATION.md) for the executable backlog, dependency
  graph, parallel work lanes, quality gates, and agent workflow.

The implementation plan is the project's source of truth. Check off an item only
when its stated evidence and all applicable quality gates exist in the repository.
