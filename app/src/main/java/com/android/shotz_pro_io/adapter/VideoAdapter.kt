package com.android.shotz_pro_io.adapter

import android.content.Context
import android.media.ThumbnailUtils
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.shotz_pro_io.R
import com.android.shotz_pro_io.main.MainActivity
import com.android.shotz_pro_io.stream.EventData
import com.bumptech.glide.Glide
import com.google.android.gms.common.api.GoogleApiClient
import com.google.api.client.googleapis.services.AbstractGoogleClient
import java.io.File

class VideoAdapter(
    mContext: Context,
    mVideos: MutableList<EventData>,
    callback: MainActivity.CallbackVideo,
    googleClient: GoogleApiClient
) :
    RecyclerView.Adapter<VideoAdapter.Viewholder>() {

    private val mContext = mContext
    private val mVideos = mVideos
    private val callback = callback
    private val googleClient = googleClient

    class Viewholder(itemview: View) : RecyclerView.ViewHolder(itemview) {
        val videoThumbnail: ImageView = itemview.findViewById(R.id.videoThumbnail)
        val file_name: TextView = itemview.findViewById(R.id.file_name)
        val file_details: TextView = itemview.findViewById(R.id.file_details)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Viewholder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.videos_item, parent, false)
        return Viewholder(view)
    }

    override fun onBindViewHolder(holder: Viewholder, position: Int) {
        val event = mVideos[position]
        holder.file_name.text = event.getTitle()
        holder.file_details.text = event.getWatchUri()
        holder.itemView.setOnClickListener {
            callback.onEventSelected(event)
        }
    }

    override fun getItemCount(): Int {
        return mVideos.size
    }
}