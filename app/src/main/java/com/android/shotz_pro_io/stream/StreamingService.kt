package com.android.shotz_pro_io.stream

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.DisplayMetrics
import android.view.Display
import androidx.annotation.NonNull
import com.android.shotz_pro_io.main.MainActivity
import com.android.shotz_pro_io.controllers.rtmp.Publisher
import com.android.shotz_pro_io.controllers.rtmp.PublisherListener
import com.android.shotz_pro_io.controllers.rtmp.utils.StreamProfile
import java.lang.Exception

class StreamingService : BaseService(), PublisherListener {
    companion object {
        var mContext: StreamingService? = null
        val KEY_NOTIFY_MSG = "stream service notify"
        val NOTIFY_MSG_CONNECTION_FAILED = "Stream connection failed"
        val NOTIFY_MSG_CONNECTION_STARTED = "Stream started"
        val NOTIFY_MSG_ERROR = "Stream connection error!"
        val NOTIFY_MSG_UPDATED_STREAM_PROFILE = "Updated stream profile"
        val NOTIFY_MSG_CONNECTION_DISCONNECTED = "Connection disconnected!"
        val NOTIFY_MSG_STREAM_STOPPED = "Stream stopped"
        val NOTIFY_MSG_REQUEST_START = "Request start stream"
        val NOTIFY_MSG_REQUEST_STOP = "Request stop stream"
        val AUDIO_SAMPLE_RATE = 44100

    }

    val binder = LocalBinder()
    private val TAG = "Streaming Service"
    private val frame_mutex = Object()
    open var isStreaming = false

    private var mediaProjection: MediaProjection? = null
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mPublisher: Publisher? = null
    private var mScreenCaptureIntent: Intent? = null
    private var mScreenCaptureResultCode = 0
    private var mScreenWidth: Int = 0
    private var mScreenHeight: Int = 0
    private var mScreenDensity: Int = 0

    override fun onCreate() {
        super.onCreate()
        mContext = this
        mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        
    }

    fun getScreenSize() {
        val displayMetrics = DisplayMetrics()
        mScreenDensity = displayMetrics.density.toInt()
        var w = displayMetrics.widthPixels
        var h = displayMetrics.heightPixels
        if (w > h) {
            val s_x = w / 1920f
            val s_y = h / 1080f
            val s = Math.max(s_x, s_y)
            w = (w / s).toInt()
            h = (h / s).toInt()
        } else {
            val s_x = w / 1080f
            val s_y = h / 1920f
            val s = Math.max(s_x, s_y)
            w = (w / s).toInt()
            h = (h / s).toInt()
        }

        if (w > h) {
            mScreenWidth = w
            mScreenHeight = h
        } else {
            mScreenWidth = h
            mScreenHeight = w
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        mScreenCaptureIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT)
        mScreenCaptureIntent?.let {
            mScreenCaptureResultCode = it.getIntExtra(
                "SCREEN_CAPTURE_INTENT_RESULT_CODE",
                -999999
            )
        }

        getScreenSize()
        mScreenCaptureIntent?.let {
            mediaProjection = mediaProjectionManager?.getMediaProjection(
                mScreenCaptureResultCode,
                it
            )
        }
        val dm = getSystemService(DISPLAY_SERVICE) as DisplayManager
        val defaultDisplay: Display?
        defaultDisplay = if (dm != null) {
            dm.getDisplay(Display.DEFAULT_DISPLAY)
        } else {
            throw IllegalStateException("Cannot display manager?!?")
        }
        if (defaultDisplay == null) {
            throw RuntimeException("No display found.")
        }

        return binder
    }

    open fun startStreaming(streamUrl: String,broadcastId : String) {
        synchronized(frame_mutex) {
            if (mPublisher == null) {
                try {
                    mediaProjection?.let {
                       createPublisher(streamUrl,it)
                    }
                       mPublisher?.let {
                        it.startPublishing()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                mPublisher?.startPublishing()
            }
        }
    }

    open fun stopStreaming() {
        mPublisher?.let {
            if (it.isPublishing()!!) {
                it.stopPublishing()
                MainActivity.mContext?.endEvent(StreamProfile.broadcastKey)
            }
        }
    }

    fun createPublisher(streamUrl: String,@NonNull mediaProjection: MediaProjection){
        mPublisher = Publisher.Builder().setUrl(streamUrl)
            .setWidth(1280)
            .setHeight(720)
            .setAudioBitrate(Publisher.Builder.DEFAULT_AUDIO_BITRATE)
            .setVideoBitrate(Publisher.Builder.DEFAULT_VIDEO_BITRATE)
            .setMediaProjection(mediaProjection)
            .setPublisherListener(this)
            .build()
    }

    inner class LocalBinder : Binder() {
        fun getService(): StreamingService {
            return this@StreamingService
        }
    }

    override fun startPerformService(streamUrl: String,broadcastId: String) {
        startStreaming(streamUrl,broadcastId)
    }

    override fun stopPerformService() {

    }

    override fun onStarted() {
    }

    override fun onStopped() {
    }

    override fun onDisconnected() {
    }

    override fun onFailedToConnect() {
    }
}