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
    
    val isHoliday = dayOfWeek == 6 || dayOfWeek == 7
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
            // نوار انتخاب روز
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

            // لیست کامل کارت درس‌ها همراه با نمایش مستقیم طرح درس
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
                            Text("برای این روز برنامه‌ای ثبت نشده است. از منوی بالا برنامه هفتگی را تنظیم کنید.", color = Color.Gray)
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
                                // هدر کارت (نام درس و زمان)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = schedule.subjectName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp,
                                        color = Color(0xFF043927)
                                    )
                                    Text(
                                        text = if (schedule.timeSlot.isNotBlank()) schedule.timeSlot else "زمان تعیین نشده",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                
                                if (schedule.className.isNotBlank()) {
                                    Text("کلاس: ${schedule.className}", fontSize = 12.sp, color = Color.DarkGray)
                                }

                                Divider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFE0E0E0))

                                // نمایش کامل محتوای طرح درس مستقیماً در همین کارت
                                if (plan != null && (plan.topic.isNotBlank() || plan.objective.isNotBlank() || plan.learningActivity.isNotBlank())) {
                                    if (plan.topic.isNotBlank()) {
                                        PlanDetailRow(title = "موضوع:", value = plan.topic)
                                    }
                                    if (plan.objective.isNotBlank()) {
                                        PlanDetailRow(title = "هدف آموزشی:", value = plan.objective)
                                    }
                                    if (plan.teachingMethod.isNotBlank()) {
                                        PlanDetailRow(title = "روش تدریس:", value = plan.teachingMethod)
                                    }
                                    if (plan.learningActivity.isNotBlank()) {
                                        PlanDetailRow(title = "فعالیت یاددهی-یادگیری:", value = plan.learningActivity)
                                    }
                                } else {
                                    Text(
                                        text = "✏️ هنوز طرح درسی ثبت نشده است. (جهت ثبت/ویرایش کلیک کنید)",
                                        fontSize = 13.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Normal,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // نام طراح
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

@Composable
fun PlanDetailRow(title: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(text = title, fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
        Text(text = value, fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Medium)
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
                title = { Text("ویرایش طرح درس: ${schedule.subjectName}") },
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
            if (!previousNote.isNullOrBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("📌 یادداشت منتقل‌شده از جلسه قبل (${schedule.subjectName}):", fontWeight = FontWeight.Bold, color = Color(0xFFF57F17), fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(previousNote, fontSize = 14.sp, color = Color(0xFF3E2723), fontWeight = FontWeight.Medium)
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
                    label = { Text("یادداشت برای جلسه بعدی (در جلسه بعد یادآوری می‌شود)") },
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

    val predefinedSubjects = listOf(
        "قرآن", "ورزش", "هنر", "تفکر و پژوهش", "کاروفناوری",
        "هدیه‌ها", "علوم", "مطالعات اجتماعی", "فارسی", "ریاضی"
    )

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
                            Text("${ShamsiCalendar.getDayNameByNumber(sch.dayOfWeek)} - ${sch.subjectName}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF043927))
                            Text("کلاس: ${if(sch.className.isBlank()) "ثبت نشده" else sch.className} | زمان: ${if(sch.timeSlot.isBlank()) "تعیین نشده" else sch.timeSlot}", fontSize = 12.sp, color = Color.Gray)
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

    if (showAddDialog) {
        var selectedSubject by remember { mutableStateOf(predefinedSubjects.first()) }
        val selectedDays = remember { mutableStateListOf<Int>() }
        var className by remember { mutableStateOf("") }
        var timeSlot by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("افزودن درس به برنامه") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("انتخاب درس:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    Column {
                        predefinedSubjects.chunked(3).forEach { rowSubjects ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                rowSubjects.forEach { sub ->
                                    FilterChip(
                                        selected = selectedSubject == sub,
                                        onClick = { selectedSubject = sub },
                                        label = { Text(sub, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = Color.LightGray)

                    Text("روزهای برگزاری در هفته:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        (1..5).forEach { day ->
                            FilterChip(
                                selected = selectedDays.contains(day),
                                onClick = {
                                    if (selectedDays.contains(day)) selectedDays.remove(day)
                                    else selectedDays.add(day)
                                },
                                label = { Text(ShamsiCalendar.getDayShortName(day), fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = className,
                        onValueChange = { className = it },
                        label = { Text("نام کلاس (اختیاری)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = timeSlot,
                        onValueChange = { timeSlot = it },
                        label = { Text("ساعت کلاس (مثلاً 08:00 - 09:30)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedDays.isNotEmpty()) {
                            selectedDays.forEach { day ->
                                dao.insertSchedule(
                                    WeeklySchedule(
                                        dayOfWeek = day,
                                        subjectName = selectedSubject,
                                        className = className,
                                        timeSlot = timeSlot
                                    )
                                )
                            }
                            schedules = dao.getAllSchedules()
                            showAddDialog = false
                        }
                    }
                ) { Text("ثبت برنامه") }
            }
        )
    }
}
