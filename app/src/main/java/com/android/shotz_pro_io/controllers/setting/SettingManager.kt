package com.android.shotz_pro_io.controllers.setting

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.android.shotz_pro_io.R

object SettingManager {
    fun getCountdown(context: Context): Int {
        val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val key = getStringRes(context, R.string.setting_common_countdown)
        val defValue = getStringRes(context, R.string.default_setting_countdown)
        val res = preferences.getString(key, defValue)
        return res!!.toInt()
    }

    private fun getOrientation(context: Context): String? {
        val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val key = getStringRes(context, R.string.setting_common_orientation)
        val defValue = getStringRes(context, R.string.default_setting_orientation)
        return preferences.getString(key, defValue)
    }

    private fun getVideoBitrate(context: Context): Int {
        val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val key = getStringRes(context, R.string.setting_video_bitrate)
        val defValue = getStringRes(context, R.string.default_setting_bitrate)
        val res = preferences.getString(key, defValue)
        return res!!.toInt()
    }

    private fun getVideoResolution(context: Context): String? {
        val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val key = getStringRes(context, R.string.setting_video_resolution)
        val defValue = getStringRes(context, R.string.default_setting_resolution)
        return preferences.getString(key, defValue)
    }

    private fun getVideoFPS(context: Context): Int {
        val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val key = getStringRes(context, R.string.setting_video_fps)
        val defValue = getStringRes(context, R.string.default_setting_fps)
        val res = preferences.getString(key, defValue)
        return res!!.toInt()
    }

    private fun getCameraMode(context: Context): String? {
        val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val key = getStringRes(context, R.string.setting_camera_mode)
        val defValue = getStringRes(context, R.string.default_camera_mode)
        return preferences.getString(key, defValue)
    }

    private fun getCameraPosition(context: Context): String? {
        val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val key = getStringRes(context, R.string.setting_camera_position)
        val defValue = getStringRes(context, R.string.default_camera_position)
        return preferences.getString(key, defValue)
    }

    private fun getCameraSize(context: Context): String? {
        val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val key = getStringRes(context, R.string.setting_camera_size)
        val defValue = getStringRes(context, R.string.default_camera_size)
        return preferences.getString(key, defValue)
    }

    private fun getStringRes(context: Context, resId: Int): String {
        return context.resources.getString(resId)
    }

    fun getVideoProfile(context: Context): VideoSetting? {
        var videoSetting: VideoSetting? = null
        val resolution = getVideoResolution(context)
        videoSetting = when (resolution) {
            "SD" -> VideoSetting.VIDEO_PROFILE_SD
            "HD" -> VideoSetting.VIDEO_PROFILE_HD
            "FHD" -> VideoSetting.VIDEO_PROFILE_FHD
            else -> VideoSetting.getFitDeviceResolution(context)
        }
        val fps = getVideoFPS(context)
        val bitrate = getVideoBitrate(context)
        if (bitrate != -1)
            videoSetting?.setBitrate(bitrate)
        val orientation = getOrientation(context)
        if (orientation == "Portrait") {
            videoSetting?.setOrientation(VideoSetting.ORIENTATION_PORTRAIT)
        } else videoSetting?.setOrientation(VideoSetting.ORIENTATION_LANDSCAPE)
        videoSetting?.swapResolutionMatchToOrientation()
        videoSetting?.setFPS(fps)
        return videoSetting
    }

    fun getCameraProfile(context: Context): CameraSetting {
        val size = getCameraSize(context)
        val mode = getCameraMode(context)
        val pos = getCameraPosition(context)
        return CameraSetting(mode!!, pos!!, size!!)
    }
}