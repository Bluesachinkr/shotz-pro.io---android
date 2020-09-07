package com.android.shotz_pro_io.rtmp

interface PublisherListener {
    fun onStarted()
    fun onStopped()
    fun onDisconnected()
    fun onFailedToConnect()
}