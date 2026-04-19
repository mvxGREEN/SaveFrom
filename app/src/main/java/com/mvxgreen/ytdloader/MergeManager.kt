package com.mvxgreen.ytdloader

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

// TODO implement merging video and audio files

object MergeManager {
    const val TAG: String = "MergeManager"

    fun mergeAV(filepath: String, vFilepath: String, aFilepath: String) {
        val msg = ("MERGING:\nfilepath=" + filepath
                + "\nvFilepath=" + vFilepath
                + "\naFilepath=" + aFilepath)
        Log.i(TAG, msg)

        try {
            mergeFiles(vFilepath, aFilepath, filepath)
        } catch (e: IOException) {
            Log.e(TAG, "Error merging files: " + e.message)
        }
    }

    /**
     * Merges a video file and an audio file into a single output MP4 file.
     * 
     * @param videoFilePath Path to the input video file (e.g., .mp4).
     * @param audioFilePath Path to the input audio file (e.g., .m4a).
     * @param outputFilePath Path for the merged output MP4 file.
     * @throws IOException If an error occurs during the merging process.
     */
    @Throws(IOException::class)
    fun mergeFiles(videoFilePath: String, audioFilePath: String, outputFilePath: String) {
        var muxer: MediaMuxer? = null
        var videoExtractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null

        try {
            // Setup output file
            val outputFile = File(outputFilePath)
            if (outputFile.exists()) {
                outputFile.delete()
            }

            muxer = MediaMuxer(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // --- Video Track ---
            videoExtractor = MediaExtractor()
            try {
                videoExtractor.setDataSource(videoFilePath)
            } catch (e: Exception) {
                Log.e(TAG, "Error setting videoExtractor data source: " + e.message)
                e.printStackTrace()
            }
            var videoTrackIndex = -1
            var videoFormat: MediaFormat? = null
            for (i in 0..<videoExtractor.getTrackCount()) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime != null && mime.startsWith("video/")) {
                    videoExtractor.selectTrack(i)
                    videoFormat = format
                    //videoFormat.setString(MediaFormat.KEY_MIME, "video/mp4v");
                    videoTrackIndex = muxer.addTrack(videoFormat)
                    break
                }
            }
            if (videoTrackIndex == -1) {
                throw IOException("No video track found in " + videoFilePath)
            }

            // --- Audio Track ---
            audioExtractor = MediaExtractor()
            try {
                audioExtractor.setDataSource(audioFilePath)
            } catch (e: Exception) {
                Log.e(TAG, "Error setting audioExtractor data source: " + e.message)
                e.printStackTrace()
            }

            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0..<audioExtractor.getTrackCount()) {
                val format = audioExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime != null && mime.startsWith("audio/")) {
                    audioExtractor.selectTrack(i)
                    audioFormat = format
                    audioTrackIndex = muxer.addTrack(audioFormat)
                    break
                }
            }
            if (audioTrackIndex == -1) {
                throw IOException("No audio track found in " + audioFilePath)
            }

            muxer.start()

            // --- Copy Video Samples ---
            val videoBuffer = ByteBuffer.allocate(1024 * 1024) // 1MB buffer
            val videoBufferInfo = MediaCodec.BufferInfo()
            videoExtractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            while (true) {
                val sampleSize = videoExtractor.readSampleData(videoBuffer, 0)
                if (sampleSize < 0) {
                    break
                }
                videoBufferInfo.offset = 0
                videoBufferInfo.size = sampleSize
                videoBufferInfo.presentationTimeUs = videoExtractor.getSampleTime()
                videoBufferInfo.flags =
                    MediaCodec.BUFFER_FLAG_KEY_FRAME // Use sample flags directly

                muxer.writeSampleData(videoTrackIndex, videoBuffer, videoBufferInfo)
                videoExtractor.advance()
            }
            Log.d(TAG, "Finished writing video track.")

            // --- Copy Audio Samples ---
            val audioBuffer = ByteBuffer.allocate(1024 * 1024) // 1MB buffer
            val audioBufferInfo = MediaCodec.BufferInfo()
            audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            while (true) {
                val sampleSize = audioExtractor.readSampleData(audioBuffer, 0)
                if (sampleSize < 0) {
                    break
                }
                audioBufferInfo.offset = 0
                audioBufferInfo.size = sampleSize
                audioBufferInfo.presentationTimeUs = audioExtractor.getSampleTime()
                audioBufferInfo.flags =
                    MediaCodec.BUFFER_FLAG_KEY_FRAME // Use sample flags directly

                muxer.writeSampleData(audioTrackIndex, audioBuffer, audioBufferInfo)
                audioExtractor.advance()
            }
            Log.d(TAG, "Finished writing audio track.")
        } finally {
            if (muxer != null) {
                try {
                    muxer.stop()
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "IllegalStateException when stopping muxer: " + e.message)
                }
                muxer.release()
                Log.d(TAG, "Muxer released.")
            }
            if (videoExtractor != null) {
                videoExtractor.release()
                Log.d(TAG, "Video extractor released.")
            }
            if (audioExtractor != null) {
                audioExtractor.release()
                Log.d(TAG, "Audio extractor released.")
            }
        }
    }


    /**
     * Delete temp files
     */
    fun deleteTempFiles(vfp: String?, afp: String): Boolean {
        Log.i(TAG, "deleteTempFiles: " + vfp + ", " + afp)
        var audioDeleted = false
        val audioFile = File(afp)
        audioDeleted = audioFile.delete()

        return audioDeleted
    }
}
