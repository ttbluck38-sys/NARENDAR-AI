package com.narendar.hub

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NarendarHubApp(applicationContext) }
    }
}

data class QuizQ(val subject: String, val question: String, val options: List<String>, val answer: Int, val explanation: String)
data class Note(val id: Long, val title: String, val body: String)
data class Task(val id: Long, val text: String, val subject: String, val done: Boolean)
data class FlashCard(val front: String, val back: String)

private val questions = listOf(
    QuizQ("Math", "What is 15% of 200?", listOf("15", "20", "30", "40"), 2, "15/100 × 200 = 30."),
    QuizQ("Math", "If x + 7 = 19, what is x?", listOf("10", "11", "12", "13"), 2, "Subtract 7 from both sides: x = 12."),
    QuizQ("Math", "What is the square of 12?", listOf("124", "144", "154", "164"), 1, "12 × 12 = 144."),
    QuizQ("Math", "What is 3/4 as a percentage?", listOf("25%", "50%", "75%", "80%"), 2, "3 ÷ 4 = 0.75, which is 75%."),
    QuizQ("Science", "Which gas do plants mainly use for photosynthesis?", listOf("Oxygen", "Nitrogen", "Carbon dioxide", "Hydrogen"), 2, "Plants use carbon dioxide with water and light to make food."),
    QuizQ("Science", "What is the SI unit of force?", listOf("Joule", "Watt", "Newton", "Pascal"), 2, "Force is measured in newtons (N)."),
    QuizQ("Science", "Water boils at what temperature at standard pressure?", listOf("50°C", "75°C", "100°C", "120°C"), 2, "At standard pressure, water boils at 100°C."),
    QuizQ("Computer", "What does CPU stand for?", listOf("Central Processing Unit", "Computer Power Unit", "Central Program Utility", "Core Processing User"), 0, "CPU means Central Processing Unit."),
    QuizQ("Computer", "Which is used to style HTML pages?", listOf("CSS", "SQL", "FTP", "BIOS"), 0, "CSS controls the presentation and layout of web pages."),
    QuizQ("Computer", "Which number system uses only 0 and 1?", listOf("Decimal", "Binary", "Octal", "Hexadecimal"), 1, "Binary uses two digits: 0 and 1."),
    QuizQ("Computer", "What does HTML stand for?", listOf("HyperText Markup Language", "High Transfer Machine Language", "Home Tool Markup Link", "Hyperlink Text Management Language"), 0, "HTML means HyperText Markup Language."),
    QuizQ("English", "Choose the correct sentence.", listOf("He go to school.", "He goes to school.", "He going school.", "He gone school."), 1, "With singular he, the present-tense verb is goes."),
    QuizQ("English", "What is the opposite of 'ancient'?", listOf("Old", "Modern", "Historic", "Past"), 1, "Modern means belonging to the present or recent times."),
    QuizQ("GK", "What is the largest planet in our solar system?", listOf("Earth", "Mars", "Jupiter", "Venus"), 2, "Jupiter is the largest planet in the Solar System."),
    QuizQ("GK", "How many continents are commonly recognized?", listOf("5", "6", "7", "8"), 2, "The commonly taught model recognizes seven continents.")
)

