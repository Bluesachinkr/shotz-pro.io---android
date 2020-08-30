package com.android.shotz_pro_io.main

import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.os.RecoverySystem
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.shotz_pro_io.R
import com.android.shotz_pro_io.adapter.VideoAdapter
import java.io.File
import java.lang.StringBuilder
import java.util.*

class VideosFragment(mContext: Context) : Fragment() {

    private val mContext = mContext
    private lateinit var videosRecyclerView: RecyclerView
    private lateinit var adapter: VideoAdapter

    private val videoList = mutableListOf<File>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_videos, container, false)

        videosRecyclerView = view.findViewById(R.id.videos_recycler)
        val layoutManager = LinearLayoutManager(mContext)
        videosRecyclerView.layoutManager = layoutManager
        adapter = VideoAdapter(mContext, videoList)
        videosRecyclerView.adapter = adapter
        loadFiles()
        return view
    }

    private fun loadFiles() {
        val builder = StringBuilder(Environment.getExternalStorageDirectory().toString())
        builder.append(File.separator)
        builder.append(R.string.app_name)
        builder.append(File.separator)
        builder.append("Video Captures")
        val dir = File(builder.toString())
        if (!dir.exists()) {
            return
        }
        val list: Array<File> = dir.listFiles()
        for (l in list) {
            if(l.length() >0){
            videoList.add(l)
            }
        }
        Collections.reverse(videoList)
        adapter.notifyDataSetChanged()
    }

}