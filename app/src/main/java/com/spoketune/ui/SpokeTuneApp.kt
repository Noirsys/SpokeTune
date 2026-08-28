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
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.spoketune.app.session.CaptureSession
import com.spoketune.core.domain.WheelProfile
import com.spoketune.core.domain.Measurement
import com.spoketune.app.BuildConfig

private val Ink = Color(0xFF183B3B)
private val Teal = Color(0xFF147D76)
private val Sand = Color(0xFFF7F4ED)
private val Moss = Color(0xFF5B7567)
private val WarmWhite = Color(0xFFFFFCF6)
private val Forest = Color(0xFF0D5148)
private val Mint = Color(0xFFDCEFE7)
private val Clay = Color(0xFFB76B45)
private val Charcoal = Color(0xFF263938)
private val Muted = Color(0xFF637771)
private val Rule = Color(0xFFD6E2DC)

private val WorkshopTypography = Typography(
    displayLarge = androidx.compose.ui.text.TextStyle(fontSize = 38.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.7).sp),
    headlineLarge = androidx.compose.ui.text.TextStyle(fontSize = 30.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.3).sp),
    headlineSmall = androidx.compose.ui.text.TextStyle(fontSize = 21.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
    titleMedium = androidx.compose.ui.text.TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = .5.sp)
)

private enum class Screen { Welcome, Wheels, Create, Detail, Capture, Results }
private data class UiWheel(val name: String, val sizeLabel: String, val spokeCount: Int)

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier = modifier, fontSize = 11.sp, letterSpacing = 1.6.sp,
        fontWeight = FontWeight.Bold, color = Teal)
}

@Composable
private fun StepBadge(number: String, title: String, detail: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Surface(shape = CircleShape, color = Forest, modifier = Modifier.size(30.dp)) {
            Box(contentAlignment = Alignment.Center) { Text(number, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun WorkshopCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = WarmWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun StatusPill(text: String, positive: Boolean = true) {
    Surface(shape = RoundedCornerShape(50), color = if (positive) Mint else Color(0xFFF7E7DE)) {
        Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape), contentAlignment = Alignment.Center) { Surface(color = if (positive) Forest else Clay) {} }
            Text(text, color = if (positive) Forest else Color(0xFF86482F), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 7.dp))
        }
    }
}

@Composable
private fun ProgressPill(current: Int, total: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("PASS PROGRESS", fontSize = 11.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Bold, color = Muted)
        Spacer(Modifier.weight(1f))
        Text("$current / $total", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Forest)
    }
    LinearProgressIndicator(progress = { current.toFloat() / total.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(6.dp)), color = Teal, trackColor = Rule)
}

@Composable
fun SpokeTuneApp() {
    var screen by remember { mutableStateOf(Screen.Welcome) }
    var wheels by remember { mutableStateOf(listOf(
        UiWheel("Commuter", "700c", 32),
        UiWheel("Trail e-bike", "27-inch fat tire", 36),
    )) }
    var selected by remember { mutableStateOf(0) }
    var spokeCount by remember { mutableStateOf(32) }
    var session by remember { mutableStateOf<CaptureSession?>(null) }
    MaterialTheme(colorScheme = lightColorScheme(primary = Teal, onPrimary = Color.White, background = Sand, surface = WarmWhite, onSurface = Ink, surfaceVariant = Color(0xFFE8EFE9), onSurfaceVariant = Moss, secondary = Clay, outline = Rule), typography = WorkshopTypography) {
        Surface(Modifier.fillMaxSize(), color = Sand) {
            when (screen) {
                Screen.Welcome -> WelcomeScreen { screen = Screen.Wheels }
                Screen.Wheels -> WheelListScreen(wheels, { screen = Screen.Create }, { selected = it; screen = Screen.Detail })
                Screen.Create -> CreateWheelScreen(spokeCount, { spokeCount = it }, { name, count -> wheels = wheels + UiWheel(name, "Custom wheel", count); selected = wheels.size; screen = Screen.Detail }, { screen = Screen.Wheels })
                Screen.Detail -> wheels.getOrNull(selected)?.let { wheel -> WheelDetailScreen("${wheel.name} · ${wheel.sizeLabel}", wheel.spokeCount, { session = CaptureSession(WheelProfile(name = wheel.name, spokeCount = wheel.spokeCount)); screen = Screen.Capture }, { screen = Screen.Wheels }) }
                Screen.Capture -> session?.let { CaptureScreen(it, { screen = Screen.Results }, { screen = Screen.Detail }) }
                Screen.Results -> session?.let { ResultsScreen(it) { screen = Screen.Detail } }
            }
        }
    }
}

