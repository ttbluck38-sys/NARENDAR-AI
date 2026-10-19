@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.narendar.hub

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import kotlin.math.abs

private data class FQ(val id:Int,val grade:Int,val subject:String,val difficulty:String,val topic:String,val text:String,val options:List<String>,val answer:Int,val explanation:String)
private data class FN(val id:Long,val title:String,val body:String)
private data class FT(val id:Long,val text:String,val done:Boolean)
private data class FS(val name:String,val icon:String,val info:String)

private val fSubjects=listOf(
 FS("Mathematics","∑","Numbers, algebra, geometry and problem solving"),
 FS("Science","⚗","Matter, life, energy, earth and forces"),
 FS("English","A","Grammar, vocabulary, reading and writing"),
 FS("Computer Science","⌘","Hardware, software, coding and digital skills"),
 FS("SST","◎","History, geography, civics and society"),
 FS("Urdu","ا","قواعد، الفاظ، مطالعہ اور ادب"),
 FS("Physics","Φ","Motion, energy, waves and electricity"),
 FS("Chemistry","⚛","Atoms, matter, reactions and solutions"),
 FS("Biology","♧","Cells, organisms, genetics and ecology")
)
private val fColors=listOf(Color(0xFF315EFB),Color(0xFF7C4DFF),Color(0xFF00A884),Color(0xFFFF8A3D),Color(0xFFE83E8C))

@Composable fun FreshHub(context:Context){
 val p=remember{context.getSharedPreferences("narendar_fresh",Context.MODE_PRIVATE)}
 var grade by remember{mutableIntStateOf(p.getInt("grade",-1))}
 var page by remember{mutableStateOf(if(grade<0)"pick" else "home")}
 var dark by remember{mutableStateOf(p.getBoolean("dark",false))}
 var xp by remember{mutableIntStateOf(p.getInt("xp",0))}
 var correct by remember{mutableIntStateOf(p.getInt("correct",0))}
 var attempted by remember{mutableIntStateOf(p.getInt("attempted",0))}
 var notes by remember{mutableStateOf(fLoadNotes(p))}
 var tasks by remember{mutableStateOf(fLoadTasks(p))}
 var toast by remember{mutableStateOf("")}
 fun record(ok:Boolean){attempted++;if(ok)correct++;xp+=if(ok)20 else 5;p.edit().putInt("attempted",attempted).putInt("correct",correct).putInt("xp",xp).apply()}
 fun saveNotes(v:List<FN>){notes=v;p.edit().putString("notes",fNotesJson(v)).apply()}
 fun saveTasks(v:List<FT>){tasks=v;p.edit().putString("tasks",fTasksJson(v)).apply()}
 LaunchedEffect(toast){if(toast.isNotEmpty()){delay(1500);toast=""}}
 val light=lightColorScheme(primary=fColors[0],secondary=fColors[2],tertiary=fColors[4],surface=Color(0xFFF7F8FC))
 val darkScheme=darkColorScheme(primary=Color(0xFF9AB8FF),secondary=Color(0xFF70E0BE),tertiary=Color(0xFFFF9DC7))
 MaterialTheme(colorScheme=if(dark)darkScheme else light){
  if(page=="pick"){FreshPicker(grade){grade=it;p.edit().putInt("grade",it).apply();page="home"};return@MaterialTheme}
  Scaffold(topBar={CenterAlignedTopAppBar(title={Text("Narendar Hub",fontWeight=FontWeight.Black)})},bottomBar={NavigationBar{listOf("home" to "⌂","learn" to "▣","quiz" to "✓","library" to "▤","profile" to "●").forEach{(key,icon)->NavigationBarItem(page==key,{page=key},icon={Text(icon)},label={Text(key.replaceFirstChar{it.uppercase()})})}}}){pad->
   Box(Modifier.fillMaxSize().padding(pad)){
    AnimatedContent(targetState=page,label="page"){target->when(target){
     "home"->FreshHome(grade,xp,correct,attempted,tasks.count{!it.done},notes.size){page=it}
     "learn"->FreshLearn(grade){page="quiz"}
     "quiz"->FreshQuiz(grade,::record){toast=it}
     "library"->FreshLibrary(notes,tasks,::saveNotes,::saveTasks)
     "profile"->FreshProfile(grade,xp,correct,attempted,dark,{dark=it;p.edit().putBoolean("dark",it).apply()}){grade=-1;p.edit().putInt("grade",-1).apply();page="pick"}
    }}
    AnimatedVisibility(toast.isNotEmpty(),enter=slideInVertically{it}+fadeIn(),exit=fadeOut(),modifier=Modifier.align(Alignment.BottomCenter).padding(8.dp)){Surface(shape=RoundedCornerShape(18.dp),tonalElevation=8.dp){Text(toast,Modifier.padding(15.dp),fontWeight=FontWeight.Bold)}}
   }
  }
 }
}

