# S4 Phase 2 Codex Hand-off

## 1. Project Identity

- Project root: `D:/Github/MediBox`
- Project type: Android local-first medicine cabinet app
- Tech stack: Kotlin 2.0.21, Jetpack Compose, Material3, Room, CameraX, ML Kit Chinese OCR, WorkManager
- Build toolchain:
  - Gradle wrapper: 8.9
  - Android Gradle Plugin: 8.5.2 (build warning says tested up to compileSdk 34)
  - Kotlin plugin: 2.0.21
  - KSP: 2.0.21-1.0.28
  - compileSdk / targetSdk: 35
  - minSdk: 29
  - JDK: 17 (Eclipse Adoptium 17.0.20.8-hotspot from build commands)
- Git status:
  - `.git` metadata was not discoverable from `D:/Github/MediBox` or its parent walk during this audit.
  - Because of that, branch and recent commits could not be read reliably.
  - Working tree cleanliness could not be verified via Git for the same reason.

## 2. Actual File Map

### Root
- `app/`
- `docs/`
- `.tools/gradle-dist/gradle-8.9/`

### Core app sources
- `app/src/main/java/com/medibox/MediBoxApp.kt`
- `app/src/main/java/com/medibox/data/`
- `app/src/main/java/com/medibox/domain/`
- `app/src/main/java/com/medibox/ui/`

### Data layer
- `app/src/main/java/com/medibox/data/local/db/MediBoxDatabase.kt`
- `app/src/main/java/com/medibox/data/local/db/Converters.kt`
- `app/src/main/java/com/medibox/data/local/db/PhotoFileManager.kt`
- `app/src/main/java/com/medibox/data/local/db/dao/MedicineDao.kt`
- `app/src/main/java/com/medibox/data/local/db/dao/LocationDao.kt`
- `app/src/main/java/com/medibox/data/local/db/dao/StockRecordDao.kt`
- `app/src/main/java/com/medibox/data/local/db/entity/Medicine.kt`
- `app/src/main/java/com/medibox/data/local/db/entity/Location.kt`
- `app/src/main/java/com/medibox/data/local/db/entity/StockRecord.kt`
- `app/src/main/java/com/medibox/data/repository/MedicineRepository.kt`
- `app/src/main/java/com/medibox/data/repository/LocationRepository.kt`

### Domain
- `app/src/main/java/com/medibox/domain/ApproxDate.kt`
- `app/src/main/java/com/medibox/domain/ExpiryChecker.kt`
- `app/src/main/java/com/medibox/domain/StockChangeType.kt`
- `app/src/main/java/com/medibox/domain/ocr/OcrModels.kt`
- `app/src/main/java/com/medibox/domain/ocr/TextNormalizer.kt`
- `app/src/main/java/com/medibox/domain/ocr/CandidateParser.kt`
- `app/src/main/java/com/medibox/domain/ocr/StabilityTracker.kt`

### UI
- `app/src/main/java/com/medibox/ui/screens/ScanScreen.kt`
- `app/src/main/java/com/medibox/ui/screens/OcrConfirmScreen.kt`
- `app/src/main/java/com/medibox/ui/screens/OcrConfirmViewModel.kt`
- `app/src/main/java/com/medibox/ui/screens/DetailScreen.kt`
- `app/src/main/java/com/medibox/ui/screens/DetailViewModel.kt`
- `app/src/main/java/com/medibox/ui/screens/ExpiringScreen.kt`
- `app/src/main/java/com/medibox/ui/screens/InventoryScreen.kt`
- `app/src/main/java/com/medibox/ui/navigation/Routes.kt`
- `app/src/main/java/com/medibox/ui/navigation/NavGraph.kt`

### Tests
- `app/src/test/java/com/medibox/ui/screens/OcrConfirmViewModelTest.kt`
- `app/src/test/java/com/medibox/ui/screens/DetailViewModelTest.kt`
- `app/src/test/java/com/medibox/data/MediBoxDataLayerTest.kt`
- `app/src/test/java/com/medibox/domain/ApproxDateTest.kt`

### Docs
- `docs/DEVELOPMENT_LOG.md`
- `docs/S4_MVP_DESIGN.md`

