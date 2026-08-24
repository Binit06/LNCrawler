# Implementation Plan - Crawler-wise Concurrency

Implement per-crawler concurrency limits in the `JobScheduler` to allow different sources to have different numbers of concurrent jobs.

## Proposed Changes

### [Component: Scheduler]

#### [MODIFY] [JobScheduler.kt](file:///home/zenit/Projects/LNCrawler/app/src/main/java/com/halovoid/lncrawler/data/scheduler/jobs/JobScheduler.kt)

- Replace the single `workerPool` with a `ConcurrentHashMap<String, WorkerPool>` to track concurrency per crawler.
- Add a "GLOBAL" pool key for jobs that don't have a crawler associated (like some system tasks).
- Update `launchReadyJobs` to:
    - Extract the `crawlerName` from the request metadata using `parsedMetadata`.
    - Retrieve or create a `WorkerPool` for that specific crawler.
    - If a crawler is found, use its `config.runnerConcurrency` as the limit.
    - Acquire a permit from the specific crawler's pool before launching the job.
    - Release the permit back to the same pool when the job completes.

## Verification Plan

### Automated Tests
- Since I cannot easily run unit tests that require complex scheduler setup in this environment, I will verify via deployment and logging.

### Manual Verification
- Deploy the app.
- Start multiple downloads from different sources.
- Verify that the number of concurrent jobs for each source respects its own limit (if I had multiple sources to test with).
- Check logs to ensure `JobScheduler` is correctly identifying the crawler and using the right pool.
