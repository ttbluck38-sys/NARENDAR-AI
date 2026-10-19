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

private data class ZQ(val id:Int,val grade:Int,val subject:String,val difficulty:String,val topic:String,val text:String,val options:List<String>,val answer:Int,val explanation:String)
private data class ZN(val id:Long,val title:String,val body:String)
private data class ZT(val id:Long,val text:String,val done:Boolean)
private data class ZS(val name:String,val icon:String,val info:String)
private val zSubjects=listOf(
 ZS("Mathematics","∑","Numbers, algebra, geometry and problem solving"),ZS("Science","⚗","Matter, life, energy and forces"),
 ZS("English","A","Grammar, vocabulary, reading and writing"),ZS("Computer Science","⌘","Hardware, software, coding and digital skills"),
 ZS("SST","◎","History, geography, civics and society"),ZS("Urdu","ا","قواعد، الفاظ، مطالعہ اور ادب"),
 ZS("Physics","Φ","Motion, energy, waves and electricity"),ZS("Chemistry","⚛","Atoms, matter, reactions and solutions"),ZS("Biology","♧","Cells, organisms, genetics and ecology")
)
private val zColors=listOf(Color(0xFF315EFB),Color(0xFF7C4DFF),Color(0xFF00A884),Color(0xFFFF8A3D),Color(0xFFE83E8C))

@Composable fun FreshHub2(context:Context){
 val p=remember{context.getSharedPreferences("narendar_fresh2",Context.MODE_PRIVATE)}
 var grade by remember{mutableIntStateOf(p.getInt("grade",-1))}
 var page by remember{mutableStateOf(if(grade<0)"pick" else "home")}
 var dark by remember{mutableStateOf(p.getBoolean("dark",false))}
 var xp by remember{mutableIntStateOf(p.getInt("xp",0))}
 var correct by remember{mutableIntStateOf(p.getInt("correct",0))}
 var attempted by remember{mutableIntStateOf(p.getInt("attempted",0))}
 var notes by remember{mutableStateOf(zLoadNotes(p))}
 var tasks by remember{mutableStateOf(zLoadTasks(p))}
 var toast by remember{mutableStateOf("")}
 fun record(ok:Boolean){attempted++;if(ok)correct++;xp+=if(ok)20 else 5;p.edit().putInt("attempted",attempted).putInt("correct",correct).putInt("xp",xp).apply()}
 fun saveNotes(v:List<ZN>){notes=v;p.edit().putString("notes",zNotesJson(v)).apply()}
 fun saveTasks(v:List<ZT>){tasks=v;p.edit().putString("tasks",zTasksJson(v)).apply()}
 LaunchedEffect(toast){if(toast.isNotEmpty()){delay(1500);toast=""}}
 val scheme=if(dark)darkColorScheme(primary=Color(0xFF9AB8FF),secondary=Color(0xFF70E0BE),tertiary=Color(0xFFFF9DC7)) else lightColorScheme(primary=zColors[0],secondary=zColors[2],tertiary=zColors[4],surface=Color(0xFFF7F8FC))
 MaterialTheme(colorScheme=scheme){
  if(page=="pick"){ZPicker(grade){grade=it;p.edit().putInt("grade",it).apply();page="home"};return@MaterialTheme}
  Scaffold(topBar={CenterAlignedTopAppBar(title={Text("Narendar Hub",fontWeight=FontWeight.Black)})},bottomBar={NavigationBar{listOf("home" to "⌂","learn" to "▣","quiz" to "✓","library" to "▤","profile" to "●").forEach{(k,icon)->NavigationBarItem(page==k,{page=k},icon={Text(icon)},label={Text(k.replaceFirstChar{it.uppercase()})})}}}){pad->
   Box(Modifier.fillMaxSize().padding(pad)){
    AnimatedContent(targetState=page,label="page"){s->
     when(s){
      "home"->ZHome(grade,xp,correct,attempted,tasks.count{!it.done},notes.size){page=it}
      "learn"->ZLearn(grade){page="quiz"}
      "quiz"->ZQuiz(grade,::record){toast=it}
      "library"->ZLibrary(notes,tasks,saveNotes,saveTasks)
      "profile"->ZProfile(grade,xp,correct,attempted,dark,{dark=it;p.edit().putBoolean("dark",it).apply()}){grade=-1;p.edit().putInt("grade",-1).apply();page="pick"}
     }
    }
    AnimatedVisibility(toast.isNotEmpty(),enter=slideInVertically{it}+fadeIn(),exit=fadeOut(),modifier=Modifier.align(Alignment.BottomCenter).padding(8.dp)){Surface(shape=RoundedCornerShape(18.dp),tonalElevation=8.dp){Text(toast,Modifier.padding(15.dp),fontWeight=FontWeight.Bold)}}
   }
  }
 }
}

