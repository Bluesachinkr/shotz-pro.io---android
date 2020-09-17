package com.android.shotz_pro_io.controllers.rtmp.utils

import com.google.api.services.youtube.YouTube

class StreamProfile {
    companion object{
        var streamName : String = ""
        var youTube : YouTube? = null
        var rtmpUrl : String = ""
        var broadcastKey : String = ""
        var streamId : String = ""
        var lievChatId : String = ""
        var isStreaming : Boolean = false
    }
}