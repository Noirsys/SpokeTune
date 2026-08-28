# SpokeTune implementation source of truth

> **Purpose:** This is the durable, checkable backlog for humans and iterative
> coding agents. The product contract lives in
> [`docs/PRODUCT_REQUIREMENTS.md`](docs/PRODUCT_REQUIREMENTS.md). If implementation
> and product requirements conflict, stop, record a decision, and update both.

## How to use this file

### Checkbox contract

- `[ ]` not started; `[~]` is **not valid Markdown** and must not be used to imply
  progress. Use the status board below for active ownership.
- `[x]` means the code/docs, tests, and evidence named by the task are committed.
- Never check a parent until every required child and its acceptance evidence pass.
- A checkbox is not evidence. Link the commit/PR, test, artifact, or decision beside
  the item when completing it.
- Discovered work receives a new stable ID; do not hide it inside a commit.
- Keep IDs stable. Deleted tasks are marked `CANCELLED` with an ADR link rather than
  removed or renumbered.

### Task metadata

Each task uses: **ID** · `phase` · `lane` · **dependencies** · **parallel safety**.

- `deps: none` means it can start from the current repository state.
- `after: X` means all named tasks must be complete first.
- `parallel: yes` means it can proceed beside other ready lanes if agents own
  disjoint files. `parallel: caution` means agree on an interface/fixture first.
- `MVP-GATE` means the release cannot ship while unchecked.

### Active work board

Update this before editing. One owner per task; one task per row. Remove completed
rows after adding evidence to the task.

| Task | Owner | Branch/worktree | Files reserved | Started | Notes |
| --- | --- | --- | --- | --- | --- |
| _none_ | | | | | |

### Iteration (“Ralph loop”) protocol

1. Read the PRD, this file, all applicable `AGENTS.md` files, ADRs, and current
   `git status`; never overwrite unrelated work.
2. Choose the highest-priority ready task whose dependencies are checked. Prefer
   the smallest vertical slice that leaves the repository healthier.
3. Claim it in the active work board and reserve files. For parallel agents,
   isolate worktrees/branches when available and communicate shared contracts.
4. Write or update the failing test/fixture first when practical. Implement only
   enough scope to satisfy the task and its quality gates.
5. Run the task checks plus the affected module checks. Record failures honestly;
   environment failures are not passes.
6. Review the diff for privacy, safety language, accessibility, lifecycle behavior,
   numerical edge cases, and accidental scope growth.
7. Update documentation/ADR and checkbox evidence in the same change. Commit one
   coherent task using `type(scope): summary`.
8. Remove the active claim, pull/rebase, resolve conflicts by intent, and repeat.

### Definition of done (applies to every implementation task)

- Acceptance criteria and dependency contracts are met.
- New behavior has deterministic automated tests at the cheapest useful layer.
- Static analysis, formatting, relevant unit/instrumentation tests, and build pass.
- UI includes loading, empty, content, permission-denied, and recoverable-error
  states where applicable; accessibility semantics and previews are included.
- No secrets, personal data, raw audio, generated build output, or machine-specific
  paths are committed.
- Public contracts and non-obvious DSP math are documented; user-visible copy is a
  string resource and does not overstate safety or accuracy.
- Performance-sensitive changes include measurement evidence, not intuition.
- The task checkbox includes a PR/commit and test evidence.

## Dependency graph and parallel lanes

```text
P0 research/decisions ─┬─> P1 bootstrap ─┬─> P2 domain/data ─┬─> P5 integration
                       │                 ├─> P3 DSP/audio ───┤       │
                       │                 └─> P4 design/UI ───┘       v
                       └─> fixture plan                         P6 hardening -> P7 release
```

After bootstrap contracts stabilize, these lanes can run concurrently:

| Lane | Owns | Can run alongside | Synchronization point |
| --- | --- | --- | --- |
| A — platform | Gradle, CI, app shell, DI | B/C/D after P1 | version catalog and module graph |
| B — signal | pure Kotlin DSP and fixtures | C/D | `PitchAnalyzer` contract and fixture manifest |
| C — data/domain | entities, repositories, use cases | B/D | IDs, units, session state machine |
| D — experience | design system, Compose screens | B/C | immutable UI models and navigation routes |
| E — quality | test tooling, benchmark, accessibility | all with file ownership | shared test conventions |

## Milestones

| Milestone | Exit condition |
| --- | --- |
| M0 Decisions | P0 risks tested; ADRs fix foundational contracts |
| M1 Walking skeleton | Debug app builds; CI and core navigation pass |
| M2 Offline core | Profiles/sessions persist and survive recreation |
| M3 Acoustic spike | Recorded and synthetic fixtures produce evidence |
| M4 End-to-end alpha | User can scan, review, repeat, export, and import |
| M5 Beta | Device, accessibility, performance, privacy, and migration gates pass |
| M6 MVP | Signed release candidate and operational checklist pass |

---

# P0 — Research, risk reduction, and decisions (ready now)

All P0 tasks have `deps: none` and are intentionally parallel-safe unless noted.

## Product and field research

- [ ] **P0-001** · `research` · `product` · `parallel: yes` — Interview at least
  five users spanning DIY, mechanic, wheel builder, and hub-motor contexts; capture
  workflows, terminology, failure modes, willingness to grant microphone access,
  and anonymized findings in `docs/research/user-interviews.md`.
