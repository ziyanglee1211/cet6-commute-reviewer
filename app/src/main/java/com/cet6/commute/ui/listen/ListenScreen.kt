package com.cet6.commute.ui.listen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cet6.commute.App
import com.cet6.commute.data.AudioFile
import com.cet6.commute.data.Repo
import com.cet6.commute.ui.fmtBytes

@Composable
fun ListenScreen() {
    SingleAudioSection()
}

@Composable
private fun SingleAudioSection() {
    val app = LocalContext.current.applicationContext as App
    val ctx = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val audioList = remember(refresh) { Repo.allAudio(app.db) }
    var playerFor by remember { mutableStateOf<AudioFile?>(null) }
    var showNotes by remember { mutableStateOf(false) }

    if (showNotes) {
        NotesScreen(onBack = { showNotes = false })
        return
    }
    playerFor?.let { af ->
        PlayerScreen(audio = af, onBack = { playerFor = null; refresh++ })
        return
    }

    // SAF 导入
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                var name = "audio_${System.currentTimeMillis()}.mp3"
                ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    val i = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (i >= 0 && c.moveToFirst() && !c.isNull(i)) name = c.getString(i)
                }
                val dir = java.io.File(ctx.filesDir, "audio").apply { mkdirs() }
                val out = java.io.File(dir, "aud_${System.currentTimeMillis()}.mp3")
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    out.outputStream().use { input.copyTo(it) }
                }
                Repo.addAudio(app.db, name, out.absolutePath, out.length())
                refresh++
                android.widget.Toast.makeText(ctx, "已导入:$name", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(ctx, "导入失败:${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("听力精听", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = { showNotes = true }) { Text("📝 听力笔记") }
        }
        Text(
            "导入真题听力 MP3,导入后永久保存在本机,完全离线可听。",
            fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = { launcher.launch(arrayOf("audio/mpeg", "audio/*")) },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text("📁 导入 MP3 音频", fontSize = 18.sp) }

        if (audioList.isEmpty()) {
            Card(Modifier.fillMaxWidth().weight(1f)) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🎧", fontSize = 52.sp)
                    Text("还没有音频\n点上面按钮从手机导入吧", textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 17.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                items(audioList, key = { it.id }) { af ->
                    Card(modifier = Modifier.fillMaxWidth(), onClick = { playerFor = af }) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (af.builtin) {
                                        androidx.compose.material3.Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier.padding(end = 6.dp)
                                        ) {
                                            Text(
                                                "内置",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(af.name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
                                        maxLines = 2)
                                }
                                Text(
                                    "${fmtBytes(af.sizeBytes)} · ${Repo.millisToDay(af.addedMs)} 导入",
                                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text("▶ 播放", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
