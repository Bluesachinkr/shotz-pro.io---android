package com.android.shotz_pro_io

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        val RECORD_SCREEN = 1
    }

    override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val from = it.extras?.get("from") as Int
                when(from){
                    RECORD_SCREEN->{
                        startStopCapturing(context)
                        Toast.makeText(context,"Done",Toast.LENGTH_SHORT).show()
                    }
                }

            }
    }

    private fun startStopCapturing(context: Context?){
        ScreenCaptureActivity.getInstance()?.let {
            val b = (it as ScreenCaptureActivity).toggle_button_screen_capturing.isChecked
            if(b){
                (it as ScreenCaptureActivity).toggle_button_screen_capturing.isChecked =false
            }else{
                (it as ScreenCaptureActivity).toggle_button_screen_capturing.isChecked = true
            }
            (it as ScreenCaptureActivity).onToggleScreenShare()
        }
        Toast.makeText(context,"Done",Toast.LENGTH_SHORT).show()
    }
}