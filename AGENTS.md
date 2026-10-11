# AGENTS.md — Stash Android TV Application

## 1. Purpose

This document defines the global engineering instructions for AI coding agents working on the **Stash Android TV Application (StashTvApp)**.

All AI agents MUST follow these instructions when analyzing, implementing, refactoring, debugging, or testing code.

### Primary Objectives

1. Maintain a stable, responsive Android TV and Amazon Fire TV experience.
2. Preserve existing working functionality.
3. Follow the established architecture and coding conventions.
4. Prioritize D-Pad navigation, focus behavior, and TV usability.
5. Ensure compatibility with the Stash GraphQL API.
6. Produce maintainable, testable Kotlin code.
7. Avoid unnecessary dependencies, abstractions, and rewrites.

**Core principle: Make the smallest correct change that fully solves the requested problem.**

---

## 2. Project Information

| Property | Value |
|---|---|
| Application | Stash TV App |
| Package | `com.kb2.stashtvapp` |
| Platforms | Android TV / Amazon Fire TV |
| Minimum SDK | 28 |
| Target SDK | 35 |
| Java Target | 17 |
| Gradle | 8.10.2 |
| UI Framework | Jetpack Compose for TV |
| Dependency Injection | Hilt |
| GraphQL Client | Apollo Kotlin 3 |
| Media Playback | AndroidX Media3 ExoPlayer |
| Image Loading | Coil |
| Backend | Stash GraphQL Server |

### Backend Configuration

Development endpoint:

`http://192.168.1.19:9999/graphql`

This address is environment-specific.

Agents MUST NOT:

- Hardcode additional backend addresses.
- Change the existing endpoint without authorization.
- Expose API keys, authentication tokens, or credentials in logs.
- Introduce unnecessary network configuration changes.

Prefer the project's existing configuration mechanism.

---

## 3. Agent Operating Rules

### 3.1 Before Writing Code

Every agent MUST:

1. Understand the user's requested change.
2. Inspect the relevant existing files.
3. Identify the affected UI, ViewModel, repository, and data-source components.
4. Search for reusable implementations already present.
5. Identify possible regressions.
6. Check related GraphQL operations and generated Apollo types when applicable.
7. Develop a minimal implementation plan.

Do not assume a method, class, GraphQL field, or dependency exists without checking.

### 3.2 During Implementation

- Follow existing naming and package conventions.
- Prefer modifying existing components over creating duplicates.
- Keep composables focused on presentation.
- Keep business logic out of composables.
- Use ViewModels for screen state and user actions.
- Use repositories for data operations.
- Use data sources for API communication.
- Keep changes scoped to the requested feature.
- Preserve existing public interfaces unless changes are necessary.
- Avoid unrelated formatting or refactoring.
- Do not silently remove existing features.

### 3.3 After Implementation

Agents MUST:

1. Review the complete code diff.
2. Check imports and unresolved references.
3. Run relevant compilation or tests when tools are available.
4. Validate affected GraphQL operations.
5. Check for focus-navigation regressions.
6. Report what changed and what was verified.

Never claim that a build or test passed unless it was actually executed.

---

## 4. Application Architecture

Follow the existing layered architecture.

```text
Compose TV UI
      |
      v
ViewModel
      |
      v
Repository Interface
      |
      v
Repository Implementation
      |
      v
StashGraphQLDataSource
      |
      v
Apollo GraphQL / HTTP
      |
      v
Stash Server
```

### UI Layer

Responsibilities:

- Render UI state.
- Handle user interaction.
- Manage focus and TV navigation.
- Trigger ViewModel actions.
- Display loading, empty, and error states.

Do not perform GraphQL requests directly inside composables.

### ViewModel Layer

Use:

- `@HiltViewModel`
- `StateFlow`
- Kotlin coroutines
- `combine` when multiple streams need aggregation
- `collectAsStateWithLifecycle()` from the UI

ViewModels SHOULD expose a consistent screen state and clearly named actions.

Prefer immutable UI state.

### Repository Layer

Repositories coordinate application data operations.

- Keep API implementation details hidden from UI code.
- Reuse existing repository interfaces.
- Handle mapping between API responses and application models.
- Preserve established error-handling patterns.

### Data Source Layer

`StashGraphQLDataSource` is responsible for communicating with the Stash API.

Use Apollo Kotlin 3 for supported GraphQL operations.

The existing raw GraphQL HTTP fallback may be used for operations that cannot reasonably be handled through the current Apollo setup.

Do not introduce raw HTTP implementations merely to avoid updating a GraphQL operation.

---

## 5. Stash GraphQL Rules

### 5.1 Pagination

Stash's `findScenes` operation does not accept `per_page: -1` in the current project configuration.

For scene requests, use:

```kotlin
FindFilterType(
    per_page = Optional.present(200)
)
```

