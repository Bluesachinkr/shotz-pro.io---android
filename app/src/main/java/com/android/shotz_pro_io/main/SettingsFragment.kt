package com.android.shotz_pro_io.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.android.shotz_pro_io.LoginActivity
import com.android.shotz_pro_io.R

class SettingsFragment(mContext : Context) : Fragment() {

    private val mContext = mContext
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        val btn = view.findViewById<Button>(R.id.login)
        btn.setOnClickListener {
            mContext.startActivity(Intent(mContext,LoginActivity::class.java))
        }
        return view
    }

}