package com.example.wowwaw.services

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import androidx.core.net.toUri
import timber.log.Timber
import java.io.File

class SoundPlayerService : Service() {
    private val binder = LocalBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var filePlayingNow: String? = null

    inner class LocalBinder : Binder() {
        fun getService(): SoundPlayerService = this@SoundPlayerService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Timber.i("Im UNBIND ${SoundPlayerService::class.java}")
        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    fun play(name: String) {
        if (mediaPlayer == null) {
            val path = getExternalFilesDir("media")?.absolutePath + File.separator + name
            mediaPlayer = MediaPlayer.create(this, File(path).toUri())
            filePlayingNow = name
            mediaPlayer?.let {
                it.isLooping = true
                it.start()
            }
            Timber.i("Sound $name playing...")
        }
    }

    fun stop() {
        mediaPlayer?.let {
            it.stop()
            it.release()
        }
        filePlayingNow = null
        mediaPlayer = null
        Timber.i("Sound $filePlayingNow stopped")
    }

    fun playingNow(): String? = filePlayingNow
}
