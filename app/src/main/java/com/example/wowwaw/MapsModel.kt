package com.example.wowwaw

import android.app.Service
import android.media.MediaPlayer
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay


class MapsModel(scope: MapsActivity): MediaPlayer.OnPreparedListener {
    var scope: MapsActivity = scope
//    var mediaPlayer: MediaPlayer = MediaPlayer.create(this.scope, R.raw.aaa)

    fun run() {
//        mediaPlayer.prepareAsync()
//        mediaPlayer.setOnPreparedListener(this)

//        scope.mainScope.async {
//            var i = 1;
//            while(true) {
//                i = i +1
//                delay(1000L)
//                print("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
//                Toast.makeText(this@MapsModel.scope, "sas $i", Toast.LENGTH_LONG).show()
//            }
//        }.start()
    }

    override fun onPrepared(p0: MediaPlayer?) {
        p0?.start()
    }
}