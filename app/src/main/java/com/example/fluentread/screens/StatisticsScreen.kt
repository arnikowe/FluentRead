package com.example.fluentread.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluentread.R
import com.example.fluentread.ui.theme.FluentBackgroundDark
import com.example.fluentread.ui.theme.FluentOnPrimaryDark
import com.example.fluentread.ui.theme.FluentSecondaryDark
import com.example.fluentread.viewmodel.UserViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf
import java.text.SimpleDateFormat
import java.util.*
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider







@Composable
fun StatisticsScreen(userViewModel: UserViewModel) {
    val userId = userViewModel.userId ?: return

    var bookData by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    var flashcardData by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }

    var selectedPeriodBooks by remember { mutableStateOf("Tydzień") }
    var selectedPeriodFlashcards by remember { mutableStateOf("Tydzień") }

    var showWeekPickerBooks by remember { mutableStateOf(false) }
    var showMonthPickerBooks by remember { mutableStateOf(false) }
    var showWeekPickerFlashcards by remember { mutableStateOf(false) }
    var showMonthPickerFlashcards by remember { mutableStateOf(false) }
    var showYearPickerBooks by remember { mutableStateOf(false) }
    var showYearPickerFlashcards by remember { mutableStateOf(false) }

    var weekRangeBooks by remember { mutableStateOf<Pair<Date, Date>?>(null) }
    var weekRangeFlashcards by remember { mutableStateOf<Pair<Date, Date>?>(null) }
    var selectedMonthBooks by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var selectedMonthFlashcards by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var selectedYearBooks by remember { mutableStateOf<Int?>(null) }
    var selectedYearFlashcards by remember { mutableStateOf<Int?>(null) }

    var streakDays by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val start = cal.time
        cal.add(Calendar.DAY_OF_WEEK, 6)
        val end = cal.time
        weekRangeBooks = start to end
        weekRangeFlashcards = start to end
        selectedYearBooks = currentYear()
        selectedYearFlashcards = currentYear()

        userViewModel.getReadingStreak(userId) {
            streakDays = it
        }

    }

    fun loadDataForBooks() {
        when (selectedPeriodBooks) {
            "Tydzień" -> {
                userViewModel.getTotalDailyReadingStats(userId) { map ->
                    val last7Days = getLastNDays(7)
                    bookData = last7Days.mapIndexed { index, date ->
                        index.toFloat() to (map[date]?.toFloat() ?: 0f)
                    }
                }
            }
            "Miesiąc" -> {
                val (month, year) = selectedMonthBooks ?: return
                val days = getDaysInMonth(year, month)
                userViewModel.getTotalDailyReadingStatsInMonth(userId, year, month) { map ->
                    bookData = days.mapIndexed { index, date ->
                        index.toFloat() to (map[date]?.toFloat() ?: 0f)
                    }
                }
            }
            "Rok" -> {
                val year = selectedYearBooks ?: return
                userViewModel.getMonthlyReadingStatsInYear(userId, year) { map ->
                    val months = (1..12).map { it.toString().padStart(2, '0') }
                    bookData = months.mapIndexed { index, month ->
                        index.toFloat() to (map[month]?.toFloat() ?: 0f)
                    }
                }
            }
        }
    }

    fun loadDataForFlashcards() {
        when (selectedPeriodFlashcards) {
            "Tydzień" -> {
                userViewModel.getFlashcardStatsLastNDays(userId, 7) { map ->
                    val last7Days = getLastNDays(7)
                    flashcardData = last7Days.mapIndexed { index, date ->
                        index.toFloat() to (map[date]?.toFloat() ?: 0f)
                    }
                }
            }
            "Miesiąc" -> {
                val (month, year) = selectedMonthFlashcards ?: return
                val days = getDaysInMonth(year, month)
                Log.d("loadData", "selectedMonthFlashcards=$selectedMonthFlashcards")
                userViewModel.getFlashcardStatsInMonth(userId, year, month) { map ->
                    Log.d("loadData", "Fetched map size=${map.size}")
                    flashcardData = days.mapIndexed { index, date ->
                        index.toFloat() to (map[date]?.toFloat() ?: 0f)
                    }
                }
            }
            "Rok" -> {
                val year = selectedYearFlashcards ?: return
                userViewModel.getFlashcardStatsInYear(userId, year) { map ->
                    val months = (1..12).map { it.toString().padStart(2, '0') }
                    flashcardData = months.mapIndexed { index, month ->
                        index.toFloat() to (map[month]?.toFloat() ?: 0f)
                    }
                }
            }
        }
    }

    LaunchedEffect(selectedPeriodBooks, selectedMonthBooks, selectedYearBooks) {
        loadDataForBooks()
    }

    LaunchedEffect(selectedPeriodFlashcards, selectedMonthFlashcards, selectedYearFlashcards) {
        Log.d("LaunchedEffect", "Loading flashcards: $selectedPeriodFlashcards, $selectedMonthFlashcards, $selectedYearFlashcards")
        loadDataForBooks()
        loadDataForFlashcards()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FluentOnPrimaryDark)
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = FluentBackgroundDark)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_streak),
                    contentDescription = "Streak Icon",
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${streakDays}-dniowy streak nauki!",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEDE6B1),
                    fontSize = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionWithChart(
            title = "Ilość przeczytanych rozdziałów",
            selectedPeriod = selectedPeriodBooks,
            onPeriodSelected = {
                selectedPeriodBooks = it
                showWeekPickerBooks = it == "Tydzień"
                showMonthPickerBooks = it == "Miesiąc"
                showYearPickerBooks = it == "Rok"
                if (it == "Tydzień") {
                    weekRangeBooks = null
                }
            },
            chartEntries = bookData,
            xLabels = when (selectedPeriodBooks) {
                "Tydzień" -> weekRangeBooks?.let { getLabelsBetweenDates(it.first, it.second) } ?: emptyList()
                "Miesiąc" -> selectedMonthBooks?.let { getDaysInMonth(it.second, it.first).map { date -> date.substring(8) } } ?: emptyList()
                "Rok" -> (1..12).map {
                    listOf(
                        "St", "Lu", "Mr", "Kw", "Mj", "Cz", "Lp", "Si", "Wr", "Pa", "Li", "Gr"
                    )[it - 1]
                }
                else -> emptyList()
            }
            ,
            weekRange = weekRangeBooks,
            month = selectedMonthBooks,
            year = selectedYearBooks
        )


        Spacer(modifier = Modifier.height(24.dp))
        Log.d("SectionChartDebug", "selectedPeriod=$selectedPeriodFlashcards, weekRange=$weekRangeFlashcards, month=$selectedMonthFlashcards, year=$selectedYearFlashcards")

        SectionWithChart(
            title = "Ilość powtórzonych fiszek",
            selectedPeriod = selectedPeriodFlashcards,
            onPeriodSelected = {
                selectedPeriodFlashcards = it
                showWeekPickerFlashcards = it == "Tydzień"
                showMonthPickerFlashcards = it == "Miesiąc"
                showYearPickerFlashcards = it == "Rok"
                if (it == "Tydzień") {
                    weekRangeFlashcards = null
                }
            },
            chartEntries = flashcardData,
            xLabels = when (selectedPeriodFlashcards) {
                "Tydzień" -> weekRangeFlashcards?.let { getLabelsBetweenDates(it.first, it.second) } ?: emptyList()
                "Miesiąc" -> selectedMonthFlashcards?.let { getDaysInMonth(it.second, it.first).map { date -> date.substring(8) } } ?: emptyList()
                "Rok" -> (1..12).map {
                    listOf(
                        "St", "Lu", "Mr", "Kw", "Mj", "Cz", "Lp", "Si", "Wr", "Pa", "Li", "Gr"
                    )[it - 1]
                }
                else -> emptyList()
            },
            weekRange = weekRangeFlashcards,
            month = selectedMonthFlashcards,
            year = selectedYearFlashcards
        )



        if (showWeekPickerBooks) {
            WeekPickerDialog(
                onDismiss = { showWeekPickerBooks = false },
                onWeekSelected = { start, end ->
                    Log.d("WeekPickerBooks", "Picked week: $start – $end")
                    weekRangeBooks = start to end
                    userViewModel.getReadingStatsBetween(userId, start, end) { map ->
                        bookData = map.entries.mapIndexed { index, entry ->
                            index.toFloat() to entry.value.toFloat()
                        }
                    }
                    showWeekPickerBooks = false
                }
            )
        }
        if (showWeekPickerFlashcards) {
            WeekPickerDialog(
                onDismiss = { showWeekPickerFlashcards = false },
                onWeekSelected = { start, end ->
                    Log.d("WeekPickerFlashcards", "Picked week: $start – $end")
                    weekRangeFlashcards = start to end
                    userViewModel.getFlashcardStatsBetween(userId, start, end) { map ->
                        flashcardData = map.entries.mapIndexed { index, entry ->
                            index.toFloat() to entry.value.toFloat()
                        }
                    }
                    showWeekPickerFlashcards = false
                }
            )
        }


        if (showMonthPickerBooks) {
            MonthPickerDialog(
                onDismiss = { showMonthPickerBooks = false },
                currentMonth = currentMonth() - 1,
                currentYear = currentYear()
            ) { month: Int, year: Int ->
                selectedMonthBooks = month to year
                showMonthPickerBooks = false
            }


        }
        if (showMonthPickerFlashcards) {
            MonthPickerDialog(
                onDismiss = { showMonthPickerFlashcards = false },
                currentMonth = currentMonth() - 1,
                currentYear = currentYear()
            ) { month: Int, year: Int ->
                selectedMonthFlashcards = month to year
                loadDataForFlashcards()
                showMonthPickerFlashcards = false
            }

        }

        if (showYearPickerBooks) {
            YearPickerDialog(
                initialYear = currentYear(),
                onDismiss = { showYearPickerBooks = false },
                onYearSelected = {  year ->
                    selectedYearBooks = year
                    userViewModel.getTotalReadChaptersInYear(userId, year) {
                        bookData = listOf(1f to it.toFloat())
                    }
                }
            )
        }

        if (showYearPickerFlashcards) {
            YearPickerDialog(
                initialYear = currentYear(),
                onDismiss = { showYearPickerFlashcards = false },
                onYearSelected = { year ->
                    userViewModel.getFlashcardStatsInYear(userId, year) { map ->
                        val months = (1..12).map { it.toString().padStart(2, '0') }
                        flashcardData = months.mapIndexed { index, month ->
                            index.toFloat() to (map[month]?.toFloat() ?: 0f)
                        }
                    }

                }
            )
        }


    }
}

