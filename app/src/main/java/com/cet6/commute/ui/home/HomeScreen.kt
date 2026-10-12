package com.cet6.commute.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cet6.commute.App
import com.cet6.commute.data.Repo
import com.cet6.commute.logic.SessionTimer
import com.cet6.commute.ui.MonthCalendar
import com.cet6.commute.ui.StudySub
import com.cet6.commute.ui.fmtSec
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun HomeScreen(onGoStudy: (StudySub) -> Unit) {
    val app = LocalContext.current.applicationContext as App
    val st by SessionTimer.st.collectAsState()

    var refresh by remember { mutableIntStateOf(0) }
    val today = LocalDate.now().toString()
    val checkedToday = remember(refresh) { Repo.isCheckedIn(app.db, today) }
    val monthDays = remember(refresh) { Repo.monthCheckinDays(app.db, today.substring(0, 7)) }
    val streak = remember(refresh) { Repo.streak(Repo.allCheckinDays(app.db)) }

    var celebrated by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(st.done) { if (st.done) refresh++ }
    if (st.done && !celebrated) {
        AlertDialog(
            onDismissRequest = { celebrated = true },
            title = { Text("🎉 完成40分钟!") },
            text = { Text("今天的三阶段学习计划已完成,已自动打卡。\n保持节奏,六级稳了!") },
            confirmButton = { TextButton(onClick = { celebrated = true }) { Text("好的") } }
        )
    }

    val h = LocalTime.now().hour
    val greet = when {
        h < 11 -> "早上好"
        h < 14 -> "中午好"
        h < 18 -> "下午好"
        else -> "晚上好"
    }

    val phaseIdx = SessionTimer.phaseIndex(st.elapsedSec)
    val phase = SessionTimer.PHASES[phaseIdx]
    val phaseRemain = if (st.done) 0 else (phase.endSec - st.elapsedSec).coerceAtLeast(0)
    val progress = (st.elapsedSec.toFloat() / SessionTimer.TOTAL_SEC).coerceIn(0f, 1f)
    val animProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(600), label = "progress")

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 顶部问候
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("$greet,该学习啦", style = MaterialTheme.typography.headlineMedium)
                Text(today, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
            }
            Text("六级\n必胜", textAlign = androidx.compose.ui.text.style.TextAlign.End,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        // 今日概览
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val todayWords = remember(refresh) { Repo.wordsSeenOn(app.db, LocalDate.now().toEpochDay().toInt()) }
            val todayListen = remember(refresh) { Repo.listenSecondsBetween(app.db, today, today) / 60 }
            val todayMinutes = remember(refresh) { Repo.studySecondsOn(app.db, today) / 60 }
            TodayChip("📖 背词 $todayWords", Modifier.weight(1f))
            TodayChip("🎧 听力 ${todayListen}分", Modifier.weight(1f))
            TodayChip("⏱️ 合计 ${todayMinutes}分", Modifier.weight(1f))
        }

        // 计时总控
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("阶段${phaseIdx + 1} · ${phase.name}", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    fmtSec(phaseRemain),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "总进度 ${fmtSec(st.elapsedSec)} / 40:00",
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f), fontSize = 15.sp
                )
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(progress = { animProgress }, modifier = Modifier.fillMaxWidth().height(10.dp))
                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { SessionTimer.toggle() },
                        enabled = !st.done,
                        modifier = Modifier.height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            when {
                                st.done -> "已完成 ✓"
                                st.running -> "⏸ 暂停"
                                st.elapsedSec > 0 -> "▶ 继续"
                                else -> "▶ 开始学习"
                            },
                            fontSize = 19.sp
                        )
                    }
                    if (st.elapsedSec > 0 && !st.running) {
                        OutlinedButton(onClick = { SessionTimer.reset() }, modifier = Modifier.height(56.dp)) {
                            Text("重置", fontSize = 17.sp)
                        }
                    }
                }
            }
        }

        // 阶段指示
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SessionTimer.PHASES.forEachIndexed { i, p ->
                val done = st.elapsedSec >= p.endSec
                val cur = i == phaseIdx && !st.done
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        done -> MaterialTheme.colorScheme.secondary
                        cur -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            (if (done) "✓ " else if (cur) "▶ " else "") + p.name,
                            fontSize = 14.sp,
                            color = when {
                                done -> MaterialTheme.colorScheme.onSecondary
                                cur -> MaterialTheme.colorScheme.onPrimary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = if (cur) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            "${p.startSec / 60}-${p.endSec / 60}分钟",
                            fontSize = 12.sp,
                            color = when {
                                done -> MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.85f)
                                cur -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }

        // 快捷入口
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickEntry("📖", "背单词", Modifier.weight(1f)) { onGoStudy(StudySub.VOCAB) }
            QuickEntry("🎧", "练听力", Modifier.weight(1f)) { onGoStudy(StudySub.LISTEN) }
            QuickEntry("✍️", "背作文", Modifier.weight(1f)) { onGoStudy(StudySub.ESSAY) }
        }

        // 打卡 + 日历
        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("🔥 连续打卡 $streak 天", style = MaterialTheme.typography.titleMedium)
                        Text(
                            when {
                                streak >= 7 -> "太强了,这份坚持会兑现成分数!"
                                streak >= 3 -> "状态拉满,继续保持!"
                                streak >= 1 -> "坚持就是胜利!"
                                else -> "今天开始也不晚"
                            },
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    OutlinedButton(
                        onClick = { Repo.checkin(app.db, auto = false); refresh++ },
                        enabled = !checkedToday
                    ) {
                        Text(if (checkedToday) "今日已打卡 ✓" else "打卡", fontSize = 16.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                MonthCalendar(days = monthDays, compact = true)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TodayChip(text: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Text(
            text, fontSize = 14.sp, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun QuickEntry(icon: String, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier = modifier, onClick = onClick, colors = CardDefaults.cardColors()) {
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 30.sp)
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}
