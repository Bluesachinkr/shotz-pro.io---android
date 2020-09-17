package com.android.shotz_pro_io.controllers.setting

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils

class VideoSetting() {

    var mWidth =
        0
    var mHeight:Int = 0
    var mFPS:Int = 0
    var mBirate:Int = 0
    var mOrientation:Int = 0

    constructor(width: Int, height: Int, FPS: Int, bitrate: Int, orientation: Int) : this(){
        this.mWidth = width
        this.mHeight = height
        this.mFPS = FPS
        this.mBirate = bitrate
        this.mOrientation = orientation
    }

    companion object{
        val ORIENTATION_PORTRAIT = 0
        val ORIENTATION_LANDSCAPE = 1
        var VIDEO_PROFILE_SD= VideoSetting(
            640,
            360,
            30,
            11200 * 1024,
            ORIENTATION_LANDSCAPE
        )
        var VIDEO_PROFILE_HD= VideoSetting(
            1280,
            720,
            30,
            2000 * 1024,
            ORIENTATION_LANDSCAPE
        )
        var VIDEO_PROFILE_FHD= VideoSetting(
            1920,
            1080,
            30,
            4000 * 1024,
            ORIENTATION_LANDSCAPE
        )
        private var mOutputPath: String? = null

        fun getShortResolution(width: Int, height: Int): String? {
            var factor = width
            if (width < height) factor = height
            when (factor) {
                1920 -> return "FHD"
                1280 -> return "HD"
                480 -> return "SD"
            }
            return "FIT"
        }

        //Size: bytes
        @SuppressLint("DefaultLocale")
        fun getFormattedSize(bytes: Long): String? {
            val si = true
            val unit = if (si) 1000 else 1024
            if (bytes < unit) return "$bytes B"
            val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
            val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1].toString() + if (si) "" else "i"
            return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
        }

        fun getFormattedDuration(duration: Long): String? {
            return String.format(
                "%d:%02d:%02d",
                duration / 3600,
                duration % 3600 / 60,
                duration % 60
            )
        }

        fun getFormattedBitrate(bitrate: Int): String? {
            return (bitrate / 1024).toString() + " Kbps"
        }

        fun getFitDeviceResolution(context: Context): VideoSetting? {
            val displayMetrics = context.resources.displayMetrics
            val height = displayMetrics.heightPixels
            val width = displayMetrics.widthPixels
            if (height < width) {
                swap(width, height)
            }
            return VideoSetting(width, height, 30, 4000 * 1024, ORIENTATION_LANDSCAPE)
        }

        private fun swap(x: Int, y: Int) {}
    }

    fun getWidth(): Int {
        return mWidth
    }

    fun getHeight(): Int {
        return mHeight
    }

    fun getFPS(): Int {
        return mFPS
    }

    fun getBitrate(): Int {
        return mBirate
    }

    fun setFPS(_fps: Int) {
        mFPS = if (_fps > 0 && _fps <= 30) _fps else 25
    }

    fun setBitrate(bitrate: Int) {
        mBirate = bitrate
    }

    fun setOrientation(orientation: Int) {
        mOrientation = orientation
    }

    fun getOrientation(): Int {
        return mOrientation
    }

    fun getOutputPath(): String? {
        return mOutputPath
    }

    fun getResolutionString(): String? {
        return mWidth.toString() + "x" + mHeight
    }

    fun setOutputPath(outputFile: String) {
        mOutputPath = outputFile
    }

    override fun toString(): String {
        return "VideoSetting{" +
                "mWidth=" + mWidth +
                ", mHeight=" + mHeight +
                ", mFPS=" + mFPS +
                ", mBirate=" + mBirate +
                ", mOrientation=" + mOrientation +
                '}'
    }


    fun getTitle(): String? {
        return if (!TextUtils.isEmpty(mOutputPath)) {
            mOutputPath!!.substring(mOutputPath!!.lastIndexOf('/'))
        } else "Title ERROR"
    }

    fun swapResolutionMatchToOrientation() {
        val x = mWidth
        val y = mHeight
        if (mOrientation == ORIENTATION_PORTRAIT && mWidth > mHeight ||
            mOrientation == ORIENTATION_LANDSCAPE && mWidth < mHeight
        ) {
            mWidth = mWidth + mHeight
            mHeight = mWidth - mHeight
            mWidth = mWidth - mHeight
        }
    }
}