package com.android.shotz_pro_io.main

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.android.shotz_pro_io.R
import com.android.shotz_pro_io.screenCapture.CapturingBallService
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.material.tabs.TabLayout


class MainActivity : AppCompatActivity() {
    private val PERMISSION_CODE = 208
    private val  CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2048
    private val permission = arrayOf<String>(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.RECORD_AUDIO
    )


    private lateinit var mViewPager: ViewPager
    private lateinit var mTabLayout: TabLayout
    private val fragList = mutableListOf<Fragment>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.mViewPager = findViewById(R.id.viewPager_main)
        this.mTabLayout = findViewById(R.id.tabLayout_main)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)){
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION)
        }else {
            if(!CapturingBallService.isBallOpen) {
                val intentBall = Intent(this, CapturingBallService::class.java)
                startService(intentBall)
                CapturingBallService.isBallOpen = true
            }
        }

        if (!hasPermissions(permission)) {
            ActivityCompat.requestPermissions(this, permission, PERMISSION_CODE)
        }
        this.fragList.add(VideosFragment(this))
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
}