private val flashCards = listOf(
    FlashCard("CPU", "Central Processing Unit — the main processor of a computer."),
    FlashCard("Photosynthesis", "The process plants use to make food using light, water and carbon dioxide."),
    FlashCard("HTML", "HyperText Markup Language — the structure of a web page."),
    FlashCard("Force", "A push or pull; SI unit is the newton (N)."),
    FlashCard("Binary", "A base-2 number system using only 0 and 1."),
    FlashCard("CSS", "Cascading Style Sheets — controls the visual style of web pages.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NarendarHubApp(context: Context) {
    val prefs = remember { context.getSharedPreferences("narendar_hub", Context.MODE_PRIVATE) }
    var page by remember { mutableStateOf("Home") }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark", false)) }
    var score by remember { mutableIntStateOf(prefs.getInt("score", 0)) }
    var streak by remember { mutableIntStateOf(prefs.getInt("streak", 0)) }
    var completedQuizzes by remember { mutableIntStateOf(prefs.getInt("quizzes", 0)) }
    var notes by remember { mutableStateOf(loadNotes(prefs)) }
    var tasks by remember { mutableStateOf(loadTasks(prefs)) }
    var snackbar by remember { mutableStateOf("") }

    fun saveNotes(value: List<Note>) { notes = value; prefs.edit().putString("notes", notesToJson(value)).apply() }
    fun saveTasks(value: List<Task>) { tasks = value; prefs.edit().putString("tasks", tasksToJson(value)).apply() }

    LaunchedEffect(snackbar) { if (snackbar.isNotBlank()) { delay(1800); snackbar = "" } }

    val colors = if (dark) darkColorScheme(primary = Color(0xFF8AB4FF), secondary = Color(0xFF7FE3C4)) else lightColorScheme(primary = Color(0xFF315EFB), secondary = Color(0xFF00A884), tertiary = Color(0xFF7C4DFF))
    MaterialTheme(colorScheme = colors) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Narendar Hub", fontWeight = FontWeight.ExtraBold) },
                    actions = { Text(if (dark) "☾" else "☀", fontSize = 22.sp, modifier = Modifier.padding(end = 16.dp)) }
                )
            },
            bottomBar = {
                NavigationBar {
                    listOf("Home" to "⌂", "Quiz" to "✓", "Tools" to "✦", "Notes" to "✎", "More" to "☰").forEach { (name, icon) ->
                        NavigationBarItem(selected = page == name, onClick = { page = name }, icon = { Text(icon, fontSize = 20.sp) }, label = { Text(name) })
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                AnimatedContent(targetState = page, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "page") { target ->
                    when (target) {
                        "Home" -> HomeScreen(score, streak, completedQuizzes, tasks, notes.size) { page = it }
                        "Quiz" -> QuizScreen { gained ->
                            score += gained
                            completedQuizzes++
                            streak = (streak + 1).coerceAtMost(999)
                            prefs.edit().putInt("score", score).putInt("streak", streak).putInt("quizzes", completedQuizzes).apply()
                            snackbar = "Quiz saved • +$gained points"
                        }
                        "Tools" -> ToolsScreen()
                        "Notes" -> NotesScreen(notes, ::saveNotes)
                        else -> MoreScreen(dark, { value -> dark = value; prefs.edit().putBoolean("dark", value).apply() }, score, streak, completedQuizzes, tasks, ::saveTasks) { page = it }
                    }
                }
                AnimatedVisibility(visible = snackbar.isNotBlank(), enter = slideInVertically { it } + fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)) {
                    Surface(shape = RoundedCornerShape(18.dp), tonalElevation = 8.dp) { Text(snackbar, Modifier.padding(horizontal = 18.dp, vertical = 11.dp), fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(score: Int, streak: Int, quizzes: Int, tasks: List<Task>, noteCount: Int, onNavigate: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            AnimatedVisibility(true, enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 3 }) {
                Column {
                    Text("Welcome back, learner 👋", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text("Learn smarter. Stay consistent. Build your skills.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { HeroCard(score, streak) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Quizzes", quizzes.toString(), "✓", Modifier.weight(1f))
                StatCard("Notes", noteCount.toString(), "✎", Modifier.weight(1f))
                StatCard("Pending", tasks.count { !it.done }.toString(), "◷", Modifier.weight(1f))
            }
        }
        item { SectionTitle("Quick actions", "Everything important is one tap away") }
        item { ActionCard("🧠", "Quiz Arena", "15 offline MCQs + explanations", { onNavigate("Quiz") }) }
        item { ActionCard("🧮", "Study Tools", "Calculator, formulas, converter & tables", { onNavigate("Tools") }) }
        item { ActionCard("📝", "My Notes", "Create, save and manage notes", { onNavigate("Notes") }) }
        item { ActionCard("📅", "Planner & Focus", "Tasks, flashcards, timer and stats", { onNavigate("More") }) }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("100% Offline", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(5.dp))
                    Text("No login • no API key • no server • your notes and tasks stay on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun HeroCard(score: Int, streak: Int) {
    Card(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)))) {
            Column(Modifier.padding(22.dp)) {
                Text("YOUR LEARNING SCORE", color = Color.White.copy(alpha = .8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(score.toString(), color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔥 $streak day streak", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("Keep going!", color = Color.White.copy(alpha = .9f))
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, sub: String) { Column { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold); Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) } }

@Composable
fun StatCard(label: String, value: String, icon: String, modifier: Modifier) { Card(modifier, shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(13.dp)) { Text(icon, fontSize = 18.sp); Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold); Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }

@Composable
fun ActionCard(icon: String, title: String, sub: String, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) .975f else 1f, tween(100, easing = FastOutSlowInEasing), label = "cardScale")
    val elevation by animateDpAsState(if (pressed) 2.dp else 7.dp, tween(120), label = "cardElevation")
    Card(shape = RoundedCornerShape(22.dp), elevation = CardDefaults.cardElevation(elevation), modifier = Modifier.fillMaxWidth().scale(scale).clickable { pressed = true; onClick(); pressed = false }) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) { Text(icon, Modifier.padding(12.dp), fontSize = 25.sp) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
            Text("›", fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun QuizScreen(onComplete: (Int) -> Unit) {
    var selectedSubject by remember { mutableStateOf("All") }
    var current by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var picked by remember { mutableIntStateOf(-1) }
    var finished by remember { mutableStateOf(false) }
    val filtered = if (selectedSubject == "All") questions else questions.filter { it.subject == selectedSubject }
    val progress = if (filtered.isEmpty()) 0f else (current + if (picked != -1) 1 else 0).toFloat() / filtered.size
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Quiz Arena", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text("Practice • learn • improve", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator({ progress.coerceIn(0f, 1f) }, Modifier.fillMaxWidth().height(7.dp), strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) { listOf("All", "Math", "Science", "Computer", "English", "GK").forEach { subject -> FilterChip(selected = selectedSubject == subject, onClick = { selectedSubject = subject; current = 0; correct = 0; picked = -1; finished = false }, label = { Text(subject) }) } }
        Spacer(Modifier.height(14.dp))
        if (finished) {
            AnimatedVisibility(true, enter = scaleIn(tween(300)) + fadeIn()) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) { Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("🎉", fontSize = 48.sp); Text("Quiz complete!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold); Text("$correct / ${filtered.size} correct", fontSize = 24.sp); Text("Great work — your score has been saved.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center); Spacer(Modifier.height(14.dp)); Button(onClick = { current = 0; correct = 0; picked = -1; finished = false }, shape = RoundedCornerShape(15.dp)) { Text("Try Again") } } }
            }
        } else if (filtered.isNotEmpty()) {
            val q = filtered[current.coerceAtMost(filtered.lastIndex)]
            Text("Question ${current + 1} of ${filtered.size} • ${q.subject}", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(9.dp))
            AnimatedContent(q.question, transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(100)) }, label = "question") { text -> Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(12.dp))
            q.options.forEachIndexed { index, option ->
                val chosen = picked == index
                OutlinedButton(onClick = { if (picked == -1) picked = index }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = if (chosen) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)) { Text(option, Modifier.fillMaxWidth(), fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal) }
            }
            AnimatedVisibility(picked != -1, enter = fadeIn() + slideInVertically { it / 4 }) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text(if (picked == q.answer) "✓ Correct!" else "✗ Not quite. Correct: ${q.options[q.answer]}", fontWeight = FontWeight.Bold, color = if (picked == q.answer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    Text(q.explanation, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { if (picked == q.answer) correct++; if (current == filtered.lastIndex) { finished = true; onComplete(correct) } else { current++; picked = -1 } }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) { Text(if (current == filtered.lastIndex) "Finish Quiz" else "Next Question") }
                }
            }
        }
    }
}

