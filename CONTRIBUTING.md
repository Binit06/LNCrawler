# Contributing to LNCrawler

Welcome! We're excited that you're interested in contributing to **LNCrawler**. This project aims to bring the powerful scraping logic of `lightnovel-crawler` to a modern, Tachiyomi-inspired Android experience.

---

## 🏗 Architecture Overview

The project follows a **Clean Architecture** pattern with a specialized **Job Scheduler System** to handle long-running crawling tasks:

### 1. Data Layer (`com.halovoid.lncrawler.data`)
This is where the bulk of the logic resides, organized into several sub-systems:
- **Crawler:** The scraping engine core. Sources implement the `Crawler` interface using `Scrapper` (OkHttp + Jsoup).
- **Scheduler:** A persistent job management system.
    - `RequestEntity`: Represents a unit of work (e.g., fetch novel, download chapter).
    - `JobRunner` & `SchedulerService`: Execute requests from the queue.
    - `JobHandler`: Specific handlers (`NovelHandler`, `ChapterHandler`, `ArtifactHandler`) that process different `RequestType`s.
- **Repository:** Manages data access for Novels, Chapters, Volumes, Preferences (DataStore), and Storage (File System).
- **DB:** Room database for storing metadata, chapter lists, and the request queue.
- **Artifact:** Logic for generating export files (e.g., EPUB) via `ArtifactGenerator`.

### 2. Domain Layer (`com.halovoid.lncrawler.domain`)
- **Models:** POJOs representing `Novel`, `Chapter`, `Volume`, and `Request`.
- **Note:** Business logic is primarily orchestrated via `JobHandlers` in the Data layer for this specific implementation.

### 3. UI Layer (`com.halovoid.lncrawler.ui`)
- **Screens:** Modular Jetpack Compose screens (Library, Request, Novel Detail, Settings).
- **ViewModels:** Maintain UI state and trigger jobs by inserting requests into the database.
- **Navigation:** Type-safe navigation using `NavGraph` and `Screen` sealed classes.

---

## How to Add a New Crawler

Adding support for a new light novel site is easy:

1.  **Inherit from `Crawler`:** Create a new class in `data.crawler.sources`.
    ```kotlin
    class MyNewSourceCrawler : Crawler() {
        override val name = "MyNewSource"
        override val baseUrl = "https://mynewsource.com"
        
        override fun canHandle(url: String): Boolean = url.contains("mynewsource.com")
        
        override suspend fun getNovelDetails(novelUrl: String): Novel {
            // Use getDocument(novelUrl) to get a Jsoup Document
            // Extract metadata and chapter links
        }
        
        override suspend fun getChapterContent(chapterUrl: String): String {
            // Use cleanHtml(doc, selector) to extract the chapter text
        }
    }
    ```
2.  **Register in `CrawlerFactory`:** Add your new crawler to the `crawlers` list in `CrawlerFactory.kt`.

---

## 🛠 Adding a New Job Handler

If you need to introduce a new type of background task (e.g., a new export format or a different scraping strategy):

1.  **Define `RequestType`:** Add a new entry to the `RequestType` enum in `data.db.entities`.
2.  **Implement `JobHandler`:** Create a new handler in `data.handlers`.
3.  **Register in `JobHandlerRegistry`:** Add your handler to the registry (typically done in the `SchedulerService` or a DI module).

---

## 🎨 UI Contributions

If you want to improve the UI:
- We aim for a look and feel similar to **Tachiyomi**. Use the established `LNCrawlerTheme` and reusable components in `ui.components`.
- All screens should be state-driven using `StateFlow` from ViewModels.

---

## 📦 Submission Process

1.  **Branch:** Create a feature branch for your changes.
2.  **KDoc:** Ensure all new classes and public methods are documented with KDoc.
3.  **Build:** Verify that `./gradlew assembleDebug` passes without errors.
4.  **Test:** Manually verify your scraper or feature on a real device/emulator.

We look forward to your contributions!
