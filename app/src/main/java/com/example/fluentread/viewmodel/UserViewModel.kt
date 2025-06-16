package com.example.fluentread.viewmodel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluentread.dateClass.Book
import com.example.fluentread.dateClass.FinishedBookWithFlashcards
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

    var showTextSettingsDialog by mutableStateOf(false)
        private set

    var readChapters by mutableStateOf<Map<String, Set<Int>>>(emptyMap())
        private set

    var totalChapters by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    var currentBooks by mutableStateOf<List<Book>>(emptyList())
        private set

    var isCurrentBooksLoading by mutableStateOf(true)
        private set

    var currentBooksError by mutableStateOf<String?>(null)
        private set

    var sessionSource: String? by mutableStateOf(null)


    fun loadCurrentBooks() {
        val uid = userId
        if (uid == null) {
            currentBooksError = "Nie zalogowano użytkownika"
            isCurrentBooksLoading = false
            return
        }

        viewModelScope.launch {
            try {
                val currentReadDocs = db.collection("users")
                    .document(uid)
                    .collection("currentRead")
                    .get()
                    .await()

                val bookIds = currentReadDocs.documents.map { it.id }

                val fetchedBooks = bookIds.mapNotNull { bookId ->
                    try {
                        val snapshot = db.collection("books").document(bookId).get().await()
                        if (snapshot.exists()) {
                            Book(
                                id = snapshot.id,
                                title = snapshot.getString("title") ?: "Nieznany tytuł",
                                cover = snapshot.getString("cover") ?: "",
                                author = snapshot.getString("author") ?: "Nieznany autor",
                                genre = (snapshot.get("genre") as? List<*>)?.filterIsInstance<String>()?.toTypedArray()
                                    ?: emptyArray(),
                                level = snapshot.getString("level") ?: "",
                                wordCount = 0.0
                            )
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }

                currentBooks = fetchedBooks
            } catch (e: Exception) {
                currentBooksError = "Błąd ładowania: ${e.localizedMessage}"
            } finally {
                isCurrentBooksLoading = false
            }
        }
    }


    fun toggleTextSettingsDialog() {
        showTextSettingsDialog = !showTextSettingsDialog
    }

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
                val chapterNumbers = chaptersSnapshot.documents.mapNotNull { it.getDouble("number") }

                chapters = chapterNumbers
                totalChapters = totalChapters.toMutableMap().apply {
                    put(bookId, chapterNumbers.size)
                }

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

            val ref = db.collection("users").document(userId)
                .collection("currentRead").document(bookId)
                .collection("bookmarks").document(chapter)

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
                val snap = db.collection("users").document(userId)
                    .collection("currentRead").document(bookId)
                    .collection("bookmarks").document(chapter)
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
        contextSentence: String,
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
                    "context" to contextSentence,
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
               sessionFinished = true
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
    fun resetFlashcardSession() {
        sessionFinished = false
        flashcardIndex = 0
        knowCount = 0
        dontKnowCount = 0
        answerHistory.clear()
        showTranslation = false
    }


    fun toggleTranslation() {
        showTranslation = !showTranslation
    }

    fun incrementKnow() {
        knowCount++
        answerHistory.add(true)
    }

    fun incrementDontKnow() {
        dontKnowCount++
        answerHistory.add(false)
    }

    fun saveChapterAsRead(userId: String, bookId: String, chapter: Int) {
        val db = FirebaseFirestore.getInstance()
        val progressRef = db.collection("users").document(userId)
            .collection("readProgress").document(bookId)

        progressRef.get().addOnSuccessListener { snapshot ->
            val chaptersMap = snapshot.get("chaptersRead") as? Map<String, Long> ?: emptyMap()

            if (!chaptersMap.containsKey(chapter.toString())) {
                val updatedMap = chaptersMap.toMutableMap()
                updatedMap[chapter.toString()] = System.currentTimeMillis()

                progressRef.set(
                    mapOf(
                        "bookId" to bookId,
                        "chaptersRead" to updatedMap
                    )
                )
            }
        }
    }

    fun getChaptersRead(userId: String, bookId: String, onResult: (List<Int>) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .collection("readProgress").document(bookId)
            .get()
            .addOnSuccessListener { snapshot ->
                val chaptersMap = snapshot.get("chaptersRead") as? Map<String, Long> ?: emptyMap()
                val chaptersSet = chaptersMap.keys.mapNotNull { it.toIntOrNull() }.toSet()

                readChapters = readChapters.toMutableMap().apply {
                    put(bookId, chaptersSet)
                }

                onResult(chaptersSet.toList())
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    fun updateLastRead(userId: String, bookId: String, chapter: Int) {
        val bookRef = db.collection("books").document(bookId)
        val currentReadRef = db.collection("users").document(userId)
            .collection("currentRead").document(bookId)

        db.collection("users").document(userId)
            .update("lastRead", bookRef)
            .addOnSuccessListener {
                Log.d("UserViewModel", "Zaktualizowano lastRead na $bookId")

                currentReadRef.get()
                    .addOnSuccessListener { snapshot ->
                        val data = mutableMapOf<String, Any>("chapter" to chapter)
                        if (!snapshot.exists()) {
                            data["startedAt"] = System.currentTimeMillis()
                            Log.d("UserViewModel", "Ustawiam startedAt dla $bookId")
                        }

                        currentReadRef.set(data)
                            .addOnSuccessListener {
                                Log.d("UserViewModel", "Zapisano/zaaktualizowano currentRead dla $bookId -> chapter $chapter")
                            }
                            .addOnFailureListener { e ->
                                Log.e("UserViewModel", "Błąd przy zapisie currentRead: ${e.localizedMessage}")
                            }
                    }
            }
            .addOnFailureListener { e ->
                Log.e("UserViewModel", "Błąd przy zapisie lastRead: ${e.localizedMessage}")
            }
    }



    fun loadLastReadProgress(userId: String, bookId: String, onResult: (String?, Int?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .collection("currentRead").document(bookId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val chapter = snapshot.getLong("chapter")?.toInt()
                    onResult(bookId, chapter)
                } else {
                    onResult(null, null)
                }
            }
            .addOnFailureListener {
                onResult(null, null)
            }
    }


    //statystyki
    fun getTotalReadChaptersInMonth(
        userId: String,
        year: Int,
        month: Int,
        onResult: (Int) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .collection("readingStats")
            .get()
            .addOnSuccessListener { snapshot ->
                val total = snapshot.documents.sumOf { doc ->
                    val parts = doc.id.split("-")
                    if (parts.size == 3 && parts[0].toIntOrNull() == year && parts[1].toIntOrNull() == month) {
                        doc.getLong("count")?.toInt() ?: 0
                    } else 0
                }
                onResult(total)
            }
            .addOnFailureListener {
                onResult(0)
            }
    }

    fun getTotalReadChaptersInYear(
        userId: String,
        year: Int,
        onResult: (Int) -> Unit
    ) {
        db.collection("users").document(userId)
            .collection("readingStats")
            .get()
            .addOnSuccessListener { snapshot ->
                val total = snapshot.documents.sumOf { doc ->
                    val parts = doc.id.split("-")
                    if (parts.size == 3 && parts[0].toIntOrNull() == year) {
                        doc.getLong("count")?.toInt() ?: 0
                    } else 0
                }
                onResult(total)
            }
            .addOnFailureListener {
                onResult(0)
            }
    }

    fun getTotalDailyReadingStats(
        userId: String,
        onResult: (Map<String, Int>) -> Unit
    ) {
        db.collection("users").document(userId)
            .collection("readingStats")
            .get()
            .addOnSuccessListener { snapshot ->
                val result = snapshot.documents.associate { doc ->
                    doc.id to (doc.getLong("count")?.toInt() ?: 0)
                }
                onResult(result.toSortedMap())
            }
            .addOnFailureListener {
                onResult(emptyMap())
            }
    }

    fun incrementTodayReadingCount(userId: String) {
        val db = FirebaseFirestore.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayDate = dateFormat.format(Date())

        val statRef = db.collection("users")
            .document(userId)
            .collection("readingStats")
            .document(todayDate)

        statRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    statRef.update("count", FieldValue.increment(1))
                } else {
                    statRef.set(mapOf("count" to 1))
                }
            }
            .addOnFailureListener { e ->
                Log.e("ReadingStats", "Błąd przy aktualizacji stats: ${e.localizedMessage}")
            }
    }

    fun getReadingStreak(userId: String, onResult: (Int) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .collection("readingStats")
            .get()
            .addOnSuccessListener { snapshot ->
                val readingDays = snapshot.documents.map { it.id }.toSet()

                if (readingDays.isEmpty()) {
                    onResult(0)
                    return@addOnSuccessListener
                }

                val calendar = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                var streak = 0

                while (true) {
                    val date = dateFormat.format(calendar.time)

                    if (readingDays.contains(date)) {
                        streak++
                        calendar.add(Calendar.DAY_OF_MONTH, -1)
                    } else {
                        break
                    }
                }

                onResult(streak)
            }
            .addOnFailureListener {
                onResult(0)
            }
    }

    fun removeBookFromCurrentRead(bookId: String) {
        val userId = this.userId ?: return
        val userDoc = db.collection("users").document(userId)
        val currentReadRef = userDoc.collection("currentRead").document(bookId)
        val readProgressRef = userDoc.collection("readProgress").document(bookId)
        val bookmarksRef = currentReadRef.collection("bookmarks")
        val lastReadField = "lastRead"

        currentReadRef.delete()
            .addOnSuccessListener {
                Log.d("CurrentRead", "Usunięto książkę $bookId z currentRead")

                readProgressRef.delete()
                    .addOnSuccessListener {
                        Log.d("ReadProgress", "Usunięto postęp czytania dla książki $bookId")
                    }
                    .addOnFailureListener { e ->
                        Log.e("ReadProgress", "Błąd usuwania postępu: ${e.localizedMessage}")
                    }
                bookmarksRef.get()
                    .addOnSuccessListener { snapshot ->
                        val batch = db.batch()
                        for (doc in snapshot.documents) {
                            batch.delete(doc.reference)
                        }
                        batch.commit()
                            .addOnSuccessListener {
                                Log.d("Bookmarks", "Usunięto zakładki dla książki $bookId")
                            }
                            .addOnFailureListener { e ->
                                Log.e("Bookmarks", "Błąd przy usuwaniu zakładek: ${e.localizedMessage}")
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Bookmarks", "Błąd przy odczycie zakładek: ${e.localizedMessage}")
                    }

                userDoc.get()
                    .addOnSuccessListener { userSnapshot ->
                        val lastReadRef = userSnapshot.getDocumentReference(lastReadField)
                        if (lastReadRef?.id == bookId) {
                            userDoc.update(lastReadField, null)
                                .addOnSuccessListener {
                                    Log.d("LastRead", "Usunięto lastRead bo wskazywał na $bookId")

                                    userDoc.collection("currentRead")
                                        .get()
                                        .addOnSuccessListener { currentReadSnapshot ->
                                            val firstDoc = currentReadSnapshot.documents.firstOrNull()
                                            if (firstDoc != null) {
                                                val newBookRef = db.collection("books").document(firstDoc.id)
                                                userDoc.update(lastReadField, newBookRef)
                                                    .addOnSuccessListener {
                                                        Log.d("LastRead", "Ustawiono nowy lastRead na ${firstDoc.id}")
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Log.e("LastRead", "Błąd przy ustawianiu nowego lastRead: ${e.localizedMessage}")
                                                    }
                                            }
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("LastRead", "Błąd usuwania lastRead: ${e.localizedMessage}")
                                }
                        }
                    }

                loadCurrentBooks()
            }
            .addOnFailureListener { e ->
                Log.e("CurrentRead", "Błąd usuwania książki: ${e.localizedMessage}")
            }
    }

    fun addToFinished(bookId: String) {
        val userId = this.userId ?: return
        val userRef = db.collection("users").document(userId)
        val bookRef = db.collection("books").document(bookId)
        val currentReadRef = userRef.collection("currentRead").document(bookId)
        val finishedCollectionRef = userRef.collection("finishedBooks")
        val bookmarksRef = currentReadRef.collection("bookmarks")

        currentReadRef.get().addOnSuccessListener { currentReadSnapshot ->
            val startedAt = currentReadSnapshot.getLong("startedAt") ?: System.currentTimeMillis()
            val endedAt = System.currentTimeMillis()

            val finishedData = mapOf(
                "bookId" to bookRef,
                "startedAt" to startedAt,
                "endedAt" to endedAt
            )

            finishedCollectionRef.add(finishedData)
                .addOnSuccessListener {
                    Log.d("FinishedBooks", "Dodano wpis ukończenia książki $bookId")
                }
                .addOnFailureListener { e ->
                    Log.e("FinishedBooks", "Błąd zapisu: ${e.localizedMessage}")
                }

            bookmarksRef.get()
                .addOnSuccessListener { snapshot ->
                    val batch = db.batch()
                    for (doc in snapshot.documents) {
                        batch.delete(doc.reference)
                    }
                    batch.commit()
                        .addOnSuccessListener {
                            Log.d("Bookmarks", "Usunięto zakładki dla książki $bookId")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Bookmarks", "Błąd przy usuwaniu zakładek: ${e.localizedMessage}")
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("Bookmarks", "Błąd przy odczycie zakładek: ${e.localizedMessage}")
                }
        }
    }


    fun checkIfBookIsFinishedOrCurrent(
        userId: String,
        bookId: String,
        onResult: (isFinished: Boolean, isCurrent: Boolean) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(userId)
        val finishedBooksRef = userRef.collection("finishedBooks")
        val currentReadDoc = userRef.collection("currentRead").document(bookId)

        finishedBooksRef
            .whereEqualTo("bookId", db.collection("books").document(bookId))
            .limit(1)
            .get()
            .addOnSuccessListener { finishedQuery ->
                val isFinished = !finishedQuery.isEmpty
                currentReadDoc.get()
                    .addOnSuccessListener { docSnapshot ->
                        val isCurrent = docSnapshot.exists()
                        onResult(isFinished, isCurrent)
                    }
                    .addOnFailureListener {
                        onResult(isFinished, false)
                    }
            }
            .addOnFailureListener {
                onResult(false, false)
            }
    }


    fun shouldAllowChat(bookId: String, chapter: Int, onResult: (Boolean) -> Unit) {
        val uid = userId ?: return onResult(false)
        val db = FirebaseFirestore.getInstance()

        val userRef = db.collection("users").document(uid)
        val finishedBooksRef = userRef.collection("finishedBooks")
        val currentReadRef = userRef.collection("currentRead").document(bookId)

        finishedBooksRef
            .whereEqualTo("bookId", db.collection("books").document(bookId))
            .limit(1)
            .get()
            .addOnSuccessListener { finishedQuery ->
                if (!finishedQuery.isEmpty) {
                    onResult(true)
                } else {
                    currentReadRef.get()
                        .addOnSuccessListener { currentReadDoc ->
                            val currentChapter = currentReadDoc.getLong("chapter")?.toInt()
                            onResult(currentChapter != null && currentChapter > chapter)
                        }
                        .addOnFailureListener {
                            onResult(false)
                        }
                }
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    //Statystyki lista przeczytanych książek

    fun loadFinishedBooksWithFlashcards(
        userId: String,
        onResult: (List<FinishedBookWithFlashcards>) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(userId)
        val finishedBooksRef = userRef.collection("finishedBooks")
        val flashcardsRef = userRef.collection("flashcards")

        finishedBooksRef.get()
            .addOnSuccessListener { finishedSnapshot ->
                val results = mutableListOf<FinishedBookWithFlashcards>()
                val finishedList = finishedSnapshot.documents

                if (finishedList.isEmpty()) {
                    onResult(emptyList())
                    return@addOnSuccessListener
                }

                var completed = 0

                for (doc in finishedList) {
                    val bookRef = doc.getDocumentReference("bookId")
                    val bookId = bookRef?.id ?: continue
                    val startedAt = doc.getLong("startedAt") ?: continue
                    val endedAt = doc.getLong("endedAt") ?: continue

                    flashcardsRef
                        .whereEqualTo("bookId", bookId)
                        .whereGreaterThanOrEqualTo("timestamp", startedAt)
                        .whereLessThanOrEqualTo("timestamp", endedAt)
                        .get()
                        .addOnSuccessListener { flashcardSnapshot ->
                            val flashcardCount = flashcardSnapshot.size()
                            results.add(
                                FinishedBookWithFlashcards(
                                    bookId = bookId,
                                    startedAt = startedAt,
                                    endedAt = endedAt,
                                    flashcardCount = flashcardCount
                                )
                            )
                            completed++
                            if (completed == finishedList.size) {
                                onResult(results)
                            }
                        }
                        .addOnFailureListener {
                            completed++
                            if (completed == finishedList.size) {
                                onResult(results)
                            }
                        }
                }
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }
    fun incrementFlashcardRepetitionCount(userId: String) {
        val db = FirebaseFirestore.getInstance()

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val docRef = db.collection("users").document(userId)
            .collection("flashcardStats").document(currentDate)

        docRef.set(
            mapOf("count" to FieldValue.increment(1)),
            com.google.firebase.firestore.SetOptions.merge()
        )
    }
    fun getFlashcardStatsLastNDays(
        userId: String,
        days: Int,
        onResult: (Map<String, Int>) -> Unit
    ) {
        val calendar = Calendar.getInstance()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateThresholds = (0 until days).map {
            calendar.add(Calendar.DAY_OF_YEAR, -if (it == 0) 0 else 1)
            dateFormatter.format(calendar.time)
        }.reversed()

        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .collection("flashcardStats")
            .get()
            .addOnSuccessListener { snapshot ->
                val result = mutableMapOf<String, Int>()
                for (date in dateThresholds) {
                    val count = snapshot.documents.find { it.id == date }?.getLong("count")?.toInt() ?: 0
                    result[date] = count
                }
                onResult(result)
            }
            .addOnFailureListener { onResult(emptyMap()) }
    }
    fun getFlashcardStatsInMonth(
        userId: String,
        year: Int,
        month: Int,
        onResult: (Map<String, Int>) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .collection("flashcardStats")
            .get()
            .addOnSuccessListener { snapshot ->
                val result = mutableMapOf<String, Int>()
                for (doc in snapshot.documents) {
                    val date = doc.id
                    val count = doc.getLong("count")?.toInt() ?: 0
                    val parts = date.split("-")
                    if (parts.size == 3) {
                        val y = parts[0].toIntOrNull()
                        val m = parts[1].toIntOrNull()
                        if (y == year && m == month) {
                            result[date] = count
                        }
                    }
                }
                onResult(result.toSortedMap())
            }
            .addOnFailureListener {
                onResult(emptyMap())
            }
    }

    fun getFlashcardStatsInYear(
        userId: String,
        year: Int,
        onResult: (Map<String, Int>) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .collection("flashcardStats")
            .get()
            .addOnSuccessListener { snapshot ->
                val result = mutableMapOf<String, Int>()
                for (doc in snapshot.documents) {
                    val date = doc.id
                    val count = doc.getLong("count")?.toInt() ?: 0
                    val parts = date.split("-")
                    if (parts.size == 3) {
                        val y = parts[0].toIntOrNull()
                        val m = parts[1].toIntOrNull()
                        if (y == year && m != null) {
                            val key = m.toString().padStart(2, '0')
                            result[key] = result.getOrDefault(key, 0) + count
                        }
                    }
                }
                onResult(result.toSortedMap())
            }
            .addOnFailureListener {
                onResult(emptyMap())
            }
    }

    fun getReadingStatsBetween(
        userId: String,
        startDate: Date,
        endDate: Date,
        onResult: (Map<String, Int>) -> Unit
    ) {
        getTotalDailyReadingStats(userId) { fullMap ->
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val filtered = fullMap.filter { (dateStr, _) ->
                try {
                    val date = formatter.parse(dateStr)
                    date != null && date.after(startDate) && date.before(endDate) || date == startDate || date == endDate
                } catch (e: Exception) {
                    false
                }
            }
            onResult(filtered.toSortedMap())
        }
    }
    fun getFlashcardStatsBetween(
        userId: String,
        startDate: Date,
        endDate: Date,
        onResult: (Map<String, Int>) -> Unit
    ) {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        db.collection("users").document(userId)
            .collection("flashcardStats")
            .get()
            .addOnSuccessListener { snapshot ->
                val result = mutableMapOf<String, Int>()
                for (doc in snapshot.documents) {
                    val dateStr = doc.id
                    val count = doc.getLong("count")?.toInt() ?: 0
                    try {
                        val date = dateFormatter.parse(dateStr)
                        if (date != null && (date in startDate..endDate)) {
                            result[dateStr] = count
                        }
                    } catch (_: Exception) { }
                }
                onResult(result.toSortedMap())
            }
            .addOnFailureListener {
                onResult(emptyMap())
            }
    }
    fun getTotalDailyReadingStatsInMonth(
        userId: String,
        year: Int,
        month: Int,
        onResult: (Map<String, Int>) -> Unit
    ) {
        db.collection("users").document(userId)
            .collection("readingStats")
            .get()
            .addOnSuccessListener { snapshot ->
                val result = snapshot.documents.mapNotNull { doc ->
                    val parts = doc.id.split("-")
                    val y = parts.getOrNull(0)?.toIntOrNull()
                    val m = parts.getOrNull(1)?.toIntOrNull()
                    if (y == year && m == month) {
                        doc.id to (doc.getLong("count")?.toInt() ?: 0)
                    } else null
                }.toMap()
                onResult(result.toSortedMap())
            }
            .addOnFailureListener {
                onResult(emptyMap())
            }
    }

    fun getMonthlyReadingStatsInYear(
        userId: String,
        year: Int,
        onResult: (Map<String, Int>) -> Unit
    ) {
        val monthCounts = (1..12).associate { "%02d".format(it) to 0 }.toMutableMap()

        db.collection("users").document(userId)
            .collection("readingStats")
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { doc ->
                    val parts = doc.id.split("-")
                    val y = parts.getOrNull(0)?.toIntOrNull()
                    val m = parts.getOrNull(1)?.toIntOrNull()
                    if (y == year && m != null) {
                        val key = "%02d".format(m)
                        monthCounts[key] = monthCounts.getOrDefault(key, 0) + (doc.getLong("count")?.toInt() ?: 0)
                    }
                }
                onResult(monthCounts.toSortedMap())
            }
            .addOnFailureListener {
                onResult(emptyMap())
            }
    }


}