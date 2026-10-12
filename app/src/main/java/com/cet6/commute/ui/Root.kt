package com.cet6.commute.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.cet6.commute.App
import com.cet6.commute.ui.home.HomeScreen
import com.cet6.commute.ui.listen.ListenScreen
import com.cet6.commute.ui.listen.PapersScreen
import com.cet6.commute.ui.profile.ProfileScreen
import com.cet6.commute.ui.read.EssayScreen
import com.cet6.commute.ui.settings.SettingsScreen
import com.cet6.commute.ui.theme.AppTheme
import com.cet6.commute.ui.theme.FONT_SCALES
import com.cet6.commute.ui.theme.ThemeMode
import com.cet6.commute.ui.vocab.VocabScreen

/** 底部导航(三个一级目的地) */
enum class Tab(val label: String, val icon: String) {
    HOME("今日", "🏠"),
    STUDY("学习", "📚"),
    PROFILE("我的", "👤")
}

/** 「学习」页内的子栏目 */
enum class StudySub(val label: String, val icon: String) {
    VOCAB("词汇", "📖"),
    LISTEN("听力", "🎧"),
    TEST("套卷", "📗"),
    ESSAY("作文", "✍️")
}

@Composable
fun Root() {
    val app = LocalContext.current.applicationContext as App
    var mode by rememberSaveable {
        mutableStateOf(
            when (app.themeMode) {
                1 -> ThemeMode.LIGHT
                2 -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        )
    }
    var fontLevel by remember { mutableStateOf(app.fontScaleLevel) }
    var wordLimit by remember { mutableStateOf(app.newWordLimit) }

    AppTheme(mode, FONT_SCALES[fontLevel]) {
        var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
        var studySub by rememberSaveable { mutableStateOf(StudySub.VOCAB) }
        var vocabMode by rememberSaveable { mutableStateOf("daily") }
        var showSettings by rememberSaveable { mutableStateOf(false) }

        if (showSettings) {
            SettingsScreen(
                mode = mode,
                onMode = { m -> mode = m; app.themeMode = m.ordinal },
                fontLevel = fontLevel,
                onFontLevel = { l -> fontLevel = l; app.fontScaleLevel = l },
                wordLimit = wordLimit,
                onWordLimit = { n -> wordLimit = n; app.newWordLimit = n },
                onBack = { showSettings = false }
            )
            return@AppTheme
        }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Text(t.icon, fontSize = 23.sp) },
                            label = { Text(t.label, fontSize = 13.sp) }
                        )
                    }
                }
            }
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                Crossfade(
                    targetState = tab,
                    animationSpec = tween(280),
                    label = "tab"
                ) { t ->
                    when (t) {
                        Tab.HOME -> HomeScreen(
                            onGoStudy = { sub -> studySub = sub; tab = Tab.STUDY }
                        )
                        Tab.STUDY -> StudyScreen(
                            sub = studySub,
                            onSub = { studySub = it },
                            vocabMode = vocabMode,
                            newLimit = wordLimit,
                            onNewLimit = { n -> wordLimit = n; app.newWordLimit = n }
                        )
                        Tab.PROFILE -> ProfileScreen(
                            onReviewWordbook = { vocabMode = "wordbook"; studySub = StudySub.VOCAB; tab = Tab.STUDY },
                            onOpenSettings = { showSettings = true },
                            mode = mode,
                            onMode = { m -> mode = m; app.themeMode = m.ordinal }
                        )
                    }
                }
            }
        }
    }
}