@Composable private fun ZPicker(current:Int,choose:(Int)->Unit){
 val pulse=rememberInfiniteTransition(label="pulse").animateFloat(1f,1.06f,infiniteRepeatable(tween(1100),RepeatMode.Reverse),label="pulse").value
 Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF111827),Color(0xFF312E81),Color(0xFF0F766E)))).verticalScroll(rememberScrollState())){
  Column(Modifier.padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally){
   Spacer(Modifier.height(28.dp));Surface(shape=CircleShape,color=Color.White.copy(.13f),modifier=Modifier.size(105.dp).scale(pulse)){Box(contentAlignment=Alignment.Center){Text("N",color=Color.White,fontSize=50.sp,fontWeight=FontWeight.Black)}}
   Text("NARENDAR HUB",color=Color.White,fontSize=29.sp,fontWeight=FontWeight.Black);Text("Learn • Practice • Master",color=Color.White.copy(.7f));Spacer(Modifier.height(24.dp))
   Text("Choose learning scope",color=Color.White,fontSize=21.sp,fontWeight=FontWeight.Black);Text("A selected class includes Class 1 through that class.",color=Color.White.copy(.65f),textAlign=TextAlign.Center);Spacer(Modifier.height(12.dp))
   listOf(-1 to "🌎 All Classes",1 to "🎒 Class 1",2 to "🎒 Class 2",3 to "🎒 Class 3",4 to "🎒 Class 4",5 to "🎒 Class 5",6 to "🎒 Class 6",7 to "🎒 Class 7",8 to "🚀 Class 8",9 to "🏆 Class 9+").forEach{(v,label)->
    Card(onClick={choose(v)},modifier=Modifier.fillMaxWidth().padding(4.dp),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White.copy(.11f))){Row(Modifier.padding(16.dp)){Text(label,color=Color.White,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Text(if(current==v)"✓" else "›",color=Color.White,fontSize=22.sp)}}
   }
   Text("2,500 unique MCQs per class × subject × difficulty",color=Color.White.copy(.55f),fontSize=12.sp,modifier=Modifier.padding(top=12.dp))
  }
 }
}