- [ ] **P0-002** · `research` · `product` · `parallel: yes` — Observe at least three
  full wheel checks; map physical hand/phone placement, ambient noise, scan order,
  gloves/grease constraints, interruption behavior, and average task duration.
- [ ] **P0-003** · `research` · `product` · `parallel: yes` — Review competing pitch
  tuners and spoke-tension tools without copying protected UI; document gaps,
  pricing, privacy, offline behavior, and accessibility.
- [ ] **P0-004** · `research` · `safety` · `parallel: yes` · `MVP-GATE` — Have a
  qualified wheel builder review comparison assumptions, dangerous wording,
  damaged-wheel stop conditions, and hub-motor edge cases.
- [ ] **P0-005** · `research` · `legal` · `parallel: yes` · `MVP-GATE` — Decide
  distribution countries, privacy policy needs, open-source notices, disclaimer
  review, data-safety declarations, and analytics/crash-reporting consent.

## Acoustic feasibility spike

- [ ] **P0-010** · `research` · `signal` · `parallel: yes` — Define an audio fixture
  manifest: source, device/microphone, sample rate, bit depth, distance, environment,
  spoke/wheel metadata, physical tension if independently measured, expected pitch
  interval, consent/license, and SHA-256.
- [ ] **P0-011** · `research` · `signal` · `after: P0-010` · `parallel: caution` —
  Record a consented, redistributable corpus across round/bladed spokes, lengths,
  crossings, tensions, hub-motor/conventional wheels, quiet/shop/outdoor noise,
  cases/phone orientations, muted rings, double strikes, speech, and silence.
- [ ] **P0-012** · `research` · `signal` · `parallel: yes` — Build a throwaway JVM
  notebook/CLI comparing YIN/CMNDF, autocorrelation, and spectral validation on
  generated tones, harmonics, transients, and noise. Record accuracy, octave-error,
  reject-rate, latency, and CPU results; retain reusable fixtures, not prototype
  architecture.
- [ ] **P0-013** · `research` · `signal` · `after: P0-011,P0-012` · `parallel: no`
  — Set evidence-based pitch range, capture duration, transient exclusion, minimum
  SNR/amplitude, stability window, confidence threshold, and known unsupported
  cases. Record an ADR; if targets fail, revise scope before app implementation.
- [ ] **P0-014** · `research` · `signal` · `after: P0-011` · `parallel: yes` — Test
  whether touching/crossing/damping changes which segment rings; turn findings into
  user instructions and fixture categories.
- [ ] **P0-015** · `research` · `signal` · `after: P0-011` · `parallel: yes` — Pair a
  subset of recordings with a calibrated physical tensiometer to characterize
  correlation and explicitly bound what relative pitch can and cannot claim.

## Architecture and UX decisions

- [ ] **P0-020** · `decision` · `platform` · `parallel: yes` — ADR: select `minSdk`,
  `targetSdk`, Java/Kotlin toolchains, compile SDK, supported form factors, and
  device tiers using current Android distribution and API requirements.
- [ ] **P0-021** · `decision` · `platform` · `parallel: yes` — ADR: module graph and
  dependency rules (`app`, `core:model`, `core:domain`, `core:data`, `core:database`,
  `core:audio`, `core:signal`, `core:designsystem`, feature modules, test fixtures).
- [ ] **P0-022** · `decision` · `data` · `parallel: yes` — ADR: Room entities,
  migration policy, UUID generation, UTC timestamp representation, deletion,
  archival, and immutable measurement history.
- [ ] **P0-023** · `decision` · `platform` · `parallel: yes` — ADR: Compose,
  Navigation, coroutines/Flow, DI, Room, serialization, logging, crash reporting,
  and dependency-update policy. Prefer official Jetpack capabilities and justify
  every third-party runtime dependency.
- [ ] **P0-024** · `decision` · `experience` · `parallel: yes` — Specify deterministic
  spoke numbering, side assignment, clockwise/counter-clockwise traversal, visual
  lacing abstraction, RTL behavior, and accessible list equivalent with diagrams.
- [ ] **P0-025** · `decision` · `experience` · `parallel: yes` — Prototype wheel
  creation, scan, retry, and result flows; test clickable prototype with five users
  including TalkBack and one-handed scenarios; record changes.
- [ ] **P0-026** · `decision` · `statistics` · `parallel: yes` — ADR: same-side
  comparison cohorts, median/MAD formula, minimum sample count, MAD-zero behavior,
  threshold naming, missing values, superseded measurements, and rounding.
- [ ] **P0-027** · `decision` · `privacy` · `parallel: yes` · `MVP-GATE` — Threat and
  privacy model covering microphone, temporary PCM, exports, logs, backups,
  screenshots, content URIs, malicious imports, dependencies, and deletion.
- [ ] **P0-028** · `decision` · `quality` · `parallel: yes` — Define supported-device
  matrix (API levels, low/mid/high hardware, microphone characteristics, screen
  sizes) and test ownership.

---

# P1 — Repository and walking skeleton

Start P1 after P0-020, P0-021, and P0-023 settle the platform contracts.

## Build foundation

- [ ] **P1-001** · `bootstrap` · `platform` · `after: P0-020,P0-021,P0-023` ·
  `parallel: no` — Generate the Gradle Android project with wrapper, Kotlin DSL,
  version catalog, settings repositories, configured SDK/toolchains, application
  ID, debug/release variants, and reproducible configuration.
