package com.android.shotz_pro_io.screenCapture

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.android.shotz_pro_io.R

class CapturingBallService : Service(), View.OnTouchListener, View.OnClickListener {
    private lateinit var windowManager: WindowManager
    private lateinit var mBallView: View
    private lateinit var mStreamController: View
    private lateinit var accountImageLayout: RelativeLayout
    private lateinit var live_stream_panel_after: LinearLayout
    private lateinit var liveBtn_streamController: Button
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX: Float = 0.toFloat()
    private var initialTouchY: Float = 0.toFloat()
    private var lastAction = 0
    private var isStreamControllerEnabled: Boolean = false
    private var isLive : Boolean = false
    private var params: WindowManager.LayoutParams? = null
    private var controllerParams: WindowManager.LayoutParams? = null

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        mBallView = LayoutInflater.from(this).inflate(R.layout.capture_bubble_view, null)
        mStreamController = LayoutInflater.from(this).inflate(R.layout.control_stream_view, null)

        accountImageLayout = mBallView.findViewById(R.id.controlLayout)
        live_stream_panel_after = mStreamController.findViewById(R.id.live_stream_panel_after)
        liveBtn_streamController = mStreamController.findViewById(R.id.liveBtn_streamController)

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

        //layout initial position
        params?.gravity = Gravity.TOP or Gravity.RIGHT
        params?.x = 0
        params?.y = 100

        //stream controller postion
        controllerParams?.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager?.let {
            it.addView(mBallView, params)
        }

        //movement
        mBallView?.let {
            val accountImageLayout = it.findViewById<RelativeLayout>(R.id.controlLayout)
            accountImageLayout.setOnTouchListener(this)
            val image = it.findViewById<ImageView>(R.id.bubble_account_image_view)
            image.setOnTouchListener(this)
        }
        liveBtn_streamController.setOnClickListener(this)
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
                        /*val intent = Intent(this,ScreenCaptureActivity::class.java)
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

    override fun onClick(p0: View?) {
        when(p0){
            liveBtn_streamController->{
                if(isLive) {
                    liveBtn_streamController.text = "Live"
                    live_stream_panel_after.visibility = View.GONE
                    isLive = false
                }else{
                    liveBtn_streamController.text = "Stop"
                    live_stream_panel_after.visibility = View.VISIBLE
                    isLive = true
                }
            }
            else->{
                return
            }
        }
    }
}