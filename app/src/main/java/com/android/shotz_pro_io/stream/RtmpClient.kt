package com.android.shotz_pro_io.stream

import com.android.shotz_pro_io.rtmp.AudioHandler

class RtmpClient {

    companion object {
        var streamingStartAt : Long = 0
        var listenerVideo: VideoHandler.OnVideoEncoderStateListener? = null
        var listenerAudio: AudioHandler.OnAudioEncoderStateListener? = null

        fun setAudioListener(listener: AudioHandler.OnAudioEncoderStateListener) {
            this.listenerAudio = listener
        }

        fun setVideoListener(listener: VideoHandler.OnVideoEncoderStateListener) {
            this.listenerVideo = listener
        }
    }
}