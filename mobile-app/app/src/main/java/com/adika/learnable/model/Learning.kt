package com.adika.learnable.model

import com.google.firebase.Timestamp

data class LearningHistory(
    val id: String = "",
    val studentId: String = "",
    val lessonId: String = "",
    val lessonTitle: String = "",
    val completionDate: Timestamp = Timestamp.now(),
    val duration: Int = 0, // dalam menit
    val score: Int = 0, // Nilai yang didapat
)

data class Quiz(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val questions: List<Question> = listOf(),
    val subjectId: String = "",
    val teacherId: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val timeLimit: Int = 0, // Batas waktu dalam menit
    val passingScore: Int = 0, // Nilai minimum untuk lulus
    val isPublished: Boolean = false // Status publikasi quiz
)

data class Question(
    val id: String = "",
    val question: String = "",
    val options: List<String> = listOf(),
    val correctAnswer: Int = 0,
    val explanation: String = "", // Penjelasan jawaban
    val points: Int = 1 // Bobot nilai pertanyaan
)

data class QuizResult(
    val id: String = "",
    val quizId: String = "",
    val quizTitle: String = "",
    val studentId: String = "",
    val score: Int = 0,
    val totalQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val completionDate: Timestamp = Timestamp.now(),
    val answers: List<StudentAnswer> = listOf(),
    val timeTaken: Int = 0, // Waktu yang digunakan dalam menit
    val isPassed: Boolean = false // Status kelulusan
)

data class StudentAnswer(
    val questionId: String = "",
    val selectedOption: Int = -1,
    val isCorrect: Boolean = false,
    val timeSpent: Int = 0 // Waktu yang digunakan untuk menjawab dalam detik
)

data class Progress(
    val id: String = "",
    val studentId: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val progressPercentage: Int = 0,
    val completedLessons: Int = 0,
    val totalLessons: Int = 0,
    val quizAverage: Float = 0f,
    val lastUpdated: Timestamp = Timestamp.now(),
    val streak: Int = 0, // Jumlah hari belajar berturut-turut
    val totalTimeSpent: Int = 0, // Total waktu belajar dalam menit
    val lastActivityDate: Timestamp = Timestamp.now()
)

// Model untuk materi pembelajaran
data class Lesson(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val subjectId: String = "",
    val teacherId: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val duration: Int = 0, // Durasi dalam menit
    val difficulty: String = "", // "easy", "medium", "hard"
    val prerequisites: List<String> = listOf(), // ID materi yang harus diselesaikan sebelumnya
    val attachments: List<String> = listOf() // URL file lampiran
)

// Model untuk mata pelajaran
data class Subject(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val teacherId: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val isActive: Boolean = true,
    val grade: String = "", // Kelas yang dituju
    val totalLessons: Int = 0,
    val totalQuizzes: Int = 0
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
