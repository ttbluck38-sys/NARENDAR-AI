package com.narendar.hub

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NarendarHubApp(applicationContext) }
    }
}

data class QuizQ(val subject: String, val question: String, val options: List<String>, val answer: Int, val explanation: String)
data class Note(val id: Long, val title: String, val body: String)
data class Task(val id: Long, val text: String, val subject: String, val done: Boolean)

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NarendarHubApp(context: Context) {
    val prefs = remember { context.getSharedPreferences("narendar_hub", Context.MODE_PRIVATE) }
    var page by remember { mutableStateOf("Home") }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark", false)) }
    var score by remember { mutableIntStateOf(prefs.getInt("score", 0)) }
    var streak by remember { mutableIntStateOf(prefs.getInt("streak", 0)) }
    var notes by remember { mutableStateOf(emptyList<Note>()) }
    var tasks by remember { mutableStateOf(emptyList<Task>()) }

    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Narendar Hub", fontWeight = FontWeight.Bold) }) },
            bottomBar = {
                NavigationBar {
                    listOf("Home" to "⌂", "Quiz" to "✓", "Tools" to "⌕", "Notes" to "✎", "More" to "☰").forEach { (name, icon) ->
                        NavigationBarItem(
                            selected = page == name,
                            onClick = { page = name },
                            icon = { Text(icon, fontSize = 20.sp) },
                            label = { Text(name) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (page) {
                    "Home" -> HomeScreen(score, streak, tasks) { page = it }
                    "Quiz" -> QuizScreen { gained ->
                        score += gained
                        streak = (streak + 1).coerceAtMost(999)
                        prefs.edit().putInt("score", score).putInt("streak", streak).apply()
                    }
                    "Tools" -> ToolsScreen()
                    "Notes" -> NotesScreen(notes) { notes = it }
                    else -> MoreScreen(dark, { value -> dark = value; prefs.edit().putBoolean("dark", value).apply() }, score, streak, tasks) { tasks = it }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(score: Int, streak: Int, tasks: List<Task>, onNavigate: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Good day, learner 👋", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Everything you need to study — offline.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Score", score.toString(), Modifier.weight(1f))
                StatCard("Streak", "🔥 $streak", Modifier.weight(1f))
                StatCard("Tasks", tasks.count { !it.done }.toString(), Modifier.weight(1f))
            }
        }
        item { Text("Quick actions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item { ActionCard("🧠", "Start a Quiz", "Practice built-in MCQs", { onNavigate("Quiz") }) }
        item { ActionCard("🧮", "Study Tools", "Calculator, formulas and tables", { onNavigate("Tools") }) }
        item { ActionCard("📝", "My Notes", "Write and organize notes", { onNavigate("Notes") }) }
        item { ActionCard("📅", "Study Planner", "Set tasks and track progress", { onNavigate("More") }) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Why Narendar Hub?", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("No account, API key, server or internet is required for the core app.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier) {
    Card(modifier) { Column(Modifier.padding(12.dp)) { Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
}

@Composable
fun ActionCard(icon: String, title: String, sub: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 28.sp)
            Spacer(Modifier.width(14.dp))
            Column { Text(title, fontWeight = FontWeight.Bold); Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
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

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Quiz Arena", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Built-in questions • fully offline", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Math", "Science", "Computer", "English", "GK").forEach { subject ->
                FilterChip(selected = selectedSubject == subject, onClick = { selectedSubject = subject; current = 0; correct = 0; picked = -1; finished = false }, label = { Text(subject) })
            }
        }
        Spacer(Modifier.height(16.dp))
        if (finished) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Quiz complete! 🎉", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("$correct / ${filtered.size} correct", fontSize = 22.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { current = 0; correct = 0; picked = -1; finished = false }) { Text("Try Again") }
                }
            }
        } else {
            val q = filtered[current]
            Text("Question ${current + 1} of ${filtered.size} • ${q.subject}", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(q.question, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            q.options.forEachIndexed { index, option ->
                OutlinedButton(
                    onClick = { if (picked == -1) picked = index },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (picked == index) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                ) { Text(option, Modifier.fillMaxWidth()) }
            }
            if (picked != -1) {
                Spacer(Modifier.height(8.dp))
                Text(if (picked == q.answer) "✓ Correct!" else "✗ Not quite. Correct: ${q.options[q.answer]}", fontWeight = FontWeight.Bold)
                Text(q.explanation, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Button(onClick = {
                    if (picked == q.answer) correct++
                    if (current == filtered.lastIndex) { finished = true; onComplete(correct) } else { current++; picked = -1 }
                }, modifier = Modifier.fillMaxWidth()) { Text(if (current == filtered.lastIndex) "Finish" else "Next") }
            }
        }
    }
}

@Composable
fun ToolsScreen() {
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    var op by remember { mutableStateOf("+") }
    val x = a.toDoubleOrNull()
    val y = b.toDoubleOrNull()
    val result = if (x != null && y != null) when (op) { "+" -> x + y; "−" -> x - y; "×" -> x * y; "÷" -> if (y == 0.0) null else x / y; else -> null } else null
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Study Tools", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Calculator", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(a, { a = it }, Modifier.weight(1f), label = { Text("Number") })
            OutlinedTextField(b, { b = it }, Modifier.weight(1f), label = { Text("Number") })
        }
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("+", "−", "×", "÷").forEach { buttonOp -> Button(onClick = { op = buttonOp }) { Text(buttonOp) } } }
        Text("Result: ${result?.let { "%.2f".format(it) } ?: if (x != null && y == 0.0 && op == "÷") "Cannot divide by zero" else "—"}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        Text("Quick formulas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        listOf("Percentage = (part ÷ whole) × 100", "Average = sum of values ÷ number of values", "Speed = distance ÷ time", "Force = mass × acceleration", "Power = work ÷ time", "Rectangle area = length × width", "Triangle area = ½ × base × height").forEach { Text("• $it", Modifier.padding(vertical = 5.dp)) }
        Spacer(Modifier.height(16.dp))
        Text("Unit converter reference", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("1 km = 1000 m • 1 m = 100 cm • 1 kg = 1000 g • 1 L = 1000 mL", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text("7 Times Table", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text((1..10).joinToString("   ") { "7×$it=${7 * it}" })
    }
}

@Composable
fun NotesScreen(notes: List<Note>, onChange: (List<Note>) -> Unit) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("My Notes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth().padding(top = 10.dp), label = { Text("Title") })
        OutlinedTextField(body, { body = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Write your note") }, minLines = 3)
        Button(onClick = { if (title.isNotBlank() || body.isNotBlank()) { onChange(listOf(Note(System.currentTimeMillis(), title.ifBlank { "Untitled" }, body)) + notes); title = ""; body = "" } }, Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Text("Save Note") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(notes, key = { it.id }) { note ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(note.title, fontWeight = FontWeight.Bold); TextButton(onClick = { onChange(notes.filterNot { it.id == note.id }) }) { Text("Delete") } }
                        Text(note.body)
                    }
                }
            }
        }
    }
}

@Composable
fun MoreScreen(dark: Boolean, setDark: (Boolean) -> Unit, score: Int, streak: Int, tasks: List<Task>, onTasks: (List<Task>) -> Unit) {
    var taskText by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("General") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Planner & Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Study Planner", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(taskText, { taskText = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("New task") })
                OutlinedTextField(subject, { subject = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Subject") })
                Button(onClick = { if (taskText.isNotBlank()) { onTasks(tasks + Task(System.currentTimeMillis(), taskText, subject, false)); taskText = "" } }, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Add Task") }
            }
        }
        Spacer(Modifier.height(12.dp))
        tasks.forEach { task ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = task.done, onCheckedChange = { checked -> onTasks(tasks.map { if (it.id == task.id) it.copy(done = checked) else it }) })
                Column(Modifier.weight(1f)) { Text(task.text, fontWeight = FontWeight.Bold); Text(task.subject, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                TextButton(onClick = { onTasks(tasks.filterNot { it.id == task.id }) }) { Text("Delete") }
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 16.dp))
        Text("Stats", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Total score: $score")
        Text("Current streak: $streak")
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Dark theme", fontWeight = FontWeight.Bold)
            Switch(checked = dark, onCheckedChange = setDark)
        }
        Spacer(Modifier.height(12.dp))
        Text("About", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Narendar Hub • offline study companion • no account or API key required.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
