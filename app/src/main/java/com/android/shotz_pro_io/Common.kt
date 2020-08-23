package com.android.shotz_pro_io

import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class Common {
    val capture: String = "VIDEO-CAPTURE"

    open fun getFile(type: String): File {
        var builder = StringBuilder(getDirectory())
        if (builder.isEmpty()) {
            builder = StringBuilder(getDirectory())
        } else {
            builder.append(File.separator)
            if (type.equals(capture)) {
                builder.append("Video Captures")
                val subdirectory = File(builder.toString())
                if(!subdirectory.exists()){
                    subdirectory.mkdirs()
                }
                builder.append(File.separator)
                builder.append(SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date()))
                builder.append(".mp4")
                val ouputfile = File(builder.toString())
                if (!ouputfile.exists()) {
                    ouputfile.createNewFile()
                }
                return ouputfile
            }
        }
        return File("")
    }

    open fun getDirectory(): String {
        val builder = StringBuilder(Environment.getExternalStorageDirectory().toString())
        builder.append(File.separator)
        builder.append(R.string.app_name)
        val mediaDir = File(builder.toString())
        while (!mediaDir.exists()) {
            if (!mediaDir.mkdirs()) {
                throw RuntimeException("File is not creating")
                return ""
            }
        }
        return builder.toString()
    }
}