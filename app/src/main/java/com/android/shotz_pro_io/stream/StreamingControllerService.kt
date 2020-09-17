package com.android.shotz_pro_io.stream

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.hardware.Camera
import android.os.*
import android.text.TextUtils
import android.view.*
import android.widget.*
import com.android.shotz_pro_io.R
import com.android.shotz_pro_io.controllers.setting.CameraSetting
import com.android.shotz_pro_io.controllers.setting.SettingManager
import com.android.shotz_pro_io.main.MainActivity
import com.android.shotz_pro_io.controllers.rtmp.utils.StreamProfile
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.mikhaellopez.circularimageview.CircularImageView
import org.json.JSONObject

class StreamingControllerService : Service(), View.OnTouchListener, View.OnClickListener {

    private val TAG = StreamingControllerService::class.java.simpleName
    val ACTION_NOTIFY_FROM_STREAM_SERVICE = "ACTION_NOTIFY_FROM_STREAM_SERVICE"
    val ACTION_UPDATE_SETTING = "ACTION_UPDATE_SETTING"

    private var mStreamingService: StreamingService? = null
    private var mCamera: android.hardware.Camera? = null
    private var mScreenCaptureIntent: Intent? = null

    private var health: String = ""
    private var streamLifecycle: String = ""
    private var liveStatus: String = ""
    private var isEventStarted: Boolean = false

    private var stremStartingAt: Long = 1
    private var isCamOpen: Boolean = false

    private lateinit var windowManager: WindowManager
    private lateinit var countDownTimer: CountDownTimer

    private var params: WindowManager.LayoutParams? = null
    private var controllerParams: WindowManager.LayoutParams? = null
    private var paramCam: WindowManager.LayoutParams? = null
    private var paramCountDown: WindowManager.LayoutParams? = null
    private var paramStatus: WindowManager.LayoutParams? = null


    private lateinit var mBallView: View
    private lateinit var mCameraLayout: View
    private lateinit var mStreamController: View
    private lateinit var mCountDown: View
    private lateinit var mStatusView: View

    private lateinit var accountImageLayout: FrameLayout
    private lateinit var accountImage: CircularImageView
    private lateinit var countDownText: TextView
    private lateinit var healthStatus: TextView
    private lateinit var statusLive: TextView
    private lateinit var cameraPreview: RelativeLayout
    private lateinit var setting_stream_panel: RelativeLayout
    private lateinit var close_stream_panel: RelativeLayout
    private lateinit var live_chats_panel: RelativeLayout
    private lateinit var camera_open_close_panel: RelativeLayout

    /*private lateinit var lifecycleStatus : TextView*/
    private lateinit var textureViewCamera: TextureView
    private lateinit var live_stream_panel_after: LinearLayout
    private lateinit var liveBtn_streamController: Button

    private var mCameraWidth = 0
    private var mCameraHeight = 0
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX: Float = 0.toFloat()
    private var initialTouchY: Float = 0.toFloat()
    private var lastAction = 0
    private var isStreamControllerEnabled: Boolean = false
    private var isLive: Boolean = false


