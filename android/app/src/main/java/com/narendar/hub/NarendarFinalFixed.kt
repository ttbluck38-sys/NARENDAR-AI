@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.narendar.hub

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import com.narendar.hub.ai.StudyPilotAIEngine

private data class M(val id:Int,val grade:Int,val subject:String,val diff:String,val topic:String,val q:String,val opts:List<String>,val ans:Int,val exp:String)
private data class Note(val id:Long,val title:String,val body:String)
private data class Task(val id:Long,val text:String,val done:Boolean)
private val subs=listOf("Mathematics" to "∑","Science" to "⚗","English" to "A","Computer Science" to "⌘","SST" to "◎","Urdu" to "ا","Physics" to "Φ","Chemistry" to "⚛","Biology" to "♧")
private val palette=listOf(Color(0xFF315EFB),Color(0xFF7C4DFF),Color(0xFF00A884),Color(0xFFFF8A3D),Color(0xFFE83E8C))

@Composable fun FixedHub(context:Context){
 val p=remember{context.getSharedPreferences("narendar_fixed",Context.MODE_PRIVATE)}
 var appLanguage by remember{mutableStateOf(p.getString("app_language","English") ?: "English")};var studentName by remember{mutableStateOf(p.getString("student_name","") ?: "")};var grade by remember{mutableIntStateOf(p.getInt("grade",-1))};var page by remember{mutableStateOf(if(StudentAuth.token(context).isBlank())"auth" else if(grade<0)"pick" else "home")};var dark by remember{mutableStateOf(p.getBoolean("dark",false))}
 var xp by remember{mutableIntStateOf(p.getInt("xp",0))};var correct by remember{mutableIntStateOf(p.getInt("correct",0))};var tried by remember{mutableIntStateOf(p.getInt("tried",0))};var notes by remember{mutableStateOf(loadNotes(p))};var tasks by remember{mutableStateOf(loadTasks(p))};var toast by remember{mutableStateOf("")};var showExitDialog by remember{mutableStateOf(false)};var sessionReady by remember{mutableStateOf(false)}
 fun record(ok:Boolean){tried++;if(ok)correct++;xp+=if(ok)20 else 5;p.edit().putInt("xp",xp).putInt("correct",correct).putInt("tried",tried).apply()}
 fun saveNotes(v:List<Note>){notes=v;p.edit().putString("notes",notesJson(v)).apply()}
 fun saveTasks(v:List<Task>){tasks=v;p.edit().putString("tasks",tasksJson(v)).apply()}
 BackHandler {
  if (page != "home" && page != "pick") page = "home" else showExitDialog = true
 }
 LaunchedEffect(Unit) {
   val valid = withContext(Dispatchers.IO) { StudentAuth.restore(context) }
   if (!valid) { page = "auth" }
   sessionReady = true
  }
 LaunchedEffect(toast){if(toast.isNotEmpty()){delay(1500);toast=""}}
 LaunchedEffect(grade, studentName, appLanguage) {
   if (StudentAuth.token(context).isNotBlank() && grade in 1..12) {
    withContext(Dispatchers.IO) { runCatching { StudentAuth.saveProfile(context, studentName, grade, appLanguage) } }
   }
  }
 androidx.compose.runtime.CompositionLocalProvider(LocalAppLanguage provides appLanguage) { MaterialTheme(colorScheme=if(dark)darkColorScheme(primary=Color(0xFF9AB8FF),secondary=Color(0xFF70E0BE),tertiary=Color(0xFFFF9DC7))else lightColorScheme(primary=palette[0],secondary=palette[2],tertiary=palette[4],surface=Color(0xFFF7F8FC))){
  if(!sessionReady){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()};return@MaterialTheme}
  if(showExitDialog){AlertDialog(onDismissRequest={showExitDialog=false},title={AppText("Close StudyPilot AI?")},text={AppText("Do you want to close the app?")},confirmButton={TextButton(onClick={showExitDialog=false;(context as? android.app.Activity)?.finish()}){AppText("Close app")}},dismissButton={TextButton(onClick={showExitDialog=false}){AppText("Stay")}})}
  if(page=="auth"){StudentLogin{page=if(grade<0)"pick" else "home"};return@MaterialTheme}
  if(page=="pick"){Pick(grade,appLanguage,{appLanguage=it;p.edit().putString("app_language",it).apply()},studentName,{studentName=it;p.edit().putString("student_name",it).apply()}){grade=it;p.edit().putInt("grade",it).apply();page="home"};return@MaterialTheme}
  Scaffold(topBar={CenterAlignedTopAppBar(title={AppText("StudyPilot AI",fontWeight=FontWeight.Black)})},bottomBar={NavigationBar{listOf("home" to "⌂","learn" to "▣","quiz" to "✓","tutor" to "✦","library" to "▤","profile" to "●","student_info" to "◎").forEach{(k,ic)->NavigationBarItem(page==k,{page=k},icon={AppText(ic)},label={AppText(if(k=="tutor")"StudyPilot AI" else if(k=="student_info")"My Info" else k.replaceFirstChar{it.uppercase()})})}}}){pad->Box(Modifier.fillMaxSize().padding(pad)){AnimatedContent(page,label="page"){s->when(s){"home"->Home(grade,xp,correct,tried,tasks.count{!it.done},notes.size){page=it};"learn"->Learn(grade){page="quiz"};"quiz"->Quiz(grade,appLanguage,studentName,::record){toast=it};"library"->Library(grade,notes,tasks,::saveNotes,::saveTasks);"tutor"->StudyPilotAIPage(grade,appLanguage,studentName);"student_info"->StudentInfoPage(studentName,grade,appLanguage,{studentName=it;p.edit().putString("student_name",it).apply()},{appLanguage=it;p.edit().putString("app_language",it).apply()},{StudentAuth.logout(context);page="auth"});"profile"->Profile(grade,xp,correct,tried,studentName,{studentName=it;p.edit().putString("student_name",it).apply()},dark,{dark=it;p.edit().putBoolean("dark",it).apply()},appLanguage,{appLanguage=it;p.edit().putString("app_language",it).apply()}){grade=-1;p.edit().putInt("grade",-1).apply();page="pick"}}};AnimatedVisibility(toast.isNotEmpty(),enter=slideInVertically{it}+fadeIn(),exit=fadeOut(),modifier=Modifier.align(Alignment.BottomCenter).padding(8.dp)){Surface(shape=RoundedCornerShape(18.dp),tonalElevation=8.dp){AppText(toast,Modifier.padding(14.dp),fontWeight=FontWeight.Bold)}}}}
 }
}

 }
