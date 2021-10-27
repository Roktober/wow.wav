package com.example.wowwaw.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.LatLng

class SoundLocationViewModel(a: Application) :
    AndroidViewModel(a) {
    private val soundRepository = SoundRepository.instance
    val data = soundRepository.data
}

class MyViewModelFactory(private val mApplication: Application) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SoundLocationViewModel(mApplication) as T
    }
}

data class SoundMapModel(val soundLocations: List<SoundLocation>)

data class SoundLocation(val fileName: String, val location: LatLng)
