package com.cet6.commute.ui.listen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cet6.commute.App
import com.cet6.commute.data.AudioFile
import com.cet6.commute.data.Paper
import com.cet6.commute.data.Repo
import com.cet6.commute.ui.fmtBytes
import org.json.JSONArray
import org.json.JSONObject

/** 独立「套卷」模块:完整卷面(写作/听力/阅读/翻译)+答案解析+听力音频联动 */
@Composable
fun PapersScreen() {
    val app = LocalContext.current.applicationContext as App
    var refresh by remember { mutableIntStateOf(0) }
    val papers = remember(refresh) { Repo.allPapers(app.db) }
    var openPaperId by remember { mutableStateOf<Long?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    openPaperId?.let { pid ->
        PaperDetailScreen(paperId = pid, onBack = { openPaperId = null; refresh++ })
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("真题套卷", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = { showCreate = true }) { Text("➕ 新建套卷") }
        }
        Text(
            "内置 2 套配音频模拟卷 + 2018-2026 年份冲刺卷(写作题为历年真题,听力/阅读/翻译为同难度仿真题;年份卷听力可点上方「导入听力MP3」搭配真题音频使用)。",
            fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (papers.isEmpty()) {
            Card(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("📗", fontSize = 52.sp)
                    Text("还没有套卷\n点「新建套卷」开始", textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 17.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                items(papers, key = { it.id }) { p ->
                    val segs = remember(p.id, refresh) { Repo.segments(app.db, p.id) }
                    Card(onClick = { openPaperId = p.id }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(p.name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.weight(1f), maxLines = 2)
                                Text(if (p.content.isNotBlank()) "📄 完整卷面" else "🎧 仅音频", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                "${segs.size} 段听力音频 · " + (if (p.builtin) "内置" else "自建"),
                                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("新建套卷") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("建议命名:年份+月份+套数,如「2024年6月 第1套」。创建后可在各部分粘贴导入真题文本。", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("套卷名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        val id = Repo.createPaper(app.db, name.trim())
                        showCreate = false
                        openPaperId = id
                    }
                }) { Text("创建") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("取消") } }
        )
    }
}

// ================= 卷面详情 =================

@Composable
fun PaperDetailScreen(paperId: Long, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as App
    val ctx = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val paper = remember(paperId, refresh) { Repo.paperById(app.db, paperId) }
    val segs = remember(paperId, refresh) { Repo.segments(app.db, paperId) }
    var playSeg by remember { mutableStateOf<AudioFile?>(null) }
    var playAll by remember { mutableStateOf(false) }
    var section by rememberSaveable { mutableIntStateOf(0) }

    if (playAll && segs.isNotEmpty()) {
        PlayerScreen(audio = segs[0], playlist = segs, paperName = paper?.name, onBack = { playAll = false; refresh++ })
        return
    }
    playSeg?.let { seg ->
        PlayerScreen(audio = seg, playlist = listOf(seg), paperName = paper?.name, onBack = { playSeg = null })
        return
    }

    if (paper == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            TextButton(onClick = onBack) { Text("← 套卷不存在") }
        }
        return
    }

    // 解析卷面 JSON
    val jc = remember(paper.content) { if (paper.content.isNotBlank()) runCatching { JSONObject(paper.content) }.getOrNull() else null }
    val listening = remember(jc) {
        runCatching {
            val arr = jc?.optJSONArray("listening") ?: JSONArray()
            (0 until arr.length()).map { arr.getJSONObject(it) }
        }.getOrDefault(emptyList())
    }
    val hasWriting = jc?.optJSONObject("writing") != null
    val hasReading = jc?.optJSONObject("reading") != null
    val hasTranslation = jc?.optJSONObject("translation") != null

    // 导入音频到本套卷
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
                ctx.contentResolver.openInputStream(uri)?.use { input -> out.outputStream().use { input.copyTo(it) } }
                Repo.addAudio(app.db, name, out.absolutePath, out.length(), paperId = paperId, segOrder = Repo.nextSegOrder(app.db, paperId), segTitle = name.substringBeforeLast('.'))
                refresh++
                android.widget.Toast.makeText(ctx, "已加入套卷", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(ctx, "导入失败:${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("←", fontSize = 26.sp) }
            Column(Modifier.weight(1f)) {
                Text(paper.name, style = MaterialTheme.typography.titleLarge, maxLines = 2)
                Text(
                    (if (paper.builtin) "内置模拟卷" else "自建套卷") + " · ${segs.size}段听力" + if (jc != null) " · 完整卷面" else "",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!paper.builtin) {
                TextButton(onClick = {
                    Repo.deletePaper(app.db, paperId)
                    onBack()
                }) { Text("🗑") }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { playAll = true }, enabled = segs.isNotEmpty(), modifier = Modifier.weight(1f).height(50.dp)) {
                Text("▶ 连播听力", fontSize = 16.sp)
            }
            OutlinedButton(onClick = { launcher.launch(arrayOf("audio/mpeg", "audio/*")) }, modifier = Modifier.weight(1f).height(50.dp)) {
                Text("📁 导入听力MP3", fontSize = 15.sp)
            }
        }

        TabRow(selectedTabIndex = section) {
            Tab(selected = section == 0, onClick = { section = 0 }, text = { Text("写作", fontSize = 16.sp) })
            Tab(selected = section == 1, onClick = { section = 1 }, text = { Text("听力", fontSize = 16.sp) })
            Tab(selected = section == 2, onClick = { section = 2 }, text = { Text("阅读", fontSize = 16.sp) })
            Tab(selected = section == 3, onClick = { section = 3 }, text = { Text("翻译", fontSize = 16.sp) })
        }

        when (section) {
            0 -> if (hasWriting) WritingSection(jc!!, paperId, paper.content, segs, { playSeg = it }, { refresh++ })
                 else EmptySection("写作", paperId, paper.content, { refresh++ })
            1 -> ListeningSection(jc, listening, segs, { playSeg = it }, paperId, paper.content, { refresh++ })
            2 -> if (hasReading) ReadingSection(jc!!, paperId, paper.content, { refresh++ })
                 else EmptySection("阅读", paperId, paper.content, { refresh++ })
            3 -> if (hasTranslation) TranslationSection(jc!!, paperId, paper.content, { refresh++ })
                 else EmptySection("翻译", paperId, paper.content, { refresh++ })
        }
    }
}

@Composable
private fun EmptySection(name: String, paperId: Long, content: String, onRefresh: () -> Unit) {
    val app = LocalContext.current.applicationContext as App
    var text by remember { mutableStateOf("") }
    Card(Modifier.fillMaxSize(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("📄", fontSize = 40.sp)
            Spacer(Modifier.height(8.dp))
            Text("此部分还没有内容\n可从真题网站复制「$name」部分粘贴到下面", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                label = { Text("粘贴${name}部分文本(题目/原文)") },
                minLines = 5, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = {
                if (text.isNotBlank()) {
                    val o = if (content.isNotBlank()) runCatching { JSONObject(content) }.getOrDefault(JSONObject()) else JSONObject()
                    when (name) {
                        "写作" -> o.put("writing", JSONObject().put("prompt", text.trim()))
                        "阅读" -> o.put("reading", JSONObject().put("passage", text.trim()))
                        "翻译" -> o.put("translation", JSONObject().put("prompt", text.trim()))
                    }
                    Repo.updatePaperContent(app.db, paperId, o.toString())
                    onRefresh()
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("保存到本套卷") }
        }
    }
}

// ---------- 各部分渲染 ----------

@Composable
private fun WritingSection(jc: JSONObject, paperId: Long, content: String, segs: List<AudioFile>, onPlay: (AudioFile) -> Unit, onRefresh: () -> Unit) {
    val w = jc.optJSONObject("writing") ?: return
    var showModel by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PaperCard("题目") { SectionText(w.optString("prompt")) }
        if (w.optString("ideas").isNotBlank()) PaperCard("审题思路") { SectionText(w.optString("ideas")) }
        OutlinedButton(onClick = { showModel = !showModel }, modifier = Modifier.fillMaxWidth()) {
            Text(if (showModel) "🙈 隐藏范文" else "✍️ 写完后点此查看参考范文", fontSize = 16.sp)
        }
        if (showModel) {
            PaperCard("参考范文") { SectionText(w.optString("model")) }
            if (w.optString("notes").isNotBlank()) PaperCard("亮点与解析") { SectionText(w.optString("notes")) }
        }
    }
}

@Composable
private fun ListeningSection(
    jc: JSONObject?, listening: List<JSONObject>, segs: List<AudioFile>,
    onPlay: (AudioFile) -> Unit, paperId: Long, content: String, onRefresh: () -> Unit
) {
    var showAns by remember { mutableStateOf(setOf<Int>()) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (listening.isEmpty()) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    if (segs.isEmpty()) {
                        Text("本套卷还没有听力音频,点上方「导入听力MP3」添加。", fontSize = 15.sp)
                    } else {
                        Text("本套卷有 ${segs.size} 段音频,暂无卷面题目。\n可点下方「编辑本段题目」为每段添加配套题目与答案。", fontSize = 15.sp)
                    }
                }
            }
        }
        listening.forEachIndexed { i, l ->
            val segTitle = l.optString("segTitle", "Section ${i + 1}")
            val audioBase = l.optString("audioBase")
            val match = if (audioBase.isNotEmpty()) segs.firstOrNull { java.io.File(it.path).nameWithoutExtension.contains(audioBase, ignoreCase = true) } else null
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(segTitle, style = MaterialTheme.typography.titleMedium, fontSize = 17.sp, modifier = Modifier.weight(1f))
                        if (match != null) {
                            Button(onClick = { onPlay(match) }, modifier = Modifier.height(40.dp)) { Text("▶ 播放本段", fontSize = 14.sp) }
                        }
                    }
                    if (l.optString("topic").isNotBlank()) Text("主题:${l.optString("topic")}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (l.optString("intro").isNotBlank()) SectionText(l.optString("intro"))
                    if (l.optString("questions").isNotBlank()) SectionText(l.optString("questions"))
                    OutlinedButton(onClick = {
                        showAns = if (i in showAns) showAns - i else showAns + i
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (i in showAns) "🙈 隐藏答案与解析" else "✅ 做完后显示答案与解析", fontSize = 15.sp)
                    }
                    if (i in showAns) {
                        if (l.optString("answer").isNotBlank()) PaperCard("答案") { SectionText(l.optString("answer")) }
                        if (l.optString("analysis").isNotBlank()) PaperCard("解析") { SectionText(l.optString("analysis")) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingSection(jc: JSONObject, paperId: Long, content: String, onRefresh: () -> Unit) {
    val r = jc.optJSONObject("reading") ?: return
    var showAns by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (r.optString("title").isNotBlank()) Text(r.optString("title"), style = MaterialTheme.typography.titleMedium)
        PaperCard("文章") { SectionText(r.optString("passage")) }
        PaperCard("题目") { SectionText(r.optString("questions")) }
        OutlinedButton(onClick = { showAns = !showAns }, modifier = Modifier.fillMaxWidth()) {
            Text(if (showAns) "🙈 隐藏答案与解析" else "✅ 做完后显示答案与解析", fontSize = 15.sp)
        }
        if (showAns) {
            PaperCard("答案") { SectionText(r.optString("answer")) }
            if (r.optString("analysis").isNotBlank()) PaperCard("解析") { SectionText(r.optString("analysis")) }
        }
    }
}

@Composable
private fun TranslationSection(jc: JSONObject, paperId: Long, content: String, onRefresh: () -> Unit) {
    val t = jc.optJSONObject("translation") ?: return
    var showAns by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PaperCard("原文") { SectionText(t.optString("prompt")) }
        OutlinedButton(onClick = { showAns = !showAns }, modifier = Modifier.fillMaxWidth()) {
            Text(if (showAns) "🙈 隐藏参考译文" else "✍️ 自己译完后对照参考译文", fontSize = 15.sp)
        }
        if (showAns) {
            PaperCard("参考译文") { SectionText(t.optString("reference")) }
            if (t.optString("notes").isNotBlank()) PaperCard("要点解析") { SectionText(t.optString("notes")) }
        }
    }
}

@Composable
private fun PaperCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun SectionText(text: String) {
    if (text.isBlank()) return
    Text(text, fontSize = 16.sp, lineHeight = 25.sp)
}
