package com.android.shotz_pro_io.stream

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.android.shotz_pro_io.R
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential

class StreamingActivity : AppCompatActivity() {

    private val SCREEN_CAPTURE_CODE = 201

    private var mScreenCaptureIntent: Intent? = null
    private var nScreenCaptureResultCode: Int = 0

    companion object {
        var rtmpUrl: String = ""
        var broadCastId: String = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_streaming)
        intent?.let {
            rtmpUrl = it.getStringExtra(YoutubeApi.RTMP_URL_KEY) as String
            broadCastId = it.getStringExtra(YoutubeApi.BROADCAST_ID_KEY) as String
        }
        requestScreenCaptureIntent()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SCREEN_CAPTURE_CODE) {
            if (resultCode != RESULT_OK) {
                return
            } else {
                mScreenCaptureIntent = data
                mScreenCaptureIntent?.putExtra("SCREEN_CAPTURE_INTENT_RESULT_CODE", resultCode)
                nScreenCaptureResultCode = resultCode
                runtimePermission()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun startStreamControllerServie() {
        val streamingControllerService = Intent(this, StreamingControllerService()::class.java)
        streamingControllerService.action = "Camera_Available"

        streamingControllerService.putExtra(Intent.EXTRA_INTENT, mScreenCaptureIntent)
        streamingControllerService.putExtra(YoutubeApi.RTMP_URL_KEY, rtmpUrl)
        streamingControllerService.putExtra(YoutubeApi.BROADCAST_ID_KEY, broadCastId)
        startService(streamingControllerService)
    }

    fun requestScreenCaptureIntent() {
        if (mScreenCaptureIntent == null) {
            val mediaProjectionManager =
                getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                SCREEN_CAPTURE_CODE
            )
        }
    }

    fun runtimePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val myIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                startActivity(myIntent)
            } else {
                startStreamControllerServie()
            }
        } else {
            startStreamControllerServie()
        }
    }

    private fun hasCameraFeature(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(CAMERA_SERVICE)
    }
}