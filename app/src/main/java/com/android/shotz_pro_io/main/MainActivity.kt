package com.android.shotz_pro_io.main

import android.accounts.AccountManager
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.android.shotz_pro_io.R
import com.android.shotz_pro_io.controllers.rtmp.utils.StreamProfile
import com.android.shotz_pro_io.stream.EventData
import com.android.shotz_pro_io.stream.StreamingActivity
import com.android.shotz_pro_io.stream.YoutubeApi
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeScopes
import com.google.api.services.youtube.model.LiveBroadcast
import com.mikhaellopez.circularimageview.CircularImageView
import org.json.JSONObject
import java.util.*


class MainActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener,
    GoogleApiClient.ConnectionCallbacks, StreamCallbackListener {

    private val PERMISSION_CODE = 201
    private var event: EventData? = null

    private lateinit var container: FrameLayout
    private lateinit var accountImage: CircularImageView
    private lateinit var bottomBar: BottomNavigationView
    private lateinit var gso: GoogleSignInOptions
    private lateinit var googleSignInClient: GoogleApiClient

    private lateinit var credential: GoogleAccountCredential
    private lateinit var listFragment: Fragment
    open var mChooseAccuntName: String? = null
    private val permission = arrayOf<String>(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.GET_ACCOUNTS
    )
    private val selectedEventCallback: CallbackVideo = object : CallbackVideo {
        override fun onEventSelected(event: EventData) {
            startStreaming()
            //deleteStreaming(event)
        }
    }

    companion object {
        val scopes = mutableListOf(Scopes.PROFILE, YouTubeScopes.YOUTUBE)
        val ACCOUNT_KEY = "Account key"
        var youSelect: YouTube? = null
        var mContext: MainActivity? = null
        val PREF_ACCOUNT_NAME = "accountName"
        val SIGN_IN = 500
        val REQUEST_ACCOUNT_PICKER = 1000
        val REQUEST_AUTHORIZATION = 1001
        val REQUEST_GOOGLE_PLAY_SERVICES = 1002
        val REQUEST_PERMISSION_GET_ACCOUNTS = 1003
        val REQUEST_STREAMER = 1005
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mContext = this

        this.accountImage = findViewById(R.id.accountImageMain)
        this.container = findViewById(R.id.containerMain)
        this.bottomBar = findViewById(R.id.bottomMain)

        if (!hasPermissions(permission)) {
            ActivityCompat.requestPermissions(this, permission, PERMISSION_CODE)
        }

        credential =
            GoogleAccountCredential.usingOAuth2(this, scopes).setBackOff(ExponentialBackOff())

        val transport = NetHttpTransport()
        val jsonFactory = JacksonFactory.getDefaultInstance()
        val youTube = YouTube.Builder(transport, jsonFactory, credential)
            .setApplicationName(HomeActivity.APP_NAME).build()
        StreamProfile.youTube = youTube

        //google sign in
        gso =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build()
        googleSignInClient = GoogleApiClient.Builder(this)
            .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
            .addConnectionCallbacks(this)
            .setAccountName(mChooseAccuntName)
            .addOnConnectionFailedListener(this)
            .build()

        signIn()

        getApiRequest()
        listFragment = VideosFragment(
            this,
            accountImage,
            selectedEventCallback,
            googleSignInClient
        )
        supportFragmentManager.beginTransaction().add(R.id.containerMain, listFragment).commit()

        youSelect =
            YouTube.Builder(NetHttpTransport(), JacksonFactory.getDefaultInstance(), credential)
                .setApplicationName(HomeActivity.APP_NAME).build()

        bottomBar.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.recording -> {
                    val fragment = VideosFragment(
                        this,
                        accountImage,
                        selectedEventCallback,
                        googleSignInClient
                    )
                    supportFragmentManager?.beginTransaction()
                        ?.replace(R.id.containerMain, fragment).addToBackStack(null).commit()
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.streaming -> {
                    val fragment = StreamFragment(this)
                    supportFragmentManager?.beginTransaction()
                        ?.replace(R.id.containerMain, fragment).addToBackStack(null).commit()
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.settings -> {
                    val fragment = SettingsFragment(this)
                    supportFragmentManager?.beginTransaction()
                        ?.replace(R.id.containerMain, fragment).addToBackStack(null).commit()
                    return@setOnNavigationItemSelectedListener true
                }
                else -> {
                    return@setOnNavigationItemSelectedListener false
                }
            }
        }
    }

    private fun signIn() {
        val intent = Auth.GoogleSignInApi.getSignInIntent(googleSignInClient)
        startActivityForResult(intent, SIGN_IN)
    }

    override fun onStart() {
        super.onStart()
        googleSignInClient.connect()
    }

    override fun onStop() {
        super.onStop()
        googleSignInClient.disconnect()
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
                    ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA),PERMISSION_CODE)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            SIGN_IN -> {
                val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
                result?.let {
                    handleSignIn(it)
                }
            }
            REQUEST_GOOGLE_PLAY_SERVICES -> {
                if (resultCode != RESULT_OK) {
                    Toast.makeText(this, "Google Play Services is not installed", Toast.LENGTH_LONG)
                        .show()
                } else {
                    getApiRequest()
                }
            }
            REQUEST_ACCOUNT_PICKER -> {
                if (resultCode == RESULT_OK && data != null && data.extras != null) {
                    val name = data.extras!![AccountManager.KEY_ACCOUNT_NAME].toString()
                    name?.let {
                        mChooseAccuntName = it
                        credential?.setSelectedAccountName(it)
                        saveAccount()
                        getApiRequest()
                    }
                }
            }
            REQUEST_AUTHORIZATION -> {
                if (resultCode == RESULT_OK) {
                    getApiRequest()
                }
            }
            REQUEST_STREAMER -> {
                if (resultCode == RESULT_OK && data != null && data.extras != null) {
                    val Id = data.extras!![YoutubeApi.BROADCAST_ID_KEY].toString()
                    Id?.let {
                        EndLiveEventTask().execute()
                    }
                }
            }
        }
    }

    private fun handleSignIn(result: GoogleSignInResult) {
        val account = result.signInAccount
        account?.let {
            Glide.with(this).load(it.photoUrl).into(accountImage)
        }
    }

    private fun saveAccount() {
        val sharedPreferences = getPreferences(MODE_PRIVATE)
        sharedPreferences.edit().putString(PREF_ACCOUNT_NAME, mChooseAccuntName).apply()
    }

    private fun hasPermissions(permission: Array<String>): Boolean {
        for (p in permission) {
            if (ActivityCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_DENIED) {
                return false
            }
        }
        return true
    }

    private fun getApiRequest() {
        if (!isGoogleServicesAvailable()) {
            acquireGooglePlayServices()
        } else if (mChooseAccuntName == null) {
            chooseAccount()
        }
    }

    private fun chooseAccount() {
        if (ActivityCompat.checkSelfPermission(
                this,
                permission[3]
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val name = getPreferences(MODE_PRIVATE).getString(AccountManager.KEY_ACCOUNT_NAME, null)
            if (name != null) {
                credential.setSelectedAccountName(name)
                getApiRequest()
            } else {
                startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER)
            }
        } else {
            ActivityCompat.requestPermissions(this, permission, PERMISSION_CODE)
        }
    }

    fun startStreaming() {
        val intent = Intent(this, StreamingActivity()::class.java).also {
            it.putExtra(YoutubeApi.RTMP_URL_KEY, StreamProfile.rtmpUrl)
            it.putExtra(YoutubeApi.BROADCAST_ID_KEY, StreamProfile.broadcastKey)
        }
        startActivityForResult(intent, HomeActivity.REQUEST_STREAMER)
    }

    fun endEvent(broadcastId: String) {
        EndLiveEventTask().execute()
    }

    fun deleteEvent(broadcastId: String) {
        StreamProfile.youTube?.let {
            DeleteEventTask(it).execute(broadcastId)
        }
    }

    private fun acquireGooglePlayServices() {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this)
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode)
        }
    }

    private fun showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode: Int) {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val dialog: Dialog = apiAvailability.getErrorDialog(
            this@MainActivity,
            connectionStatusCode,
            REQUEST_GOOGLE_PLAY_SERVICES
        )
        dialog.show()
    }

    private fun loadStreamProfile(youtube: YouTube, result: LiveBroadcast) {
        val BindResult = JSONObject(result)
        StreamProfile.broadcastKey = BindResult["id"] as String
        val cd = BindResult["contentDetails"] as JSONObject
        StreamProfile.streamId = cd["boundStreamId"] as String
        val liveBroadcastObject = youtube.liveBroadcasts().list("id,snippet")
        liveBroadcastObject.id = StreamProfile.broadcastKey
        val liveBroadcastResult = liveBroadcastObject.execute()
        val liveBroadcastItem = liveBroadcastResult.items[0]
        val liveBroadcastJSONObject = JSONObject(liveBroadcastItem)
        println(liveBroadcastJSONObject)
        val snippetObject = liveBroadcastJSONObject["snippet"] as JSONObject
        StreamProfile.lievChatId = snippetObject["liveChatId"] as String
        val url =
            YoutubeApi.getIngestionAddress(StreamProfile.youTube!!, StreamProfile.streamId)
        StreamProfile.rtmpUrl = url
    }

    private fun isGoogleServicesAvailable(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this)
        return connectionStatusCode == ConnectionResult.SUCCESS
    }

    inner class CreateLiveEventTask() : AsyncTask<Void, Void, Void?>() {
        private lateinit var youTube: YouTube

        init {
            StreamProfile.youTube?.let {
                youTube = it
            }
        }

        private var progressDailog: ProgressDialog? = null
        override fun onPreExecute() {
            progressDailog = ProgressDialog.show(
                this@MainActivity,
                null,
                this@MainActivity.resources.getText(R.string.creatingEvent),
                true
            )
        }

        override fun doInBackground(vararg params: Void?): Void? {
            try {
                val date = Date().toString()
                val result = YoutubeApi.createLiveEvent(
                    youTube, "Event - " + date,
                    "A live streaming event - " + date
                )
                result?.let {
                    loadStreamProfile(youTube, it)
                }
            } catch (e: UserRecoverableAuthIOException) {
                this@MainActivity.startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: Void?) {
            progressDailog?.dismiss()
            startStreaming()
        }
    }

    inner class DeleteEventTask() : AsyncTask<String, Void, Void?>() {
        private var progressDialog: ProgressDialog? = null
        private lateinit var youTube: YouTube

        constructor(youTube: YouTube) : this() {
            this.youTube = youTube
        }

        override fun onPreExecute() {
            progressDialog =
                ProgressDialog.show(this@MainActivity, null, "Deleting Events...", true)
        }

        override fun doInBackground(vararg p0: String?): Void? {
            try {
                p0[0]?.let {
                    val id = it
                    val liveBroadcast = youTube.liveBroadcasts().delete("id")
                    liveBroadcast.id = id
                    liveBroadcast.execute()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: Void?) {
            progressDialog?.dismiss()
        }
    }

    inner class GetLiveEventTask() : AsyncTask<Fragment, Void, List<EventData>?>() {

        private var progressDailog: ProgressDialog? = null
        private var fragment: Fragment? = null
        private lateinit var youTube: YouTube

        constructor(credential: GoogleAccountCredential) : this() {
            val transport = NetHttpTransport()
            val jsonFactory = JacksonFactory.getDefaultInstance()
            youTube = YouTube.Builder(transport, jsonFactory, credential)
                .setApplicationName(HomeActivity.APP_NAME).build()
            youSelect = youTube
        }

        override fun onPreExecute() {
            HomeActivity.mContext?.let {
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
                try {
                    return YoutubeApi.getLiveEvents(youTube)
                } catch (e: UserRecoverableAuthIOException) {
                    mContext?.startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
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


    inner class EndLiveEventTask : AsyncTask<Void, Void, Void?>() {
        private lateinit var youTube: YouTube

        init {
            StreamProfile.youTube?.let {
                youTube = it
            }
        }

        override fun doInBackground(vararg params: Void?): Void? {
            try {
                YoutubeApi.endEvent(youTube, StreamProfile.broadcastKey)
            } catch (e: UserRecoverableAuthIOException) {
                this@MainActivity.startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }

    interface CallbackVideo {
        fun onEventSelected(event: EventData)
    }

    override fun onConnectionFailed(p0: ConnectionResult) {

    }

    override fun onConnected(p0: Bundle?) {

    }

    override fun onConnectionSuspended(p0: Int) {

    }

    override fun startStream() {
        CreateLiveEventTask().execute()
    }

    override fun stopStream() {
        CreateLiveEventTask().execute()
        startStreaming()
    }
}
