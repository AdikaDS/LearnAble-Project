package com.adika.learnable.util

import android.media.MediaRecorder
import java.io.File

class AudioRecorder {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    var isRecording = false
        private set

    fun start(outputFile: File) {
        this.outputFile = outputFile
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
        isRecording = true
    }

    fun stop(): File? {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: RuntimeException) {
            outputFile?.delete()
            return null
        } finally {
            recorder = null
            isRecording = false
        }
        return outputFile
    }

    fun cancel() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Ignore
        } finally {
            recorder = null
            outputFile?.delete()
            outputFile = null
            isRecording = false
        }
    }
}
