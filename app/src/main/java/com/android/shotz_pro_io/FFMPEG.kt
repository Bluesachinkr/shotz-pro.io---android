package com.android.shotz_pro_io

class FFMPEG {

    companion object {

        init {
            System.loadLibrary("ffmpeg")
        }

        external fun init(audioSampleRate: Int, rtmpUrl: String): Boolean

        external fun shutDown()

        external fun encodeVideoFrame(yuvImage: Array<Byte>): Int

        external fun encodeAudioFrame(length: Int, audioData: Array<Short>): Int
    }
}