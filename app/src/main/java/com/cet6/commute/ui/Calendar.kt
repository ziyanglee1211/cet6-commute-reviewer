package com.cet6.commute.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

/**
 * 打卡日历(月视图),days 为已打卡日期集合("yyyy-MM-dd")。
 * compact=true 时隐藏月份切换栏(首页用)。
 */
@Composable
fun MonthCalendar(days: Set<String>, modifier: Modifier = Modifier, compact: Boolean = false) {
    var ym by remember { mutableStateOf(YearMonth.now()) }
    val today = LocalDate.now()

    Column(modifier.fillMaxWidth()) {
        if (!compact) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { ym = ym.minusMonths(1) }) { Text("‹", fontSize = 26.sp) }
                Text("%d年%d月".format(ym.year, ym.monthValue), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { ym = ym.plusMonths(1) }) { Text("›", fontSize = 26.sp) }
            }
        } else {
            Text(
                "%d年%d月打卡".format(ym.year, ym.monthValue),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
        }

        val first = ym.atDay(1)
        val offset = first.dayOfWeek.value - 1 // 周一=0
        val totalCells = offset + ym.lengthOfMonth()
        val rows = (totalCells + 6) / 7
        var day = 1
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (c in 0 until 7) {
                    val cell = r * 7 + c
                    if (cell < offset || day > ym.lengthOfMonth()) {
                        Box(Modifier.size(34.dp))
                        continue
                    }
                    val date = ym.atDay(day)
                    val checked = date.toString() in days
                    val isToday = date == today
                    Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                        if (checked) {
                            Box(
                                Modifier.size(30.dp).background(
                                    MaterialTheme.colorScheme.primary, CircleShape
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontSize = 15.sp)
                            }
                        } else {
                            Text(
                                day.toString(),
                                fontSize = 14.sp,
                                color = if (isToday) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    day++
                }
            }
        }
    }
}
