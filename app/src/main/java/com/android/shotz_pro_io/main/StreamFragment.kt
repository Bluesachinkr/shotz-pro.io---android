package com.android.shotz_pro_io.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.android.shotz_pro_io.R
import com.android.shotz_pro_io.controllers.rtmp.utils.StreamProfile

class StreamFragment(listener: StreamCallbackListener) : Fragment() {

    private val listener = listener
    private lateinit var startStreamBtn: Button
    private lateinit var statusStream: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stream, container, false)
        startStreamBtn = view.findViewById(R.id.startStream_btn)
        statusStream = view.findViewById(R.id.status_stream)

        startStreamBtn.setOnClickListener {
            if (StreamProfile.isStreaming) {
                listener.stopStream()
                StreamProfile.isStreaming = false
            } else {
                listener.startStream()
                StreamProfile.isStreaming = true
            }
        }
        return view
    }
}