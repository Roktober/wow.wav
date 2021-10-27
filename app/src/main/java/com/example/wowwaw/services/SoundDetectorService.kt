package com.example.wowwaw.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import android.location.LocationListener
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import com.example.wowwaw.BuildConfig
import com.example.wowwaw.data.SoundModel
import com.example.wowwaw.viewModels.SoundRepository
import timber.log.Timber

class SoundDetectorService : Service(), LocationListener {
    private val binder = LocalBinder()
    private val soundRepository = SoundRepository.instance
    private lateinit var soundPlayerService: SoundPlayerService

    @SuppressLint("MissingPermission")
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            if (service is SoundPlayerService.LocalBinder) {
                soundPlayerService = service.getService()
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): SoundDetectorService = this@SoundDetectorService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Timber.i("Im UNBIND ${SoundDetectorService::class.java}")
        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Intent(this, SoundPlayerService::class.java).also { i ->
            bindService(i, serviceConnection, Context.BIND_AUTO_CREATE)
        }
        return START_STICKY
    }

    private fun searchClosestSound(
        soundModels: HashMap<String, SoundModel>,
        currentLocation: Location
    ): String? {
        var minDistance = Float.POSITIVE_INFINITY
        var closestSound: SoundModel? = null
        var closestSoundName: String? = null
        for ((name, soundModel) in soundModels) {
            val soundLocation = Location("").apply {
                latitude = soundModel.lat
                longitude = soundModel.lng
            }
            val distance = currentLocation.distanceTo(soundLocation)
            if (distance < minDistance) {
                minDistance = distance
                closestSound = soundModel
                closestSoundName = name
            }
        }
        if (minDistance < BuildConfig.SOUND_DISTANCE_MAX.toFloat()) {
            Timber.i("Closest sound: $closestSound")
            return closestSoundName
        }
        return null
    }

    override fun onLocationChanged(location: Location) {
        val closedSound = soundRepository.getData()?.let { searchClosestSound(it, location) }
        if (closedSound != null) {
            if (closedSound != soundPlayerService.playingNow()) {
                soundPlayerService.stop()
                soundPlayerService.play(closedSound)
            }
        } else {
            soundPlayerService.stop()
        }
    }

    override fun onProviderDisabled(provider: String) {
        Timber.i("OMG I HAVENT PROVIDER")
    }

    override fun onProviderEnabled(provider: String) {
        Timber.i("OMG I HAVE PROVIDER")
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        // На андроиде 8 (было замечено на телефоне Хуавей) вызвывается данный метод
    }
}