@Composable private fun ZHome(g:Int,xp:Int,ok:Int,attempted:Int,tasks:Int,notes:Int,nav:(String)->Unit){
 val acc=if(attempted==0)0 else ok*100/attempted
 val pulse=rememberInfiniteTransition(label="hero").animateFloat(.97f,1f,infiniteRepeatable(tween(1400),RepeatMode.Reverse),label="hero").value
 LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  item{Text("Welcome back 👋",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black);Text(if(g<0)"All Classes" else "Class $g",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)}
  item{Card(Modifier.fillMaxWidth().scale(pulse),shape=RoundedCornerShape(30.dp)){Box(Modifier.background(Brush.linearGradient(listOf(zColors[(xp/100)%5],zColors[(xp/100+2)%5])))){Column(Modifier.padding(22.dp)){Text("LEVEL ${xp/500+1} • $xp XP",color=Color.White.copy(.82f),fontWeight=FontWeight.Bold);Text("Your learning journey",color=Color.White,fontSize=27.sp,fontWeight=FontWeight.Black);Spacer(Modifier.height(12.dp));LinearProgressIndicator({(xp%500)/500f},Modifier.fillMaxWidth().height(9.dp),color=Color.White,trackColor=Color.White.copy(.2f));Spacer(Modifier.height(9.dp));Text("🎯 $acc% accuracy   🔥 ${if(attempted>0)1 else 0} streak",color=Color.White,fontWeight=FontWeight.Bold)}}}}
  item{Text("Explore",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black)}
  item{ZAction("🎮","Quiz Arena","Unique MCQs • Easy / Medium / Hard",zColors[0]){nav("quiz")}}
  item{ZAction("📚","Study Library","Subjects, concepts and explanations",zColors[1]){nav("learn")}}
  item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ZStat("Questions",attempted.toString(),zColors[2],Modifier.weight(1f));ZStat("Notes",notes.toString(),zColors[3],Modifier.weight(1f));ZStat("Tasks",tasks.toString(),zColors[4],Modifier.weight(1f))}}
  item{ZAction("🃏","Revision Hub","Notes + planner + progress",zColors[2]){nav("library")}}
  item{ZAction("🏆","Stats + Settings","XP, accuracy and theme",zColors[3]){nav("profile")}}
 }
}
@Composable private fun ZAction(icon:String,title:String,sub:String,c:Color,go:()->Unit){Card(Modifier.fillMaxWidth().clickable{go()},shape=RoundedCornerShape(22.dp)){Row(Modifier.padding(17.dp),verticalAlignment=Alignment.CenterVertically){Surface(shape=CircleShape,color=c.copy(.13f),modifier=Modifier.size(50.dp)){Box(contentAlignment=Alignment.Center){Text(icon,fontSize=25.sp)}};Spacer(Modifier.width(13.dp));Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.Black);Text(sub,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text("›",fontSize=28.sp,color=c)}}}
@Composable private fun ZStat(name:String,value:String,c:Color,m:Modifier){Card(m,shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(13.dp)){Text(value,fontSize=22.sp,fontWeight=FontWeight.Black,color=c);Text(name,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}

@Composable private fun ZLearn(g:Int,start:()->Unit){
 var selected by remember{mutableStateOf("Mathematics")};var difficulty by remember{mutableStateOf("All")};val list=if(g<0||g>=9)zSubjects else zSubjects.take(6)
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){Text("Study Library",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black);Text("Study → revise → practice → test",color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(12.dp));
  list.forEachIndexed{ix,s->Card(onClick={selected=s.name},modifier=Modifier.fillMaxWidth().padding(vertical=4.dp),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=if(selected==s.name)zColors[ix%5].copy(.15f)else MaterialTheme.colorScheme.surfaceVariant)){Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically){Text(s.icon,fontSize=23.sp);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(s.name,fontWeight=FontWeight.Black);Text(s.info,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text(if(selected==s.name)"✓" else "›",color=zColors[ix%5])}}}
  Spacer(Modifier.height(8.dp));Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("All","Easy","Medium","Hard").forEach{FilterChip(difficulty==it,{difficulty=it},label={Text(it)})}}
  Spacer(Modifier.height(12.dp));Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(25.dp)){Column(Modifier.padding(20.dp)){Text("📖 $selected",fontSize=23.sp,fontWeight=FontWeight.Black);Text("Class ${if(g<0)"1–9+" else "1–$g"} • 2,500 unique questions per difficulty",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=12.sp);Spacer(Modifier.height(8.dp));Text("MCQ Study Library",fontWeight=FontWeight.Bold);Text("Every answer has an explanation. Test questions are selected from unique IDs.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(12.dp));Button(start,modifier=Modifier.fillMaxWidth()){Text("START PRACTICE")}}}}
}

@Composable private fun ZQuiz(g:Int,record:(Boolean)->Unit,toast:(String)->Unit){
 var subject by remember{mutableStateOf("Mathematics")};var difficulty by remember{mutableStateOf("Mixed")};var size by remember{mutableIntStateOf(10)};var started by remember{mutableStateOf(false)};var index by remember{mutableIntStateOf(0)};var chosen by remember{mutableIntStateOf(-1)};var score by remember{mutableIntStateOf(0)};var seed by remember{mutableIntStateOf(0)}
 val maxGrade=if(g<0)9 else g;val questions=remember(subject,difficulty,size,seed,g){zTest(maxGrade,subject,difficulty,size,seed)}
 if(!started){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){Text("Quiz Arena",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black);Text("No duplicate question IDs inside a test.",color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(14.dp));Text("Subject",fontWeight=FontWeight.Black);Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){(if(g<0||g>=9)zSubjects else zSubjects.take(6)).forEach{s->FilterChip(subject==s.name,{subject=s.name},label={Text(s.name)})}};Spacer(Modifier.height(10.dp));Text("Difficulty",fontWeight=FontWeight.Black);Row{listOf("Mixed","Easy","Medium","Hard").forEach{FilterChip(difficulty==it,{difficulty=it},label={Text(it)},modifier=Modifier.padding(end=4.dp))}};Spacer(Modifier.height(10.dp));Text("Test size",fontWeight=FontWeight.Black);Row{listOf(10,20,50,100).forEach{FilterChip(size==it,{size=it},label={Text("$it")},modifier=Modifier.padding(end=4.dp))}};Spacer(Modifier.height(18.dp));ZBigCard(zColors[0],"🎮 READY","2,500 unique MCQs per class × subject × difficulty."){started=true}};return}
 if(index>=questions.size){Column(Modifier.fillMaxSize().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){Spacer(Modifier.height(40.dp));Text("🏆",fontSize=70.sp);Text("Test Complete",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black);Text("$score / $size",fontSize=34.sp,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary);Text("Accuracy ${score*100/size}% • XP ${score*20}",color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(20.dp));Button({seed++;index=0;chosen=-1;score=0}){Text("NEW UNIQUE TEST")}};return}
 val q=questions[index]
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){Row{Text("${index+1}/$size",fontWeight=FontWeight.Black);Spacer(Modifier.weight(1f));Text("+20 XP",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)};Spacer(Modifier.height(7.dp));LinearProgressIndicator({(index+if(chosen>=0)1 else 0).toFloat()/size},Modifier.fillMaxWidth().height(8.dp));Spacer(Modifier.height(10.dp));Text("Class ${q.grade} • ${q.subject} • ${q.difficulty} • ${q.topic}",color=MaterialTheme.colorScheme.primary,fontSize=12.sp,fontWeight=FontWeight.Bold);AnimatedContent(q.id,label="q"){Card(Modifier.fillMaxWidth().padding(vertical=10.dp),shape=RoundedCornerShape(25.dp)){Text(q.text,Modifier.padding(21.dp),fontSize=19.sp,fontWeight=FontWeight.Black)}};q.options.forEachIndexed{ix,opt->val good=chosen>=0&&ix==q.answer;val bad=chosen==ix&&ix!=q.answer;Card(onClick={if(chosen<0){chosen=ix;record(ix==q.answer);if(ix==q.answer)score++;toast(if(ix==q.answer)"Correct! +20 XP 🎉" else "Review the explanation 📘")}},modifier=Modifier.fillMaxWidth().padding(vertical=4.dp),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=if(good)Color(0xFF00A884).copy(.16f)else if(bad)Color(0xFFE83E8C).copy(.16f)else MaterialTheme.colorScheme.surfaceVariant)){Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically){Text("${'A'+ix}",fontWeight=FontWeight.Black,color=if(good)Color(0xFF00A884)else MaterialTheme.colorScheme.primary);Spacer(Modifier.width(12.dp));Text(opt,fontWeight=FontWeight.SemiBold)}}};AnimatedVisibility(chosen>=0,enter=scaleIn()+fadeIn()){Card(Modifier.fillMaxWidth().padding(top=10.dp),shape=RoundedCornerShape(19.dp)){Column(Modifier.padding(16.dp)){Text(if(chosen==q.answer)"✓ Correct answer" else "✕ Review this one",fontWeight=FontWeight.Black);Text(q.explanation,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(9.dp));Button({index++;chosen=-1},modifier=Modifier.fillMaxWidth()){Text(if(index==size-1)"FINISH TEST" else "NEXT UNIQUE QUESTION →")}}}}}
}
@Composable private fun ZBigCard(c:Color,title:String,body:String,go:()->Unit){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(27.dp)){Box(Modifier.background(Brush.linearGradient(listOf(c,c.copy(.72f))))){Column(Modifier.padding(21.dp)){Text(title,color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Black);Text(body,color=Color.White.copy(.86f));Spacer(Modifier.height(13.dp));Button(go,colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=c),modifier=Modifier.fillMaxWidth()){Text("ENTER ARENA",fontWeight=FontWeight.Black)}}}}}

