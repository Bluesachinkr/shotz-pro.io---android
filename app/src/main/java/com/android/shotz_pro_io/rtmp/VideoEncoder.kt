package com.android.shotz_pro_io.rtmp

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import java.io.IOException

class VideoEncoder : Encoder {

    // H.264 Advanced Video Coding
    private val MIME_TYPE = "video/avc"

    // 5 seconds between I-frames
    private val IFRAME_INTERVAL = 5

    private var isEncoding = false
    private val TIMEOUT_USEC = 10000

    private var inputSurface: Surface? = null
    private var encoder: MediaCodec? = null
    private var bufferInfo: MediaCodec.BufferInfo? = null
    private var listener: VideoHandler.OnVideoEncoderStateListener? = null
    private var lastFrameEncodedAt: Long = 0
    private var startStreamingAt: Long = 0

    fun setOnVideoEncoderStateListener(listener: VideoHandler.OnVideoEncoderStateListener?) {
        this.listener = listener
    }

    fun getLastFrameEncodedAt(): Long {
        return lastFrameEncodedAt
    }

    fun getInputSurface(): Surface? {
        return inputSurface
    }

    /**
     * prepare the Encoder. call this before start the encoder.
     */
    @Throws(IOException::class)
    fun prepare(width: Int, height: Int, bitRate: Int, frameRate: Int, startStreamingAt: Long) {
        this.startStreamingAt = startStreamingAt
        bufferInfo = MediaCodec.BufferInfo()
        val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height)
        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL)
        encoder = MediaCodec.createEncoderByType(MIME_TYPE)
        encoder!!.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = encoder!!.createInputSurface()
    }

    override fun start() {
        encoder!!.start()
        isEncoding = true
        drain()
    }

    override fun stop() {
        if (isEncoding()) {
            encoder!!.signalEndOfInputStream()
        }
    }

    override fun isEncoding(): Boolean {
        return encoder != null && isEncoding
    }

    fun drain() {
        val handlerThread = HandlerThread("VideoEncoder-drain")
        handlerThread.start()
        val handler = Handler(handlerThread.looper)
        handler.post(Runnable { // keep running... so use a different thread.
            while (isEncoding) {
                if (encoder == null) return@Runnable
                val encoderOutputBuffers = encoder!!.outputBuffers
                val inputBufferId =
                    encoder!!.dequeueOutputBuffer(bufferInfo!!, TIMEOUT_USEC.toLong())
                if (inputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newFormat = encoder!!.outputFormat
                    val sps = newFormat.getByteBuffer("csd-0")
                    val pps = newFormat.getByteBuffer("csd-1")
                    val config = ByteArray(sps!!.limit() + pps!!.limit())
                    sps[config, 0, sps.limit()]
                    pps[config, sps.limit(), pps.limit()]
                    listener?.onVideoDataEncoded(config, config.size, 0)
                } else {
                    if (inputBufferId > 0) {
                        val encodedData = encoderOutputBuffers[inputBufferId]
                            ?: continue
                        if (bufferInfo!!.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            bufferInfo!!.size = 0
                        }
                        if (bufferInfo!!.size != 0) {
                            encodedData.position(bufferInfo!!.offset)
                            encodedData.limit(bufferInfo!!.offset + bufferInfo!!.size)
                            val currentTime = System.currentTimeMillis()
                            val timestamp = (currentTime - startStreamingAt).toInt()
                            val data = ByteArray(bufferInfo!!.size)
                            encodedData[data, 0, bufferInfo!!.size]
                            encodedData.position(bufferInfo!!.offset)
                            listener?.onVideoDataEncoded(data, bufferInfo!!.size, timestamp)
                            lastFrameEncodedAt = currentTime
                        }
                        encoder!!.releaseOutputBuffer(inputBufferId, false)
                    } else if (inputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        continue
                    }
                    if (bufferInfo!!.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        break
                    }
                }
            }
            release()
        })
    }

    private fun release() {
        if (encoder != null) {
            isEncoding = false
            encoder!!.stop()
            encoder!!.release()
            encoder = null
        }
    }
}