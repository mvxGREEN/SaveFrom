package com.mvxgreen.ytdloader

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit

/**
 * Provide access to formatting & shared preferences routines & constants
 */
class PrefsManager(ctx: Context) {
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~  SHARED PREFERENCES  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    var sharedPrefs: SharedPreferences? = null

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~  CONSTRUCTOR  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    init {
        initSharedPrefs(ctx)
    }

    /**
     * Initialize Shared Preferences
     * @param ctx main activity
     */
    fun initSharedPrefs(ctx: Context) {
        // Create & store default values, if necessary
        try {
            sharedPrefs = ctx.getSharedPreferences(
                "ULOADER_PREFS",
                Context.MODE_PRIVATE
            )

            val editor = sharedPrefs!!.edit()

            // total runs
            if (!sharedPrefs!!.contains("TOTAL_RUNS")) {
                editor.putInt(
                    "TOTAL_RUNS",
                    0
                )
            }
            editor.apply()

            // other default values
            if (!sharedPrefs!!.contains("TOTAL_CONVERSIONS")) {
                editor.putInt(
                    "TOTAL_CONVERSIONS",
                    0
                )
            }

            // default string values
            for (i in KEY_IDS.indices) {
                val key = ctx.getString(KEY_IDS[i])
                if (!sharedPrefs!!.contains(key)) {
                    editor.putString(
                        key,
                        ""
                    )
                    editor.apply()
                }
            }
        } catch (e: NullPointerException) {
            Log.e(TAG, "Edge Error: NullPointer during initSharedPrefs")
            e.printStackTrace()
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~  GETTERS/SETTERS  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    var backgroundEnabled: String?
        get() = sharedPrefs!!.getString("BACKGROUND_ENABLED", "")
        set(value) {
            val key = "BACKGROUND_ENABLED"

            Log.i(
                TAG,
                "set " + key + " in shared prefs: {" + key + "," + value + "}"
            )

            sharedPrefs!!.edit {
                putString(
                    key,
                    value
                )
            }
        }

    var fileName: String?
        /**
         * @return filepath for new file
         */
        get() = sharedPrefs!!.getString("FILE_NAME", "")
        set(value) {
            val key = "FILE_NAME"

            Log.i(
                TAG,
                "set $key in shared prefs: {$key,$value}"
            )

            sharedPrefs!!.edit {
                putString(
                    key,
                    value
                )
            }
        }

    var downloadUrl = ""

    var thumbnailUrl: String?
        /**
         * @return filepath for new file
         */
        get() = sharedPrefs!!.getString("THUMBNAIL_URL", "")
        set(value) {
            val key = "THUMBNAIL_URL"

            Log.i(
                TAG,
                "set $key in shared prefs: {$key,$value}"
            )

            sharedPrefs!!.edit {
                putString(
                    key,
                    value
                )
            }
        }

    var fileSize = ""

    var fileExt: String?
        get() = sharedPrefs!!.getString("FILE_EXTENSION", "")
        set(value) {
            val key = "FILE_EXTENSION"

            Log.i(
                TAG,
                "set $key in shared prefs: {$key,$value}"
            )

            sharedPrefs!!.edit {
                putString(
                    key,
                    value
                )
            }
        }

    var formatId: String?
        get() = sharedPrefs!!.getString("FORMAT_ID", "")
        set(value) {
            val key = "FORMAT_ID"

            Log.i(
                TAG,
                "set $key in shared prefs: {$key,$value}"
            )

            sharedPrefs!!.edit {
                putString(
                    key,
                    value
                )
            }
        }

    var originalUrl: String?
        get() = sharedPrefs!!.getString("ORIGINAL_URL", "")
        set(value) {
            val key = "ORIGINAL_URL"

            Log.i(
                TAG,
                "set $key in shared prefs: {$key,$value}"
            )

            sharedPrefs!!.edit {
                putString(
                    key,
                    value
                )
            }
        }

    companion object {
        private val TAG: String = "PrefsManager"

        private val KEY_IDS = intArrayOf(
            R.string.prefs_key_download_url,
            R.string.prefs_key_folder,
            R.string.prefs_key_filename,
            R.string.prefs_key_file_ext,
            R.string.prefs_key_original_url,
            R.string.prefs_key_title,
            R.string.prefs_key_thumbnail_url,
            R.string.prefs_key_token,
            R.string.prefs_key_background_enabled

        )
    }
}