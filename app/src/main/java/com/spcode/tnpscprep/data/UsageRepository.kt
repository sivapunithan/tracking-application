package com.spcode.tnpscprep.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import androidx.room.withTransaction
import com.spcode.tnpscprep.data.db.AppDatabase
import com.spcode.tnpscprep.data.db.UsageDayEntity
import com.spcode.tnpscprep.data.db.UsageTotalEntity
import com.spcode.tnpscprep.domain.PhoneUsage
import com.spcode.tnpscprep.domain.UsageEv
import com.spcode.tnpscprep.domain.UsageForeground
import com.spcode.tnpscprep.domain.UsageKind
import java.time.LocalDate
import java.time.ZoneId

/** Phone-usage snapshots. Reads Android's own usage stats ON APP OPEN only; stores minutes per day on this phone. */
class UsageRepository(private val db: AppDatabase, private val context: Context) {
    private val dao = db.usageDao()

    fun hasAccess(): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= 29) ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        else @Suppress("DEPRECATION") ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun settingsIntent(): Intent? =
        Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS).takeIf { it.resolveActivity(context.packageManager) != null }

    fun observeApps(from: LocalDate) = dao.observeApps(from.toString())
    fun observeTotals(from: LocalDate) = dao.observeTotals(from.toString())

    /** Refresh the last 7 days. Returns false when access is not granted (nothing is read). */
    suspend fun snapshot(today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault(), nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!hasAccess()) return false
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = context.packageManager
        val home = pm.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        for (date in PhoneUsage.datesToRead(today)) {
            val (start, end) = PhoneUsage.window(date, nowMs, zone)
            val raw = if (end <= start) emptyMap() else UsageForeground.compute(readEvents(usm, start, end), start, end)
                // Only apps with a home-screen icon, minus the launcher and this app itself: keeps system services out of her numbers.
                .filter { (pkg, _) -> pkg != home && pkg != context.packageName && pm.getLaunchIntentForPackage(pkg) != null }
            val u = PhoneUsage.summarise(raw)
            db.withTransaction {
                dao.deleteDay(date.toString())
                dao.insertApps(u.topApps.map { (pkg, min) -> UsageDayEntity(date.toString(), pkg, labelOf(pm, pkg), min) })
                dao.upsertTotal(UsageTotalEntity(date.toString(), u.totalMinutes))
            }
        }
        val cutoff = today.minusDays(PhoneUsage.KEEP_DAYS.toLong()).toString()
        dao.pruneApps(cutoff); dao.pruneTotals(cutoff)
        return true
    }

    /** Raw open/close events for one window. Numeric values are the stable UsageEvents.Event constants (1 = activity resumed, 2 = paused, 16 = screen off, 17 = keyguard shown, 26 = shutdown). */
    private fun readEvents(usm: UsageStatsManager, start: Long, end: Long): List<UsageEv> {
        val out = ArrayList<UsageEv>()
        val events = usm.queryEvents(start, end)
        val e = android.app.usage.UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            val kind = when (e.eventType) {
                1 -> UsageKind.FG
                2 -> UsageKind.BG // NOT 23 (stopped): it arrives after the next activity resumed and would cut that span short
                16, 17, 26 -> UsageKind.SCREEN_OFF // screen off / keyguard shown / shutdown
                else -> null
            } ?: continue
            out += UsageEv(e.packageName ?: "", kind, e.timeStamp)
        }
        return out
    }

    suspend fun deleteAll() = db.withTransaction { dao.deleteAllApps(); dao.deleteAllTotals() }

    private fun labelOf(pm: PackageManager, pkg: String): String =
        runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrElse { PhoneUsage.fallbackName(pkg) }
}