- [ ] **P1-002** · `bootstrap` · `platform` · `after: P1-001` · `parallel: no` — Add
  modules from the ADR, enforce dependency direction, centralize Android/Kotlin
  conventions in a build-logic included build, and prove each module compiles.
- [ ] **P1-003** · `bootstrap` · `platform` · `after: P1-001` · `parallel: yes` — Add
  `.editorconfig`, Android/Kotlin formatting, static analysis, lint policy, binary
  file policy, `.gitignore`, license/NOTICE strategy, and secret scanning.
- [ ] **P1-004** · `bootstrap` · `platform` · `after: P1-001` · `parallel: yes` —
  Enable dependency locking/verification, commit verification metadata, pin CI
  actions by immutable reference, and document dependency upgrade procedure.
- [ ] **P1-005** · `bootstrap` · `platform` · `after: P1-002` · `parallel: yes` — Add
  application class, single activity, Material theme, edge-to-edge behavior,
  lifecycle-aware collection, and a deterministic debug launcher.
- [ ] **P1-006** · `bootstrap` · `platform` · `after: P1-002` · `parallel: yes` —
  Configure DI and dispatcher abstractions; verify graph creation in a test and
  prohibit hard-coded production dispatchers/clocks in domain logic.

## CI and developer experience

- [ ] **P1-010** · `bootstrap` · `quality` · `after: P1-003` · `parallel: yes` — CI
  jobs for formatting, static analysis, Android lint, JVM tests, debug build, and
  dependency verification with cancellation, timeouts, minimal permissions, and
  cached-but-reproducible dependencies.
- [ ] **P1-011** · `bootstrap` · `quality` · `after: P1-005` · `parallel: yes` — Add
  emulator instrumentation workflow with retained test reports/screenshots and a
  small smoke test that launches the activity.
- [ ] **P1-012** · `bootstrap` · `quality` · `after: P1-001` · `parallel: yes` — Add
  `./gradlew doctor`-equivalent environment documentation, JDK/SDK setup, common
  commands, emulator creation, fixture handling, and troubleshooting to README.
- [ ] **P1-013** · `bootstrap` · `quality` · `after: P1-010` · `parallel: yes` — Add
  pull-request template with safety/privacy/accessibility/DSP checklist, code-owner
  expectations, contribution guide, and conventional commit guidance.
- [ ] **P1-014** · `bootstrap` · `quality` · `after: P1-010` · `parallel: yes` — Add
  coverage reports with per-module trend visibility; use meaningful critical-path
  expectations rather than optimizing a single global percentage.
- [ ] **P1-015** · `bootstrap` · `quality` · `after: P1-002` · `parallel: yes` — Add
  a test-fixtures module/builders, fake clock/dispatcher, deterministic IDs, and
  coroutine-test conventions.

## App shell

- [ ] **P1-020** · `skeleton` · `experience` · `after: P1-005` · `parallel: yes` —
  Define typed routes and top-level navigation for onboarding, wheel list/editor,
  session, results, settings, about, and licenses; test deep-link rejection and
  back-stack behavior.
- [ ] **P1-021** · `skeleton` · `experience` · `after: P1-005` · `parallel: yes` —
  Create design tokens, typography, dynamic-color policy, light/dark/high-contrast
  previews, icon policy, and semantic status colors that are never color-only.
- [ ] **P1-022** · `skeleton` · `experience` · `after: P1-020,P1-021` ·
  `parallel: yes` — Add placeholder screens with loading/empty/error content and
  screenshot/golden test infrastructure.
- [ ] **P1-023** · `skeleton` · `privacy` · `after: P1-005,P0-027` · `parallel: yes`
  — Configure manifest with no microphone permission until feature integration,
  explicit component export rules, network/security/backup policy, and tests or
  inspection for merged-manifest surprises.

---

# P2 — Domain, statistics, and persistence

P2 can run in parallel with P3 and P4 once module/API contracts are agreed.

## Domain model and rules

- [ ] **P2-001** · `domain` · `data` · `after: P0-022,P0-024,P1-002` ·
  `parallel: caution` — Implement typed IDs, `WheelProfile`, `Spoke`, `WheelSide`,
  `MeasurementSession`, `Measurement`, status enums, frequency/confidence value
  objects, timestamps, and validation; no Android types in model/domain modules.
- [ ] **P2-002** · `domain` · `data` · `after: P2-001` · `parallel: yes` — Implement
  wheel creation/edit/archive validation, even count 12–48, stable numbering, side
  mapping, and tests for boundaries and invalid edits.
- [ ] **P2-003** · `domain` · `data` · `after: P2-001` · `parallel: yes` — Implement
  session state machine: start, accept, reject, skip, supersede, complete, abandon,
  resume. Add exhaustive transition and idempotency tests.
- [ ] **P2-004** · `domain` · `statistics` · `after: P0-026,P2-001` ·
  `parallel: yes` — Implement median/MAD and comparison classification as pure
  functions with tests for odd/even groups, tiny groups, zero MAD, outliers,
  duplicates, NaN/infinity rejection, and deterministic rounding.