@Composable
fun SectionWithChart(
    title: String,
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit,
    chartEntries: List<Pair<Float, Float>>,
    xLabels: List<String> = emptyList(),
    weekRange: Pair<Date, Date>? = null,
    month: Pair<Int, Int>? = null,
    year: Int? = null
)
 {
    val options = listOf("Tydzień", "Miesiąc", "Rok")

    Text(
        title,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFEDE6B1),
        fontSize = 16.sp
    )
    Divider(color = Color(0xFFEDE6B1), thickness = 1.dp)

         PeriodSelector(
             selectedPeriod = selectedPeriod,
             onPeriodSelected = onPeriodSelected,
             weekRange = weekRange,
             month = month,
             year = year
         )
     Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(vertical = 8.dp)
            .background(Color.White, shape = MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center
    ) {
        val visibleEntries = chartEntries.takeLast(when (selectedPeriod) {
            "Tydzień" -> 7
            "Miesiąc" -> 31
            "Rok" -> 12
            else -> chartEntries.size
        })


        val model = entryModelOf(visibleEntries.mapIndexed { index, (_, y) -> entryOf(index.toFloat(), y) })
        val labels = if (xLabels.isEmpty()) {
            chartEntries.indices.map { it.toString() }
        } else {
            xLabels
        }
        val xRange = if (visibleEntries.isNotEmpty()) 0f to (visibleEntries.size - 1).toFloat() else 0f to 1f
        val axisValuesOverrider = AxisValuesOverrider.fixed(minX = xRange.first, maxX = xRange.second)

        val pointDrawer = shapeComponent(shape = Shapes.pillShape, Color(0xFFEDE6B1))

        Chart(
            chart = lineChart(
                lines = listOf(
                    lineSpec(
                        lineColor = Color(0xFFEDE6B1),
                        lineThickness = 2.dp,
                        point = pointDrawer,
                        pointSize = 8.dp
                    )
                ),
                axisValuesOverrider = axisValuesOverrider
            ),
            model = model,
            bottomAxis = bottomAxis(
                valueFormatter = { value, _ ->
                    labels.getOrNull(value.toInt()) ?: ""
                },
                guideline = null
            )
        )

        Log.d("ChartDebug", "entries=${chartEntries.size}, labels=${xLabels.size}")


    }
}

