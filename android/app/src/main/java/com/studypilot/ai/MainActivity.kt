package com.studypilot.ai

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

private val client = OkHttpClient()
private const val PREFS = "studypilot"
private const val TOKEN = "token"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StudyPilotApp(getSharedPreferences(PREFS, Context.MODE_PRIVATE)) }
    }
}

private suspend fun api(path: String, method: String = "GET", body: String? = null, token: String? = null): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
        val builder = Request.Builder().url(BuildConfig.API_BASE_URL.trimEnd('/') + path)
        if (!token.isNullOrBlank()) builder.header("Authorization", "Bearer $token")
        if (method != "GET") builder.method(method, (body ?: "{}").toRequestBody("application/json".toMediaType()))
        client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) error(JSONObject(text).optString("detail", "Request failed (${response.code})"))
            text
        }
    }
}

@Composable
fun StudyPilotApp(prefs: android.content.SharedPreferences) {
    var token by remember { mutableStateOf(prefs.getString(TOKEN, null)) }
    var screen by remember { mutableStateOf("Home") }
    MaterialTheme(colorScheme = lightColorScheme()) {
        if (token.isNullOrBlank()) {
            AuthScreen(onLoggedIn = { t -> prefs.edit().putString(TOKEN, t).apply(); token = t })
        } else {
            Scaffold(topBar = { TopAppBar(title = { Text("StudyPilot AI") }) }, bottomBar = {
                NavigationBar { listOf("Home", "Learn", "Practice", "Tools", "Profile").forEach { item ->
                    NavigationBarItem(selected = screen == item, onClick = { screen = item }, icon = { Text(item.take(1)) }, label = { Text(item) })
                }}
            }) { pad ->
                when (screen) {
                    "Home" -> Dashboard(Modifier.padding(pad), token!!)
                    "Learn" -> LearnScreen(Modifier.padding(pad), token!!)
                    "Practice" -> PracticeScreen(Modifier.padding(pad), token!!)
                    "Tools" -> ToolsScreen(Modifier.padding(pad))
                    else -> ProfileScreen(Modifier.padding(pad), prefs) { prefs.edit().remove(TOKEN).apply(); token = null }
                }
            }
        }
    }
}

@Composable
private fun AuthScreen(onLoggedIn: (String) -> Unit) {
    var register by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("8") }; var loading by remember { mutableStateOf(false) }; var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("StudyPilot AI", style = MaterialTheme.typography.displaySmall); Text("Your adaptive learning command center", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        if (register) OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
        if (register) OutlinedTextField(grade, { grade = it.filter(Char::isDigit) }, label = { Text("Grade") }, modifier = Modifier.fillMaxWidth())
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        Button(enabled = !loading, onClick = {
            loading = true; error = ""
            scope.launch {
                val payload = if (register) JSONObject().apply { put("name", name); put("email", email); put("password", password); put("grade", grade.toIntOrNull() ?: 8) } else JSONObject().apply { put("email", email); put("password", password) }
                val r = api(if (register) "/api/v1/auth/register" else "/api/v1/auth/login", "POST", payload.toString())
                loading = false
                r.onSuccess { onLoggedIn(JSONObject(it).getString("token")) }.onFailure { error = it.message ?: "Unable to connect" }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(if (loading) "Please wait…" else if (register) "Create account" else "Sign in") }
        TextButton(onClick = { register = !register; error = "" }) { Text(if (register) "Already have an account? Sign in" else "Create a new account") }
    }
}

@Composable
private fun Dashboard(modifier: Modifier, token: String) {
    var data by remember { mutableStateOf<JSONObject?>(null) }; var error by remember { mutableStateOf("") }
    LaunchedEffect(token) { api("/api/v1/dashboard", token = token).onSuccess { data = JSONObject(it) }.onFailure { error = it.message ?: "Failed to load dashboard" } }
    val d = data
    Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Good to see you 👋", style = MaterialTheme.typography.headlineMedium); Text("Personalized learning, powered by your progress.")
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        if (d == null) CircularProgressIndicator() else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("Accuracy", "${d.optDouble("accuracy", 0.0)}%", Modifier.weight(1f)); Stat("Streak", "${d.optJSONObject("user")?.optInt("streak", 0) ?: 0} days", Modifier.weight(1f)); Stat("XP", d.optJSONObject("user")?.optInt("xp", 0).toString(), Modifier.weight(1f))
            }
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text("Level ${d.optInt("level", 1)}", style = MaterialTheme.typography.titleLarge); Text("${d.optInt("xp_to_next", 100)} XP to next level"); Text("${d.optInt("topics_mastered", 0)} topics mastered") } }
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text("AI Tutor", style = MaterialTheme.typography.titleLarge); Text("Ask for simple explanations, deep explanations, hints and worked examples.") } }
        }
    }
}
@Composable private fun Stat(title: String, value: String, modifier: Modifier) = Card(modifier) { Column(Modifier.padding(12.dp)) { Text(value, style = MaterialTheme.typography.titleMedium); Text(title) } }

@Composable private fun LearnScreen(modifier: Modifier, token: String) {
    var subjects by remember { mutableStateOf(listOf<String>()) }
    LaunchedEffect(token) { api("/api/v1/curriculum/8", token = token).onSuccess { j -> subjects = JSONObject(j).getJSONArray("subjects").let { a -> List(a.length()) { i -> a.getJSONObject(i).getString("name") } } } }
    LazyColumn(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { item { Text("Learn", style = MaterialTheme.typography.headlineMedium); Text("Choose a subject and explore its topics.") }; items(subjects) { s -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text(s, style = MaterialTheme.typography.titleLarge); Text(TOPIC_COPY[s] ?: "Lessons • Practice • Revision") } } } }
}
private val TOPIC_COPY = mapOf("Mathematics" to "Numbers • Fractions • Percentages • Algebra • Geometry", "English" to "Grammar • Vocabulary • Reading • Writing", "Science" to "Matter • Energy • Forces • Living Things", "Computer Science" to "Algorithms • Programming • Data • AI")

@Composable private fun PracticeScreen(modifier: Modifier, token: String) {
    var message by remember { mutableStateOf("") }; val scope = rememberCoroutineScope()
    Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Practice", style = MaterialTheme.typography.headlineMedium); Text("Adaptive tools that feed your progress engine.")
        listOf("Quick Quiz" to "Generate questions for a topic", "Mistake Review" to "Review weak answers", "Revision Queue" to "Mastery-first review", "Study Planner" to "Build a daily plan", "Achievements" to "Track XP milestones").forEach { (a,b) -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text(a, style = MaterialTheme.typography.titleMedium); Text(b); Button(onClick = { scope.launch { message = api("/api/v1/revision/queue", token = token).fold({ "Revision queue loaded successfully." }, { it.message ?: "Could not load" }) } }) { Text("Open") } } } }
        if (message.isNotBlank()) Text(message)
    }
}

@Composable private fun ToolsScreen(modifier: Modifier) { Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Study Tools", style = MaterialTheme.typography.headlineMedium); listOf("Multiplication Tables", "Safe Calculator", "Unit Converter", "Formula Reference", "Notes", "Bookmarks", "Focus Mode", "Search").forEach { Card(Modifier.fillMaxWidth()) { Text(it, Modifier.padding(18.dp), style = MaterialTheme.typography.titleMedium) } } } }
@Composable private fun ProfileScreen(modifier: Modifier, prefs: android.content.SharedPreferences, logout: () -> Unit) { Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Profile", style = MaterialTheme.typography.headlineMedium); Text("Account • Goals • Language • Notifications • Theme • Privacy & Security"); Button(onClick = logout) { Text("Sign out") } } }