@Composable private fun ZLibrary(notes:List<ZN>,tasks:List<ZT>,saveNotes:(List<ZN>)->Unit,saveTasks:(List<ZT>)->Unit){
 var tab by remember{mutableIntStateOf(0)};var title by remember{mutableStateOf("")};var body by remember{mutableStateOf("")};var taskText by remember{mutableStateOf("")}
 Column(Modifier.fillMaxSize().padding(16.dp)){
  Text("Revision Hub",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
  Row{FilterChip(tab==0,{tab=0},label={Text("Notes")});Spacer(Modifier.width(6.dp));FilterChip(tab==1,{tab=1},label={Text("Planner")})}
  Spacer(Modifier.height(10.dp))
  if(tab==0){
   OutlinedTextField(title,{title=it},label={Text("Note title")},modifier=Modifier.fillMaxWidth())
   Spacer(Modifier.height(7.dp));OutlinedTextField(body,{body=it},label={Text("Study note")},modifier=Modifier.fillMaxWidth().height(120.dp))
   Spacer(Modifier.height(7.dp));Button({if(title.isNotBlank()){saveNotes(notes+ZN(System.currentTimeMillis(),title,body));title="";body=""}},modifier=Modifier.fillMaxWidth()){Text("SAVE NOTE")}
   Spacer(Modifier.height(10.dp));LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(notes.reversed(),key={it.id}){n->NoteCard(n){saveNotes(notes.filterNot{it.id==n.id})}}}
  }else{
   OutlinedTextField(taskText,{taskText=it},label={Text("New study task")},modifier=Modifier.fillMaxWidth())
   Spacer(Modifier.height(7.dp));Button({if(taskText.isNotBlank()){saveTasks(tasks+ZT(System.currentTimeMillis(),taskText,false));taskText=""}},modifier=Modifier.fillMaxWidth()){Text("ADD TASK")}
   Spacer(Modifier.height(10.dp));LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(tasks,key={it.id}){t->TaskCard(t,{v->saveTasks(tasks.map{if(it.id==t.id)it.copy(done=v)else it})},{saveTasks(tasks.filterNot{it.id==t.id})})}}
  }
 }
}
@Composable private fun NoteCard(n:ZN,delete:()->Unit){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(15.dp)){Text(n.title,fontWeight=FontWeight.Black);Text(n.body,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text("DELETE",Modifier.clickable{delete()}.padding(top=7.dp),fontSize=11.sp,color=zColors[4],fontWeight=FontWeight.Bold)}}}
@Composable private fun TaskCard(t:ZT,toggle:(Boolean)->Unit,delete:()->Unit){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically){Checkbox(t.done,toggle);Text(t.text,Modifier.weight(1f),fontWeight=FontWeight.Bold);Text("✕",Modifier.clickable{delete()})}}}