@Composable
fun PeriodSelector(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit,
    weekRange: Pair<Date, Date>?,
    month: Pair<Int, Int>?,
    year: Int?
) {
    val options = listOf("Tydzień", "Miesiąc", "Rok")
    var expanded by remember { mutableStateOf(false) }
    val formatter = remember {
        SimpleDateFormat("d MMMM yyyy", Locale("pl"))
    }

    val labelText by remember(selectedPeriod, weekRange, month, year) {
        val result = when (selectedPeriod) {
            "Tydzień" -> weekRange?.let {
                val start = it.first
                val end = it.second
                val dayFormatter = SimpleDateFormat("d", Locale("pl"))
                val monthYearFormatter = SimpleDateFormat("MMMM yyyy", Locale("pl"))
                "${dayFormatter.format(start)}–${dayFormatter.format(end)} ${monthYearFormatter.format(start)}"
            } ?: ""
            "Miesiąc" -> month?.let {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.MONTH, it.first - 1)
                    set(Calendar.YEAR, it.second)
                }
                "${cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("pl"))} ${it.second}"
            } ?: ""
            "Rok" -> year?.toString() ?: ""
            else -> ""
        }
        Log.d("LabelTextDebug", "labelText recomputed: $result")
        mutableStateOf(result)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(40.dp)
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .background(FluentBackgroundDark, shape = MaterialTheme.shapes.small)
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(selectedPeriod, color = Color.White, fontSize = 14.sp)
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Rozwiń", tint = Color.White)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(120.dp)
                    .background(FluentBackgroundDark)
            ) {
                options.forEach { label ->
                    val isSelected = selectedPeriod == label
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = label,
                                color = if (isSelected) FluentBackgroundDark else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        onClick = {
                            onPeriodSelected(label)
                            expanded = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(0.dp,0.dp)
                            .background(if (isSelected) Color(0xFFB3A3A3) else Color.Transparent),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    )


                }
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .background(FluentBackgroundDark, shape = MaterialTheme.shapes.small)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(labelText, color = Color.White, fontSize = 14.sp)
        }
    }
}