## 3. Build / Test Status

- `assembleDebug`: PASS
- `testDebugUnitTest`: BLOCKED

## 4. Current S4 Progress

### S4 Phase 1: PASS

Why:
- `OcrConfirmScreen` is implemented.
- `OcrConfirmViewModel` exists.
- OCR candidate -> form -> repository -> detail route is wired.
- Date precision is preserved via `ApproxDate`.
- Default location uses `LocationRepository` and the default location concept is present.
- This was already coded and remains present in the current tree.

### S4 Phase 2: BLOCKED

Why:
- `DetailScreen` is implemented and no longer a placeholder.
- `DetailViewModel` exists and handles load/edit/stock/delete flows.
- `assembleDebug` passes.
- `testDebugUnitTest` still fails.
- The remaining failure is not a missing entity/DAO/schema issue; it is a Robolectric-side runtime problem combined with app startup side effects and still-failing assertions in the new ViewModel tests.

### S4 Phase 3: NOT STARTED

Current phase 3 pages (`ExpiringScreen`, `InventoryScreen`) remain placeholders only and should not be expanded before Phase 2 is closed.

## 5. DetailScreen / DetailViewModel Audit

### What exists
- Detail page shows medicine name, spec, expiry status, stock, location, manufacturer, batch number, production date, user notes, photo placeholder, and stock record section.
- Edit mode exists with fields for name/spec/manufacturer/batchNo/productionDate/expiryDate/user notes/location/photoPath.
- Stock operations exist: purchase, increase, use, discard, decrease, adjust.
- Delete flow exists with confirmation dialog.
- Navigation after delete is tightened by clearing the success message after popping back.

### What is truly usable
- Load existing medicine by ID.
- Enter edit mode.
- Save edits through `MedicineRepository.updateMedicine`.
- Submit stock operations through repository transaction methods.
- Delete medicine through repository.
- Show expiry status using `ExpiryChecker`.

### What is still rough / not fully closed
- The page still shows placeholder-like UI in some places (for example photo display is text-only, stock record section is not a full operational history view).
- There is no separate stock-operation dialog; operations are directly wired as buttons with hardcoded amounts.
- `DetailViewModel` exposes a fairly large state object and uses `reload()` after saves/stock updates.
- Some UI text/status messaging is duplicated in the content area and state handling.

### Architecture checks
- No direct DAO write in `DetailScreen` or `DetailViewModel` was found.
- `ExpiryChecker` is used for status and display strings.
- `ApproxDate` is preserved via parse/toStorageString behavior.
- No Entity/DAO/Database/Migration changes are required for the current Phase 2 design.

## 6. Test Failure Investigation

### A. `DetailViewModelTest`

Observed failures in the latest run:
- `loads_medicine_location_and_expiry_status`
- `missing_medicine_returns_error`
- `editing_form_initializes_from_medicine`
- `stock_operation_updates_repository`
- `invalid_stock_operation_keeps_error`
- `delete_medicine_removes_entity_and_records`
- `editing_invalid_date_is_rejected`
- `editing_preserves_year_month_precision`

Most likely causes from the current trace:
- The tests are using Robolectric + in-memory Room correctly, but the `ViewModel` itself still runs async work in `viewModelScope`, so some assertions may be executed before state settles.
- The tests currently do not always wait for coroutine completion in a way that matches the production code path.
- The tests still assume immediate state transition for some flows that are launched asynchronously.

### B. `OcrConfirmViewModelTest`

Observed failures in the latest run:
- `candidate_initializes_form_and_preserves_year_month_precision`
- `empty_candidate_allows_manual_entry`
- `invalid_date_blocks_save_and_keeps_form`
- `save_success_returns_medicine_id_and_persists_year_month_precision`
- `save_failure_keeps_form`

Current root cause from the stack trace:
- Robolectric executes `MediBoxApp.onCreate()` during application startup.
- `MediBoxApp.onCreate()` launches `locationRepository.ensureDefaultLocation()` on `Dispatchers.IO`.
- That call hits SQLite through Room / Robolectric shadowed SQLite.
- The resulting failure is `IllegalStateException: Illegal connection pointer` from `ShadowLegacySQLiteConnection`.

