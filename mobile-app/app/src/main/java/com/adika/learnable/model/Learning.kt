package com.adika.learnable.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize
import java.util.UUID

data class Quiz(
    val id: String = "",
    val subBabId: String = "",
    val title: String = "",
    val questions: List<QuizQuestion> = listOf(),
    val passingScore: Float = 70f,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

data class OptionItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val mediaUrl: String = ""
)

enum class QuestionType {
    MULTIPLE_CHOICE,
    ESSAY
}

data class QuizQuestion(
    val id: String = UUID.randomUUID().toString(),
    val question: String = "",
    val mediaQuestion: String = "",
    val optionItems: List<OptionItem> = emptyList(),
    val correctAnswer: Int = 0,
    val explanation: String = "",
    val mediaExplanation: String = "",
    val questionType: QuestionType = QuestionType.MULTIPLE_CHOICE
)

data class QuizResult(
    val id: String = "",
    val studentId: String = "",
    val quizId: String = "",
    val subBabId: String = "",
    val score: Float = 0f,
    val answers: List<Int> = listOf(),
    val essayAnswers: Map<String, String> = emptyMap(),
    val essayGrading: Map<String, Boolean> = emptyMap(), // Map<questionId, isCorrect>
    val timeSpent: Int = 0,
    val completedAt: Timestamp = Timestamp.now(),
    val isPassed: Boolean = false
)

data class Subject(
    val id: String = "",
    val idSubject: String = "",
    val name: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val isActive: Boolean = true,
    val schoolLevel: String = "",
    val totalLessons: Int = 0,
    val totalQuizzes: Int = 0
)

data class Lesson(
    val id: String = "",
    val title: String = "",
    val idSubject: String = "",
    val schoolLevel: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val totalSubBab: Int = 0,
    val subBab: List<SubBab> = listOf(),
    val teacherId: String? = null,
    val coverImage: String? = null
)

@Parcelize
data class SubBab(
    val id: String = "",
    val lessonId: String = "",
    val title: String = "",
    val mediaUrls: Map<String, String> = mapOf(
        "video" to "",
        "pdfLesson" to ""
    ),
    val subtitle: String = "",
    val coverImage: String = "",
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

data class PdfResource(
    override val id: String,
    override val title: String,
    override val bucketName: String,
    override val objectKey: String ,
) : MaterialResource

data class SubtitleResource(
    override val id: String,
    override val title: String,
    override val bucketName: String,
    override val objectKey: String ,
) : MaterialResource

data class StudentOverallProgress(
    val id: String = "",
    val studentId: String = "",
    val totalSubjects: Int = 0,
    val completedSubjects: Int = 0,
    val overallProgressPercentage: Int = 0,
    val totalTimeSpent: Int = 0,
    val quizAverage: Float = 0f,
    val streak: Int = 0,
    val lastActivityDate: Timestamp = Timestamp.now(),
    val subjectProgress: List<StudentSubjectProgress> = listOf()
)

data class StudentSubjectProgress(
    val id: String = "",
    val studentId: String = "",
    val subjectId: String = "",
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

data class StudentLessonProgress(
    val id: String = "",
    val studentId: String = "",
    val lessonId: String = "",
    val lessonTitle: String = "",
    val subjectId: String = "",
    val progressPercentage: Int = 0,
    val completedSubBabs: Int = 0,
    val totalSubBabs: Int = 0,
    val subBabProgress: List<StudentSubBabProgress> = listOf(),
    val quizAverage: Float = 0f,
    val quizScores: List<Float> = listOf(), // List nilai quiz untuk lesson ini
    val lastActivityDate: Timestamp = Timestamp.now(),
    val totalTimeSpent: Int = 0, // dalam menit
    var isCompleted: Boolean = false
)

data class StudentSubBabProgress(
    val id: String = "",
    val studentId: String = "",
    val subBabId: String = "",
    val lessonId: String = "",
    val completedMaterials: Map<String, Boolean> = mapOf(
        "pdf" to false,
        "video" to false,
        "quiz" to false
    ),
    val quizScore: Float = 0f,
    val timeSpent: Int = 0, // dalam menit
    val lastActivityDate: Timestamp = Timestamp.now(),
    var isCompleted: Boolean = false
)

@Parcelize
data class Bookmark(
    val id: String = "",
    val studentId: String = "",
    val subjectId: String = "",
    val lessonId: String = "",
    val subBabId: String = "",
    val completedMaterials: Map<String, Boolean> = mapOf(
        "pdf" to false,
        "video" to false,
        "quiz" to false
    ),
    val lessonTitle: String = "",
    val subBabTitle: String = "",
    val subjectName: String = "",
    val schoolLevel: String = "",
    val coverImage: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val lastAccessedAt: Timestamp = Timestamp.now()
) : Parcelable

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