- [ ] **P2-005** · `domain` · `statistics` · `after: P2-003,P2-004` ·
  `parallel: yes` — Implement session summary/progress, per-side cohorts, provisional
  state, retry queue, and supersession selection with property-based tests.
- [ ] **P2-006** · `domain` · `experience` · `after: P2-002` · `parallel: yes` —
  Implement traversal sequence for start spoke/direction and tests for every
  supported count, wraparound, side labels, and RTL independence.

## Persistence

- [ ] **P2-010** · `data` · `database` · `after: P0-022,P2-001,P1-002` ·
  `parallel: caution` — Define Room schema, converters, indices, foreign keys,
  uniqueness constraints, DAO projections, and schema export; review query plans.
- [ ] **P2-011** · `data` · `database` · `after: P2-010` · `parallel: yes` — Create
  wheel DAO/repository with transactional create/update/archive/delete and Flow
  observation; unit/instrumentation tests cover empty/error/change streams.
- [ ] **P2-012** · `data` · `database` · `after: P2-010,P2-003` · `parallel: yes` —
  Create session/measurement DAO and repository with atomic accept/supersede,
  resumability, cascade semantics, and concurrent-write tests.
- [ ] **P2-013** · `data` · `database` · `after: P2-011,P2-012` · `parallel: yes` —
  Add repository integration tests for process reopen, cancellation, transaction
  rollback, corrupted inputs, and realistic 48-spoke multi-pass data.
- [ ] **P2-014** · `data` · `database` · `after: P2-010` · `parallel: yes` ·
  `MVP-GATE` — Establish migration test harness with exported schemas and require a
  forward migration test for every schema version; forbid destructive fallback in
  release.
- [ ] **P2-015** · `data` · `settings` · `after: P1-002` · `parallel: yes` — Add
  typed DataStore preferences for onboarding, capture hints, direction, theme, and
  privacy choices with corruption/default tests; do not duplicate relational data.

## Portable data

- [ ] **P2-020** · `data` · `interop` · `after: P2-001,P0-027` · `parallel: yes` —
  Specify JSON Schema v1, MIME type/file suffix, unit/timestamp conventions,
  extension policy, maximum sizes/counts/string lengths, and example fixtures.
- [ ] **P2-021** · `data` · `interop` · `after: P2-020,P2-011,P2-012` ·
  `parallel: yes` — Implement streaming export without audio or internal-only data;
  test deterministic semantic round trip and cancellation cleanup.
- [ ] **P2-022** · `data` · `interop` · `after: P2-020,P2-011,P2-012` ·
  `parallel: yes` — Implement defensive import into staging models, full validation,
  duplicate policy, preview, transactional commit, and actionable errors.
- [ ] **P2-023** · `data` · `security` · `after: P2-022` · `parallel: yes` ·
  `MVP-GATE` — Fuzz import with truncation, deep nesting, huge values, unknown
  fields/versions, duplicate IDs, traversal-like names, invalid Unicode and broken
  references; prove resource limits and no partial commits.

---

# P3 — Audio capture and pitch analysis

Keep signal processing pure Kotlin and Android capture behind an interface. Never
use production recordings in tests without consent and an explicit license.

## Signal-processing core

- [ ] **P3-001** · `signal` · `signal` · `after: P0-013,P1-002` ·
  `parallel: caution` — Define `PitchAnalyzer`, immutable configuration, PCM input,
  success metrics, rejection taxonomy, diagnostics safe for logs, and versioned
  algorithm ID. Contract tests reject invalid sample rates/buffers/configuration.
- [ ] **P3-002** · `signal` · `signal` · `after: P3-001` · `parallel: yes` — Add
  saturating PCM conversion/normalization, DC removal, amplitude/RMS/peak/noise
  metrics and tests for silence, offsets, clipping, minimum/maximum samples.
- [ ] **P3-003** · `signal` · `signal` · `after: P3-002` · `parallel: yes` — Add
  transient segmentation and bounded analysis windows; verify clicks/double plucks,
  late rings, truncated buffers, and no out-of-bounds allocation.
- [ ] **P3-004** · `signal` · `signal` · `after: P3-002` · `parallel: yes` — Add the
  evidence-selected band-pass/window strategy with response tests at pass/stop
  bands and supported sample rates; document phase/latency implications.
- [ ] **P3-005** · `signal` · `signal` · `after: P3-003,P3-004` · `parallel: no` —
  Implement YIN difference and cumulative mean normalized difference with bounded
  lag search, interpolation, threshold selection, and numerical tests against
  known arrays and generated tones.
- [ ] **P3-006** · `signal` · `signal` · `after: P3-005` · `parallel: yes` — Analyze
  overlapping frames and compute temporal pitch stability; handle attack/decay,
  missing frames, vibrato/beating, and conflicting candidates.
- [ ] **P3-007** · `signal` · `signal` · `after: P3-005` · `parallel: yes` — Add
  spectral/harmonic candidate validation to reduce octave/subharmonic errors;
  verify fundamentals weaker than harmonics and multi-tone interference.
- [ ] **P3-008** · `signal` · `signal` · `after: P3-006,P3-007` · `parallel: no` —
  Combine periodicity, amplitude/SNR proxy, stability, range, clipping, and harmonic
  plausibility into calibrated confidence and explicit rejection results.
