package com.cet6.commute.ui.listen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cet6.commute.App
import com.cet6.commute.audio.PlayerEngine
import com.cet6.commute.data.AudioFile
import com.cet6.commute.data.Repo
import com.cet6.commute.ui.fmtMs

@Composable
fun PlayerScreen(
    audio: AudioFile,
    playlist: List<AudioFile> = listOf(audio),
    paperName: String? = null,
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as App
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember { PlayerEngine(scope) }
    val u by engine.st.collectAsState()

    // 套卷连播:当前段索引
    var segIdx by remember(audio.id) {
        mutableIntStateOf(playlist.indexOfFirst { it.id == audio.id }.coerceAtLeast(0))
    }
    val current = playlist.getOrNull(segIdx) ?: audio

    DisposableEffect(Unit) {
        engine.onListenSeconds = { sec -> Repo.addListenSeconds(app.db, sec) }
        engine.onEnded = {
            if (segIdx + 1 < playlist.size) segIdx++   // 自然播完 → 下一段
        }
        onDispose { engine.release() }
    }
    LaunchedEffect(current.path) { engine.load(current.path) }

    var note by remember { mutableStateOf("") }

    // 内置音频提供文稿(同目录同名txt)
    val transcriptFile = remember(current.path) {
        val f = java.io.File(current.path)
        val t = java.io.File(f.parentFile, f.nameWithoutExtension + ".txt")
        if (t.exists()) t else null
    }
    var showTranscript by remember { mutableStateOf(false) }
    if (showTranscript && transcriptFile != null) {
        val transcriptText = remember(current.path) {
            try { transcriptFile.readText() } catch (e: Exception) { "文稿读取失败" }
        }
        AlertDialog(
            onDismissRequest = { showTranscript = false },
            title = { Text("📄 精听文稿") },
            text = {
                Column(Modifier.height(380.dp).verticalScroll(rememberScrollState())) {
                    Text(transcriptText, fontSize = 16.sp, lineHeight = 25.sp)
                }
            },
            confirmButton = { TextButton(onClick = { showTranscript = false }) { Text("关闭") } }
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Text("←", fontSize = 26.sp) }
            Column(Modifier.weight(1f)) {
                if (paperName != null && playlist.size > 1) {
                    Text(
                        "$paperName · 第${segIdx + 1}/${playlist.size}段",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    current.segTitle.ifBlank { current.name },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
            }
        }

        if (playlist.size > 1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { if (segIdx > 0) segIdx-- }, enabled = segIdx > 0, modifier = Modifier.weight(1f)) {
                    Text("⏮ 上一段", fontSize = 15.sp)
                }
                OutlinedButton(onClick = { if (segIdx + 1 < playlist.size) segIdx++ }, enabled = segIdx + 1 < playlist.size, modifier = Modifier.weight(1f)) {
                    Text("下一段 ⏭", fontSize = 15.sp)
                }
            }
        }

        if (transcriptFile != null) {
            OutlinedButton(
                onClick = { showTranscript = true },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("📄 查看精听文稿(先盲听,再逐句对照)", fontSize = 15.sp) }
        }

        if (u.loading) {
            Text("加载中…", fontSize = 20.sp, modifier = Modifier.padding(top = 60.dp))
        } else if (!u.loaded) {
            Text("音频加载失败,文件可能已损坏", fontSize = 17.sp, modifier = Modifier.padding(top = 60.dp))
        } else {
            // 时间与进度
            Text("${fmtMs(u.positionMs)} / ${fmtMs(u.durationMs)}",
                fontSize = 26.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)

            var dragging by remember { mutableStateOf(false) }
            var dragVal by remember { mutableFloatStateOf(0f) }
            Slider(
                value = if (dragging) dragVal else u.positionMs.toFloat(),
                onValueChange = { dragging = true; dragVal = it },
                onValueChangeFinished = {
                    engine.seekTo(dragVal.toInt()); dragging = false
                },
                valueRange = 0f..u.durationMs.toFloat().coerceAtLeast(1f),
                modifier = Modifier.fillMaxWidth()
            )

            // 主控
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                OutlinedButton(onClick = { engine.skip(-10) }, modifier = Modifier.size(84.dp, 56.dp)) {
                    Text("−10s", fontSize = 17.sp)
                }
                FilledIconButton(
                    onClick = { engine.toggle() },
                    modifier = Modifier.size(84.dp).height(84.dp),
                    shape = CircleShape
                ) {
                    Text(if (u.playing) "⏸" else "▶", fontSize = 34.sp)
                }
                OutlinedButton(onClick = { engine.skip(10) }, modifier = Modifier.size(84.dp, 56.dp)) {
                    Text("+10s", fontSize = 17.sp)
                }
            }

            // 倍速
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("倍速", fontSize = 16.sp)
                listOf(0.8f, 1.0f, 1.2f).forEach { s ->
                    FilterChip(
                        selected = (u.speed - s).let { it < 0.01f && it > -0.01f },
                        onClick = { engine.setSpeed(s) },
                        label = { Text("${s}x", fontSize = 16.sp) }
                    )
                }
            }

            // A-B 循环
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("A-B 循环精听", style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
                    Text(
                        if (u.abActive) "🔁 循环中:${fmtMs(u.aMs)} → ${fmtMs(u.bMs)}(适合反复听一句话)"
                        else "先播到句子开头点「设A」,再播到结尾点「设B」",
                        fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { engine.markA() }, modifier = Modifier.weight(1f)) { Text("设A ${if (u.aMs >= 0) fmtMs(u.aMs) else ""}") }
                        OutlinedButton(onClick = { engine.markB() }, modifier = Modifier.weight(1f)) { Text("设B ${if (u.bMs >= 0) fmtMs(u.bMs) else ""}") }
                        OutlinedButton(onClick = { engine.clearAB() }, enabled = u.aMs >= 0 || u.bMs >= 0, modifier = Modifier.weight(1f)) { Text("清除") }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // 快速笔记
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("边听边记", style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("记下听不懂的句子、连读、生词…") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (note.isNotBlank()) {
                            Repo.addNote(app.db, note)
                            note = ""
                            android.widget.Toast.makeText(ctx, "已保存到今日笔记", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("保存笔记(按日期)", fontSize = 16.sp) }
            }
        }
    }
}
