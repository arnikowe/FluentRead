package com.example.fluentread.dateClass

data class Book(
    val id: String,
    val title: String,
    val cover: String,
    val author: String,
    val genre: Array<String>,
    val level: String,
    val wordCount: Double
)