- [ ] **P3-009** · `signal` · `quality` · `after: P3-008,P0-011` · `parallel: yes` ·
  `MVP-GATE` — Run immutable corpus evaluation and publish per-category error,
  octave-error, acceptance/rejection, confidence calibration, latency, and known
  limitations. Do not tune on the final holdout set.
- [ ] **P3-010** · `signal` · `quality` · `after: P3-008` · `parallel: yes` — Add
  property/fuzz tests ensuring finite bounded outputs, termination, deterministic
  results, input immutability, and safe handling of arbitrary PCM/configuration.
- [ ] **P3-011** · `signal` · `performance` · `after: P3-008` · `parallel: yes` — Add
  JVM microbenchmarks for DSP stages, allocation tracking, regression thresholds,
  and low-tier-device budget; optimize only from profiles.

## Android capture

- [ ] **P3-020** · `audio` · `platform` · `after: P1-006,P3-001` ·
  `parallel: caution` — Define `AudioCapture` state/events and fake implementation;
  cover idle/requesting/listening/analyzing/success/failure/cancelled transitions.
- [ ] **P3-021** · `audio` · `platform` · `after: P3-020` · `parallel: yes` — Implement
  `AudioRecord` configuration probing, minimum-buffer validation, mono PCM capture,
  bounded ring/buffer ownership, read-error mapping, and no unbounded retries.
- [ ] **P3-022** · `audio` · `platform` · `after: P3-021` · `parallel: yes` — Make
  capture lifecycle-safe: one owner, explicit cancellation, focus/call/interruption
  response, app backgrounding behavior, configuration changes, and resource release
  in every path. Add fake/state tests and StrictMode/manual evidence.
- [ ] **P3-023** · `audio` · `privacy` · `after: P3-021,P0-027` · `parallel: yes` ·
  `MVP-GATE` — Prove raw PCM is ephemeral: no database/files/cache/logs/analytics,
  clear buffers after use where practical, and document memory boundaries.
- [ ] **P3-024** · `audio` · `platform` · `after: P3-021` · `parallel: yes` — Add
  route-change detection and configuration diagnostics without device fingerprinting;
  show unsupported capture configuration rather than fake success.
- [ ] **P3-025** · `audio` · `quality` · `after: P3-022,P3-008` · `parallel: yes` —
  Device instrumentation tests feed known tones acoustically where feasible and
  validate captured-format handling; separate lab evidence from deterministic CI.

---

# P4 — User experience (parallel feature slices)

Each screen exposes immutable UI state/events, uses lifecycle-aware Flow collection,
and has phone/large-font/dark/RTL previews plus state and navigation tests.

## Design system and reusable components

- [ ] **P4-001** · `ui` · `experience` · `after: P1-021` · `parallel: yes` — Build
  scaffold, app bars, buttons, dialogs, banners, progress, permission rationale,
  empty/error states, unit formatting, and semantic status badge components.
- [ ] **P4-002** · `ui` · `experience` · `after: P0-024,P1-021,P2-001` ·
  `parallel: caution` — Build performant wheel diagram with stable geometry,
  numbered/selected/status spokes, zoom policy, large touch targets, no color-only
  meaning, and separate accessible list/grid representation.
- [ ] **P4-003** · `ui` · `quality` · `after: P4-001` · `parallel: yes` — Establish
  screenshot matrices and semantics assertions; define intentional-update workflow
  so goldens are reviewed rather than blindly regenerated.

## Onboarding and wheels

- [ ] **P4-010** · `feature` · `experience` · `after: P1-020,P2-015,P4-001` ·
  `parallel: yes` — Implement onboarding for value, limitations, privacy, safe-use
  stop conditions, and tutorial skip/replay. Do not request permission here.
- [ ] **P4-011** · `feature` · `experience` · `after: P2-011,P4-001` ·
  `parallel: yes` — Wheel list with loading/empty/content/error, archive/restore,
  delete confirmation, test data affordance only in debug, and state restoration.
- [ ] **P4-012** · `feature` · `experience` · `after: P2-002,P2-011,P4-001` ·
  `parallel: yes` — Wheel create/edit form with inline validation, keyboard/focus,
  unsaved-change confirmation, spoke-count explanation, optional fields, and no
  misleading geometry-to-tension implication.
- [ ] **P4-013** · `feature` · `experience` · `after: P4-002,P4-012` ·
  `parallel: yes` — Wheel detail shows diagram, metadata, past sessions, new/resume
  actions, archive state, and safe empty history.

## Capture and session

- [ ] **P4-020** · `feature` · `experience` · `after: P2-003,P2-006,P4-002` ·
  `parallel: caution` — Session setup selects start spoke/direction, explains side
  comparisons, validates resumable draft, and confirms abandoning another draft.
- [ ] **P4-021** · `feature` · `experience` · `after: P3-020,P4-020` ·
  `parallel: caution` — Capture screen state machine with selected spoke, large
  capture/cancel/retry controls, timer/progress without fake precision, waveform or
  level semantics, keep-screen-on only while active, and interruption recovery.
- [ ] **P4-022** · `feature` · `privacy` · `after: P4-021` · `parallel: yes` ·
  `MVP-GATE` — Implement just-in-time runtime microphone permission: rationale,
  grant, deny, “don't ask again,” settings return, revocation while open, and a
  useful non-capture path. Test state transitions; never permission-loop.
