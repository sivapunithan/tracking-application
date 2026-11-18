package com.spcode.tnpscprep.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.spcode.tnpscprep.MainActivity
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.TnpscApp
import com.spcode.tnpscprep.domain.LogicalDay
import com.spcode.tnpscprep.domain.ReminderTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires at the chosen time: schedules tomorrow's alarm first, then shows ONE calm notification unless she already wrapped up today. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as TnpscApp
        val settings = app.container.settingsStore
        if (!settings.reminderEnabled.value) return
        ReminderScheduler.schedule(context, settings.reminderMinutes.value) // next day, whatever happens below
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val day = LogicalDay.of(System.currentTimeMillis(), ZoneId.systemDefault())
                val wrapped = app.container.diaryRepository.wrapupFor(day) != null
                if (ReminderTime.shouldNotify(true, wrapped)) {
                    // her face follows how long she has been away (the avatar's own ladder); the words stay kind
                    val last = app.container.sessionRepository.lastStudyDate()
                    val away = last?.let { java.time.temporal.ChronoUnit.DAYS.between(it, day).toInt() }
                    show(localised(context), com.spcode.tnpscprep.domain.PraiseRules.reminderMood(away, java.time.LocalTime.now().hour))
                }
            } finally { pending.finish() }
        }
    }

    /** The app's chosen language (per-app locale), not necessarily the phone's. */
    private fun localised(ctx: Context): Context {
        val l = AppCompatDelegate.getApplicationLocales()
        if (l.isEmpty) return ctx
        val cfg = Configuration(ctx.resources.configuration).apply { setLocale(l[0]) }
        return ctx.createConfigurationContext(cfg)
    }

    private fun show(ctx: Context, mood: com.spcode.tnpscprep.domain.Mood) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val clip = com.spcode.tnpscprep.domain.MoodSoundRules.forMood(mood.name)
        val chan = com.spcode.tnpscprep.notify.Praise.channelFor(ctx, "reminder", ctx.getString(R.string.reminder_channel), clip, (ctx.applicationContext as TnpscApp).container.settingsStore.moodSounds.value)
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, chan)
            .setSmallIcon(R.drawable.ic_stat_wrapup)
            .setContentTitle(ctx.getString(R.string.wrap_title))
            .setContentText(ctx.getString(R.string.reminder_text)) // generic on purpose: nothing private on the lock screen
            .setContentIntent(open).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(NotificationCompat.Builder(ctx, chan).setSmallIcon(R.drawable.ic_stat_wrapup).setContentTitle(ctx.getString(R.string.app_name)).build())
        com.spcode.tnpscprep.notify.Praise.moodBitmap(ctx, mood)?.let { n.setLargeIcon(it) }
        val built = n.build()
        nm.notify(NOTIF_ID, built)
    }

    companion object { const val CHANNEL = "wrapup_reminder"; const val NOTIF_ID = 4102 }
}
