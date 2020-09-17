package com.android.shotz_pro_io.controllers.setting

import android.view.Gravity


class CameraSetting(
    var mode: String,
    var mPosition: String,
    var size: String
) {

    val paramGravity: Int
        get() = when (mPosition) {
            POSITION_BOTTOM_LEFT -> Gravity.BOTTOM or Gravity.START
            POSITION_BOTTOM_RIGHT -> Gravity.BOTTOM or Gravity.END
            POSITION_TOP_LEFT -> Gravity.TOP or Gravity.START
            POSITION_TOP_RIGHT -> Gravity.TOP or Gravity.END
            else -> Gravity.CENTER
        }

    companion object {
        const val CAMERA_MODE_FRONT = "Front"
        const val CAMERA_MODE_BACK = "Back"
        const val CAMERA_MODE_OFF = "Off"
        const val POSITION_TOP_LEFT = "Top Left"
        const val POSITION_TOP_RIGHT = "Top Right"
        const val POSITION_BOTTOM_LEFT = "Bottom Left"
        const val POSITION_BOTTOM_RIGHT = "Bottom Right"
        const val POSITION_CENTER = "Center"
        const val SIZE_BIG = "Big"
        const val SIZE_MEDIUM = "Medium"
        const val SIZE_SMALL = "Small"
    }
}