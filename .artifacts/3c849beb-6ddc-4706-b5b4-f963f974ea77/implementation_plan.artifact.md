# Onboarding Screen Design Improvement

The goal is to update the Onboarding screens to align with the modern dark visual style used throughout the rest of the LNCrawler app. This includes consistent use of `BrandAccent` (Electric Indigo), `DarkSurfaceVariant`, and refined typography.

## Proposed Changes

### [Onboarding Components]

#### [MODIFY] [OnboardingStep.kt](file:///home/zenit/Projects/LNCrawler/app/src/main/java/com/halovoid/lncrawler/ui/screens/onboarding/OnboardingStep.kt)
- Update the primary button to use `BrandAccent` (Electric Indigo) for better visual pop.
- Refine typography: Use `headlineMedium` for titles and ensure proper line height for body text.
- Improve spacing and layout hierarchy.
- Use `DarkSurfaceVariant` for the content area to provide better depth against `DarkBackground`.

#### [MODIFY] [FolderScreen.kt](file:///home/zenit/Projects/LNCrawler/app/src/main/java/com/halovoid/lncrawler/ui/screens/onboarding/FolderScreen.kt)
- Update the icon container to use a more modern glassmorphism-inspired look or consistent accent backgrounds.
- Align button styling with the new design system.

#### [MODIFY] [PermissionScreen.kt](file:///home/zenit/Projects/LNCrawler/app/src/main/java/com/halovoid/lncrawler/ui/screens/onboarding/PermissionScreen.kt)
- Redesign `PermissionItem` to use `DarkSurfaceVariant` with subtle borders.
- Use `BrandAccent` for the "Grant" action text.
- Improve icon and text alignment.

#### [MODIFY] [SourceSyncScreen.kt](file:///home/zenit/Projects/LNCrawler/app/src/main/java/com/halovoid/lncrawler/ui/screens/onboarding/SourceSyncScreen.kt)
- Update the "terminal" log box to use `DarkSurfaceVariant`.
- Style the sync indicator and success/error states using `BrandAccent` and semantic colors.

## Verification Plan

### Manual Verification
- Deploy the app and navigate through the onboarding flow to ensure visual consistency and correct behavior of all interactive elements.
- Verify that the colors and typography match the rest of the app (e.g., `NovelDetailScreen`).
