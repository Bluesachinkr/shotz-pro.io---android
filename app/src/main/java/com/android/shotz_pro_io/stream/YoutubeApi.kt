package com.android.shotz_pro_io.stream

import com.google.api.client.util.DateTime
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class YoutubeApi {

    companion object {
        val RTMP_URL_KEY = "rtmpUrl"
        val BROADCAST_ID_KEY = "broadcastId"

        fun createLiveEvent(youtube: YouTube, youtubeTitle: String, youtubeDescription: String) {
            val dataFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
            dataFormat.timeZone = TimeZone.getTimeZone("UTC")
            val futureDateMilis = System.currentTimeMillis() + 5000
            val date = Date()
            date.time = futureDateMilis
            val dateCurrent = dataFormat.format(date)

            try {
                //set BroadCast
                val broadcastSnippet = LiveBroadcastSnippet()
                broadcastSnippet.title = youtubeTitle
                broadcastSnippet.description = youtubeDescription
                broadcastSnippet.scheduledStartTime = DateTime(date)

                val contentDetails = LiveBroadcastContentDetails()
                val monitorStreamInfo = MonitorStreamInfo()
                monitorStreamInfo.enableMonitorStream = false
                contentDetails.monitorStream = monitorStreamInfo

                //Set LiveBroadCast Status
                val liveBroadcastStatus = LiveBroadcastStatus()
                liveBroadcastStatus.setPrivacyStatus("unlisted")

                //set LiveBroadCast
                val liveBroadcast = LiveBroadcast()
                liveBroadcast.kind = "youtube#liveBroadCast"
                liveBroadcast.snippet = broadcastSnippet
                liveBroadcast.status = liveBroadcastStatus
                liveBroadcast.contentDetails = contentDetails

                //create the insert Request
                val liveBroadcastInsert =
                    youtube.liveBroadcasts().insert("snippet,status,contentDetails", liveBroadcast)

                //request for reqturned Broadcast
                val returnedBroadcast = liveBroadcastInsert.execute()

                //create Stream Snippet
                val streamSnippet = LiveStreamSnippet()
                streamSnippet.title = youtubeTitle

                //cdn settings
                val cdn = CdnSettings()
                cdn.format = "240p"
                cdn.ingestionType = "rtmp"

                val liveStream = LiveStream()
                liveStream.kind = "youtube#liveBroadCast"
                liveStream.snippet = streamSnippet
                liveStream.cdn = cdn

                //create the stream insert request
                val liveStreamInsert = youtube.liveStreams().insert("snippet,cdn", liveStream)

                //returned stream
                val returnedStream = liveStreamInsert.execute()

                //create the bind request
                val liveBroadcastBind =
                    youtube.liveBroadcasts().bind(returnedBroadcast.id, "id,contentDetails")

                //set stream id to bind
                liveBroadcastBind.streamId = returnedStream.id

                //returned bind
                liveBroadcastBind.execute()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getLiveEvents(youtube: YouTube): List<EventData> {
        val liveBroadcastRequest = youtube.liveBroadcasts().list("id,snippet,contentDetails")
        liveBroadcastRequest.broadcastStatus = "upcoming"

        val returnedBroadcastRequest = liveBroadcastRequest.execute()

        val returnedList = returnedBroadcastRequest.items

        val resultList = ArrayList<EventData>(returnedList.size)
        for (data in returnedList) {
            val streamId = data.contentDetails.boundStreamId
            if (streamId != null) {
                val eventData =
                    EventData(data, getIngestionAddress(youtube, data.contentDetails.boundStreamId))
                resultList.add(eventData)
            } else {
                val eventData =
                    EventData(data, "")
                resultList.add(eventData)
            }
        }
        return resultList
    }

    fun startEvent(youtube: YouTube, broadcastId: String) {
        try {
            Thread.sleep(1000)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        val transitionRequest = youtube.liveBroadcasts().transition("live", broadcastId, "status")
        transitionRequest.execute()
    }

    fun endEvent(youtube: YouTube, broadcastId: String) {
        val transitionRequest =
            youtube.liveBroadcasts().transition("completed", broadcastId, "status")
        transitionRequest.execute()
    }

    fun getIngestionAddress(youtube: YouTube, streamId: String): String {
        val liveStreamRequest = youtube.liveStreams().list("cdn")
        liveStreamRequest.id = streamId

        val returnedStreamRequest = liveStreamRequest.execute()

        val returnedList = returnedStreamRequest.items
        if (returnedList.isEmpty()) {
            return ""
        } else {
            val ingestionInfo = returnedList[0].cdn.ingestionInfo
            return ingestionInfo.ingestionAddress + "/" + ingestionInfo.streamName
        }
    }
}