package com.android.shotz_pro_io.stream

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class StreamingService : Service() {
    companion object {
        lateinit var mContext: StreamingService
        val AUDIO_SAMPLE_RATE = 44100
    }

    val binder = LocalBinder()
    val activity = StreamingActivity.getInstance()
    private val frame_mutex = Object()
    private var encoding = false
    open var isStreaming = false

    override fun onBind(p0: Intent?): IBinder? {
        mContext = this
        return binder
    }

    open fun startStreaming(streamUrl: String) {
        encoding = true
        isStreaming = true
        activity.setVideoFrameCallback(object : StreamingActivity.VideoFrameCallback {
            override fun onHandleFrame(result: ByteArray) {
                if (encoding) {
                    synchronized(frame_mutex) {
                        val currentTime = System.currentTimeMillis()
                        val timestamp = (currentTime - RtmpClient.streamingStartAt).toInt()
                        RtmpClient.listenerVideo?.onVideoDataEncoded(result, result.size, timestamp)
                    }
                }
            }
        })
        activity.setAudioFrameCallback(object : StreamingActivity.AudioFrameCallback {
            override fun onHandleFrame(data: ByteArray, length: Int) {
                if (encoding) {
                    synchronized(frame_mutex) {
                        val currentTime = System.currentTimeMillis()
                        val timestamp = (currentTime - RtmpClient.streamingStartAt).toInt()
                        RtmpClient.listenerAudio?.onAudioDataEncoded(data, length, timestamp)
                    }
                }
            }
        })
        synchronized(frame_mutex) {
            activity.startVideoStream()
            activity.startAudioStream(AUDIO_SAMPLE_RATE)
        }
    }

    open fun stopStreaming() {
        activity.stopVideoStream()
        activity.stopAudioStream()
        encoding = false
        isStreaming = false
    }

    class LocalBinder : Binder() {
        fun getService(): StreamingService {
            return mContext
        }
    }
}