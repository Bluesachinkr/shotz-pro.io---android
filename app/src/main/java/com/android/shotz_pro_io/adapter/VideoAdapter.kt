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
import java.io.File

class VideoAdapter(mContext: Context, mVideos: MutableList<File>) :
    RecyclerView.Adapter<VideoAdapter.Viewholder>() {

    private val mContext = mContext
    private val mVideos = mVideos

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
        val file = mVideos[position]
        holder.file_name.text = file.name.toString()

        val size = (file.length() / 1000)
        val sizeStr = StringBuffer(size.toString())
        sizeStr.append(" Kb")
        holder.file_details.text = sizeStr.toString()

        val bitmap = ThumbnailUtils.createVideoThumbnail(
            file.absolutePath,
            MediaStore.Video.Thumbnails.MINI_KIND
        )
        holder.videoThumbnail.setImageBitmap(bitmap)
    }

    override fun getItemCount(): Int {
        return mVideos.size
    }
}