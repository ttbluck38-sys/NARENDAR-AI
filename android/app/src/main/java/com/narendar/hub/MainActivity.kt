package com.narendar.hub

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private val crashPrefs by lazy {
        getSharedPreferences("studypilot_crash_report", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            // Keep a local crash trace so the next launch can show the real cause
            // instead of silently entering the same crash loop again.
            crashPrefs.edit()
                .putString("last_crash", Log.getStackTraceString(error).take(12000))
                .commit()
            previousHandler?.uncaughtException(thread, error)
        }

        setContent {
            var crashReport by remember {
                mutableStateOf(crashPrefs.getString("last_crash", null))
            }
            if (!crashReport.isNullOrBlank()) {
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text("StudyPilot AI recovered a crash report") },
                    text = {
                        Column(
                            Modifier.heightIn(max = 360.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                crashReport.orEmpty(),
                                maxLines = 120,
                                overflow = TextOverflow.Clip,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            crashPrefs.edit().remove("last_crash").apply()
                            crashReport = null
                        }) { Text("Continue to app") }
                    },
                    dismissButton = {
                        TextButton(onClick = { finish() }) { Text("Close") }
                    }
                )
            } else {
                // Pass the Activity context to Compose, not applicationContext.
                FixedHub(this@MainActivity)
            }
        }
    }
}