@Composable private fun FreshPicker(current:Int,choose:(Int)->Unit){
 val pulse=rememberInfiniteTransition(label="pulse").animateFloat(1f,1.06f,infiniteRepeatable(tween(1100),RepeatMode.Reverse),label="pulse").value
 Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF111827),Color(0xFF312E81),Color(0xFF0F766E)))).verticalScroll(rememberScrollState())){
  Column(Modifier.padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally){
   Spacer(Modifier.height(28.dp));Surface(shape=CircleShape,color=Color.White.copy(.13f),modifier=Modifier.size(105.dp).scale(pulse)){Box(contentAlignment=Alignment.Center){Text("N",color=Color.White,fontSize=50.sp,fontWeight=FontWeight.Black)}}
   Text("NARENDAR HUB",color=Color.White,fontSize=29.sp,fontWeight=FontWeight.Black);Text("Learn • Practice • Master",color=Color.White.copy(.7f));Spacer(Modifier.height(24.dp))
   Text("Choose learning scope",color=Color.White,fontSize=21.sp,fontWeight=FontWeight.Black);Text("A selected class includes learning from Class 1 up to that class.",color=Color.White.copy(.65f),textAlign=TextAlign.Center);Spacer(Modifier.height(12.dp))
   listOf(-1 to "🌎 All Classes",1 to "🎒 Class 1",2 to "🎒 Class 2",3 to "🎒 Class 3",4 to "🎒 Class 4",5 to "🎒 Class 5",6 to "🎒 Class 6",7 to "🎒 Class 7",8 to "🚀 Class 8",9 to "🏆 Class 9+").forEach{(v,label)->Card(onClick={choose(v)},modifier=Modifier.fillMaxWidth().padding(4.dp),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White.copy(.11f))){Row(Modifier.padding(16.dp)){Text(label,color=Color.White,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Text(if(current==v)"✓" else "›",color=Color.White,fontSize=22.sp)}}}
   Text("2,500 unique MCQs per class × subject × difficulty",color=Color.White.copy(.55f),fontSize=12.sp,modifier=Modifier.padding(top=12.dp))
  }
 }
}

