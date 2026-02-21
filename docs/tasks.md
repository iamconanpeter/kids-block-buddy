# Kids Block Buddy — MVP Execution Tasks

This task list is implementation-oriented and prioritized for fast, safe MVP delivery.

---

## P0 (Must Ship)

- [ ] `[P0]` Plan artifacts aligned: `docs/spec.md`, `docs/technical-plan.md`, `docs/tasks.md` reflect current MVP scope and safety constraints.
- [ ] `[P0]` Implement non-punitive welcome-back reward: if last snapshot update was `>=24h`, grant modest stars and set a friendly hint.
- [ ] `[P0]` Keep reward deterministic/testable through pure domain logic with explicit timestamps.
- [ ] `[P0]` Persist updated stars/snapshot immediately after reward grant to avoid instant re-grants on reopen.
- [ ] `[P0]` Add/expand unit tests for reward engine and touched mission/state logic.
- [ ] `[P0]` Verify compile and quality gates with `testDebugUnitTest` and `assembleDebug`.

## P1 (Important Polish)

- [ ] `[P1]` Add a small UX signal for granted return reward (friendly hint wording is clear and celebratory, never pressure-based).
- [ ] `[P1]` Confirm mission hints still behave correctly when welcome-back hint is applied.
- [ ] `[P1]` Validate snapshot timestamp usage stays consistent across load/save paths.
- [ ] `[P1]` Document reward constants (window and bonus amount) in code for easy tuning.

## P2 (Post-MVP / Nice-to-Have)

- [ ] `[P2]` Add analytics-safe local counter for reward grants (no personal data).
- [ ] `[P2]` Add parent-facing explanation of non-punitive return rewards in settings/help.
- [ ] `[P2]` Add integration test harness for ViewModel load/reopen scenarios.

---

## Done Gate Checklist (All Required)

- [ ] Scope gate: MVP remains Android Kotlin Compose and child-safe (no chat, no outbound links in child flow).
- [ ] Behavior gate: Welcome-back stars grant exactly once per eligible return window.
- [ ] Fairness gate: no streak loss or punitive copy tied to absence.
- [ ] Determinism gate: reward eligibility logic is timestamp-driven and unit-tested.
- [ ] Persistence gate: rewarded stars + snapshot timestamp are saved after grant.
- [ ] Test gate: new and existing unit tests pass.
- [ ] Build gate: `assembleDebug` succeeds.
- [ ] Change gate: modified files and verification results are summarized in handoff.

---

## Execution Notes

- Keep diffs small and isolated to docs + reward flow.
- Prefer domain-first logic for new behavior; keep UI/ViewModel orchestration thin.
- Preserve existing style and avoid unrelated refactors.