@Composable private fun Pick(g:Int,language:String,setLanguage:(String)->Unit,studentName:String,setStudentName:(String)->Unit,choose:(Int)->Unit){val pulse=rememberInfiniteTransition(label="p").animateFloat(1f,1.06f,infiniteRepeatable(tween(1100),RepeatMode.Reverse),label="p").value;Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF111827),Color(0xFF312E81),Color(0xFF0F766E)))).verticalScroll(rememberScrollState())){Column(Modifier.padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally){Spacer(Modifier.height(25.dp));Surface(shape=CircleShape,color=Color.White.copy(.13f),modifier=Modifier.size(100.dp).scale(pulse)){Box(contentAlignment=Alignment.Center){Image(painter=painterResource(R.drawable.studypilot_logo),contentDescription="StudyPilot AI logo",modifier=Modifier.fillMaxSize().padding(8.dp))}};AppText("STUDYPILOT AI",color=Color.White,fontSize=29.sp,fontWeight=FontWeight.Black);AppText("Learn • Practice • Master",color=Color.White.copy(.7f));Spacer(Modifier.height(22.dp));AppText("Welcome to StudyPilot AI",color=Color.White,fontSize=21.sp,fontWeight=FontWeight.Black);OutlinedTextField(value=studentName,onValueChange=setStudentName,label={AppText("Student name")},singleLine=true,modifier=Modifier.fillMaxWidth());LanguagePicker(language,setLanguage);AppText("Class 8 includes Class 1–8 learning.",color=Color.White.copy(.65f));Spacer(Modifier.height(10.dp));listOf(-1 to "🌎 All Classes",1 to "🎒 Class 1",2 to "🎒 Class 2",3 to "🎒 Class 3",4 to "🎒 Class 4",5 to "🎒 Class 5",6 to "🎒 Class 6",7 to "🎒 Class 7",8 to "🚀 Class 8",9 to "🏆 Class 9+").forEach{(v,t)->Card(onClick={choose(v)},modifier=Modifier.fillMaxWidth().padding(4.dp),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White.copy(.11f))){Row(Modifier.padding(16.dp)){AppText(t,color=Color.White,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));AppText(if(g==v)"✓" else "›",color=Color.White,fontSize=22.sp)}}};AppText("Generated practice set • curriculum review pending",color=Color.White.copy(.55f),fontSize=12.sp,modifier=Modifier.padding(top=10.dp))}}}

