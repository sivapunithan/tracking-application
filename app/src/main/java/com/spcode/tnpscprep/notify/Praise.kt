package com.spcode.tnpscprep.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.spcode.tnpscprep.MainActivity
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.TnpscApp
import com.spcode.tnpscprep.domain.Mood
import com.spcode.tnpscprep.domain.PraiseEvent
import com.spcode.tnpscprep.domain.PraiseRules
import androidx.compose.ui.unit.dp
import com.spcode.tnpscprep.ui.mood.moodImageRes

/** Kind notifications that carry her mood picture. One slot: a new one replaces the last, so they never pile up. */
object Praise {
    const val CHANNEL = "praise"
    private const val ID = 4201

    fun localised(ctx: Context): Context {
        val l = AppCompatDelegate.getApplicationLocales()
        if (l.isEmpty) return ctx
        return ctx.createConfigurationContext(Configuration(ctx.resources.configuration).apply { setLocale(l[0]) })
    }

    fun canPost(ctx: Context): Boolean =
        NotificationManagerCompat.from(ctx).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    /** The mood picture as a notification icon, or null if the picture is not in this build. */
    fun moodBitmap(ctx: Context, mood: Mood) = moodImageRes(ctx, mood, 100.dp).takeIf { it != 0 }?.let { runCatching { BitmapFactory.decodeResource(ctx.resources, it) }.getOrNull() }

    fun notify(context: Context, event: PraiseEvent, goalMet: Boolean = false) {
        val app = context.applicationContext as TnpscApp
        val settings = app.container.settingsStore
        if (!settings.praiseNotifs.value || !canPost(app)) return
        val ctx = localised(app)
        val v = PraiseRules.variant(settings.nextPraiseCounter())
        val key = event.name.lowercase()
        fun arr(suffix: String): Array<String>? = ctx.resources.getIdentifier("praise_${key}_$suffix", "array", ctx.packageName).takeIf { it != 0 }?.let { ctx.resources.getStringArray(it) }
        // a missing text must never crash the app: simply no banner
        val title = arr("title")?.getOrNull(v) ?: return; val body = arr("body")?.getOrNull(v) ?: return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // HIGH importance = a banner that slides in; the channel itself is silent (our own mood sounds are the only sound).
        nm.createNotificationChannel(NotificationChannel(CHANNEL, ctx.getString(R.string.praise_channel), NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null); enableVibration(false); setShowBadge(false)
        })
        val open = PendingIntent.getActivity(ctx, 1, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE)
        // On a locked screen nothing personal shows: the public version has no picture and no wording.
        val public = NotificationCompat.Builder(ctx, CHANNEL).setSmallIcon(R.drawable.ic_stat_wrapup).setContentTitle(ctx.getString(R.string.app_name)).build()
        val b = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_wrapup).setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open).setAutoCancel(true).setTimeoutAfter(12_000).setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(public)
        moodBitmap(ctx, PraiseRules.moodFor(event, goalMet))?.let { b.setLargeIcon(it) }
        NotificationManagerCompat.from(ctx).notify(ID, b.build())
    }
}
