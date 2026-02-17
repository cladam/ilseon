---
name: kotlin-developer
description: Lead Android/Kotlin developer guidance for the ilseon executive function assistant app
version: 2.0.0
author: Claes Adamsson
tags:
  - kotlin
  - android
  - jetpack-compose
  - hilt
  - room
  - coroutines
---

# Kotlin Developer Skill

## Overview

Use this skill when acting as a Lead Android/Kotlin Developer on the ilseon codebase. This is an executive function assistant Android app targeting Kotlin 2.2+, Jetpack Compose with Material 3, Hilt for dependency injection, Room for persistence, and Coroutines/Flow for async operations. The goal is to ensure idiomatic Kotlin, accessible UI, and robust offline-first architecture.

## When to Use

- Implementing or reviewing Android features, Compose screens, or ViewModels.
- Designing coroutine-based flows, StateFlow/SharedFlow patterns, or data pipelines.
- Working with Room entities, DAOs, or database migrations.
- Building UI components following Material 3 guidelines with accessibility in mind.
- Guiding architectural or style decisions for this Android app.

## When Not to Use

- Backend/server-side Kotlin services (Ktor, Spring).
- Infrastructure-as-code, Terraform, or pure documentation tasks.
- iOS or cross-platform development.

## Instructions

### General Guidance

- Always read `app/build.gradle.kts` and `gradle/libs.versions.toml` before suggesting dependencies or versions.
- Use idiomatic Kotlin: data classes, sealed hierarchies, scoped functions, and expression-bodied members when clear.
- Default to immutable data (`val`) and null-safety features; avoid `!!` and unchecked casts.
- Prefer Coroutines/Flow over callbacks; use structured concurrency and cancellation-aware code.
- Follow MVVM architecture: ViewModels expose UI state via StateFlow, Compose screens observe state.
- Remember this app targets users with executive function challenges—UI must be minimal, calm, and low-friction.

### Preferred Practices

- Follow Jetpack Compose conventions; keep Composables focused and stateless where possible.
- Use `remember`, `rememberSaveable`, and proper state hoisting patterns.
- Leverage Material 3 theming with the existing dark-mode, low-saturation color scheme.
- Use extension functions and Kotlin DSL builders for readability.
- Keep functions/files concise; split screens into smaller Composable components when complexity grows.
- Use Hilt's `@HiltViewModel` and `@Inject` annotations consistently.

### Patterns to Follow

- Room entities should be data classes; use TypeConverters for complex types like enums and UUIDs.
- Expose database queries as `Flow<List<T>>` from DAOs for reactive UI updates.
- Model UI state with sealed classes or data classes; use `when` expressions for exhaustive state handling.
- Use `collectAsStateWithLifecycle()` to observe Flows in Compose.
- Handle errors through sealed `Result` types or dedicated error states in UI state classes.
- Use WorkManager for background scheduling; AlarmManager for time-critical reminders.
- Leverage Glance for app widget development with `GlanceAppWidget`.

### Patterns to Avoid

- Ignoring nullability warnings or using `lateinit` fields without lifecycle guarantees.
- Putting business logic directly in Composables—delegate to ViewModels.
- Exposing mutable state (`MutableStateFlow`) directly to the UI layer.
- Blocking calls on the main thread; always use appropriate dispatchers (`Dispatchers.IO` for database).
- Over-engineering—keep solutions simple and focused on user needs.
- Complex animations or flashy UI elements that could overwhelm users.

### Tooling & Dependency Checks

- Confirm Kotlin, Compose BOM, Hilt, and Room versions from `gradle/libs.versions.toml` before coding.
- Current stack: Kotlin 2.2.x, Compose BOM 2025.x, Hilt 2.57.x, Room 2.8.x.
- Use MockK for mocking in tests; Turbine for testing Flows; Compose UI testing for UI tests.
- Use Espresso for instrumented tests when needed.
- Maintain test coverage on critical business logic (ViewModels, repositories).

### Output Expectations

- Document public APIs with KDoc focusing on rationale (the "why").
- Keep UI text concise and clear—users should understand features instantly.
- Prioritise accessibility: content descriptions, sufficient contrast, touch targets ≥48dp.
- Follow Android best practices for permissions, battery optimisation, and background work.

## Notes

- This app is designed for users with ADD/ADHD and ASD—always consider cognitive load and sensory control.
- Core features: task capture, prioritisation, time-based reminders, context filtering, voice memos.
- If unsure about a dependency or pattern, prototype a small experiment first before full implementation.
