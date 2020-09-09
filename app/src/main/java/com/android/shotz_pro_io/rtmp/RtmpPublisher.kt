package com.android.shotz_pro_io.rtmp

import android.media.projection.MediaProjection

class RtmpPublisher() : Publisher {
    private val TAG = RtmpPublisher::class.java.simpleName

    private var streamer: Streamer? = null

    private var url: String? = null
    private var width = 0
    private var height = 0
    private var audioBitrate = 0
    private var videoBitrate = 0
    private var density = 0

    constructor(
        url: String?,
        width: Int,
        height: Int,
        audioBitrate: Int,
        videoBitrate: Int,
        density: Int,
        listener: PublisherListener?,
        mediaProjection: MediaProjection
    ) : this() {
        this.url = url
        this.width = width
        this.height = height
        this.audioBitrate = audioBitrate
        this.videoBitrate = videoBitrate
        this.density = density
        streamer = Streamer(mediaProjection)
        streamer?.setMuxerListener(listener)
    }

    override fun startPublishing() {
        url?.let {
            streamer?.open(it, width, height)
        }
        streamer?.startStreaming(width, height, audioBitrate, videoBitrate, density)
    }

    override fun stopPublishing() {
        streamer?.let {
            if(it.isStreaming()){
                it.stopStreaming()
            }
        }
    }

    override fun isPublishing(): Boolean? {
        return streamer?.isStreaming()
    }
}