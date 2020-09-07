package com.android.shotz_pro_io.rtmp

import android.opengl.EGLContext
import android.os.Handler
import android.os.HandlerThread
import java.io.IOException

class VideoHandler {

    private val FRAME_RATE = 30

    private var handler: Handler? = null
    private var videoEncoder: VideoEncoder? = null

    interface OnVideoEncoderStateListener {
        fun onVideoDataEncoded(data: ByteArray?, size: Int, timestamp: Int)
    }

    fun setOnVideoEncoderStateListener(listener: OnVideoEncoderStateListener?) {
        videoEncoder?.setOnVideoEncoderStateListener(listener)
    }

    fun VideoHandler() {
        videoEncoder = VideoEncoder()
        val handlerThread = HandlerThread("VideoHandler")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }

    fun start(
        width: Int, height: Int, bitRate: Int,
        sharedEglContext: EGLContext?, startStreamingAt: Long
    ) {
        handler!!.post {
            try {
                videoEncoder?.prepare(width, height, bitRate, FRAME_RATE, startStreamingAt)
                videoEncoder?.start()
            } catch (ioe: IOException) {
                throw RuntimeException(ioe)
            }
        }
    }

    fun stop() {
        handler!!.post {
            videoEncoder?.let {
                if(it.isEncoding()){
                    it.stop()
                }
            }
        }
    }
}