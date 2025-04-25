package com.example.fluentread.viewmodel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
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

    var isBookmarked by mutableStateOf(false)
        private set

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        fetchRemoteConfig()
    }

    var currentChapter by mutableStateOf<String?>(null)
    var currentBookId by mutableStateOf<String?>(null)

    var readScrollIndex by mutableStateOf(0)
        private set

    var readScrollOffset by mutableStateOf(0)
        private set

    val favoriteIds = mutableStateOf(setOf<String>())

    var flashcardIndex by mutableStateOf(0)
        private set

    var showTranslation by mutableStateOf(false)
        private set

    var showFlashcardSettings by mutableStateOf(false)
        private set

    var flashcardLanguageMode by mutableStateOf("EN")
        private set

    var sessionFinished by mutableStateOf(false)
        private set

    var knowCount by mutableStateOf(0)
        private set

    var dontKnowCount by mutableStateOf(0)
        private set

    val answerHistory = mutableListOf<Boolean>()

    var shuffleFlashcards by mutableStateOf(true)
        private set
    var shuffleEnabled by mutableStateOf(true)
    var currentFlashcards by mutableStateOf<List<DocumentSnapshot>>(emptyList())


    fun updateReadingPosition(index: Int, offset: Int) {
        readScrollIndex = index
        readScrollOffset = offset
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

    fun toggleBookmark(
        bookId: String,
        chapter: String,
        userId: String,
        index: Int,
        offset: Int,
        content: String
    ) {
        viewModelScope.launch {
            Log.d("Bookmark", "Zapisuję zakładkę dla $bookId/$chapter user=$userId, index=$index, offset=$offset")

            val ref = db.collection("books").document(bookId)
                .collection("chapters").document(chapter)
                .collection("bookmarks").document(userId)

            try {
                if (isBookmarked) {
                    ref.delete().await()
                    isBookmarked = false
                } else {
                    val previewText = content.split(Regex("[.!?]"))
                        .firstOrNull()?.take(100) ?: content.take(100)

                    val bookmarkData = mapOf(
                        "chapter" to chapter,
                        "itemIndex" to index,
                        "scrollOffset" to offset,
                        "preview" to previewText,
                        "timestamp" to System.currentTimeMillis()
                    )

                    ref.set(bookmarkData).await()
                    isBookmarked = true
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Błąd przy zapisie/usuaniu zakładki: ${e.localizedMessage}")
            }
        }
    }

    fun checkBookmark(
        bookId: String,
        chapter: String,
        userId: String,
        onLoaded: (Int, Int) -> Unit
    ) {
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
                    val index = snap.getLong("itemIndex")?.toInt() ?: 0
                    val offset = snap.getLong("scrollOffset")?.toInt() ?: 0
                    onLoaded(index, offset)
                } else {
                    isBookmarked = false
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Błąd ładowania zakładki: ${e.localizedMessage}")
            }
        }
    }

    fun translateWord(word: String, sentence: String, onResult: (String) -> Unit) {
        if (translateApi.isBlank()) {
            onResult("Błąd: Brak klucza API")
            return
        }

        val url = "https://api.openai.com/v1/chat/completions"
        val client = OkHttpClient()

        val prompt = "\"$word\". Przetłumacz \"$word\" z języka angielskiego na polski w kontekście tego zdania: \"$sentence\". Zwróć wyłącznie to tłumaczenie."

        val messagesArray = org.json.JSONArray().put(
            JSONObject()
                .put("role", "user")
                .put("content", prompt)
        )

        val json = JSONObject()
            .put("model", "gpt-3.5-turbo")
            .put("messages", messagesArray)
            .put("temperature", 0.3)

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $translateApi")
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
                    val jsonObject = JSONObject(responseText)
                    val translation = jsonObject
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                        .trim()
                    onResult(translation)
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

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun goToPreviousFlashcard() {
        if (flashcardIndex > 0) {
            flashcardIndex--
            showTranslation = false

            if (answerHistory.isNotEmpty()) {
                val last = answerHistory.removeLast()
                if (last) {
                    knowCount--
                } else {
                    dontKnowCount--
                }
            }
        }
    }


    fun goToNextFlashcard(maxIndex: Int) {
        if (flashcardIndex <= maxIndex) {
            flashcardIndex++
            showTranslation = false
            Log.d("FlashcardVM", "goToNextFlashcard -> new index: $flashcardIndex")

            if (flashcardIndex > maxIndex) {
                Log.d("FlashcardVM", "Session finished")
                markSessionAsFinished()
            }
        }
    }

    fun toggleFlashcardSettingsDialog() {
        showFlashcardSettings = !showFlashcardSettings
    }

    fun setFlashcardLanguage(mode: String) {
        flashcardLanguageMode = mode
    }

    fun resetFlashcards(original: List<DocumentSnapshot>) {
        flashcardIndex = 0
        showTranslation = false
        knowCount = 0
        dontKnowCount = 0
        showFlashcardSettings = false
        sessionFinished = false
        currentFlashcards = if (shuffleEnabled) original.shuffled() else original
    }

    fun toggleTranslation() {
        showTranslation = !showTranslation
    }

    fun markSessionAsFinished() {
        sessionFinished = true
    }
    fun incrementKnow() {
        knowCount++
        answerHistory.add(true)
    }

    fun incrementDontKnow() {
        dontKnowCount++
        answerHistory.add(false)
    }

}