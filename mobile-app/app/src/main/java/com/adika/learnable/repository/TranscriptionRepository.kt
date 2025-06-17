package com.adika.learnable.repository

import android.content.Context
import com.adika.learnable.api.TranscriptionService
import com.adika.learnable.util.AudioRecorder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transcriptionService: TranscriptionService,
    private val audioRecorder: AudioRecorder
) {
    private var currentRecordingFile: File? = null

    fun startRecording(): Flow<Unit> = flow {
        val outputFile = File(context.cacheDir, "recording.mp4")
        currentRecordingFile = outputFile
        audioRecorder.start(outputFile)
        emit(Unit)
    }.flowOn(Dispatchers.IO)

    fun stopRecording(): Flow<File?> = flow {
        val recordedFile = audioRecorder.stop()
        currentRecordingFile = null
        emit(recordedFile)
    }.flowOn(Dispatchers.IO)

    fun cancelRecording(): Flow<Unit> = flow {
        audioRecorder.cancel()
        currentRecordingFile = null
        emit(Unit)
    }.flowOn(Dispatchers.IO)

    fun transcribeAudio(file: File): Flow<Result<String>> = flow {
        try {
            val requestFile = file.asRequestBody("audio/mp4".toMediaTypeOrNull())
            val audioPart = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val response = transcriptionService.transcribeAudio(audioPart)
            if (response.isSuccessful) {
                emit(Result.success(response.body()?.text ?: "No transcription available"))
            } else {
                emit(Result.failure(Exception("Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    fun transcribeEvaluate(
        file: File,
        question: String,
        options: List<String>
    ): Flow<Result<Pair<String, String>>> = flow {
        try {
            val requestFile = file.asRequestBody("audio/mp4".toMediaTypeOrNull())
            val audioPart = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val questionBody = question.toRequestBody("text/plain".toMediaTypeOrNull())
            val optionBodies = options.map { it.toRequestBody("text/plain".toMediaTypeOrNull()) }

            val response =
                transcriptionService.transcribeAndEvaluate(audioPart, questionBody, optionBodies)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val transcript = body.transcript
                    val predictedAnswer = body.evaluation.predictedAnswer
                    emit(Result.success(transcript to predictedAnswer))
                } else {
                    emit(Result.failure(Exception("Response body null")))
                }
            } else {
                emit(Result.failure(Exception("Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
} 