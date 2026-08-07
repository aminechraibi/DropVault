package com.example.share

import android.content.Intent
import android.net.Uri
import android.os.Build

data class ParsedShareContent(
    val text: String? = null,
    val uris: List<Uri> = emptyList(),
    val mimeType: String? = null,
    val sourceApp: String = ""
)

object ShareParser {

    fun parseIntent(intent: Intent): ParsedShareContent? {
        val action = intent.action ?: return null
        val type = intent.type

        val sourcePackage = intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME) ?: ""

        when (action) {
            Intent.ACTION_PROCESS_TEXT -> {
                val selectedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
                if (!selectedText.isNull_or_blank()) {
                    return ParsedShareContent(
                        text = selectedText,
                        mimeType = "text/plain",
                        sourceApp = sourcePackage
                    )
                }
            }
            Intent.ACTION_SEND -> {
                val extraText = intent.getStringExtra(Intent.EXTRA_TEXT)
                val extraStream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }

                if (extraStream != null) {
                    return ParsedShareContent(
                        text = extraText,
                        uris = listOf(extraStream),
                        mimeType = type,
                        sourceApp = sourcePackage
                    )
                } else if (!extraText.isNull_or_blank()) {
                    return ParsedShareContent(
                        text = extraText,
                        mimeType = type ?: "text/plain",
                        sourceApp = sourcePackage
                    )
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val extraStreams = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }

                if (!extraStreams.isNullOrEmpty()) {
                    return ParsedShareContent(
                        uris = extraStreams.filterNotNull(),
                        mimeType = type,
                        sourceApp = sourcePackage
                    )
                }
            }
        }
        return null
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()
