package com.daftarkelas.app

import android.os.Bundle
import androidx.activity.ComponentActivity
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

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(
                    colorScheme = lightColorScheme(
                        primary = Color(0xFF1E88E5),
                        secondary = Color(0xFF26A69A),
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
    var currentScreen by remember { mutableStateOf("HOME") }
    var selectedStudentId by remember { mutableStateOf<Long?>(null) }
    var selectedSessionId by remember { mutableStateOf<Long?>(null) }
    var editSessionId by remember { mutableStateOf<Long?>(null) }

    when (currentScreen) {
        "HOME" -> HomeScreen(
            onNavigateToAttendance = { editSessionId = null; currentScreen = "ATTENDANCE" },
            onNavigateToVirtual = { editSessionId = null; currentScreen = "VIRTUAL" },
            onNavigateToStudents = { currentScreen = "STUDENTS" },
            onNavigateToHistory = { currentScreen = "HISTORY" }
        )
        "STUDENTS" -> StudentsScreen(
            onSelectStudent = { id -> selectedStudentId = id; currentScreen = "STUDENT_PROFILE" },
            onBack = { currentScreen = "HOME" }
        )
        "STUDENT_PROFILE" -> selectedStudentId?.let { id ->
            StudentProfileScreen(studentId = id, onBack = { currentScreen = "STUDENTS" })
        }
        "ATTENDANCE" -> AttendanceSessionScreen(
            isVirtual = false,
            existingSessionId = editSessionId,
            onFinish = { id -> selectedSessionId = id; currentScreen = "REPORT" },
            onBack = { currentScreen = "HOME" }
        )
        "VIRTUAL" -> AttendanceSessionScreen(
            isVirtual = true,
            existingSessionId = editSessionId,
            onFinish = { id -> selectedSessionId = id; currentScreen = "REPORT" },
            onBack = { currentScreen = "HOME" }
        )
        "REPORT" -> selectedSessionId?.let { id ->
            ReportScreen(sessionId = id, onBack = { currentScreen = "HOME" })
        }
        "HISTORY" -> HistoryScreen(
            onEditSession = { session ->
                editSessionId = session.id
                currentScreen = if (session.isVirtual) "VIRTUAL" else "ATTENDANCE"
            },
            onBack = { currentScreen = "HOME" }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAttendance: () -> Unit,
    onNavigateToVirtual: () -> Unit,
    onNavigateToStudents: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("دفتر کلاس", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MenuCard("🏫 کلاس حضوری", "ثبت یا ویرایش حضور و غیاب امروز", Color(0xFF1E88E5), onNavigateToAttendance)
                MenuCard("💻 کلاس مجازی", "ثبت یا ویرایش وضعیت تکالیف امروز", Color(0xFF8E24AA), onNavigateToVirtual)
                MenuCard("👨‍🎓 دانش‌آموزان", "مدیریت پرونده و سوابق", Color(0xFF43A047), onNavigateToStudents)
                MenuCard("📜 سوابق جلسات", "مشاهده و ویرایش جلسات قبلی", Color(0xFFFB8C00), onNavigateToHistory)
            }

            // نام طراح پایین صفحه سمت چپ
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp, start = 4.dp)
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
fun MenuCard(title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, fontSize = 13.sp, color = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen(onSelectStudent: (Long) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).classDao() }
    var students by remember { mutableStateOf(dao.getAllStudents()) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("لیست دانش‌آموزان") },
                navigationIcon = { TextButton(onClick = onBack) { Text("بازگشت") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) { Text("+") }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            items(students) { student ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onSelectStudent(student.id) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(student.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("کلاس: ${student.className} | شماره ولی: ${student.parentPhone}", color = Color.Gray)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var className by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("افزودن دانش‌آموز") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام و نام خانوادگی") })
                    OutlinedTextField(value = className, onValueChange = { className = it }, label = { Text("نام کلاس") })
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("شماره ولی") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        dao.insertStudent(Student(name = name, className = className, parentPhone = phone))
                        students = dao.getAllStudents()
                        showAddDialog = false
                    }
                }) { Text("ثبت") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProfileScreen(studentId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).classDao() }
    val student = dao.getAllStudents().find { it.id == studentId } ?: return
    val records = dao.getRecordsForStudent(studentId)
    val sessions = dao.getAllSessions().associateBy { it.id }

    val physicalRecords = records.filter { sessions[it.sessionId]?.isVirtual == false }
    val virtualRecords = records.filter { sessions[it.sessionId]?.isVirtual == true }

    Scaffold(
        topBar = { TopAppBar(title = { Text("پرونده دانش‌آموز") }, navigationIcon = { TextButton(onClick = onBack) { Text("بازگشت") } }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(student.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("کلاس: ${student.className}", color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))

            Text("آمار کلاس حضوری", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBadge("غیبت: ${physicalRecords.count { it.status == "غایب" }}", Color.Red)
                StatBadge("حضور: ${physicalRecords.count { it.status == "حاضر" }}", Color(0xFF43A047))
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("آمار کلاس مجازی", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBadge("غیبت: ${virtualRecords.count { it.status == "غایب" }}", Color.Red)
                StatBadge("تکلیف ناقص: ${virtualRecords.count { it.homeworkStatus == "ناقص" }}", Color(0xFFFBC02D))
                StatBadge("تکلیف کامل: ${virtualRecords.count { it.homeworkStatus == "کامل" }}", Color(0xFF43A047))
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("سوابق جلسات", fontWeight = FontWeight.Bold)
            LazyColumn {
                items(records.reversed()) { rec ->
                    val sess = sessions[rec.sessionId]
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${sess?.shamsiDate ?: ""} - ${if (sess?.isVirtual == true) "مجازی" else "حضوری"}")
                            Text("وضعیت: ${rec.status} ${if (rec.homeworkStatus != "-") "| تکلیف: ${rec.homeworkStatus}" else ""}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBadge(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
        Text(text, color = color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceSessionScreen(
    isVirtual: Boolean,
    existingSessionId: Long? = null,
    onFinish: (Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).classDao() }
    val repo = remember { ClassRepository(context) }
    val students = remember { dao.getAllStudents() }

    val todayDate = remember { ShamsiCalendar.getCurrentShamsiDate() }
    val activeSession = remember {
        if (existingSessionId != null) {
            dao.getAllSessions().find { it.id == existingSessionId }
        } else {
            dao.getTodaySession(todayDate, isVirtual)
        }
    }

    val statusMap = remember { mutableStateMapOf<Long, String>() }
    val hwMap = remember { mutableStateMapOf<Long, String>() }

    LaunchedEffect(activeSession) {
        val existingRecords = activeSession?.let { dao.getRecordsForSession(it.id) }?.associateBy { it.studentId }
        students.forEach { st ->
            val rec = existingRecords?.get(st.id)
            statusMap[st.id] = rec?.status ?: "حاضر"
            hwMap[st.id] = rec?.homeworkStatus ?: "کامل"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (activeSession != null) "ویرایش جلسه (${activeSession.shamsiDate})" else if (isVirtual) "ثبت کلاس مجازی" else "ثبت کلاس حضوری") },
                navigationIcon = { TextButton(onClick = onBack) { Text("انصراف") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(students) { st ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(st.name, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    colors = ButtonDefaults.buttonColors(containerColor = if (statusMap[st.id] == "حاضر") Color(0xFF43A047) else Color.Gray),
                                    onClick = { statusMap[st.id] = "حاضر" }
                                ) { Text("حاضر") }
                                Button(
                                    colors = ButtonDefaults.buttonColors(containerColor = if (statusMap[st.id] == "غایب") Color.Red else Color.Gray),
                                    onClick = { statusMap[st.id] = "غایب" }
                                ) { Text("غایب") }
                            }
                            if (isVirtual && statusMap[st.id] == "حاضر") {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        colors = ButtonDefaults.buttonColors(containerColor = if (hwMap[st.id] == "کامل") Color(0xFF43A047) else Color.Gray),
                                        onClick = { hwMap[st.id] = "کامل" }
                                    ) { Text("تکلیف کامل") }
                                    Button(
                                        colors = ButtonDefaults.buttonColors(containerColor = if (hwMap[st.id] == "ناقص") Color(0xFFFBC02D) else Color.Gray),
                                        onClick = { hwMap[st.id] = "ناقص" }
                                    ) { Text("تکلیف ناقص") }
                                }
                            }
                        }
                    }
                }
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val sessionId = activeSession?.id ?: dao.insertSession(
                        ClassSession(className = "عمومی", isVirtual = isVirtual, shamsiDate = todayDate, time = ShamsiCalendar.getCurrentTime())
                    )

                    dao.deleteRecordsForSession(sessionId)

                    val records = students.map {
                        AttendanceRecord(
                            sessionId = sessionId,
                            studentId = it.id,
                            status = statusMap[it.id] ?: "حاضر",
                            homeworkStatus = if (isVirtual && statusMap[it.id] == "حاضر") hwMap[it.id] ?: "کامل" else "-"
                        )
                    }
                    dao.insertAttendanceRecords(records)

                    val alerts = repo.checkAndGetSmsAlerts(sessionId, isVirtual, todayDate)
                    alerts.forEach { (phone, msg) -> repo.sendSmsIntent(context, phone, msg) }

                    onFinish(sessionId)
                }
            ) { Text("ذخیره و مشاهده گزارش") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(sessionId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).classDao() }
    val session = dao.getAllSessions().find { it.id == sessionId } ?: return
    val records = dao.getRecordsForSession(sessionId)
    val students = dao.getAllStudents().associateBy { it.id }

    Scaffold(
        topBar = { TopAppBar(title = { Text("گزارش تصویری جلسه") }, navigationIcon = { TextButton(onClick = onBack) { Text("پایان") } }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("گزارش کلاس ${if (session.isVirtual) "مجازی" else "حضوری"}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("تاریخ: ${session.shamsiDate} | ساعت: ${session.time}")
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    records.forEach { rec ->
                        val st = students[rec.studentId]
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(st?.name ?: "")
                            Row {
                                if (rec.status == "غایب") StatBadge("🔴 غایب", Color.Red)
                                else StatBadge("🟢 حاضر", Color(0xFF43A047))

                                if (session.isVirtual && rec.status == "حاضر") {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    if (rec.homeworkStatus == "ناقص") StatBadge("🟡 تکلیف ناقص", Color(0xFFFBC02D))
                                    else StatBadge("🟢 تکلیف کامل", Color(0xFF43A047))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onEditSession: (ClassSession) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).classDao() }
    val sessions = dao.getAllSessions()

    Scaffold(
        topBar = { TopAppBar(title = { Text("سوابق جلسات") }, navigationIcon = { TextButton(onClick = onBack) { Text("بازگشت") } }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            items(sessions) { sess ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${sess.shamsiDate} - ساعت ${sess.time}", fontWeight = FontWeight.Bold)
                            Text("نوع: ${if (sess.isVirtual) "کلاس مجازی" else "کلاس حضوری"}", color = Color.Gray)
                        }
                        OutlinedButton(onClick = { onEditSession(sess) }) {
                            Text("ویرایش")
                        }
                    }
                }
            }
        }
    }
}
