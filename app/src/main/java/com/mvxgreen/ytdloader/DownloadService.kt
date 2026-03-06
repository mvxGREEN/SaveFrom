package com.mvxgreen.ytdloader

import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.AsyncTask
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.firebase.analytics.FirebaseAnalytics
import com.mvxgreen.ytdloader.manager.PrefsManager
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.FileTime

class DownloadService : Service() {
    private var mPrefsManager: PrefsManager? = null
    private val binder: IBinder = LocalBinder()
    var pendingIntentId: Int = 0

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        val service: DownloadService
            get() =// Return this instance of LocalService so clients can call public methods
                this@DownloadService
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.i(TAG, "OnBind")
        return binder
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand")

        mPrefsManager = PrefsManager(getApplicationContext())
        registerNotification()

        // get original url from prefs
        val ogUrl = mPrefsManager!!.originalUrl

        // start download
        downloadVideo(ogUrl)

        return START_STICKY
    }

    @Suppress("deprecation")
    override fun onStart(intent: Intent?, startId: Int) {
        super.onStart(intent, startId)
        Log.i(TAG, "onStart")
    }

    private fun downloadVideo(url: String?) {
        try {
            val bundle = Bundle()
            bundle.putString("app_name", "savefrom")
            bundle.putString("url", url)
            FirebaseAnalytics.getInstance(this)
                .logEvent("download_start", bundle)
        } catch (ignored: Exception) {
        }

        DownloadVideoTask(MainActivity.activityCurrent).execute(url)
    }

    // async download video
    class DownloadVideoTask(ctx: Context) : AsyncTask<String?, Void?, String?>() {
        var vidExt: String = ".mp4"
        var audExt: String = ".m4a"
        var ap: AndroidPlatform
        var prefsManager: PrefsManager

        init {
            prefsManager = PrefsManager(ctx)
            ap = AndroidPlatform(ctx)
        }

        //this method will download the audio file by using python script
        override fun doInBackground(vararg urls: String?): String {
            Log.i(TAG, "doInBackground()")

            // init python
            if (!Python.isStarted()) {
                Python.start(ap)
            }
            val py = Python.getInstance()
            val pyObject = py.getModule("vidloader")

            // get video url and resolution
            val videoUrl: String? = urls[0]
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
                    MainActivity.ABS_PATH_DOCS,
                    prefsManager.fileName,
                    resolution
                )
                res = result.toString()
                Log.i(TAG, "format_ids: " + res)
                prefsManager.formatId = res
            } catch (e: Exception) {
                e.printStackTrace()
                val msg = "error downloading video! e=" + e
                Log.e(TAG, msg)

                // send finish broadcast
                val intent = Intent("69")
                intent.putExtra("FILEPATH", "")
                MainActivity.activityCurrent.sendBroadcast(intent)
            }

            return res
        }

        override fun onPostExecute(s: String?) {
            Log.i(TAG, "OnPostExecute")

            // build filepaths
            val absFilename = prefsManager.fileName + vidExt
            var absFilepath: String? = MainActivity.ABS_PATH_DOCS + prefsManager.fileName
            absFilepath += vidExt

            // scan video file (no audio)
            val dl = File(absFilepath)
            if (dl.exists()) {
                var now: FileTime? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    now = FileTime.fromMillis(System.currentTimeMillis())
                    try {
                        Files.setLastModifiedTime(dl.toPath(), now)
                    } catch (e: IOException) {
                        throw RuntimeException(e)
                    }
                }
            }

            // send finish broadcast
            val intent = Intent("69")
            intent.putExtra("FILEPATH", absFilepath)
            MainActivity.activityCurrent.sendBroadcast(intent)
        }

        companion object {
            private val TAG: String = DownloadVideoTask::class.java.getCanonicalName()
        }
    }

    private fun registerNotification() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        try {
            val intent = Intent(this@DownloadService, MainActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            var pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                pendingIntentFlags =
                    (PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }
            val pi = PendingIntent.getActivity(
                MainActivity.activityCurrent,
                pendingIntentId++,
                intent,
                pendingIntentFlags
            )

            val channel =
                NotificationChannel("SaveFrom", "SaveFrom", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)

            val notification =
                NotificationCompat.Builder(this@DownloadService, "SaveFrom")
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
            ServiceCompat.startForeground( /* service = */
                this,  /* id = */
                43,  // Cannot be 0
                /* notification = */
                notification,  /* foregroundServiceType = */
                type
            )
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e is ForegroundServiceStartNotAllowedException
            ) {
                // App not in a valid state to start foreground service
                // (e.g started from bg)
            }
            // ...
        }
    }

    fun setProgress(max_progress: Int, progress: Int) {
        val intent = Intent(this@DownloadService, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        var pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntentFlags = (PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val pi = PendingIntent.getActivity(
            MainActivity.activityCurrent,
            pendingIntentId++,
            intent,
            pendingIntentFlags
        )
        val notification = NotificationCompat.Builder(this@DownloadService, "SaveFrom")
            .setContentTitle("Downloading Video…")
            .setSmallIcon(R.drawable.downloader_raw)
            .setProgress(max_progress, progress, false)
            .setOngoing(true)
            .setContentIntent(pi)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(43, notification)
    }

    companion object {
        private val TAG: String = DownloadService::class.java.getCanonicalName()
    }
}
