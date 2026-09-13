package com.daftarkelas.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val parentPhone: String,
    val internalId: String
)

@Entity(tableName = "sessions")
data class ClassSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateShamsi: String,
    val classType: String
)

@Entity(tableName = "session_records")
data class SessionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val studentId: Long,
    val isPresent: Boolean,
    val homeworkStatus: String,
    val smsSent: Boolean = false
)

@Entity(tableName = "settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val schoolName: String = "مدرسه من",
    val teacherName: String = "معلم گرامی",
    val className: String = "کلاس عمومی",
    val smsTemplate: String = "ولی محترم، به اطلاع می‌رساند فرزند شما [نام] امروز [تاریخ] در مدرسه [مدرسه] حضور نداشته است."
)

@Dao
interface AppDao {
    @Query("SELECT * FROM students ORDER BY fullName ASC")
    fun getAllStudents(): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student)

    @Delete
    suspend fun deleteStudent(student: Student)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ClassSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionRecords(records: List<SessionRecord>)

    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettings(): Flow<AppSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSettings(settings: AppSettings)
}

@Database(entities = [Student::class, ClassSession::class, SessionRecord::class, AppSettings::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "daftar_kelas_db").build()
                INSTANCE = instance
                instance
            }
        }
    }
}

object JalaliCalendar {
    fun getCurrentPersianDate(): String {
        val calendar = Calendar.getInstance()
        val gYear = calendar.get(Calendar.YEAR)
        val gMonth = calendar.get(Calendar.MONTH) + 1
        val gDay = calendar.get(Calendar.DAY_OF_MONTH)
        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
        var gy = gYear - 1600
        var gm = gMonth - 1
        var gd = gDay - 1
        var gDayNo = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400
        for (i in 0 until gm) gDayNo += gDaysInMonth[i + 1]
        if (gm > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) gDayNo++
        gDayNo += gd
        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053
        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461
        if (jDayNo >= 366) { jy += (jDayNo - 1) / 365; jDayNo = (jDayNo - 1) % 365 }
        var jm = 0
        var jd = 0
        for (i in 0..11) {
            if (jDayNo < jDaysInMonth[i + 1]) { jm = i + 1; jd = jDayNo + 1; break }
            jDayNo -= jDaysInMonth[i + 1]
        }
        val monthNames = arrayOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
        return "$jd ${monthNames[jm - 1]} $jy"
    }
}

data class AttendanceState(val student: Student, var isPresent: Boolean = true, var homeworkStatus: String = "COMPLETE")

class MainViewModel(application: android.app.Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).appDao()
    val students: StateFlow<List<Student>> = dao.getAllStudents().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val settings: StateFlow<AppSettings> = dao.getSettings().map { it ?: AppSettings() }.stateIn(viewModelScope, SharingStarted.Lazily, AppSettings())
    
    private val _attendanceList = MutableStateFlow<List<AttendanceState>>(emptyList())
    val attendanceList: StateFlow<List<AttendanceState>> = _attendanceList

    init {
        viewModelScope.launch {
            students.collect { list -> _attendanceList.value = list.map { AttendanceState(it) } }
        }
    }

    fun addStudent(name: String, phone: String, internalId: String) {
        viewModelScope.launch { dao.insertStudent(Student(fullName = name, parentPhone = phone, internalId = internalId)) }
    }

    fun deleteStudent(student: Student) {
        viewModelScope.launch { dao.deleteStudent(student) }
    }

    fun updatePresence(studentId: Long, isPresent: Boolean) {
        _attendanceList.value = _attendanceList.value.map { if (it.student.id == studentId) it.copy(isPresent = isPresent) else it }
    }

    fun saveSession(classType: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val dateStr = JalaliCalendar.getCurrentPersianDate()
            val sessionId = dao.insertSession(ClassSession(dateShamsi = dateStr, classType = classType))
            val records = _attendanceList.value.map {
                SessionRecord(sessionId = sessionId, studentId = it.student.id, isPresent = it.isPresent, homeworkStatus = if (classType == "VIRTUAL") it.homeworkStatus else "NONE")
            }
            dao.insertSessionRecords(records)
            onComplete()
        }
    }

    fun updateSettings(school: String, teacher: String, cls: String, sms: String) {
        viewModelScope.launch { dao.updateSettings(AppSettings(schoolName = school, teacherName = teacher, className = cls, smsTemplate = sms)) }
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF2E7D32), background = Color(0xFFF1F8E9))) {
                    AppNavigation(viewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("virtual") { VirtualClassScreen(viewModel) }
        composable("physical") { PhysicalClassScreen(viewModel, navController) }
        composable("students") { StudentManagementScreen(viewModel) }
        composable("settings") { SettingsScreen(viewModel) }
    }
}

