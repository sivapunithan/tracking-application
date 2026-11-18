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

    private val CLIP_MOOD = mapOf("laugh" to Mood.LAUGH, "cheer" to Mood.CHEER, "proud" to Mood.PROUD, "celebrate" to Mood.CELEBRATE, "cry" to Mood.CRY, "angry" to Mood.ANGRY, "worried" to Mood.WORRIED)

    /**
     * One channel per clip (a channel's sound is fixed once created), so each notification plays ITS mood's clip through Android's own notification
     * sound path: it follows the phone's silent mode and notification volume. When her "Mood sounds" switch is off, or the clip is not in this build,
     * the silent channel is used. Returns the channel id.
     */
    fun channelFor(ctx: Context, base: String, baseName: String, clip: String?, soundsOn: Boolean): String {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val res = clip?.let { ctx.resources.getIdentifier("snd_$it", "raw", ctx.packageName) } ?: 0
        if (clip == null || !soundsOn || res == 0) {
            val id = base + "_silent"
            nm.createNotificationChannel(NotificationChannel(id, baseName, NotificationManager.IMPORTANCE_HIGH).apply { setSound(null, null); enableVibration(false); setShowBadge(false) })
            return id
        }
        val id = base + "_" + clip
        val label = CLIP_MOOD[clip]?.let { ctx.resources.getStringArray(R.array.mood_labels)[it.ordinal] } ?: ctx.getString(R.string.praise_channel_plan)
        nm.createNotificationChannel(NotificationChannel(id, "$baseName · $label", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(android.net.Uri.parse("android.resource://${ctx.packageName}/$res"), android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            enableVibration(false); setShowBadge(false)
        })
        return id
    }

    fun canPost(ctx: Context): Boolean =
        NotificationManagerCompat.from(ctx).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    /** The mood picture as a notification icon, or null if the picture is not in this build. */
    fun moodBitmap(ctx: Context, mood: Mood) = moodImageRes(ctx, mood, 100.dp).takeIf { it != 0 }?.let { runCatching { BitmapFactory.decodeResource(ctx.resources, it) }.getOrNull() }

    fun notify(context: Context, event: PraiseEvent, goalMet: Boolean = false) {
        val app = context.applicationContext as TnpscApp
        val settings = app.container.settingsStore
        if (!settings.praiseNotifs.value) return
        // notifications not allowed on this phone: she still hears the mood's clip, right in the app
        if (!canPost(app)) { com.spcode.tnpscprep.ui.mood.MoodSounds.play(app, PraiseRules.soundFor(event, goalMet)); return }
        val ctx = localised(app)
        val v = PraiseRules.variant(settings.nextPraiseCounter())
        val key = event.name.lowercase()
        fun arr(suffix: String): Array<String>? = ctx.resources.getIdentifier("praise_${key}_$suffix", "array", ctx.packageName).takeIf { it != 0 }?.let { ctx.resources.getStringArray(it) }
        // a missing text must never crash the app: simply no banner
        val title = arr("title")?.getOrNull(v) ?: return; val body = arr("body")?.getOrNull(v) ?: return
        val chan = channelFor(ctx, CHANNEL, ctx.getString(R.string.praise_channel), PraiseRules.soundFor(event, goalMet), settings.moodSounds.value)
        val open = PendingIntent.getActivity(ctx, 1, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE)
        // On a locked screen nothing personal shows: the public version has no picture and no wording.
        val public = NotificationCompat.Builder(ctx, chan).setSmallIcon(R.drawable.ic_stat_wrapup).setContentTitle(ctx.getString(R.string.app_name)).build()
        val b = NotificationCompat.Builder(ctx, chan)
            .setSmallIcon(R.drawable.ic_stat_wrapup).setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open).setAutoCancel(true).setTimeoutAfter(12_000)
            .setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(public)
        moodBitmap(ctx, PraiseRules.moodFor(event, goalMet))?.let { b.setLargeIcon(it) }
        NotificationManagerCompat.from(ctx).notify(ID, b.build())
    }
}
