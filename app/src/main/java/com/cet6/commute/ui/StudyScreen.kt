package com.cet6.commute.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.cet6.commute.ui.listen.ListenScreen
import com.cet6.commute.ui.listen.PapersScreen
import com.cet6.commute.ui.read.EssayScreen
import com.cet6.commute.ui.vocab.VocabScreen

/** 「学习」一级页:内部子栏目(词汇/听力/套卷/作文),切换时水平滑动过渡 */
@Composable
fun StudyScreen(
    sub: StudySub,
    onSub: (StudySub) -> Unit,
    vocabMode: String,
    newLimit: Int,
    onNewLimit: (Int) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = StudySub.entries.indexOf(sub)) {
            StudySub.entries.forEach { s ->
                Tab(
                    selected = sub == s,
                    onClick = { onSub(s) },
                    text = { Text(s.label, fontSize = 16.sp) }
                )
            }
        }
        AnimatedContent(
            targetState = sub,
            transitionSpec = {
                val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (slideInHorizontally(tween(260)) { w -> dir * w / 5 } + fadeIn(tween(220)))
                    .togetherWith(fadeOut(tween(160)))
            },
            label = "study-sub"
        ) { s ->
            when (s) {
                StudySub.VOCAB -> VocabScreen(mode = vocabMode, newLimit = newLimit, onNewLimit = onNewLimit)
                StudySub.LISTEN -> ListenScreen()
                StudySub.TEST -> PapersScreen()
                StudySub.ESSAY -> EssayScreen()
            }
        }
    }
}
