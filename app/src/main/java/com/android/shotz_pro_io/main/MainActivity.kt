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
import com.android.shotz_pro_io.stream.EventData
import com.android.shotz_pro_io.stream.StreamingActivity
import com.android.shotz_pro_io.stream.YoutubeApi
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.android.gms.plus.Plus
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeScopes
import com.mikhaellopez.circularimageview.CircularImageView
import java.util.*


class MainActivity : AppCompatActivity() ,GoogleApiClient.OnConnectionFailedListener,GoogleApiClient.ConnectionCallbacks{

    private val PERMISSION_CODE = 201

    private lateinit var createEvent: Button
    private lateinit var container: FrameLayout
    private lateinit var accountImage: CircularImageView
    private lateinit var accountName: TextView
    private lateinit var gso : GoogleSignInOptions
    private lateinit var googleSignInClient : GoogleApiClient

    private lateinit var credential: GoogleAccountCredential
    private lateinit var listFragment: Fragment
    open var mChooseAccuntName: String? = null
    private val permission = arrayOf<String>(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.GET_ACCOUNTS
    )
    private val selectedEventCallback: CallbackVideo = object : CallbackVideo {
        override fun onEventSelected(event: EventData) {
            startStreaming(event)
        }
    }

    companion object {
        val scopes = mutableListOf(Scopes.PROFILE, YouTubeScopes.YOUTUBE)
        val ACCOUNT_KEY = "Account key"
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

        this.createEvent = findViewById(R.id.createLiveEventMain)
        this.accountImage = findViewById(R.id.accountImageMain)
        this.accountName = findViewById(R.id.accountNameMain)
        this.container = findViewById(R.id.containerMain)

        if (!hasPermissions(permission)) {
            ActivityCompat.requestPermissions(this, permission, PERMISSION_CODE)
        }
        createEvent.setOnClickListener {
            createLiveEvent()
        }

        accountName.setOnClickListener {
            getLiveEvent()
        }

        credential =
            GoogleAccountCredential.usingOAuth2(this, scopes).setBackOff(ExponentialBackOff())

        //google sign in
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().requestProfile().build()
        googleSignInClient = GoogleApiClient.Builder(this)
            .addApi(Auth.GOOGLE_SIGN_IN_API,gso)
            .addConnectionCallbacks(this)
            .setAccountName(mChooseAccuntName)
            .addOnConnectionFailedListener(this)
            .build()

        signIn()

        listFragment = VideosFragment(this, accountImage, accountName, selectedEventCallback,googleSignInClient)
        getApiRequest()
        supportFragmentManager.beginTransaction().add(R.id.containerMain, listFragment).commit()
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
                    finish()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            SIGN_IN->{
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
                        EndLiveEventTask().execute(it)
                    }
                }
            }
        }
    }

    private fun handleSignIn(result: GoogleSignInResult) {
        val account = result.signInAccount
        account?.let {
            accountName.text = account.displayName
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

    private fun startStreaming(event: EventData) {
        val broadcastId = event.getId()
        StartEventTask(credential).execute(broadcastId)
        val intent = Intent(this, StreamingActivity()::class.java).also {
            it.putExtra(YoutubeApi.RTMP_URL_KEY, event.mIngestionAddress)
            it.putExtra(YoutubeApi.BROADCAST_ID_KEY, broadcastId)
        }
        startActivityForResult(intent, HomeActivity.REQUEST_STREAMER)
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

    private fun isGoogleServicesAvailable(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this)
        return connectionStatusCode == ConnectionResult.SUCCESS
    }

    private fun createLiveEvent() {
        CreateLiveEventTask(credential).execute()
    }

    private fun getLiveEvent() {
        if (mChooseAccuntName == null) {
            chooseAccount()
            return
        }
        GetLiveEventTask(credential).execute(listFragment)
    }

    private class CreateLiveEventTask() : AsyncTask<Void, Void, List<EventData>?>() {
        private lateinit var youTube: YouTube

        constructor(credential: GoogleAccountCredential) : this() {
            val transport = NetHttpTransport()
            val jsonFactory = JacksonFactory.getDefaultInstance()
            youTube = YouTube.Builder(transport, jsonFactory, credential)
                .setApplicationName(HomeActivity.APP_NAME).build()
        }

        private var progressDailog: ProgressDialog? = null
        override fun onPreExecute() {
            HomeActivity.mContext?.let {
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
                try {
                    val date = Date().toString()
                    YoutubeApi.createLiveEvent(
                        youTube, "Event - " + date,
                        "A live streaming event - " + date
                    )
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
            //set create live button
            result?.let {
                for (l in it) {
                    println(l.mIngestionAddress)
                }
            }
            progressDailog?.dismiss()
        }
    }

    private class GetLiveEventTask() : AsyncTask<Fragment, Void, List<EventData>?>() {

        private var progressDailog: ProgressDialog? = null
        private var fragment: Fragment? = null
        private lateinit var youTube: YouTube

        constructor(credential: GoogleAccountCredential) : this() {
            val transport = NetHttpTransport()
            val jsonFactory = JacksonFactory.getDefaultInstance()
            youTube = YouTube.Builder(transport, jsonFactory, credential)
                .setApplicationName(HomeActivity.APP_NAME).build()
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
                }
                catch (e: UserRecoverableAuthIOException) {
                    mContext?.startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
                }catch (e: Exception) {
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

    private class StartEventTask() : AsyncTask<String, Void, Void?>() {
        private var progressDailog: ProgressDialog? = null
        private lateinit var youTube: YouTube

        constructor(credential: GoogleAccountCredential) : this() {
            val transport = NetHttpTransport()
            val jsonFactory = JacksonFactory.getDefaultInstance()
            youTube = YouTube.Builder(transport, jsonFactory, credential)
                .setApplicationName(HomeActivity.APP_NAME).build()
        }

        override fun onPreExecute() {
            mContext?.let {
                progressDailog = ProgressDialog.show(
                    it,
                    null,
                    it.resources.getText(R.string.startingEvent),
                    true
                )
            }
        }

        override fun doInBackground(vararg params: String?): Void? {
            mContext?.let {
                try {
                    params[0]?.let {
                        YoutubeApi.startEvent(youTube, it)
                    }
                }
                catch (e: UserRecoverableAuthIOException) {
                    mContext?.startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
                }catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return null
        }

        override fun onPostExecute(result: Void?) {
            progressDailog?.dismiss()
        }
    }

    private class EndLiveEventTask() : AsyncTask<String, Void, Void?>() {
        private var progressDailog: ProgressDialog? = null
        private lateinit var youTube: YouTube

        constructor(credential: GoogleAccountCredential) : this() {
            val transport = NetHttpTransport()
            val jsonFactory = JacksonFactory.getDefaultInstance()
            youTube = YouTube.Builder(transport, jsonFactory, credential)
                .setApplicationName(HomeActivity.APP_NAME).build()
        }

        override fun onPreExecute() {
            mContext?.let {
                progressDailog = ProgressDialog.show(
                    it,
                    null,
                    it.resources.getText(R.string.endingEvent),
                    true
                )
            }
        }

        override fun doInBackground(vararg params: String?): Void? {
            mContext?.let {
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
            progressDailog?.dismiss()
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
        TODO("Not yet implemented")
    }
}
