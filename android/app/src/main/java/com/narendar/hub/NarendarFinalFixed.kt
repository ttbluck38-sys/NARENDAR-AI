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
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

private data class M(val id:Int,val grade:Int,val subject:String,val diff:String,val topic:String,val q:String,val opts:List<String>,val ans:Int,val exp:String)
private data class Note(val id:Long,val title:String,val body:String)
private data class Task(val id:Long,val text:String,val done:Boolean)
private val subs=listOf("Mathematics" to "∑","Science" to "⚗","English" to "A","Computer Science" to "⌘","SST" to "◎","Urdu" to "ا","Physics" to "Φ","Chemistry" to "⚛","Biology" to "♧")
private val palette=listOf(Color(0xFF315EFB),Color(0xFF7C4DFF),Color(0xFF00A884),Color(0xFFFF8A3D),Color(0xFFE83E8C))

@Composable fun FixedHub(context:Context){
 val p=remember{context.getSharedPreferences("narendar_fixed",Context.MODE_PRIVATE)}
 var grade by remember{mutableIntStateOf(p.getInt("grade",-1))};var page by remember{mutableStateOf(if(grade<0)"pick" else "home")};var dark by remember{mutableStateOf(p.getBoolean("dark",false))}
 var xp by remember{mutableIntStateOf(p.getInt("xp",0))};var correct by remember{mutableIntStateOf(p.getInt("correct",0))};var tried by remember{mutableIntStateOf(p.getInt("tried",0))};var notes by remember{mutableStateOf(loadNotes(p))};var tasks by remember{mutableStateOf(loadTasks(p))};var toast by remember{mutableStateOf("")}
 fun record(ok:Boolean){tried++;if(ok)correct++;xp+=if(ok)20 else 5;p.edit().putInt("xp",xp).putInt("correct",correct).putInt("tried",tried).apply()}
 fun saveNotes(v:List<Note>){notes=v;p.edit().putString("notes",notesJson(v)).apply()}
 fun saveTasks(v:List<Task>){tasks=v;p.edit().putString("tasks",tasksJson(v)).apply()}
 LaunchedEffect(toast){if(toast.isNotEmpty()){delay(1500);toast=""}}
 MaterialTheme(colorScheme=if(dark)darkColorScheme(primary=Color(0xFF9AB8FF),secondary=Color(0xFF70E0BE),tertiary=Color(0xFFFF9DC7))else lightColorScheme(primary=palette[0],secondary=palette[2],tertiary=palette[4],surface=Color(0xFFF7F8FC))){
  if(page=="pick"){Pick(grade){grade=it;p.edit().putInt("grade",it).apply();page="home"};return@MaterialTheme}
  Scaffold(topBar={CenterAlignedTopAppBar(title={Text("StudyPilot AI",fontWeight=FontWeight.Black)})},bottomBar={NavigationBar{listOf("home" to "⌂","learn" to "▣","quiz" to "✓","library" to "▤","profile" to "●").forEach{(k,ic)->NavigationBarItem(page==k,{page=k},icon={Text(ic)},label={Text(k.replaceFirstChar{it.uppercase()})})}}}){pad->Box(Modifier.fillMaxSize().padding(pad)){AnimatedContent(page,label="page"){s->when(s){"home"->Home(grade,xp,correct,tried,tasks.count{!it.done},notes.size){page=it};"learn"->Learn(grade){page="quiz"};"quiz"->Quiz(grade,::record){toast=it};"library"->Library(grade,notes,tasks,::saveNotes,::saveTasks);"profile"->Profile(grade,xp,correct,tried,dark,{dark=it;p.edit().putBoolean("dark",it).apply()}){grade=-1;p.edit().putInt("grade",-1).apply();page="pick"}}};AnimatedVisibility(toast.isNotEmpty(),enter=slideInVertically{it}+fadeIn(),exit=fadeOut(),modifier=Modifier.align(Alignment.BottomCenter).padding(8.dp)){Surface(shape=RoundedCornerShape(18.dp),tonalElevation=8.dp){Text(toast,Modifier.padding(14.dp),fontWeight=FontWeight.Bold)}}}}
 }
}

