package com.android.shotz_pro_io.stream

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.shotz_pro_io.R
import com.google.api.services.youtube.YouTube
import net.ossrs.yasea.SrsPublisher
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.jar.Manifest
import kotlin.experimental.and

class StreamingActivity() : AppCompatActivity(), ImageReader.OnImageAvailableListener {

    private lateinit var mediaProjectionPermissionResultData: Intent
    private lateinit var mediaProjectionCallback: MediaProjection.Callback
    private lateinit var youTube: YouTube

    constructor(
        mediaProjectionPermissionResultData: Intent,
        mediaProjectionCallback: MediaProjection.Callback,youTube: YouTube
    ) : this() {
        this.mediaProjectionPermissionResultData = mediaProjectionPermissionResultData
        this.mediaProjectionCallback = mediaProjectionCallback
        this.youTube = youTube
    }

    private val STREAM_REQUEST_CODE = 201
    private val PERMISSION_CODE = 202
    private val permissios = Array<String>(1, { android.Manifest.permission.RECORD_AUDIO })


    private var mScreenDensity: Int = 0
    private var frequency = 0
    private var isDisposed: Boolean = false
    private var cancel = false
    private lateinit var broadcastId: String
    private lateinit var rtmpUrl: String

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var imageReader: ImageReader

    private var streamerService: StreamingService? = null
    private var mediaProjection: MediaProjection? = null
    private var videoCallback: VideoFrameCallback? = null
    private var audioCallback: AudioFrameCallback? = null
    private var audioThread: Thread? = null
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            streamerService = StreamingService.LocalBinder().getService() as StreamingService
            startStreaming()
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            streamerService = null
        }

    }

    private var virtualDisplay: VirtualDisplay? = null

    companion object {
        var DISPLAY_WIDTH = 720
        var DISPLAY_HEIGHT = 1280
        private lateinit var context: StreamingActivity
        fun getInstance(): StreamingActivity {
            return context
        }
    }

    open fun setVideoFrameCallback(videoCallback: VideoFrameCallback) {
        this.videoCallback = videoCallback
    }

    open fun setAudioFrameCallback(audioCallback: AudioFrameCallback) {
        this.audioCallback = audioCallback
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
        this.mScreenDensity = resources.displayMetrics.densityDpi

        if (!havePermissions()) {
            ActivityCompat.requestPermissions(this, permissios, PERMISSION_CODE)
        }

        if(!bindService(Intent(this,StreamingService::class.java),serviceConnection,
                BIND_AUTO_CREATE or BIND_DEBUG_UNBIND)){
            Toast.makeText(this,"Failed to bind streamer services",Toast.LENGTH_LONG).show()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        if(serviceConnection!=null){
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
                    val transition = youTube.LiveBroadcasts().transition("live",broadcastId,"status")
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
        }else {
            createVirtualDisplay()
        }
    }

    open fun startAudioStream(frequency: Int) {
        this.frequency = frequency
        this.cancel = false

        //for thread
        audioThread = Thread(Runnable {
            recordThread()
        })
        audioThread?.start()
    }

    private fun recordThread() {
        val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
        val channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_STEREO
        var bufferSize =
            AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.CAMCORDER, frequency,
            channelConfiguration, audioEncoding, bufferSize
        )
        recorder.startRecording()

        //make buffersize in samples instead of bytes
        bufferSize /= 2
        val buffer = ShortArray(bufferSize)
        while (!cancel) {
            val bufferReadResult = recorder.read(buffer, 0, bufferSize)
            if (bufferReadResult > 0) {
                audioCallback?.onHandleFrame(buffer, bufferReadResult)
            } else if (bufferReadResult < 0) {
                Log.w("Streaming Activity", "Error calling recorder.read: " + bufferReadResult);
            }
        }
        recorder.stop()
    }

    open fun stopAudioStream() {
        cancel = true
        try {
            audioThread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun createVirtualDisplay(){
        if(virtualDisplay == null) {
            virtualDisplay = mediaProjection!!.createVirtualDisplay(
                "Stream Activity",
                DISPLAY_WIDTH,
                DISPLAY_HEIGHT,
                mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface, null, null
            )
        }
        val src =
        val p = SrsPublisher()
    }

    open fun stopVideoStream() {
        virtualDisplay?.release()
        virtualDisplay = null
    }

    override fun onImageAvailable(reader: ImageReader?) {
        var image: Image? = null
        var byteArrayOutputStream: ByteArrayOutputStream? = null
        try {
            reader?.let {
                image = reader.acquireLatestImage()
                image?.let {
                    byteArrayOutputStream = ByteArrayOutputStream()
                    val planes: Array<Image.Plane> = it.planes
                    val w = it.width
                    val h = it.height
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * w
                    var offset = 0
                    val bitmap = Bitmap.createBitmap(
                        resources.displayMetrics,
                        w,
                        h,
                        Bitmap.Config.ARGB_8888
                    )

                    val buffer = planes[0].buffer as ByteBuffer
                    var i = 0
                    while (++i < h) {
                        var j = 0
                        while (++j < w) {
                            var pixel = 0
                            pixel = pixel or (buffer[offset].toInt()) shl 16 // R

                            pixel = pixel or (buffer[offset + 1].toInt()) shl 8 // G

                            pixel = pixel or (buffer[offset + 2].toInt()) // B

                            pixel = pixel or (buffer[offset + 3].toInt()) shl 24 // A

                            bitmap.setPixel(i, j, pixel)
                            offset += pixelStride
                        }
                        offset += rowPadding
                    }

                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                    val result = byteArrayOutputStream!!.toByteArray()
                    videoCallback?.onHandleFrame(result)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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

    interface VideoFrameCallback {
        fun onHandleFrame(result: ByteArray)
    }

    interface AudioFrameCallback {
        fun onHandleFrame(data: ShortArray, length: Int)
    }
}