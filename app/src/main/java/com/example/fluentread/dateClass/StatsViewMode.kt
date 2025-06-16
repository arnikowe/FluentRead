package com.example.fluentread.dateClass

enum class StatsViewMode {
    DAILY, MONTHLY, YEARLY
}

data class FinishedBookWithFlashcards(
    val bookId: String,
    val startedAt: Long,
    val endedAt: Long,
    val flashcardCount: Int
)
data class FinishedBookWithFlashcardsDetails(
    val entryId: String,
    val bookId: String,
    val title: String,
    val startedAt: Long,
    val endedAt: Long,
    val flashcards: List<String>
)
