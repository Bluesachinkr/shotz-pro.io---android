package com.android.shotz_pro_io.stream

import android.content.Context
import android.content.res.Configuration
import android.hardware.Camera
import android.util.Log
import android.view.*
import java.io.IOException


class CameraPreview(context: Context, camera: Camera?) : SurfaceView(context),
    SurfaceHolder.Callback {
    private val mHolder: SurfaceHolder
    private var mCamera: Camera?
    private val mContext: Context
    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        refreshCamera(mCamera)
    }

    fun refreshCamera(camera: Camera?) {
        if (mHolder.getSurface() == null) {
            return
        }
        try {
            mCamera?.stopPreview()
        } catch (e: Exception) {
        }
        setCameraOrientation()
        setCamera(camera)
        try {
            mCamera?.setPreviewDisplay(mHolder)
            mCamera?.startPreview()
        } catch (e: Exception) {
            Log.d(VIEW_LOG_TAG, "Error starting camera preview: " + e.message)
        }
    }

    fun setCamera(camera: Camera?) {
        mCamera = camera
    }

    private fun setCameraOrientation() {
        val camInfo = Camera.CameraInfo()
        Camera.getCameraInfo(backFacingCameraId, camInfo)
        val windowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val rotation: Int = display.getRotation()
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        var result: Int
        if (camInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (camInfo.orientation + degrees) % 360
            result = (360 - result) % 360 // compensate the mirror
        } else {  // back-facing
            result = (camInfo.orientation - degrees + 360) % 360
        }
        mCamera?.setDisplayOrientation(result)
    }

    // Search for the front facing camera
    private val backFacingCameraId: Int
        private get() {
            var cameraId = -1
            val numberOfCameras: Int = Camera.getNumberOfCameras()
            for (i in 0 until numberOfCameras) {
                val info: Camera.CameraInfo = Camera.CameraInfo()
                Camera.getCameraInfo(i, info)
                if (info.facing === Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraId = i
                    break
                }
            }
            return cameraId
        }

    companion object {
        private const val TAG = "CameraPreview"
    }

    init {
        mContext = context
        mCamera = camera
        mHolder = getHolder()
        mHolder.addCallback(this)
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    override fun surfaceCreated(p0: SurfaceHolder) {
        try {
            if (mCamera == null) {
                mCamera?.setPreviewDisplay(holder)
                mCamera?.startPreview()
            }
        } catch (e: IOException) {
            Log.d(TAG, "Error setting camera preview: " + e.message)
        }
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
        refreshCamera(mCamera)
    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {

    }
}