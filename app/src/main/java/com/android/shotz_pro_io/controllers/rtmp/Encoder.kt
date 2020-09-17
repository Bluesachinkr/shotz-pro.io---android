package com.android.shotz_pro_io.controllers.rtmp

interface Encoder {
    fun start()
    fun stop()
    fun isEncoding(): Boolean
}