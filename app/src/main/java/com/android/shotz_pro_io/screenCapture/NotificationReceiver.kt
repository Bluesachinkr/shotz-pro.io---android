package com.android.shotz_pro_io.screenCapture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Toast
import com.android.shotz_pro_io.R

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        val RECORD_SCREEN = 1
        val PLAY_PAUSE = 2
        val START_STOP = 3
        val STOP = 4
        val PURPOSE = "Purpose" as String
    }

    override fun onReceive(context: Context?, recieve: Intent?) {
        recieve?.let{
            val from = it?.extras?.get("from") as Int
            val purpose = it?.extras?.get(PURPOSE) as Int
            when (from) {
                RECORD_SCREEN -> {
                    captureControl(context,purpose)
                    Toast.makeText(context, "Done", Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    private fun captureControl(context: Context?, purpose: Int) {
        ScreenRecordingActivity.getInstance()?.let {
            if (it.isRecording) {
                if (purpose == PLAY_PAUSE) {
                    playAndResumeRecordingControl(it)
                } else {
                    stopRecordingControl(it)
                }
            } else {
                //start recording
                startRecordingControl(it)
            }
            it.updateNotification()
        }
        Toast.makeText(context, "Done", Toast.LENGTH_SHORT).show()
    }

    private fun playAndResumeRecordingControl(activity: ScreenRecordingActivity) {
        if (activity.isPause) {
            activity.mCollapsedNotificationView.setImageViewResource(
                R.id.play_pause_btn,
                R.drawable.ic_pause_recording
            )
            activity.resumeScreenCapturing()
            activity.isPause = false
        } else {
            activity.mCollapsedNotificationView.setImageViewResource(
                R.id.play_pause_btn,
                R.drawable.ic_play_recording
            )
            activity.pauseScreenCapturing()
            activity.isPause = true
        }
    }

    private fun stopRecordingControl(activity: ScreenRecordingActivity) {
        activity.toggle_button_screen_capturing.isChecked = false

        //visibility
        activity.mCollapsedNotificationView.setViewVisibility(
            R.id.start_btn_notification,
            View.VISIBLE
        )
        activity.mCollapsedNotificationView.setViewVisibility(R.id.stop_btn_notification, View.GONE)
        activity.mCollapsedNotificationView.setViewVisibility(R.id.video_btn, View.VISIBLE)
        activity.mCollapsedNotificationView.setViewVisibility(R.id.stop_recording_btn, View.GONE)

        activity.mCollapsedNotificationView.setImageViewResource(
            R.id.play_pause_btn,
            R.drawable.ic_play_recording
        )
        activity.onToggleScreenShare()
        activity.isRecording = false
    }

    private fun startRecordingControl(activity: ScreenRecordingActivity) {
        activity.toggle_button_screen_capturing.isChecked = true

        //visibilty
        activity.mCollapsedNotificationView.setViewVisibility(
            R.id.start_btn_notification,
            View.GONE
        )
        activity.mCollapsedNotificationView.setViewVisibility(
            R.id.stop_btn_notification,
            View.VISIBLE
        )
        activity.mCollapsedNotificationView.setViewVisibility(R.id.video_btn, View.GONE)
        activity.mCollapsedNotificationView.setViewVisibility(R.id.stop_recording_btn, View.VISIBLE)

        activity.mCollapsedNotificationView.setImageViewResource(
            R.id.play_pause_btn,
            R.drawable.ic_pause_recording
        )
        activity.onToggleScreenShare()
        activity.isRecording = true
    }
}