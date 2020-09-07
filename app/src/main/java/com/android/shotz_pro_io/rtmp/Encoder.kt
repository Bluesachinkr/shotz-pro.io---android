package com.android.shotz_pro_io.rtmp

interface Encoder {
    fun start()
    fun stop()
    fun isEncoding(): Boolean
}