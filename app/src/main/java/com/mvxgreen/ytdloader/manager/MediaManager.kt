package com.mvxgreen.ytdloader.manager

import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.MediaScannerConnectionClient
import android.net.Uri
import android.util.Log
import com.mvxgreen.ytdloader.MainActivity

class MediaManager(main: MainActivity?, private val PATH: String?, private val MIME_TYPE: String?) :
    MediaScannerConnectionClient {
    private val TAG: String = MediaManager::class.java.getCanonicalName()
    private val CONNECTION: MediaScannerConnection
    private val main: MainActivity?


    // filePath - where to scan;
    // mime type of media to scan i.e. "image/jpeg".
    // use "*/*" for any media
    init {
        CONNECTION = MediaScannerConnection(main, this)
        this.main = main
    }

    // do the scanning
    fun scanMedia() {
        CONNECTION.connect()
    }

    // start the scan when scanner is ready
    override fun onMediaScannerConnected() {
        CONNECTION.scanFile(PATH, MIME_TYPE)
        Log.w(TAG, "media file scanned: " + PATH)
    }

    override fun onScanCompleted(path: String?, uri: Uri?) {
        Log.i(TAG, "onScanCompleted")
    }

    companion object {
        const val MIME_MP4: String = "video/mp4"
        const val EXT_MP4: String = ".mp4"
    }
}