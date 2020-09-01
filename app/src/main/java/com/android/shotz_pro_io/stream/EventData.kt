package com.android.shotz_pro_io.stream

import com.google.api.services.youtube.model.LiveBroadcast

data class EventData(
    var mEvent: LiveBroadcast,
    var mIngestionAddress: String
) {
    fun getId(): String {
        return mEvent.id
    }

    fun getThumbUri(): String {
        var url = mEvent.snippet.thumbnails.default.url
        if (url.startsWith("//")) {
            url = "https:" + url
        }
        return url
    }

    fun getWatchUri(): String {
        return "http://www.youtube.com/watch?v=" + getId();
    }
}