@Composable private fun Home(g:Int,xp:Int,ok:Int,tried:Int,tasks:Int,notes:Int,nav:(String)->Unit){val acc=if(tried==0)0 else ok*100/tried;val pulse=rememberInfiniteTransition(label="hero").animateFloat(.97f,1f,infiniteRepeatable(tween(1400),RepeatMode.Reverse),label="hero").value;LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{AppText("Welcome back 👋",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black);AppText(if(g<0)"All Classes" else "Class $g",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)};item{Card(Modifier.fillMaxWidth().scale(pulse),shape=RoundedCornerShape(30.dp)){Box(Modifier.background(Brush.linearGradient(listOf(palette[(xp/100)%5],palette[(xp/100+2)%5])))){Column(Modifier.padding(22.dp)){AppText("LEVEL ${xp/500+1} • $xp XP",color=Color.White.copy(.8f),fontWeight=FontWeight.Bold);AppText("Your learning journey",color=Color.White,fontSize=27.sp,fontWeight=FontWeight.Black);Spacer(Modifier.height(12.dp));LinearProgressIndicator({(xp%500)/500f},Modifier.fillMaxWidth().height(9.dp),color=Color.White,trackColor=Color.White.copy(.2f));Spacer(Modifier.height(9.dp));AppText("🎯 $acc% accuracy   📚 $tried attempts",color=Color.White,fontWeight=FontWeight.Bold)}}}};item{AppText("Explore",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black)};item{Action("🎮","Quiz Arena","Practice MCQs • Easy / Medium / Hard",palette[0]){nav("quiz")}};item{Action("📚","Study Library","Subjects + explanations",palette[1]){nav("learn")}};item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Stat("Questions",tried.toString(),palette[2],Modifier.weight(1f));Stat("Notes",notes.toString(),palette[3],Modifier.weight(1f));Stat("Tasks",tasks.toString(),palette[4],Modifier.weight(1f))}};item{Action("🃏","Revision Hub","Notes + planner",palette[2]){nav("library")}};item{Action("🏆","Stats + Settings","XP + theme",palette[3]){nav("profile")}}}}
@Composable private fun Action(ic:String,title:String,sub:String,c:Color,go:()->Unit){Card(Modifier.fillMaxWidth().clickable{go()},shape=RoundedCornerShape(22.dp)){Row(Modifier.padding(17.dp),verticalAlignment=Alignment.CenterVertically){Surface(shape=CircleShape,color=c.copy(.13f),modifier=Modifier.size(50.dp)){Box(contentAlignment=Alignment.Center){AppText(ic,fontSize=25.sp)}};Spacer(Modifier.width(13.dp));Column(Modifier.weight(1f)){AppText(title,fontWeight=FontWeight.Black);AppText(sub,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};AppText("›",fontSize=28.sp,color=c)}}}
@Composable private fun Stat(n:String,v:String,c:Color,m:Modifier){Card(m,shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(13.dp)){AppText(v,fontSize=22.sp,fontWeight=FontWeight.Black,color=c);AppText(n,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}

@Composable private fun Learn(g:Int,start:()->Unit){var sub by remember{mutableStateOf("Mathematics")};val list=if(g<0||g>=9)subs else subs.take(6);Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){AppText("Study Library",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black);AppText("Study → revise → practice → test",color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(12.dp));list.forEachIndexed{ix,(name,icon)->Card(onClick={sub=name},modifier=Modifier.fillMaxWidth().padding(vertical=4.dp),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=if(sub==name)palette[ix%5].copy(.15f)else MaterialTheme.colorScheme.surfaceVariant)){Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically){AppText(icon,fontSize=23.sp);Spacer(Modifier.width(12.dp));AppText(name,Modifier.weight(1f),fontWeight=FontWeight.Black);AppText(if(sub==name)"✓" else "›",color=palette[ix%5])}}};Spacer(Modifier.height(12.dp));Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(25.dp)){Column(Modifier.padding(20.dp)){AppText("📖 $sub",fontSize=23.sp,fontWeight=FontWeight.Black);AppText("Class ${if(g<0)"1–9+" else "1–$g"} • Generated practice set • curriculum review pending per difficulty",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(8.dp));AppText("MCQ Study Library",fontWeight=FontWeight.Bold);AppText("Every test question has a unique ID and an explanation.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(12.dp));Button(start,modifier=Modifier.fillMaxWidth()){AppText("START PRACTICE")}}}}}

@Composable private fun Quiz(g:Int,appLanguage:String,studentName:String,record:(Boolean)->Unit,toast:(String)->Unit){
 val context = LocalContext.current
 var sub by remember{mutableStateOf("Mathematics")}
 var diff by remember{mutableStateOf("Mixed")}
 var size by remember{mutableIntStateOf(10)}
 var start by remember{mutableStateOf(false)}
 var at by remember{mutableIntStateOf(0)}
 var pick by remember{mutableIntStateOf(-1)}
 var score by remember{mutableIntStateOf(0)}
 var seed by remember{mutableIntStateOf(0)}
  var revealUses by remember{mutableIntStateOf(0)}
 var questions by remember{mutableStateOf(emptyList<M>())}
 var generating by remember{mutableStateOf(false)}
 var generationError by remember{mutableStateOf("")}
 var fromGrade by remember(g){mutableIntStateOf(if(g in 1..8)g else 8)}
  val maxG=if(g<0)9 else g
 LaunchedEffect(start,sub,diff,size,seed,g,fromGrade,studentName,appLanguage){if(start){generating=true;generationError="";try{questions=withContext(Dispatchers.IO){generateAiQuiz(g,fromGrade,sub,diff,size,studentName,appLanguage,context)}}catch(e:Exception){generationError=e.message ?: "AI question generation failed. Check the Supabase AI backend and retry."}finally{generating=false}}}
 if(!start){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){
  AppText("Quiz Arena",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
     AppText("Fresh AI-generated questions tailored to the student profile, class, subject and difficulty.",color=MaterialTheme.colorScheme.onSurfaceVariant)
  Spacer(Modifier.height(12.dp));AppText("Subject",fontWeight=FontWeight.Black)
  Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){subs.forEach{(n,_)->FilterChip(sub==n,{sub=n},label={AppText(n)})}}
  Spacer(Modifier.height(10.dp));Row{listOf("Mixed","Easy","Medium","Hard").forEach{FilterChip(diff==it,{diff=it},label={AppText(it)},modifier=Modifier.padding(end=4.dp))}}
  Spacer(Modifier.height(10.dp));Row(Modifier.horizontalScroll(rememberScrollState())){listOf(10,20,30,50,100,500).forEach{FilterChip(size==it,{size=it},label={AppText("$it")},modifier=Modifier.padding(end=4.dp))}}
  Spacer(Modifier.height(16.dp));BigCard(palette[0],"🎮 READY","Practice MCQs • AI explanation • speaker support"){start=true}
 };return}
 if(generating){Column(Modifier.fillMaxSize().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){CircularProgressIndicator();Spacer(Modifier.height(12.dp));AppText("StudyPilot AI is creating questions for ${studentName.ifBlank{"you"}}…",fontWeight=FontWeight.Bold);AppText("Class $g • $sub • $diff",color=MaterialTheme.colorScheme.onSurfaceVariant)}}else if(generationError.isNotBlank()){Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.Center){AppText("Could not generate your quiz",fontWeight=FontWeight.Black);AppText(generationError,color=MaterialTheme.colorScheme.error);Button({start=false;generationError=""}){AppText("BACK TO QUIZ SETTINGS")};Button({seed++;generationError="";start=true}){AppText("RETRY AI GENERATION")}};return}else if(questions.isNotEmpty()&&at>=questions.size){Column(Modifier.fillMaxSize().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){
  Spacer(Modifier.height(40.dp));AppText("🏆",fontSize=70.sp);AppText("Test Complete",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black)
  AppText("$score / $size",fontSize=34.sp,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary)
  AppText("Accuracy ${score*100/size}%",color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(18.dp))
  Button({seed++;at=0;pick=-1;score=0;revealUses=0}){AppText("NEW UNIQUE TEST")}
 };return}
 if(questions.isEmpty()){return};val q=questions[at]
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){
  Row{AppText("${at+1}/$size",fontWeight=FontWeight.Black);Spacer(Modifier.weight(1f));AppText("+20 XP",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)}
  LinearProgressIndicator({(at+if(pick>=0)1 else 0).toFloat()/size},Modifier.fillMaxWidth().height(8.dp));Spacer(Modifier.height(10.dp))
  AppText("Class ${q.grade} • ${q.subject} • ${q.diff} • ${q.topic}",color=MaterialTheme.colorScheme.primary,fontSize=12.sp,fontWeight=FontWeight.Bold)
  Card(Modifier.fillMaxWidth().padding(vertical=10.dp),shape=RoundedCornerShape(25.dp)){AppText(q.q,Modifier.padding(21.dp),fontSize=19.sp,fontWeight=FontWeight.Black)}
  q.opts.forEachIndexed{i,o->
   val bad=pick==i&&i!=q.ans;val good=pick>=0&&i==q.ans
   Card(onClick={if(pick<0){pick=i;record(i==q.ans);StudentAuth.logEvent(context,"mcq_answer",q.subject,q.topic,q.q,q.opts[i],q.opts[q.ans],i==q.ans);if(i==q.ans)score++;toast(if(i==q.ans)"Correct! 🎉" else "Review explanation 📘")}},modifier=Modifier.fillMaxWidth().padding(vertical=4.dp),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=if(good)Color(0xFF16A34A).copy(.18f)else if(bad)Color(0xFFDC2626).copy(.18f)else MaterialTheme.colorScheme.surfaceVariant)){
    Row(Modifier.padding(15.dp)){AppText("${'A'+i}",fontWeight=FontWeight.Black,color=if(good)Color(0xFF16A34A)else if(bad)Color(0xFFDC2626)else MaterialTheme.colorScheme.primary);Spacer(Modifier.width(12.dp));AppText(o,fontWeight=FontWeight.SemiBold)}
   }
  }
  if(pick>=0){
   Card(Modifier.fillMaxWidth().padding(top=8.dp),shape=RoundedCornerShape(19.dp)){Column(Modifier.padding(15.dp)){
    AppText(if(pick==q.ans)"✓ Correct" else "✕ Review",fontWeight=FontWeight.Black)
    AppText(if(pick==q.ans) "Correct — use StudyPilot AI to learn why." else "Review with StudyPilot AI to understand the mistake.",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
    AiQuestionTools(question=q.q,options=q.opts,correctAnswer=q.opts[q.ans],selectedAnswer=q.opts[pick],appLanguage=appLanguage,megaTest=false,revealUses=revealUses,onReveal={revealUses++})
    Spacer(Modifier.height(8.dp));Button({at++;pick=-1},modifier=Modifier.fillMaxWidth()){AppText(if(at==size-1)"FINISH" else "NEXT QUESTION →")}
   }}
  } else {
   AiQuestionTools(question=q.q,options=q.opts,correctAnswer=q.opts[q.ans],selectedAnswer=null,appLanguage=appLanguage,megaTest=false,revealUses=revealUses,onReveal={revealUses++})
  }
 }
}

@Composable private fun BigCard(c:Color,title:String,body:String,go:()->Unit){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(27.dp)){Box(Modifier.background(Brush.linearGradient(listOf(c,c.copy(.72f))))){Column(Modifier.padding(21.dp)){AppText(title,color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Black);AppText(body,color=Color.White.copy(.86f));Spacer(Modifier.height(12.dp));Button(go,colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=c),modifier=Modifier.fillMaxWidth()){AppText("ENTER ARENA")}}}}}