- [ ] **P4-023** · `feature` · `experience` · `after: P3-008,P4-021` ·
  `parallel: caution` — Measurement proposal shows Hz, confidence wording, side,
  accept/retry/skip and safe rejection coaching for silence/noise/clipping/
  instability/out-of-range. Keep technical diagnostics out of user copy.
- [ ] **P4-024** · `feature` · `experience` · `after: P2-003,P4-023` ·
  `parallel: yes` — Auto-advance, manual selection, undo-as-supersede, skipped/retry
  queue, progress by side/total, draft persistence, and completion confirmation.
- [ ] **P4-025** · `feature` · `accessibility` · `after: P4-024` · `parallel: yes` ·
  `MVP-GATE` — TalkBack scan flow provides concise announcements, list navigation,
  adjustable progress, no audio-only status, logical focus after auto-advance, and
  no focus theft during live level updates.

## Results, data controls, and help

- [ ] **P4-030** · `feature` · `experience` · `after: P2-005,P4-002` ·
  `parallel: yes` — Results summary with separate side cards, sample sufficiency,
  median/spread methodology, legend, frequency-vs-tension caveat, and filters.
- [ ] **P4-031** · `feature` · `experience` · `after: P4-030` · `parallel: yes` —
  Spoke result detail/history shows accepted/superseded/rejected attempts, confidence,
  relative label, repeat action, and comparison cohort without adjustment commands.
- [ ] **P4-032** · `feature` · `experience` · `after: P2-021,P2-022,P4-001` ·
  `parallel: yes` — Export/share and import/preview flows use system pickers/content
  URIs, explain included data, handle cancellation/errors/duplicates, and never ask
  for broad storage permission.
- [ ] **P4-033** · `feature` · `experience` · `after: P2-015,P4-001` ·
  `parallel: yes` — Settings for theme, tutorial replay, capture hints, privacy/data
  controls, local deletion, diagnostic version info, and reset with confirmation.
- [ ] **P4-034** · `feature` · `content` · `after: P0-004,P4-001` · `parallel: yes` ·
  `MVP-GATE` — Help/about content: plucking technique, environment tips, limitations,
  damage stop conditions, relative methodology, privacy, licenses, feedback path,
  and version; review all safety language.

---

# P5 — End-to-end integration and alpha

- [ ] **P5-001** · `integration` · `platform` · `after: P2-012,P3-022,P3-008,P4-024`
  · `parallel: no` — Wire permission -> capture -> analysis -> proposal -> atomic
  persistence -> auto-advance with injected boundaries and structured, audio-free
  diagnostics. Add happy-path and every-rejection integration tests.
- [ ] **P5-002** · `integration` · `platform` · `after: P5-001,P4-030` ·
  `parallel: yes` — Wire live progress/results to repositories; verify same-side
  isolation, provisional thresholds, supersession, completion, and reopen behavior.
- [ ] **P5-003** · `integration` · `reliability` · `after: P5-001` · `parallel: yes`
  — Test rotation, font/theme change, background/foreground, process recreation,
  permission revocation, audio route change, rapid taps, cancellation at every
  suspension point, and low-storage/database failure.
- [ ] **P5-004** · `integration` · `quality` · `after: P5-002,P4-032` ·
  `parallel: yes` — Automated user journeys: first run, permission denial/recovery,
  create wheel, full fake 12/32/36/48 scans, reject/retry/skip, resume, results,
  remeasure, export/import, archive/delete.
- [ ] **P5-005** · `integration` · `quality` · `after: P5-002` · `parallel: yes` —
  Build debug-only deterministic demo/fake-audio mode gated out of release; use it
  for screenshots, UI tests, accessibility audits, and store assets.
- [ ] **P5-006** · `integration` · `observability` · `after: P5-001,P0-027` ·
  `parallel: yes` — Define privacy-preserving event/error taxonomy with algorithm
  version and coarse outcomes only; prove no PCM, exact spectra, wheel names, notes,
  file contents, or stable device fingerprint enter logs/telemetry.
- [ ] **P5-007** · `integration` · `product` · `after: P5-004,P5-005` ·
  `parallel: yes` — Moderated alpha with target personas on varied wheels/devices;
  log task success, retries, false confidence, comprehension, safety issues, and
  prioritized changes without collecting audio unexpectedly.
- [ ] **P5-008** · `integration` · `content` · `after: P5-002` · `parallel: yes` —
  Audit every string for “frequency” versus “tension,” side comparison, uncertainty,
  actionable errors, consistent terminology, localization readiness, and reading
  level.

---

# P6 — Hardening and beta gates

## Correctness and reliability

- [ ] **P6-001** · `hardening` · `quality` · `after: P5-004` · `parallel: yes` ·
  `MVP-GATE` — Full unit/instrumentation/screenshot suite is deterministic under
  repeat runs; quarantine is forbidden without owner, issue, rationale, and expiry.
- [ ] **P6-002** · `hardening` · `quality` · `after: P2-014,P5-004` ·
  `parallel: yes` · `MVP-GATE` — Test clean install, every historical migration,
  upgrade with draft/complete data, backup/restore policy, clear-data, and uninstall.
- [ ] **P6-003** · `hardening` · `quality` · `after: P5-003` · `parallel: yes` ·
  `MVP-GATE` — 2-hour soak with repeated captures/navigation and lifecycle churn;
  no leaked recorder, runaway coroutine, ANR, unbounded heap, or corrupt session.
