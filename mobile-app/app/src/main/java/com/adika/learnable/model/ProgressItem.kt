package com.adika.learnable.model

data class StudyHistoryItem(
    val title: String,
    val subtitle: String,
    val timeText: String,
    val schoolLevel: String? = null,
    val subjectId: String? = null,
    val coverImage: String = ""
)

data class LessonProgressItem(
    val lesson: Lesson,
    val progress: StudentLessonProgress
)

data class SubBabDoneItem(
    val title: String,
    val subtitle: String,
    val coverImage: String = "",
    val progress: StudentSubBabProgress
)

data class SubBabProgressHorizontalItem(
    val title: String,
    val subtitle: String,
    val schoolLevel: String? = null,
    val coverImage: String = "",
    val progress: StudentSubBabProgress
)

data class SubBabProgressItem(
    val subBab: SubBab,
    val progress: StudentSubBabProgress
)

data class SubjectProgressItem(
    val subject: Subject,
    val progress: StudentSubjectProgress
)

data class WeekGroup(
    val title: String,
    val items: List<StudyHistoryItem>
)