@Composable private fun Library(g:Int,notes:List<Note>,tasks:List<Task>,saveNotes:(List<Note>)->Unit,saveTasks:(List<Task>)->Unit){
 var tab by remember{mutableIntStateOf(0)}
 var title by remember{mutableStateOf("")};var body by remember{mutableStateOf("")};var taskText by remember{mutableStateOf("")}
 var revealed by remember{mutableStateOf(setOf<Long>())}
 Column(Modifier.fillMaxSize().padding(16.dp)){
  AppText("Study Library",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
  AppText("Notes, tasks, flashcards, MCQ bank and focus tools — stored locally.",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=13.sp)
  Row(Modifier.horizontalScroll(rememberScrollState())){listOf("Notes","Planner","MCQ Bank","Flashcards","Focus","Tools").forEachIndexed{index,label->FilterChip(tab==index,{tab=index},label={AppText(label)},modifier=Modifier.padding(end=6.dp) )}}
  Spacer(Modifier.height(10.dp))
  if(tab==0){
   OutlinedTextField(title,{title=it},label={AppText("Note title")},modifier=Modifier.fillMaxWidth())
   Spacer(Modifier.height(7.dp));OutlinedTextField(body,{body=it},label={AppText("Study note / answer")},modifier=Modifier.fillMaxWidth().height(120.dp))
   Spacer(Modifier.height(7.dp));Button({if(title.isNotBlank()){saveNotes(notes+Note(System.currentTimeMillis(),title,body));title="";body=""}},modifier=Modifier.fillMaxWidth()){AppText("SAVE NOTE")}
   Spacer(Modifier.height(10.dp))
   LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){items(notes.reversed(),key={it.id}){n->Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(15.dp)){AppText(n.title,fontWeight=FontWeight.Black);AppText(n.body,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);AppText("DELETE",Modifier.clickable{saveNotes(notes.filterNot{it.id==n.id})}.padding(top=7.dp),fontSize=11.sp,color=palette[4],fontWeight=FontWeight.Bold)}}}}
  }else if(tab==1){
   OutlinedTextField(taskText,{taskText=it},label={AppText("New study task")},modifier=Modifier.fillMaxWidth())
   Spacer(Modifier.height(7.dp));Button({if(taskText.isNotBlank()){saveTasks(tasks+Task(System.currentTimeMillis(),taskText,false));taskText=""}},modifier=Modifier.fillMaxWidth()){AppText("ADD TASK")}
   Spacer(Modifier.height(10.dp))
   LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){items(tasks,key={it.id}){t->Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically){Checkbox(t.done,{v->saveTasks(tasks.map{if(it.id==t.id)it.copy(done=v)else it})});AppText(t.text,Modifier.weight(1f),fontWeight=FontWeight.Bold);AppText("✕",Modifier.clickable{saveTasks(tasks.filterNot{it.id==t.id})})}}}}
  }else if(tab==2){
   QuestionBank(g,Modifier.weight(1f))
  }else if(tab==3){
   if(notes.isEmpty())AppText("Create a note first, then it will appear here as a revision flashcard.",color=MaterialTheme.colorScheme.onSurfaceVariant)
   LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){items(notes.reversed(),key={it.id}){n->Card(Modifier.fillMaxWidth().clickable{revealed=if(n.id in revealed)revealed-n.id else revealed+n.id},shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(18.dp)){AppText(if(n.id in revealed)"ANSWER" else "QUESTION",fontSize=11.sp,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary);Spacer(Modifier.height(5.dp));AppText(n.title,fontWeight=FontWeight.Black,fontSize=17.sp);if(n.id in revealed){Spacer(Modifier.height(8.dp));AppText(n.body.ifBlank{"No answer saved yet."},color=MaterialTheme.colorScheme.onSurfaceVariant)}else{Spacer(Modifier.height(8.dp));AppText("Tap to reveal answer",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}}
  }else if(tab==4){
   FocusTimer()
  }else{
   QuickTools()
  }
 }
}

