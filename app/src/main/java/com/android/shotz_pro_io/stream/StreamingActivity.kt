package com.android.shotz_pro_io.stream

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.IBinder
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.shotz_pro_io.R
import com.android.shotz_pro_io.rtmp.AudioHandler
import com.android.shotz_pro_io.rtmp.Muxer
import com.android.shotz_pro_io.rtmp.PublisherListener
import com.android.shotz_pro_io.rtmp.VideoHandler
import com.google.api.services.youtube.YouTube

class StreamingActivity() : AppCompatActivity(),
    AudioHandler.OnAudioEncoderStateListener, VideoHandler.OnVideoEncoderStateListener,PublisherListener {

    private val mediaProjectionCallback: MediaProjection.Callback = object : MediaProjection.Callback(){
        override fun onStop() {
            mediaProjection?.stop()
            mediaProjection = null
        }
    }
    private var youTube: YouTube? = null

    constructor(youTube: YouTube): this(){
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
    private var videoHandler: VideoHandler

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
        var DISPLAY_WIDTH = 640
        var DISPLAY_HEIGHT = 480
        var mScreenDensity: Int = 0
        var virtualDisplay: VirtualDisplay? = null
        private lateinit var context: StreamingActivity
        fun getInstance(): StreamingActivity {
            return context
        }
    }

    init {
        this.audioHandler = AudioHandler()
        this.videoHandler = VideoHandler()
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
            prepareStream()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun open(url : String){
        muxer.open(url, DISPLAY_WIDTH, DISPLAY_HEIGHT)
    }

    /*
    * ********* Capturing ball ***********************************
    */

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
                    /*val transition =
                        youTube?.LiveBroadcasts()?.transition("live", broadcastId, "status")
                    transition?.execute()*/
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

    /*
    ******** Video Stream *********************
    */

    open fun startVideoStream() {
        if (mediaProjection == null) {
            startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                STREAM_REQUEST_CODE
            )
        }else {
            prepareStream()
        }
    }

    open fun stopVideoStream() {
        videoHandler.stop()
        virtualDisplay?.release()
        virtualDisplay = null
    }

    fun prepareStream(){
        mediaProjection?.let {
            muxer.setOnMuxerStateListener(this)
            val startStreamingAt = System.currentTimeMillis()
            videoHandler.setOnVideoEncoderStateListener(this)
            val surface = videoHandler.start(DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity, startStreamingAt, it)
            surface?.let {
                virtualDisplay = createVirtualDisplay(it)
            }
        }
    }

    private fun createVirtualDisplay(surface: Surface) : VirtualDisplay{
        return mediaProjection!!.createVirtualDisplay("Stream Activity", DISPLAY_WIDTH,
            DISPLAY_HEIGHT, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,surface,null,null)
    }


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

    override fun onVideoDataEncoded(data: ByteArray?, size: Int, timestamp: Int) {
        muxer.sendVideo(data, size, timestamp)
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