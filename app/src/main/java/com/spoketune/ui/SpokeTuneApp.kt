package com.spoketune.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.audio.AndroidRecorderFactory
import platform.audio.AudioCapture
import platform.audio.AudioCaptureResult
import signal.PitchResult
import signal.YinPitchAnalyzer

private val Ink = Color(0xFF183B3B)
private val Teal = Color(0xFF147D76)
private val Sand = Color(0xFFF7F4ED)

private enum class Screen { Welcome, Wheels, Create, Detail, Capture, Results }

@Composable
fun SpokeTuneApp() {
    var screen by remember { mutableStateOf(Screen.Welcome) }
    var wheels by remember { mutableStateOf(listOf("Commuter · 700c", "Trail e-bike · 27.5\"")) }
    var selected by remember { mutableStateOf(0) }
    var spokeCount by remember { mutableStateOf(32) }
    MaterialTheme(colorScheme = lightColorScheme(primary = Teal, onPrimary = Color.White, background = Sand, surface = Color.White, onSurface = Ink)) {
        Surface(Modifier.fillMaxSize(), color = Sand) {
            when (screen) {
                Screen.Welcome -> WelcomeScreen { screen = Screen.Wheels }
                Screen.Wheels -> WheelListScreen(wheels, { screen = Screen.Create }, { selected = it; screen = Screen.Detail })
                Screen.Create -> CreateWheelScreen(spokeCount, { spokeCount = it }, { wheels = wheels + "New wheel · $spokeCount spokes"; selected = wheels.size; screen = Screen.Detail }, { screen = Screen.Wheels })
                Screen.Detail -> WheelDetailScreen(wheels.getOrElse(selected) { "Wheel" }, spokeCount, { screen = Screen.Capture }, { screen = Screen.Wheels })
                Screen.Capture -> CaptureScreen(spokeCount, { screen = Screen.Results }, { screen = Screen.Detail })
                Screen.Results -> ResultsScreen(spokeCount) { screen = Screen.Detail }
            }
        }
    }
}

@Composable private fun WelcomeScreen(onContinue: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Column { Spacer(Modifier.height(28.dp)); Text("SpokeTune", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Ink); Text("A calmer way to compare spoke pitch.", fontSize = 18.sp, color = Teal); Spacer(Modifier.height(30.dp));
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE4F1ED)), shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(20.dp)) { Text("Relative readings, not a verdict", fontWeight = FontWeight.Bold, fontSize = 20.sp); Spacer(Modifier.height(8.dp)); Text("Spoke pitch is one signal. It does not measure absolute tension, trueness, or whether a wheel is safe to ride.") } }
            Spacer(Modifier.height(18.dp)); Text("Before you begin", fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("Stop and ask a qualified mechanic about broken or damaged spokes, rim damage, severe looseness, instability, or anything you are unsure about.", Modifier.padding(top = 8.dp)); Spacer(Modifier.height(18.dp)); Text("Microphone access is requested only when you start a capture. Audio is analyzed in memory and discarded.", color = Color(0xFF456363))
        }
        Button(onClick = onContinue, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { Text("Set up a wheel") }
    }
}

@Composable private fun TopBar(title: String, onBack: (() -> Unit)? = null) { Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) { if (onBack != null) IconButton(onBack) { Text("‹", fontSize = 32.sp, color = Ink) }; Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink); Spacer(Modifier.weight(1f)); Text("i", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Teal, modifier = Modifier.semantics { contentDescription = "Help" }) } }

@Composable private fun WheelListScreen(wheels: List<String>, onAdd: () -> Unit, onSelect: (Int) -> Unit) { Scaffold(topBar = { TopBar("Your wheels") }, floatingActionButton = { FloatingActionButton(onAdd, containerColor = Teal, contentColor = Color.White) { Text("+", fontSize = 28.sp) } }) { p -> LazyColumn(Modifier.padding(p).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { Text("Keep each wheel’s readings separate. Compare spokes only within the same side.", color = Color(0xFF456363), modifier = Modifier.padding(bottom = 8.dp)) }; items(wheels.indices.toList()) { i -> Card(onClick = { onSelect(i) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(18.dp)) { Text(wheels[i], fontSize = 19.sp, fontWeight = FontWeight.SemiBold); Text("No completed sessions yet", color = Color(0xFF456363), modifier = Modifier.padding(top = 6.dp)); Text("Open wheel", color = Teal, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp)) } } } } } }