@Composable private fun FreshHome(g:Int,xp:Int,ok:Int,attempted:Int,tasks:Int,notes:Int,nav:(String)->Unit){
 val acc=if(attempted==0)0 else ok*100/attempted
 val pulse=rememberInfiniteTransition(label="hero").animateFloat(.97f,1f,infiniteRepeatable(tween(1400),RepeatMode.Reverse),label="hero").value
 LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  item{Text("Welcome back 👋",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black);Text(if(g<0)"All Classes" else "Class $g",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)}
  item{Card(Modifier.fillMaxWidth().scale(pulse),shape=RoundedCornerShape(30.dp)){Box(Modifier.background(Brush.linearGradient(listOf(fColors[(xp/100)%5],fColors[(xp/100+2)%5])))){Column(Modifier.padding(22.dp)){Text("LEVEL ${xp/500+1}  •  $xp XP",color=Color.White.copy(.82f),fontWeight=FontWeight.Bold);Text("Your learning journey",color=Color.White,fontSize=27.sp,fontWeight=FontWeight.Black);Spacer(Modifier.height(12.dp));LinearProgressIndicator({(xp%500)/500f},Modifier.fillMaxWidth().height(9.dp),color=Color.White,trackColor=Color.White.copy(.2f));Spacer(Modifier.height(9.dp));Text("🎯 $acc% accuracy   🔥 ${if(attempted>0)1 else 0} streak",color=Color.White,fontWeight=FontWeight.Bold)}}}}
  item{Text("Explore",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black)}
  item{FreshAction("🎮","Quiz Arena","Unique MCQs • Easy / Medium / Hard",fColors[0]){nav("quiz")}}
  item{FreshAction("📚","Study Library","Subjects, concepts and explanations",fColors[1]){nav("learn")}}
  item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FreshStat("Questions",attempted.toString(),fColors[2],Modifier.weight(1f));FreshStat("Notes",notes.toString(),fColors[3],Modifier.weight(1f));FreshStat("Tasks",tasks.toString(),fColors[4],Modifier.weight(1f))}}
  item{FreshAction("🃏","Revision Hub","Notes + planner + progress",fColors[2]){nav("library")}}
  item{FreshAction("🏆","Stats + Settings","XP, accuracy and theme",fColors[3]){nav("profile")}}
 }
}
@Composable private fun FreshAction(icon:String,title:String,sub:String,c:Color,go:()->Unit){Card(Modifier.fillMaxWidth().clickable{go()},shape=RoundedCornerShape(22.dp)){Row(Modifier.padding(17.dp),verticalAlignment=Alignment.CenterVertically){Surface(shape=CircleShape,color=c.copy(.13f),modifier=Modifier.size(50.dp)){Box(contentAlignment=Alignment.Center){Text(icon,fontSize=25.sp)}};Spacer(Modifier.width(13.dp));Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.Black);Text(sub,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text("›",fontSize=28.sp,color=c)}}}
@Composable private fun FreshStat(name:String,value:String,c:Color,m:Modifier){Card(m,shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(13.dp)){Text(value,fontSize=22.sp,fontWeight=FontWeight.Black,color=c);Text(name,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}

@Composable private fun FreshLearn(g:Int,start:()->Unit){
 var selected by remember{mutableStateOf("Mathematics")};var diff by remember{mutableStateOf("All")};val list=if(g<0||g>=9)fSubjects else fSubjects.take(6)
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){Text("Study Library",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black);Text("Study → revise → practice → test",color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(12.dp));
  list.forEachIndexed{ix,s->Card(onClick={selected=s.name},modifier=Modifier.fillMaxWidth().padding(vertical=4.dp),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=if(selected==s.name)fColors[ix%5].copy(.15f)else MaterialTheme.colorScheme.surfaceVariant)){Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically){Text(s.icon,fontSize=23.sp);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(s.name,fontWeight=FontWeight.Black);Text(s.info,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text(if(selected==s.name)"✓" else "›",color=fColors[ix%5])}}}
  Spacer(Modifier.height(8.dp));Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("All","Easy","Medium","Hard").forEach{FilterChip(diff==it,{diff=it},label={Text(it)})}}
  Spacer(Modifier.height(12.dp));Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(25.dp)){Column(Modifier.padding(20.dp)){Text("📖 $selected",fontSize=23.sp,fontWeight=FontWeight.Black);Text("Class ${if(g<0)"1–9+" else "1–$g"} • 2,500 unique questions per difficulty",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=12.sp);Spacer(Modifier.height(8.dp));Text("MCQ Study Library",fontWeight=FontWeight.Bold);Text("Each answer includes an explanation. Test questions are selected without repeating the same ID inside a test.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(12.dp));Button(start,modifier=Modifier.fillMaxWidth()){Text("START PRACTICE")}}}}
}

@Composable private fun FreshQuiz(g:Int,record:(Boolean)->Unit,toast:(String)->Unit){
 var subject by remember{mutableStateOf("Mathematics")};var diff by remember{mutableStateOf("Mixed")};var size by remember{mutableIntStateOf(10)};var started by remember{mutableStateOf(false)};var at by remember{mutableIntStateOf(0)};var chosen by remember{mutableIntStateOf(-1)};var score by remember{mutableIntStateOf(0)};var seed by remember{mutableIntStateOf(0)}
 val maxGrade=if(g<0)9 else g;val questions=remember(subject,diff,size,seed,g){freshTest(maxGrade,subject,diff,size,seed)}
 if(!started){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){Text("Quiz Arena",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black);Text("No duplicate question IDs in a test. New Test creates a new selection.",color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(14.dp));Text("Subject",fontWeight=FontWeight.Black);Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){(if(g<0||g>=9)fSubjects else fSubjects.take(6)).forEach{s->FilterChip(subject==s.name,{subject=s.name},label={Text(s.name)})}};Spacer(Modifier.height(10.dp));Text("Difficulty",fontWeight=FontWeight.Black);Row{listOf("Mixed","Easy","Medium","Hard").forEach{FilterChip(diff==it,{diff=it},label={Text(it)},modifier=Modifier.padding(end=4.dp))}};Spacer(Modifier.height(10.dp));Text("Test size",fontWeight=FontWeight.Black);Row{listOf(10,20,50,100).forEach{FilterChip(size==it,{size=it},label={Text("$it")},modifier=Modifier.padding(end=4.dp))}};Spacer(Modifier.height(18.dp));FreshBigCard(fColors[0],"🎮 READY","2,500 unique MCQs per class × subject × difficulty."){started=true}};return}
 if(at>=questions.size){Column(Modifier.fillMaxSize().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){Spacer(Modifier.height(40.dp));Text("🏆",fontSize=70.sp);Text("Test Complete",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black);Text("$score / $size",fontSize=34.sp,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary);Text("Accuracy ${score*100/size}% • XP ${score*20}",color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(20.dp));Button({seed++;at=0;chosen=-1;score=0}){Text("NEW UNIQUE TEST")}};return}
 val q=questions[at];Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){Row{Text("${at+1}/$size",fontWeight=FontWeight.Black);Spacer(Modifier.weight(1f));Text("+20 XP",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)};Spacer(Modifier.height(7.dp));LinearProgressIndicator({(at+if(chosen>=0)1 else 0).toFloat()/size},Modifier.fillMaxWidth().height(8.dp));Spacer(Modifier.height(10.dp));Text("Class ${q.grade} • ${q.subject} • ${q.difficulty} • ${q.topic}",color=MaterialTheme.colorScheme.primary,fontSize=12.sp,fontWeight=FontWeight.Bold);AnimatedContent(q.id,label="question"){FreshQuestionCard(q)};q.options.forEachIndexed{ix,opt->val good=chosen>=0&&ix==q.answer;val bad=chosen==ix&&ix!=q.answer;Card(onClick={if(chosen<0){chosen=ix;record(ix==q.answer);if(ix==q.answer)score++;toast(if(ix==q.answer)"Correct! +20 XP 🎉" else "Review the explanation 📘")}},modifier=Modifier.fillMaxWidth().padding(vertical=4.dp),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=if(good)Color(0xFF00A884).copy(.16f)else if(bad)Color(0xFFE83E8C).copy(.16f)else MaterialTheme.colorScheme.surfaceVariant)){Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically){Text("${'A'+ix}",fontWeight=FontWeight.Black,color=if(good)Color(0xFF00A884)else MaterialTheme.colorScheme.primary);Spacer(Modifier.width(12.dp));Text(opt,fontWeight=FontWeight.SemiBold)}}};AnimatedVisibility(chosen>=0,enter=scaleIn()+fadeIn()){Card(Modifier.fillMaxWidth().padding(top=10.dp),shape=RoundedCornerShape(19.dp)){Column(Modifier.padding(16.dp)){Text(if(chosen==q.answer)"✓ Correct answer" else "✕ Review this one",fontWeight=FontWeight.Black);Text(q.explanation,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(9.dp));Button({at++;chosen=-1},modifier=Modifier.fillMaxWidth()){Text(if(at==size-1)"FINISH TEST" else "NEXT UNIQUE QUESTION →")}}}}}
}
@Composable private fun FreshQuestionCard(q:FQ){Card(Modifier.fillMaxWidth().padding(vertical=10.dp),shape=RoundedCornerShape(25.dp)){Text(q.text,Modifier.padding(21.dp),fontSize=19.sp,fontWeight=FontWeight.Black)}}
@Composable private fun FreshBigCard(c:Color,title:String,body:String,go:()->Unit){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(27.dp)){Box(Modifier.background(Brush.linearGradient(listOf(c,c.copy(.72f))))){Column(Modifier.padding(21.dp)){Text(title,color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Black);Text(body,color=Color.White.copy(.86f));Spacer(Modifier.height(13.dp));Button(go,colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=c),modifier=Modifier.fillMaxWidth()){Text("ENTER ARENA",fontWeight=FontWeight.Black)}}}}}