@Composable private fun QuestionBank(selectedGrade:Int,modifier:Modifier=Modifier){
 val context=LocalContext.current
 val prefs=remember{context.getSharedPreferences("studypilot_bank",Context.MODE_PRIVATE)}
 var g by remember(selectedGrade){mutableIntStateOf(if(selectedGrade in 1..9)selectedGrade else 1)}
 var subject by remember{mutableStateOf("Mathematics")}
 var difficulty by remember{mutableStateOf("Easy")}
 var page by remember{mutableIntStateOf(0)}
 var query by remember{mutableStateOf("")}
 var savedOnly by remember{mutableStateOf(false)}
 var bookmarks by remember{mutableStateOf(prefs.getStringSet("bookmarks",emptySet())?.toSet()?:emptySet())}
 val all=remember(g,subject,difficulty){emptyList<M>()}
 val filtered=remember(all,query,savedOnly,bookmarks){all.filter{q->(query.isBlank()||q.q.contains(query,true)||q.opts.any{it.contains(query,true)}||q.exp.contains(query,true))&&(!savedOnly||q.id.toString() in bookmarks)}}
 val pageCount=maxOf(1,(filtered.size+24)/25)
 val questions=remember(filtered,page){filtered.drop(page*25).take(25)}
 Column(modifier.fillMaxSize()){
  AppText("MCQ Question Bank",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black)
  AppText("AI quiz generation is available in Quiz Arena. This local archive only contains previously saved items.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
  Spacer(Modifier.height(7.dp))
  Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){(1..9).forEach{n->FilterChip(g==n,{g=n;page=0},label={AppText(if(n==9)"9+" else "$n")})}}
  Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){subs.forEach{(name,_)->FilterChip(subject==name,{subject=name;page=0},label={AppText(name)})}}
  Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf("Easy","Medium","Hard").forEach{d->FilterChip(difficulty==d,{difficulty=d;page=0},label={AppText(d)})};FilterChip(savedOnly,{savedOnly=!savedOnly;page=0},label={AppText("★ Bookmarks")})}
  OutlinedTextField(query,{query=it;page=0},label={AppText("Search questions, options or explanations")},singleLine=true,modifier=Modifier.fillMaxWidth())
  AppText("Showing ${if(filtered.isEmpty())0 else page*25+1}–${minOf((page+1)*25,filtered.size)} of ${filtered.size} matches • Page ${page+1}/$pageCount",fontSize=11.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(vertical=5.dp))
  LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
   items(questions,key={it.id}){q->var revealBankAnswer by remember(q.id){mutableStateOf(false)};Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(13.dp)){
    Row(verticalAlignment=Alignment.CenterVertically){AppText("Class ${q.grade} • ${q.subject} • ${q.diff} • ${q.topic}",fontSize=10.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary,modifier=Modifier.weight(1f));AppText(if(q.id.toString() in bookmarks)"★ Saved" else "☆ Save",Modifier.clickable{val next=bookmarks.toMutableSet();if(q.id.toString() in next)next.remove(q.id.toString())else next.add(q.id.toString());bookmarks=next;prefs.edit().putStringSet("bookmarks",next).apply()}.padding(4.dp),fontSize=12.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)}
    Spacer(Modifier.height(5.dp));AppText(q.q,fontWeight=FontWeight.Bold)
    q.opts.forEachIndexed{ix,opt->AppText("${'A'+ix}. $opt",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurface,modifier=Modifier.padding(top=3.dp))}
    Spacer(Modifier.height(5.dp));OutlinedButton(onClick={revealBankAnswer=!revealBankAnswer}){AppText(if(revealBankAnswer)"Hide Correct Answer" else "Show Correct Answer")};if(revealBankAnswer)AppText("Correct answer: ${q.opts[q.ans]}",fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary,fontSize=13.sp)
    if(revealBankAnswer)AppText(q.exp,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=3.dp))
   }}}
  }
  Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.padding(top=5.dp)){
   OutlinedButton(onClick={page=(page-1).coerceAtLeast(0)},enabled=page>0){AppText("← Previous")}
   Spacer(Modifier.weight(1f));AppText("${page+1}/$pageCount",fontWeight=FontWeight.Bold)
   Spacer(Modifier.weight(1f));Button(onClick={page=(page+1).coerceAtMost(pageCount-1)},enabled=page<pageCount-1){AppText("Next →")}
  }
 }
}