- [ ] **P6-004** · `hardening` · `signal` · `after: P3-009,P5-007` ·
  `parallel: caution` · `MVP-GATE` — Freeze algorithm/config version from held-out
  corpus plus field evidence; publish limitations and regression thresholds.
- [ ] **P6-005** · `hardening` · `quality` · `after: P5-004` · `parallel: yes` — Run
  mutation/property testing on statistics, traversal, state transitions and import;
  add tests for surviving meaningful mutants and boundary failures.

## Performance and device compatibility

- [ ] **P6-010** · `hardening` · `performance` · `after: P5-001,P3-011` ·
  `parallel: yes` · `MVP-GATE` — Macrobench startup and capture-to-result on low/
  mid/high tiers; enforce agreed p50/p95, frame, CPU, allocation, thermal and energy
  budgets with reproducible traces.
- [ ] **P6-011** · `hardening` · `performance` · `after: P6-010` · `parallel: yes` —
  Generate and verify baseline profiles for startup and core scan/results journeys;
  test release-like non-debuggable artifacts.
- [ ] **P6-012** · `hardening` · `quality` · `after: P3-025,P5-004,P0-028` ·
  `parallel: yes` · `MVP-GATE` — Complete device matrix across API tiers, OEMs,
  microphone routes, cases, orientations, screen sizes, dark mode, locale/RTL,
  offline mode, battery saver, interruptions, and noisy environments.

## Accessibility, privacy, and security

- [ ] **P6-020** · `hardening` · `accessibility` · `after: P4-025,P5-004` ·
  `parallel: yes` · `MVP-GATE` — Automated scanner plus manual TalkBack, Switch
  Access/keyboard, 200% font, display scaling, contrast, grayscale, reduced motion,
  touch-target, focus-order and hearing-independent audits; close all blockers.
- [ ] **P6-021** · `hardening` · `privacy` · `after: P3-023,P5-006` ·
  `parallel: yes` · `MVP-GATE` — Inspect network traffic, files, backups, logs,
  crash reports, database, exports and memory workflow to verify privacy model and
  data-safety/privacy-policy statements.
- [ ] **P6-022** · `hardening` · `security` · `after: P2-023,P1-004` ·
  `parallel: yes` · `MVP-GATE` — Dependency/license/vulnerability review, SAST,
  secret scan, exported-component/content-provider inspection, malicious intent/
  URI/import tests, release manifest review, and remediation sign-off.
- [ ] **P6-023** · `hardening` · `safety` · `after: P4-034,P5-007` ·
  `parallel: yes` · `MVP-GATE` — Final product, qualified-mechanic, and legal review
  of onboarding/results/help/store copy; prove no prescriptive or safety claim.
- [ ] **P6-024** · `hardening` · `localization` · `after: P5-008` ·
  `parallel: yes` — Pseudolocalization, RTL, long-string, plurals, decimals, Hz/unit,
  date/time, truncation and content-description audit; fix concatenated strings.

---

# P7 — Release and operations

- [ ] **P7-001** · `release` · `platform` · `after: P6-001,P6-002,P6-022` ·
  `parallel: caution` · `MVP-GATE` — Configure release signing via protected CI
  secrets, R8/resource shrinking, mapping/native symbol retention, deterministic
  versioning, provenance/SBOM, and signed AAB; no key material in repository.
- [ ] **P7-002** · `release` · `quality` · `after: P7-001` · `parallel: yes` ·
  `MVP-GATE` — Test release AAB installed from internal track: clean/upgrade,
  offline capture journey, import/export, licenses, obfuscation, crash symbolication,
  and no debug/demo endpoints or flags.
- [ ] **P7-003** · `release` · `product` · `after: P5-005,P6-023` ·
  `parallel: yes` — Produce honest store name/description, screenshots, icon,
  feature graphic, privacy-policy/support URLs, content rating, microphone rationale,
  and data-safety declarations; assets reflect release UI.
- [ ] **P7-004** · `release` · `operations` · `after: P6-004,P6-021` ·
  `parallel: yes` · `MVP-GATE` — Create dashboards/alerts for crashes, ANRs, capture
  outcome categories, performance regressions and import failures without sensitive
  payloads; define owner, severity, escalation and retention.
- [ ] **P7-005** · `release` · `operations` · `after: P7-002,P7-003,P7-004` ·
  `parallel: no` · `MVP-GATE` — Stage rollout with stop/rollback thresholds, support
  runbook, known-issues page, feedback triage, on-call owner, and review checkpoints.
- [ ] **P7-006** · `release` · `product` · `after: all MVP-GATE tasks` ·
  `parallel: no` · `MVP-GATE` — Go/no-go review verifies PRD acceptance, metric
  definitions, evidence links, zero P0/P1 defects, signed approvals, and rollback.
- [ ] **P7-007** · `release` · `operations` · `after: P7-005` · `parallel: yes` —
  Conduct 24-hour/7-day/30-day reviews, compare indicators to targets, audit user
  reports for dangerous misunderstanding, and create prioritized follow-up tasks.

---

# Post-MVP backlog (not release blockers)

- [ ] **POST-001** — Research wheel-specific calibration from paired frequency and
  physical tensiometer measurements, with uncertainty propagation and expiration.
