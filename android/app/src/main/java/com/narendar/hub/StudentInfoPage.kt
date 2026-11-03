package com.narendar.hub

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable internal fun StudentInfoPage(name:String,grade:Int,language:String,onName:(String)->Unit,onLanguage:(String)->Unit,onLogout:()->Unit){
 val context=LocalContext.current;val scope= rememberCoroutineScope()
 var localName by remember(name){mutableStateOf(name)}
 var answerStyle by remember{mutableStateOf("Step-by-step with examples")}
 var interests by remember{mutableStateOf("")}
 var goals by remember{mutableStateOf("")}
 var saving by remember{mutableStateOf(false)}
 var status by remember{mutableStateOf("")}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)){
  Text("My Student Info",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
  Text("Your profile helps StudyPilot adjust explanations to your learning. You can change it any time.",color=MaterialTheme.colorScheme.onSurfaceVariant)
  Spacer(Modifier.height(14.dp))
  OutlinedTextField(localName,{localName=it},label={Text("Your name")},modifier=Modifier.fillMaxWidth())
  Text("Class $grade",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
  Text("Preferred language",style=MaterialTheme.typography.titleSmall)
  Row{listOf("English","Urdu","Sindhi").forEach{FilterChip(selected=language==it,onClick={onLanguage(it)},label={Text(it)},modifier=Modifier.padding(end=5.dp))}}
  Text("How should StudyPilot explain?",style=MaterialTheme.typography.titleSmall)
  listOf("Step-by-step with examples","Short and simple","Detailed explanation","Ask me questions first").forEach{FilterChip(selected=answerStyle==it,onClick={answerStyle=it},label={Text(it)},modifier=Modifier.padding(vertical=3.dp))}
  OutlinedTextField(interests,{interests=it},label={Text("Subjects or topics I enjoy")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(goals,{goals=it},label={Text("My learning goals")},modifier=Modifier.fillMaxWidth(),minLines=2)
  Spacer(Modifier.height(12.dp))
  Button(enabled=!saving,onClick={saving=true;status="";onName(localName);scope.launch{try{withContext(Dispatchers.IO){StudentAuth.saveProfile(context,localName,grade,language,JSONObject().put("answer_style",answerStyle).put("interests",interests).put("goals",goals))};status="Your student information is saved securely."}catch(e:Exception){status=e.message?:"Could not save profile."}finally{saving=false}}},modifier=Modifier.fillMaxWidth()){Text(if(saving)"Saving…" else "Save student information")}
  if(status.isNotBlank())Text(status,color=MaterialTheme.colorScheme.primary)
  Spacer(Modifier.height(12.dp))
  OutlinedButton(onClick=onLogout,modifier=Modifier.fillMaxWidth()){Text("Sign out")}
 }
}