@Composable private fun FocusTimer(){
 var minutes by remember{mutableIntStateOf(25)}
 var remaining by remember{mutableIntStateOf(25*60)}
 var running by remember{mutableStateOf(false)}
 LaunchedEffect(running){while(running&&remaining>0){delay(1000);if(running)remaining=(remaining-1).coerceAtLeast(0)};if(remaining==0)running=false}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally){
  AppText("Focus Studio",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black)
  AppText("Offline Pomodoro timer • take a short break between sessions",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=TextAlign.Center)
  Spacer(Modifier.height(18.dp))
  Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf(15,25,45).forEach{m->FilterChip(minutes==m,{minutes=m;remaining=m*60;running=false},label={AppText("$m min")})}}
  Spacer(Modifier.height(18.dp))
  Card(shape=RoundedCornerShape(30.dp)){Column(Modifier.padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally){AppText("FOCUS TIME",fontSize=12.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary);AppText("${remaining/60}:${(remaining%60).toString().padStart(2,'0')}",fontSize=48.sp,fontWeight=FontWeight.Black);AppText(if(remaining==0)"Session complete 🎉" else if(running)"Stay focused" else "Ready when you are",color=MaterialTheme.colorScheme.onSurfaceVariant)}}
  Spacer(Modifier.height(16.dp))
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({if(remaining==0)remaining=minutes*60;running=!running}){AppText(if(running)"Pause" else "Start")};OutlinedButton({running=false;remaining=minutes*60}){AppText("Reset")}}
 }
}

