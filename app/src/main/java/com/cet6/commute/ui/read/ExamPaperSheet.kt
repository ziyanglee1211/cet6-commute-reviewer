package com.cet6.commute.ui.read

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import com.cet6.commute.ui.theme.LocalExamPaperColors
import com.cet6.commute.ui.theme.LocalFontScale

/**
 * 标准考试卷面(仿 CET-6 真题卷):
 * 粗边框 + 卷头信息 + 居中标题 + Directions + 衬线正文(首行缩进/行距) + 词数统计 + 署名区
 */
@Composable
fun ExamPaperSheet(
    title: String,
    category: String,
    directions: String,
    bodyText: String,
    modifier: Modifier = Modifier,
    headerLine: String = "CET-6  ·  Part I Writing  ·  30 minutes",
    bodyContent: (@Composable () -> Unit)? = null
) {
    val scale = LocalFontScale.current
    val pc = LocalExamPaperColors.current
    val wordCount = remember(bodyText) { bodyText.split(Regex("\\s+")).count { it.isNotBlank() } }
    val inRange = wordCount in 150..200

    // 入场动画:卷面淡入 + 轻微上移
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = pc.paper),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, pc.line),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        AnimatedVisibility(
            visible = shown,
            enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 6 }
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // ===== 卷头 =====
                Text(
                    headerLine,
                    fontSize = 12.sp * scale,
                    letterSpacing = 1.5.sp,
                    color = pc.faint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                // 试卷标志性双细线(粗+细)
                Box(Modifier.fillMaxWidth().height(2.dp).background(pc.line))
                Spacer(Modifier.height(2.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(pc.line))

                // ===== 标题区 =====
                Spacer(Modifier.height(18.dp))
                Text(
                    title,
                    fontSize = 21.sp * scale,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = pc.ink,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp * scale,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    category,
                    fontSize = 13.sp * scale,
                    fontStyle = FontStyle.Italic,
                    color = pc.faint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(pc.line.copy(alpha = 0.6f)))

                // ===== Directions =====
                if (directions.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        directions,
                        fontSize = 14.sp * scale,
                        fontFamily = FontFamily.Serif,
                        color = pc.faint,
                        lineHeight = 22.sp * scale
                    )
                    Spacer(Modifier.height(14.dp))
                }

                // ===== 正文 =====
                if (bodyContent != null) {
                    bodyContent()
                } else {
                    ExamPaperBody(bodyText, scale, pc.ink)
                }

                Spacer(Modifier.height(20.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(pc.line.copy(alpha = 0.6f)))

                // ===== 词数统计 =====
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "词数 Words: $wordCount",
                        fontSize = 14.sp * scale,
                        fontFamily = FontFamily.Serif,
                        color = pc.ink,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        when {
                            wordCount < 150 -> "低于 150 词要求"
                            wordCount > 200 -> "超出 200 词上限"
                            else -> "符合 150–200 词要求 ✓"
                        },
                        fontSize = 13.sp * scale,
                        color = if (inRange) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                    )
                }

                // ===== 署名区 =====
                Spacer(Modifier.height(14.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(pc.line.copy(alpha = 0.4f)))
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ExamSignature("Name", scale, pc.faint)
                    ExamSignature("Class", scale, pc.faint)
                    ExamSignature("Score", scale, pc.faint)
                }
            }
        }
    }
}

/** 试卷正文:衬线字体、首行缩进、宽松行距、段间空行 */
@Composable
private fun ExamPaperBody(text: String, scale: Float, ink: androidx.compose.ui.graphics.Color) {
    val paragraphs = remember(text) { text.split("\n").map { it.trim() }.filter { it.isNotBlank() } }
    val annotated = remember(paragraphs, scale) {
        buildAnnotatedString {
            paragraphs.forEachIndexed { i, p ->
                withStyle(
                    ParagraphStyle(
                        textIndent = TextIndent(firstLine = 28.sp * scale),
                        lineHeight = 31.sp * scale
                    )
                ) {
                    append(p)
                }
                if (i < paragraphs.lastIndex) append("\n\n")
            }
        }
    }
    Text(
        annotated,
        style = TextStyle(
            fontSize = 18.sp * scale,
            fontFamily = FontFamily.Serif,
            color = ink,
            lineHeight = 31.sp * scale
        )
    )
}

@Composable
private fun ExamSignature(label: String, scale: Float, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            "$label:",
            fontSize = 13.sp * scale,
            fontFamily = FontFamily.Serif,
            color = color
        )
        Box(
            Modifier
                .padding(start = 6.dp)
                .height(14.dp)
                .width(72.dp)
                .background(color.copy(alpha = 0.35f))
        )
    }
}

/** 字数统计行(供非试卷视图复用) */
@Composable
fun WordCountRow(text: String, modifier: Modifier = Modifier) {
    val scale = LocalFontScale.current
    val count = remember(text) { text.split(Regex("\\s+")).count { it.isNotBlank() } }
    Row(modifier, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("词数 $count", fontSize = 13.sp * scale, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            when {
                count < 150 -> "不足150词"
                count > 200 -> "超出200词"
                else -> "150–200 ✓"
            },
            fontSize = 13.sp * scale,
            color = if (count in 150..200) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
