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

                currentReadRef.set(mapOf("chapter" to chapter))
                    .addOnSuccessListener {
                        Log.d("UserViewModel", "Zapisano/zaaktualizowano currentRead dla $bookId -> chapter $chapter")
                    }
                    .addOnFailureListener { e ->
                        Log.e("UserViewModel", "Błąd przy zapisie currentRead: ${e.localizedMessage}")
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
            .collection("readProgress")
            .get()
            .addOnSuccessListener { querySnapshot ->
                var totalCount = 0
                for (doc in querySnapshot.documents) {
                    val chaptersMap = doc.get("chaptersRead") as? Map<String, Long> ?: continue
                    totalCount += chaptersMap.values.count { timestamp ->
                        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
                        calendar.get(java.util.Calendar.YEAR) == year &&
                                (calendar.get(java.util.Calendar.MONTH) + 1) == month
                    }
                }
                onResult(totalCount)
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
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .collection("readProgress")
            .get()
            .addOnSuccessListener { querySnapshot ->
                var totalCount = 0
                for (doc in querySnapshot.documents) {
                    val chaptersMap = doc.get("chaptersRead") as? Map<String, Long> ?: continue
                    totalCount += chaptersMap.values.count { timestamp ->
                        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
                        calendar.get(java.util.Calendar.YEAR) == year
                    }
                }
                onResult(totalCount)
            }
            .addOnFailureListener {
                onResult(0)
            }
    }
    fun getTotalDailyReadingStats(
        userId: String,
        onResult: (Map<String, Int>) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .collection("readProgress")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val dayCounts = mutableMapOf<String, Int>()
                for (doc in querySnapshot.documents) {
                    val chaptersMap = doc.get("chaptersRead") as? Map<String, Long> ?: continue
                    for (timestamp in chaptersMap.values) {
                        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
                        val date = "%04d-%02d-%02d".format(
                            calendar.get(java.util.Calendar.YEAR),
                            calendar.get(java.util.Calendar.MONTH) + 1,
                            calendar.get(java.util.Calendar.DAY_OF_MONTH)
                        )
                        dayCounts[date] = (dayCounts[date] ?: 0) + 1
                    }
                }
                onResult(dayCounts)
            }
            .addOnFailureListener {
                onResult(emptyMap())
            }
    }
    fun getReadingStreak(
        userId: String,
        onResult: (Int) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .collection("readProgress")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val readingDays = mutableSetOf<String>()

                for (doc in querySnapshot.documents) {
                    val chaptersMap = doc.get("chaptersRead") as? Map<String, Long> ?: continue
                    for (timestamp in chaptersMap.values) {
                        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
                        val date = "%04d-%02d-%02d".format(
                            calendar.get(java.util.Calendar.YEAR),
                            calendar.get(java.util.Calendar.MONTH) + 1,
                            calendar.get(java.util.Calendar.DAY_OF_MONTH)
                        )
                        readingDays.add(date)
                    }
                }

                if (readingDays.isEmpty()) {
                    onResult(0)
                    return@addOnSuccessListener
                }

                val today = java.util.Calendar.getInstance()
                var streak = 0

                while (true) {
                    val date = "%04d-%02d-%02d".format(
                        today.get(java.util.Calendar.YEAR),
                        today.get(java.util.Calendar.MONTH) + 1,
                        today.get(java.util.Calendar.DAY_OF_MONTH)
                    )

                    if (readingDays.contains(date)) {
                        streak++
                        today.add(java.util.Calendar.DAY_OF_MONTH, -1)
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
    @RequiresApi(Build.VERSION_CODES.O)
    fun getLongestReadingStreak(
        userId: String,
        onResult: (Int) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .collection("readProgress")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val readingDays = mutableSetOf<String>()

                for (doc in querySnapshot.documents) {
                    val chaptersMap = doc.get("chaptersRead") as? Map<String, Long> ?: continue
                    for (timestamp in chaptersMap.values) {
                        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
                        val date = "%04d-%02d-%02d".format(
                            calendar.get(java.util.Calendar.YEAR),
                            calendar.get(java.util.Calendar.MONTH) + 1,
                            calendar.get(java.util.Calendar.DAY_OF_MONTH)
                        )
                        readingDays.add(date)
                    }
                }

                if (readingDays.isEmpty()) {
                    onResult(0)
                    return@addOnSuccessListener
                }

                val sortedDays = readingDays.map {
                    java.time.LocalDate.parse(it)
                }.sorted()

                var maxStreak = 1
                var currentStreak = 1

                for (i in 1 until sortedDays.size) {
                    val prev = sortedDays[i - 1]
                    val curr = sortedDays[i]

                    if (prev.plusDays(1) == curr) {
                        currentStreak++
                        if (currentStreak > maxStreak) {
                            maxStreak = currentStreak
                        }
                    } else {
                        currentStreak = 1
                    }
                }

                onResult(maxStreak)
            }
            .addOnFailureListener {
                onResult(0)
            }
    }
    fun getStreakBadge(maxStreak: Int): String {
        return when {
            maxStreak >= 100 -> "🏆 Legendarny streak! 100+ dni!"
            maxStreak >= 60 -> "🏆 Mistrz czytania! 60+ dni!"
            maxStreak >= 30 -> "🔥 Mega streak! 30+ dni!"
            maxStreak >= 14 -> "🔥🔥🔥 Super streak! 14+ dni!"
            maxStreak >= 7 -> "🔥 Świetny start! 7 dni!"
            maxStreak >= 3 -> "📖 Dobry początek! 3 dni!"
            maxStreak >= 1 -> "📖 Pierwszy dzień!"
            else -> "❗️ Jeszcze brak streaka"
        }
    }
    fun removeBookFromCurrentRead(bookId: String) {
        val userId = this.userId ?: return
        val userDoc = db.collection("users").document(userId)
        val currentReadRef = userDoc.collection("currentRead").document(bookId)
        val readProgressRef = userDoc.collection("readProgress").document(bookId)
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

        userRef.update("finishedBooks", FieldValue.arrayUnion(bookRef))
            .addOnSuccessListener {
                Log.d("FinishedBooks", "Dodano książkę $bookId do finishedBooks")
            }
            .addOnFailureListener { e ->
                Log.e("FinishedBooks", "Błąd przy dodawaniu książki: ${e.localizedMessage}")
            }
    }
    fun checkIfBookIsFinishedOrCurrent(
        userId: String,
        bookId: String,
        onResult: (isFinished: Boolean, isCurrent: Boolean) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(userId)
        val bookRef = db.collection("books").document(bookId)
        val currentReadDoc = userRef.collection("currentRead").document(bookId)

        userRef.get()
            .addOnSuccessListener { userSnapshot ->
                val finishedList = userSnapshot.get("finishedBooks") as? List<*>
                val isFinished = finishedList?.any {
                    (it as? DocumentReference)?.id == bookId
                } ?: false

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

}