@Composable private fun ZProfile(g:Int,xp:Int,ok:Int,attempted:Int,dark:Boolean,setDark:(Boolean)->Unit,change:()->Unit){val acc=if(attempted==0)0 else ok*100/attempted;Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){Text("Profile & Stats",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black);ZBigCard(zColors[1],"LEVEL ${xp/500+1}","$xp XP • $acc% accuracy • ${if(g<0)"All Classes" else "Class $g"}"){};Spacer(Modifier.height(12.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ZStat("Attempted",attempted.toString(),zColors[0],Modifier.weight(1f));ZStat("Correct",ok.toString(),zColors[2],Modifier.weight(1f))};Spacer(Modifier.height(10.dp));Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Text("Dark theme",fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Switch(dark,setDark)}};Spacer(Modifier.height(8.dp));Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(16.dp)){Text("Narendar Hub",fontWeight=FontWeight.Black);Text("Offline learning • unique MCQ engine • study, practice and revision.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}};Spacer(Modifier.height(8.dp));OutlinedButton(change,modifier=Modifier.fillMaxWidth()){Text("CHANGE CLASS")}}
}

private fun zTest(maxGrade:Int,subject:String,difficulty:String,size:Int,seed:Int):List<ZQ>{val all=ArrayList<ZQ>();val ds=if(difficulty=="Mixed")listOf("Easy","Medium","Hard")else listOf(difficulty);for(g in 1..maxGrade)for(d in ds)all.addAll(zBank(g,subject,d,2500));return all.shuffled(kotlin.random.Random(seed*7919+subject.hashCode())).take(size)}
private fun zBank(g:Int,s:String,d:String,n:Int):List<ZQ>{val list=ArrayList<ZQ>(n);for(i in 0 until n)list.add(zQuestion(g,s,d,i));return list}
private fun zQuestion(g:Int,s:String,d:String,i:Int):ZQ{val n=i+1;val b=i+g*37+when(d){"Easy"->11;"Medium"->101;else->1001};val topic=when(s){"Mathematics"->listOf("Arithmetic","Fractions","Algebra","Geometry","Patterns")[i%5];"Science"->listOf("Matter","Life","Energy","Earth","Forces")[i%5];"English"->listOf("Grammar","Vocabulary","Tenses","Reading","Writing")[i%5];"Computer Science"->listOf("Hardware","Software","Programming","Networks","Cyber Safety")[i%5];"SST"->listOf("History","Geography","Civics","Economics","Society")[i%5];"Urdu"->listOf("قواعد","الفاظ","مطالعہ","ادب","تحریر")[i%5];"Physics"->listOf("Motion","Energy","Waves","Electricity","Forces")[i%5];"Chemistry"->listOf("Atoms","Matter","Reactions","Solutions","Elements")[i%5];else->listOf("Cells","Plants","Animals","Genetics","Ecology")[i%5]};return if(s=="Mathematics")zMath(g,d,topic,n,b)else zConcept(g,s,d,topic,n,b)}
private fun zMath(g:Int,d:String,t:String,n:Int,b:Int):ZQ{val mode=n%8;val a=b%89+g;val x=b*3%47+2;val ans=when(mode){0->a+x;1->a*x;2->abs(a-x);3->(a+x)%10;4->a+x+5;5->a*x+3;6->abs(a-x)+2;else->a*x%17};val text=when(mode){0->"Class $g Mathematics: calculate $a + $x. Set #$n";1->"Class $g Mathematics: calculate $a × $x. Set #$n";2->"Class $g Mathematics: find the positive difference of $a and $x. Set #$n";3->"Class $g Mathematics: find the remainder of ${a+x} ÷ 10. Set #$n";4->"Class $g Mathematics: $a increases by ${x+5}. What is the result? Set #$n";5->"Class $g Mathematics: multiply $a by $x, then add 3. Set #$n";6->"Class $g Mathematics: what is |$a − $x| + 2? Set #$n";else->"Class $g Mathematics: what is the remainder of ${a*x} ÷ 17? Set #$n"};return ZQ(g*10000+n,g,"Mathematics",d,t,text,listOf(ans.toString(),(ans+1).toString(),(if(ans>1)ans-1 else ans+2).toString(),(ans+3).toString()),0,"Solve the operation carefully. The correct result is $ans.")}
private fun zConcept(g:Int,s:String,d:String,t:String,n:Int,b:Int):ZQ{val data=mapOf("Science" to listOf("evaporation","photosynthesis","gravity","condensation","friction","ecosystem","reflection","magnetism"),"English" to listOf("quickly","beautiful","because","carefully","although","honest","between","however"),"Computer Science" to listOf("CPU","RAM","browser","algorithm","firewall","compiler","keyboard","router"),"SST" to listOf("map","constitution","river","trade","community","citizen","climate","culture"),"Urdu" to listOf("اسم","فعل","صفت","جملہ","محاورہ","کہانی","شاعر","مضمون"),"Physics" to listOf("velocity","force","energy","frequency","voltage","current","mass","pressure"),"Chemistry" to listOf("atom","molecule","element","mixture","reaction","solution","electron","acid"),"Biology" to listOf("cell","root","organ","gene","ecosystem","chlorophyll","tissue","habitat"));val set=data[s]?:data["Science"]!!;val c=set[b%set.size];val opts=listOf(c,set[(b+1)%set.size],set[(b+3)%set.size],set[(b+5)%set.size]);val prompts=listOf("Which $s concept best matches the topic $t?","Choose the most accurate $s term for this Class $g question.","Which option correctly describes the Class $g $s idea?","Select the best answer for this $t exercise.");return ZQ(g*10000+n,g,s,d,t,"${prompts[b%prompts.size]} Set #$n",opts,0,"The correct answer is $c. Review the definition of $t and compare the alternatives.")}

private fun zLoadNotes(p:android.content.SharedPreferences):List<ZN>{return try{val a=JSONArray(p.getString("notes","[]"));List(a.length()){val o=a.getJSONObject(it);ZN(o.getLong("id"),o.getString("title"),o.getString("body"))}}catch(_:Exception){emptyList()}}
private fun zLoadTasks(p:android.content.SharedPreferences):List<ZT>{return try{val a=JSONArray(p.getString("tasks","[]"));List(a.length()){val o=a.getJSONObject(it);ZT(o.getLong("id"),o.getString("text"),o.getBoolean("done"))}}catch(_:Exception){emptyList()}}
private fun zNotesJson(v:List<ZN>)=JSONArray().apply{v.forEach{put(JSONObject().apply{put("id",it.id);put("title",it.title);put("body",it.body)})}}.toString()
private fun zTasksJson(v:List<ZT>)=JSONArray().apply{v.forEach{put(JSONObject().apply{put("id",it.id);put("text",it.text);put("done",it.done)})}}.toString()
