package com.android.shotz_pro_io.main

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.viewpager.widget.ViewPager
import com.android.shotz_pro_io.R
import com.android.shotz_pro_io.screenCapture.ScreenCaptureActivity
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {
    private val PERMISSION_CODE = 208
    private val permission = arrayOf<String>(android.Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ,android.Manifest.permission.RECORD_AUDIO)

    private lateinit var mViewPager : ViewPager
    private lateinit var mTabLayout: TabLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.mViewPager = findViewById(R.id.viewPager_main)
        this.mTabLayout = findViewById(R.id.tabLayout_main)

        if(!hasPermissions(permission)){
            ActivityCompat.requestPermissions(this,permission,PERMISSION_CODE)
        }
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