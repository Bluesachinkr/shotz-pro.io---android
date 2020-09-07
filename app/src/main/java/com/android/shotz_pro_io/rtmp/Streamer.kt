package com.android.shotz_pro_io.rtmp

import android.opengl.EGLContext

internal class Streamer : VideoHandler.OnVideoEncoderStateListener,
    AudioHandler.OnAudioEncoderStateListener {
    private val videoHandler: VideoHandler
    private val audioHandler: AudioHandler
    private val muxer: Muxer = Muxer()
    fun open(url: String?, width: Int, height: Int) {
        muxer.open(url, width, height)
    }

    fun startStreaming(
        context: EGLContext?, width: Int, height: Int, audioBitrate: Int,
        videoBitrate: Int
    ) {
        if (muxer.isConnected) {
            val startStreamingAt = System.currentTimeMillis()
            videoHandler.setOnVideoEncoderStateListener(this)
            audioHandler.setOnAudioEncoderStateListener(this)
            videoHandler.start(width, height, videoBitrate, context, startStreamingAt)
            audioHandler.start(audioBitrate, startStreamingAt)
        }
    }

    fun stopStreaming() {
        videoHandler.stop()
        audioHandler.stop()
        muxer.close()
    }

    val isStreaming: Boolean
        get() = muxer.isConnected


    fun setMuxerListener(listener: PublisherListener?) {
        muxer.setOnMuxerStateListener(listener)
    }

    init {
        videoHandler = VideoHandler()
        audioHandler = AudioHandler()
    }

    override fun onVideoDataEncoded(data: ByteArray?, size: Int, timestamp: Int) {
        muxer.sendVideo(data, size, timestamp)
    }

    override fun onAudioDataEncoded(data: ByteArray?, size: Int, timestamp: Int) {
        muxer.sendAudio(data, size, timestamp)
    }
}
