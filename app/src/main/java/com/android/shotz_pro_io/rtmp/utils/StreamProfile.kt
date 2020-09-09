package com.android.shotz_pro_io.rtmp.utils

import java.io.Serializable

class StreamProfile(var id: String, var streamUrl: String?, var secureStreamUrl: String) :
    Serializable {
    private var mStreamUrl: String? = null
    var title: String? = null
    var description: String? = null
    var host: String? = null
    var app: String? = null
    var playPath: String? = null
    var port = 0

    private fun updateHostPortPlayPath() {
        host = "live-api-s.facebook.com"
        port = 80
        app = "rtmp"
        playPath = mStreamUrl!!.substring(mStreamUrl!!.lastIndexOf('/') + 1)
    }

    override fun toString(): String {
        return "StreamProfile{" +
                "mId='" + id + '\'' +
                ", mStreamUrl='" + mStreamUrl + '\'' +
                ", mSecureStreamUrl='" + secureStreamUrl + '\'' +
                ", mTitle='" + title + '\'' +
                ", mDescription='" + description + '\'' +
                ", mHost='" + host + '\'' +
                ", mApp='" + app + '\'' +
                ", mPlayPath='" + playPath + '\'' +
                ", mPort=" + port +
                '}'
    }
}