@Composable private fun WelcomeScreen(onContinue: () -> Unit) {
    Scaffold(
        containerColor = Sand,
        bottomBar = {
            Surface(color = Sand) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp).navigationBarsPadding().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                ) { Text("Set up a wheel", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 20.dp),
        ) {
            item {
                Text("SPOKETUNE", fontSize = 14.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold, color = Teal); Spacer(Modifier.height(10.dp)); Text("Hear the wheel.\nUnderstand the pattern.", fontSize = 34.sp, lineHeight = 39.sp, fontWeight = FontWeight.Bold, color = Ink); Text("A calmer way to compare spoke pitch.", fontSize = 18.sp, color = Moss, modifier = Modifier.padding(top = 12.dp)); Spacer(Modifier.height(30.dp));
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE4F1ED)), shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(20.dp)) { Text("Relative readings, not a verdict", fontWeight = FontWeight.Bold, fontSize = 20.sp); Spacer(Modifier.height(8.dp)); Text("Spoke pitch is one signal. It does not measure absolute tension, trueness, or whether a wheel is safe to ride.") } }
                Spacer(Modifier.height(22.dp)); SectionLabel("The simple loop"); Spacer(Modifier.height(14.dp)); StepBadge("1", "Set up the wheel", "Name it and choose its spoke count."); Spacer(Modifier.height(14.dp)); StepBadge("2", "Tap and pluck", "One spoke at a time, one clean ring."); Spacer(Modifier.height(14.dp)); StepBadge("3", "Read the pattern", "Compare each side with itself."); Spacer(Modifier.height(22.dp)); Text("Before you begin", fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("Stop and ask a qualified mechanic about broken or damaged spokes, rim damage, severe looseness, instability, or anything you are unsure about.", Modifier.padding(top = 8.dp)); Spacer(Modifier.height(18.dp)); Text("Microphone access is requested only when you start a capture. Audio is analyzed in memory and discarded.", color = Muted)
            }
        }
    }
}

@Composable private fun TopBar(title: String, onBack: (() -> Unit)? = null) { Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) { if (onBack != null) IconButton(onBack) { Text("‹", fontSize = 32.sp, color = Ink) }; Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink); Spacer(Modifier.weight(1f)); Text("i", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Teal, modifier = Modifier.semantics { contentDescription = "Help" }) } }

@Composable private fun WheelListScreen(wheels: List<UiWheel>, onAdd: () -> Unit, onSelect: (Int) -> Unit) { Scaffold(topBar = { TopBar("Your wheels") }, floatingActionButton = { FloatingActionButton(onAdd, containerColor = Teal, contentColor = Color.White) { Text("+", fontSize = 28.sp) } }) { p -> LazyColumn(Modifier.padding(p).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { Text("Keep each wheel’s readings separate. Compare spokes only within the same side.", color = Color(0xFF456363), modifier = Modifier.padding(bottom = 8.dp)) }; items(wheels.indices.toList()) { i -> Card(onClick = { onSelect(i) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(18.dp)) { Text("${wheels[i].name} · ${wheels[i].sizeLabel}", fontSize = 19.sp, fontWeight = FontWeight.SemiBold); Text("${wheels[i].spokeCount} spokes · alternating hub sides", color = Color(0xFF456363), modifier = Modifier.padding(top = 6.dp)); Text("Open wheel", color = Teal, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp)) } } } } } }

