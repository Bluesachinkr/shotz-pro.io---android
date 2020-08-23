package com.android.shotz_pro_io

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    private val PERMISSION_CODE = 208
    private val permission = arrayOf<String>(android.Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ,android.Manifest.permission.RECORD_AUDIO)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(hasPermissions(permission)){

        }else{
            ActivityCompat.requestPermissions(this,permission,PERMISSION_CODE)
        }
        startActivity(Intent(this,ScreenCaptureActivity::class.java))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == PERMISSION_CODE){
            for (p in permissions){
                if(ActivityCompat.checkSelfPermission(this,p) == PackageManager.PERMISSION_DENIED){
                    finish()
                }
            }
        }
    }

    private fun hasPermissions(permission: Array<String>): Boolean {
        for (p in permission){
            if(ActivityCompat.checkSelfPermission(this,p) == PackageManager.PERMISSION_DENIED){
                return false
            }
        }
        return true
    }
}