@Composable
fun ToolsScreen() {
    var a by remember { mutableStateOf("") }; var b by remember { mutableStateOf("") }; var op by remember { mutableStateOf("+") }
    val x = a.toDoubleOrNull(); val y = b.toDoubleOrNull()
    val result = if (x != null && y != null) when (op) { "+" -> x + y; "−" -> x - y; "×" -> x * y; "÷" -> if (y == 0.0) null else x / y; else -> null } else null
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Study Tools", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text("Useful tools without internet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(15.dp))
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("🧮 Calculator", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) { OutlinedTextField(a, { a = it }, Modifier.weight(1f), label = { Text("Number") }, singleLine = true); OutlinedTextField(b, { b = it }, Modifier.weight(1f), label = { Text("Number") }, singleLine = true) }
                Row(Modifier.padding(vertical = 9.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) { listOf("+", "−", "×", "÷").forEach { buttonOp -> FilterChip(selected = op == buttonOp, onClick = { op = buttonOp }, label = { Text(buttonOp, fontSize = 18.sp) }) } }
                Text("${result?.let { "%.2f".format(Locale.US, it) } ?: if (x != null && y == 0.0 && op == "÷") "Cannot divide by zero" else "—"}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(15.dp)); SectionTitle("Formula Library", "Quick revision references")
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(16.dp)) { listOf("Percentage = (part ÷ whole) × 100", "Average = sum ÷ number of values", "Speed = distance ÷ time", "Force = mass × acceleration", "Power = work ÷ time", "Rectangle area = length × width", "Triangle area = ½ × base × height").forEach { Text("• $it", Modifier.padding(vertical = 5.dp)) } } }
        Spacer(Modifier.height(15.dp)); SectionTitle("Conversions", "Common school references")
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) { Text("1 km = 1000 m\n1 m = 100 cm\n1 kg = 1000 g\n1 L = 1000 mL\n1 hour = 60 minutes", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.height(15.dp)); SectionTitle("Multiplication", "7 times table")
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) { Text((1..10).joinToString("\n") { "7 × $it = ${7 * it}" }, Modifier.padding(16.dp), fontWeight = FontWeight.Medium) }
    }
}