@Composable private fun CreateWheelScreen(count: Int, setCount: (Int) -> Unit, onSave: (String, Int) -> Unit, onBack: () -> Unit) { Scaffold(topBar = { TopBar("Create wheel", onBack) }) { p -> Column(Modifier.padding(p).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) { var name by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }; OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Wheel name") }, supportingText = { Text("A name helps you recognize this wheel later.") }, singleLine = true); Text("Spoke count", fontWeight = FontWeight.Bold); Text("Choose the count printed by the wheel or count each spoke once.", color = Color(0xFF456363)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(24, 28, 32, 36).forEach { n -> FilterChip(selected = count == n, onClick = { setCount(n) }, label = { Text(n.toString()) }) } }; OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth(), label = { Text("Notes (optional)") }, minLines = 3); Text("Geometry notes do not convert pitch into tension.", color = Color(0xFF456363), fontSize = 13.sp); Spacer(Modifier.weight(1f)); Button({ onSave(name.trim(), count) }, Modifier.fillMaxWidth().height(52.dp), enabled = name.isNotBlank()) { Text("Save wheel") } } } }

@Composable private fun WheelDetailScreen(name: String, count: Int, onStart: () -> Unit, onBack: () -> Unit) { Scaffold(topBar = { TopBar(name, onBack) }) { p -> Column(Modifier.padding(p).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { WheelDiagram(count, emptySet(), emptySet()); Row(verticalAlignment = Alignment.CenterVertically) { StatusPill("READY TO MEASURE"); Spacer(Modifier.width(10.dp)); Text("$count spokes · local only", color = Muted, fontSize = 13.sp) }; WorkshopCard { Text("No sessions yet", style = MaterialTheme.typography.headlineSmall); Text("Start a pass to build a same-side comparison. Previous accepted readings stay auditable.", color = Muted, modifier = Modifier.padding(top = 7.dp)); HorizontalDivider(color = Rule, modifier = Modifier.padding(vertical = 16.dp)); Text("Tip", fontWeight = FontWeight.Bold, color = Forest); Text("A consistent pluck and a quiet workspace make patterns easier to trust.", color = Muted, modifier = Modifier.padding(top = 4.dp)) }; Button(onStart, Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) { Text("Start a new pass", fontWeight = FontWeight.Bold, fontSize = 16.sp) } } } }

@Composable private fun WheelDiagram(count: Int, accepted: Set<Int>, current: Set<Int>) { Canvas(Modifier.fillMaxWidth().height(260.dp).semantics { contentDescription = "Wheel map with $count numbered spokes. Use the spoke list for accessible navigation." }) { val c = center; val r = size.minDimension * .34f; drawCircle(Color(0xFFE2ECE6), r + 11f); drawCircle(Color(0xFFFDFBF5), r, style = Stroke(7f)); drawCircle(Color(0xFFEAF1EB), r * .34f); drawCircle(Color(0xFFB4C9BD), r * .12f); repeat(count) { i -> val a = (i * 360f / count - 90f) * kotlin.math.PI.toFloat() / 180f; val x = c.x + kotlin.math.cos(a) * r; val y = c.y + kotlin.math.sin(a) * r; val point = androidx.compose.ui.geometry.Offset(x, y); drawLine(if (current.contains(i + 1)) Teal else Color(0xFF9BB7AA), c, point, if (current.contains(i + 1)) 4f else 2f, StrokeCap.Round); drawCircle(if (current.contains(i + 1)) Teal else if (accepted.contains(i + 1)) Color(0xFF5B9D78) else Color(0xFFB8CBC5), if (current.contains(i + 1)) 9f else 6f, point) } } }

@Composable private fun CaptureScreen(session: CaptureSession, onDone: () -> Unit, onBack: () -> Unit) {
    val count = session.profile.spokeCount
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var spoke by remember { mutableStateOf(session.currentSpoke) }
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

    fun injectLabPluck() {
        busy = true
        proposal = null
        status = "Analyzing repeatable lab pluck…"
        scope.launch {
            val sampleRate = 44_100
            val pcm = FloatArray(sampleRate) { index ->
                val seconds = index.toDouble() / sampleRate
                val strike = if (index < 70) (1.0 - index / 70.0) * 0.16 else 0.0
                val ring = kotlin.math.sin(2.0 * kotlin.math.PI * 320.0 * seconds) *
                    kotlin.math.exp(-seconds * 4.2) * 0.72
                (strike + ring).toFloat()
            }
            when (val pitch = withContext(Dispatchers.Default) { YinPitchAnalyzer().analyze(pcm, sampleRate) }) {
                is PitchResult.Accepted -> { proposal = pitch; status = "Repeatable lab reading ready for review" }
                is PitchResult.Rejected -> status = "Lab fixture was rejected: ${pitch.reason.name.lowercase()}"
            }
            pcm.fill(0f)
            busy = false
        }
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
                if (BuildConfig.DEBUG && !busy) {
                    TextButton(onClick = ::injectLabPluck) { Text("Lab: inject test pluck") }
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
                    Button(onClick = { session.accept(proposal!!.frequencyHz.toDouble(), proposal!!.confidence.toDouble()); spoke = session.currentSpoke; proposal = null; if (session.measurements().size + session.skippedSpokes().size >= count) onDone() }) { Text("Accept reading") }
                }
            }
            Text("Microphone is used only during this bounded capture. No recording is saved.", color = Color(0xFF456363), fontSize = 13.sp)
            TextButton(enabled = !busy, onClick = { proposal = null; session.skip(); spoke = session.currentSpoke; status = "Spoke skipped. Tap capture when ready." }) { Text("Skip this spoke") }
        }
    }
}

@Composable
private fun ResultsScreen(session: CaptureSession, onBack: () -> Unit) {
    val count = session.profile.spokeCount
    val readings = session.measurements()
    fun median(values: List<Double>): Double? {
        val sorted = values.sorted()
        if (sorted.isEmpty()) return null
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2.0
    }
    val left = readings.filter { session.profile.spoke(it.spokeNumber).side.name == "LEFT" }
    val right = readings.filter { session.profile.spoke(it.spokeNumber).side.name == "RIGHT" }
    fun medianLabel(values: List<Measurement>) = median(values.map { it.frequencyHz })?.let { "%.1f Hz".format(it) } ?: "—"
    Scaffold(topBar = { TopBar("Pass results", onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Text("Provisional comparison", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("$count spokes · ${readings.size} accepted · ${session.skippedSpokes().size} skipped", color = Muted)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Right side", medianLabel(right), "median · ${right.size} samples")
                    StatCard("Left side", medianLabel(left), "median · ${left.size} samples")
                }
            }
            item { Text("Spoke readings", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            items(readings) { measurement ->
                ListItem(
                    headlineContent = { Text("Spoke ${measurement.spokeNumber}") },
                    supportingContent = { Text("%.1f Hz · accepted".format(measurement.frequencyHz)) },
                )
            }
        }
    }
}

@Composable private fun RowScope.StatCard(label: String, value: String, detail: String) { Card(Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { Text(label, fontWeight = FontWeight.Bold); Text(value, fontSize = 23.sp, color = Teal, fontWeight = FontWeight.Bold); Text(detail, fontSize = 12.sp, color = Color(0xFF456363)) } } }

