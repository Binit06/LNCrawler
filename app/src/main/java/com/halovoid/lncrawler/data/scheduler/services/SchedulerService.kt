package com.halovoid.lncrawler.data.scheduler.services

import android.R
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.halovoid.lncrawler.MainActivity
import com.halovoid.lncrawler.data.artifact.ArtifactGenerator
import com.halovoid.lncrawler.data.artifact.ArtifactGeneratorFactory
import com.halovoid.lncrawler.data.artifact.generators.EpubGenerator
import com.halovoid.lncrawler.api.core.crawler.CrawlerFactory
import com.halovoid.lncrawler.api.core.scrapper.Scrapper
import com.halovoid.lncrawler.data.db.AppDatabase
import com.halovoid.lncrawler.data.db.dao.RequestDao
import com.halovoid.lncrawler.data.db.entities.RequestEntity
import com.halovoid.lncrawler.data.db.entities.RequestStatus
import com.halovoid.lncrawler.data.db.entities.RequestType
import com.halovoid.lncrawler.data.handlers.ArtifactHandler
import com.halovoid.lncrawler.data.handlers.ChapterHandler
import com.halovoid.lncrawler.data.handlers.NovelHandler
import com.halovoid.lncrawler.data.handlers.NovelMetadataHandler
import com.halovoid.lncrawler.data.handlers.VolumeHandler
import com.halovoid.lncrawler.data.repository.*
import com.halovoid.lncrawler.data.scheduler.jobs.JobHandlerRegistry
import com.halovoid.lncrawler.data.scheduler.jobs.JobScheduler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private data class NotificationConfig(
    val title: String,
    val content: String,
    val icon: Int,
    val progressCurrent: Int,
    val progressTotal: Int,
    val isIndeterminate: Boolean
)

private fun RequestEntity.toNotificationConfig(): NotificationConfig {
    val (title, icon) = when (this.type) {
        RequestType.FULL_NOVEL -> "Crawling Novel" to R.drawable.stat_sys_download
        RequestType.ARTIFACT -> "Creating Artifact" to R.drawable.stat_sys_upload
        RequestType.NOVEL_METADATA -> "Refreshing Novel" to R.drawable.stat_notify_sync
        else -> "LN Crawler Task" to R.drawable.stat_sys_download
    }

    return NotificationConfig(
        title = title,
        content = this.name,
        icon = icon,
        progressCurrent = this.progressSuccess,
        progressTotal = this.progressTotal,
        isIndeterminate = this.progressTotal <= 0
    )
}
class SchedulerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var scheduler: JobScheduler
    private lateinit var requestDao: RequestDao

    companion object {
        private const val CHANNEL_ID = "scheduler_channel"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_START = "ACTION_START"
        private const val ACTION_STOP = "ACTION_STOP"

        private const val ACTION_CANCEL_JOB = "ACTION_CANCEL_JOB"

        private const val EXTRA_REQUEST_ID = "EXTRA_REQUEST_ID"

        fun startService(context: Context) {
            val intent = Intent(context, SchedulerService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, SchedulerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun cancelJob(context: Context, requestId: String) {
            val intent = Intent(context, SchedulerService::class.java).apply {
                action = ACTION_CANCEL_JOB
                putExtra(EXTRA_REQUEST_ID, requestId)
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(this)
        requestDao = db.requestDao()
        val chapterDao = db.chapterDao()

        // 1. Initializing Repositories
        val novelRepository = NovelRepository(this)
        val chapterRepository = ChapterRepository(this)
        val volumeRepository = VolumeRepository(this)
        val preferenceRepository = PreferenceRepository(this)
        val storageRepository = StorageRepositoryImpl(this, preferenceRepository)
        val artifactRepository = ArtifactRepository(this)

        // 2. Initialize Artifact System
        val epubGenerator = EpubGenerator(storageRepository)
        val generators = listOf<ArtifactGenerator>(epubGenerator)
        val generatorFactory = ArtifactGeneratorFactory(generators)

        // 3. Initialize Handler and Registry
        val registry = JobHandlerRegistry()
        val crawlerFactory = CrawlerFactory
        val scrapper = Scrapper()

        // 4. Register Handlers
        registry.register(RequestType.FULL_NOVEL, NovelHandler(
            crawlerFactory, novelRepository, chapterRepository, volumeRepository, storageRepository, requestDao
        ))
        registry.register(RequestType.VOLUME, VolumeHandler(
            chapterDao, requestDao
        ))
        registry.register(RequestType.CHAPTER, ChapterHandler(
            requestDao, scrapper, chapterRepository, storageRepository, crawlerFactory
        ))
        registry.register(RequestType.NOVEL_METADATA, NovelMetadataHandler(
            crawlerFactory, novelRepository, volumeRepository, chapterRepository, storageRepository, requestDao
        ))
        registry.register(RequestType.ARTIFACT, ArtifactHandler(
            novelRepository, chapterRepository, volumeRepository,
            crawlerFactory, storageRepository, generatorFactory, artifactRepository,
            requestDao
        ))

        // 5. Set Up Scheduler
        scheduler = JobScheduler(requestDao, registry)
        scheduler.setOnEmptyListener {
            stopSelf()
        }

        createNotificationChannel()
        observeProgress()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val initialConfig = NotificationConfig("Initializing...", "Starting scheduler", R.drawable.stat_sys_download, 0, 0, true)
                startForeground(NOTIFICATION_ID, createNotification(initialConfig, 0))
                scheduler.start()
            }
            ACTION_STOP -> {
                scheduler.stop()
                stopSelf()
            }
            ACTION_CANCEL_JOB -> {
                val requestId = intent.getStringExtra(EXTRA_REQUEST_ID)
                if (requestId != null) {
                    scheduler.cancelActiveJob(requestId)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scheduler.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Job Scheduler",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of background crawl and download tasks"
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(config: NotificationConfig, othersCount: Int): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contextText = if (othersCount > 0) {
            "${config.content} (+ $othersCount others)"
        } else {
            config.content
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(config.title)
            .setContentText(contextText)
            .setSmallIcon(config.icon)
            .setContentIntent(pendingIntent)
            .setProgress(config.progressTotal, config.progressCurrent, config.isIndeterminate)
            .setOngoing(true)
            .build()
    }

    private fun observeProgress() {
        requestDao.getRootRequests()
            .onEach { requests ->
                val activeRequests = requests.filter { it.progressSuccess + it.progressFailed + it.progressCancelled < it.progressTotal }
                    .sortedWith (
                        compareByDescending<RequestEntity> {
                            it.status == RequestStatus.RUNNING
                        }.thenByDescending { it.updatedAt }
                    )
                if (activeRequests.isEmpty()) return@onEach

                val primaryRequest = activeRequests.first()
                val config = primaryRequest.toNotificationConfig()
                val othersCount = activeRequests.size - 1
                
                val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, createNotification(config, othersCount))
            }
            .launchIn(serviceScope)
    }
}
