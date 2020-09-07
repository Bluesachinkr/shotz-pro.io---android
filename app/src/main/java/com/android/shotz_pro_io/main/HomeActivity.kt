package com.android.shotz_pro_io.main

import android.accounts.AccountManager
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.android.shotz_pro_io.R
import com.android.shotz_pro_io.screenCapture.CapturingBallService
import com.android.shotz_pro_io.stream.EventData
import com.android.shotz_pro_io.stream.StreamingActivity
import com.android.shotz_pro_io.stream.YoutubeApi
import com.google.android.gms.auth.api.signin.GoogleSignInApi
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.material.tabs.TabLayout
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeScopes
import com.mikhaellopez.circularimageview.CircularImageView
import java.util.*


class HomeActivity : AppCompatActivity() {
    private val PERMISSION_CODE = 208
    private val CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2048

    private val transport = NetHttpTransport()
    private val jsonFactory = JacksonFactory()
    private var credential: GoogleAccountCredential? = null

    private val permission = arrayOf<String>(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.RECORD_AUDIO,android.Manifest.permission.GET_ACCOUNTS
    )

    private val selectedVideoCallback: CallbackVideo = object : CallbackVideo {
        override fun onEventSelected(event: EventData) {
            startStreaming(event)
        }
    }

    private var listFragment: Fragment = Fragment()

    companion object {
        var mContext: HomeActivity? = null
        var SCOPES = mutableListOf(Scopes.PROFILE, YouTubeScopes.YOUTUBE)
        val ACCOUNT_KEY = "accountName"
        val APP_NAME = "Shotz Pro"
        var accountName = ""
        val REQUEST_PLAY_SERVICES = 0
        val REQUEST_GMS_ERROR_DIALOG = 1
        val REQUEST_GMS_ACCOUNT_PICKER = 2
        val REQUEST_AUTH = 3
        val REQUEST_STREAMER = 4

        fun getYoutubeService(): YouTube {
            return YouTube.Builder(mContext?.transport, mContext?.jsonFactory, mContext?.credential)
                .setApplicationName(
                    APP_NAME
                ).build()
        }
    }


    private lateinit var mViewPager: ViewPager
    private lateinit var mTabLayout: TabLayout
    private lateinit var createLiveEvent: Button
    private lateinit var accountImageLayout: RelativeLayout
    private lateinit var accountImage: CircularImageView
    private lateinit var profileName: TextView

    private val fragList = mutableListOf<Fragment>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        //set activity
        mContext = this



        getGoogleApiConnection(savedInstanceState)

        this.mViewPager = findViewById(R.id.viewPager_main)
        this.mTabLayout = findViewById(R.id.tabLayout_main)
        this.accountImageLayout = findViewById(R.id.accountImagelayout)
        this.accountImage = findViewById(R.id.accountImage)
        this.createLiveEvent = findViewById(R.id.createLiveEvent)
        this.profileName = findViewById(R.id.accountName)

