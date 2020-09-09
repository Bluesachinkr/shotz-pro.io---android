package com.android.shotz_pro_io.rtmp

import android.media.projection.MediaProjection
import android.text.style.LineHeightSpan
import androidx.annotation.NonNull
import java.lang.IllegalStateException
import java.lang.RuntimeException

interface Publisher {
    fun startPublishing()
    fun stopPublishing()
    fun isPublishing() : Boolean?

    class Builder{
        companion object {
            val DEFAULT_WIDTH = 720
            val DEFAULT_HEIGHT = 1280

            val DEFAULT_WIDTH_LAND = 1280
            val DEFAULT_HEIGHT_LAND = 720

            val DEFAULT_AUDIO_BITRATE = 64000
            val DEFAULT_VIDEO_BITRATE = 4000 * 1024
            val DEFAULT_DENSITY = 300
        }

        private var url : String? = null
        private var width : Int = 0
        private var height : Int = 0
        private var audioBitrate : Int = 0
        private var videoBitrate : Int = 0
        private var density : Int = 0
        private var publisherListener : PublisherListener? = null
        private var mediaProjection : MediaProjection? = null

        fun setUrl(@NonNull url : String):Builder{
            this.url = url
            return this
        }

        fun setWidth(width : Int) : Builder{
            this.width = width
            return this
        }

        fun setHeight(height : Int) : Builder{
            this.height = height
            return this
        }

        fun setAudioBitrate(audioBitrate : Int) : Builder{
            this.audioBitrate = audioBitrate
            return this
        }

        fun setVideoBitrate(videoBitrate : Int): Builder{
            this.videoBitrate = videoBitrate
            return this
        }

        fun setScreenDensity(screenDensity: Int) : Builder{
            this.density = screenDensity
            return this
        }

        fun setPublisherListener(listener: PublisherListener) : Builder{
            this.publisherListener = listener
            return this
        }

        fun setMediaProjection(@NonNull projection: MediaProjection) : Builder{
            this.mediaProjection = projection
            return this
        }

        fun build() : RtmpPublisher{
            url?.let {
                if(it.isEmpty()){
                    throw IllegalStateException("url should not be empty")
                }
            }
            if(height <=0){
                height = DEFAULT_HEIGHT_LAND
            }
            if(width<=0){
                width = DEFAULT_WIDTH_LAND
            }
            if(audioBitrate <=0){
                audioBitrate = DEFAULT_AUDIO_BITRATE
            }
            if(videoBitrate<=0){
                videoBitrate = DEFAULT_VIDEO_BITRATE
            }
            if(density <= 0){
                density = DEFAULT_DENSITY
            }
            if(mediaProjection == null){
                throw RuntimeException("Media projection is cancelled")
            }
            return RtmpPublisher(url,width,height,audioBitrate,videoBitrate,density,publisherListener,mediaProjection!!)
        }
    }
}