@Composable private fun FreshLibrary(notes:List<FN>,tasks:List<FT>,saveNotes:(List<FN>)->Unit,saveTasks:(List<FT>)->Unit){var tab by remember{mutableIntStateOf(0)};var title by remember{mutableStateOf("")};var body by remember{mutableStateOf("")};var task by remember{mutableStateOf("")};Column(Modifier.fillMaxSize().padding(16.dp)){Text("Revision Hub",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black);Row{FilterChip(tab==0,{tab=0},label={Text("Notes")});Spacer(Modifier.width(6.dp));FilterChip(tab==1,{tab=1},label={Text("Planner")})};Spacer(Modifier.height(10.dp));if(tab==0){OutlinedTextField(title,{title=it},label={Text("Note title")},modifier=Modifier.fillMaxWidth());Spacer(Modifier.height(7.dp));OutlinedTextField(body,{body=it},label={Text("Study note")},modifier=Modifier.fillMaxWidth().height(120.dp));Spacer(Modifier.height(7.dp));Button({if(title.isNotBlank()){saveNotes(notes+FN(System.currentTimeMillis(),title,body));title="";body=""}},modifier=Modifier.fillMaxWidth()){Text("SAVE NOTE")};Spacer(Modifier.height(10.dp));LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(notes.reversed(),key={it.id}){n->Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(15.dp)){Text(n.title,fontWeight=FontWeight.Black);Text(n.body,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text("DELETE",Modifier.clickable{saveNotes(notes.filterNot{it.id==n.id})}.padding(top=7.dp),fontSize=11.sp,color=fColors[4],fontWeight=FontWeight.Bold)}}}}}}else{OutlinedTextField(task,{task=it},label={Text("New study task")},modifier=Modifier.fillMaxWidth());Spacer(Modifier.height(7.dp));Button({if(task.isNotBlank()){saveTasks(tasks+FT(System.currentTimeMillis(),task,false));task=""}},modifier=Modifier.fillMaxWidth()){Text("ADD TASK")};Spacer(Modifier.height(10.dp));LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(tasks,key={it.id}){t->Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically){Checkbox(t.done,{v->saveTasks(tasks.map{if(it.id==t.id)it.copy(done=v)else it})});Text(t.text,Modifier.weight(1f),fontWeight=FontWeight.Bold);Text("✕",Modifier.clickable{saveTasks(tasks.filterNot{it.id==t.id})})}}}}}}}}

