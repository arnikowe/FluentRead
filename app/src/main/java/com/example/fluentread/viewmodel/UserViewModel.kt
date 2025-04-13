package com.example.fluentread.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

open class UserViewModel : ViewModel() {
    open val db = FirebaseFirestore.getInstance()
    open val userId: String? = FirebaseAuth.getInstance().currentUser?.uid

    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig

    var translateApi by mutableStateOf("")
        private set

    var bookTitle by mutableStateOf("Tytuł książki")
        private set

    var bookAuthor by mutableStateOf("Autor książki")
        private set

    var chapters by mutableStateOf<List<Double>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        fetchRemoteConfig()
    }

    private fun fetchRemoteConfig() {
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                translateApi = remoteConfig.getString("translateApi")
                Log.d("RemoteConfig", "Pobrano klucz API: $translateApi")
            } else {
                Log.e("RemoteConfig", "Błąd pobierania Remote Config")
            }
        }
    }

    fun loadBookDetails(bookId: String) {
        viewModelScope.launch {
            isLoading = true
            try {
                val bookRef = db.collection("books").document(bookId)
                val bookSnapshot = bookRef.get().await()
                bookTitle = bookSnapshot.getString("title") ?: "Tytuł nieznany"
                bookAuthor = bookSnapshot.getString("author") ?: "Autor nieznany"

                val chaptersSnapshot = bookRef.collection("chapters").get().await()
                chapters = chaptersSnapshot.documents.mapNotNull { it.getDouble("number") }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Błąd ładowania danych: ${e.localizedMessage}")
            } finally {
                isLoading = false
            }
        }
    }

    var currentTitle by mutableStateOf("Tytuł książki")
        private set

    var chapterContent by mutableStateOf("")
        private set

    var isBookmarked by mutableStateOf(false)
        private set

    fun loadChapter(bookId: String, chapter: String) {
        viewModelScope.launch {
            try {
                val bookRef = db.collection("books").document(bookId)
                val chapterRef = bookRef.collection("chapters").document(chapter)

                val (bookSnapshot, chapterSnapshot) = listOf(
                    bookRef.get(),
                    chapterRef.get()
                ).map { it.await() }

                currentTitle = bookSnapshot.getString("title") ?: "Tytuł nieznany"
                chapterContent = chapterSnapshot.getString("content") ?: ""

            } catch (e: Exception) {
                Log.e("UserViewModel", "Błąd ładowania danych: ${e.localizedMessage}")
            }
        }
    }

    fun checkBookmark(bookId: String, chapter: String, userId: String, onOffsetLoaded: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val snap = db.collection("books")
                    .document(bookId)
                    .collection("chapters").document(chapter)
                    .collection("bookmarks").document(userId)
                    .get()
                    .await()

                if (snap.exists()) {
                    isBookmarked = true
                    val offset = snap.getLong("scrollOffset")?.toInt() ?: 0
                    onOffsetLoaded(offset)
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Błąd ładowania zakładki: ${e.localizedMessage}")
            }
        }
    }

    fun toggleBookmark(bookId: String, chapter: String, userId: String, offset: Int, content: String) {
        viewModelScope.launch {
            val ref = db.collection("books").document(bookId)
                .collection("chapters").document(chapter)
                .collection("bookmarks").document(userId)

            try {
                if (isBookmarked) {
                    ref.delete()
                    isBookmarked = false
                } else {
                    val previewText = content.split(Regex("[.!?]")).firstOrNull()?.take(100)
                        ?: content.take(100)
                    ref.set(
                        mapOf(
                            "chapter" to chapter,
                            "scrollOffset" to offset,
                            "preview" to previewText,
                            "timestamp" to System.currentTimeMillis()
                        )
                    )
                    isBookmarked = true
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Błąd przy zmianie zakładki: ${e.localizedMessage}")
            }
        }
    }

    fun addFlashcard(bookId: String, chapter: String, userId: String, word: String, onSuccess: () -> Unit, onAlreadyExists: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                val flashcardRef = db.collection("books")
                    .document(bookId)
                    .collection("chapters")
                    .document(chapter)
                    .collection("flashcards")
                    .document(word)

                val snapshot = flashcardRef.get().await()
                if (!snapshot.exists()) {
                    flashcardRef.set(mapOf("word" to word))
                    onSuccess()
                } else {
                    onAlreadyExists()
                }
            } catch (e: Exception) {
                Log.e("FlashcardAdd", "Błąd: ${e.localizedMessage}")
                onError()
            }
        }
    }

    fun translateWord(word: String, onResult: (String) -> Unit) {
        if (translateApi.isBlank()) {
            onResult("Błąd: Brak klucza API")
            return
        }

        val url = "https://translateai.p.rapidapi.com/google/translate/json"
        val client = OkHttpClient()

        val content = JSONObject().put("message", word)

        val json = JSONObject()
            .put("origin_language", "en")
            .put("target_language", "pl")
            .put("json_content", content)

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("x-rapidapi-key", translateApi)
            .addHeader("x-rapidapi-host", "translateai.p.rapidapi.com")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult("Błąd: ${e.localizedMessage}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseText = response.body?.string()
                Log.d("TranslateResponse", "Raw: $responseText")

                if (!response.isSuccessful) {
                    onResult("HTTP ${response.code}")
                    return
                }

                try {
                    val json = JSONObject(responseText)
                    val translatedText = json
                        .optJSONObject("translated_json")
                        ?.optString("message")

                    if (!translatedText.isNullOrBlank()) {
                        onResult(translatedText)
                    } else {
                        onResult("Błąd: brak tłumaczenia")
                    }
                } catch (e: Exception) {
                    onResult("Błąd JSON: ${e.localizedMessage}")
                }
            }
        })
    }

    fun saveFlashcard(
        bookId: String,
        chapter: String,
        userId: String,
        word: String,
        translation: String,
        note: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val flashcardRef = db.collection("users")
                    .document(userId)
                    .collection("flashcards")
                    .document(word.lowercase())

                val data = mapOf(
                    "word" to word,
                    "translation" to translation,
                    "note" to note,
                    "bookId" to bookId,
                    "chapter" to chapter,
                    "timestamp" to System.currentTimeMillis()
                )

                flashcardRef.set(data).await()
                onSuccess()
            } catch (e: Exception) {
                Log.e("SaveFlashcard", "Błąd zapisu fiszki: ${e.localizedMessage}")
                onError()
            }
        }
    }
}
