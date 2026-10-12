package com.cet6.commute.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.cet6.commute.data.Repo
import com.cet6.commute.ui.MonthCalendar
import com.cet6.commute.ui.theme.ThemeMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** 「我的」页:本周计划 / 累计统计 / 打卡日历 / 生词本 / 设置入口 */
@Composable
fun ProfileScreen(
    onReviewWordbook: () -> Unit,
    onOpenSettings: () -> Unit,
    mode: ThemeMode,
    onMode: (ThemeMode) -> Unit
) {
    val app = LocalContext.current.applicationContext as App
    var refresh by remember { mutableIntStateOf(0) }
    var showEditGoals by remember { mutableStateOf(0) } // 0隐藏 1显示

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text("我的", style = MaterialTheme.typography.headlineMedium) }

        // ===== 本周概览 =====
        item {
            val monday = remember(refresh) { LocalDate.now().with(DayOfWeek.MONDAY) }
            val sunday = monday.plusDays(6)
            val today = LocalDate.now()
            val days = remember(refresh) { (0..6).map { monday.plusDays(it.toLong()) } }
            val dayMinutes = days.map { Repo.studySecondsOn(app.db, it.toString()) / 60 }
            val checkedDays = Repo.monthCheckinDays(app.db, monday.toString().substring(0, 7))
                .filter { it >= monday.toString() && it <= sunday.toString() }.toSet()

            val wordsTarget = app.prefs.getInt("goal_words", 100)
            val listenTarget = app.prefs.getInt("goal_listen", 60)
            val studyTarget = app.prefs.getInt("goal_study", 240)
            val daysTarget = app.prefs.getInt("goal_days", 5)

            val weekWords = Repo.wordsSeenBetween(app.db, monday.toEpochDay().toInt(), sunday.toEpochDay().toInt())
            val weekListenMin = Repo.listenSecondsBetween(app.db, monday.toString(), sunday.toString()) / 60
            val weekStudyMin = Repo.studySecondsBetween(app.db, monday.toString(), sunday.toString()) / 60
            val weekCheckins = checkedDays.size

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "本周 %d.%02d - %d.%02d".format(monday.monthValue, monday.dayOfMonth, sunday.monthValue, sunday.dayOfMonth),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        TextButton(onClick = { showEditGoals = 1 }) { Text("🎯 设目标") }
                    }

                    // 七天打卡条
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        days.forEachIndexed { i, d ->
                            val isToday = d == today
                            val done = d.toString() in checkedDays
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = CircleShape,
                                    color = when {
                                        done -> MaterialTheme.colorScheme.secondary
                                        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    modifier = Modifier.height(34.dp).fillMaxWidth(0.86f)
                                ) {
                                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Text(
                                            if (done) "✓" else d.dayOfMonth.toString(),
                                            fontSize = 13.sp,
                                            fontWeight = if (isToday || done) FontWeight.Bold else FontWeight.Normal,
                                            color = if (done) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                Text(
                                    d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.CHINESE),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Text(
                                    if (dayMinutes[i] > 0) "${dayMinutes[i]}分" else "—",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }

                    if (weekCheckins >= daysTarget) {
                        Text(
                            "🎉 本周满贯!目标已全部达成,给自己点个赞!",
                            fontSize = 15.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            "再坚持 ${daysTarget - weekCheckins} 天打卡即达成本周目标,加油!",
                            fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // 目标进度
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GoalBar("📚 背单词", weekWords, wordsTarget, "个")
                    GoalBar("🎧 听力精听", weekListenMin, listenTarget, "分钟")
                    GoalBar("⏱️ 学习时长", weekStudyMin, studyTarget, "分钟")
                    GoalBar("✅ 打卡天数", weekCheckins, daysTarget, "天")
                }
            }
        }

        // ===== 累计统计 =====
        item { Text("累计统计", style = MaterialTheme.typography.titleMedium) }

        item {
            val studyDays = remember(refresh) { Repo.totalStudyDays(app.db) }
            val learned = remember(refresh) { Repo.countLearned(app.db) }
            val listenSec = remember(refresh) { Repo.totalListenSeconds(app.db) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("累计学习", "$studyDays", "天", Modifier.weight(1f))
                StatCard("累计背词", "$learned", "个", Modifier.weight(1f))
                StatCard("累计听力", "${listenSec / 60}", "分钟", Modifier.weight(1f))
            }
        }

        // 打卡日历
        item {
            var ym by remember { mutableStateOf(YearMonth.now()) }
            val monthDays = remember(refresh, ym) { Repo.monthCheckinDays(app.db, ym.toString()) }
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("打卡日历", style = MaterialTheme.typography.titleMedium)
                        Row {
                            TextButton(onClick = { ym = ym.minusMonths(1) }) { Text("‹") }
                            Text(ym.toString(), fontSize = 15.sp, modifier = Modifier.padding(horizontal = 4.dp))
                            TextButton(onClick = { ym = ym.plusMonths(1) }, enabled = ym.isBefore(YearMonth.now())) { Text("›") }
                        }
                    }
                    MonthCalendar(days = monthDays)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "✓ 表示已打卡(完成40分钟计划或手动打卡)",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 生词本
        item {
            val wordbook = remember(refresh) { Repo.wordbook(app.db) }
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("📖 生词本(${wordbook.size})", style = MaterialTheme.typography.titleMedium)
                        Button(onClick = onReviewWordbook, enabled = wordbook.isNotEmpty()) {
                            Text("去复习", fontSize = 16.sp)
                        }
                    }
                    if (wordbook.isEmpty()) {
                        Text("生词本是空的,快去背单词吧!", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                    } else {
                        wordbook.take(50).forEach { w ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(w.term, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.9f))
                                Surface(
                                    color = if (w.rating == "no") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        if (w.rating == "no") "不会" else "模糊",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    w.meaning, fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1.4f).padding(start = 8.dp),
                                    maxLines = 1
                                )
                            }
                        }
                        if (wordbook.size > 50) {
                            Text("仅显示前50个,点「去复习」逐个攻克", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // 外观(快捷,详细设置在设置页)
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("外观", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(onClick = { onMode(ThemeMode.SYSTEM) }) {
                            Text(if (mode == ThemeMode.SYSTEM) "跟随系统 ✓" else "跟随系统", fontSize = 15.sp,
                                color = if (mode == ThemeMode.SYSTEM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                        TextButton(onClick = { onMode(ThemeMode.LIGHT) }) {
                            Text(if (mode == ThemeMode.LIGHT) "浅色 ✓" else "浅色", fontSize = 15.sp,
                                color = if (mode == ThemeMode.LIGHT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                        TextButton(onClick = { onMode(ThemeMode.DARK) }) {
                            Text(if (mode == ThemeMode.DARK) "深色 🌙 ✓" else "深色 🌙", fontSize = 15.sp,
                                color = if (mode == ThemeMode.DARK) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Text(
                        "本应用完全离线:无网络权限、无账号、数据只存在本机。",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 设置入口
        item {
            Card(onClick = onOpenSettings) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("⚙️ 设置", style = MaterialTheme.typography.titleMedium)
                        Text("字号、每轮词数、震动反馈、数据管理", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("›", fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }

    // 目标编辑对话框
    if (showEditGoals == 1) {
        var w by remember { mutableStateOf(app.prefs.getInt("goal_words", 100).toString()) }
        var l by remember { mutableStateOf(app.prefs.getInt("goal_listen", 60).toString()) }
        var s by remember { mutableStateOf(app.prefs.getInt("goal_study", 240).toString()) }
        var d by remember { mutableStateOf(app.prefs.getInt("goal_days", 5).toString()) }
        AlertDialog(
            onDismissRequest = { showEditGoals = 0 },
            title = { Text("设定本周目标") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = w, onValueChange = { w = it.filter { c -> c.isDigit() }.take(5) }, label = { Text("每周背词(个)") }, singleLine = true)
                    OutlinedTextField(value = l, onValueChange = { l = it.filter { c -> c.isDigit() }.take(5) }, label = { Text("每周听力(分钟)") }, singleLine = true)
                    OutlinedTextField(value = s, onValueChange = { s = it.filter { c -> c.isDigit() }.take(5) }, label = { Text("每周学习时长(分钟)") }, singleLine = true)
                    OutlinedTextField(value = d, onValueChange = { d = it.filter { c -> c.isDigit() }.take(1) }, label = { Text("每周打卡(天,1-7)") }, singleLine = true)
                    Text("建议:工作日每天40分钟通勤计划,周末自由复习。", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    app.prefs.edit()
                        .putInt("goal_words", w.toIntOrNull()?.coerceIn(10, 99999) ?: 100)
                        .putInt("goal_listen", l.toIntOrNull()?.coerceIn(10, 99999) ?: 60)
                        .putInt("goal_study", s.toIntOrNull()?.coerceIn(30, 99999) ?: 240)
                        .putInt("goal_days", d.toIntOrNull()?.coerceIn(1, 7) ?: 5)
                        .apply()
                    showEditGoals = 0
                    refresh++
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showEditGoals = 0 }) { Text("取消") } }
        )
    }
}

@Composable
private fun GoalBar(label: String, value: Int, target: Int, unit: String) {
    val frac = if (target <= 0) 1f else (value.toFloat() / target).coerceIn(0f, 1f)
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 16.sp)
            Text(
                "$value / $target $unit" + if (value >= target) " ✓" else "",
                fontSize = 15.sp,
                color = if (value >= target) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (value >= target) FontWeight.Bold else FontWeight.Normal
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { frac },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = if (value >= target) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(unit, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(start = 3.dp, bottom = 5.dp))
            }
            Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