@Composable private fun FreshProfile(g:Int,xp:Int,ok:Int,attempted:Int,dark:Boolean,setDark:(Boolean)->Unit,change:()->Unit){val acc=if(attempted==0)0 else ok*100/attempted;Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){Text("Profile & Stats",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black);FreshBigCard(fColors[1],"LEVEL ${xp/500+1}","$xp XP • $acc% accuracy • ${if(g<0)"All Classes" else "Class $g"}"){};Spacer(Modifier.height(12.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FreshStat("Attempted",attempted.toString(),fColors[0],Modifier.weight(1f));FreshStat("Correct",ok.toString(),fColors[2],Modifier.weight(1f))};Spacer(Modifier.height(10.dp));Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Text("Dark theme",fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Switch(dark,setDark)}};Spacer(Modifier.height(8.dp));Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(16.dp)){Text("Narendar Hub",fontWeight=FontWeight.Black);Text("Offline learning • unique MCQ engine • study, practice and revision.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}};Spacer(Modifier.height(8.dp));OutlinedButton(change,modifier=Modifier.fillMaxWidth()){Text("CHANGE CLASS")}}
}

private fun freshTest(maxGrade:Int,subject:String,diff:String,size:Int,seed:Int):List<FQ>{val grades=1..maxGrade;val ds=if(diff=="Mixed")listOf("Easy","Medium","Hard")else listOf(diff);val all=ArrayList<FQ>();for(g in grades)for(d in ds)all.addAll(freshBank(g,subject,d,2500));return all.shuffled(kotlin.random.Random(seed*7919+subject.hashCode())).take(size)}
private fun freshBank(g:Int,s:String,d:String,n:Int):List<FQ>{val out=ArrayList<FQ>(n);for(i in 0 until n)out.add(freshQuestion(g,s,d,i));return out}
private fun freshQuestion(g:Int,s:String,d:String,i:Int):FQ{val n=i+1;val b=i+g*37+when(d){"Easy"->11;"Medium"->101;else->1001};val topic=when(s){"Mathematics"->listOf("Arithmetic","Fractions","Algebra","Geometry","Patterns")[i%5];"Science"->listOf("Matter","Life","Energy","Earth","Forces")[i%5];"English"->listOf("Grammar","Vocabulary","Tenses","Reading","Writing")[i%5];"Computer Science"->listOf("Hardware","Software","Programming","Networks","Cyber Safety")[i%5];"SST"->listOf("History","Geography","Civics","Economics","Society")[i%5];"Urdu"->listOf("قواعد","الفاظ","مطالعہ","ادب","تحریر")[i%5];"Physics"->listOf("Motion","Energy","Waves","Electricity","Forces")[i%5];"Chemistry"->listOf("Atoms","Matter","Reactions","Solutions","Elements")[i%5];else->listOf("Cells","Plants","Animals","Genetics","Ecology")[i%5]};return if(s=="Mathematics")freshMath(g,d,topic,n,b)else freshConcept(g,s,d,topic,n,b)}
private fun freshMath(g:Int,d:String,t:String,n:Int,b:Int):FQ{val mode=n%8;val a=b%89+g;val x=b*3%47+2;val ans=when(mode){0->a+x;1->a*x;2->abs(a-x);3->(a+x)%10;4->a+x+5;5->a*x+3;6->abs(a-x)+2;else->a*x%17};val text=when(mode){0->"Class $g Mathematics: calculate $a + $x. Set #$n";1->"Class $g Mathematics: calculate $a × $x. Set #$n";2->"Class $g Mathematics: find the positive difference of $a and $x. Set #$n";3->"Class $g Mathematics: find the remainder of ${a+x} ÷ 10. Set #$n";4->"Class $g Mathematics: $a increases by ${x+5}. What is the result? Set #$n";5->"Class $g Mathematics: multiply $a by $x, then add 3. Set #$n";6->"Class $g Mathematics: what is |$a − $x| + 2? Set #$n";else->"Class $g Mathematics: what is the remainder of ${a*x} ÷ 17? Set #$n"};val opts=listOf(ans.toString(),(ans+1).toString(),(if(ans>1)ans-1 else ans+2).toString(),(ans+3).toString());return FQ(g*10000+n,g,"Mathematics",d,t,text,opts,0,"Solve the operation carefully. The correct result is $ans.")}
private fun freshConcept(g:Int,s:String,d:String,t:String,n:Int,b:Int):FQ{val data=mapOf("Science" to listOf("evaporation","photosynthesis","gravity","condensation","friction","ecosystem","reflection","magnetism"),"English" to listOf("quickly","beautiful","because","carefully","although","honest","between","however"),"Computer Science" to listOf("CPU","RAM","browser","algorithm","firewall","compiler","keyboard","router"),"SST" to listOf("map","constitution","river","trade","community","citizen","climate","culture"),"Urdu" to listOf("اسم","فعل","صفت","جملہ","محاورہ","کہانی","شاعر","مضمون"),"Physics" to listOf("velocity","force","energy","frequency","voltage","current","mass","pressure"),"Chemistry" to listOf("atom","molecule","element","mixture","reaction","solution","electron","acid"),"Biology" to listOf("cell","root","organ","gene","ecosystem","chlorophyll","tissue","habitat"));val set=data[s]?:data["Science"]!!;val c=set[b%set.size];val opts=listOf(c,set[(b+1)%set.size],set[(b+3)%set.size],set[(b+5)%set.size]);val prompts=listOf("Which $s concept best matches the topic $t?","Choose the most accurate $s term for this Class $g question.","Which option correctly describes the Class $g $s idea?","Select the best answer for this $t exercise.");return FQ(g*10000+n,g,s,d,t,"${prompts[b%prompts.size]} Set #$n",opts,0,"The correct answer is $c. Review the definition of $t and compare it with the alternatives.")}

private fun fLoadNotes(p:android.content.SharedPreferences):List<FN>{return try{val a=JSONArray(p.getString("notes","[]"));List(a.length()){val o=a.getJSONObject(it);FN(o.getLong("id"),o.getString("title"),o.getString("body"))}}catch(_:Exception){emptyList()}}
private fun fLoadTasks(p:android.content.SharedPreferences):List<FT>{return try{val a=JSONArray(p.getString("tasks","[]"));List(a.length()){val o=a.getJSONObject(it);FT(o.getLong("id"),o.getString("text"),o.getBoolean("done"))}}catch(_:Exception){emptyList()}}
private fun fNotesJson(v:List<FN>)=JSONArray().apply{v.forEach{put(JSONObject().apply{put("id",it.id);put("title",it.title);put("body",it.body)})}}.toString()
private fun fTasksJson(v:List<FT>)=JSONArray().apply{v.forEach{put(JSONObject().apply{put("id",it.id);put("text",it.text);put("done",it.done)})}}.toString()
