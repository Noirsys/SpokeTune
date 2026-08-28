# Product Requirements Document: SpokeTune

| Field | Value |
| --- | --- |
| Status | Approved for MVP planning |
| Version | 1.0 |
| Platform | Android |
| Product type | Acoustic wheel-building assistant |
| Source of truth for execution | [`../IMPLEMENTATION.md`](../IMPLEMENTATION.md) |

## 1. Product summary

SpokeTune helps bicycle and e-bike owners, mechanics, and wheel builders measure
the resonant frequency of a plucked spoke. It records a measurement against a
spoke in a visual wheel map, compares spokes **within the same wheel side**, and
highlights measurements that are inconsistent with that side's robust center.

The MVP is a relative consistency tool. It does not infer exact nipple turns,
diagnose wheel safety, measure runout or dish, or replace a calibrated tensiometer
and an experienced mechanic. Absolute tension is reserved for a later,
calibrated workflow.

## 2. Problem

Experienced builders often compare spoke pitch by ear, but pitch discrimination,
background noise, spoke identification, and record keeping are error-prone. Hub
motors and asymmetric hubs make it especially dangerous to assume both wheel
sides should have the same pitch. Users need a repeatable way to:

1. associate a captured pitch with the correct spoke;
2. see whether it is high or low relative to comparable spokes;
3. reject unreliable captures rather than present false precision; and
4. repeat a full pass after adjustment without losing the prior pass.

## 3. Goals and success measures

### MVP goals

- Create, edit, archive, and restore wheel profiles locally.
- Represent common even spoke counts from 12 through 48 and preserve stable
  spoke numbering and side assignment.
- Capture microphone audio only after explicit user action.
- Estimate a stable fundamental frequency with a confidence score and clear
  rejection reasons.
- Store accepted measurements in immutable sessions and allow re-measurement.
- Compare only compatible spokes (at minimum, the same wheel side).
- Show progress, side medians, dispersion, and clearly labelled outliers.
- Work offline and keep microphone recordings ephemeral by default.
- Export and import a documented, versioned wheel/session format.
- Be accessible, testable, observable without collecting sensitive audio, and
  honest about limitations.

### Launch indicators

| Indicator | MVP target |
| --- | --- |
| Valid synthetic tones detected from 100–2,000 Hz | median error <= 1% in the supported test matrix |
| Invalid/silent fixture rejection | >= 95% |
| Complete-session crash-free rate | >= 99.5% in release monitoring |
| A first-time user completes a 32/36-spoke scan | >= 80% in moderated usability testing |
| Accepted capture shown after pluck | p95 <= 2 seconds on the minimum supported device |
| Accessibility scanner blockers in core flow | 0 |

Targets are release gates to validate, not claims that are already achieved.

## 4. Users and jobs

### Primary personas

- **DIY owner:** identify unusually pitched spokes before deciding whether to
  adjust the wheel or visit a mechanic.
- **Working mechanic:** record consistent passes and quickly revisit rejected or
  suspicious spokes.
- **Wheel builder:** use repeatable relative readings as one input alongside
  lateral/radial runout, dish, stress relief, and physical tension measurements.
- **Hub-motor e-bike owner:** compare sides separately on a high-load wheel.

### Core job story

When I am checking a wheel, I want to capture each spoke in a known sequence and
see trustworthy same-side comparisons, so I can find measurements worth manually
checking without relying only on my ear.

## 5. MVP experience

1. The user sees the safety/privacy onboarding and grants microphone permission
   only when starting their first capture.
2. They create a wheel profile: name, vehicle/wheel type, spoke count, and
   optional notes. Advanced geometry is optional and must not imply calibrated
   tension.
3. They start a session, choose a starting spoke and direction, and see a numbered
   wheel diagram plus a large non-visual capture control.
4. The app listens for a short bounded interval. It discards the impact transient,
   estimates pitch, checks stability/confidence/range, and either proposes a
   reading or explains how to retry.
5. The user accepts, retries, or skips. Auto-advance follows the deterministic
   numbering order and the selected direction.
6. Results remain provisional until enough compatible samples exist. The result
   view shows frequency, confidence, sample status, same-side median and spread,
   but never prescribes a nipple turn.
7. The user can repeat an individual spoke or begin another pass. Prior accepted
   readings remain auditable.

## 6. Functional requirements

### Profiles and sessions

- Wheel names need not be unique; IDs are generated and stable.
- Supported spoke counts are even integers from 12 through 48 for MVP.
- A profile records spoke count and an explicit side-assignment strategy.
- Sessions have draft, complete, and abandoned states.
- Accepted captures are append-only. A superseded measurement remains linked to
  its replacement.
