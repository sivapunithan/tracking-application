package com.spcode.tnpscprep.ui.mood

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.spcode.tnpscprep.TnpscApp

/**
 * Short reaction clips (res/raw/snd_<name>), played only when she herself picks a mood/food answer or opens the recap. Never at app start.
 * Optional like the pictures: a missing clip is simply silent. Follows the phone's silent mode and system sound volume
 * (USAGE_ASSISTANCE_SONIFICATION). One clip at a time; the player is released when the clip ends, so nothing stays in memory.
 */
object MoodSounds {
    private var current: MediaPlayer? = null

    fun play(context: Context, name: String?, force: Boolean = false) {
        if (name == null) return
        val ctx = context.applicationContext
        if (!force && !(ctx as TnpscApp).container.settingsStore.moodSounds.value) return
        val id = ctx.resources.getIdentifier("snd_$name", "raw", ctx.packageName)
        if (id == 0) return
        runCatching {
            current?.release()
            val mp = MediaPlayer()
            current = mp
            ctx.resources.openRawResourceFd(id).use { mp.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            mp.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener { it.release(); if (current === it) current = null }
            mp.setOnErrorListener { p, _, _ -> p.release(); if (current === p) current = null; true }
            mp.prepareAsync()
        }
    }
}
