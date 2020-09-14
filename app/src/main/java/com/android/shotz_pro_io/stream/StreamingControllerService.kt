package com.android.shotz_pro_io.stream

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.PixelFormat
import android.hardware.Camera
import android.os.*
import android.text.TextUtils
import android.view.*
import android.widget.*
import com.android.shotz_pro_io.LoginActivity
import com.android.shotz_pro_io.R
import com.android.shotz_pro_io.main.MainActivity
import com.android.shotz_pro_io.rtmp.utils.StreamProfile
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.mikhaellopez.circularimageview.CircularImageView
import org.json.JSONObject

class StreamingControllerService() : Service(), View.OnTouchListener, View.OnClickListener {

    private val TAG = StreamingControllerService::class.java.simpleName
    val ACTION_NOTIFY_FROM_STREAM_SERVICE = "ACTION_NOTIFY_FROM_STREAM_SERVICE"
    val ACTION_UPDATE_SETTING = "ACTION_UPDATE_SETTING"

    private var mStreamingService: StreamingService? = null
    private var mCamera: android.hardware.Camera? = null
    private var mScreenCaptureIntent: Intent? = null

    private var rtmpUrl: String = ""
    private var broadcastId: String = ""
    private var health: String = ""
    private var streamLifecycle: String = ""
    private var status: String = ""
    private var isEventStarted: Boolean = false

    private var stremStartingAt: Long = 1
    private var isCamOpen: Boolean = false

    private lateinit var windowManager: WindowManager
    private lateinit var countDownTimer: CountDownTimer

    private var params: WindowManager.LayoutParams? = null
    private var controllerParams: WindowManager.LayoutParams? = null
    private var paramCam: WindowManager.LayoutParams? = null
    private var paramCountDown: WindowManager.LayoutParams? = null


    private lateinit var mBallView: View
    private lateinit var mCameraLayout: View
    private lateinit var mStreamController: View
    private lateinit var mCountDown: View

    private lateinit var accountImageLayout: FrameLayout
    private lateinit var accountImage: CircularImageView
    private lateinit var countDownText: TextView
    private lateinit var healthStatus: TextView
    private lateinit var cameraPreview: RelativeLayout
    private lateinit var setting_stream_panel: RelativeLayout
    private lateinit var close_stream_panel: RelativeLayout
    private lateinit var live_chats_panel: RelativeLayout
    private lateinit var camera_open_close_panel: RelativeLayout

    /*private lateinit var lifecycleStatus : TextView*/
    private lateinit var textureViewCamera: TextureView
    private lateinit var live_stream_panel_after: LinearLayout
    private lateinit var liveBtn_streamController: Button

    private var mRecordingStarted = false
    private var mRecordingPaused = false
    private var mCameraWidth = false
    private var mCameraHeight = false
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX: Float = 0.toFloat()
    private var initialTouchY: Float = 0.toFloat()
    private var lastAction = 0
    private var isStreamControllerEnabled: Boolean = false
    private var isLive: Boolean = false


    companion object {
        var isBallOpen = false
        var live_status_time: TextView? = null
        var id: String = ""
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        this.mBallView = LayoutInflater.from(this).inflate(R.layout.capture_bubble_view, null)
        this.mStreamController =
            LayoutInflater.from(this).inflate(R.layout.control_stream_view, null)
        this.mCameraLayout = LayoutInflater.from(this).inflate(R.layout.cam_streaming_view, null)
        this.mCountDown =
            LayoutInflater.from(this).inflate(R.layout.count_down_streaming_view, null)

        this.accountImageLayout = mBallView.findViewById(R.id.controlLayout)
        this.live_stream_panel_after = mStreamController.findViewById(R.id.live_stream_panel_after)
        live_status_time = mStreamController.findViewById(R.id.live_status_time)
        this.healthStatus = mStreamController.findViewById(R.id.healthStatus)
        this.close_stream_panel = mStreamController.findViewById(R.id.close_stream_panel)
        this.camera_open_close_panel = mStreamController.findViewById(R.id.camera_open_close_panel)
        this.live_chats_panel = mStreamController.findViewById(R.id.camera_open_close_panel)
        this.setting_stream_panel = mStreamController.findViewById(R.id.setting_stream_panel)
        this.liveBtn_streamController =
            mStreamController.findViewById(R.id.liveBtn_streamController)
        this.cameraPreview = mCameraLayout.findViewById(R.id.cameraPreview)
        this.countDownText = mCountDown.findViewById(R.id.countDownText)
        this.textureViewCamera = mCameraLayout.findViewById(R.id.textureViewCamStream)
        accountImage = mBallView.findViewById(R.id.bubble_account_image_view)

        this.countDownTimer = object : CountDownTimer(3600 * 24 * 1000, 500) {
            override fun onTick(p0: Long) {
                GetTime().execute(p0)
                healthStatus.text = health
                setTime(stremStartingAt / 2)
                stremStartingAt++;
                if (health == "noData") {
                    stopStreamingEncoder()
                }
            }

            override fun onFinish() {

            }
        }

        var LAYOUT_FLAG = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE
        }