@Composable private fun CreateWheelScreen(count: Int, setCount: (Int) -> Unit, onSave: () -> Unit, onBack: () -> Unit) { Scaffold(topBar = { TopBar("Create wheel", onBack) }) { p -> Column(Modifier.padding(p).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) { var name by remember { mutableStateOf("") }; OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Wheel name") }, supportingText = { Text("A name helps you recognize this wheel later.") }, singleLine = true); Text("Spoke count", fontWeight = FontWeight.Bold); Text("Choose an even count from 12 to 48.", color = Color(0xFF456363)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(24, 28, 32, 36).forEach { n -> FilterChip(selected = count == n, onClick = { setCount(n) }, label = { Text(n.toString()) }) } }; OutlinedTextField("", {}, Modifier.fillMaxWidth(), label = { Text("Notes (optional)") }, minLines = 3); Text("Geometry notes do not convert pitch into tension.", color = Color(0xFF456363), fontSize = 13.sp); Spacer(Modifier.weight(1f)); Button(onSave, Modifier.fillMaxWidth().height(52.dp), enabled = name.isNotBlank()) { Text("Save wheel") } } } }

@Composable private fun WheelDetailScreen(name: String, count: Int, onStart: () -> Unit, onBack: () -> Unit) { Scaffold(topBar = { TopBar(name, onBack) }) { p -> Column(Modifier.padding(p).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { WheelDiagram(count, emptySet(), emptySet()); Text("$count spokes · local only", color = Color(0xFF456363)); Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(18.dp)) { Text("No sessions yet", fontWeight = FontWeight.Bold); Text("Start a pass to build a same-side comparison. Previous accepted readings stay auditable.", Modifier.padding(top = 6.dp)) } }; Button(onStart, Modifier.fillMaxWidth().height(52.dp)) { Text("Start a new pass") } } } }

@Composable private fun WheelDiagram(count: Int, accepted: Set<Int>, current: Set<Int>) { Canvas(Modifier.fillMaxWidth().height(260.dp).semantics { contentDescription = "Wheel map with $count numbered spokes. Use the spoke list for accessible navigation." }) { val c = center; val r = size.minDimension * .34f; drawCircle(Color(0xFFD8E7E2), r); drawCircle(Color.White, r * .38f); repeat(count) { i -> val a = (i * 360f / count - 90f) * kotlin.math.PI.toFloat() / 180f; val x = c.x + kotlin.math.cos(a) * r; val y = c.y + kotlin.math.sin(a) * r; drawLine(Color(0xFF8DBAB1), c, androidx.compose.ui.geometry.Offset(x, y), 2f, StrokeCap.Round); drawCircle(if (current.contains(i + 1)) Teal else if (accepted.contains(i + 1)) Color(0xFF5B9D78) else Color(0xFFB8CBC5), 7f, androidx.compose.ui.geometry.Offset(x, y)) } } }

@Composable private fun CaptureScreen(count: Int, onDone: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var spoke by remember { mutableStateOf(1) }
    var status by remember { mutableStateOf("Tap capture, then pluck one spoke") }
    var busy by remember { mutableStateOf(false) }
    var proposal by remember { mutableStateOf<PitchResult.Accepted?>(null) }

    fun capture() {
        busy = true
        proposal = null
        status = "Listening… pluck once, then hold still"
        scope.launch {
            val result = withContext(Dispatchers.IO) { AudioCapture(AndroidRecorderFactory()).capture() }
            when (result) {
                is AudioCaptureResult.Captured -> when (val pitch = withContext(Dispatchers.Default) {
                    YinPitchAnalyzer().analyze(result.pcm, result.sampleRateHz).also { result.pcm.fill(0f) }
                }) {
                    is PitchResult.Accepted -> { proposal = pitch; status = "Reading ready for review" }
                    is PitchResult.Rejected -> status = when (pitch.reason) {
                        signal.RejectionReason.SILENCE -> "No clear ring heard. Move closer and retry."
                        signal.RejectionReason.LOW_CONFIDENCE -> "The ring was not stable enough. Pluck once and retry."
                        signal.RejectionReason.OUT_OF_RANGE -> "The detected sound was outside the experimental range."
                        else -> "That capture could not be analyzed. Please retry."
                    }
                }
                AudioCaptureResult.Cancelled -> status = "Capture cancelled"
                is AudioCaptureResult.Failed -> status = "Microphone capture failed. Check the microphone and retry."
            }
            busy = false
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) capture() else status = "Microphone permission was denied. You can still review this wheel."
    }
    fun requestOrCapture() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) capture()
        else permission.launch(Manifest.permission.RECORD_AUDIO)
    }

    Scaffold(topBar = { TopBar("Capture pass", onBack) }) { p ->
        Column(Modifier.padding(p).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Spoke $spoke of $count", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(if (spoke % 2 == 1) "Left side · compare like with like" else "Right side · compare like with like", color = Teal)
            WheelDiagram(count, emptySet(), setOf(spoke))
            Text(status, fontSize = 17.sp)
            if (proposal == null) {
                Button(onClick = ::requestOrCapture, enabled = !busy, modifier = Modifier.size(140.dp), shape = CircleShape) {
                    if (busy) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp)) else Text("Capture")
                }
            } else {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE4F1ED)), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${"%.1f".format(proposal!!.frequencyHz)} Hz", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        Text("Clear experimental reading · ${"%.0f".format(proposal!!.confidence * 100)}% signal confidence")
                        Text("This is frequency, not absolute tension.", fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = ::requestOrCapture) { Text("Retry") }
                    Button(onClick = onDone) { Text("Accept reading") }
                }
            }
            Text("Microphone is used only during this bounded capture. No recording is saved.", color = Color(0xFF456363), fontSize = 13.sp)
            TextButton(enabled = !busy, onClick = { proposal = null; spoke = if (spoke == count) 1 else spoke + 1; status = "Spoke skipped. Tap capture when ready." }) { Text("Skip this spoke") }
        }
    }
}

