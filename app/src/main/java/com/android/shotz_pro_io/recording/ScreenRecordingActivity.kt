package com.android.shotz_pro_io.recording

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import android.util.DisplayMetrics
import android.widget.RemoteViews
import android.widget.Toast
import android.widget.ToggleButton
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.android.shotz_pro_io.common.Common
import com.android.shotz_pro_io.R
import java.io.File
import java.lang.Exception

class ScreenRecordingActivity : AppCompatActivity() {

    open lateinit var toggle_button_screen_capturing: ToggleButton
    private var mMediaProjection: MediaProjection? = null
    private lateinit var mMediaProjectionManager: MediaProjectionManager
    private var mMediaRecorder: MediaRecorder? = null
    private lateinit var mNotificationManager: NotificationManager
    open lateinit var mCollapsedNotificationView: RemoteViews
    open lateinit var mExpandedNotificationView: RemoteViews
    private var mVirtualDisplay: VirtualDisplay?=null

    private val permission = arrayOf<String>(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.RECORD_AUDIO
    )

    private val mVideosList = mutableListOf<File>()
    private var mCurrentFile: File? = null

    open var isPause = false
    open var isRecording = false

    private val CAPTURE_REQUEST_CODE = 202
    private val PERMISSION_CODE = 230

    private var mScreenDensity: Int = 0
    private val mMediaProjectionCallback: MediaProjection.Callback =
        object : MediaProjection.Callback() {
            override fun onStop() {
                if (toggle_button_screen_capturing.isChecked) {
                    toggle_button_screen_capturing.isChecked = false
                    mMediaRecorder?.stop()
                    mMediaRecorder?.reset()
                }
                stopScreenCapturing()
            }
        }

    companion object {
        var DISPLAY_WIDTH = 720
        var DISPLAY_HEIGHT = 1280
        private var mContext: ScreenRecordingActivity? = null
        fun getInstance(): ScreenRecordingActivity {
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

        initRecorder()
        prepareRecorder()
        this.toggle_button_screen_capturing.setOnClickListener {
            if (hasPermissions(permission)) {
                onToggleScreenShare()
            } else {
                ActivityCompat.requestPermissions(this, permission, PERMISSION_CODE)
            }
        }

        this.mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        this.mCollapsedNotificationView = RemoteViews(
            packageName,
            R.layout.custom_capture_control_notification_layout
        )
        this.mExpandedNotificationView =
            RemoteViews(packageName, R.layout.custom_capture_control_notification_expanded)
        setUpNotification()
    }

    private fun setUpNotification() {
        //set up start and stop functionalities
        val intentStartStop = Intent(this, NotificationReceiver::class.java)
        intentStartStop.putExtra("from", NotificationReceiver.RECORD_SCREEN)
        intentStartStop.putExtra(NotificationReceiver.PURPOSE, NotificationReceiver.START_STOP)
        intentStartStop.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        this.mCollapsedNotificationView.setOnClickPendingIntent(
            R.id.start_btn_notification,
            PendingIntent.getBroadcast(this, 30, intentStartStop, PendingIntent.FLAG_UPDATE_CURRENT)
        )
        this.mCollapsedNotificationView.setOnClickPendingIntent(
            R.id.stop_btn_notification,
            PendingIntent.getBroadcast(this, 31, intent, 0)
        )

        //play and pasue functionalities
        val playPauseIntent = Intent(this, NotificationReceiver::class.java)
        playPauseIntent.putExtra("from", NotificationReceiver.RECORD_SCREEN)
        playPauseIntent.putExtra(NotificationReceiver.PURPOSE, NotificationReceiver.PLAY_PAUSE)
        playPauseIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        this.mCollapsedNotificationView.setOnClickPendingIntent(
            R.id.play_pause_btn,
            PendingIntent.getBroadcast(this, 32, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        )

        //stop functionalities
        val stopIntent = Intent(this, NotificationReceiver::class.java)
        stopIntent.putExtra("from", NotificationReceiver.RECORD_SCREEN)
        stopIntent.putExtra(NotificationReceiver.PURPOSE, NotificationReceiver.STOP)
        stopIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        this.mCollapsedNotificationView.setOnClickPendingIntent(
            R.id.stop_btn_notification,
            PendingIntent.getBroadcast(this, 33, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        )

        //send notificayion
        sendControlNotification()
    }

    private fun sendControlNotification() {
        val channel = NotificationChannel("Shotz", "shotz", NotificationManager.IMPORTANCE_HIGH)
        channel.enableVibration(true)
        mNotificationManager.createNotificationChannel(channel)

        val builder = NotificationCompat.Builder(this, "Shotz")
            .setSmallIcon(R.drawable.app_icon)
            .setOngoing(true)
            .setCustomContentView(mCollapsedNotificationView)
            .setAutoCancel(true)

        mNotificationManager.notify(0, builder.build())
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
            mMediaRecorder?.start()
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
            mMediaRecorder?.prepare()
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }

    private fun initRecorder() {
        if (mMediaRecorder == null) {
            mMediaRecorder = MediaRecorder()
            mMediaRecorder?.let {
                it.setAudioSource(MediaRecorder.AudioSource.MIC)
                it.setVideoSource(MediaRecorder.VideoSource.SURFACE)
                it.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                it.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                it.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                it.setVideoEncodingBitRate(512 * 1000)
                it.setVideoFrameRate(30)
                it.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT)
            }
        }
        mMediaRecorder?.let {
            mCurrentFile = getOutputFile()
            it.setOutputFile(mCurrentFile)
        }
    }

    //output file for capturing
    private fun getOutputFile(): File {
        val common = Common()
        return common.getFile(common.capture)
    }

    //set dendisty for video capture
    private fun setDisplayDimensions() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        this.mScreenDensity = displayMetrics.densityDpi
    }

    //toggle screen share button
    open fun onToggleScreenShare() {
        if (toggle_button_screen_capturing.isChecked) {
            startScreenCapturing()
        } else {
            if (isPause) {
                mMediaRecorder?.resume()
            }
            mMediaRecorder?.stop()
            mMediaRecorder?.reset()
            mMediaRecorder = null
            stopScreenCapturing()
        }
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
        if (mMediaRecorder == null) {
            initRecorder()
            prepareRecorder()
        }
        mVirtualDisplay = createVirtualDisplay()
        mMediaRecorder?.start()
    }

    // stop screen capturing
    open fun stopScreenCapturing() {
        mVirtualDisplay?.release()
        mVirtualDisplay = null
    }

    //pause screen capturing
    open fun pauseScreenCapturing() {
        if (isRecording) {
            mMediaRecorder?.pause()
        }
    }

    //resume screen capturing
    open fun resumeScreenCapturing() {
        if (isRecording) {
            mMediaRecorder?.resume()
        }
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

    open fun updateNotification() {
        val builder = NotificationCompat.Builder(this, "Shotz")
            .setSmallIcon(R.drawable.app_icon)
            .setOngoing(true)
            .setCustomContentView(mCollapsedNotificationView)
            .setAutoCancel(true)
        mNotificationManager.notify(0, builder.build())
    }

    //create virtual display for capturing
    private fun createVirtualDisplay(): VirtualDisplay {
        return mMediaProjection!!.createVirtualDisplay(
            "Main Activity", DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder?.surface, null, null
        )
    }

}