@Composable
fun HomeScreen(navController: androidx.navigation.NavController) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("دفتر کلاس", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 24.dp))
        Text("امروز: ${JalaliCalendar.getCurrentPersianDate()}", fontSize = 16.sp, color = Color.DarkGray, modifier = Modifier.padding(bottom = 24.dp))
        
        Card(modifier = Modifier.fillMaxWidth().clickable { navController.navigate("virtual") }.padding(bottom = 12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("💻 کلاس مجازی", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("ثبت حضور و غیاب و تکالیف", fontSize = 12.sp, color = Color.Gray)
            }
        }
        
        Card(modifier = Modifier.fillMaxWidth().clickable { navController.navigate("physical") }.padding(bottom = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🏫 کلاس حضوری", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("ثبت سریع حضور و ارسال پیامک غیبت", fontSize = 12.sp, color = Color.Gray)
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { navController.navigate("students") }, modifier = Modifier.weight(1f).padding(end = 4.dp)) { Text("دانش‌آموزان") }
            Button(onClick = { navController.navigate("settings") }, modifier = Modifier.weight(1f).padding(start = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("تنظیمات") }
        }
    }
}

@Composable
fun VirtualClassScreen(viewModel: MainViewModel) {
    val attendanceList by viewModel.attendanceList.collectAsState()
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(attendanceList) { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(item.student.fullName, fontWeight = FontWeight.Bold)
                        Row {
                            FilterChip(selected = item.isPresent, onClick = { viewModel.updatePresence(item.student.id, true) }, label = { Text("🟢 حاضر") })
                            Spacer(modifier = Modifier.width(8.dp))
                            FilterChip(selected = !item.isPresent, onClick = { viewModel.updatePresence(item.student.id, false) }, label = { Text("🔴 غایب") })
                        }
                    }
                }
            }
        }
        Button(onClick = { viewModel.saveSession("VIRTUAL") { Toast.makeText(context, "ثبت شد", Toast.LENGTH_SHORT).show() } }, modifier = Modifier.fillMaxWidth()) {
            Text("ثبت جلسه")
        }
    }
}

@Composable
fun PhysicalClassScreen(viewModel: MainViewModel, navController: androidx.navigation.NavController) {
    val attendanceList by viewModel.attendanceList.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    var showSmsDialog by remember { mutableStateOf(false) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) showSmsDialog = true else Toast.makeText(context, "مجوز پیامک داده نشد", Toast.LENGTH_SHORT).show()
    }

    if (showSmsDialog) {
        val absents = attendanceList.filter { !it.isPresent }
        AlertDialog(
            onDismissRequest = { showSmsDialog = false },
            title = { Text("تأیید ارسال پیامک") },
            text = { Text("${absents.size} دانش‌آموز غایب هستند. پیامک ارسال شود؟") },
            confirmButton = {
                TextButton(onClick = {
                    showSmsDialog = false
                    viewModel.saveSession("PHYSICAL") {
                        val date = JalaliCalendar.getCurrentPersianDate()
                        absents.forEach { item ->
                            if (item.student.parentPhone.isNotBlank()) {
                                val msg = settings.smsTemplate.replace("[نام]", item.student.fullName).replace("[تاریخ]", date).replace("[مدرسه]", settings.schoolName)
                                try {
                                    val smsManager = SmsManager.getDefault()
                                    smsManager.sendTextMessage(item.student.parentPhone, null, msg, null, null)
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                        }
                        Toast.makeText(context, "ثبت و ارسال شد", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                }) { Text("ارسال") }
            },
            dismissButton = { TextButton(onClick = { showSmsDialog = false }) { Text("لغو") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(attendanceList) { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.student.fullName)
                        Row {
                            Button(onClick = { viewModel.updatePresence(item.student.id, true) }, colors = ButtonDefaults.buttonColors(containerColor = if (item.isPresent) Color(0xFF4CAF50) else Color.LightGray)) { Text("حاضر") }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(onClick = { viewModel.updatePresence(item.student.id, false) }, colors = ButtonDefaults.buttonColors(containerColor = if (!item.isPresent) Color(0xFFE53935) else Color.LightGray)) { Text("غایب") }
                        }
                    }
                }
            }
        }
        Button(onClick = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) showSmsDialog = true
            else smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }, modifier = Modifier.fillMaxWidth()) { Text("ثبت نهایی و ارسال پیامک") }
    }
}

@Composable
fun StudentManagementScreen(viewModel: MainViewModel) {
    val students by viewModel.students.collectAsState()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var idNum by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام و نام خانوادگی") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("شماره ولی") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = idNum, onValueChange = { idNum = it }, label = { Text("شناسه داخلی") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { if (name.isNotBlank()) { viewModel.addStudent(name, phone, idNum); name = ""; phone = ""; idNum = "" } }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Text("افزودن") }

        LazyColumn {
            items(students) { student ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(student.fullName)
                        IconButton(onClick = { viewModel.deleteStudent(student) }) { Text("❌") }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsState()
    var school by remember { mutableStateOf(settings.schoolName) }
    var teacher by remember { mutableStateOf(settings.teacherName) }
    var cls by remember { mutableStateOf(settings.className) }
    var sms by remember { mutableStateOf(settings.smsTemplate) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(value = school, onValueChange = { school = it }, label = { Text("نام مدرسه") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = teacher, onValueChange = { teacher = it }, label = { Text("نام معلم") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = cls, onValueChange = { cls = it }, label = { Text("نام کلاس") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = sms, onValueChange = { sms = it }, label = { Text("قالب پیامک") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        Button(onClick = { viewModel.updateSettings(school, teacher, cls, sms); Toast.makeText(context, "ذخیره شد", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("ذخیره") }
    }
}
