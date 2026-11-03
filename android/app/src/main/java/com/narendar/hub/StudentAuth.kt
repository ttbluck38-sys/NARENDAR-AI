package com.narendar.hub

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal data class StudentSession(val accessToken:String,val refreshToken:String,val userId:String,val email:String)
internal object StudentAuth {
 private const val PREFS="studypilot_auth"
 private fun prefs(c:Context)=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
 fun token(c:Context)=prefs(c).getString("access_token","").orEmpty()
 fun userId(c:Context)=prefs(c).getString("user_id","").orEmpty()
 fun email(c:Context)=prefs(c).getString("email","").orEmpty()
 fun restore(c:Context):Boolean{val p=prefs(c);val refresh=p.getString("refresh_token","").orEmpty();if(refresh.isBlank())return false;if(p.getLong("expires_at",0L)>System.currentTimeMillis()/1000+120)return true;return try{authRequest(c,"/auth/v1/token?grant_type=refresh_token",JSONObject().put("refresh_token",refresh));true}catch(_:Exception){logout(c);false}}
 fun logout(c:Context){prefs(c).edit().clear().apply()}
 fun signIn(c:Context,email:String,password:String)=authRequest(c,"/auth/v1/token?grant_type=password",JSONObject().put("email",email.trim()).put("password",password))
 fun signUp(c:Context,email:String,password:String,name:String)=authRequest(c,"/auth/v1/signup",JSONObject().put("email",email.trim()).put("password",password).put("data",JSONObject().put("display_name",name.trim())))
 private fun authRequest(c:Context,path:String,body:JSONObject):StudentSession{
  val conn=(URL(AiConfig.SUPABASE_URL+path).openConnection() as HttpURLConnection).apply{requestMethod="POST";connectTimeout=15000;readTimeout=25000;doOutput=true;setRequestProperty("Content-Type","application/json");setRequestProperty("apikey",AiConfig.SUPABASE_PUBLISHABLE_KEY)}
  try{
   conn.outputStream.use{it.write(body.toString().toByteArray(Charsets.UTF_8))}
   val code=conn.responseCode;val raw=(if(code in 200..299)conn.inputStream else conn.errorStream)?.bufferedReader()?.use{it.readText()}.orEmpty()
   val j=JSONObject(raw.ifBlank{"{}"});if(code !in 200..299)throw IllegalStateException(j.optString("msg",j.optString("message",j.optString("error_description","Sign in failed ($code)."))))
   val user=j.optJSONObject("user")?:throw IllegalStateException("Check your email confirmation, then sign in.")
   val session=StudentSession(j.optString("access_token"),j.optString("refresh_token"),user.optString("id"),user.optString("email"))
   if(session.accessToken.isBlank()||session.userId.isBlank())throw IllegalStateException("Email confirmation may be required. Sign in after confirming your email.")
   prefs(c).edit().putString("access_token",session.accessToken).putString("refresh_token",session.refreshToken).putString("user_id",session.userId).putString("email",session.email).putLong("expires_at",System.currentTimeMillis()/1000+j.optLong("expires_in",3600)).apply()
   return session
  }finally{conn.disconnect()}
 }
 fun saveProfile(c:Context,name:String,grade:Int,language:String,preferences:JSONObject=JSONObject(),memory:JSONObject=JSONObject()){
  val body=JSONObject().put("user_id",userId(c)).put("display_name",name).put("grade",grade.coerceIn(1,12)).put("preferred_language",language).put("learning_preferences",preferences).put("student_memory",memory).put("updated_at",java.time.Instant.now().toString())
  request(c,"/rest/v1/student_profiles?on_conflict=user_id","POST",body,"resolution=merge-duplicates,return=minimal")
 }
 fun logEvent(c:Context,type:String,subject:String,topic:String,question:String,selected:String="",correct:String="",isCorrect:Boolean?=null,details:JSONObject=JSONObject()){
  if(token(c).isBlank())return
  val body=JSONObject().put("user_id",userId(c)).put("event_type",type).put("subject",subject).put("topic",topic).put("question",question).put("selected_answer",selected).put("correct_answer",correct).put("details",details)
  if(isCorrect!=null)body.put("is_correct",isCorrect)
  request(c,"/rest/v1/learning_events","POST",body,"return=minimal")
 }
 fun request(c:Context,path:String,method:String,body:JSONObject?,prefer:String?=null):String{
  val conn=(URL(AiConfig.SUPABASE_URL+path).openConnection() as HttpURLConnection).apply{requestMethod=method;connectTimeout=15000;readTimeout=30000;doInput=true;setRequestProperty("Content-Type","application/json");setRequestProperty("apikey",AiConfig.SUPABASE_PUBLISHABLE_KEY);setRequestProperty("Authorization","Bearer "+token(c));if(prefer!=null)setRequestProperty("Prefer",prefer);if(body!=null)doOutput=true}
  try{if(body!=null)conn.outputStream.use{it.write(body.toString().toByteArray(Charsets.UTF_8))};val code=conn.responseCode;val raw=(if(code in 200..299)conn.inputStream else conn.errorStream)?.bufferedReader()?.use{it.readText()}.orEmpty();if(code !in 200..299)throw IllegalStateException("Profile sync failed ($code): "+JSONObject(raw.ifBlank{"{}"}).optString("message","Try again."));return raw}finally{conn.disconnect()}
 }
}
