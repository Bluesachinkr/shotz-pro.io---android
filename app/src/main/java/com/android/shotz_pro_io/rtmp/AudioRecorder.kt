package com.android.shotz_pro_io.rtmp

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread

internal class AudioRecorder(private val sampleRate: Int) {
    private var audioRecord: AudioRecord? = null
    private var listener: OnAudioRecorderStateChangedListener? = null

    internal interface OnAudioRecorderStateChangedListener {
        fun onAudioRecorded(data: ByteArray?, length: Int)
    }

    fun setOnAudioRecorderStateChangedListener(listener: OnAudioRecorderStateChangedListener?) {
        this.listener = listener
    }

    fun start() {
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize
        )
        audioRecord!!.startRecording()
        val handlerThread = HandlerThread("AudioRecorder-record")
        handlerThread.start()
        val handler = Handler(handlerThread.looper)
        handler.post {
            var bufferReadResult: Int = 0
            val data = ByteArray(bufferSize)
            // keep running... so use a different thread.
            while (isRecording && audioRecord!!.read(data, 0, bufferSize).also {
                    bufferReadResult = it
                } > 0) {
                listener!!.onAudioRecorded(data, bufferReadResult)
            }
        }
    }

    fun stop() {
        if (isRecording) {
            audioRecord!!.stop()
            audioRecord!!.release()
            audioRecord = null
        }
    }

    val isRecording: Boolean
        get() = (audioRecord != null
                && audioRecord!!.recordingState == AudioRecord.RECORDSTATE_RECORDING)

}
