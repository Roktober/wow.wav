package com.example.wowwaw.services

import android.app.Service
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import com.example.wowwaw.BuildConfig
import com.example.wowwaw.data.SoundModel
import com.example.wowwaw.data.readSoundModelFromJsonFIle
import com.example.wowwaw.data.writeSoundModelToJsonFile
import com.example.wowwaw.fs.saveFileIntoExternalStorageByUri
import com.example.wowwaw.services.sftp.SftpClient
import com.example.wowwaw.viewModels.SoundRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import net.schmizz.sshj.sftp.Response
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.sftp.SFTPException
import timber.log.Timber
import java.io.File
import java.lang.Thread.sleep
import java.security.Security

class FileSynchronizationService : Service() {
    private val SYNC_INTERVAL_MS = (BuildConfig.SYNC_INTERVAL_SECONDS * 1000).toLong()
    private val JSON_FILE_NAME = BuildConfig.JSON_FILE_NAME
    private val SHARE_FOLDER = BuildConfig.SFTP_SHARE_FOLDER
    private val EXTERNAL_FOLDER = "media"

    private lateinit var JSON_FILE: File
    private val sshClient = SftpClient(
        BuildConfig.SFTP_SERVER_HOST,
        BuildConfig.SFTP_SERVER_PORT,
        BuildConfig.SFTP_USER,
        BuildConfig.SFTP_PASSWORD
    )
    private val coroutineScope = MainScope()
    private val soundRepository = SoundRepository.instance

    private val pointsData: HashMap<String, SoundModel> = HashMap()

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): FileSynchronizationService = this@FileSynchronizationService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Timber.i("Im UNBIND ${SoundPlayerService::class.java}")
        return super.onUnbind(intent)
    }

    override fun onCreate() {
        // Для поддержки sftp
        // https://github.com/web3j/web3j/issues/915
        Security.removeProvider("BC")
        Security.insertProviderAt(org.bouncycastle.jce.provider.BouncyCastleProvider(), 0)
        super.onCreate()
    }

    private fun initJsonFile() {
        JSON_FILE = File(getExternalFilesDir(EXTERNAL_FOLDER), JSON_FILE_NAME)
        if (JSON_FILE.exists()) {
            tryToReadJsonFile(JSON_FILE)?.let { pointsData.putAll(it) }
            soundRepository.setData(pointsData)
        } else {
            JSON_FILE.createNewFile()
        }
        Timber.i("Init Json points data $pointsData")
    }

    private fun tryToReadJsonFile(jsonFile: File): HashMap<String, SoundModel>? {
        return try {
            readSoundModelFromJsonFIle(jsonFile)
        } catch (ex: SerializationException) {
            Timber.e(ex, "Json file is broken or empty")
            null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        initJsonFile()
        run()
        return START_STICKY
    }

    private fun run() {
        coroutineScope.launch(Dispatchers.IO) {
            while (true) {
                var outputFile: File? = null
                try {
                    Timber.d("Start connection to sftp server, current state: $pointsData")
                    sshClient.withSession { session ->
                        val tempFile = File.createTempFile("points_data", ".json.tmp", cacheDir)
                        var jsonExists = false
                        outputFile = tempFile
                        try {
                            session.get(
                                File(SHARE_FOLDER, JSON_FILE_NAME).path,
                                tempFile.absolutePath
                            )
                            jsonExists = true
                        } catch (ex: SFTPException) {
                            if (Response.StatusCode.NO_SUCH_FILE != ex.statusCode) {
                                throw ex
                            }
                        }

                        val pointsDataServer =
                            if (jsonExists) tryToReadJsonFile(tempFile) else HashMap()

                        if (pointsDataServer == null) {
                            putJsonFileOnServer(session)
                            return@withSession
                        }

                        for (pointName in pointsDataServer.keys - pointsData.keys) {
                            session.get(
                                File(SHARE_FOLDER, pointName).toString(),
                                getExternalFilesDir(EXTERNAL_FOLDER)?.absolutePath
                            )
                            val pointModel = pointsDataServer[pointName]
                            pointsData[pointName] = pointModel!!
                        }

                        for (pointName in pointsData.keys - pointsDataServer.keys) {
                            session.put(
                                File(getExternalFilesDir(EXTERNAL_FOLDER), pointName).toString(),
                                File(SHARE_FOLDER, pointName).toString()
                            )
                        }
                        writeSoundModelToJsonFile(JSON_FILE, pointsData)
                        putJsonFileOnServer(session)
                        soundRepository.setData(pointsData)
                    }
                } catch (ex: Exception) {
                    Timber.e(ex, "Syn error: $ex")
                } finally {
                    outputFile?.delete()
                    sleep(SYNC_INTERVAL_MS)
                }
            }
        }
    }

    private fun putJsonFileOnServer(session: SFTPClient) {
        session.put(
            File(getExternalFilesDir(EXTERNAL_FOLDER), JSON_FILE_NAME).toString(),
            File(SHARE_FOLDER, JSON_FILE_NAME).toString()
        )
    }

    fun addFile(userFile: Uri, location: Location) {
        coroutineScope.launch(Dispatchers.IO) {
            Timber.i("Try to save file $userFile with location $location")
            val externalFolder = getExternalFilesDir(EXTERNAL_FOLDER)
            if (externalFolder == null) {
                Timber.e("Cant get external folder")
                return@launch
            }
            val file = saveFileIntoExternalStorageByUri(userFile, externalFolder, contentResolver)
            file?.let {
                pointsData[it.name] = SoundModel(location.latitude, location.longitude)
                writeSoundModelToJsonFile(JSON_FILE, pointsData)
                soundRepository.setData(pointsData)
            }
        }
    }
}