This avoids the default 25-item result limit.

**Important:** A page size of 200 is not equivalent to fetching every matching scene.

When complete results are required:

- Fetch subsequent pages.
- Stop when the returned result count or pagination metadata indicates completion.
- Avoid unbounded concurrent requests.
- Prevent duplicate results.
- Respect cancellation.

For UI carousels, fetch only as many results as the screen needs when possible.

Never apply `per_page: -1` to `findScenes`.

### 5.2 Saved Filters

Stash saved filters may contain:

- `find_filter`
- `object_filter`
- Filter mode
- Sorting and pagination settings

Agents MUST preserve both the generic search criteria and scene-specific filter criteria when implementing saved-filter functionality.

Do not assume that `find_filter` alone represents the complete saved filter.

### 5.3 Object Filter Conversion

The project contains:

`convertObjectFilterToSceneFilter()`

This function maps saved-filter JSON into `SceneFilterType`.

Preserve support for:

**Multi-value and hierarchical criteria**

- Performers
- Tags
- Studios
- Groups
- Galleries

Expected structures may include:

```json
{
  "items": [
    {"id": "123", "label": "Example"}
  ],
  "excluded": []
}
```

Convert to the appropriate GraphQL filter representation.

**EXCLUDES handling**

The current compatibility logic normalizes certain `EXCLUDES` structures into supported include/exclude fields.

Do not change this conversion without checking the actual Stash schema and testing include/exclude behavior.

**Boolean and string criteria**

Normalize wrapped values such as:

```json
{"value": true}
```

and:

```json
{"value": "true"}
```

into the scalar representation expected by the specific GraphQL field.

Do not convert every string boolean indiscriminately; follow the target field's schema type.

### 5.4 GraphQL Schema Compatibility

Before modifying GraphQL operations:

1. Inspect existing `.graphql` definitions.
2. Inspect relevant Apollo-generated types.
3. Verify field names and input types against the project's configured Stash schema.
4. Update operations when necessary.
5. Regenerate Apollo sources.
6. Compile affected Kotlin code.

Never invent GraphQL fields or enum values.

### 5.5 Scene Tag Updates

Stash `sceneUpdate` with `tag_ids` replaces the scene's complete tag association.

When adding or removing tags:

1. Read the existing scene tag IDs.
2. Preserve unrelated tags.
3. Apply only the requested additions or removals.
4. Remove duplicate IDs.
5. Submit the complete updated list.

Never replace all scene tags with only the newly selected tags.

Consider stale-data and concurrent-update risks when reading and updating tags.

### 5.6 GraphQL Error Handling

Agents MUST handle:

- Network failures
- Authentication failures
- GraphQL errors
- Empty results
- Invalid responses
- Server unavailability
- Request cancellation

Do not swallow exceptions silently.

Do not log sensitive response payloads or credentials.

---

## 6. Android TV UI Standards

The application is TV-first, not mobile-first.

All UI changes must be designed for remote-control interaction.

### 6.1 Landscape Layout

The target display is widescreen 16:9.

Scene thumbnails and video preview cards SHOULD use:

```kotlin
Modifier.aspectRatio(16f / 9f)
```

with:

```kotlin
ContentScale.Crop
```

Do not stretch images.

Use portrait aspect ratios only when the content or existing screen design explicitly requires them.

Maintain appropriate spacing and safe margins for TV displays.

### 6.2 D-Pad Navigation

Every interactive element MUST support appropriate remote navigation.

Required inputs:

- UP
- DOWN
- LEFT
- RIGHT
- CENTER / ENTER
- BACK

Focus navigation must be predictable.

Agents MUST verify:

- Focus moves between adjacent items.
- Focus remains visible.
- Focus does not become trapped.
- Focus returns to a meaningful element after closing dialogs.
- Focus is restored when returning from detail screens where appropriate.
- Off-screen focused elements scroll into view.
- Back navigation follows the existing navigation structure.

### 6.3 Focus Styling

Default focused-card scale:

```kotlin
focusedScale = 1.05f
```

Default focus border:

```kotlin
BorderStroke(
    2.dp,
    MaterialTheme.colorScheme.onSurface
)
```

Use the existing TV Material 3 focus APIs.

These are design defaults, not universal requirements for every control.

Avoid applying focus scale to elements where scaling causes clipping, overlapping, or layout instability.

### 6.4 Initial Focus

When entering a screen, initial focus should move to the first meaningful interactive element.

Use the existing `focusOnInitialVisibility()` behavior where appropriate.

Do not request focus repeatedly during recomposition.

Avoid focus loops caused by state updates.

### 6.5 Focus Preservation

When updating lists:

- Use stable item keys.
- Preserve focus where possible.
- Avoid recreating the entire list unnecessarily.
- Restore focus to a nearby valid item if the previously focused item disappears.

