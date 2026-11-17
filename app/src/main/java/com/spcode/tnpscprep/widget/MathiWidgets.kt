package com.spcode.tnpscprep.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.view.View
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatDelegate
import com.spcode.tnpscprep.MainActivity
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.TnpscApp
import com.spcode.tnpscprep.data.SettingsStore
import com.spcode.tnpscprep.domain.LogicalDay
import com.spcode.tnpscprep.domain.WidgetRules
import com.spcode.tnpscprep.ui.timer.buildDayStats
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Home-screen widgets (plain RemoteViews: no extra library). They show STUDY facts only, because a widget sits outside the PIN lock.
 * The system refreshes them about every 30 minutes at most, and the app refreshes them whenever she leaves it (MainActivity.onStop).
 */
object WidgetUpdater {
    fun refreshAll(context: Context) {
        val app = context.applicationContext
        val mgr = AppWidgetManager.getInstance(app)
        for (cls in listOf(TodayWidget::class.java, PlanWidget::class.java)) {
            val ids = mgr.getAppWidgetIds(ComponentName(app, cls))
            if (ids.isNotEmpty()) update(app, mgr, cls, ids)
        }
    }

    /** True when Android accepted the request (the system then shows its own "add widget" dialog); false when this launcher cannot pin widgets. */
    fun requestPin(context: Context, cls: Class<*>): Boolean {
        val mgr = AppWidgetManager.getInstance(context)
        if (!mgr.isRequestPinAppWidgetSupported) return false
        return runCatching { mgr.requestPinAppWidget(ComponentName(context, cls), null, null) }.getOrDefault(false)
    }

    fun update(context: Context, mgr: AppWidgetManager, cls: Class<*>, ids: IntArray, done: (() -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val ctx = localised(context)
                val c = (context.applicationContext as TnpscApp).container
                val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE)
                val views = if (cls == TodayWidget::class.java) todayViews(ctx, c.settingsStore, c.database.sessionDao().observeDayTotals().first()) else planViews(ctx, c.plannedTaskRepository.get(LogicalDay.of(System.currentTimeMillis(), ZoneId.systemDefault())))
                views.setOnClickPendingIntent(R.id.widget_root, open)
                ids.forEach { mgr.updateAppWidget(it, views) }
            } catch (_: Exception) {
                // a widget must never crash the app: keep whatever it showed before
            } finally { done?.invoke() }
        }
    }

    private fun todayViews(ctx: Context, s: SettingsStore, totals: List<com.spcode.tnpscprep.data.db.DayTotal>): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_today)
        val goal = s.dailyGoalMinutes.value
        val st = buildDayStats(totals, goal, LocalDate.now())
        val examDay = s.examEpochDay.value
        val days = if (examDay == SettingsStore.NO_DATE) null else ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.ofEpochDay(examDay))
        val count = WidgetRules.countdown(days)
        if (count != null) {
            v.setTextViewText(R.id.w_big, count.toString())
            v.setTextViewText(R.id.w_big_label, ctx.resources.getQuantityString(R.plurals.widget_days_to_exam, count.toInt().coerceAtLeast(0)))
        } else {
            v.setTextViewText(R.id.w_big, "—")
            v.setTextViewText(R.id.w_big_label, ctx.getString(R.string.widget_set_exam))
        }
        val (m, g) = WidgetRules.minutesLine(st.todayMinutes, goal)
        v.setTextViewText(R.id.w_minutes, ctx.getString(R.string.widget_minutes, m, g))
        v.setTextViewText(R.id.w_streak, if (st.streak > 0) ctx.getString(R.string.tile_streak, st.streak) else "")
        v.setViewVisibility(R.id.w_streak, if (st.streak > 0) View.VISIBLE else View.GONE)
        val pic = ctx.resources.getIdentifier("mood_proud", "drawable", ctx.packageName) // git-ignored personal picture: optional
        if (pic != 0) { v.setImageViewResource(R.id.w_pic, pic); v.setViewVisibility(R.id.w_pic, View.VISIBLE) } else v.setViewVisibility(R.id.w_pic, View.GONE)
        return v
    }

    private fun planViews(ctx: Context, tasks: List<com.spcode.tnpscprep.data.db.PlannedTaskEntity>): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_plan)
        val lines = WidgetRules.planLines(tasks.map { it.title to it.done })
        val rows = intArrayOf(R.id.w_line1, R.id.w_line2, R.id.w_line3)
        rows.forEachIndexed { i, id ->
            val l = lines.getOrNull(i)
            if (l == null) v.setViewVisibility(id, View.GONE) else { v.setViewVisibility(id, View.VISIBLE); v.setTextViewText(id, (if (l.second) "✓  " else "○  ") + l.first) }
        }
        val doneCount = tasks.count { it.done }
        v.setTextViewText(R.id.w_plan_title, if (tasks.isEmpty()) ctx.getString(R.string.widget_plan_title) else ctx.getString(R.string.widget_plan_title_n, doneCount, tasks.size))
        v.setViewVisibility(R.id.w_plan_empty, if (tasks.isEmpty()) View.VISIBLE else View.GONE)
        return v
    }

    /** The app's chosen language (per-app locale), not necessarily the phone's. */
    private fun localised(ctx: Context): Context {
        val l = AppCompatDelegate.getApplicationLocales()
        if (l.isEmpty) return ctx
        return ctx.createConfigurationContext(Configuration(ctx.resources.configuration).apply { setLocale(l[0]) })
    }
}

class TodayWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        val p = goAsync(); WidgetUpdater.update(context, mgr, TodayWidget::class.java, ids) { p.finish() }
    }
}

class PlanWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        val p = goAsync(); WidgetUpdater.update(context, mgr, PlanWidget::class.java, ids) { p.finish() }
    }
}
