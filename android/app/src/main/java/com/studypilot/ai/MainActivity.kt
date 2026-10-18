package com.studypilot.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { StudyPilotApp() } }
}

@Composable
fun StudyPilotApp() {
    var screen by remember { mutableStateOf("Home") }
    MaterialTheme {
        Scaffold(topBar={ TopAppBar(title={Text("StudyPilot AI")}) }, bottomBar={
            NavigationBar { listOf("Home","Learn","Practice","Tools","Profile").forEach { item -> NavigationBarItem(selected=screen==item,onClick={screen=item},icon={},label={Text(item)}) } }
        }) { pad ->
            when(screen) {
                "Home" -> Dashboard(Modifier.padding(pad))
                "Learn" -> LearnScreen(Modifier.padding(pad))
                "Practice" -> PracticeScreen(Modifier.padding(pad))
                "Tools" -> ToolsScreen(Modifier.padding(pad))
                else -> ProfileScreen(Modifier.padding(pad))
            }
        }
    }
}

@Composable fun Dashboard(modifier: Modifier=Modifier) { Column(modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
    Text("Good morning 👋",style=MaterialTheme.typography.headlineMedium); Text("Your learning command center",style=MaterialTheme.typography.bodyLarge)
    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) { Stat("Accuracy","--"); Stat("Streak","0 days"); Stat("XP","0") }
    Card { Column(Modifier.padding(18.dp)) { Text("Today's plan",style=MaterialTheme.typography.titleLarge); Text("• 15 min Mathematics\n• 10 min revision\n• 1 quick quiz\n• Review your mistakes") } }
    Card { Column(Modifier.padding(18.dp)) { Text("AI Tutor",style=MaterialTheme.typography.titleLarge); Text("Ask questions and get step-by-step explanations tailored to your level.") } }
} }
@Composable fun Stat(a:String,b:String)=Card{Column(Modifier.padding(12.dp)){Text(b,style=MaterialTheme.typography.titleMedium);Text(a)}}

@Composable fun LearnScreen(modifier: Modifier=Modifier) { val subjects=listOf("Mathematics","English","Science","Computer Science"); LazyColumn(modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){ item{Text("Learn",style=MaterialTheme.typography.headlineMedium);Text("Choose a subject and continue where you left off.")} items(subjects){s->Card{Column(Modifier.padding(18.dp)){Text(s,style=MaterialTheme.typography.titleLarge);Text("Lessons • Topics • Practice • Revision")}}} } }

@Composable fun PracticeScreen(modifier: Modifier=Modifier) { Column(modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("Practice",style=MaterialTheme.typography.headlineMedium);listOf("Quick Quiz","Mock Exam","Mistake Review","Flashcards","Daily Challenge","Adaptive Practice").forEach{Card{Text(it,Modifier.padding(18.dp),style=MaterialTheme.typography.titleMedium)}}} }
@Composable fun ToolsScreen(modifier: Modifier=Modifier) { Column(modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("Study Tools",style=MaterialTheme.typography.headlineMedium);Text("Tables  •  Calculator  •  Notes  •  Bookmarks  •  Search  •  Focus Timer  •  Formula Reference  •  Dictionary") } }
@Composable fun ProfileScreen(modifier: Modifier=Modifier) { Column(modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("Profile",style=MaterialTheme.typography.headlineMedium);Text("Class 8");Text("Goals");Text("Language");Text("Notifications");Text("Theme");Text("Privacy & Security") } }

suspend fun apiGet(path:String):String = withContext(Dispatchers.IO){ val req=Request.Builder().url(BuildConfig.API_BASE_URL+path).build(); OkHttpClient().newCall(req).execute().use{it.body?.string()?:("{}")}}
fun parseDashboard(json:String):JSONObject=JSONObject(json)
