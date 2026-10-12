package com.cet6.commute.ui.vocab

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cet6.commute.App
import com.cet6.commute.data.Repo
import com.cet6.commute.data.Word
import com.cet6.commute.ui.haptic
import com.cet6.commute.ui.speakOrToast
import java.time.LocalDate

@Composable
fun VocabScreen(mode: String, newLimit: Int, onNewLimit: (Int) -> Unit) {
    val app = LocalContext.current.applicationContext as App
    val ctx = LocalContext.current
    val today = remember { LocalDate.now().toEpochDay().toInt() }

    var queue by remember { mutableStateOf(listOf<Word>()) }
    var idx by remember { mutableIntStateOf(0) }
    var flipped by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var showImport by remember { mutableStateOf(false) }
    var showLimit by remember { mutableStateOf(false) }

    val todayNew = remember(refresh) { Repo.todayNewCount(app.db, today) }
    val wbCount = remember(refresh) { Repo.wordbook(app.db).size }

    // 学习时长统计(每5秒累计,供周计划使用)
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5000)
            Repo.addStudySeconds(app.db, 5)
        }
    }

    fun advance() {
        flipped = false
        if (idx + 1 < queue.size) idx++ else refresh++
    }

    LaunchedEffect(mode, refresh, newLimit) {
        queue = if (mode == "wordbook") {
            Repo.wordbook(app.db)
        } else {
            val due = Repo.reviewQueue(app.db, today)
            due + Repo.newWords(app.db, newLimit - Repo.todayNewCount(app.db, today))
        }
        idx = 0
        flipped = false
    }

    val word = queue.getOrNull(idx)

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 标题行
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (mode == "wordbook") "生词本复习" else "词汇闪卡", style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (mode == "wordbook") "生词共 $wbCount 个"
                    else "今日新词 $todayNew/$newLimit · 剩余 ${queue.size - idx} 张",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp
                )
            }
            if (mode != "wordbook") {
                TextButton(onClick = { showLimit = true }) { Text("⚙️ $newLimit 个/轮") }
            }
            TextButton(onClick = { showImport = true }) { Text("📥 导入词表") }
        }

        if (word == null) {
            Card(
                Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🎉", fontSize = 60.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (mode == "wordbook") "生词本清空啦!\n别让生词过夜。" else "今日词汇任务完成!",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (mode == "wordbook") "去首页继续听力或阅读吧" else "明天见,记得保持节奏",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val anim by animateFloatAsState(if (flipped) 180f else 0f, label = "flip")
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .graphicsLayer {
                        rotationY = anim
                        cameraDistance = 16f * density
                    }
                    .clickable { flipped = !flipped; haptic(ctx) },
                colors = CardDefaults.cardColors(
                    containerColor = if (anim <= 90f) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.secondaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                if (anim <= 90f) {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(word.term, fontSize = 44.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        if (word.ipa.isNotBlank()) {
                            Text(word.ipa, fontSize = 22.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                        if (word.pos.isNotBlank()) {
                            Text(word.pos, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            TextButton(onClick = { speakOrToast(ctx, word.term) }) { Text("🔊 朗读", fontSize = 18.sp) }
                            Text("点击卡片看释义", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Column(
                        Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }
                            .padding(24.dp).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(word.meaning, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        if (word.example.isNotBlank()) {
                            Text(
                                word.example, fontSize = 19.sp, textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { speakOrToast(ctx, word.example) }) { Text("🔊 读例句") }
                        }
                        Text("点击卡片返回", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        Repo.rateWord(app.db, word.id, "no", today)
                        advance()
                        haptic(ctx)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f).height(60.dp)
                ) { Text("不会", fontSize = 19.sp) }
                Button(
                    onClick = {
                        Repo.rateWord(app.db, word.id, "fuzzy", today)
                        advance()
                        haptic(ctx)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00)),
                    modifier = Modifier.weight(1f).height(60.dp)
                ) { Text("模糊", fontSize = 19.sp) }
                Button(
                    onClick = {
                        Repo.rateWord(app.db, word.id, "yes", today)
                        advance()
                        haptic(ctx)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.weight(1f).height(60.dp)
                ) { Text("认识", fontSize = 19.sp) }
            }
            Text(
                "「不会」明天复现 · 「模糊」3天后 · 「认识」7天后",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
            )
        }
    }

    // 从txt文件导入词表(自动识别 UTF-8/GBK 编码)
    val fileImportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val bytes = ctx.contentResolver.openInputStream(uri)?.readBytes() ?: ByteArray(0)
                var text = String(bytes, Charsets.UTF_8)
                if (text.contains('\uFFFD')) text = String(bytes, charset("GBK"))
                val (ok, skip) = Repo.addCustomWords(app.db, text)
                refresh++
                android.widget.Toast.makeText(ctx, "导入 $ok 个,跳过 $skip 个", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(ctx, "读取失败:${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    if (showImport) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text("导入自定义词表") },
            text = {
                Column {
                    Text("每行一个,格式:单词,释义\n例:anchor,锚\n(也支持 单词,音标,释义 三段式)", fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            showImport = false
                            fileImportLauncher.launch(arrayOf("text/plain", "text/*", "application/octet-stream"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("📁 从手机文件导入(txt,可一次导入几千词)", fontSize = 15.sp) }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        minLines = 5,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val (ok, skip) = Repo.addCustomWords(app.db, text)
                    showImport = false
                    refresh++
                    android.widget.Toast.makeText(ctx, "导入 $ok 个,跳过 $skip 个", android.widget.Toast.LENGTH_SHORT).show()
                }) { Text("导入") }
            },
            dismissButton = { TextButton(onClick = { showImport = false }) { Text("取消") } }
        )
    }

    // 每轮新词数调节(即时生效,与设置页联动)
    if (showLimit) {
        AlertDialog(
            onDismissRequest = { showLimit = false },
            title = { Text("每轮新词数") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("每轮闪卡学习的新词上限。调大刷词更快,调小记得更牢。", fontSize = 15.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10, 20, 30, 40, 60).forEach { n ->
                            FilterChip(
                                selected = newLimit == n,
                                onClick = { onNewLimit(n); showLimit = false },
                                label = { Text("$n", fontSize = 15.sp) }
                            )
                        }
                    }
                    Text(
                        "2000 词库按当前速度约需 ${(2000 + newLimit - 1) / newLimit} 天学完;到期复习词不受此限制。",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showLimit = false }) { Text("完成") } }
        )
    }
}
