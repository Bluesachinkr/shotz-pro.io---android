package com.android.shotz_pro_io

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.widget.RemoteViews
import android.widget.Toast
import android.widget.ToggleButton
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import java.io.File
import java.lang.Exception
import java.util.*
import java.util.jar.Manifest

class ScreenCaptureActivity : AppCompatActivity() {

    lateinit var toggle_button_screen_capturing: ToggleButton
    private var mMediaProjection: MediaProjection? = null
    private lateinit var mMediaProjectionManager: MediaProjectionManager
    private lateinit var mMediaRecorder: MediaRecorder
    private lateinit var mCollapsedNotificationView : RemoteViews
    private lateinit var mExpandedView : RemoteViews
    private lateinit var mVirtualDisplay: VirtualDisplay

    private val permission = arrayOf<String>(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.RECORD_AUDIO
    )

    private val CAPTURE_REQUEST_CODE = 202
    private val PERMISSION_CODE = 230

    private var mScreenDensity: Int = 0
    private val mMediaProjectionCallback: MediaProjection.Callback =
        object : MediaProjection.Callback() {
            override fun onStop() {
                if (toggle_button_screen_capturing.isChecked) {
                    toggle_button_screen_capturing.isChecked = false
                    mMediaRecorder.stop()
                    mMediaRecorder.reset()
                }
                stopScreenCapturing()
            }
        }

    companion object {
        var DISPLAY_WIDTH = 720
        var DISPLAY_HEIGHT = 1280
        private var mContext : ScreenCaptureActivity? = null
        fun getInstance() : Context{
            return mContext!!
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_capture)
        mContext = this
        this.toggle_button_screen_capturing = findViewById(R.id.toggle_button_screen_capturing)

        this.mMediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        setDisplayDimensions()
        this.mMediaRecorder = MediaRecorder()
        initRecorder()
        prepareRecorder()
        this.toggle_button_screen_capturing.setOnClickListener {
            if (hasPermissions(permission)) {
                onToggleScreenShare()
            } else {
                ActivityCompat.requestPermissions(this, permission, PERMISSION_CODE)
            }
        }

        this.mCollapsedNotificationView = RemoteViews(packageName,R.layout.custom_capture_control_notification_layout)
        val intent = Intent(this,NotificationReceiver::class.java)
        intent.putExtra("from",NotificationReceiver.RECORD_SCREEN)
        this.mCollapsedNotificationView.setOnClickPendingIntent(R.id.start_stop_btn_notification,
            PendingIntent.getBroadcast(this,30,intent,0))

        sendControlNotification()
    }

    private fun sendControlNotification(){

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel("Chatterz","shotz",NotificationManager.IMPORTANCE_HIGH)
        channel.enableVibration(true)
        notificationManager.createNotificationChannel(channel)

        val builder = NotificationCompat.Builder(this,"Chatterz")
            .setContentTitle("Shotz Pro")
            .setOngoing(true)
            .setSmallIcon(R.drawable.app_icon)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContent(mCollapsedNotificationView)
            .setAutoCancel(true)

        notificationManager.notify(0,builder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        mMediaProjection?.let {
            mMediaProjection?.stop()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != CAPTURE_REQUEST_CODE) {
            return
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(
                this,
                "Screen Cast Permission Denied", Toast.LENGTH_SHORT
            ).show();
            toggle_button_screen_capturing.isChecked = false
            return
        }
        data?.let {
            mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, it)
            mMediaProjection?.registerCallback(mMediaProjectionCallback, null)
            mVirtualDisplay = createVirtualDisplay()
            mMediaRecorder.start()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE) {
            for (p in permissions) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        p
                    ) == PackageManager.PERMISSION_DENIED
                ) {
                    finish()
                }
            }
        }
    }

    // preaore recorder for recording
    private fun prepareRecorder() {
        try {
            mMediaRecorder.prepare()
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }

    private fun initRecorder() {
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        mMediaRecorder.setVideoEncodingBitRate(512 * 1000)
        mMediaRecorder.setVideoFrameRate(30)
        mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT)
        mMediaRecorder.setOutputFile(getOutputFile())
    }

    //output file for capturing
    private fun getOutputFile(): String {
        val common = Common()
        return common.getFile(common.capture).path.toString()
    }

    //set dendisty for video capture
    private fun setDisplayDimensions() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        this.mScreenDensity = displayMetrics.densityDpi
    }

    // start capture when user click on toggle
    open fun startScreenCapturing() {
        if (mMediaProjection == null) {
            startActivityForResult(
                mMediaProjectionManager.createScreenCaptureIntent(),
                CAPTURE_REQUEST_CODE
            )
            return
        }
        mVirtualDisplay = createVirtualDisplay()
        mMediaRecorder.start()
    }

    // to check for permission
    private fun hasPermissions(permission: Array<String>): Boolean {
        for (p in permission) {
            if (ActivityCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_DENIED) {
                return false
            }
        }
        return true
    }

    //toggle screen share button
    open fun onToggleScreenShare() {
        if (toggle_button_screen_capturing.isChecked) {
            startScreenCapturing()
        } else {
            mMediaRecorder.stop()
            mMediaRecorder.reset()
            stopScreenCapturing()
        }
    }

    //create virtual display for capturing
    private fun createVirtualDisplay(): VirtualDisplay {
        return mMediaProjection!!.createVirtualDisplay(
            "Main Activity", DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.surface, null, null
        )
    }

    // stop screen capturing
    private fun stopScreenCapturing() {
        if (mVirtualDisplay == null) {
            return
        }
        mVirtualDisplay.release()
    }
}