fun getLastNDays(n: Int): List<String> {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return (0 until n).map { offset ->
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -offset)
        dateFormat.format(calendar.time)
    }.reversed()
}
fun getLabelsBetweenDates(start: Date, end: Date): List<String> {
    val format = SimpleDateFormat("dd.MM", Locale.getDefault())
    val labels = mutableListOf<String>()
    val calendar = Calendar.getInstance().apply { time = start }

    while (!calendar.time.after(end)) {
        labels.add(format.format(calendar.time))
        calendar.add(Calendar.DAY_OF_MONTH, 1)
    }
    return labels
}
@Composable
fun MonthPickerDialog(
    onDismiss: () -> Unit,
    currentMonth: Int,
    currentYear: Int,
    onMonthChange: (Int, Int) -> Unit
) {
    var month by remember { mutableStateOf(currentMonth) }
    var year by remember { mutableStateOf(currentYear) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onMonthChange(month + 1, year)
                onDismiss()
            }) {
                Text("Zatwierdź", color = Color(0xFFD6CFC3))
            }
        },
        title = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = {
                    if (month == 0) {
                        month = 11
                        year--
                    } else month--
                }) { Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF685044), shape = MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Text("<", color = Color(0xFF503D34))
                } }
                Text(
                    text = "${getMonthName(month)} $year",
                    color = Color(0xFFEDE6B1),
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    if (month == 11) {
                        month = 0
                        year++
                    } else month++
                }) { Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF685044), shape = MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Text(">", color = Color(0xFF503D34))
                }}
            }
        },
        text = {
            CalendarGrid(month = month, year = year, highlightWeek = null)
        },
        containerColor = Color(0xFF7D5E4C)
    )
    //onMonthChange(month + 1, year)
}

@Composable
fun WeekPickerDialog(
    onDismiss: () -> Unit,
    onWeekSelected: (start: Date, end: Date) -> Unit
) {
    val today = Calendar.getInstance()
    var month by remember { mutableStateOf(today.get(Calendar.MONTH)) }
    var year by remember { mutableStateOf(today.get(Calendar.YEAR)) }
    var selectedWeek by remember { mutableStateOf<Pair<Date, Date>?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    selectedWeek?.let { onWeekSelected(it.first, it.second) }
                    onDismiss()
                }
            ) {
                Text("Zatwierdź", color = Color(0xFFD6CFC3))
            }
        },
        title = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = {
                    if (month == 0) {
                        month = 11
                        year--
                    } else month--
                }) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF685044), shape = MaterialTheme.shapes.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("<", color = Color(0xFF503D34))
                    }}
                Text("${getMonthName(month)} $year", color = Color(0xFFEDE6B1), fontWeight = FontWeight.Bold)
                IconButton(onClick = {
                    if (month == 11) {
                        month = 0
                        year++
                    } else month++
                }) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF685044), shape = MaterialTheme.shapes.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(">", color = Color(0xFF503D34))
                    }
                }

            }
        },
        text = {
            CalendarGrid(month = month, year = year, highlightWeek = selectedWeek) { clickedDate ->
                val calendar = Calendar.getInstance().apply {
                    time = clickedDate
                }

                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val offsetToMonday = when (dayOfWeek) {
                    Calendar.MONDAY -> 0
                    Calendar.TUESDAY -> -1
                    Calendar.WEDNESDAY -> -2
                    Calendar.THURSDAY -> -3
                    Calendar.FRIDAY -> -4
                    Calendar.SATURDAY -> -5
                    Calendar.SUNDAY -> -6
                    else -> 0
                }

                calendar.add(Calendar.DAY_OF_MONTH, offsetToMonday)
                val start = calendar.time

                calendar.add(Calendar.DAY_OF_MONTH, 6)
                val end = calendar.time

                selectedWeek = start to end
            }

        },
        containerColor = Color(0xFF7D5E4C)
    )
}

