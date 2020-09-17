package com.android.shotz_pro_io.controllers.rtmp

import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import java.io.IOException
import java.nio.ByteBuffer

internal class VideoEncoder(mediaProjection: MediaProjection) : Encoder {

    private val mediaProjection = mediaProjection

    private var isEncoding = false
    private var inputSurface: Surface? = null
    private var encoder: MediaCodec? = null
    private var bufferInfo: MediaCodec.BufferInfo? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mDensity: Int = 0
    private var mWidth: Int = 0
    private var mHeight: Int = 0
    private var mBitrate: Int = 0
    private var mFrameRate: Int = 0
    private var listener: VideoHandler.OnVideoEncoderStateListener? = null
    private var lastFrameEncodedAt: Long = 0
    private var startStreamingAt: Long = 0

    companion object {
        private const val MIME_TYPE = "video/avc"
        private const val IFRAME_INTERVAL = 5
        private const val TIMEOUT_USEC = 10000
    }

    fun setOnVideoEncoderStateListener(listener: VideoHandler.OnVideoEncoderStateListener?) {
        this.listener = listener
    }

    fun getLastFrameEncodedAt(): Long {
        return lastFrameEncodedAt
    }

    fun getInputSurface(): Surface? {
        return inputSurface
    }

    @Throws(IOException::class)
    fun prepare(
        width: Int,
        height: Int,
        bitRate: Int,
        frameRate: Int,
        startStreamingAt: Long, density: Int
    ) {
        this.startStreamingAt = startStreamingAt
        this.mWidth = width
        this.mHeight = height
        this.mBitrate = bitRate
        this.mFrameRate = frameRate
        this.mDensity = density
        this.bufferInfo = MediaCodec.BufferInfo()
        val format = MediaFormat.createVideoFormat(MIME_TYPE,1280,720)
        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitrate)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL)

        encoder = MediaCodec.createEncoderByType(MIME_TYPE)
        encoder?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = encoder?.createInputSurface()
        inputSurface?.let {
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "Capturing Display",
               1280,
                720,
                mDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                it,
                null,
                null
            )
        }
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
                val encoderOutputBuffers: Array<ByteBuffer> = encoder!!.outputBuffers
                val inputBufferId =
                    encoder!!.dequeueOutputBuffer(bufferInfo!!, TIMEOUT_USEC.toLong())
                if (inputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newFormat = encoder!!.outputFormat

                    val sps: ByteBuffer? = newFormat.getByteBuffer("csd-0")
                    val pps: ByteBuffer? = newFormat.getByteBuffer("csd-1")
                    if (sps != null && pps != null) {
                        val config = ByteArray(sps.limit() + pps.limit())
                        sps.get(config, 0, sps.limit())
                        pps.get(config, sps.limit(), pps.limit())
                        println("Streaming")
                        listener!!.onVideoDataEncoded(config, config.size, 0)
                    }
                } else {
                    if (inputBufferId > 0) {
                        val encodedData: ByteBuffer =
                            encoderOutputBuffers[inputBufferId] ?: continue
                        if (bufferInfo!!.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            bufferInfo!!.size = 0
                        }
                        if (bufferInfo!!.size != 0) {
                            encodedData.position(bufferInfo!!.offset)
                            encodedData.limit(bufferInfo!!.offset + bufferInfo!!.size)
                            val currentTime = System.currentTimeMillis()
                            val timestamp = (currentTime - startStreamingAt).toInt()
                            val data = ByteArray(bufferInfo!!.size)
                            encodedData.get(data, 0, bufferInfo!!.size)
                            encodedData.position(bufferInfo!!.offset)
                            println("Streaming")
                            listener!!.onVideoDataEncoded(data, bufferInfo!!.size, timestamp)
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
            encoder?.stop()
            encoder?.release()
            encoder = null
        }
    }
}