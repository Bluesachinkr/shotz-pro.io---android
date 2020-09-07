package com.android.shotz_pro_io.main

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.shotz_pro_io.R
import com.android.shotz_pro_io.adapter.VideoAdapter
import com.android.shotz_pro_io.stream.EventData
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.plus.Plus
import kotlinx.android.synthetic.main.activity_home.*
import java.io.File

class VideosFragment(
    mContext: Context,
    accountImage: ImageView,
    profileName: TextView,
    callbacks: MainActivity.CallbackVideo,googleApiClient: GoogleApiClient
) : Fragment(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private val mContext = mContext
    private lateinit var videosRecyclerView: RecyclerView
    private val accountImage = accountImage
    private val profileName = profileName
    private lateinit var adapter: VideoAdapter
    private var mEvents = mutableListOf<EventData>()
    private val callbacks = callbacks
    private val mGoogleApiClient = googleApiClient

    private val videoList = mutableListOf<File>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_videos, container, false)
        videosRecyclerView = view.findViewById(R.id.videos_recycler)
        adapter = VideoAdapter(mContext,mEvents,callbacks,mGoogleApiClient)
        val layoutManager = LinearLayoutManager(mContext)
        videosRecyclerView.layoutManager = layoutManager
        videosRecyclerView.adapter = adapter
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onConnected(p0: Bundle?) {
        if (videosRecyclerView.adapter != null) {
            adapter.notifyDataSetChanged()
        }
    }

    override fun onConnectionSuspended(p0: Int) {
    }

    override fun onConnectionFailed(result: ConnectionResult) {
        if (result.hasResolution()) {
            try {
                result.startResolutionForResult(activity, 0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    open fun setEvents(eventList: List<EventData>) {
        if (!isAdded) {
            return
        }
        mEvents = eventList as MutableList<EventData>
        adapter = VideoAdapter(mContext,mEvents,callbacks,mGoogleApiClient)
        videosRecyclerView.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    /*private fun loadFiles() {
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
    }*/
}