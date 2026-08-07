package com.example.share

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.MainActivity
import com.example.data.database.AppDatabase
import com.example.data.database.InboxItem
import com.example.data.domain.InboxRepository
import com.example.data.preferences.SettingsRepository
import com.example.data.storage.FileStorageManager
import com.example.ui.theme.Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProcessTextActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val parsed = ShareParser.parseIntent(intent)
        val textToSave = parsed?.text?.trim()

        if (textToSave.isNullOrBlank()) {
            Toast.makeText(this, "No valid text selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val repository = InboxRepository(
            applicationContext,
            AppDatabase.getDatabase(applicationContext).inboxDao(),
            FileStorageManager(applicationContext),
            SettingsRepository(applicationContext)
        )

        lifecycleScope.launch {
            val savedItem = repository.saveTextOrUrl(textToSave, sourceApp = parsed.sourceApp)

            setContent {
                Theme {
                    QuickSavePopupContent(
                        savedItem = savedItem,
                        onUndo = {
                            lifecycleScope.launch {
                                repository.deleteItem(savedItem)
                                Toast.makeText(this@ProcessTextActivity, "Item deleted", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        },
                        onOpenInbox = {
                            val mainIntent = Intent(this@ProcessTextActivity, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                            startActivity(mainIntent)
                            finish()
                        },
                        onAutoDismiss = {
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickSavePopupContent(
    savedItem: InboxItem,
    onUndo: () -> Unit,
    onOpenInbox: () -> Unit,
    onAutoDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(3500)
        onAutoDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Saved",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Saved to Inbox",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (savedItem.text.isNotBlank()) savedItem.text else savedItem.url,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onUndo,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Undo")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = onOpenInbox,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Inbox")
                    }
                }
            }
        }
    }
}