@Composable private fun QuickTools(){
 var tableNumber by remember{mutableStateOf("7")}
 var left by remember{mutableStateOf("12")}
 var right by remember{mutableStateOf("4")}
 var operation by remember{mutableStateOf("+")}
 val table=tableNumber.toIntOrNull()
 val result=remember(left,right,operation){val a=left.toDoubleOrNull();val b=right.toDoubleOrNull();if(a==null||b==null)"Enter valid numbers" else when(operation){"+"->(a+b).toString();"-"->(a-b).toString();"×"->(a*b).toString();else->if(b==0.0)"Cannot divide by zero" else (a/b).toString()}}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
  var numberForMath by remember{mutableStateOf("12")}
   val mathN=numberForMath.toLongOrNull()
   AppText("Quick Study Tools",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black)
   Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(14.dp)){
    AppText("Number Explorer",fontWeight=FontWeight.Black)
    OutlinedTextField(numberForMath,{numberForMath=it.filter{ch->ch.isDigit()||ch==45.toChar()}.take(12)},label={AppText("Enter an integer")},singleLine=true,modifier=Modifier.fillMaxWidth())
    if(mathN!=null && mathN!=0L && kotlin.math.abs(mathN)<=1000000000L){AppText("Square: "+(mathN*mathN),fontWeight=FontWeight.SemiBold);AppText("Cube: "+(mathN*mathN*mathN),fontWeight=FontWeight.SemiBold);val factors=(1L..kotlin.math.abs(mathN).coerceAtMost(100000L)).filter{mathN%it==0L};AppText("Positive factors: "+factors.joinToString(", "),fontSize=12.sp);AppText("Prime: "+(kotlin.math.abs(mathN)>1 && (2L..kotlin.math.sqrt(kotlin.math.abs(mathN).toDouble()).toLong()).none{mathN%it==0L}),fontSize=12.sp)} else if(mathN==0L) AppText("Zero has no finite factor list.",fontSize=12.sp)
   }}
   Spacer(Modifier.height(10.dp))
  AppText("Offline calculator, multiplication tables and formula cards.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
  Spacer(Modifier.height(10.dp))
  Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(14.dp)){
   AppText("Multiplication Table",fontWeight=FontWeight.Black)
   OutlinedTextField(tableNumber,{tableNumber=it.filter{ch->ch.isDigit()}.take(8)},label={AppText("Enter any whole number")},singleLine=true,modifier=Modifier.fillMaxWidth())
   if(table!=null){(1..10).forEach{n->AppText("$table × $n = ${table*n}",fontWeight=FontWeight.Medium,modifier=Modifier.padding(vertical=2.dp))}}
  }}
  Spacer(Modifier.height(10.dp))
  Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(14.dp)){
   AppText("Basic Calculator",fontWeight=FontWeight.Black)
   Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){OutlinedTextField(left,{left=it},label={AppText("Number A")},modifier=Modifier.weight(1f),singleLine=true);OutlinedTextField(right,{right=it},label={AppText("Number B")},modifier=Modifier.weight(1f),singleLine=true)}
   Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("+","-","×","÷").forEach{op->FilterChip(operation==op,{operation=op},label={AppText(op)})}}
   AppText("Result: $result",fontSize=20.sp,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary)
  }}
  Spacer(Modifier.height(10.dp))
  AppText("Formula Cards",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black)
  listOf("Rectangle area" to "A = length × width","Rectangle perimeter" to "P = 2 × (length + width)","Speed" to "speed = distance ÷ time","Density" to "density = mass ÷ volume","Ohm’s law" to "voltage = current × resistance","Circle circumference" to "C = 2 × π × radius","Average" to "average = total ÷ number of values").forEach{(name,formula)->Card(Modifier.fillMaxWidth().padding(vertical=3.dp),shape=RoundedCornerShape(15.dp)){Column(Modifier.padding(12.dp)){AppText(name,fontWeight=FontWeight.Bold);AppText(formula,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
 }
}

@Composable private fun Profile(g:Int,xp:Int,ok:Int,tried:Int,studentName:String,setStudentName:(String)->Unit,dark:Boolean,setDark:(Boolean)->Unit,language:String,setLanguage:(String)->Unit,change:()->Unit){val acc=if(tried==0)0 else ok*100/tried;Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){AppText("Profile & Stats",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black);OutlinedTextField(value=studentName,onValueChange=setStudentName,label={AppText("Student name")},singleLine=true,modifier=Modifier.fillMaxWidth());AppText("Class $g • Your name and learning settings personalize AI-generated questions.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);LanguagePicker(language,setLanguage);BigCard(palette[1],"LEVEL ${xp/500+1}","$xp XP • $acc% accuracy",{});Spacer(Modifier.height(10.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Stat("Attempted",tried.toString(),palette[0],Modifier.weight(1f));Stat("Correct",ok.toString(),palette[2],Modifier.weight(1f))};Spacer(Modifier.height(10.dp));Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){AppText("Dark theme",modifier=Modifier.weight(1f),fontWeight=FontWeight.Bold);Switch(dark,setDark)}};Spacer(Modifier.height(8.dp));OutlinedButton(change,modifier=Modifier.fillMaxWidth()){AppText("CHANGE CLASS")}}
}


private fun generateAiQuiz(maxGrade:Int,fromGrade:Int,subject:String,difficulty:String,count:Int,studentName:String,language:String,context:Context):List<M>{
    if(!AiConfig.isConfigured()) throw IllegalStateException("StudyPilot Supabase AI backend is not configured. Please retry after backend deployment is complete.")
    val grade=if(maxGrade in 1..9) fromGrade.coerceIn(1,maxGrade) else fromGrade.coerceIn(1,9)
    val all=ArrayList<M>()
    val seen=HashSet<String>()
    var remaining=count
    while(remaining>0){
        val batch=minOf(10,remaining)
        val beforeBatch=all.size
        val body=JSONObject().apply{
            put("grade",grade);put("subject",subject);put("difficulty",difficulty);put("language",language)
            put("count",batch);put("studentName",studentName.take(80))
            put("learningLevel","Adapt to class $grade and the student's demonstrated understanding")
            put("excludeQuestions",JSONArray(all.map{it.q}))
        }
        val connection=(java.net.URL(AiConfig.SUPABASE_URL.trimEnd()+"/functions/v1/study-generate-mcqs").openConnection() as java.net.HttpURLConnection).apply{
            requestMethod="POST";connectTimeout=20000;readTimeout=65000;doOutput=true
            setRequestProperty("Content-Type","application/json; charset=utf-8")
            setRequestProperty("Accept","application/json")
            setRequestProperty("apikey",AiConfig.SUPABASE_PUBLISHABLE_KEY)
            if(StudentAuth.token(context).isNotBlank())setRequestProperty("Authorization","Bearer "+StudentAuth.token(context))
        }
        try{
            connection.outputStream.use{it.write(body.toString().toByteArray(Charsets.UTF_8))}
            val code=connection.responseCode
            val stream=if(code in 200..299)connection.inputStream else connection.errorStream
            val raw=stream?.bufferedReader(Charsets.UTF_8)?.use{it.readText()}.orEmpty()
            val json=try{JSONObject(raw.ifBlank{"{}"})}catch(_:Exception){val html=raw.trimStart().startsWith("<")||connection.contentType?.contains("html",true)==true;if(html)throw IllegalStateException("Supabase AI backend is not deployed or returned an HTML error (HTTP $code). Retry after backend deployment.");throw IllegalStateException("AI server returned an unreadable response (HTTP $code). Please retry.")}
            if(code !in 200..299)throw IllegalStateException(json.optString("error","AI service returned HTTP $code"))
            val arr=json.optJSONArray("questions")?:throw IllegalStateException("AI returned no questions.")
            for(i in 0 until arr.length()){
                val item=arr.optJSONObject(i)?:continue
                val q=item.optString("question").trim()
                val opts=item.optJSONArray("options")?:continue
                if(q.isBlank()||opts.length()!=4)continue
                val options=(0 until 4).map{opts.optString(it)}
                val answer=item.optInt("answerIndex",-1)
                if(answer !in 0..3||options.any{it.isBlank()}||!seen.add(q.lowercase()))continue
                all.add(M(all.size,grade,subject,item.optString("difficulty",difficulty),item.optString("topic",subject),q,options,answer,item.optString("explanation")))
            }
        }finally{connection.disconnect()}
        val added=all.size-beforeBatch
        if(added<=0)throw IllegalStateException("StudyPilot AI could not produce another unique batch. Please retry the remaining questions.")
        remaining-=added
    }
    return all
}

private fun loadNotes(p:android.content.SharedPreferences):List<Note>{return try{val a=JSONArray(p.getString("notes","[]"));List(a.length()){val o=a.getJSONObject(it);Note(o.getLong("id"),o.getString("title"),o.getString("body"))}}catch(_:Exception){emptyList()}}
private fun loadTasks(p:android.content.SharedPreferences):List<Task>{return try{val a=JSONArray(p.getString("tasks","[]"));List(a.length()){val o=a.getJSONObject(it);Task(o.getLong("id"),o.getString("text"),o.getBoolean("done"))}}catch(_:Exception){emptyList()}}
private fun notesJson(v:List<Note>)=JSONArray().apply{v.forEach{put(JSONObject().apply{put("id",it.id);put("title",it.title);put("body",it.body)})}}.toString()
private fun tasksJson(v:List<Task>)=JSONArray().apply{v.forEach{put(JSONObject().apply{put("id",it.id);put("text",it.text);put("done",it.done)})}}.toString()

