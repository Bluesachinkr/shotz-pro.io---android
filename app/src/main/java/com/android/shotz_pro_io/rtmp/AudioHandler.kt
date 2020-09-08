package com.android.shotz_pro_io.rtmp

import android.os.Handler
import android.os.HandlerThread

class AudioHandler : AudioRecorder.OnAudioRecorderStateChangedListener {

    private val handler: Handler
    private val audioEncoder: AudioEncoder
    private val audioRecorder: AudioRecorder

    interface OnAudioEncoderStateListener {
        fun onAudioDataEncoded(data: ByteArray?, size: Int, timestamp: Int)
    }

    fun setOnAudioEncoderStateListener(listener: OnAudioEncoderStateListener?) {
        audioEncoder.setOnAudioEncoderStateListener(listener)
    }

    fun start(bitrate: Int, startStreamingAt: Long) {
        handler.post {
            audioEncoder.prepare(
                bitrate,
                SAMPLE_RATE,
                startStreamingAt
            )
            audioEncoder.start()
            audioRecorder.start()
        }
    }

    fun stop() {
        handler.post {
            if (audioRecorder.isRecording) {
                audioRecorder.stop()
            }
            if (audioEncoder.isEncoding()) {
                audioEncoder.stop()
            }
        }
    }

    companion object {
        private const val SAMPLE_RATE = 44100
    }

    init {
        audioEncoder  = AudioEncoder()
        audioRecorder = AudioRecorder(SAMPLE_RATE)
        audioRecorder.setOnAudioRecorderStateChangedListener(this)
        val handlerThread = HandlerThread("VideoHandler")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }

    override fun onAudioRecorded(data: ByteArray?, length: Int) {
        handler.post { audioEncoder.enqueueData(data, length) }
    }
}