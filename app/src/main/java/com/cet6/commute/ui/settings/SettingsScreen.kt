package com.cet6.commute.ui.settings

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.cet6.commute.ui.theme.FONT_SCALE_LABELS
import com.cet6.commute.ui.theme.FONT_SCALES
import com.cet6.commute.ui.theme.ThemeMode

/** 每轮新词数候选 */
private val WORD_LIMITS = listOf(10, 20, 30, 40, 60)

/** 设置页:字号 / 每轮词数 / 主题 / 震动 / 数据管理 / 关于 */
@Composable
fun SettingsScreen(
    mode: ThemeMode,
    onMode: (ThemeMode) -> Unit,
    fontLevel: Int,
    onFontLevel: (Int) -> Unit,
    wordLimit: Int,
    onWordLimit: (Int) -> Unit,
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as App
    var vibrate by remember { mutableStateOf(app.vibrateOnFlip) }
    var showClear by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 顶栏
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("←", fontSize = 24.sp) }
            Text("设置", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 4.dp))
        }

        // ===== 字号 =====
        SettingsCard(title = "🔤 字号", desc = "调节全 App 文字大小,通勤路上看得更清楚") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FONT_SCALE_LABELS.forEachIndexed { i, label ->
                    FilterChip(
                        selected = fontLevel == i,
                        onClick = { onFontLevel(i) },
                        label = { Text(label, fontSize = 15.sp) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            // 预览
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("预览效果", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("abandon /əˈbændən/ v. 放弃;抛弃", fontSize = 16.sp)
                    Text("正文示例:听力精听时先听大意,再逐句对照原文。", fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // ===== 每轮新词数 =====
        SettingsCard(title = "📖 每轮新词数", desc = "词汇闪卡每轮学习的新词上限(当前 $wordLimit 个/轮)") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WORD_LIMITS.forEach { n ->
                    FilterChip(
                        selected = wordLimit == n,
                        onClick = { onWordLimit(n) },
                        label = { Text("$n", fontSize = 15.sp) }
                    )
                }
            }
            Text(
                "2000 词库按当前速度约需 ${(2000 + wordLimit - 1) / wordLimit} 天学完;复习队列不受此限制,到期就会推送。",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ===== 主题 =====
        SettingsCard(title = "🎨 主题", desc = "浅色适合白天,深色适合夜间通勤") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(selected = mode == ThemeMode.SYSTEM, onClick = { onMode(ThemeMode.SYSTEM) }, label = { Text("跟随系统") })
                FilterChip(selected = mode == ThemeMode.LIGHT, onClick = { onMode(ThemeMode.LIGHT) }, label = { Text("浅色") })
                FilterChip(selected = mode == ThemeMode.DARK, onClick = { onMode(ThemeMode.DARK) }, label = { Text("深色 🌙") })
            }
        }

        // ===== 震动反馈 =====
        SettingsCard(title = "📳 翻牌震动", desc = "翻闪卡和标记掌握时轻微震动反馈") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (vibrate) "已开启" else "已关闭", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Switch(
                    checked = vibrate,
                    onCheckedChange = { v -> vibrate = v; app.vibrateOnFlip = v }
                )
            }
        }

        // ===== 数据管理 =====
        SettingsCard(title = "🗄 数据管理", desc = "所有数据仅存本机,清空后不可恢复") {
            OutlinedButton(
                onClick = { showClear = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("清空学习记录") }
            Text(
                "将清空:打卡日历、学习时长、听力时长、词汇进度(恢复到「未学习」状态)。生词本、自定义导入的词表和范文不受影响。",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ===== 关于 =====
        SettingsCard(title = "ℹ️ 关于", desc = null) {
            Text("六级通勤复习器", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("版本 1.4.1 · 纯离线 · 无网络权限", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "为通勤碎片时间设计:40 分钟三阶段(词汇闪卡 / 听力精听 / 写作),进度自动持久化,中断后可续学。",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(8.dp))
    }

    // 清空数据二次确认
    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text("⚠️ 确认清空学习记录?") },
            text = {
                Text("此操作不可恢复!将清空打卡日历、学习/听力时长和全部词汇进度,2000 词恢复为未学习状态。生词本与自定义内容保留。")
            },
            confirmButton = {
                TextButton(onClick = {
                    Repo.clearStudyProgress(app.db)
                    com.cet6.commute.logic.SessionTimer.reset()
                    showClear = false
                }) { Text("确认清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showClear = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun SettingsCard(title: String, desc: String?, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (desc != null) {
                Text(desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(2.dp))
            content()
        }
    }
}
