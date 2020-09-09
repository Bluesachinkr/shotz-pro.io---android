package com.android.shotz_pro_io.stream

import android.app.Service
import android.content.Intent
import android.os.IBinder

abstract class BaseService : Service() {
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    abstract fun startPerformService(StreamUrl : String)
    abstract fun stopPerformService()
}