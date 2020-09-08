package com.android.shotz_pro_io.stream

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.opengl.EGLContext
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.shotz_pro_io.R
import com.android.shotz_pro_io.rtmp.AudioHandler
import com.android.shotz_pro_io.rtmp.Muxer
import com.android.shotz_pro_io.rtmp.VideoEncoder
import com.google.api.services.youtube.YouTube
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer

class StreamingActivity() : AppCompatActivity(),
    AudioHandler.OnAudioEncoderStateListener {

    private lateinit var mediaProjectionPermissionResultData: Intent
    private lateinit var mediaProjectionCallback: MediaProjection.Callback
    private lateinit var youTube: YouTube

    constructor(
        mediaProjectionPermissionResultData: Intent,
        mediaProjectionCallback: MediaProjection.Callback, youTube: YouTube
    ) : this() {
        this.youTube = youTube
    }


    private val STREAM_REQUEST_CODE = 201
    private val PERMISSION_CODE = 202
    private val permissios = Array<String>(1, { android.Manifest.permission.RECORD_AUDIO })


    private var frequency = 0
    private var isDisposed: Boolean = false
    private var cancel = false
    private lateinit var broadcastId: String
    private lateinit var rtmpUrl: String

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var imageReader: ImageReader

    private var streamerService: StreamingService? = null
    private var mediaProjection: MediaProjection? = null
    private var muxer: Muxer

    //Audio
    private var audioHandler: AudioHandler

    //Video
    private val handler: Handler
    private val videoEncoder: VideoEncoder

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            streamerService = StreamingService.LocalBinder().getService()
            startStreaming()
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            streamerService = null
        }

    }

    companion object {
        private const val FRAME_RATE = 30
        var DISPLAY_WIDTH = 720
        var DISPLAY_HEIGHT = 1280
        var mScreenDensity: Int = 0
        var virtualDisplay : VirtualDisplay? = null
        private lateinit var context: StreamingActivity
        fun getInstance(): StreamingActivity {
            return context
        }
    }

    init {
        this.audioHandler = AudioHandler()
        this.videoEncoder = VideoEncoder()
        val handlerThread = HandlerThread("VideoHandler")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        this.muxer = Muxer()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //set context and intent
        context = this
        intent?.let {
            broadcastId = it.getStringExtra(YoutubeApi.BROADCAST_ID_KEY) as String
            rtmpUrl = it.getStringExtra(YoutubeApi.RTMP_URL_KEY) as String
        }
        if (rtmpUrl == null) {
            finish()
        }
        //end getIntent

        setContentView(R.layout.activity_streaming)
        this.mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        imageReader =
            ImageReader.newInstance(DISPLAY_WIDTH, DISPLAY_HEIGHT, ImageFormat.YUV_420_888, 5)
        mScreenDensity = resources.displayMetrics.densityDpi

        if (!havePermissions()) {
            ActivityCompat.requestPermissions(this, permissios, PERMISSION_CODE)
        }

        if (!bindService(
                Intent(this, StreamingService::class.java), serviceConnection,
                BIND_AUTO_CREATE or BIND_DEBUG_UNBIND
            )
        ) {
            Toast.makeText(this, "Failed to bind streamer services", Toast.LENGTH_LONG).show()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceConnection != null) {
            unbindService(serviceConnection)
        }
        stopStreaming()
    }

    private fun stopStreaming() {
        streamerService?.let {
            if (it.isStreaming) {
                it.stopStreaming()
            }
        }
    }

    private fun startStreaming() {
        streamerService?.let {
            if (!it.isStreaming) {
                if (havePermissions()) {
                    it.startStreaming(rtmpUrl)
                    val transition =
                        youTube.LiveBroadcasts().transition("live", broadcastId, "status")
                    transition.execute()
                }
            }
        }
    }

    private fun havePermissions(): Boolean {
        for (permission in permissios) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    permission
                ) == PackageManager.PERMISSION_DENIED
            ) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) ==
                PackageManager.PERMISSION_DENIED
            ) {
                finish()
            }
        }
        streamerService?.let {
            it.startStreaming(rtmpUrl)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != STREAM_REQUEST_CODE) {
            return
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "Permission denied for screen capturing", Toast.LENGTH_SHORT)
                .show()
        }
        data?.let {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, it)
            mediaProjection?.registerCallback(mediaProjectionCallback, null)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    open fun startVideoStream() {
        if (mediaProjection == null) {
            startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                STREAM_REQUEST_CODE
            )
        }
    }

    open fun stopVideoStream() {
        stop()
        virtualDisplay?.release()
        virtualDisplay = null
    }


    fun start(
        width: Int, height: Int, bitRate: Int, startStreamingAt: Long
    ) {
        handler.post {
            try {
                videoEncoder.prepare(
                    width,
                    height,
                    bitRate,
                    FRAME_RATE,
                    startStreamingAt,mediaProjection!!
                )
                videoEncoder.start()

            } catch (ioe: IOException) {
                throw RuntimeException(ioe)
            }
        }
    }

    fun stop() {
        handler?.post {
            videoEncoder?.let {
                if (it.isEncoding()) {
                    it.stop()
                }
            }
        }
    }

    private val frameInterval: Long
        private get() = (1000 / FRAME_RATE).toLong()

    /*
    *  ********** AUDIO STREAMING ****************************
    */
    open fun startAudioStream(frequency: Int) {
        if (muxer.isConnected) {
            val streamingStartAt = System.currentTimeMillis()
            audioHandler.start(frequency, streamingStartAt)
            audioHandler.setOnAudioEncoderStateListener(this)
        }
    }

    open fun stopAudioStream() {
        audioHandler.stop()
    }

    /*
    * *****************************************************************************************************************************************************
    */

    open fun endEvent(view: View) {
        val data = Intent()
        data.putExtra(YoutubeApi.BROADCAST_ID_KEY, broadcastId)
        if (parent == null) {
            setResult(RESULT_OK, data)
        } else {
            parent.setResult(RESULT_OK, data)
        }
        finish()
    }

    override fun onAudioDataEncoded(data: ByteArray?, size: Int, timestamp: Int) {
        muxer.sendAudio(data, size, timestamp)
    }

    interface OnVideoEncoderStateListener {
        fun onVideoDataEncoded(data: ByteArray?, size: Int, timestamp: Int)
    }

    fun setOnVideoEncoderStateListener(listener: OnVideoEncoderStateListener?) {
        videoEncoder?.setOnVideoEncoderStateListener(listener)
    }
}