- [ ] **POST-002** — Add absolute N/kgf only after spoke geometry/material modeling,
  traceable calibration, validation, safety review, and explicit uncertainty.
- [ ] **POST-003** — Support bladed/aero spokes and nonstandard lacing only with
  separate acoustic evidence and comparison cohorts.
- [ ] **POST-004** — Add optional encrypted sync/backup only after account, threat,
  consent, deletion, portability, offline-conflict, and operations design.
- [ ] **POST-005** — Explore external calibrated tensiometer/Bluetooth integration
  with protocol ownership, reconnect behavior, firmware compatibility, and mocks.
- [ ] **POST-006** — Evaluate tablets, foldables, Chromebooks and professional
  multi-wheel batch workflows from observed demand.
- [ ] **POST-007** — Explore camera-assisted runout visualization as a distinct
  measurement with independent validation; never infer it from pitch.

---

# Cross-cutting test catalog

These cases must be assigned to owning tasks and automated wherever deterministic.

## DSP fixtures

- [ ] Pure sine at boundaries and semitone/fractional frequencies.
- [ ] Fundamental weaker than second/third harmonic; missing fundamental.
- [ ] White/pink/shop/traffic/wind noise at multiple SNRs.
- [ ] Click only, silence, clipped pluck, double pluck, speech/music, two spokes.
- [ ] Attack/decay, beating, damping, late onset, truncated ring, DC offset.
- [ ] 8/16/24-bit source conversion fixtures and every supported capture sample rate.
- [ ] Real round/bladed/crossed/hub-motor cases split into tune/dev/holdout sets.

## State and lifecycle

- [ ] Permission grant, denial, permanent denial, revocation and settings return.
- [ ] Rotate/background/process-kill before, during and after capture/analyze/save.
- [ ] Audio route change, phone call/focus loss, recorder init/read/dead-object errors.
- [ ] Rapid repeated controls, concurrent starts, cancellation and stale result.
- [ ] Database full/corrupt/closed, transaction cancellation and clock/ID collision.

## Data/statistics

- [ ] 12/14/.../48 spokes, wraparound, invalid odd/out-of-range count and both sides.
- [ ] No/one/two samples, identical group/MAD zero, extreme outlier, missing/skipped,
  superseded attempts, NaN/infinity and rounding boundaries.
- [ ] Clean/current/future/unknown/truncated/oversized/malicious imports and duplicate
  wheel/session IDs with all-or-nothing commit.

## UI/accessibility

- [ ] Light/dark/dynamic/high contrast; phone/foldable/tablet bounds; cutouts/insets.
- [ ] 200% font/display scale, RTL, pseudolocale, keyboard/Switch Access and TalkBack.
- [ ] Loading/empty/error/offline/permission/rejection states and restored focus.
- [ ] Color-blind/grayscale status, touch targets, reduced motion and no audio-only cue.

---

# Decision log template

Create `docs/adr/NNNN-short-title.md`:

```markdown
# NNNN: Decision title
Status: Proposed | Accepted | Superseded
Date: YYYY-MM-DD
Owners: ...

## Context and evidence
## Options considered
## Decision
## Consequences and risks
## Validation / reversal trigger
## Related task IDs
```

# Defect and discovery intake

When a loop finds a defect or missing requirement, append it here before selecting
it. Promote it into the appropriate phase with a stable ID during triage.

| Date | Reporter | Related ID | Severity | Discovery | Proposed next step |
| --- | --- | --- | --- | --- | --- |
| | | | | | |

# Current project state

- Walking alpha implemented in `f79c545`: Gradle/Compose app shell, onboarding,
  wheel/session UI, just-in-time microphone permission, bounded ephemeral capture,
  YIN/CMNDF analyzer, pure domain/statistics module, strict portable-data model,
  unit tests, and a buildable debug APK.
- Verified on 2026-08-28 with `testDebugUnitTest assembleDebug` plus passing
  `:core:domain:test`, `:signal:test`, and `:core:data:test`. No Android device was
  connected, so installation, real microphone behavior, and visual/device QA are
  not yet verified.
- The results surface is still demonstration data; accepted readings are not yet
  persisted into a complete multi-spoke session. Room persistence, real results
  wiring, import/export UI, lifecycle instrumentation, and accessibility QA remain.
- External P0 research and all qualified wheel-builder, legal, field-corpus, and
  device-matrix gates remain open. This artifact is not a release candidate.
- Highest technical risk remains real-world fundamental-frequency reliability
  across phones, spokes, crossings, damping, and ambient noise—not microphone access.

# Research starting points

Verify versions and platform behavior at implementation time; these are starting
points, not dependency pins:

- Android Developers, “Guide to app architecture”:
  https://developer.android.com/topic/architecture
- Android Developers, `AudioRecord` reference:
  https://developer.android.com/reference/android/media/AudioRecord
- Android Developers, requesting runtime permissions:
  https://developer.android.com/training/permissions/requesting
- Android Developers, app quality and accessibility:
  https://developer.android.com/docs/quality-guidelines/core-app-quality
- Android Developers, Room migrations:
  https://developer.android.com/training/data-storage/room/migrating-db-versions
- de Cheveigné & Kawahara, “YIN, a fundamental frequency estimator” (JASA, 2002),
  DOI: https://doi.org/10.1121/1.1458024