@Composable private fun Pick(g:Int,choose:(Int)->Unit){val pulse=rememberInfiniteTransition(label="p").animateFloat(1f,1.06f,infiniteRepeatable(tween(1100),RepeatMode.Reverse),label="p").value;Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF111827),Color(0xFF312E81),Color(0xFF0F766E)))).verticalScroll(rememberScrollState())){Column(Modifier.padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally){Spacer(Modifier.height(25.dp));Surface(shape=CircleShape,color=Color.White.copy(.13f),modifier=Modifier.size(100.dp).scale(pulse)){Box(contentAlignment=Alignment.Center){Text("N",color=Color.White,fontSize=48.sp,fontWeight=FontWeight.Black)}};Text("STUDYPILOT AI",color=Color.White,fontSize=29.sp,fontWeight=FontWeight.Black);Text("Learn • Practice • Master",color=Color.White.copy(.7f));Spacer(Modifier.height(22.dp));Text("Choose class",color=Color.White,fontSize=21.sp,fontWeight=FontWeight.Black);Text("Class 8 includes Class 1–8 learning.",color=Color.White.copy(.65f));Spacer(Modifier.height(10.dp));listOf(-1 to "🌎 All Classes",1 to "🎒 Class 1",2 to "🎒 Class 2",3 to "🎒 Class 3",4 to "🎒 Class 4",5 to "🎒 Class 5",6 to "🎒 Class 6",7 to "🎒 Class 7",8 to "🚀 Class 8",9 to "🏆 Class 9+").forEach{(v,t)->Card(onClick={choose(v)},modifier=Modifier.fillMaxWidth().padding(4.dp),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White.copy(.11f))){Row(Modifier.padding(16.dp)){Text(t,color=Color.White,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Text(if(g==v)"✓" else "›",color=Color.White,fontSize=22.sp)}}};Text("Generated practice set • curriculum review pending",color=Color.White.copy(.55f),fontSize=12.sp,modifier=Modifier.padding(top=10.dp))}}}

