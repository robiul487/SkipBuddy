package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.example.model.AppLanguage

/**
 * Plays the bundled voice announcement MP3s (res/raw/ad_wait_bn.mp3, ad_wait_en.mp3, ad_wait_hi.mp3)
 * on the Android Accessibility audio stream (USAGE_ASSISTANCE_ACCESSIBILITY + CONTENT_TYPE_SPEECH)
 * so the announcement stays clearly audible while STREAM_MUSIC is muted for an ad.
 *
 * Uses MediaPlayer exclusively (NO TextToSpeech anywhere).
 * Never loops and never overlaps.
 */
class AnnouncementAudioPlayer(private val appContext: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val lock = Any()

    fun playOnce(language: AppLanguage, onComplete: (() -> Unit)? = null) {
        synchronized(lock) {
            stopInternal()
            val rawRes = language.resolveAudioRes()
            try {
                val afd = appContext.resources.openRawResourceFd(rawRes) ?: return
                val player = MediaPlayer()
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                player.setAudioAttributes(attrs)
                player.isLooping = false
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                player.setOnCompletionListener { mp ->
                    synchronized(lock) {
                        if (mediaPlayer === mp) {
                            mediaPlayer = null
                        }
                    }
                    runCatching { mp.release() }
                    onComplete?.invoke()
                }
                player.setOnErrorListener { mp, _, _ ->
                    synchronized(lock) {
                        if (mediaPlayer === mp) {
                            mediaPlayer = null
                        }
                    }
                    runCatching { mp.release() }
                    true
                }
                player.prepare()
                mediaPlayer = player
                player.start()
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to play bundled announcement audio", t)
                stopInternal()
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            stopInternal()
        }
    }

    private fun stopInternal() {
        val existing = mediaPlayer
        mediaPlayer = null
        if (existing != null) {
            runCatching {
                if (existing.isPlaying) {
                    existing.stop()
                }
            }
            runCatching { existing.release() }
        }
    }

    companion object {
        private const val TAG = "SkipBuddyAudio"
    }
}