This is especially important for:

- Home sections
- Scene grids
- Search results
- Saved filters
- Favorites
- Watch Later

### 6.6 Text Input

Never place `BasicTextField` inside a clickable parent `Surface(onClick = ...)` when the parent may intercept keyboard confirmation.

Use a non-clickable container:

```kotlin
Box(
    modifier = Modifier
        .background(backgroundColor)
        .border(borderStroke)
) {
    BasicTextField(...)
}
```

Preserve correct behavior for:

- D-Pad CENTER
- ENTER
- Soft keyboard confirmation
- Back navigation
- Focus transitions

---

## 7. Video Playback Standards

The project uses AndroidX Media3 ExoPlayer.

### 7.1 Playback Components

Existing implementations include:

- `rememberPlayer`
- `PlayerSurface`
- `VideoPreviewPlayer`
- `VideoPlayerScreen`

Reuse existing player abstractions before creating new ones.

### 7.2 Video Previews

Background previews begin after a focus delay of:

```kotlin
delay(3_000L)
```

Requirements:

- Cancel the pending preview when focus moves away.
- Avoid starting multiple competing previews.
- Release or reuse player resources appropriately.
- Prevent unnecessary playback during rapid D-Pad navigation.
- Respect lifecycle changes.

### 7.3 Player Controls

Player controls automatically hide after three seconds of inactivity:

```kotlin
rememberVideoPlayerState(
    hideSeconds = 3
)
```

Requirements:

- Remote interaction should reveal controls.
- Focused controls must remain usable.
- Seeking must work with the remote.
- Playback state must remain synchronized with UI state.
- Player resources must be released appropriately.

Do not hide controls while the user is actively interacting with them.

### 7.4 Playback Error Handling

Handle:

- Invalid media URLs
- Network interruptions
- Unsupported formats
- Player initialization errors
- Media source failures

Show meaningful errors without crashing the application.

---

## 8. Image Loading and Caching

Use Coil Compose for image loading.

Preferred components:

- `AsyncImage`
- `ImageRequest.Builder`

Requirements:

- Use the existing image-loading configuration.
- Preserve image aspect ratios.
- Use appropriate loading placeholders when available.
- Handle failed image requests.
- Avoid unnecessary reloads during recomposition.
- Avoid decoding unnecessarily large images for small cards.

Do not introduce another image-loading library without a clear requirement.

---

## 9. Performance Requirements

Android TV and Fire TV devices may have limited CPU and memory resources.

Performance is a priority.

### Compose Performance

- Avoid unnecessary recompositions.
- Use stable keys in lazy lists.
- Use `remember` for appropriate local calculations.
- Use `derivedStateOf` when it provides a measurable or clear state-management benefit.
- Avoid expensive calculations inside composables.
- Avoid launching network operations from ordinary recomposition.
- Keep large lists lazy.

### Networking

- Avoid duplicate GraphQL requests.
- Reuse existing data-loading mechanisms.
- Support cancellation for obsolete requests.
- Avoid fetching full datasets when a page is sufficient.
- Avoid blocking the main thread.

### Video and Images

- Avoid multiple unnecessary ExoPlayer instances.
- Release media resources when no longer required.
- Avoid excessive image memory consumption.
- Limit simultaneous previews.

Do not optimize speculatively at the cost of code clarity. Prioritize demonstrated bottlenecks and obvious resource waste.

---

## 10. State Management

Use unidirectional data flow.

```text
User Action
    |
    v
ViewModel
    |
    v
Repository
    |
    v
Updated UI State
    |
    v
Compose Rendering
```

### Requirements

- Prefer immutable UI state.
- Expose observable state using `StateFlow`.
- Keep persistent screen state in the ViewModel.
- Keep transient presentation state local when appropriate.
- Avoid duplicating the same state in multiple layers.
- Prevent outdated asynchronous responses from overwriting newer results.

### Optimistic Updates

Optimistic updates are allowed when appropriate.

Required behavior:

1. Update the visible UI immediately.
2. Perform the backend operation.
3. Confirm success.
4. Roll back or reconcile the UI if the operation fails.
5. Show a meaningful error when necessary.

Never leave the UI permanently inconsistent with the server after a failed mutation.

---

## 11. Dependency Management

Before adding a dependency:

1. Check whether an equivalent library already exists.
2. Verify compatibility with the current SDK and Kotlin configuration.
3. Prefer existing AndroidX, Compose, Hilt, Apollo, Coil, and Media3 dependencies.
4. Explain why the new dependency is necessary.

Do not upgrade unrelated dependencies during feature development.

Do not modify Gradle or version catalog files without a clear requirement.

---

## 12. Testing and Validation

Agents MUST choose validation appropriate to the change.

### Kotlin Changes

Compile the affected module.

### GraphQL Changes

