package com.cet6.commute.ui.listen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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

@Composable
fun NotesScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as App
    val ctx = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val notes = remember(refresh) { Repo.allNotes(app.db) }
    var text by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Text("←", fontSize = 26.sp) }
            Text("听力笔记", style = MaterialTheme.typography.headlineMedium)
        }

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("今天听到什么难点?快速记下来…") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                if (text.isNotBlank()) {
                    Repo.addNote(app.db, text)
                    text = ""
                    refresh++
                    android.widget.Toast.makeText(ctx, "已保存", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("保存", fontSize = 17.sp) }

        if (notes.isEmpty()) {
            Card(Modifier.fillMaxWidth().weight(1f)) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("📝", fontSize = 46.sp)
                    Text("还没有笔记", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 17.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(notes, key = { it.id }) { n ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Text(n.day, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold)
                                Text(n.content, fontSize = 16.sp)
                            }
                            IconButton(onClick = { Repo.deleteNote(app.db, n.id); refresh++ }) {
                                Text("🗑", fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
