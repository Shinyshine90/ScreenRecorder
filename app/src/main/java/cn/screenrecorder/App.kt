package cn.screenrecorder

import android.app.Application
import android.content.Context

class App: Application() {

    companion object {
        var app: Context? = null
    }

    override fun onCreate() {
        super.onCreate()
        app = this
    }
}