- Deleting a wheel requires confirmation and describes cascading local deletion.

### Capture and analysis

- Use Android microphone APIs with a mono PCM stream and an app-selected supported
  sample rate. Never silently fall back to fabricated results.
- The analyzer contract accepts PCM plus sample rate and returns a sealed success
  or rejection result independent of Android UI code.
- Preprocessing includes DC removal, bounded filtering/windowing, impact-transient
  exclusion, and amplitude/noise checks.
- Fundamental estimation begins with YIN/CMNDF-style detection. FFT/spectral data
  may validate candidates and detect harmonic mistakes, but is not the sole
  estimator.
- The supported pitch range is configurable and verified against recorded-spoke
  fixtures before release. The initial engineering test range is 100–2,000 Hz;
  product copy must not promise that range until validation is complete.
- Confidence must combine periodicity, temporal stability, usable amplitude, and
  harmonic plausibility. Low-confidence results are rejected, not silently saved.
- Raw audio remains in memory and is discarded after analysis unless the user
  explicitly opts into a future diagnostic feature. No such opt-in is in MVP.

### Comparison

- Never compare left and right side medians as though they should match.
- Use robust statistics (median and median absolute deviation) when the group has
  enough data; define deterministic fallback behavior when MAD is zero or the
  group is too small.
- Labels describe frequency relative to the comparable group (for example,
  “lower frequency than right-side median”), not proven tension or safety.
- Thresholds are configurable constants with unit tests and visible methodology.

### Import/export

- Export uses a versioned JSON schema, stable units, ISO-8601 timestamps, and no
  raw audio.
- Import validates size, schema, enums, numeric bounds, referential integrity, and
  duplicates before a transaction commits.
- Sharing uses Android's secure content URI mechanism rather than broad storage
  permissions.

## 7. Non-functional requirements

- **Minimum platform:** decide and record `minSdk` during bootstrap based on the
  supported-device study; do not choose it accidentally from a template.
- **Architecture:** single-activity Compose UI; unidirectional data flow;
  presentation, domain, data, and signal-processing boundaries; repositories are
  the only data entry point for UI-facing logic.
- **Reliability:** process recreation cannot corrupt a session; database migrations
  have upgrade tests; capture resources release on every lifecycle transition.
- **Performance:** audio analysis runs off the main thread with bounded queues;
  UI remains responsive and capture does not accumulate unbounded buffers.
- **Privacy:** offline-first, least-privilege permissions, no raw audio analytics,
  no advertising identifier, and documented deletion/export behavior.
- **Accessibility:** screen-reader alternatives to the wheel diagram, 48 dp touch
  targets, scalable text, non-color status cues, logical focus, reduced-motion
  behavior, and meaningful announcements.
- **Localization:** all user-visible strings are resources; quantities and units
  use locale-aware formatting; diagrams support RTL without changing spoke IDs.
- **Security:** dependency verification, release signing outside the repository,
  no secrets in source, strict import parser, and exported components minimized.

## 8. Safety and product language

Every onboarding and result surface must communicate that spoke pitch is only one
signal. Corrosion, damage, spoke shape, crossings, damping, length, material,
wheel geometry, runout, dish, and stress state can affect the observation or the
correct repair. Users must be told to stop riding and consult a qualified mechanic
for broken/damaged parts, severe looseness, instability, rim damage, or uncertainty.

Forbidden MVP claims include:

- exact absolute tension without calibration;
- exact adjustment such as “tighten 1/8 turn”;
- “safe to ride” or “wheel is true”;
- equal pitch required across opposite sides;
- medical-grade or laboratory-grade accuracy.

## 9. Out of scope for MVP

- iOS, web, cloud sync, accounts, social/community features, and ads.
- Automatic spoke selection based only on audio.
- Camera measurement of radial/lateral runout or dish.
- Bluetooth tensiometer integration.
- Prescriptive adjustment instructions.
- Absolute N/kgf estimates, spoke catalogs, or uncalibrated string-equation output.
- Background or continuous microphone monitoring.

## 10. Release acceptance criteria

The MVP is releasable only when all `MVP-GATE` items in the implementation plan
are checked, fixture-derived accuracy is documented, supported devices pass the
manual matrix, privacy/safety copy has been reviewed, data migrations and
export/import round trips pass, accessibility blockers are closed, and no open
P0/P1 defects remain.

## 11. Post-MVP candidates

- Wheel-specific calibration from paired physical tensiometer readings.
- Absolute-tension estimates with uncertainty intervals and compatible spoke data.
- Multiple lacing patterns and paired/bladed-spoke workflows.
- Optional encrypted backup/sync.
- Tablet/Chromebook layouts and professional batch workflows.
