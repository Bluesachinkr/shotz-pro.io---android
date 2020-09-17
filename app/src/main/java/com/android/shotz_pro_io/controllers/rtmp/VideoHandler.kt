package com.android.shotz_pro_io.controllers.rtmp

import android.media.projection.MediaProjection
import java.io.IOException
import android.os.Handler
import android.os.HandlerThread

class VideoHandler(mediaProjection: MediaProjection) {

    private val mediaProjection = mediaProjection

    private var handler: Handler
    private var videoEncoder: VideoEncoder

    init {
        videoEncoder = VideoEncoder(mediaProjection)
        val handlerThread = HandlerThread("VideoHandler")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }

    companion object {
        private const val FRAME_RATE = 30
    }

    interface OnVideoEncoderStateListener {
        fun onVideoDataEncoded(data: ByteArray?, size: Int, timestamp: Int)
    }

    fun setOnVideoEncoderStateListener(listener: OnVideoEncoderStateListener?) {
        videoEncoder?.setOnVideoEncoderStateListener(listener)
    }

    fun start(
        width: Int,
        height: Int,
        bitRate: Int,
        startStreamingAt: Long,density : Int
    ){
        handler.post(Runnable {
            try {
                videoEncoder.prepare(
                    width,
                    height,
                    bitRate,
                    FRAME_RATE,
                    startStreamingAt,density
                )
                videoEncoder.start()

            } catch (ioe: IOException) {
                throw RuntimeException(ioe)
            }
        })
    }

    fun stop() {
        handler?.post {
            videoEncoder?.let {
                if (it.isEncoding()) {
                    it.stop()
                }
            }
        }
    }

    private val frameInterval: Long
        private get() = (1000 / FRAME_RATE).toLong()
}