# Novel Similarity Checking via Simhash

Implement a similarity check when adding novels to the library to prevent clutter and duplicate entries from different sources.

## User Review Required

> [!IMPORTANT]
> The similarity check is **action-triggered**. It only appears after the user clicks "Add to Library".
> We will use a **Hamming Distance threshold of 3** (out of 64 bits) to identify "similar" novels. This is generally effective for catching slight title variations.

## Proposed Changes

### Database & Data Layer

#### [MODIFY] [NovelEntity.kt](file:///home/zenit/Projects/LNCrawler/app/src/main/java/com/halovoid/lncrawler/data/db/entities/NovelEntity.kt)
- Add `val titleHash: Long?` to the entity.

#### [MODIFY] [Novel.kt](file:///home/zenit/Projects/LNCrawler/api/src/main/kotlin/com/halovoid/lncrawler/domain/models/Novel.kt)
- Add `val titleHash: Long? = null` to the data class.

#### [MODIFY] [ModelMappers.kt](file:///home/zenit/Projects/LNCrawler/app/src/main/java/com/halovoid/lncrawler/data/db/mappers/ModelMappers.kt)
- Update `toDomain` and `toEntity` to include `titleHash`.

---

### Similarity Engine

#### [NEW] [SimhashUtils.kt](file:///home/zenit/Projects/LNCrawler/app/src/main/java/com/halovoid/lncrawler/utils/SimhashUtils.kt)
- Create a utility class for 64-bit Simhash generation.
- Implementation will include string normalization and n-gram based hashing.

#### [MODIFY] [NovelRepository.kt](file:///home/zenit/Projects/LNCrawler/app/src/main/java/com/halovoid/lncrawler/data/repository/NovelRepository.kt)
- Add `getSimilarNovels(hash: Long, threshold: Int): List<Novel>`.
- This will fetch all novels and filter them in Kotlin (Hamming distance is faster in memory for typical library sizes).

---

### Logic & UI

#### [MODIFY] [RequestViewModel.kt](file:///home/zenit/Projects/LNCrawler/app/src/main/java/com/halovoid/lncrawler/ui/screens/request/RequestViewModel.kt)
- Add `_similarNovels` StateFlow.
- Update `addNovelDirectly` to perform the check.
- Add `clearSimilarNovels()` and `saveNovel(novel: Novel)` to finalize the operation.

#### [MODIFY] [NovelPreviewScreen.kt](file:///home/zenit/Projects/LNCrawler/app/src/main/java/com/halovoid/lncrawler/ui/screens/request/NovelPreviewScreen.kt)
- Integrate `ModalBottomSheet` to show similar novels.
- Display similar novels as a list of horizontal cards.

## Verification Plan

### Manual Verification
- Add a novel with an exact title.
- Add a novel with a slightly different title (e.g., adding "The Great Novel" when "Great Novel" exists).
- Verify the bottom sheet appears and "Proceed Anyway" works.
- Verify "Cancel" dismisses the sheet and stays on the preview.