@Composable private fun Home(g:Int,xp:Int,ok:Int,tried:Int,tasks:Int,notes:Int,nav:(String)->Unit){val acc=if(tried==0)0 else ok*100/tried;val pulse=rememberInfiniteTransition(label="hero").animateFloat(.97f,1f,infiniteRepeatable(tween(1400),RepeatMode.Reverse),label="hero").value;LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Text("Welcome back 👋",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black);Text(if(g<0)"All Classes" else "Class $g",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)};item{Card(Modifier.fillMaxWidth().scale(pulse),shape=RoundedCornerShape(30.dp)){Box(Modifier.background(Brush.linearGradient(listOf(palette[(xp/100)%5],palette[(xp/100+2)%5])))){Column(Modifier.padding(22.dp)){Text("LEVEL ${xp/500+1} • $xp XP",color=Color.White.copy(.8f),fontWeight=FontWeight.Bold);Text("Your learning journey",color=Color.White,fontSize=27.sp,fontWeight=FontWeight.Black);Spacer(Modifier.height(12.dp));LinearProgressIndicator({(xp%500)/500f},Modifier.fillMaxWidth().height(9.dp),color=Color.White,trackColor=Color.White.copy(.2f));Spacer(Modifier.height(9.dp));Text("🎯 $acc% accuracy   📚 $tried attempts",color=Color.White,fontWeight=FontWeight.Bold)}}}};item{Text("Explore",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black)};item{Action("🎮","Quiz Arena","Practice MCQs • Easy / Medium / Hard",palette[0]){nav("quiz")}};item{Action("📚","Study Library","Subjects + explanations",palette[1]){nav("learn")}};item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Stat("Questions",tried.toString(),palette[2],Modifier.weight(1f));Stat("Notes",notes.toString(),palette[3],Modifier.weight(1f));Stat("Tasks",tasks.toString(),palette[4],Modifier.weight(1f))}};item{Action("🃏","Revision Hub","Notes + planner",palette[2]){nav("library")}};item{Action("🏆","Stats + Settings","XP + theme",palette[3]){nav("profile")}}}}
@Composable private fun Action(ic:String,title:String,sub:String,c:Color,go:()->Unit){Card(Modifier.fillMaxWidth().clickable{go()},shape=RoundedCornerShape(22.dp)){Row(Modifier.padding(17.dp),verticalAlignment=Alignment.CenterVertically){Surface(shape=CircleShape,color=c.copy(.13f),modifier=Modifier.size(50.dp)){Box(contentAlignment=Alignment.Center){Text(ic,fontSize=25.sp)}};Spacer(Modifier.width(13.dp));Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.Black);Text(sub,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text("›",fontSize=28.sp,color=c)}}}
@Composable private fun Stat(n:String,v:String,c:Color,m:Modifier){Card(m,shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(13.dp)){Text(v,fontSize=22.sp,fontWeight=FontWeight.Black,color=c);Text(n,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}

@Composable private fun Learn(g:Int,start:()->Unit){var sub by remember{mutableStateOf("Mathematics")};val list=if(g<0||g>=9)subs else subs.take(6);Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){Text("Study Library",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black);Text("Study → revise → practice → test",color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(12.dp));list.forEachIndexed{ix,(name,icon)->Card(onClick={sub=name},modifier=Modifier.fillMaxWidth().padding(vertical=4.dp),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=if(sub==name)palette[ix%5].copy(.15f)else MaterialTheme.colorScheme.surfaceVariant)){Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically){Text(icon,fontSize=23.sp);Spacer(Modifier.width(12.dp));Text(name,Modifier.weight(1f),fontWeight=FontWeight.Black);Text(if(sub==name)"✓" else "›",color=palette[ix%5])}}};Spacer(Modifier.height(12.dp));Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(25.dp)){Column(Modifier.padding(20.dp)){Text("📖 $sub",fontSize=23.sp,fontWeight=FontWeight.Black);Text("Class ${if(g<0)"1–9+" else "1–$g"} • Generated practice set • curriculum review pending per difficulty",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(8.dp));Text("MCQ Study Library",fontWeight=FontWeight.Bold);Text("Every test question has a unique ID and an explanation.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(12.dp));Button(start,modifier=Modifier.fillMaxWidth()){Text("START PRACTICE")}}}}}

@Composable private fun Quiz(g:Int,record:(Boolean)->Unit,toast:(String)->Unit){
 var sub by remember{mutableStateOf("Mathematics")}
 var diff by remember{mutableStateOf("Mixed")}
 var size by remember{mutableIntStateOf(10)}
 var start by remember{mutableStateOf(false)}
 var at by remember{mutableIntStateOf(0)}
 var pick by remember{mutableIntStateOf(-1)}
 var score by remember{mutableIntStateOf(0)}
 var seed by remember{mutableIntStateOf(0)}
 val maxG=if(g<0)9 else g
 val qs=remember(sub,diff,size,seed,g){test(maxG,sub,diff,size,seed)}
 if(!start){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){
  Text("Quiz Arena",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
  Text("Practice questions with explanations and read-aloud support.",color=MaterialTheme.colorScheme.onSurfaceVariant)
  Spacer(Modifier.height(12.dp));Text("Subject",fontWeight=FontWeight.Black)
  Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){subs.forEach{(n,_)->FilterChip(sub==n,{sub=n},label={Text(n)})}}
  Spacer(Modifier.height(10.dp));Row{listOf("Mixed","Easy","Medium","Hard").forEach{FilterChip(diff==it,{diff=it},label={Text(it)},modifier=Modifier.padding(end=4.dp))}}
  Spacer(Modifier.height(10.dp));Row{listOf(10,20,50,100).forEach{FilterChip(size==it,{size=it},label={Text("$it")},modifier=Modifier.padding(end=4.dp))}}
  Spacer(Modifier.height(16.dp));BigCard(palette[0],"🎮 READY","Practice MCQs • AI explanation • speaker support"){start=true}
 };return}
 if(at>=qs.size){Column(Modifier.fillMaxSize().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){
  Spacer(Modifier.height(40.dp));Text("🏆",fontSize=70.sp);Text("Test Complete",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black)
  Text("$score / $size",fontSize=34.sp,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary)
  Text("Accuracy ${score*100/size}%",color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(18.dp))
  Button({seed++;at=0;pick=-1;score=0}){Text("NEW UNIQUE TEST")}
 };return}
 val q=qs[at]
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){
  Row{Text("${at+1}/$size",fontWeight=FontWeight.Black);Spacer(Modifier.weight(1f));Text("+20 XP",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)}
  LinearProgressIndicator({(at+if(pick>=0)1 else 0).toFloat()/size},Modifier.fillMaxWidth().height(8.dp));Spacer(Modifier.height(10.dp))
  Text("Class ${q.grade} • ${q.subject} • ${q.diff} • ${q.topic}",color=MaterialTheme.colorScheme.primary,fontSize=12.sp,fontWeight=FontWeight.Bold)
  Card(Modifier.fillMaxWidth().padding(vertical=10.dp),shape=RoundedCornerShape(25.dp)){Text(q.q,Modifier.padding(21.dp),fontSize=19.sp,fontWeight=FontWeight.Black)}
  q.opts.forEachIndexed{i,o->
   val good=pick>=0&&i==q.ans;val bad=pick==i&&i!=q.ans
   Card(onClick={if(pick<0){pick=i;record(i==q.ans);if(i==q.ans)score++;toast(if(i==q.ans)"Correct! 🎉" else "Review explanation 📘")}},modifier=Modifier.fillMaxWidth().padding(vertical=4.dp),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=if(good)Color(0xFF00A884).copy(.16f)else if(bad)Color(0xFFE83E8C).copy(.16f)else MaterialTheme.colorScheme.surfaceVariant)){
    Row(Modifier.padding(15.dp)){Text("${'A'+i}",fontWeight=FontWeight.Black,color=if(good)Color(0xFF00A884)else MaterialTheme.colorScheme.primary);Spacer(Modifier.width(12.dp));Text(o,fontWeight=FontWeight.SemiBold)}
   }
  }
  if(pick>=0){
   Card(Modifier.fillMaxWidth().padding(top=8.dp),shape=RoundedCornerShape(19.dp)){Column(Modifier.padding(15.dp)){
    Text(if(pick==q.ans)"✓ Correct" else "✕ Review",fontWeight=FontWeight.Black)
    Text(q.exp,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
    AiQuestionTools(question=q.q,options=q.opts,correctAnswer=q.opts[q.ans],selectedAnswer=q.opts[pick])
    Spacer(Modifier.height(8.dp));Button({at++;pick=-1},modifier=Modifier.fillMaxWidth()){Text(if(at==size-1)"FINISH" else "NEXT QUESTION →")}
   }}
  } else {
   AiQuestionTools(question=q.q,options=q.opts,correctAnswer=q.opts[q.ans],selectedAnswer=null)
  }
 }
}

@Composable private fun BigCard(c:Color,title:String,body:String,go:()->Unit){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(27.dp)){Box(Modifier.background(Brush.linearGradient(listOf(c,c.copy(.72f))))){Column(Modifier.padding(21.dp)){Text(title,color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Black);Text(body,color=Color.White.copy(.86f));Spacer(Modifier.height(12.dp));Button(go,colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=c),modifier=Modifier.fillMaxWidth()){Text("ENTER ARENA")}}}}}

