package com.mvxgreen.ytdloader

import android.app.DownloadManager
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.AsyncTask
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.mvxgreen.ytdloader.MainActivity.Companion.ABS_PATH_MOVIES
import com.mvxgreen.ytdloader.MainActivity.Companion.ABS_PATH_TEMP
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.attribute.FileTime

class DownloadService : Service() {
    private lateinit var mPrefsManager: PrefsManager
    private val binder: IBinder = LocalBinder()
    private var pendingIntentId = 0

    companion object {
        private val TAG = DownloadService::class.java.canonicalName
    }

    /**
     * Class used for the client Binder. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        fun getService(): DownloadService = this@DownloadService
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.i(TAG, "OnBind")
        return binder
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand")

        mPrefsManager = PrefsManager(applicationContext)
        registerNotification()

        val chunkUrlsStr = mPrefsManager.chunkUrlsList ?: ""

        if (chunkUrlsStr.isNotEmpty()) {
            val chunkUrls = chunkUrlsStr.split("|||")
            Log.i(TAG, "Found ${chunkUrls.size} chunk URLs. Starting chunk downloader.")

            // Launch in IO thread
            CoroutineScope(Dispatchers.IO).launch {
                downloadChunksAndMerge(chunkUrls)
            }
        } else {
            // start download if no chunk urls
            downloadVideo(mPrefsManager.originalUrl!!)
        }

        return START_STICKY
    }

    @Suppress("DEPRECATION")
    override fun onStart(intent: Intent?, startId: Int) {
        super.onStart(intent, startId)
        Log.i(TAG, "onStart")
    }

    private fun downloadVideo(url: String) {
        MainActivity.activityCurrent?.let {
            DownloadVideoTask(it).execute(url)
        }
    }

    private suspend fun downloadChunksAndMerge(chunkUrls: List<String>) {
        val fileName = mPrefsManager.fileName ?: "downloaded_video"
        // Use the existing temporary directory
        val tempFile = File(MainActivity.ABS_PATH_TEMP, "$fileName.mp4")

        try {
            // Use true to enable "append" mode in FileOutputStream
            FileOutputStream(tempFile, true).use { output ->

                for ((index, urlStr) in chunkUrls.withIndex()) {
                    val url = URL(urlStr)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.connect()

                    if (connection.responseCode == 200) {
                        connection.inputStream.use { input ->
                            input.copyTo(output)
                        }
                    } else {
                        Log.e(TAG, "Failed to download chunk $index, HTTP Code: ${connection.responseCode}")
                    }

                    connection.disconnect()

                    // Calculate progress and update the notification
                    val progress = ((index + 1) * 100) / chunkUrls.size
                    setProgress(100, progress)

                    // Update MainActivity UI progress directly
                    MainActivity.setProgress("$progress")
                }
            }

            Log.i(TAG, "All chunks downloaded and merged successfully: ${tempFile.absolutePath}")

            // Send broadcast to MainActivity's FinishReceiver to handle moving to Movies folder
            val finishIntent = Intent("69").apply {
                putExtra("FILEPATH", tempFile.absolutePath)
            }
            MainActivity.activityCurrent?.sendBroadcast(finishIntent)

        } catch (e: Exception) {
            Log.e(TAG, "Exception occurred while downloading and merging chunks", e)
        }
    }

    // async download video
    @Suppress("DEPRECATION")
    class DownloadVideoTask(private val ctx: Context) : AsyncTask<String, Void, String>() {
        private val vidExt = ".mp4"
        private val ap = AndroidPlatform(ctx)
        private val prefsManager = PrefsManager(ctx)

        private val serviceJob = Job()
        private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

        companion object {
            private val TAG = DownloadVideoTask::class.java.canonicalName
        }

        // this method will download the audio file by using python script
        override fun doInBackground(vararg urls: String): String {
            Log.i(TAG, "doInBackground()")

            // init python
            if (!Python.isStarted()) {
                Python.start(ap)
            }
            val py = Python.getInstance()
            val pyObject = py.getModule("vidloader")

            // get video url and resolution
            val videoUrl = urls[0]
            val resolution = MainActivity.mResolution.replace("\\D".toRegex(), "")

            if (!Python.isStarted()) {
                Python.start(ap)
            }

            var res = ""
            try {
                Log.i(TAG, "trying dl_video_without_audio")

                if (!Python.isStarted()) {
                    Python.start(ap)
                }

                val result = pyObject.callAttr(
                    "dl_video_without_audio",
                    MainActivity.activityCurrent,
                    videoUrl,
                    ABS_PATH_TEMP,
                    prefsManager.fileName,
                    resolution
                )
                res = result.toString()
                Log.i(TAG, "format_ids: $res")
                prefsManager.formatId = res
            } catch (e: Exception) {
                e.printStackTrace()
                val msg = "error downloading video! e=$e"
                Log.e(TAG, msg)

                // TODO download from html info
                serviceScope.launch {
                    startDownload()
                }
            }

            return res
        }

        override fun onPostExecute(s: String?) {
            Log.i(TAG, "OnPostExecute")

            // build filepaths
            var absFilepath = ABS_PATH_TEMP + prefsManager.fileName
            absFilepath += vidExt

            // scan video file (no audio)
            val dl = File(absFilepath)
            if (dl.exists()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val now = FileTime.fromMillis(System.currentTimeMillis())
                    try {
                        Files.setLastModifiedTime(dl.toPath(), now)
                    } catch (e: IOException) {
                        throw RuntimeException(e)
                    }
                }
            }

            // send finish broadcast
            val intent = Intent("69").apply {
                putExtra("FILEPATH", absFilepath)
            }
            MainActivity.activityCurrent?.sendBroadcast(intent)
        }

        private suspend fun startDownload() {
            val downloadManager = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            // HTML Download
            if (prefsManager.downloadUrl.isNotEmpty()) {
                val downloadReq = DownloadManager.Request(Uri.parse(prefsManager.downloadUrl))
                //downloadReq.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                downloadReq.setDestinationInExternalFilesDir(
                    ctx,
                    ABS_PATH_MOVIES,
                    prefsManager.fileName + ".mp4"
                )

                delay(34) // Added delay before enqueueing download
                var downloadId = downloadManager.enqueue(downloadReq)
                Log.i(TAG, "downloadId=$downloadId")
            }
        }
    }

    private fun registerNotification() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            var pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            }
            val pi = PendingIntent.getActivity(
                MainActivity.activityCurrent,
                pendingIntentId++,
                intent,
                pendingIntentFlags
            )

            val channel = NotificationChannel("SaveFrom", "SaveFrom", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)

            val notification: Notification = NotificationCompat.Builder(this, "SaveFrom")
                .setContentTitle("Downloading…")
                .setSmallIcon(R.drawable.downloader_raw)
                .setProgress(100, 0, true)
                .setOngoing(true)
                .setContentIntent(pi)
                .build()
            manager.notify(43, notification)

            var type = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                type = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            }
            ServiceCompat.startForeground(this, 43, notification, type)
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e is ForegroundServiceStartNotAllowedException
            ) {
                // App not in a valid state to start foreground service
            }
        }
    }

    fun setProgress(maxProgress: Int, progress: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        var pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        }
        val pi = PendingIntent.getActivity(
            MainActivity.activityCurrent,
            pendingIntentId++,
            intent,
            pendingIntentFlags
        )

        val notification = NotificationCompat.Builder(this, "SaveFrom")
            .setContentTitle("Downloading Video…")
            .setSmallIcon(R.drawable.downloader_raw)
            .setProgress(maxProgress, progress, false)
            .setOngoing(true)
            .setContentIntent(pi)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(43, notification)
    }
}
