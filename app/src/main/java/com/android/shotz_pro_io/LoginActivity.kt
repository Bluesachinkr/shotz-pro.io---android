package com.android.shotz_pro_io

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.android.shotz_pro_io.main.HomeActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.tasks.Task

class LoginActivity : AppCompatActivity() {

    private val SIGN_IN_REQUEST_CODE = 201

    private lateinit var mSignInButton: SignInButton
    private lateinit var mGoogleSignInOptions: GoogleSignInOptions
    private lateinit var mGoogleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mSignInButton = findViewById(R.id.signInButton)
        mSignInButton.setSize(SignInButton.SIZE_STANDARD)

        this.mGoogleSignInOptions =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail()
                .requestProfile().build()
        this.mGoogleSignInClient = GoogleSignIn.getClient(this, mGoogleSignInOptions)

        mSignInButton.setOnClickListener {
            signIn()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SIGN_IN_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.let {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                handleSignInIntent(task)
            }
        }
    }

    private fun handleSignInIntent(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.result
            account?.let {
                val intent = Intent(this, HomeActivity::class.java)
                intent.putExtra("account", account)
                startActivity(intent)
                finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun signIn() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, SIGN_IN_REQUEST_CODE)
    }
}