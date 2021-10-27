package com.example.wowwaw

import android.app.Application
import android.content.Context
import org.acra.ACRA
import org.acra.config.httpSender
import org.acra.config.toast
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.acra.sender.HttpSender
import timber.log.Timber

class WowwawApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        initAcra {
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.JSON
            reportSendSuccessToast = "Report sent"
            sendReportsInDevMode = true
            toast {
                text = "You died"
            }
            httpSender {
                uri = BuildConfig.ACRA_REPORT_URI
                basicAuthLogin = BuildConfig.ACRA_LOGIN
                basicAuthPassword = BuildConfig.ACRA_PASSWORD
                httpMethod = HttpSender.Method.POST
            }
        }
        ACRA.DEV_LOGGING = BuildConfig.DEBUG
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