@Composable
fun NotesScreen(notes: List<Note>, onChange: (List<Note>) -> Unit) {
    var title by remember { mutableStateOf("") }; var body by remember { mutableStateOf("") }; var search by remember { mutableStateOf("") }
    val visible = notes.filter { search.isBlank() || it.title.contains(search, true) || it.body.contains(search, true) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("My Notes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text("Saved locally on this device", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth().padding(top = 10.dp), label = { Text("Title") }, singleLine = true)
        OutlinedTextField(body, { body = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Write your note") }, minLines = 3)
        Button(onClick = { if (title.isNotBlank() || body.isNotBlank()) { onChange(listOf(Note(System.currentTimeMillis(), title.ifBlank { "Untitled" }, body)) + notes); title = ""; body = "" } }, Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(15.dp)) { Text("Save Note") }
        OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth().padding(bottom = 8.dp), label = { Text("Search notes") }, singleLine = true)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(visible, key = { it.id }) { note -> Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(14.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(note.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); TextButton(onClick = { onChange(notes.filterNot { it.id == note.id }) }) { Text("Delete") } }; Text(note.body, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
    }
}

@Composable
fun MoreScreen(dark: Boolean, setDark: (Boolean) -> Unit, score: Int, streak: Int, quizzes: Int, tasks: List<Task>, onTasks: (List<Task>) -> Unit, onNavigate: (String) -> Unit) {
    var taskText by remember { mutableStateOf("") }; var subject by remember { mutableStateOf("General") }; var showTimer by remember { mutableStateOf(false) }; var showCards by remember { mutableStateOf(false) }
    val done = tasks.count { it.done }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Planner & More", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text("Your study command center", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(16.dp)) { Text("📅 Study Planner", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) { OutlinedTextField(taskText, { taskText = it }, Modifier.weight(1f), label = { Text("New task") }, singleLine = true); Button(onClick = { if (taskText.isNotBlank()) { onTasks(tasks + Task(System.currentTimeMillis(), taskText, subject, false)); taskText = "" } }, shape = RoundedCornerShape(13.dp)) { Text("+") } }; Row(Modifier.padding(vertical = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("General", "Math", "Science", "Computer", "English").forEach { s -> FilterChip(selected = subject == s, onClick = { subject = s }, label = { Text(s) }) } }; Text("$done/${tasks.size} completed", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
        }
        tasks.take(8).forEach { task ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = task.done, onCheckedChange = { checked -> onTasks(tasks.map { if (it.id == task.id) it.copy(done = checked) else it }) }); Column(Modifier.weight(1f)) { Text(task.text, fontWeight = if (task.done) FontWeight.Normal else FontWeight.SemiBold); Text(task.subject, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }; TextButton(onClick = { onTasks(tasks.filterNot { it.id == task.id }) }) { Text("×") } }
        }
        Spacer(Modifier.height(10.dp))
        Button(onClick = { showTimer = !showTimer }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) { Text(if (showTimer) "Hide Focus Timer" else "⏱ Open Focus Timer") }
        AnimatedVisibility(showTimer, enter = scaleIn() + fadeIn()) { FocusTimer() }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { showCards = !showCards }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) { Text(if (showCards) "Hide Flashcards" else "🃏 Open Flashcards") }
        AnimatedVisibility(showCards, enter = fadeIn() + slideInVertically { it / 3 }) { FlashcardDeck() }
        Spacer(Modifier.height(15.dp)); SectionTitle("Your Stats", "A quick look at your progress")
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(17.dp)) { Text("Total score: $score", fontWeight = FontWeight.Bold); Text("Streak: 🔥 $streak", Modifier.padding(top = 6.dp)); Text("Quizzes completed: $quizzes", Modifier.padding(top = 6.dp)); Text("Tasks completed: $done / ${tasks.size}", Modifier.padding(top = 6.dp)) } }
        Spacer(Modifier.height(15.dp)); SectionTitle("Appearance", "Make the app comfortable for you")
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Dark theme", fontWeight = FontWeight.Bold); Text("Smooth, eye-friendly interface", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }; Switch(checked = dark, onCheckedChange = setDark) } }
        Spacer(Modifier.height(15.dp)); OutlinedButton(onClick = { onNavigate("Home") }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) { Text("Back to Dashboard") }
        Spacer(Modifier.height(8.dp)); Text("Narendar Hub • offline study companion", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun FocusTimer() {
    var seconds by remember { mutableIntStateOf(25 * 60) }
    var running by remember { mutableStateOf(false) }
    LaunchedEffect(running) { while (running && seconds > 0) { delay(1000); seconds-- }; if (seconds == 0) running = false }
    val min = seconds / 60; val sec = seconds % 60
    Card(Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("Focus session", fontWeight = FontWeight.Bold); Text(String.format(Locale.US, "%02d:%02d", min, sec), fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { running = !running }, shape = RoundedCornerShape(14.dp)) { Text(if (running) "Pause" else "Start") }; OutlinedButton(onClick = { running = false; seconds = 25 * 60 }, shape = RoundedCornerShape(14.dp)) { Text("Reset") } } } }
}

