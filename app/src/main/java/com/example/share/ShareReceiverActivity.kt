package com.example.share

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.MainActivity
import com.example.data.database.AppDatabase
import com.example.data.domain.InboxRepository
import com.example.data.domain.SaveResult
import com.example.data.preferences.SettingsRepository
import com.example.data.storage.FileStorageManager
import com.example.ui.theme.Theme
import kotlinx.coroutines.launch

class ShareReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val parsed = ShareParser.parseIntent(intent)
        if (parsed == null || (parsed.text.isNull_or_blank() && parsed.uris.isEmpty())) {
            Toast.makeText(this, "Nothing to save", Toast.LENGTH_SHORT).show()
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
            // Quick save by default or show quick dialog
            saveSharedContent(parsed, repository)
        }
    }

    private suspend fun saveSharedContent(parsed: ParsedShareContent, repository: InboxRepository) {
        var count = 0
        if (!parsed.text.isNull_or_blank()) {
            repository.saveTextOrUrl(parsed.text!!, sourceApp = parsed.sourceApp ?: "")
            count++
        }

        for (uri in parsed.uris) {
            when (val result = repository.saveUri(contentResolver, uri, sourceApp = parsed.sourceApp ?: "", overrideMime = parsed.mimeType)) {
                is SaveResult.Success -> count++
                is SaveResult.Duplicate -> {
                    Toast.makeText(this, "Duplicate file skipped: ${result.existingItem.title}", Toast.LENGTH_SHORT).show()
                }
                is SaveResult.Error -> {
                    Toast.makeText(this, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (count > 0) {
            Toast.makeText(this, "Saved $count item(s) to Inbox", Toast.LENGTH_SHORT).show()
        }

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(mainIntent)
        finish()
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()
