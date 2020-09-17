package com.android.shotz_pro_io.controllers.rtmp

import android.media.projection.MediaProjection
import androidx.annotation.NonNull

class Streamer(@NonNull mediaProjection: MediaProjection) : VideoHandler.OnVideoEncoderStateListener,AudioHandler.OnAudioEncoderStateListener {

    private var mediaProjection: MediaProjection = mediaProjection

    private val videoHandler : VideoHandler
    private val audioHandler : AudioHandler
    private val muxer : Muxer

    init {
        videoHandler = VideoHandler(mediaProjection)
        audioHandler = AudioHandler()
        muxer = Muxer()
    }

    fun setMuxerListener(listener: PublisherListener?) {
        muxer.setOnMuxerStateListener(listener)
    }

    fun open(url : String ,width : Int,height : Int){
        muxer.open(url,width,height)
    }

    fun startStreaming(width : Int,height : Int,audioBitrate : Int,videBitrate : Int,density: Int){
        var t = 0
        while (!muxer.isConnected){
            try{
                t+=100
                Thread.sleep(100)
                if(t > 5000){
                    break
                }
            }catch (e : InterruptedException){
                e.printStackTrace()
            }
        }
        if(muxer.isConnected){
            val streamStartingAt = System.currentTimeMillis()
            videoHandler.setOnVideoEncoderStateListener(this)
            audioHandler.setOnAudioEncoderStateListener(this)
            videoHandler.start(width,height,videBitrate,streamStartingAt,density)
            audioHandler.start(audioBitrate,streamStartingAt)
        }
    }

    fun stopStreaming(){
        videoHandler.stop()
        audioHandler.stop()
        muxer.close()
    }

    fun isStreaming() : Boolean{
        return muxer.isConnected
    }

    override fun onVideoDataEncoded(data: ByteArray?, size: Int, timestamp: Int) {
        muxer.sendVideo(data,size,timestamp)
    }

    override fun onAudioDataEncoded(data: ByteArray?, size: Int, timestamp: Int) {
        muxer.sendAudio(data,size,timestamp)
    }

}