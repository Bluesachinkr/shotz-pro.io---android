package com.android.shotz_pro_io.controllers.rtmp

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import net.butterflytv.rtmp_client.RTMPMuxer

internal class Muxer {
    private val handler: Handler
    private val rtmpMuxer = RTMPMuxer()
    private var listener: PublisherListener? = null
    private var disconnected = false
    private var closed = false
    fun setOnMuxerStateListener(listener: PublisherListener?) {
        this.listener = listener
    }

    fun open(url: String?, width: Int, height: Int) {
        val message = handler.obtainMessage(MSG_OPEN, url)
        message.arg1 = width
        message.arg2 = height
        handler.sendMessage(message)
    }

    fun sendVideo(data: ByteArray?, length: Int, timestamp: Int) {
        val message =
            handler.obtainMessage(MSG_SEND_VIDEO, data)
        message.arg1 = length
        message.arg2 = timestamp
        handler.sendMessage(message)
    }

    fun sendAudio(data: ByteArray?, length: Int, timestamp: Int) {
        val message =
            handler.obtainMessage(MSG_SEND_AUDIO, data)
        message.arg1 = length
        message.arg2 = timestamp
        handler.sendMessage(message)
    }

    fun close() {
        handler.sendEmptyMessage(MSG_CLOSE)
    }

    val isConnected: Boolean
        get() = rtmpMuxer.isConnected == 1

    companion object {
        private const val MSG_OPEN = 0
        private const val MSG_CLOSE = 1
        private const val MSG_SEND_VIDEO = 2
        private const val MSG_SEND_AUDIO = 3
    }

    init {
        val uiHandler = Handler(Looper.getMainLooper())
        val handlerThread = HandlerThread("Muxer")
        handlerThread.start()
        handler = Handler(handlerThread.looper) { msg ->
            when (msg.what) {
                MSG_OPEN -> {
                    rtmpMuxer.open(msg.obj as String, msg.arg1, msg.arg2)
                    if (listener != null) {
                        uiHandler.post {
                            if (isConnected) {
                                listener?.onStarted()
                                disconnected = false
                                closed = false
                            } else {
                                listener?.onFailedToConnect()
                            }
                        }
                    }
                }
                MSG_CLOSE -> {
                    rtmpMuxer.close()
                    if (listener != null) {
                        uiHandler.post {
                            listener?.onStopped()
                            closed = true
                        }
                    }
                }
                MSG_SEND_VIDEO -> {
                    if (isConnected) {
                        rtmpMuxer.writeVideo(msg.obj as ByteArray, 0, msg.arg1, msg.arg2)
                    } else {
                        if (listener != null) {
                            uiHandler.post(Runnable {
                                if (closed || disconnected) return@Runnable
                                listener?.onDisconnected()
                                disconnected = true
                            })
                        }
                    }
                }
                MSG_SEND_AUDIO -> {
                    if (isConnected) {
                        rtmpMuxer.writeAudio(msg.obj as ByteArray, 0, msg.arg1, msg.arg2)
                    } else {
                        if (listener != null) {
                            uiHandler.post(Runnable {
                                if (closed || disconnected) return@Runnable
                                listener?.onDisconnected()
                                disconnected = true
                            })
                        }
                    }
                }
            }
            false
        }
    }
}