@Composable private fun Library(g:Int,notes:List<Note>,tasks:List<Task>,saveNotes:(List<Note>)->Unit,saveTasks:(List<Task>)->Unit){
 var tab by remember{mutableIntStateOf(0)}
 var title by remember{mutableStateOf("")};var body by remember{mutableStateOf("")};var taskText by remember{mutableStateOf("")}
 var revealed by remember{mutableStateOf(setOf<Long>())}
 Column(Modifier.fillMaxSize().padding(16.dp)){
  Text("Study Library",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
  Text("Notes, tasks, flashcards, MCQ bank and focus tools — stored locally.",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=13.sp)
  Row(Modifier.horizontalScroll(rememberScrollState())){listOf("Notes","Planner","MCQ Bank","Flashcards","Focus","Tools").forEachIndexed{index,label->FilterChip(tab==index,{tab=index},label={Text(label)},modifier=Modifier.padding(end=6.dp) )}}
  Spacer(Modifier.height(10.dp))
  if(tab==0){
   OutlinedTextField(title,{title=it},label={Text("Note title")},modifier=Modifier.fillMaxWidth())
   Spacer(Modifier.height(7.dp));OutlinedTextField(body,{body=it},label={Text("Study note / answer")},modifier=Modifier.fillMaxWidth().height(120.dp))
   Spacer(Modifier.height(7.dp));Button({if(title.isNotBlank()){saveNotes(notes+Note(System.currentTimeMillis(),title,body));title="";body=""}},modifier=Modifier.fillMaxWidth()){Text("SAVE NOTE")}
   Spacer(Modifier.height(10.dp))
   LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){items(notes.reversed(),key={it.id}){n->Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(15.dp)){Text(n.title,fontWeight=FontWeight.Black);Text(n.body,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text("DELETE",Modifier.clickable{saveNotes(notes.filterNot{it.id==n.id})}.padding(top=7.dp),fontSize=11.sp,color=palette[4],fontWeight=FontWeight.Bold)}}}}
  }else if(tab==1){
   OutlinedTextField(taskText,{taskText=it},label={Text("New study task")},modifier=Modifier.fillMaxWidth())
   Spacer(Modifier.height(7.dp));Button({if(taskText.isNotBlank()){saveTasks(tasks+Task(System.currentTimeMillis(),taskText,false));taskText=""}},modifier=Modifier.fillMaxWidth()){Text("ADD TASK")}
   Spacer(Modifier.height(10.dp))
   LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){items(tasks,key={it.id}){t->Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically){Checkbox(t.done,{v->saveTasks(tasks.map{if(it.id==t.id)it.copy(done=v)else it})});Text(t.text,Modifier.weight(1f),fontWeight=FontWeight.Bold);Text("✕",Modifier.clickable{saveTasks(tasks.filterNot{it.id==t.id})})}}}}
  }else if(tab==2){
   QuestionBank(g,Modifier.weight(1f))
  }else if(tab==3){
   if(notes.isEmpty())Text("Create a note first, then it will appear here as a revision flashcard.",color=MaterialTheme.colorScheme.onSurfaceVariant)
   LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){items(notes.reversed(),key={it.id}){n->Card(Modifier.fillMaxWidth().clickable{revealed=if(n.id in revealed)revealed-n.id else revealed+n.id},shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(18.dp)){Text(if(n.id in revealed)"ANSWER" else "QUESTION",fontSize=11.sp,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary);Spacer(Modifier.height(5.dp));Text(n.title,fontWeight=FontWeight.Black,fontSize=17.sp);if(n.id in revealed){Spacer(Modifier.height(8.dp));Text(n.body.ifBlank{"No answer saved yet."},color=MaterialTheme.colorScheme.onSurfaceVariant)}else{Spacer(Modifier.height(8.dp));Text("Tap to reveal answer",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}}
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
 val all=remember(g,subject,difficulty){bank(g,subject,difficulty,2500)}
 val filtered=remember(all,query,savedOnly,bookmarks){all.filter{q->(query.isBlank()||q.q.contains(query,true)||q.opts.any{it.contains(query,true)}||q.exp.contains(query,true))&&(!savedOnly||q.id.toString() in bookmarks)}}
 val pageCount=maxOf(1,(filtered.size+24)/25)
 val questions=remember(filtered,page){filtered.drop(page*25).take(25)}
 Column(modifier.fillMaxSize()){
  Text("MCQ Question Bank",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black)
  Text("2,500 generated items per class × subject × difficulty. Curriculum review is still needed.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
  Spacer(Modifier.height(7.dp))
  Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){(1..9).forEach{n->FilterChip(g==n,{g=n;page=0},label={Text(if(n==9)"9+" else "$n")})}}
  Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){subs.forEach{(name,_)->FilterChip(subject==name,{subject=name;page=0},label={Text(name)})}}
  Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf("Easy","Medium","Hard").forEach{d->FilterChip(difficulty==d,{difficulty=d;page=0},label={Text(d)})};FilterChip(savedOnly,{savedOnly=!savedOnly;page=0},label={Text("★ Bookmarks")})}
  OutlinedTextField(query,{query=it;page=0},label={Text("Search questions, options or explanations")},singleLine=true,modifier=Modifier.fillMaxWidth())
  Text("Showing ${if(filtered.isEmpty())0 else page*25+1}–${minOf((page+1)*25,filtered.size)} of ${filtered.size} matches • Page ${page+1}/$pageCount",fontSize=11.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(vertical=5.dp))
  LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
   items(questions,key={it.id}){q->Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(13.dp)){
    Row(verticalAlignment=Alignment.CenterVertically){Text("Class ${q.grade} • ${q.subject} • ${q.diff} • ${q.topic}",fontSize=10.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary,modifier=Modifier.weight(1f));Text(if(q.id.toString() in bookmarks)"★ Saved" else "☆ Save",Modifier.clickable{val next=bookmarks.toMutableSet();if(q.id.toString() in next)next.remove(q.id.toString())else next.add(q.id.toString());bookmarks=next;prefs.edit().putStringSet("bookmarks",next).apply()}.padding(4.dp),fontSize=12.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)}
    Spacer(Modifier.height(5.dp));Text(q.q,fontWeight=FontWeight.Bold)
    q.opts.forEachIndexed{ix,opt->Text("${'A'+ix}. $opt",fontSize=13.sp,color=if(ix==q.ans)Color(0xFF008A68)else MaterialTheme.colorScheme.onSurface,modifier=Modifier.padding(top=3.dp))}
    Spacer(Modifier.height(5.dp));Text("Answer: ${q.opts[q.ans]}",fontWeight=FontWeight.Black,color=Color(0xFF008A68),fontSize=13.sp)
    Text(q.exp,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=3.dp))
   }}}
  }
  Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.padding(top=5.dp)){
   OutlinedButton(onClick={page=(page-1).coerceAtLeast(0)},enabled=page>0){Text("← Previous")}
   Spacer(Modifier.weight(1f));Text("${page+1}/$pageCount",fontWeight=FontWeight.Bold)
   Spacer(Modifier.weight(1f));Button(onClick={page=(page+1).coerceAtMost(pageCount-1)},enabled=page<pageCount-1){Text("Next →")}
  }
 }
}

