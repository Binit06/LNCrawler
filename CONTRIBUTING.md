# Contributing to LNCrawler

Welcome! We're excited that you're interested in contributing to **LNCrawler**. This project aims to bring the powerful scraping logic of `lightnovel-crawler` to a modern, Tachiyomi-inspired Android experience.

---

## 🏗 Architecture Overview

The project follows a **Clean Architecture** pattern to ensure scalability and ease of contribution:

### 1. Data Layer (`com.halovoid.lncrawler.data`)
- **Crawler:** The scraping engine. All site-specific scrapers inherit from the `Crawler` base class.
- **DB:** Room database for local persistence of novel metadata and chapter lists.
- **Export:** Logic for packaging scraped data into formats like EPUB.
- **Repository:** The single source of truth that coordinates data between the network and the local database.

### 2. Domain Layer (`com.halovoid.lncrawler.domain`)
- **Models:** Plain data classes representing the core business objects (`Novel`, `Chapter`).
- **UseCases:** Simple classes that execute a single task, containing the business logic.

### 3. UI Layer (`com.halovoid.lncrawler.ui`)
- **Screens:** Jetpack Compose-based UI screens.
- **ViewModels:** Manage screen state and interact with the Domain layer via UseCases.
- **Theme:** Centralized colors and typography (Tachiyomi-inspired dark theme).

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
