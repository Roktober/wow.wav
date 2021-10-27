package com.example.wowwaw.viewModels

import androidx.lifecycle.MutableLiveData
import com.example.wowwaw.data.SoundModel

class SoundRepository {
    val data = MutableLiveData<HashMap<String, SoundModel>>()

    companion object {
        val instance = SoundRepository()
    }

    fun setData(s: HashMap<String, SoundModel>) {
        data.postValue(s)
    }

    fun getData(): HashMap<String, SoundModel>? {
        return data.value
    }
}
