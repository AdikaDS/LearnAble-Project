package com.adika.learnable.view

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.adika.learnable.R
import com.adika.learnable.databinding.ActivityMainBinding
import com.adika.learnable.model.Lesson
import com.adika.learnable.model.Quiz
import com.adika.learnable.model.QuizQuestion
import com.adika.learnable.model.StudentLessonProgress
import com.adika.learnable.model.StudentOverallProgress
import com.adika.learnable.model.StudentSubjectProgress
import com.adika.learnable.model.SubBab
import com.adika.learnable.model.Subject
import com.adika.learnable.util.LanguageUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val db = FirebaseFirestore.getInstance()

    companion object {
        private const val DISABILITY_SELECTION = "disability_selection"
        private const val STUDENT_DASHBOARD = "student_dashboard"
        private const val PARENT_DASHBOARD = "parent_dashboard"
        private const val TEACHER_DASHBOARD = "teacher_dashboard"
        private const val ADMIN_CONFIRMATION = "admin_confirmation"
        private const val LOGIN = "login"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
//        upQuiz()
        handleDestination()
    }

    override fun attachBaseContext(newBase: Context) {
        val languageCode = LanguageUtils.getLanguagePreference(newBase)
        val context = LanguageUtils.changeLanguage(newBase, languageCode)
        super.attachBaseContext(context)
    }

    private fun upQuiz() {
        val penjumlahanQuiz = Quiz(
            subBabId = "4eFL4FgvWizDfELs7SWf", // ID dari subbab Penjumlahan
            title = "Quiz Penjumlahan",
            description = "Quiz ini akan menguji pemahamanmu tentang penjumlahan bilangan",
            questions = listOf(
                QuizQuestion(
                    question = "Berapakah hasil dari 5 + 7?",
                    options = listOf("10", "12", "14", "16"),
                    correctAnswer = 1,
                    explanation = "5 + 7 = 12"
                ),
                QuizQuestion(
                    question = "Jika Andi memiliki 8 permen dan Budi memberikan 6 permen lagi, berapa total permen Andi?",
                    options = listOf("12", "14", "16", "18"),
                    correctAnswer = 1,
                    explanation = "8 + 6 = 14 permen"
                ),
                QuizQuestion(
                    question = "Berapakah hasil dari 25 + 17?",
                    options = listOf("40", "42", "44", "46"),
                    correctAnswer = 1,
                    explanation = "25 + 17 = 42"
                ),
                QuizQuestion(
                    question = "Jika sebuah kelas memiliki 15 siswa perempuan dan 12 siswa laki-laki, berapa total siswa di kelas tersebut?",
                    options = listOf("25", "26", "27", "28"),
                    correctAnswer = 2,
                    explanation = "15 + 12 = 27 siswa"
                )
            ),
            passingScore = 70f,
            timeLimit = 10 // 10 menit
        )

        val db = FirebaseFirestore.getInstance()

        val docRef = db.collection("quiz").document()

        val subBabWithId = penjumlahanQuiz.copy(id = docRef.id)
        docRef.set(subBabWithId)
    }

    private fun upProgress() {
        val dummyData = StudentOverallProgress(
            studentId = "L9NzsQeyjmZPiFZK25CsLn5jvJy2",
            studentName = "Upnormal Skuad",
            totalSubjects = 2,
            completedSubjects = 1,
            overallProgressPercentage = 50,
            totalTimeSpent = 240, // menit
            streak = 3,
            lastActivityDate = Timestamp.now(),
            subjectProgress = listOf(
                StudentSubjectProgress(
                    studentId = "L9NzsQeyjmZPiFZK25CsLn5jvJy2",
                    subjectId = "mathES",
                    subjectName = "Matematika",
                    progressPercentage = 75,
                    completedLessons = 3,
                    totalLessons = 4,
                    quizAverage = 82.5f,
                    lastUpdated = Timestamp.now(),
                    streak = 2,
                    totalTimeSpent = 120,
                    lastActivityDate = Timestamp.now(),
                    lessonProgress = listOf(
                        StudentLessonProgress(
                            studentId = "L9NzsQeyjmZPiFZK25CsLn5jvJy2",
                            lessonId = "2Q7jMMhilbT3bxWy1XUk",
                            lessonTitle = "Penjumlahan",
                            subjectId = "mathES",
                            subjectName = "Matematika",
                            progressPercentage = 100,
                            completedSubBabs = 3,
                            totalSubBabs = 3,
                            quizScores = listOf(90f, 85f),
                            lastActivityDate = Timestamp.now(),
                            totalTimeSpent = 60,
                            isCompleted = true
                        ),
                        StudentLessonProgress(
                            studentId = "L9NzsQeyjmZPiFZK25CsLn5jvJy2",
                            lessonId = "lesson_002",
                            lessonTitle = "Pengurangan",
                            subjectId = "math_001",
                            subjectName = "Matematika",
                            progressPercentage = 50,
                            completedSubBabs = 1,
                            totalSubBabs = 2,
                            quizScores = listOf(75f),
                            lastActivityDate = Timestamp.now(),
                            totalTimeSpent = 30,
                            isCompleted = false
                        )
                    )
                ),
                StudentSubjectProgress(
                    studentId = "L9NzsQeyjmZPiFZK25CsLn5jvJy2",
                    subjectId = "naturalAndSocialScienceES",
                    subjectName = "Ilmu Pengetahuan Alam",
                    progressPercentage = 25,
                    completedLessons = 1,
                    totalLessons = 4,
                    quizAverage = 60f,
                    lastUpdated = Timestamp.now(),
                    streak = 1,
                    totalTimeSpent = 120,
                    lastActivityDate = Timestamp.now(),
                    lessonProgress = listOf(
                        StudentLessonProgress(
                            studentId = "L9NzsQeyjmZPiFZK25CsLn5jvJy2",
                            lessonId = "q9reyDS2WYIbLdo8j26o",
                            lessonTitle = "Sifat Air",
                            subjectId = "naturalAndSocialScienceES",
                            subjectName = "IPA",
                            progressPercentage = 25,
                            completedSubBabs = 1,
                            totalSubBabs = 4,
                            quizScores = listOf(60f),
                            lastActivityDate = Timestamp.now(),
                            totalTimeSpent = 30,
                            isCompleted = false
                        )
                    )
                )
            )
        )

        val db = FirebaseFirestore.getInstance()

        val docRef = db.collection("student_overall_progress").document()

        val subBabWithId = dummyData.copy(id = docRef.id)
        docRef.set(subBabWithId)


    }

    private fun upSubbab() {

        val subBabs = listOf(
            SubBab(
                title = "Pengenalan Bilangan",
                content = "Materi pengenalan bilangan dasar untuk siswa SD",
                duration = 30,
                mediaUrls = mapOf(
                    "video" to "https://example.com/video/bilangan.mp4",
                    "audio" to "https://example.com/audio/bilangan.mp3",
                    "pdfLesson" to "https://example.com/pdf/bilangan.pdf"
                )
            ),
            SubBab(
                title = "Operasi Penjumlahan",
                content = "Materi operasi penjumlahan dasar",
                duration = 45,
                mediaUrls = mapOf(
                    "video" to "https://example.com/video/penjumlahan.mp4",
                    "audio" to "https://example.com/audio/penjumlahan.mp3",
                    "pdfLesson" to "https://example.com/pdf/penjumlahan.pdf"
                )
            ),
            SubBab(
                title = "Operasi Pengurangan",
                content = "Materi operasi pengurangan dasar",
                duration = 45,
                mediaUrls = mapOf(
                    "video" to "https://example.com/video/pengurangan.mp4",
                    "audio" to "https://example.com/audio/pengurangan.mp3",
                    "pdfLesson" to "https://example.com/pdf/pengurangan.pdf"
                )
            )
        )

        for (subBab in subBabs) {
            val docRef = db.collection("sub_bab").document()
            val subBabWithId = subBab.copy(id = docRef.id)
            docRef.set(subBabWithId)
        }
    }

    private fun upSubject() {
        val dummySubjects = listOf(
            Subject(
                description = "Materi IPAS mengenalkan siswa pada alam, makhluk hidup, benda di sekitar, dan kehidupan sosial secara sederhana dan menyenangkan, sesuai dengan kebutuhan anak disabilitas.",
                idSubject = "naturalAndSocialScience",
                name = "Ilmu Pengetahuan Alam dan Sosial",
                schoolLevel = "sd",
                totalLessons = 0,
                totalQuizzes = 0
            ), Subject(
                description = "Materi Matematika memperkenalkan konsep angka, bentuk, pola, dan operasi hitung dasar dengan cara yang konkret dan interaktif, sesuai kemampuan anak disabilitas.",
                idSubject = "math",
                name = "Matematika",
                schoolLevel = "sd",
                totalLessons = 0,
                totalQuizzes = 0
            ), Subject(
                description = "Materi Bahasa Indonesia membantu siswa memahami dan menggunakan bahasa untuk berbicara, membaca, dan menulis. Pembelajaran disesuaikan agar mudah dimengerti oleh anak disabilitas.",
                idSubject = "bahasaIndonesia",
                name = "Bahasa Indonesia",
                schoolLevel = "sd",
                totalLessons = 0,
                totalQuizzes = 0
            ),
            Subject(
                description = "Materi Bahasa Indonesia di jenjang SMP membantu siswa disabilitas mengembangkan kemampuan memahami, menyampaikan, dan menanggapi informasi dalam bentuk lisan dan tulisan dengan cara yang jelas dan menarik.",
                idSubject = "bahasaIndonesia",
                name = "Bahasa Indonesia",
                schoolLevel = "smp",
                totalLessons = 0,
                totalQuizzes = 0
            ), Subject(
                description = "Materi Matematika SMP mencakup konsep bilangan, aljabar, geometri, dan pemecahan masalah. Pembelajaran dirancang agar mudah dipahami oleh siswa disabilitas melalui pendekatan visual dan langkah-langkah konkret.",
                idSubject = "math",
                name = "Matematika",
                schoolLevel = "smp",
                totalLessons = 0,
                totalQuizzes = 0
            ), Subject(
                description = "Materi IPA mengenalkan siswa pada konsep dasar sains seperti makhluk hidup, energi, bumi, dan alam semesta. Disajikan secara bertahap dan kontekstual agar sesuai dengan kemampuan dan kebutuhan siswa disabilitas.",
                idSubject = "naturalScience",
                name = "Ilmu Pengetahuan Alam",
                schoolLevel = "smp",
                totalLessons = 0,
                totalQuizzes = 0
            )
        )

        for (subject in dummySubjects) {
            val docRef = db.collection("subjects").document()
            val subjectWithId = subject.copy(id = docRef.id)
            docRef.set(subjectWithId)
        }
    }

    private fun upLesson() {
        val dummyLessons = listOf(
            Lesson(
                title = "Mengenal Suara dan Bunyi",
                content = "Bab ini membahas tentang berbagai jenis suara dan bunyi di sekitar kita.",
                difficulty = "easy",
                duration = 30,
                schoolLevel = "sd",
                idSubject = "science",
                disabilityTypes = listOf("Tunanetra")
            ), Lesson(
                title = "Belajar Membaca",
                content = "Bab ini mengajarkan cara membaca dengan metode yang mudah dipahami.",
                difficulty = "medium",
                duration = 60,
                schoolLevel = "sd",
                idSubject = "bahasaIndonesia",
                disabilityTypes = listOf("Tunarungu"),
            ), Lesson(
                title = "Pengenalan Matematika Dasar",
                content = "Bab ini membahas tentang konsep dasar matematika seperti penjumlahan, pengurangan, perkalian, dan pembagian.",
                difficulty = "easy",
                duration = 45,
                schoolLevel = "sd",
                idSubject = "math",
                disabilityTypes = listOf("Tunarungu", "Tunanetra"),
            )
        )

        for (lesson in dummyLessons) {
            val docRef = db.collection("lessons").document()
            val lessonWithId = lesson.copy(id = docRef.id)
            docRef.set(lessonWithId)
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ ->
            try {
                supportActionBar?.title = destination.label
            } catch (e: Exception) {
                Log.e("MainActivity", "Error in navigation: ${e.message}")
            }
        }
    }

    private fun handleDestination() {
        val destination = intent.getStringExtra("destination")
        if (destination != null) {
            try {
                when (destination) {
                    DISABILITY_SELECTION -> {
                        navController.navigate(R.id.disabilitySelectionFragment)
                    }

                    ADMIN_CONFIRMATION -> {
                        navController.navigate(R.id.adminConfirmationFragment)
                    }

                    STUDENT_DASHBOARD -> {
                        navController.navigate(R.id.studentDashboardFragment)
                    }

                    TEACHER_DASHBOARD -> {
                        navController.navigate(R.id.teacherDashboardFragment)
                    }

                    PARENT_DASHBOARD -> {
                        navController.navigate(R.id.parentDashboardFragment)
                    }

                    LOGIN -> {
                        navController.navigate(R.id.loginFragment)
                    }

                    else -> {
                        Log.w("MainActivity", "Invalid destination: $destination")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Navigation error: ${e.message}")
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when (navController.currentDestination?.id) {
            R.id.studentDashboardFragment,
            R.id.teacherDashboardFragment,
            R.id.parentDashboardFragment,
            R.id.adminConfirmationFragment -> {
                showExitConfirmationDialog()
            }

            else -> {
                super.onBackPressed()
            }
        }
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.exit_app))
            .setMessage(getString(R.string.confirm_exit_app))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                finish()
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        return try {
            navController.navigateUp() || super.onSupportNavigateUp()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in navigate up: ${e.message}")
            super.onSupportNavigateUp()
        }
    }

}