Why this matters:
- The test setup is still using a context path that causes the production `Application` initialization to run.
- The default location seed is a production startup side effect, but in Robolectric it can collide with the test lifecycle and Room connection handling.

### Recommended fix direction for Codex
Do **not** change Entity / DAO / Database / Migration / Repository contract.
Prefer test-side isolation first:
1. Introduce a test-only `Application` or test runner / manifest setup that does not execute the production `onCreate()` seed path.
2. Or inject test repositories / fakes into the ViewModel tests so they do not require a real `MediBoxApp` instance.
3. If application startup must stay, isolate the default-location seeding behind a test-visible guard or startup delegate with minimal production change.

The preferred order is test-side isolation first, then the smallest possible production dependency seam only if necessary.

## 7. Build Verification

Latest verified result:
- `./gradlew assembleDebug` = PASS
- `./gradlew clean` = PASS
- `./gradlew testDebugUnitTest` = BLOCKED

Relevant failure detail for `testDebugUnitTest`:
- `IllegalStateException: Illegal connection pointer` from Robolectric SQLite shadow during `LocationRepository.ensureDefaultLocation()`.
- This comes through `MediBoxApp.onCreate()` during test runtime.

## 8. Guardrails for Codex

Codex must not:
1. Guess the project path.
2. Rely on old conversation state instead of reading files.
3. Do broad refactors.
4. Change production initialization behavior just to make tests green.
5. Change database structure to work around tests.
6. Change Repository contract to suit tests.
7. Start Phase 3 early.
8. Delete existing functionality.
9. Announce PASS based on fake or partial results.
10. Announce Phase 2 complete while `assembleDebug` or `testDebugUnitTest` is still failing.
11. Expand S4 beyond Phase 2 while fixing this blocker.

## 9. Current File / State Map for Codex

```text
D:/Github/MediBox
├── app
│   ├── src/main/java/com/medibox
│   │   ├── MediBoxApp.kt
│   │   ├── data/
│   │   │   ├── local/db/
│   │   │   │   ├── MediBoxDatabase.kt
│   │   │   │   ├── dao/
│   │   │   │   └── entity/
│   │   │   └── repository/
│   │   ├── domain/
│   │   │   ├── ApproxDate.kt
│   │   │   ├── ExpiryChecker.kt
│   │   │   └── ocr/
│   │   └── ui/
│   │       ├── navigation/
│   │       └── screens/
│   └── src/test/java/com/medibox
│       ├── data/
│       ├── domain/
│       └── ui/screens/
├── docs/
│   ├── DEVELOPMENT_LOG.md
│   └── S4_MVP_DESIGN.md
└── .tools/gradle-dist/gradle-8.9/
```

## 10. Codex Next Step

### P0
Fix or isolate the current Phase 2 test environment blocker.

### P1
Re-run:
- `./gradlew testDebugUnitTest`
- `./gradlew assembleDebug`

### P2
Re-check `DetailScreen` and `DetailViewModel` runtime behavior after tests are unblocked.

### P3
Do a quick Phase 2 UI / UX / state-flow review.

### P4
Only after all verification passes, mark:
- `S4 Phase 2 = COMPLETE`

Then stop.

## 11. Explicit Stop Rules

Codex must stop here and must not start Phase 3.

---

## Final hand-off summary

PROJECT_ROOT:
D:/Github/MediBox

CURRENT_BRANCH:
UNVERIFIED (Git metadata not discoverable from the current workspace path)

BUILD:
PASS

UNIT_TEST:
BLOCKED

S4_PHASE1:
PASS

S4_PHASE2:
BLOCKED

S4_PHASE3:
NOT_STARTED

DATABASE_CHANGED:
NO

REPOSITORY_CONTRACT_CHANGED:
NO

CURRENT_BLOCKER:
Robolectric test runtime still trips `MediBoxApp.onCreate()` / `LocationRepository.ensureDefaultLocation()` and hits `Illegal connection pointer`; DetailViewModelTest also has assertion mismatches.

CODEX_NEXT_ACTION:
Isolate the test runtime from production app startup side effects, then re-run unit tests and keep Phase 2 bounded.

DO_NOT_START_PHASE3:
YES
