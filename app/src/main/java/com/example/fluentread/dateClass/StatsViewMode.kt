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
