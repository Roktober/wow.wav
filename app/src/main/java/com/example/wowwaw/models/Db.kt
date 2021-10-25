package com.example.wowwaw.models

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.wowwaw.models.entities.SoundLocationEntity
import com.example.wowwaw.viewModels.SoundLocation
import com.google.android.gms.maps.model.LatLng

class SoundLocationRepository internal constructor(application: Application) {

    private val soundLocationDao: SoundLocationDao
    private val soundLocations: LiveData<List<SoundLocationEntity>>

    init {
        val db: LocalDatabase = LocalDatabase.getDatabase(application.applicationContext)
        soundLocationDao = db.soundLocationDao()
        soundLocations = soundLocationDao.getAll()
    }

    val allSoundLocations: LiveData<List<SoundLocation>>
        get() = Transformations.map(soundLocations) { x ->
            x.map { t -> entityToModel(t) }
        }

    suspend fun insert(soundLocation: SoundLocation) {
        soundLocationDao.insertAll(modelToEntity(soundLocation))
    }

    fun getByName(fileName: String): SoundLocationEntity? {
        return soundLocationDao.loadAllByIds(Array(1) { fileName }).value?.firstOrNull()
    }

    private fun entityToModel(entity: SoundLocationEntity): SoundLocation {
        return SoundLocation(entity.fileName, LatLng(entity.latitude, entity.longitude))
    }

    private fun modelToEntity(model: SoundLocation): SoundLocationEntity {
        return SoundLocationEntity(
            model.fileName,
            model.location.latitude,
            model.location.longitude,
        )
    }
}

@Dao
interface SoundLocationDao {
    @Query("SELECT * FROM soundLocationEntity")
    fun getAll(): LiveData<List<SoundLocationEntity>>

    @Query("SELECT * FROM soundLocationEntity WHERE fileName IN (:filenames)")
    fun loadAllByIds(filenames: Array<String>): LiveData<List<SoundLocationEntity>>

    @Insert
    suspend fun insertAll(vararg soundLocations: SoundLocationEntity)

    @Delete
    fun delete(soundLocation: SoundLocationEntity)
}

@Database(entities = [SoundLocationEntity::class], version = 3)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun soundLocationDao(): SoundLocationDao

    companion object {
        private var INSTANCE: LocalDatabase? = null
        fun getDatabase(context: Context): LocalDatabase {
            if (INSTANCE == null) {
                synchronized(LocalDatabase::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(
                            context.applicationContext,
                            LocalDatabase::class.java, "word_database"
                        )
                            // Wipes and rebuilds instead of migrating
                            // if no Migration object.
                            .fallbackToDestructiveMigration()
                            .build()
                    }
                }
            }
            return INSTANCE!!
        }
    }
}
