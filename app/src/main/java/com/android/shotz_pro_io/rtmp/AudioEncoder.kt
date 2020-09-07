package com.android.shotz_pro_io.rtmp

import android.media.*
import android.os.Handler
import android.os.HandlerThread
import java.io.IOException
import java.nio.ByteBuffer

internal class AudioEncoder : Encoder {
    private val TIMEOUT_USEC = 10000
    private var inputBuffers: Array<ByteBuffer> = emptyArray()
    private var outputBuffers: Array<ByteBuffer> = emptyArray()
    private var encoder: MediaCodec? = null
    private var startedEncodingAt: Long = 0
    private var isEncoding = false
    private var listener: AudioHandler.OnAudioEncoderStateListener? = null
    fun setOnAudioEncoderStateListener(listener: AudioHandler.OnAudioEncoderStateListener?) {
        this.listener = listener
    }

    /**
     * prepare the Encoder. call this before start the encoder.
     */
    fun prepare(bitrate: Int, sampleRate: Int, startStreamingAt: Long) {
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val audioFormat = MediaFormat.createAudioFormat(
            AUDIO_MIME_TYPE,
            sampleRate,
            CHANNEL_COUNT
        )
        audioFormat.setInteger(
            MediaFormat.KEY_AAC_PROFILE,
            MediaCodecInfo.CodecProfileLevel.AACObjectLC
        )
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
        audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize)
        startedEncodingAt = startStreamingAt
        try {
            encoder =
                MediaCodec.createEncoderByType(AUDIO_MIME_TYPE)
            encoder!!.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    override fun start() {
        encoder!!.start()
        inputBuffers = encoder!!.inputBuffers
        outputBuffers = encoder!!.outputBuffers
        isEncoding = true
        drain()
    }

    override fun stop() {
        if (isEncoding) {
            val inputBufferId = encoder!!.dequeueInputBuffer(TIMEOUT_USEC.toLong())
            encoder!!.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        }
    }

    override fun isEncoding(): Boolean {
        return encoder != null && isEncoding
    }

    /**
     * enqueue recorded dada from [AudioRecord]
     * the data will be drained from [AudioEncoder.drain]
     */
    fun enqueueData(data: ByteArray?, length: Int) {
        if (encoder == null) return
        val bufferRemaining: Int
        val timestamp = System.currentTimeMillis() - startedEncodingAt
        val inputBufferId = encoder!!.dequeueInputBuffer(TIMEOUT_USEC.toLong())
        if (inputBufferId >= 0) {
            val inputBuf = inputBuffers[inputBufferId]
            inputBuf.clear()
            bufferRemaining = inputBuf.remaining()
            if (bufferRemaining < length) {
                inputBuf.put(data, 0, bufferRemaining)
            } else {
                inputBuf.put(data, 0, length)
            }
            encoder!!.queueInputBuffer(inputBufferId, 0, inputBuf.position(), timestamp * 1000, 0)
        }
    }

    /**
     * drain data from [MediaCodec].
     * keep draining inside until it stops encoding.
     * so it would be good to use another thread for this method.
     */
    private fun drain() {
        val handlerThread = HandlerThread("AudioEncoder-drain")
        handlerThread.start()
        val handler = Handler(handlerThread.looper)
        handler.post {
            val bufferInfo = MediaCodec.BufferInfo()
            // keep running... so use a different thread.
            while (isEncoding) {
                val outputBufferId =
                    encoder!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC.toLong())
                if (outputBufferId >= 0) {
                    val encodedData = outputBuffers[outputBufferId]
                        ?: continue
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    val data = ByteArray(bufferInfo.size)
                    encodedData[data, 0, bufferInfo.size]
                    encodedData.position(bufferInfo.offset)
                    val currentTime = System.currentTimeMillis()
                    val timestamp = (currentTime - startedEncodingAt).toInt()
                    listener?.onAudioDataEncoded(data, bufferInfo.size, timestamp)
                    encoder!!.releaseOutputBuffer(outputBufferId, false)
                } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // format should not be changed
                }
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    //end of stream
                    break
                }
            }
            release()
        }
    }

    private fun release() {
        if (encoder != null) {
            isEncoding = false
            encoder!!.stop()
            encoder!!.release()
            encoder = null
        }
    }

    companion object {
        private const val AUDIO_MIME_TYPE = "audio/mp4a-latm"
        private const val CHANNEL_COUNT = 1
    }
}