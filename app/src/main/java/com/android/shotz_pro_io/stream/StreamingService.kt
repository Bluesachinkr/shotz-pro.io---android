package com.android.shotz_pro_io.stream

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.android.shotz_pro_io.FFMPEG

class StreamingService : Service() {
    companion object {
        lateinit var mContext: StreamingService
        val AUDIO_SAMPLE_RATE = 44100
    }

    private val binder = LocalBinder()
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
                        val encodedSize = FFMPEG.encodeVideoFrame(result)
                    }
                }
            }
        })
        activity.setAudioFrameCallback(object : StreamingActivity.AudioFrameCallback {
            override fun onHandleFrame(data: ShortArray, length: Int) {
                if (encoding) {
                    synchronized(frame_mutex) {
                        val encodedSize = FFMPEG.encodeAudioFrame(length, data)
                    }
                }
            }
        })
        synchronized(frame_mutex) {
            activity.startVideoStream()
            activity.startAudioStream(AUDIO_SAMPLE_RATE)
            FFMPEG.init(AUDIO_SAMPLE_RATE, streamUrl)
        }
    }

    open fun stopStreaming() {
        activity.stopVideoStream()
        activity.stopAudioStream()
        encoding = false
        isStreaming = false
        if (encoding) {
            FFMPEG.shutDown()
        }
    }

    private class LocalBinder : Binder() {
        fun getService(): StreamingService {
            return mContext
        }
    }
}