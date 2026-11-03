package com.narendar.hub

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Composable internal fun StudentLogin(onSignedIn:()->Unit){
 val context=LocalContext.current;val scope=rememberCoroutineScope()
 var signup by remember{mutableStateOf(false)};var email by remember{mutableStateOf("")};var password by remember{mutableStateOf("")};var name by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
  Text("StudyPilot AI",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary)
  Text("Your learning space. Your progress. Your AI.",style=MaterialTheme.typography.bodyMedium)
  Spacer(Modifier.height(24.dp));Text(if(signup)"Create your student account" else "Welcome back",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
  if(signup)OutlinedTextField(name,{name=it},label={Text("Your name")},singleLine=true,modifier=Modifier.fillMaxWidth())
  OutlinedTextField(email,{email=it},label={Text("Email")},singleLine=true,modifier=Modifier.fillMaxWidth())
  OutlinedTextField(password,{password=it},label={Text("Password")},singleLine=true,modifier=Modifier.fillMaxWidth())
  if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
  Spacer(Modifier.height(12.dp))
  Button(enabled=!busy&&email.contains("@")&&password.length>=6&&(!signup||name.isNotBlank()),onClick={busy=true;error="";scope.launch{try{withContext(Dispatchers.IO){if(signup)StudentAuth.signUp(context,email,password,name)else StudentAuth.signIn(context,email,password)};onSignedIn()}catch(e:Exception){error=e.message?: "Could not sign in."}finally{busy=false}}},modifier=Modifier.fillMaxWidth()){
   if(busy)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp) else Text(if(signup)"CREATE ACCOUNT" else "SIGN IN")
  }
  TextButton(onClick={signup=!signup;error=""}){Text(if(signup)"Already have an account? Sign in" else "New student? Create account")}
  Text("Your account and learning progress are private to your sign-in.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
 }
}
private data class ChatLine(val mine:Boolean,val text:String)
@Composable internal fun StudyPilotAIPage(grade:Int,language:String,studentName:String){
 val context=LocalContext.current;val scope=rememberCoroutineScope()
 var input by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")}
 var lines by remember{mutableStateOf(listOf(ChatLine(false,"Hi ${studentName.ifBlank{"there"}}! I’m StudyPilot AI. Ask me about any school topic, a question, or something you want to learn.")))}
 Column(Modifier.fillMaxSize().padding(12.dp)){
  Text("StudyPilot AI",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
  Text("Your learning companion • Class $grade • $language",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  LazyColumn(Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(vertical=12.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
   items(lines){line->Row(Modifier.fillMaxWidth(),horizontalArrangement=if(line.mine)Arrangement.End else Arrangement.Start){Surface(color=if(line.mine)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,shape=RoundedCornerShape(18.dp),modifier=Modifier.widthIn(max=320.dp)){Text(line.text,Modifier.padding(13.dp))}}}
   if(busy)item{Row(verticalAlignment=Alignment.CenterVertically){CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp);Spacer(Modifier.width(8.dp));Text("StudyPilot is thinking…")}}
  }
  if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
  Row(verticalAlignment=Alignment.CenterVertically){
   OutlinedTextField(input,{input=it},modifier=Modifier.weight(1f),placeholder={Text("Ask StudyPilot AI…")},maxLines=4)
   Spacer(Modifier.width(6.dp))
   Button(enabled=!busy&&input.isNotBlank(),onClick={val message=input.trim();input="";lines=lines+ChatLine(true,message);busy=true;error="";scope.launch{try{val answer=withContext(Dispatchers.IO){requestStudyPilotChat(context,message,grade,language)};lines=lines+ChatLine(false,answer)}catch(e:Exception){error=e.message?: "Please try again."}finally{busy=false}}}){Text("Send")}
  }
 }
}
private fun requestStudyPilotChat(context:android.content.Context,message:String,grade:Int,language:String):String{
 val url=AiConfig.SUPABASE_URL.trimEnd()+"/functions/v1/study-chat"
 val body=JSONObject().put("message",message).put("grade",grade).put("language",language)
 val conn=(URL(url).openConnection() as HttpURLConnection).apply{requestMethod="POST";connectTimeout=20000;readTimeout=60000;doOutput=true;setRequestProperty("Content-Type","application/json");setRequestProperty("apikey",AiConfig.SUPABASE_PUBLISHABLE_KEY);setRequestProperty("Authorization","Bearer "+StudentAuth.token(context))}
 try{conn.outputStream.use{it.write(body.toString().toByteArray(Charsets.UTF_8))};val code=conn.responseCode;val raw=(if(code in 200..299)conn.inputStream else conn.errorStream)?.bufferedReader()?.use{it.readText()}.orEmpty();val j=JSONObject(raw.ifBlank{"{}"});if(code !in 200..299)throw IllegalStateException(j.optString("error","StudyPilot AI request failed ($code)."));return j.optString("answer").takeIf{it.isNotBlank()}?:throw IllegalStateException("No answer returned.")}finally{conn.disconnect()}
}