- Generate Apollo sources.
- Compile Kotlin.
- Validate generated input/output types.
- Test relevant filter transformations where possible.

### UI Changes

Check:

- Initial focus
- D-Pad navigation
- Back behavior
- Focus scaling
- Scrolling
- Loading state
- Empty state
- Error state

### Playback Changes

Check:

- Play/pause
- Seek
- Control visibility
- Back navigation
- Resource cleanup
- Playback failures

### Regression Testing

Any modification affecting shared components must consider all screens that reuse them.

Do not assume that successful compilation proves runtime behavior is correct.

---

## 13. Build Commands

Use the available Gradle execution environment.

### Compile Kotlin

```text
gradle_build("stashtvapp:compileDebugKotlin")
```

### Assemble Debug APK

```text
gradle_build("stashtvapp:assembleDebug")
```

### Generate Apollo Sources

```text
gradle_build("stashtvapp:generateApolloSources")
```

### Clean Project

```text
gradle_build("clean")
```

These commands describe the project's existing agent-tool convention.

If the `gradle_build` tool is unavailable, use the project's Gradle wrapper with equivalent tasks.

On Windows:

```powershell
.\gradlew.bat :stashtvapp:compileDebugKotlin
.\gradlew.bat :stashtvapp:assembleDebug
.\gradlew.bat :stashtvapp:generateApolloSources
```

Avoid running `clean` unnecessarily because it removes build outputs and increases subsequent build time.

---

## 14. Debugging Rules

When fixing bugs, agents MUST:

1. Reproduce or understand the reported failure.
2. Identify the likely root cause.
3. Inspect the relevant call chain.
4. Apply a focused fix.
5. Check for related regressions.
6. Validate the fix where possible.

Do not repeatedly change unrelated code based on guesses.

If the root cause is uncertain, explain the uncertainty and identify the next useful diagnostic step.

### Build Failures

Do not resolve compilation errors by:

- Deleting working features.
- Commenting out required code.
- Introducing fake implementations.
- Suppressing meaningful errors without justification.
- Changing unrelated dependency versions.

Fix the underlying problem.

---

## 15. Code Quality Rules

### Kotlin

- Follow existing Kotlin naming conventions.
- Prefer readable code over clever abstractions.
- Use null safety.
- Avoid `!!` unless its safety is demonstrably guaranteed.
- Avoid blocking operations on the main thread.
- Respect coroutine cancellation.
- Avoid `GlobalScope`.
- Use structured concurrency.

### Jetpack Compose

- Keep composables reasonably small.
- Hoist state when appropriate.
- Prefer reusable components for repeated UI patterns.
- Avoid unnecessary mutable global state.
- Use lifecycle-aware state collection.
- Keep navigation logic consistent with the existing application.

### Comments

Write comments to explain non-obvious behavior and important constraints.

Avoid comments that simply repeat the code.

---

## 16. Scope Control

Agents MUST NOT make unrelated changes.

For example:

If asked to fix a HomeScreen banner:

- Modify the banner implementation and directly related code.
- Do not redesign the navigation system.
- Do not rewrite unrelated repositories.
- Do not upgrade the Compose BOM without justification.
- Do not change player behavior unless required.

If a broader architectural issue is discovered, report it separately unless it prevents the requested task.

---

## 17. Feature Completion Criteria

A task is complete only when the implementation satisfies the applicable criteria:

- [ ] Requested behavior is implemented.
- [ ] Existing functionality is preserved.
- [ ] Architecture conventions are followed.
- [ ] D-Pad navigation is considered and validated where possible.
- [ ] Focus behavior is correct for affected UI.
- [ ] GraphQL operations are valid where changed.
- [ ] Loading, empty, and error states are handled where relevant.
- [ ] No unnecessary dependencies were introduced.
- [ ] Relevant build or test checks were performed, or limitations reported.
- [ ] Changes were reviewed for regressions.

---

## 18. Agent Response Format

After completing a coding task, respond using this structure:

### Summary
Briefly explain what was implemented or fixed.

### Files Changed
List the modified files and their purpose.

### Validation
Report actual build, test, or runtime validation results.

If validation was not possible, state why.

### Notes
Mention important limitations, remaining issues, or relevant follow-up work.

Keep responses concise and technical.

Do not claim success for operations that were not performed.

---

## 19. Final Engineering Principles

1. **Preserve working code.**
2. **TV navigation comes first.**
3. **Respect the Stash GraphQL schema.**
4. **Keep UI, business logic, and networking separated.**
5. **Avoid unnecessary changes.**
6. **Prefer existing implementations.**
7. **Validate before claiming completion.**
8. **Keep performance appropriate for Fire TV hardware.**
9. **Never hide failures.**
10. **Favor maintainability over complexity.**

**The goal is a stable, responsive, maintainable Stash Android TV application—not merely code that compiles.**