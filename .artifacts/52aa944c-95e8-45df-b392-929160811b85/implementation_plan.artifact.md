# Redesign LNCrawler Onboarding UI

Redesign the onboarding screens to be cleaner, more modern, and less "purple-heavy". The goal is to move away from a developer/debug look towards a polished consumer experience.

## Proposed Changes

### [Onboarding Framework]

#### [MODIFY] [OnboardingStep.kt](file:///home/zenit/Projects/LNCrawler/app/src/main/java/com/halovoid/lncrawler/ui/screens/onboarding/OnboardingStep.kt)
- Remove the forced `Surface` card wrapper around the content. This will eliminate one level of nesting across all onboarding screens and allow for more flexible layouts.
- Adjust vertical spacing and alignment to feel more composed.
- Standardize the bottom CTA button to be more premium.

### [Storage Location Screen]

#### [MODIFY] [FolderScreen.kt](file:///home/zenit/Projects/LNCrawler/app/src/main/java/com/halovoid/lncrawler/ui/screens/onboarding/FolderScreen.kt)
- Simplify the layout by removing nested cards.
- Reduce the prominence of the folder icon (smaller size, less intense purple background).
- Make the selected path secondary information using muted colors.
- Use `BrandAccent` primarily for the "Change Directory" action and the main "Proceed" button.
- Improve vertical composition to avoid a "bottom-heavy" or "empty" feeling.

### [Permissions Screen]

#### [MODIFY] [PermissionScreen.kt](file:///home/zenit/Projects/LNCrawler/app/src/main/java/com/halovoid/lncrawler/ui/screens/onboarding/PermissionScreen.kt)
- Redesign `PermissionItem` from a large card to a clean checklist row.
- Use subtle success indicators (small checkmarks) instead of large green highlights.
- Group items in a clean list without individual card borders.
- Clarify the "What it is → Why it matters → Action" hierarchy.

### [Crawler Synchronization Screen]

#### [MODIFY] [SourceSyncScreen.kt](file:///home/zenit/Projects/LNCrawler/app/src/main/java/com/halovoid/lncrawler/ui/screens/onboarding/SourceSyncScreen.kt)
- Replace the developer-style log terminal with a clean, user-friendly progress UI.
- Implement a "Show Details" toggle to reveal technical logs for power users/debugging.
- Use human-readable status messages (e.g., "12 of 15 sources ready").
- Improve the error state to be more approachable and provide a "Retry" option alongside "Details".

## Verification Plan

### Manual Verification
- Deploy the app and walk through the entire onboarding flow.
- Verify that the "Storage Location" screen feels clean and the path is readable but secondary.
- Verify that "Permissions" feel like a lightweight checklist.
- Verify that "Source Sync" shows a clean progress indicator and hides logs by default.
- Check "Show Details" functionality in the sync screen.
- Ensure purple is used only as an accent throughout.
