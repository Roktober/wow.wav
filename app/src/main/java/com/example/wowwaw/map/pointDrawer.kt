package com.example.wowwaw.map

import android.graphics.Color
import com.example.wowwaw.BuildConfig
import com.example.wowwaw.data.SoundModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlin.random.Random

class pointDrawer(private val map: GoogleMap) {
    private val drawnPoints: HashMap<String, MarkerWithCircle> = HashMap()
    private val randomGenerator = Random(10)

    private fun drawPoint(
        name: String,
        lat: Double,
        lng: Double,
        strokeColor: Int,
        fillColor: Int
    ) {
        val loc = LatLng(lat, lng)

        val circle = map.addCircle(
            CircleOptions().apply {
                center(loc)
                radius(BuildConfig.SOUND_DISTANCE_MAX.toDouble())
                strokeColor(strokeColor)
                fillColor(fillColor)
            }
        )

        map.addMarker(
            MarkerOptions()
                .position(loc)
                .title(name)
        )?.let {
            drawnPoints[name] = MarkerWithCircle(it, circle)
        }
    }

    private fun getRandomColor(): Int {
        return Color.argb(
            BuildConfig.SOUND_ALPHA_COLOR,
            randomGenerator.nextInt(256),
            randomGenerator.nextInt(256),
            randomGenerator.nextInt(256)
        )
    }

    private fun drawRandomColoredPoint(name: String, model: SoundModel) {
        drawPoint(
            name,
            model.lat,
            model.lng,
            Color.parseColor(BuildConfig.SOUND_STROKE_COLOR),
            getRandomColor()
        )
    }

    fun drawRandomColoredPoints(models: HashMap<String, SoundModel>) {
        // FIXME: Цвета не рандомные
        val oldKeys = this.drawnPoints.keys
        val newKeys = models.keys
        val keysToDraw = newKeys - oldKeys
        val keysToDelete = oldKeys - newKeys
        keysToDraw.forEach { drawRandomColoredPoint(it, models[it]!!) }
        keysToDelete.forEach { deletePointByKey(it) }
    }

    private fun deletePointByKey(it: String) {
        val item = this.drawnPoints[it]!!
        item.circle.remove()
        item.marker.remove()
        this.drawnPoints.remove(it)
    }

    data class MarkerWithCircle(val marker: Marker, val circle: Circle)
}