    companion object {
        var isBallOpen = false
        var live_status: TextView? = null
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
        this.mStatusView =
            LayoutInflater.from(this).inflate(R.layout.status_layout_live_stream, null)

        this.accountImageLayout = mBallView.findViewById(R.id.controlLayout)
        this.live_stream_panel_after = mStreamController.findViewById(R.id.live_stream_panel_after)
        live_status = mStreamController.findViewById(R.id.live_status)
        this.healthStatus = mStreamController.findViewById(R.id.healthStatus)
        this.close_stream_panel = mStreamController.findViewById(R.id.close_stream_panel)
        this.camera_open_close_panel = mStreamController.findViewById(R.id.camera_open_close_panel)
        this.live_chats_panel = mStreamController.findViewById(R.id.camera_open_close_panel)
        this.setting_stream_panel = mStreamController.findViewById(R.id.setting_stream_panel)
        this.liveBtn_streamController = mStreamController.findViewById(R.id.liveBtn_streamController)
        this.statusLive = mStatusView.findViewById(R.id.status_live_stream)
        this.cameraPreview = mCameraLayout.findViewById(R.id.cameraPreview)
        this.countDownText = mCountDown.findViewById(R.id.countDownText)
        this.textureViewCamera = mCameraLayout.findViewById(R.id.textureViewCamStream)
        this.accountImage = mBallView.findViewById(R.id.bubble_account_image_view)

        //load google sign in details
        loadGoogleSignIn()

        this.countDownTimer = object : CountDownTimer(3600 * 24 * 1000, 500) {
            override fun onTick(p0: Long) {
                onTickWork()
            }

            override fun onFinish() {

            }
        }

        //initialize params fro streaming
        initParams()

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

    private fun initParams() {
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

        paramStatus = WindowManager.LayoutParams(
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

        //status
        paramStatus?.gravity = Gravity.LEFT or Gravity.TOP
        paramStatus?.x = 10
        paramStatus?.y = 10

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager?.let {
            it.addView(mBallView, params)
        }
    }

    private fun onTickWork() {
        statusLive.text = streamLifecycle
        healthStatus.text = health
        live_status?.text = liveStatus
        GetLiveStatus().execute()
        stremStartingAt++
        if (streamLifecycle.equals("live") && health.equals("noData")) {
            stopStreamingEncoder()
            isLive = false
            MainActivity.mContext?.stopStream()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mBallView?.let {
            windowManager?.removeView(it)
        }

        mCameraLayout?.let {
            windowManager?.removeViewImmediate(it)
            releaseCamera()
        }
        mStreamingService?.let {
            unbindService(serviceConnection)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        cameraPreview?.let {
            var width = mCameraWidth
            var height = mCameraHeight

            val params = cameraPreview.layoutParams
            if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
                params.height = width
                params.width = height
            }else{
                params.width = width
                params.height = height
            }
            cameraPreview.layoutParams = params
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
        mStreamingService?.stopStreaming()
        isLive = false
    }

    fun startSteamEncoder() {
        countDownTimer.start()
        liveBtn_streamController.text = "Stop"
        live_stream_panel_after.visibility = View.VISIBLE
        mStreamingService?.startStreaming(
            StreamProfile.rtmpUrl,
            StreamProfile.broadcastKey
        )
        isLive = true
    }

    override fun onClick(p0: View?) {
        when (p0) {
            liveBtn_streamController -> {
                if (isLive) {
                    stopStreamingEncoder()
                } else {
                    startCountDown()
                }
                showStatusLive()
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

    private fun startCountDown() {
        var count = 5
        windowManager?.addView(mCountDown, paramCountDown)
        val timer =object : CountDownTimer(5000,1000){
            override fun onTick(p0: Long) {
                countDownText.text = count.toString()
                count--
            }

            override fun onFinish() {
                windowManager?.removeViewImmediate(mCountDown)
                startSteamEncoder()
            }
        }.start()
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

    //make Timer
    private fun makeTimer() {
        this.countDownTimer = object : CountDownTimer(3600 * 24 * 1000, 500) {
            override fun onTick(p0: Long) {
                onTickWork()
            }

            override fun onFinish() {
            }
        }
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
        val cameraProfile = SettingManager.getCameraProfile(this)

        if(cameraProfile.mode.equals(CameraSetting.CAMERA_MODE_BACK)){
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK)
        }else{
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT)
        }
        calculateCameraSize(cameraProfile)

        onConfigurationChanged(resources.configuration)

        paramCam?.gravity = cameraProfile.paramGravity
        paramCam?.x = 0
        paramCam?.y = 0

        val mPreview  = CameraPreview(this,mCamera)

        cameraPreview.addView(mPreview)
        mCamera?.startPreview()

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
        val profile = SettingManager.getCameraProfile(this)
        if(profile.mode.equals(CameraSetting.CAMERA_MODE_OFF)){
            toggleView(mCameraLayout,View.GONE)
        }else{
            mCameraLayout?.let {
                windowManager?.removeViewImmediate(it)
                releaseCamera()
                initCameraView()
            }
        }
    }

    private fun toggleView(mCameraLayout: View, gone: Int) {
        mCameraLayout.visibility = gone
    }

    private fun releaseCamera() {
        mCamera?.let {
            it.stopPreview()
            it.setPreviewCallback(null)
            it.release()
        }
    }

    private fun updateCameraPosition() {
       val profile = SettingManager.getCameraProfile(this)
        paramCam?.gravity = profile.paramGravity
        paramCam?.x = 0
        paramCam?.y = 0
        windowManager?.updateViewLayout(mCameraLayout,paramCam)
    }

    private fun updateCameraSize() {
        val profile = SettingManager.getCameraProfile(this)
        calculateCameraSize(profile)
        onConfigurationChanged(resources.configuration)
    }

    private fun calculateCameraSize(profile: CameraSetting) {
        var factor = 0
        when(profile.size){
            CameraSetting.SIZE_BIG-> factor = 3
            CameraSetting.SIZE_MEDIUM -> factor = 4
            else-> factor = 5
        }
        var mScreenWidth = resources.displayMetrics.widthPixels
        var mScreenHeight = resources.displayMetrics.heightPixels
        if(mScreenWidth > mScreenHeight){
            mCameraWidth = mScreenWidth / factor
            mCameraHeight = mScreenHeight /factor
        }else{
            mCameraWidth = mScreenHeight/factor
            mCameraHeight = mScreenWidth/factor
        }
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

    private fun loadGoogleSignIn() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        account?.let {
            Glide.with(this).load(it.photoUrl).into(accountImage)
        }
    }

    fun showStatusLive() {
        if (!isLive) {
            windowManager?.addView(mStatusView, paramStatus)
        } else {
            windowManager?.removeViewImmediate(mStatusView)
        }
    }

    inner class GetLiveStatus : AsyncTask<Long, Void, Void?>() {
        override fun doInBackground(vararg p0: Long?): Void? {
            val youTube = StreamProfile.youTube!!
            val streamId = StreamProfile.streamId
            val id = StreamProfile.broadcastKey
            val liveBroadcast = youTube.liveBroadcasts().list("status")
            liveBroadcast.id = id
            val broacastResult = liveBroadcast.execute()
            val kr = broacastResult.items[0]
            streamLifecycle = kr.status.lifeCycleStatus
            val liveStream = youTube.liveStreams().list("status")
            liveStream.id = streamId
            val result = liveStream.execute()
            var item = result.items[0]
            liveStatus = item.status.streamStatus
            if (liveStatus.equals("active") && !isEventStarted) {
                startEventTask()
                isEventStarted = true
            }
            val ob = JSONObject(item)
            val status: JSONObject = ob["status"] as JSONObject
            val healthStatus = status["healthStatus"] as JSONObject
            health = healthStatus["status"] as String
            return null
        }
    }

    fun startEventTask() {
        Thread(Runnable {
            YoutubeApi.startEvent(
                StreamProfile.youTube!!,
                StreamProfile.broadcastKey,
                StreamProfile.streamId
            )
        }).start()
    }
}