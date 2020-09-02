package com.android.shotz_pro_io.stream

import android.media.ImageReader

class VideoFrameGrabber {
    private var imageReader: ImageReader? = null
    private var callback: VideoFrameCallback? = null

    open fun setVideoFrameCallback(callback: VideoFrameCallback) {
        this.callback = callback
    }

    open fun start(imageReader: ImageReader) {
        this.imageReader = imageReader
        this.imageReader?.let {

        }
    }
}