        //Layout Params Window Manager
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            LAYOUT_FLAG,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSPARENT
        )

        controllerParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            LAYOUT_FLAG,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSPARENT
        )

        paramCam = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            LAYOUT_FLAG,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSPARENT
        )

        paramCountDown = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            LAYOUT_FLAG,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSPARENT
        )

        //layout initial position
        params?.gravity = Gravity.TOP or Gravity.RIGHT
        params?.x = 0
        params?.y = 100

        //stream controller postion
        controllerParams?.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL

        //cam initial position
        paramCam?.gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
        paramCam?.x = 10

        //countDown initial position
        paramCountDown?.gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager?.let {
            it.addView(mBallView, params)
        }

        //movement
        accountImageLayout.setOnTouchListener(this)
        accountImage.setOnClickListener {
            if (isStreamControllerEnabled) {
                windowManager?.removeView(mStreamController)
                isStreamControllerEnabled = false
            } else {
                windowManager?.let {
                    it.addView(mStreamController, controllerParams)
                }
                isStreamControllerEnabled = true
            }
        }

        liveBtn_streamController.setOnClickListener(this)
        camera_open_close_panel.setOnClickListener(this)
        close_stream_panel.setOnClickListener(this)
        live_chats_panel.setOnClickListener(this)
        setting_stream_panel.setOnClickListener(this)
    }

    private fun setTime(time: Long) {
        live_status_time?.let {
            if (time < 60) {
                if (time >= 10) {
                    it.text = "00:00:" + time
                } else {
                    it.text = "00:00:0" + time
                }
            } else if (time < 3600) {
                val second = time % 60
                val minutes = time / 60
                if (minutes >= 10) {
                    if (second >= 10) {
                        it.text = "00:" + minutes + ":" + second
                    } else {
                        it.text = "00:" + minutes + ":0" + second
                    }
                } else {
                    if (second >= 10) {
                        it.text = "00:0" + minutes + ":" + second
                    } else {
                        it.text = "00:0" + minutes + ":0" + second
                    }
                }
            } else {
                val m = time % 3600
                val hour = time / 3600
                val second = m % 60
                val minutes = m / 60
                if (hour >= 10) {
                    if (minutes >= 10) {
                        if (second >= 10) {
                            it.text = "" + hour + ":" + minutes + ":" + second
                        } else {
                            it.text = "" + hour + ":" + minutes + ":0" + second
                        }
                    } else {
                        if (second >= 10) {
                            it.text = "" + hour + ":0" + minutes + ":" + second
                        } else {
                            it.text = "" + hour + ":0" + minutes + ":0" + second
                        }
                    }
                } else {
                    if (minutes >= 10) {
                        if (second >= 10) {
                            it.text = "0" + hour + ":" + minutes + ":" + second
                        } else {
                            it.text = "0" + hour + ":" + minutes + ":0" + second
                        }
                    } else {
                        if (second >= 10) {
                            it.text = "0" + hour + ":0" + minutes + ":" + second
                        } else {
                            it.text = "0" + hour + ":0" + minutes + ":0" + second
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mBallView?.let {
            windowManager?.removeView(it)
        }
    }

    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        event?.let {
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {

                    //intiial position
                    initialX = params!!.x
                    initialY = params!!.y

                    //get motion
                    initialTouchX = it.rawX
                    initialTouchY = it.rawY

                    lastAction = it.action
                }
                MotionEvent.ACTION_MOVE -> {

                    params?.x = initialX + ((initialTouchX - it.rawX).toInt())
                    params?.y = initialY + ((it.rawY - initialTouchY).toInt())

                    windowManager?.updateViewLayout(mBallView, params)
                    lastAction = it.action
                }
                MotionEvent.ACTION_UP -> {
                    if (lastAction == MotionEvent.ACTION_DOWN) {
                        /*val intent = Intent(this,ScreenRecordingActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)

                        stopSelf()*/
                        if (isStreamControllerEnabled) {
                            windowManager?.removeView(mStreamController)
                            isStreamControllerEnabled = false
                        } else {
                            windowManager?.let {
                                it.addView(mStreamController, controllerParams)
                            }
                            isStreamControllerEnabled = true
                        }
                    }
                    lastAction = it.action
                }
                else -> {
                    return false
                }
            }
        }
        return true
    }

    fun stopStreamingEncoder() {
        countDownTimer.onFinish()
        liveBtn_streamController.text = "Live"
        live_stream_panel_after.visibility = View.GONE
        mStreamingService?.stopStreaming(broadcastId)
        isLive = false
    }

    override fun onClick(p0: View?) {
        when (p0) {
            liveBtn_streamController -> {
                if (hasGoogleSignIn()) {
                    if (isLive) {
                        stopStreamingEncoder()
                    } else {
                        countDownTimer.start()
                        liveBtn_streamController.text = "Stop"
                        live_stream_panel_after.visibility = View.VISIBLE
                        StreamingActivity.rtmpUrl?.let {
                            mStreamingService?.startStreaming(it, broadcastId!!)
                        }
                        isLive = true
                    }

                } else {
                    startActivity(Intent(this, LoginActivity::class.java))
                }
            }
            camera_open_close_panel -> {
                if (isCamOpen) {
                    windowManager.removeView(mCameraLayout)
                    isCamOpen = false
                } else {
                    windowManager?.let {
                        it.addView(mCameraLayout, paramCam)
                    }
                    isCamOpen = true
                }
            }
            else -> {
                return
            }
        }
    }

    //start command service in streaming controller ball service
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return START_NOT_STICKY
        }
        intent?.let {
            val action = it.action
            val intentTwo = it
            action?.let {
                handleIncomeAction(intentTwo)
                if (TextUtils.equals(action, "Camera_Available")) {
                    initCameraView()
                }
                return START_NOT_STICKY
            }
            mScreenCaptureIntent = it.getParcelableExtra(Intent.EXTRA_INTENT)
            if (mScreenCaptureIntent == null) {
                stopSelf()
            } else {
                bindStreamingService()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    //handle income action from start command
    private fun handleIncomeAction(intent: Intent) {
        val action = intent.action
        if (TextUtils.isEmpty(action)) {
            return
        }
        when (action) {
            ACTION_UPDATE_SETTING -> {
                handleUpdateSetting(intent)
            }
            ACTION_NOTIFY_FROM_STREAM_SERVICE -> {
                handleNotifyFromStreamService(intent)
            }
            else -> {
                return
            }
        }
    }


    private fun initCameraView() {
        //set camera mode
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT)
    }


    private fun bindStreamingService() {
        val intentService = Intent(this, StreamingService::class.java)
        intentService.putExtra(Intent.EXTRA_INTENT, mScreenCaptureIntent)
        bindService(intentService, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val serviceBinder = binder as StreamingService.LocalBinder
            mStreamingService = serviceBinder.getService()
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
        }
    }

    private fun handleUpdateSetting(intent: Intent) {
        val key = intent.getIntExtra(ACTION_UPDATE_SETTING, -1)
        when (key) {
            R.string.setting_camera_size -> updateCameraSize()
            R.string.setting_camera_position -> updateCameraPosition()
            R.string.setting_camera_mode -> updateCameraMode()
            else -> return
        }
    }

    private fun updateCameraMode() {

    }

    private fun updateCameraPosition() {
        TODO("Not yet implemented")
    }

    private fun updateCameraSize() {
        TODO("Not yet implemented")
    }

    private fun handleNotifyFromStreamService(intent: Intent) {
        val notfiyMsg = intent.getStringExtra(StreamingService.KEY_NOTIFY_MSG)
        if (TextUtils.isEmpty(notfiyMsg)) {
            return
        }
        when (notfiyMsg) {
            StreamingService.NOTIFY_MSG_CONNECTION_STARTED -> Toast.makeText(
                this,
                "Stream started",
                Toast.LENGTH_SHORT
            )
            StreamingService.NOTIFY_MSG_CONNECTION_FAILED -> Toast.makeText(
                this,
                "Connection to server failed. Please try later",
                Toast.LENGTH_LONG
            )
            StreamingService.NOTIFY_MSG_CONNECTION_DISCONNECTED -> Toast.makeText(
                this,
                "Connection to server failed. Please try later",
                Toast.LENGTH_SHORT
            )
            StreamingService.NOTIFY_MSG_STREAM_STOPPED -> Toast.makeText(
                this,
                "Stream Stopped",
                Toast.LENGTH_LONG
            )
            StreamingService.NOTIFY_MSG_ERROR -> Toast.makeText(
                this,
                "Sorry, Error occurs!",
                Toast.LENGTH_LONG
            )
        }
    }

    private fun hasGoogleSignIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        account?.let {
            Glide.with(this).load(it.photoUrl).into(accountImage)
        }
        return account != null
    }

    inner class GetTime() : AsyncTask<Long, Void, Void?>() {
        override fun doInBackground(vararg p0: Long?): Void? {
            val youTube = StreamProfile.youTube!!
            val streamId = StreamProfile.streamId!!
            val id = StreamProfile.broadcastKey!!
            val liveBroadcast = youTube.liveBroadcasts().list("status")
            liveBroadcast.id = id
            val broacastResult = liveBroadcast.execute()
            val kr = broacastResult.items[0]
            streamLifecycle = kr.status.lifeCycleStatus
            val liveStream = youTube.liveStreams().list("status")
            liveStream.id = streamId
            val result = liveStream.execute()
            var item = result.items[0]
            this@StreamingControllerService.status = item.status.streamStatus
            if (item.status.streamStatus == "active" && !isEventStarted) {
                MainActivity?.mContext?.startEvent(StreamProfile.broadcastKey)
                isEventStarted = true
            }
            val ob = JSONObject(item)
            val status: JSONObject = ob["status"] as JSONObject
            val healthStatus = status["healthStatus"] as JSONObject
            health = healthStatus["status"] as String
            println(item)
            return null
        }
    }
}