@Composable private fun FocusTimer(){
 var minutes by remember{mutableIntStateOf(25)}
 var remaining by remember{mutableIntStateOf(25*60)}
 var running by remember{mutableStateOf(false)}
 LaunchedEffect(running){while(running&&remaining>0){delay(1000);if(running)remaining=(remaining-1).coerceAtLeast(0)};if(remaining==0)running=false}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally){
  Text("Focus Studio",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black)
  Text("Offline Pomodoro timer • take a short break between sessions",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=TextAlign.Center)
  Spacer(Modifier.height(18.dp))
  Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf(15,25,45).forEach{m->FilterChip(minutes==m,{minutes=m;remaining=m*60;running=false},label={Text("$m min")})}}
  Spacer(Modifier.height(18.dp))
  Card(shape=RoundedCornerShape(30.dp)){Column(Modifier.padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("FOCUS TIME",fontSize=12.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary);Text("${remaining/60}:${(remaining%60).toString().padStart(2,'0')}",fontSize=48.sp,fontWeight=FontWeight.Black);Text(if(remaining==0)"Session complete 🎉" else if(running)"Stay focused" else "Ready when you are",color=MaterialTheme.colorScheme.onSurfaceVariant)}}
  Spacer(Modifier.height(16.dp))
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({if(remaining==0)remaining=minutes*60;running=!running}){Text(if(running)"Pause" else "Start")};OutlinedButton({running=false;remaining=minutes*60}){Text("Reset")}}
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
  Text("Quick Study Tools",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black)
  Text("Offline calculator, multiplication tables and formula cards.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
  Spacer(Modifier.height(10.dp))
  Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(14.dp)){
   Text("Multiplication Table",fontWeight=FontWeight.Black)
   OutlinedTextField(tableNumber,{tableNumber=it.filter{ch->ch.isDigit()}.take(8)},label={Text("Enter any whole number")},singleLine=true,modifier=Modifier.fillMaxWidth())
   if(table!=null){(1..10).forEach{n->Text("$table × $n = ${table*n}",fontWeight=FontWeight.Medium,modifier=Modifier.padding(vertical=2.dp))}}
  }}
  Spacer(Modifier.height(10.dp))
  Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(14.dp)){
   Text("Basic Calculator",fontWeight=FontWeight.Black)
   Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){OutlinedTextField(left,{left=it},label={Text("Number A")},modifier=Modifier.weight(1f),singleLine=true);OutlinedTextField(right,{right=it},label={Text("Number B")},modifier=Modifier.weight(1f),singleLine=true)}
   Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("+","-","×","÷").forEach{op->FilterChip(operation==op,{operation=op},label={Text(op)})}}
   Text("Result: $result",fontSize=20.sp,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary)
  }}
  Spacer(Modifier.height(10.dp))
  Text("Formula Cards",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black)
  listOf("Rectangle area" to "A = length × width","Rectangle perimeter" to "P = 2 × (length + width)","Speed" to "speed = distance ÷ time","Density" to "density = mass ÷ volume","Ohm’s law" to "voltage = current × resistance","Circle circumference" to "C = 2 × π × radius","Average" to "average = total ÷ number of values").forEach{(name,formula)->Card(Modifier.fillMaxWidth().padding(vertical=3.dp),shape=RoundedCornerShape(15.dp)){Column(Modifier.padding(12.dp)){Text(name,fontWeight=FontWeight.Bold);Text(formula,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
 }
}

@Composable private fun Profile(g:Int,xp:Int,ok:Int,tried:Int,dark:Boolean,setDark:(Boolean)->Unit,change:()->Unit){val acc=if(tried==0)0 else ok*100/tried;Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){Text("Profile & Stats",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black);BigCard(palette[1],"LEVEL ${xp/500+1}","$xp XP • $acc% accuracy",{});Spacer(Modifier.height(10.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Stat("Attempted",tried.toString(),palette[0],Modifier.weight(1f));Stat("Correct",ok.toString(),palette[2],Modifier.weight(1f))};Spacer(Modifier.height(10.dp));Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Text("Dark theme",modifier=Modifier.weight(1f),fontWeight=FontWeight.Bold);Switch(dark,setDark)}};Spacer(Modifier.height(8.dp));OutlinedButton(change,modifier=Modifier.fillMaxWidth()){Text("CHANGE CLASS")}}
}

