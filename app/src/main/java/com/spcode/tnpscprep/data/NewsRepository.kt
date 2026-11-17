package com.spcode.tnpscprep.data

import com.spcode.tnpscprep.data.db.AppDatabase
import com.spcode.tnpscprep.data.news.FeedConfig
import com.spcode.tnpscprep.data.news.FeedParser
import com.spcode.tnpscprep.data.news.FeedSource
import com.spcode.tnpscprep.data.news.NewsPipeline
import java.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

data class FeedResult(val feedId: String, val name: String, val kept: Int, val error: String?)

/**
 * Fetch -> parse -> filter -> store. Each feed is isolated: one dead or changed feed never stops the others
 * (two of the feeds we tried while building this were already broken).
 */
class NewsRepository(
    private val db: AppDatabase,
    private val source: FeedSource,
    private val feeds: List<FeedConfig>,
    private val settings: SettingsStore,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val dao = db.newsDao()
    private val lock = Mutex()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()
    private val _results = MutableStateFlow<List<FeedResult>>(emptyList())
    val lastResults: StateFlow<List<FeedResult>> = _results.asStateFlow()

    fun observeSince(days: Int?) = dao.observeSince(if (days == null) 0L else now() - Duration.ofDays(days.toLong()).toMillis())
    fun observeRevised() = dao.observeRevised()

    /**
     * Returns null when nothing was done (feature off, fetched recently, or a refresh already running).
     * [force] = the user pressed Refresh: ignores the 6-hour pause but never the on/off switch.
     */
    suspend fun refresh(force: Boolean): List<FeedResult>? {
        if (!settings.newsEnabled.value) return null
        val t = now()
        if (!force && t - settings.lastNewsRefreshMs.value < MIN_INTERVAL_MS) return null
        if (!lock.tryLock()) return null
        _refreshing.value = true
        try {
            val results = feeds.map { feed ->
                try {
                    val rows = NewsPipeline.toEntities(feed, FeedParser.parse(source.fetch(feed.url)), t)
                    dao.insertAll(rows)
                    FeedResult(feed.id, feed.name, rows.size, null)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    FeedResult(feed.id, feed.name, 0, (e.message ?: e.javaClass.simpleName).take(80))
                }
            }
            dao.prune(t - Duration.ofDays(NewsPipeline.RETENTION_DAYS).toMillis())
            if (results.any { it.error == null }) settings.setLastNewsRefresh(t)
            _results.value = results
            android.util.Log.i("NewsRepository", "refresh: " + results.joinToString { "${it.feedId}=${it.error ?: it.kept}" })
            return results
        } finally {
            _refreshing.value = false
            lock.unlock()
        }
    }

    companion object { val MIN_INTERVAL_MS: Long = Duration.ofHours(6).toMillis() }
}