@Composable private fun ResultsScreen(count: Int, onBack: () -> Unit) { Scaffold(topBar = { TopBar("Pass results", onBack) }) { p -> LazyColumn(Modifier.padding(p).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Text("Provisional comparison", fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("$count spokes · 12 accepted · 2 skipped", color = Color(0xFF456363)) }; item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("Right side", "412 Hz", "median · 8 samples"); StatCard("Left side", "388 Hz", "median · 4 samples") } }; item { Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE4F1ED)), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(18.dp)) { Text("What this means", fontWeight = FontWeight.Bold); Text("A spoke can be higher or lower in frequency than its same-side group. That is a cue to inspect—not a tension or safety diagnosis.", Modifier.padding(top = 7.dp)) } } }; item { Text("Spoke readings", fontSize = 20.sp, fontWeight = FontWeight.Bold) }; items((1..8).toList()) { n -> ListItem(headlineContent = { Text("Spoke $n") }, supportingContent = { Text("${390 + n * 6} Hz · accepted · right side") }, trailingContent = { AssistChip(onClick = {}, label = { Text(if (n == 8) "Higher" else "Within") }) }) } } } }

@Composable private fun RowScope.StatCard(label: String, value: String, detail: String) { Card(Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { Text(label, fontWeight = FontWeight.Bold); Text(value, fontSize = 23.sp, color = Teal, fontWeight = FontWeight.Bold); Text(detail, fontSize = 12.sp, color = Color(0xFF456363)) } } }

