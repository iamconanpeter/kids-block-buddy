# Kids Block Buddy — Technical Plan (Plan Mode, No Implementation)

**Selected Concept:** Block Buddy Village  
**Phase:** Pre-implementation architecture and delivery planning only

---

## 1) Technical Goals

1. Deliver a smooth, premium-feeling block-building experience on mid-tier Android devices.
2. Keep architecture simple enough for a small team while supporting future content expansion.
3. Enforce child-safe constraints at system level (not just UX text).
4. Ensure robust save reliability and quick resume.

## Non-Goals (for initial release)
- Real-time multiplayer
- User-generated content sharing
- Open text/voice communication
- Large procedural infinite worlds

---

## 2) Proposed Technology Stack

- **Language:** Kotlin
- **UI Layer:** Jetpack Compose for menus/HUD/settings + dedicated game render surface for world scene
- **Architecture:** Clean-ish modular layers (UI / Domain / Data / Platform)
- **Persistence:** Room (player profile/progression/settings) + file-based binary/JSON chunk save for world state snapshots
- **Audio:** Android AudioTrack/SoundPool abstraction for low-latency SFX + media player for music
- **DI:** Lightweight manual DI or Hilt (team preference dependent)
- **Testing:** JUnit + instrumented tests + deterministic simulation tests for world logic

---

## 3) System Architecture (High-Level)

## 3.1 Modules
1. **app-shell**
   - App lifecycle, navigation, parent gate, permissions, language/theme
2. **game-runtime**
   - Main loop, input translation, world simulation tick, render orchestration
3. **world-core**
   - Voxel grid/chunk model, placement rules, undo/redo stack, interaction raycast
4. **mission-system**
   - Request cards, objective tracking, adaptive difficulty rules, hint ladder
5. **progression-system**
   - XP/stars, unlock trees, district completion, cosmetic inventory
6. **safety-system**
   - Child mode constraints, offline-first mode, telemetry guardrails, parent controls
7. **save-system**
   - Atomic save, periodic checkpoint, corruption detection + fallback restore
8. **content-pack-system**
   - District templates, NPC dialogues, block sets, seasonal event definitions

## 3.2 Runtime Flow
- App start → profile load → safety policy init → world load/resume marker
- Session loop:
  - Poll input
  - Run simulation tick
  - Resolve missions
  - Update HUD state
  - Render frame
  - Autosave checkpoints (interval + milestone triggered)

---

## 4) Data Model Plan

## 4.1 Core Entities
- `PlayerProfile`: id, ageBand, accessibility settings, parent-lock flags
- `WorldState`: districtId, chunk data, placed blocks, interactable objects
- `MissionCard`: missionId, objective list, hint level, reward payload
- `ProgressState`: level, xp, unlockedBlocks, stickerAlbum, districtMilestones
- `SafetyState`: chatEnabled(false fixed), trackingMode(minimal), parentGateHash
- `SessionState`: activeMission, lastSafePosition, lastActionTime, resumePromptData

## 4.2 Save Strategy
- Save slots: `primary`, `backup_last_good`
- Atomic write via temp file + rename
- Validation checksum per world snapshot
- On load failure: auto-recover from backup + notify with kid-friendly message

---

## 5) Gameplay Systems Plan (Technical)

## 5.1 Controls and Input
- Input abstraction layer normalizes touch gestures:
  - movement joystick vector
  - camera drag vector
  - action taps
  - block palette selection
- Accessibility toggles:
  - larger controls
  - tap-to-move assist
  - camera sensitivity presets

## 5.2 Build/Placement Rules
- Grid snap + visual ghost preview
- Placement validator:
  - bounds checks
  - support checks
  - mission compatibility checks
- Undo/redo ring buffer (target: 20+; configurable)

## 5.3 Adaptive Difficulty Engine
- Signals monitored per session:
  - repeated failed placement
  - hint usage count
  - idle duration
  - objective completion time
- Adjustments applied:
  - reduce objective count
  - enable blueprint highlights
  - increase resource drops

## 5.4 Retention Systems (Non-Manipulative)
- Daily optional visitor missions (7-day rolling window)
- Weekly district spotlight tasks
- Reward schedule is deterministic and transparent

---

## 6) iOS-Inspired Quality Bar in Engineering Terms

1. **Touch-to-feedback latency target:** immediate visual acknowledgement on tap/drag.
2. **Frame pacing target:** stable rendering budget with minimal spikes on camera movement.
3. **Animation standards:** short, purposeful transitions (150–300ms UI range).
4. **Visual consistency:** shared design tokens (spacing, typography, color system).
5. **Resume reliability:** return to exact mission/context after process death.

---

## 7) Preventing Low-Quality Android Clone Failure Modes

- **Jank risk:** enforce perf budget profiling in every milestone.
- **Inconsistent UI:** central component library and strict token usage.
- **Asset incoherence:** single art bible (palette, block proportions, icon stroke style).
- **Save corruption:** dual-slot atomic persistence and startup integrity checks.
- **Ad-tech contamination:** child mode build flavor excludes ad/tracker SDKs by policy.
- **Frustrating difficulty spikes:** telemetry-backed adaptive rules with cap limits.

---

## 8) Quality Assurance Strategy

## 8.1 Test Categories
- **Unit tests:** world placement rules, mission evaluator, progression unlock logic.
- **Simulation tests:** scripted gameplay sequences for deterministic outcomes.
- **Instrumented tests:** onboarding flow, parent gate, resume behavior.
- **Performance tests:** frame time under stress scenarios (dense build zones).
- **Safety tests:** no external comm paths, parent gate enforcement, local data purge.

## 8.2 Exit Criteria for Build Readiness
- First-session funnel passes without blockers.
- Save/load reliability proven across force-close scenarios.
- Performance and memory targets met on reference devices.
- Safety and privacy checklist fully green.

---

## 9) Delivery Sequencing (Plan-Level)

1. Foundation systems (app shell, save, world core primitives)
2. Playable vertical slice (movement + placement + one mission)
3. Adaptive difficulty + fairness systems
4. Progression + retention loops
5. Parent safety controls and policy hardening
6. Content authoring pipeline for districts and mission cards
7. Polish + performance optimization + QA hardening

---

## 10) Model Recommendation for Implementation Phase

When implementation begins (after sign-off), use:
- **Model:** `gpt-5.3-codex`
- **Effort setting:** **high-effort** (for architecture-safe refactors, test completeness, and reliability-focused coding)

This project is child-facing and quality-sensitive; lower-effort generation is likely to increase rework and QA escape risk.
