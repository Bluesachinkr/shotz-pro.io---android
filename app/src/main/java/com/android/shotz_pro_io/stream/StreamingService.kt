package com.android.shotz_pro_io.stream

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import net.butterflytv.rtmp_client.RTMPMuxer

class StreamingService : Service() {
    companion object {
        lateinit var mContext: StreamingService
        lateinit var muxer : RTMPMuxer()
        val AUDIO_SAMPLE_RATE = 44100
    }

     val binder = LocalBinder()
    val activity = StreamingActivity.getInstance()
    private val frame_mutex = Object()
    private var encoding = false
    open var isStreaming = false

    override fun onBind(p0: Intent?): IBinder? {
        mContext = this
        muxer = RTMPMuxer()
        return binder
    }

    open fun startStreaming(streamUrl: String) {
        encoding = true
        isStreaming = true
        activity.setVideoFrameCallback(object : StreamingActivity.VideoFrameCallback {
            override fun onHandleFrame(result: ByteArray) {
                if (encoding) {
                    synchronized(frame_mutex) {
                        val encodedSize = muxer.writeVideo(result,0,result.size,)
                    }
                }
            }
        })
        activity.setAudioFrameCallback(object : StreamingActivity.AudioFrameCallback {
            override fun onHandleFrame(data: ShortArray, length: Int) {
                if (encoding) {
                    synchronized(frame_mutex) {
                        val encodedSize = Ffmpeg.encodeAudioFrame(length, data)
                    }
                }
            }
        })
        synchronized(frame_mutex) {
            activity.startVideoStream()
            activity.startAudioStream(AUDIO_SAMPLE_RATE)
            Ffmpeg.init(1280,720,AUDIO_SAMPLE_RATE, streamUrl)

        }
    }

    open fun stopStreaming() {
        activity.stopVideoStream()
        activity.stopAudioStream()
        encoding = false
        isStreaming = false
        if (encoding) {
            Ffmpeg.shutDown()
        }
    }

    class LocalBinder : Binder() {
        fun getService(): StreamingService {
            return mContext
        }
    }
}