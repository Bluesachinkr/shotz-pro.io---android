package com.android.shotz_pro_io.stream

import android.app.Service
import android.content.Intent
import android.os.AsyncTask
import android.os.Handler
import android.os.IBinder
import com.android.shotz_pro_io.main.MainActivity
import com.google.api.services.youtube.YouTube

class SyncStreamService() : Service() {

    private lateinit var youTube : YouTube
    private lateinit var broadcastId: String
    constructor(youTube: YouTube,broadcastId : String) : this(){
        this.youTube = youTube
        this.broadcastId = broadcastId
    }

    companion object{
        var status : String = ""
    }

    override fun onCreate() {
        super.onCreate()
        Looper().run()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Looper().run()
        return super.onStartCommand(intent, flags, startId)
    }
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    inner class Looper : Thread(){
        val handler : Handler
        init {
            handler = Handler()
        }

        override fun run(){
            android.os.Looper.prepare()
            handler.post {
                this@SyncStreamService.CheckStatus().execute()
                if(status == "active"){
                    MainActivity?.mContext?.startEvent("")
                }else if(status == "live"){
                    println("Status is lIve")
                }
            }
        }
    }

    inner class CheckStatus : AsyncTask<Void,Void,Void?>(){

        override fun doInBackground(vararg p0: Void?): Void? {
            val liveBroadcast = youTube.liveBroadcasts().list("contentDetails")
            liveBroadcast.id = broadcastId
            val result = liveBroadcast.execute()
            val iteamBroadcast = result.items[0]
            val liveStreams : YouTube.LiveStreams.List = youTube.liveStreams().list("status")
            liveStreams.id = iteamBroadcast.contentDetails.boundStreamId
            var returnedLiveStreams = liveStreams.execute()
            var status= returnedLiveStreams.items[0]
            SyncStreamService.status = status.status.streamStatus
            return null
        }
    }
}