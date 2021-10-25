package com.example.wowwaw.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class SoundModel(val lat: Double, val lng: Double)

fun readSoundModelFromJsonFIle(jsonFile: File): HashMap<String, SoundModel> {
    return Json.decodeFromString(jsonFile.readText())
}

fun writeSoundModelToJsonFile(jsonFile: File, models: HashMap<String, SoundModel>) {
    jsonFile.writeText(Json.encodeToString(models))
}