        this.createLiveEvent.setOnClickListener {
            createLiveEvent(it)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION)
        } else {
            if (!CapturingBallService.isBallOpen) {
                val intentBall = Intent(this, CapturingBallService::class.java)
                startService(intentBall)
                CapturingBallService.isBallOpen = true
            }
        }

        if (!hasPermissions(permission)) {
            ActivityCompat.requestPermissions(this, permission, PERMISSION_CODE)
        }
       /* this.fragList.add(
            VideosFragment(
                this,
                selectedVideoCallback,
                accountImage,
                profileName,
                selectedVideoCallback
            )
        )*/
        this.fragList.add(StreamFragment())
        this.fragList.add(SettingsFragment(this))
        this.mViewPager.adapter = Adapter(supportFragmentManager, this.fragList)
        mTabLayout.setupWithViewPager(mViewPager)
        mTabLayout.setOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(p0: TabLayout.Tab?) {
                p0?.let {
                    mViewPager.setCurrentItem(it.position)
                }
            }

            override fun onTabUnselected(p0: TabLayout.Tab?) {

            }

            override fun onTabReselected(p0: TabLayout.Tab?) {

            }
        })
        mViewPager.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {

            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })

        mTabLayout.getTabAt(0)?.setText("Videos")
        mTabLayout.getTabAt(1)?.setText("Stream")
        mTabLayout.getTabAt(2)?.setText("Settings")

        supportFragmentManager?.let {
            listFragment = fragList[0]
        }
    }

    private fun getGoogleApiConnection(savedInstanceState: Bundle?) {
        //google connection api
        credential = GoogleAccountCredential.usingOAuth2(this, SCOPES).also {
            it.setBackOff(ExponentialBackOff())
        }

        if (savedInstanceState != null) {
            accountName = savedInstanceState.getString(ACCOUNT_KEY) as String
        } else {
            loadAccount()
        }

        credential?.setSelectedAccountName(accountName)
    }

    private fun loadAccount() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val name = sharedPreferences.getString(ACCOUNT_KEY, null)
        name?.let {
            accountName = it
        }
    }

    private fun saveAccount() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.edit().putString(ACCOUNT_KEY, accountName).apply()
    }

    private fun loadData() {
        if (accountName == null) {
            return
        }
        getLiveEvents()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_GMS_ERROR_DIALOG -> {
                return
            }
            REQUEST_PLAY_SERVICES -> {
                if (resultCode == Activity.RESULT_OK) {
                    haveGooglePlayServices()
                } else {
                    checkGoogleplayServiceeAvailable()
                }
            }
            REQUEST_AUTH -> {
                if (resultCode != RESULT_OK) {
                    chooseAccount()
                }
            }
            REQUEST_GMS_ACCOUNT_PICKER -> {
                if (resultCode == RESULT_OK && data != null && data.extras != null) {
                    val name = data.extras!![AccountManager.KEY_ACCOUNT_NAME].toString()
                    name?.let {
                        accountName = it
                        credential?.setSelectedAccountName(it)
                        saveAccount()
                    }
                }
            }
            REQUEST_STREAMER -> {
                if (resultCode == RESULT_OK && data != null && data.extras != null) {
                    val Id = data.extras!![YoutubeApi.BROADCAST_ID_KEY].toString()
                    Id?.let {
                        EndLiveEventTask().execute(it)
                    }
                }
            }
            else -> {
                return
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ACCOUNT_KEY, accountName)
    }


    private fun chooseAccount() {
        credential?.let {
            startActivityForResult(it.newChooseAccountIntent(), REQUEST_GMS_ACCOUNT_PICKER)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CapturingBallService.isBallOpen = false
    }

    class Adapter(fm: FragmentManager, list: MutableList<Fragment>) :
        FragmentStatePagerAdapter(fm) {
        private val list = list
        override fun getCount(): Int {
            return list.size
        }

        override fun getItem(position: Int): Fragment {
            return list[position]
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE) {
            for (p in permissions) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        p
                    ) == PackageManager.PERMISSION_DENIED
                ) {
                    finish()
                }
            }
        }
    }

    private fun hasPermissions(permission: Array<String>): Boolean {
        for (p in permission) {
            if (ActivityCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_DENIED) {
                return false
            }
        }
        return true
    }

    fun startStreaming(event: EventData) {
        val broadcastId = event.getId()
        StartEventTask().execute(broadcastId)
        val intent = Intent(this, StreamingActivity::class.java).also {
            it.putExtra(YoutubeApi.RTMP_URL_KEY, event.mIngestionAddress)
            it.putExtra(YoutubeApi.BROADCAST_ID_KEY, broadcastId)
        }
        startActivityForResult(intent, REQUEST_STREAMER)
    }

    fun getLiveEvents() {
        if (accountName == null) {
            return
        }
        GetLiveEventTask().execute(listFragment)
    }

    fun createLiveEvent(view: View) {
        CreateLiveEventTask().execute()
    }

    private fun showGooglePlayServicesAvailibilityErrorDailog(connectionStatusCode: Int) {
        runOnUiThread {
            val dialog = GooglePlayServicesUtil.getErrorDialog(
                connectionStatusCode, this,
                REQUEST_PLAY_SERVICES
            )
            dialog.show()
        }
    }

    /*
    * Check that google Play services is installed and uop to date
    * */

    private fun checkGoogleplayServiceeAvailable(): Boolean {
        val connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailibilityErrorDailog(connectionStatusCode)
            return false
        }
        return true
    }

    private fun haveGooglePlayServices() {
        //check there is already a account selected
        if (credential?.selectedAccountName == null) {
            chooseAccount()
        }
    }

    private class StartEventTask : AsyncTask<String, Void, Void?>() {
        private var progressDialog: ProgressDialog? = null

        override fun onPreExecute() {
            mContext?.let {
                progressDialog = ProgressDialog.show(
                    it,
                    null,
                    it.resources.getText(R.string.startingEvent),
                    true
                )
            }
        }

        override fun doInBackground(vararg params: String?): Void? {
            mContext?.let {
                val youtube = getYoutubeService()
                try {
                    params[0]?.let {
                        YoutubeApi.startEvent(youtube, it)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return null
        }

        override fun onPostExecute(result: Void?) {
            progressDialog?.dismiss()
        }
    }

    private class GetLiveEventTask : AsyncTask<Fragment, Void, List<EventData>?>() {

        private var progressDailog: ProgressDialog? = null
        private var fragment: Fragment? = null
        override fun onPreExecute() {
            mContext?.let {
                progressDailog = ProgressDialog.show(
                    it,
                    null,
                    it.resources.getText(R.string.loadingEvents),
                    true
                )
            }
        }

        override fun doInBackground(vararg params: Fragment?): List<EventData>? {
            params[0]?.let {
                fragment = it
            }
            mContext?.let {
                val youtube = getYoutubeService()
                try {
                    return YoutubeApi.getLiveEvents(youtube)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return null
        }

        override fun onPostExecute(result: List<EventData>?) {
            result?.let {
                (fragment as VideosFragment).setEvents(it)
            }
            progressDailog?.dismiss()
        }
    }

    private class CreateLiveEventTask : AsyncTask<Void, Void, List<EventData>?>() {

        private var progressDailog: ProgressDialog? = null
        override fun onPreExecute() {
            mContext?.let {
                progressDailog = ProgressDialog.show(
                    it,
                    null,
                    it.resources.getText(R.string.creatingEvent),
                    true
                )
            }
        }

        override fun doInBackground(vararg params: Void?): List<EventData>? {
            mContext?.let {
                val youtube = getYoutubeService()
                try {
                    val date = Date().toString()
                    YoutubeApi.createLiveEvent(
                        youtube, "Event - " + date,
                        "A live streaming event - " + date
                    )
                    return YoutubeApi.getLiveEvents(youtube)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return null
        }

        override fun onPostExecute(result: List<EventData>?) {
            //set create live button

            progressDailog?.dismiss()
        }
    }

    private class EndLiveEventTask : AsyncTask<String, Void, Void?>() {
        private var progressDialog: ProgressDialog? = null

        override fun onPreExecute() {
            mContext?.let {
                progressDialog = ProgressDialog.show(
                    it,
                    null,
                    it.resources.getText(R.string.endingEvent),
                    true
                )
            }
        }

        override fun doInBackground(vararg params: String?): Void? {
            mContext?.let {
                val youTube = getYoutubeService()
                try {
                    params[0]?.let {
                        if (params.size >= 1) {
                            YoutubeApi.endEvent(youTube, it)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return null
        }

        override fun onPostExecute(result: Void?) {
            progressDialog?.dismiss()
        }
    }


    interface CallbackVideo {
        fun onEventSelected(event: EventData)
    }
}