package com.cet6.commute.ui.read

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.cet6.commute.data.Essay
import com.cet6.commute.data.Repo
import com.cet6.commute.ui.speakOrToast

/** 按空白切词(保留标点) */
internal fun tokenize(text: String): List<String> =
    Regex("\\S+").findAll(text).map { it.value }.toList()

/** 按句号/问号/叹号切句 */
internal fun splitSentences(text: String): List<String> =
    text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotBlank() }

@Composable
fun EssayScreen() {
    val app = LocalContext.current.applicationContext as App
    val ctx = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val essays = remember(refresh) { Repo.allEssays(app.db) }
    var openEssay by remember { mutableStateOf<Essay?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    openEssay?.let { e ->
        EssayDetailScreen(
            essay = Repo.essayById(app.db, e.id) ?: e,
            onBack = { openEssay = null; refresh++ }
        )
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("作文背诵", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = { showAdd = true }) { Text("➕ 导入范文") }
        }
        Text(
            "内置多篇六级高分范文(现象阐释/观点论证/问题解决/名言引语/图表作文/热点话题),多取材历年真题。用「挖空」「逐句」「回忆」三种模式科学背诵。",
            fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val masteredCount = essays.count { it.mastered }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistStat("范文", "${essays.size}")
            AssistStat("已背熟", "$masteredCount")
            AssistStat("目标", "考前全部背熟")
        }

        if (essays.isEmpty()) {
            Card(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("✍️", fontSize = 52.sp)
                    Text("首启导入中,稍等几秒后返回刷新", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 17.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                itemsIndexed(essays, key = { _, e -> e.id }) { _, e ->
                    Card(
                        onClick = { openEssay = e },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (e.mastered) {
                                        Text("🏅 ", fontSize = 16.sp)
                                    }
                                    Text(e.title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, maxLines = 2)
                                }
                                Text(
                                    "${e.category} · ${e.content.length} 词符" + if (e.builtin) " · 内置" else "",
                                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text("背诵 →", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("观点论证") }
        var outline by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("导入自己的范文") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("现象阐释", "观点论证", "问题解决", "名言引语", "图表作文", "热点话题").forEach { c ->
                            FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c, fontSize = 13.sp) })
                        }
                    }
                    OutlinedTextField(value = outline, onValueChange = { outline = it }, label = { Text("中文背诵大纲(可选,用于回忆模式)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("粘贴范文正文(英文)") }, minLines = 5, maxLines = 10, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        Repo.addEssay(app.db, title.trim(), category, outline.trim(), content.trim())
                        showAdd = false
                        refresh++
                    } else {
                        android.widget.Toast.makeText(ctx, "标题和正文不能为空", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun AssistStat(label: String, value: String) {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            "$label $value",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

// ================= 背诵详情页 =================

private enum class MemoMode(val label: String) { FULL("全文"), SENTENCE("逐句"), CLOZE("挖空"), RECALL("回忆") }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EssayDetailScreen(essay: Essay, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as App
    val ctx = LocalContext.current
    var mode by remember { mutableStateOf(MemoMode.FULL) }
    var clozeRatio by remember { mutableIntStateOf(2) } // 1=25% 2=50% 3=75%
    var mastered by remember(essay.id) { mutableStateOf(essay.mastered) }
    val sentences = remember(essay.id) { splitSentences(essay.content) }
    var revealed by remember(essay.id) { mutableStateOf(setOf<Int>()) }
    var showAll by remember(essay.id) { mutableStateOf(false) }
    val revealedWords = remember(essay.id, clozeRatio) { mutableStateListOf<Int>() }
    var showTranscriptAll by remember(essay.id, clozeRatio) { mutableStateOf(false) }

    val tokens = remember(essay.id) { tokenize(essay.content) }
    val hideCond: (Int) -> Boolean = when (clozeRatio) {
        1 -> { i -> i % 4 == 0 }
        2 -> { i -> i % 2 == 0 }
        else -> { i -> i % 4 != 0 }
    }

    // 详情页入场动画
    var entered by remember(essay.id) { mutableStateOf(false) }
    LaunchedEffect(essay.id) { entered = true }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AnimatedVisibility(
            visible = entered,
            enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { -it / 4 }
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("←", fontSize = 24.sp) }
                Column(Modifier.weight(1f)) {
                    Text(essay.title, style = MaterialTheme.typography.titleLarge, maxLines = 2)
                    Text(essay.category, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = {
                    mastered = !mastered
                    Repo.setEssayMastered(app.db, essay.id, mastered)
                    android.widget.Toast.makeText(ctx, if (mastered) "已标记背熟 🏅" else "取消背熟", android.widget.Toast.LENGTH_SHORT).show()
                }) {
                    Text(if (mastered) "🏅 已背熟" else "标记背熟", fontSize = 15.sp)
                }
            }
        }

        // 模式切换
        AnimatedVisibility(
            visible = entered,
            enter = fadeIn(tween(350, delayMillis = 80)) + slideInVertically(tween(350, delayMillis = 80)) { -it / 4 }
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MemoMode.entries.forEach { m ->
                    FilterChip(
                        selected = mode == m,
                        onClick = { mode = m; revealed = emptySet(); showAll = false; showTranscriptAll = false },
                        label = { Text(m.label, fontSize = 15.sp) }
                    )
                }
            }
        }
        if (mode == MemoMode.CLOZE) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("遮挡", fontSize = 15.sp)
                listOf(1 to "25%", 2 to "50%", 3 to "75%").forEach { (r, label) ->
                    FilterChip(
                        selected = clozeRatio == r,
                        onClick = { clozeRatio = r; revealedWords.clear(); showTranscriptAll = false },
                        label = { Text(label, fontSize = 14.sp) }
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { revealedWords.clear(); showTranscriptAll = !showTranscriptAll }) {
                    Text(if (showTranscriptAll) "全部隐藏" else "全部显示", fontSize = 14.sp)
                }
            }
        }
        if (mode == MemoMode.SENTENCE) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("已显示 ${revealed.size}/${sentences.size} 句", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { if (revealed.size < sentences.size) revealed = revealed + setOf(revealed.size) }) { Text("下一句") }
                TextButton(onClick = { revealed = (0 until sentences.size).toSet() }) { Text("全部显示") }
            }
        }
        if (mode == MemoMode.RECALL) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("只看大纲口头复述全文,再对照原文检查。", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                TextButton(onClick = { showAll = !showAll }) { Text(if (showAll) "隐藏原文" else "对照原文") }
            }
        }

        // 朗读
        OutlinedButton(
            onClick = { speakOrToast(ctx, essay.content) },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) { Text("🔊 朗读全文(可跟读)", fontSize = 16.sp) }

        // 内容区(模式切换平滑过渡)
        Crossfade(
            targetState = mode,
            animationSpec = tween(300),
            label = "memo-mode",
            modifier = Modifier.weight(1f)
        ) { m ->
            when (m) {
                MemoMode.FULL -> {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        ExamPaperSheet(
                            title = essay.title,
                            category = essay.category,
                            directions = "Directions: For this part, you are allowed 30 minutes to write an essay on the topic below. You should write at least 150 words but no more than 200 words.",
                            bodyText = essay.content
                        )
                        if (essay.outline.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("🧭 背诵大纲", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Text(essay.outline, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        }
                    }
                }
                MemoMode.SENTENCE -> {
                    Card(Modifier.fillMaxSize()) {
                        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            sentences.forEachIndexed { i, s ->
                                val shown = i in revealed
                                Row {
                                    Text(
                                        "${i + 1}. ",
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (shown) {
                                        Text(s, fontSize = 17.sp, lineHeight = 25.sp, modifier = Modifier.clickable { revealed = revealed - i })
                                    } else {
                                        Text(
                                            "▨▨▨▨ 点击显示第${i + 1}句 ▨▨▨▨",
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.clickable { revealed = revealed + i }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                MemoMode.CLOZE -> {
                    Card(Modifier.fillMaxSize()) {
                        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                tokens.forEachIndexed { i, tk ->
                                    val isWord = tk.any { it.isLetter() }
                                    val hidden = isWord && hideCond(i) && i !in revealedWords && !showTranscriptAll
                                    if (hidden) {
                                        Text(
                                            "⎯".repeat(tk.length.coerceIn(3, 10)),
                                            fontSize = 17.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clickable { revealedWords.add(i) }
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                        )
                                    } else {
                                        Text(tk, fontSize = 17.sp)
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        "提示:被遮挡的词显示为蓝色横线,点击揭底;点「全部显示」核对后可再隐藏自测。",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                MemoMode.RECALL -> {
                    Card(Modifier.fillMaxSize(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                            Text("🧭 中文背诵大纲", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                essay.outline.ifBlank { "(未提供大纲,请自行梳理结构:开头-论证-结尾)" },
                                fontSize = 16.sp, lineHeight = 26.sp, color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            if (showAll) {
                                Spacer(Modifier.height(14.dp))
                                Text("📄 原文对照", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(Modifier.height(6.dp))
                                Text(essay.content, fontSize = 17.sp, lineHeight = 26.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
            }
        }
    }
}