private fun test(maxG:Int,s:String,d:String,size:Int,seed:Int):List<M>{
 val ds=if(d=="Mixed")listOf("Easy","Medium","Hard")else listOf(d)
 val all=ArrayList<M>()
 val poolSize=minOf(2500,maxOf(40,size*2))
 for(g in 1..maxG)for(x in ds)all.addAll(bank(g,s,x,poolSize))
 return all.shuffled(kotlin.random.Random(seed*7919+s.hashCode())).take(size).mapIndexed{index,q->shuffleOptions(q,seed*31+index+s.hashCode())}
}
private fun bank(g:Int,s:String,d:String,n:Int):List<M> = List(n){i->make(g,s,d,i)}
private data class Fact(val term:String,val meaning:String)
private fun facts(s:String):List<Fact> = when(s){
 "Science"->listOf(Fact("evaporation","a liquid changes into gas"),Fact("photosynthesis","plants use light to make food"),Fact("gravity","an attraction between objects with mass"),Fact("condensation","gas changes into liquid"),Fact("friction","a force that resists sliding motion"),Fact("ecosystem","living things interact with each other and their surroundings"),Fact("reflection","light bounces from a surface"),Fact("magnetism","a force associated with magnets and magnetic fields"),Fact("habitat","the natural place where an organism lives"),Fact("insulator","a material that slows heat or electric flow"),Fact("producer","an organism that makes its own food"),Fact("renewable resource","a resource replenished naturally over time"))
 "English"->listOf(Fact("quickly","an adverb meaning at a fast speed"),Fact("beautiful","an adjective meaning pleasing to the senses"),Fact("because","a conjunction that gives a reason"),Fact("carefully","an adverb meaning with care"),Fact("although","a conjunction that introduces contrast"),Fact("honest","an adjective meaning truthful"),Fact("between","a preposition for a position in the middle of two"),Fact("however","a word that introduces a contrast"),Fact("noun","a word naming a person, place, thing, or idea"),Fact("verb","a word expressing an action or state"),Fact("adjective","a word that describes a noun"),Fact("synonym","a word with a similar meaning"),Fact("antonym","a word with an opposite meaning"),Fact("pronoun","a word used instead of a noun"))
 "Computer Science"->listOf(Fact("CPU","the main processor that executes instructions"),Fact("RAM","temporary working memory used while programs run"),Fact("browser","software used to open and view websites"),Fact("algorithm","a sequence of steps for solving a problem"),Fact("firewall","a system that filters network traffic"),Fact("compiler","software that translates source code"),Fact("keyboard","an input device used to type"),Fact("router","a device that forwards data between networks"),Fact("database","an organized collection of data"),Fact("operating system","software that manages hardware and applications"),Fact("binary","a number system using zero and one"),Fact("backup","a separate copy kept for recovery"),Fact("phishing","a trick used to steal information by pretending to be trusted"),Fact("variable","a named place to store a value in a program"))
 "SST"->listOf(Fact("map","a representation of places or areas"),Fact("constitution","the fundamental set of laws for a state"),Fact("river","a natural stream of flowing water"),Fact("trade","the exchange of goods or services"),Fact("community","a group of people living or working together"),Fact("citizen","a legally recognized member of a country"),Fact("climate","the long-term pattern of weather in a region"),Fact("culture","shared customs, beliefs, arts, and ways of life"),Fact("compass","an instrument that helps show direction"),Fact("democracy","a system in which people participate in choosing leaders"),Fact("continent","one of Earth's major landmasses"),Fact("economy","the system of producing, sharing, and using goods and services"))
 "Urdu"->listOf(Fact("اسم","کسی شخص، جگہ، چیز یا خیال کا نام"),Fact("فعل","کام یا حالت ظاہر کرنے والا لفظ"),Fact("صفت","اسم کی خوبی یا حالت بتانے والا لفظ"),Fact("جملہ","الفاظ کا ایسا مجموعہ جو مکمل بات بتائے"),Fact("محاورہ","خاص معنی میں استعمال ہونے والا رائج فقرہ"),Fact("کہانی","واقعات پر مبنی بیان"),Fact("شاعر","شاعری کرنے والا شخص"),Fact("مضمون","کسی موضوع پر نثری تحریر"),Fact("واحد","ایک کے لیے استعمال ہونے والی صورت"),Fact("جمع","ایک سے زیادہ کے لیے استعمال ہونے والی صورت"),Fact("مترادف","ملتے جلتے معنی والا لفظ"),Fact("متضاد","الٹ معنی والا لفظ"))
 "Physics"->listOf(Fact("velocity","speed measured with a direction"),Fact("force","a push or pull that can change motion"),Fact("energy","the ability to do work or cause change"),Fact("frequency","the number of cycles per second"),Fact("voltage","electric potential difference between two points"),Fact("current","the rate at which electric charge flows"),Fact("mass","a measure of the amount of matter"),Fact("pressure","force acting per unit area"),Fact("inertia","the tendency to resist a change in motion"),Fact("wavelength","distance between matching points on consecutive waves"),Fact("conductor","a material that allows charge to flow easily"),Fact("acceleration","the rate at which velocity changes"))
 "Chemistry"->listOf(Fact("atom","the smallest unit of an element that keeps its identity"),Fact("molecule","two or more atoms chemically bonded together"),Fact("element","a pure substance made of one type of atom"),Fact("mixture","substances combined physically rather than chemically"),Fact("reaction","a process that changes substances into other substances"),Fact("solution","a uniform mixture of a solute and solvent"),Fact("electron","a negatively charged subatomic particle"),Fact("acid","a substance that donates hydrogen ions in water-based chemistry"),Fact("base","a substance that can accept hydrogen ions or produce hydroxide ions"),Fact("isotope","atoms of one element with different neutron counts"),Fact("catalyst","a substance that speeds a reaction without being used up overall"),Fact("pH","a scale used to describe acidity or basicity"))
 "Biology"->listOf(Fact("cell","the basic structural and functional unit of life"),Fact("root","a plant part that anchors it and often absorbs water"),Fact("organ","a body structure made of tissues that performs a function"),Fact("gene","a DNA segment carrying hereditary information"),Fact("ecosystem","living organisms interacting with their environment"),Fact("chlorophyll","the green pigment that absorbs light for photosynthesis"),Fact("tissue","a group of similar cells working together"),Fact("habitat","the natural home of an organism"),Fact("pollination","transfer of pollen to the female part of a flower"),Fact("respiration","cellular process that releases usable energy from food"),Fact("producer","an organism that makes its own food"),Fact("decomposer","an organism that breaks down dead matter"))
 else->listOf(Fact("concept","an idea used to explain a subject"),Fact("example","a specific case that illustrates an idea"),Fact("evidence","information used to support a claim"),Fact("pattern","a repeated or recognizable arrangement"))
}
private fun make(g:Int,s:String,d:String,i:Int):M{
 val n=i+1
 val diffCode=when(d){"Easy"->1;"Medium"->2;else->3}
 val b=i+g*37+diffCode*101
 val topics=when(s){"Mathematics"->listOf("Arithmetic","Fractions","Algebra","Geometry","Patterns");"Science"->listOf("Matter","Life","Energy","Earth","Forces");"English"->listOf("Grammar","Vocabulary","Tenses","Reading","Writing");"Computer Science"->listOf("Hardware","Software","Programming","Networks","Cyber Safety");"SST"->listOf("History","Geography","Civics","Economics","Society");"Urdu"->listOf("قواعد","الفاظ","مطالعہ","ادب","تحریر");"Physics"->listOf("Motion","Energy","Waves","Electricity","Forces");"Chemistry"->listOf("Atoms","Matter","Reactions","Solutions","Elements");else->listOf("Cells","Plants","Animals","Genetics","Ecology")}
 val topic=topics[i%topics.size]
 if(s=="Mathematics")return math(g,d,topic,n,b)
 val fs=facts(s)
 val idx=(i+g*3+diffCode*5)%fs.size
 val f=fs[idx]
 val wrong=listOf(fs[(idx+1)%fs.size].term,fs[(idx+2)%fs.size].term,fs[(idx+3)%fs.size].term)
 val slot=(i+g+diffCode)%4
 val opts=MutableList(4){""};opts[slot]=f.term
 var wi=0;for(k in 0..3)if(k!=slot){opts[k]=wrong[wi];wi++}
 val stems=listOf("Which term matches this definition: ${f.meaning}?","Class $g $s • $topic: choose the term described by “${f.meaning}”.","A learner writes clue $n: “${f.meaning}”. Which answer fits?","Select the best concept for this meaning: ${f.meaning}.","During a $topic activity, the teacher says “${f.meaning}”. What is being described?","Which $s term is correctly matched to this description: ${f.meaning}?","Review item $n: identify the term for “${f.meaning}”.","In difficulty $d practice, which term means “${f.meaning}”?")
 val question="${stems[i%stems.size]}  (Item $n)"
 return M(questionId(g,s,d,n),g,s,d,topic,question,opts,slot,"Correct answer: ${f.term}. Meaning: ${f.meaning}. Topic: $topic. Review the definition and compare it with the other choices.")
}
private fun math(g:Int,d:String,t:String,n:Int,b:Int):M{
 val mode=n%10
 val level=when(d){"Easy"->0;"Medium"->1;else->2}
 val base=b%97+g*(level+1)
 val a=base+(if(level==0)0 else n%31)
 val x=(b*(level+2)+n*3)%((if(level==0)20 else if(level==1)60 else 120))+2
 val ans=when(mode){0->a+x;1->a*x;2->abs(a-x);3->(a+x)%10;4->a+x+5;5->a*x+3;6->abs(a-x)+2;7->a*x%17;8->(a+x)*2;else->a*x+a}
 val q=when(mode){0->"Class $g Mathematics: calculate $a + $x.";1->"Class $g Mathematics: calculate $a × $x.";2->"Class $g Mathematics: find |$a − $x|.";3->"Class $g Mathematics: find the remainder when ${a+x} is divided by 10.";4->"$a increases by ${x+5}. What is the new value?";5->"Multiply $a by $x, then add 3.";6->"Find |$a − $x| + 2.";7->"Find the remainder when ${a*x} is divided by 17.";8->"Double the sum of $a and $x.";else->"Multiply $a by $x, then add $a."}
 val choices=linkedSetOf(ans,ans+1,if(ans>1)ans-1 else ans+2,ans+3,ans-2,ans+5).filter{it>=0}.take(4).toList()
 val opts=if(choices.size==4)choices else (choices+List(4-choices.size){ans+it+7}).take(4)
 return M(questionId(g,"Mathematics",d,n),g,"Mathematics",d,t,"$q (Practice item $n)",opts.map{it.toString()},opts.indexOf(ans).coerceAtLeast(0),"Work through the operation step by step. Correct answer: $ans.")
}

