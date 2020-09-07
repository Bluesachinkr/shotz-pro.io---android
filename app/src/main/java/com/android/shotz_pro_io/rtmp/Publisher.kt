package com.android.shotz_pro_io.rtmp

import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity

open interface Publisher{


    interface Publisher {
        fun startPublishing()
        fun stopPublishing()
        val isPublishing: Boolean

        class Builder(activity: AppCompatActivity) {
            private val activity: AppCompatActivity?
            private var url: String? = null

            private var width = 0
            private var height = 0
            private var audioBitrate = 0
            private var videoBitrate = 0
            private var listener: PublisherListener? = null

            /**
             * Set the RTMP url
             * this parameter is required
             */
            fun setUrl(url: String): Builder {
                this.url = url
                return this
            }

            fun setSize(width: Int, height: Int): Builder {
                this.width = width
                this.height = height
                return this
            }

            fun setAudioBitrate(audioBitrate: Int): Builder {
                this.audioBitrate = audioBitrate
                return this
            }

            fun setVideoBitrate(videoBitrate: Int): Builder {
                this.videoBitrate = videoBitrate
                return this
            }

            fun setListener(listener: PublisherListener?): Builder {
                this.listener = listener
                return this
            }

            fun build(): RtmpPublisher {
                checkNotNull(activity) { "activity should not be null" }
                check(!(url == null || url!!.isEmpty())) { "url should not be empty or null" }
                if (url == null || height <= 0) {
                    height = DEFAULT_HEIGHT
                }
                if (url == null || width <= 0) {
                    width = DEFAULT_WIDTH
                }
                if (url == null || audioBitrate <= 0) {
                    audioBitrate = DEFAULT_AUDIO_BITRATE
                }
                if (url == null || videoBitrate <= 0) {
                    videoBitrate = DEFAULT_VIDEO_BITRATE
                }

                return RtmpPublisher(
                    activity,
                    url.toString(),
                    width,
                    height,
                    audioBitrate,
                    videoBitrate,
                    listener
                )
            }

            companion object {
                const val DEFAULT_WIDTH = 720
                const val DEFAULT_HEIGHT = 1280
                const val DEFAULT_AUDIO_BITRATE = 6400
                const val DEFAULT_VIDEO_BITRATE = 100000
            }

            init {
                this.activity = activity
            }
        }
    }

}