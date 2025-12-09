package com.adika.learnable.util

import android.media.MediaRecorder
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorder @Inject constructor() {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null

    fun start(outputFile: File) {
        try {
            // Clean up any existing recorder
            stop()
            
            currentFile = outputFile
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile.absolutePath)
                try {
                    prepare()
                    start()
                } catch (e: Exception) {
                    e.printStackTrace()
                    release()
                    throw e
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    fun stop(): File? {
        return try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    release()
                }
            }
            mediaRecorder = null
            currentFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun cancel() {
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    release()
                }
            }
            mediaRecorder = null
            currentFile?.delete()
            currentFile = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
