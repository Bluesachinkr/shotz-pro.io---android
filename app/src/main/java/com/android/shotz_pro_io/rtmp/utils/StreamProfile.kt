package com.android.shotz_pro_io.rtmp.utils

import com.google.api.services.youtube.YouTube

class StreamProfile {
    companion object{
        var youTube : YouTube? = null
        var rtmpUrl : String = ""
        var broadcastKey : String = ""
        var streamId : String = ""
        var isStreaming : Boolean = false
    }
}