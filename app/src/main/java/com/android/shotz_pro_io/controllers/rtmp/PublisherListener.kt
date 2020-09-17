package com.android.shotz_pro_io.controllers.rtmp

interface PublisherListener {
    fun onStarted()
    fun onStopped()
    fun onDisconnected()
    fun onFailedToConnect()
}