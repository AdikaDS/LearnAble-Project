package com.adika.learnable.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

// Tentang per-Quizan per Sub Bab //

data class Quiz(
    val id: String = "",
    val subBabId: String = "",
    val title: String = "",
    val description: String = "",
    val questions: List<QuizQuestion> = listOf(),
    val passingScore: Float = 70f, // Nilai minimum untuk lulus (dalam persentase)
    val timeLimit: Int = 0, // Batas waktu dalam menit, 0 berarti tidak ada batas
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

data class QuizQuestion(
    val id: String = "",
    val question: String = "",
    val options: List<String> = listOf(),
    val correctAnswer: Int = 0, // Index jawaban yang benar
    val explanation: String = "" // Penjelasan untuk jawaban yang benar
)

data class QuizResult(
    val id: String = "",
    val studentId: String = "",
    val quizId: String = "",
    val subBabId: String = "",
    val score: Float = 0f,
    val answers: List<Int> = listOf(), // Index jawaban yang dipilih siswa
    val timeSpent: Int = 0, // Waktu yang dihabiskan dalam detik
    val completedAt: Timestamp = Timestamp.now(),
    val isPassed: Boolean = false
)

// Tentang permaterian siswa //

// Model untuk mata pelajaran
data class Subject(
    val id: String = "",
    val idSubject: String = "",
    val name: String = "",
    val description: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val isActive: Boolean = true,
    val schoolLevel: String = "",
    val totalLessons: Int = 0,
    val totalQuizzes: Int = 0
)

// Model untuk materi pembelajaran
data class Lesson(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val idSubject: String = "",
    val schoolLevel: String = "", // "sd", "smp", "sma"
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val duration: Int = 0, // Durasi dalam menit
    val difficulty: String = "", // "easy", "medium", "hard"
    val prerequisites: List<String> = listOf(), // ID materi yang harus diselesaikan sebelumnya
    val disabilityTypes: List<String> = listOf(), // Tipe disabilitas yang didukung ("tunarungu", "tunanetra")
    val subBab: List<SubBab> = listOf(), // List of sub-bab
    val teacherId: String = "" // ID dari teacher yang membuat lesson
)

// Model untuk sub bab materi
@Parcelize
data class SubBab(
    val id: String = "",
    val lessonId: String = "", // ID dari Lesson yang memiliki SubBab ini
    val title: String = "",
    val content: String = "",
    val duration: Int = 0, // Durasi dalam menit
    val mediaUrls: Map<String, String> = mapOf( // URL media untuk setiap tipe disabilitas
        "video" to "", // URL video bahasa isyarat
        "audio" to "", // URL deskripsi audio
        "pdfLesson" to "" // Url Materi berbentuk PDF
    )
) : Parcelable


sealed interface MaterialResource {
    val id: String
    val title: String
    val bucketName: String
    val objectKey: String
}

data class VideoResource(
    override val id: String,
    override val title: String,
    override val bucketName: String,
    override val objectKey: String ,
    val duration: Int
) : MaterialResource

data class AudioResource(
    override val id: String,
    override val title: String,
    override val bucketName: String,
    override val objectKey: String ,
    val duration: Int = 0
) : MaterialResource

data class PdfResource(
    override val id: String,
    override val title: String,
    override val bucketName: String,
    override val objectKey: String ,
) : MaterialResource

data class StudentLessonProgress(
    val id: String = "",
    val studentId: String = "",
    val lessonId: String = "",
    val lessonTitle: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val progressPercentage: Int = 0,
    val completedSubBabs: Int = 0,
    val totalSubBabs: Int = 0,
    val quizScores: List<Float> = listOf(), // List nilai quiz untuk lesson ini
    val lastActivityDate: Timestamp = Timestamp.now(),
    val totalTimeSpent: Int = 0, // dalam menit
    val isCompleted: Boolean = false
)

data class StudentSubjectProgress(
    val id: String = "",
    val studentId: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val progressPercentage: Int = 0,
    val completedLessons: Int = 0,
    val totalLessons: Int = 0,
    val quizAverage: Float = 0f,
    val lastUpdated: Timestamp = Timestamp.now(),
    val streak: Int = 0,
    val totalTimeSpent: Int = 0,
    val lastActivityDate: Timestamp = Timestamp.now(),
    val lessonProgress: List<StudentLessonProgress> = listOf()
)

data class StudentOverallProgress(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val totalSubjects: Int = 0,
    val completedSubjects: Int = 0,
    val overallProgressPercentage: Int = 0,
    val totalTimeSpent: Int = 0,
    val streak: Int = 0,
    val lastActivityDate: Timestamp = Timestamp.now(),
    val subjectProgress: List<StudentSubjectProgress> = listOf()
)

data class StudentSubBabProgress(
    val id: String = "",
    val studentId: String = "",
    val subBabId: String = "",
    val lessonId: String = "",
    val completedMaterials: Map<String, Boolean> = mapOf(
        "pdf" to false,
        "video" to false,
        "audio" to false,
        "quiz" to false
    ),
    val quizScore: Float = 0f,
    val timeSpent: Int = 0, // dalam menit
    val lastActivityDate: Timestamp = Timestamp.now(),
    val isCompleted: Boolean = false
)

// Model untuk notifikasi
data class Notification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "", // "quiz", "lesson", "progress", etc.
    val isRead: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val data: Map<String, Any> = mapOf() // Data tambahan
)


