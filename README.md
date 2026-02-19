# Kids Block Buddy (MVP)

Child-friendly Android block-building sandbox MVP inspired by the approved **Block Buddy Village** concept.

## Implemented MVP

- Landscape-first Android app (phone + tablet friendly)
- Fast onboarding into first build mission
- Mission-card gameplay loop with stars rewards
- Block grid builder with palette, erase mode, and 20-step undo
- Adaptive hint support based on struggle signals
- Atomic local save with backup fallback
- Parent gate + parent settings + local data clear
- No chat/no external sharing UX in child mode
- JVM unit tests for core domain logic

## Stack

- Kotlin
- Jetpack Compose
- AndroidX DataStore
- Kotlinx Serialization
- JUnit4

## Build & Test

```bash
./gradlew testDebugUnitTest assembleDebug
```