@Composable
fun FlashcardDeck() {
    var index by remember { mutableIntStateOf(0) }; var reveal by remember { mutableStateOf(false) }
    val card = flashCards[index]
    Card(Modifier.fillMaxWidth().padding(top = 10.dp).clickable { reveal = !reveal }, shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Text(if (reveal) "ANSWER" else "TERM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Text(if (reveal) card.back else card.front, Modifier.padding(vertical = 22.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center); Text("Tap card to flip • ${index + 1}/${flashCards.size}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { index = (index - 1 + flashCards.size) % flashCards.size; reveal = false }) { Text("‹") }; Button(onClick = { index = (index + 1) % flashCards.size; reveal = false }) { Text("Next") } } } }
}

private fun notesToJson(notes: List<Note>): String = JSONArray().apply { notes.forEach { put(JSONObject().apply { put("id", it.id); put("title", it.title); put("body", it.body) }) } }.toString()
private fun tasksToJson(tasks: List<Task>): String = JSONArray().apply { tasks.forEach { put(JSONObject().apply { put("id", it.id); put("text", it.text); put("subject", it.subject); put("done", it.done) }) } }.toString()

private fun loadNotes(prefs: android.content.SharedPreferences): List<Note> = runCatching { val a = JSONArray(prefs.getString("notes", "[]")); (0 until a.length()).map { val o = a.getJSONObject(it); Note(o.getLong("id"), o.getString("title"), o.getString("body")) } }.getOrDefault(emptyList())
private fun loadTasks(prefs: android.content.SharedPreferences): List<Task> = runCatching { val a = JSONArray(prefs.getString("tasks", "[]")); (0 until a.length()).map { val o = a.getJSONObject(it); Task(o.getLong("id"), o.getString("text"), o.getString("subject"), o.getBoolean("done")) } }.getOrDefault(emptyList())
