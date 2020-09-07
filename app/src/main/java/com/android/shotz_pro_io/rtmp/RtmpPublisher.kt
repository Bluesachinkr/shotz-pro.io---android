package com.android.shotz_pro_io.rtmp

import android.opengl.EGL14
import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent

class RtmpPublisher internal constructor(
    activity: AppCompatActivity,
    url: String,
    width: Int,
    height: Int,
    audioBitrate: Int,
    videoBitrate: Int,
    listener: PublisherListener?
) : LifecycleObserver,Publisher.Publisher {
    private val streamer: Streamer
    private val url: String
    private val width: Int
    private val height: Int
    private val audioBitrate: Int
    private val videoBitrate: Int
    override fun startPublishing() {
        streamer.open(url, width, height)
        val context = EGL14.eglGetCurrentContext()
        streamer.startStreaming(context, width, height, audioBitrate, videoBitrate)
    }

    override fun stopPublishing() {
        if (streamer.isStreaming) {
            streamer.stopStreaming()
        }
    }

    override val isPublishing: Boolean
        get() = streamer.isStreaming


    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause(owner: LifecycleOwner?) {
        if (streamer.isStreaming) {
            streamer.stopStreaming()
        }
    }

    init {
        activity.lifecycle.addObserver(this)
        this.url = url
        this.width = width
        this.height = height
        this.audioBitrate = audioBitrate
        this.videoBitrate = videoBitrate
        streamer = Streamer()
        streamer.setMuxerListener(listener)
    }
}