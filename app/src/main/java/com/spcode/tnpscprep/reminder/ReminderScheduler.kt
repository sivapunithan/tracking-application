package com.spcode.tnpscprep.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.spcode.tnpscprep.domain.ReminderTime
import java.time.ZoneId

/**
 * The optional evening reminder. WHY this mechanism: AlarmManager.setAndAllowWhileIdle is an INEXACT alarm (may be a few minutes late),
 * needs no extra permission, and works offline. It is a one-shot: each time it fires the receiver schedules the next day, and the app also
 * re-schedules on every start (a phone restart clears alarms; we do not ask for the boot permission). ColorOS may still delay or stop it: see the in-app guide.
 */
object ReminderScheduler {
    private const val REQUEST = 4101

    private fun pending(ctx: Context): PendingIntent = PendingIntent.getBroadcast(
        ctx, REQUEST, Intent(ctx, ReminderReceiver::class.java).setPackage(ctx.packageName), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun schedule(ctx: Context, minutesOfDay: Int, nowMs: Long = System.currentTimeMillis()) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ReminderTime.nextTrigger(nowMs, minutesOfDay, ZoneId.systemDefault()), pending(ctx))
    }

    fun cancel(ctx: Context) {
        (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending(ctx))
    }

    /** Makes the alarm match the settings (on app start, after toggling, after changing the time). */
    fun sync(ctx: Context, enabled: Boolean, minutesOfDay: Int) { if (enabled) schedule(ctx, minutesOfDay) else cancel(ctx) }
}