private fun questionId(g:Int,s:String,d:String,n:Int):Int{val subjectCode=(subs.indexOfFirst{it.first==s}+1).coerceAtLeast(1);val diffCode=when(d){"Easy"->1;"Medium"->2;else->3};return g*1_000_000+subjectCode*100_000+diffCode*10_000+n}
private fun shuffleOptions(q:M,seed:Int):M{val indexed=q.opts.mapIndexed{index,value->index to value}.shuffled(kotlin.random.Random(seed));return q.copy(opts=indexed.map{it.second},ans=indexed.indexOfFirst{it.first==q.ans})}
private fun loadNotes(p:android.content.SharedPreferences):List<Note>{return try{val a=JSONArray(p.getString("notes","[]"));List(a.length()){val o=a.getJSONObject(it);Note(o.getLong("id"),o.getString("title"),o.getString("body"))}}catch(_:Exception){emptyList()}}
private fun loadTasks(p:android.content.SharedPreferences):List<Task>{return try{val a=JSONArray(p.getString("tasks","[]"));List(a.length()){val o=a.getJSONObject(it);Task(o.getLong("id"),o.getString("text"),o.getBoolean("done"))}}catch(_:Exception){emptyList()}}
private fun notesJson(v:List<Note>)=JSONArray().apply{v.forEach{put(JSONObject().apply{put("id",it.id);put("title",it.title);put("body",it.body)})}}.toString()
private fun tasksJson(v:List<Task>)=JSONArray().apply{v.forEach{put(JSONObject().apply{put("id",it.id);put("text",it.text);put("done",it.done)})}}.toString()
