package com.android.shotz_pro_io.stream

import com.google.api.client.util.DateTime
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.*
import java.text.SimpleDateFormat
import java.util.*

class YoutubeApi {

    companion object{
        val RTMP_URL_KEY = "rtmpUrl"
        val BROADCAST_ID_KEY = "broadcastId"

        fun createLiveEvent(youtube : YouTube,youtubeTitle : String,youtubeDescription: String){
            val dataFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
            dataFormat.timeZone = TimeZone.getTimeZone("UTC")
            val futureDateMilis = System.currentTimeMillis()+ 5000
            val date = Date()
            date.time = futureDateMilis
            val dateCurrent = dataFormat.format(date)

            try{
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
                val liveBroadcastInsert = youtube.liveBroadcasts().insert("snippet,status,contentDetails",liveBroadcast)

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
                val liveStreamInsert = youtube.liveStreams().insert("snippet,cdn",liveStream)

                //returned stream
                val returnedStream = liveStreamInsert.execute()

                //create the bind request
                val liveBroadcastBind = youtube.liveBroadcasts().bind(returnedBroadcast.id,"id,contentDetails")

                //set stream id to bind
                liveBroadcastBind.streamId = returnedStream.id

                //returned bind
                liveBroadcastBind.execute()
            }catch (e : Exception){
                e.printStackTrace()
            }
        }
    }
}