@Composable
fun CalendarGrid(
    month: Int,
    year: Int,
    highlightWeek: Pair<Date, Date>? = null,
    onDayClick: ((Date) -> Unit)? = null
) {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, 1)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7


    val dayLabels = listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "N")
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            dayLabels.forEach { label ->
                Text(label, modifier = Modifier.weight(1f), color = Color(0xFFEDE6B1), textAlign = TextAlign.Center)
            }
        }
        var currentDay = 1
        for (week in 0..5) {
            Row(Modifier.fillMaxWidth()) {
                for (dow in 0..6) {
                    val dayNumber = if (week == 0 && dow < firstDayOfWeek) null else if (currentDay > daysInMonth) null else currentDay++
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .aspectRatio(1f)
                            .clickable(enabled = onDayClick != null && dayNumber != null) {
                                val date = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, dayNumber!!)
                                    setHMS0()
                                }.time
                                onDayClick?.invoke(date)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val thisDate = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, dayNumber ?: 1)
                        }.time
                        val isHighlighted = highlightWeek?.let { (start, end) ->
                            val dayCal = Calendar.getInstance().apply { time = thisDate; setHMS0() }
                            val startCal = Calendar.getInstance().apply { time = start; setHMS0() }
                            val endCal = Calendar.getInstance().apply { time = end; setHMS0() }
                            !dayCal.before(startCal) && !dayCal.after(endCal)
                        } ?: false

                        if (dayNumber != null) {
                            Text(
                                text = dayNumber.toString(),
                                color = if (isHighlighted) Color.Black else Color(0xFFEDE6B1),
                                modifier = if (isHighlighted) Modifier.background(Color(0xFFEDE6B1)).padding(4.dp) else Modifier
                            )
                        }
                    }
                }
            }
        }
    }
}
private fun Calendar.setHMS0() {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

fun getMonthName(month: Int): String =
    listOf("Styczeń", "Luty", "Marzec", "Kwiecień", "Maj", "Czerwiec", "Lipiec", "Sierpień", "Wrzesień", "Październik", "Listopad", "Grudzień")[month]

fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)
fun currentMonth(): Int = Calendar.getInstance().get(Calendar.MONTH) + 1
fun getDaysInMonth(year: Int, month: Int): List<String> {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val days = mutableListOf<String>()
    val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    for (i in 1..maxDay) {
        calendar.set(Calendar.DAY_OF_MONTH, i)
        days.add(format.format(calendar.time))
    }
    return days
}

@Composable
fun YearPickerDialog(
    initialYear: Int = currentYear(),
    onDismiss: () -> Unit,
    onYearSelected: (Int) -> Unit
) {
    var selectedYear by remember { mutableStateOf(initialYear) }
    val availableYears = listOf(2020, 2021, 2022, 2023, 2024, 2025)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onYearSelected(selectedYear)
                    onDismiss()
                }
            ) {
                Text("Zatwierdź", color = Color(0xFFD6CFC3))
            }
        },

        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Wybierz rok",
                    color = Color(0xFFEDE6B1),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Zamknij",
                        tint = FluentSecondaryDark
                    )
                }
            }
        },

        text = {
            Column {
                Divider(color = Color(0xFFEDE6B1), thickness = 1.dp)
                availableYears.forEach { year ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedYear = year }
                    ) {
                        RadioButton(
                            selected = selectedYear == year,
                            onClick = { selectedYear = year },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFFEDE6B1),
                                unselectedColor = Color(0xFFBBAFA5)
                            )
                        )
                        Text(
                            text = year.toString(),
                            color = Color(0xFFEDE6B1),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFF7D5E4C)
    )
}
