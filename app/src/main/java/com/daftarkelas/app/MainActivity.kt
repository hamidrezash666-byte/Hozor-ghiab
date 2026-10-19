package com.daftarkelas.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(
                    colorScheme = lightColorScheme(
                        primary = Color(0xFF043927),
                        secondary = Color(0xFF2E7D32),
                        surface = Color(0xFFF5F5F5)
                    )
                ) {
                    MainAppNav()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppNav() {
    var currentScreen by remember { mutableStateOf("CALENDAR") }
    var selectedSchedule by remember { mutableStateOf<WeeklySchedule?>(null) }
    var selectedDate by remember { mutableStateOf(ShamsiCalendar.getCurrentShamsiDate()) }

    BackHandler(enabled = currentScreen != "CALENDAR") {
        when (currentScreen) {
            "LESSON_EDIT", "SCHEDULE_SETUP" -> currentScreen = "CALENDAR"
        }
    }

    when (currentScreen) {
        "CALENDAR" -> CalendarScreen(
            currentDate = selectedDate,
            onDateChange = { selectedDate = it },
            onSelectLesson = { schedule ->
                selectedSchedule = schedule
                currentScreen = "LESSON_EDIT"
            },
            onOpenScheduleSetup = { currentScreen = "SCHEDULE_SETUP" }
        )
        "LESSON_EDIT" -> selectedSchedule?.let { schedule ->
            LessonEditScreen(
                schedule = schedule,
                dateStr = selectedDate,
                onBack = { currentScreen = "CALENDAR" }
            )
        }
        "SCHEDULE_SETUP" -> ScheduleSetupScreen(
            onBack = { currentScreen = "CALENDAR" }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    currentDate: String,
    onDateChange: (String) -> Unit,
    onSelectLesson: (WeeklySchedule) -> Unit,
    onOpenScheduleSetup: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).lessonPlanDao() }

    val dayOfWeek = remember(currentDate) { ShamsiCalendar.getDayOfWeekNumber(currentDate) }
    val dayName = remember(currentDate) { ShamsiCalendar.getDayOfWeekName(currentDate) }
    val daySchedules = remember(dayOfWeek) { dao.getSchedulesForDay(dayOfWeek) }
    
    // تشخیص روزهای تعطیل (پنج‌شنبه = 6 و جمعه = 7)
    val isHoliday = dayOfWeek == 6 || dayOfWeek == 7

    // مدیریت منوی سه‌نقطه بالا
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تقویم طرح درس", fontWeight = FontWeight.Bold) },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Text("⋮", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF043927))
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("تنظیم برنامه هفتگی") },
                                onClick = {
                                    menuExpanded = false
                                    onOpenScheduleSetup()
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // نوار تقویم روزانه با قابلیت ورق زدن
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isHoliday) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onDateChange(ShamsiCalendar.getPreviousDate(currentDate)) }) {
                        Text("▶", fontSize = 18.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(dayName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isHoliday) Color.Red else Color(0xFF043927))
                            if (isHoliday) {
                                Surface(
                                    color = Color.Red.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("تعطیل", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(currentDate, fontSize = 14.sp, color = Color.Gray)
                    }
                    IconButton(onClick = { onDateChange(ShamsiCalendar.getNextDate(currentDate)) }) {
                        Text("◀", fontSize = 18.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // لیست کلاس‌های روز انتخاب‌شده
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isHoliday) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("امروز روز تعطیل است و کلاسی برنامه‌ریزی نشده.", color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                    }
                } else if (daySchedules.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("برای این روز برنامه‌ای ثبت نشده است.", color = Color.Gray)
                        }
                    }
                } else {
                    items(daySchedules) { schedule ->
                        val plan = dao.getPlanForDate(schedule.id, currentDate)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectLesson(schedule) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(schedule.subjectName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF043927))
                                    Text(schedule.timeSlot, fontSize = 13.sp, color = Color.Gray)
                                }
                                Text("کلاس: ${schedule.className}", fontSize = 13.sp, color = Color.DarkGray)

                                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray)

                                // نمایش فعالیت یاددهی-یادگیری مستقیماً از بیرون روی کارت
                                Text("فعالیت یاددهی-یادگیری:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (!plan?.learningActivity.isNull_or_blank()) plan!!.learningActivity else "هنوز فعالیتی ثبت نشده است (جهت ورود بزنید)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (!plan?.learningActivity.isNull_or_blank()) Color.Black else Color.LightGray
                                )
                            }
                        }
                    }
                }
            }

            // نام طراح پایین صفحه سمت چپ
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 4.dp)
                ) {
                    Text(
                        text = "Designed by Hamidreza Shariati",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonEditScreen(
    schedule: WeeklySchedule,
    dateStr: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).lessonPlanDao() }

    val existingPlan = remember { dao.getPlanForDate(schedule.id, dateStr) }
    val previousNote = remember { dao.getPreviousNote(schedule.id, dateStr) }

    var topic by remember { mutableStateOf(existingPlan?.topic ?: "") }
    var objective by remember { mutableStateOf(existingPlan?.objective ?: "") }
    var teachingMethod by remember { mutableStateOf(existingPlan?.teachingMethod ?: "") }
    var learningActivity by remember { mutableStateOf(existingPlan?.learningActivity ?: "") }
    var noteForNextSession by remember { mutableStateOf(existingPlan?.noteForNextSession ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("طرح درس: ${schedule.subjectName}") },
                navigationIcon = { TextButton(onClick = onBack) { Text("بازگشت") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // نمایش یادداشت جلسه قبلی در بالاترین بخش
            if (!previousNote.isNullOrBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("📌 یادداشت منتقل‌شده از جلسه قبل:", fontWeight = FontWeight.Bold, color = Color(0xFFF57F17), fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(previousNote, fontSize = 14.sp, color = Color(0xFF3E2723))
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("موضوع درس") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = objective,
                    onValueChange = { objective = it },
                    label = { Text("هدف آموزشی") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            item {
                OutlinedTextField(
                    value = teachingMethod,
                    onValueChange = { teachingMethod = it },
                    label = { Text("روش تدریس") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = learningActivity,
                    onValueChange = { learningActivity = it },
                    label = { Text("فعالیت یاددهی-یادگیری") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }

            item {
                OutlinedTextField(
                    value = noteForNextSession,
                    onValueChange = { noteForNextSession = it },
                    label = { Text("یادداشت برای جلسه بعدی") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            item {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF043927)),
                    onClick = {
                        val planToSave = LessonPlan(
                            id = existingPlan?.id ?: 0,
                            scheduleId = schedule.id,
                            dateStr = dateStr,
                            topic = topic,
                            objective = objective,
                            teachingMethod = teachingMethod,
                            learningActivity = learningActivity,
                            noteForNextSession = noteForNextSession
                        )
                        dao.insertOrUpdatePlan(planToSave)
                        onBack()
                    }
                ) {
                    Text("ذخیره طرح درس", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleSetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).lessonPlanDao() }
    var schedules by remember { mutableStateOf(dao.getAllSchedules()) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تنظیم برنامه هفتگی") },
                navigationIcon = { TextButton(onClick = onBack) { Text("بازگشت") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF043927),
                contentColor = Color.White
            ) { Text("+", fontSize = 24.sp) }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(schedules) { sch ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${ShamsiCalendar.getDayNameByNumber(sch.dayOfWeek)} - ${sch.subjectName}", fontWeight = FontWeight.Bold)
                            Text("کلاس: ${sch.className} | زمان: ${sch.timeSlot}", fontSize = 12.sp, color = Color.Gray)
                        }
                        IconButton(onClick = {
                            dao.deleteSchedule(sch)
                            schedules = dao.getAllSchedules()
                        }) {
                            Text("🗑️")
                        }
                    }
                }
            }
        }
    }

    (showAddDialog).let { if (it) {
        var selectedDay by remember { mutableStateOf(1) }
        var subjectName by remember { mutableStateOf("") }
        var className by remember { mutableStateOf("") }
        var timeSlot by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("افزودن درس به برنامه") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("روز هفته:")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        (1..7).forEach { day ->
                            FilterChip(
                                selected = selectedDay == day,
                                onClick = { selectedDay = day },
                                label = { Text(ShamsiCalendar.getDayShortName(day), fontSize = 11.sp) }
                            )
                        }
                    }
                    OutlinedTextField(value = subjectName, onValueChange = { subjectName = it }, label = { Text("نام درس / موضوع") })
                    OutlinedTextField(value = className, onValueChange = { className = it }, label = { Text("نام کلاس") })
                    OutlinedTextField(value = timeSlot, onValueChange = { timeSlot = it }, label = { Text("ساعت کلاس (مثلاً 08:00 - 09:30)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (subjectName.isNotBlank()) {
                        dao.insertSchedule(
                            WeeklySchedule(
                                dayOfWeek = selectedDay,
                                subjectName = subjectName,
                                className = className,
                                timeSlot = timeSlot
                            )
                        )
                        schedules = dao.getAllSchedules()
                        showAddDialog = false
                    }
                }) { Text("ثبت") }
            }
        )
    }}
}
