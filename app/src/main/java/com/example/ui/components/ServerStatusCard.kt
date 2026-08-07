package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ServerStatusCard(
    isRunning: Boolean,
    ipAddress: String,
    port: Int,
    pin: String,
    onToggleServer: (Boolean) -> Unit,
    onRegeneratePin: () -> Unit
) {
    val context = LocalContext.current
    val serverUrl = "http://$ipAddress:$port"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isRunning) Color(0xFF22C55E) else Color(0xFFEF4444)
                    ) {
                        Box(modifier = Modifier.size(12.dp))
                    }
                    Column {
                        Text(
                            text = if (isRunning) "Server Active" else "Server Stopped",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isRunning) "Accessible on Local Network (LAN)" else "Tap switch to enable Web Access",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isRunning,
                    onCheckedChange = onToggleServer
                )
            }

            if (isRunning) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Web Address",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = serverUrl,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Access PIN",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = pin,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = onRegeneratePin, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Regenerate PIN",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // QR Code Box
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SimpleQrCanvas(data = serverUrl)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("URL", serverUrl))
                            Toast.makeText(context, "URL Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Link")
                    }

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("PIN", pin))
                            Toast.makeText(context, "PIN Copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy PIN")
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleQrCanvas(data: String) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val size = size.minDimension
        val grid = 15
        val cellSize = size / grid

        drawRoundRect(
            color = Color.White,
            size = Size(size, size),
            cornerRadius = CornerRadius(8f, 8f)
        )

        // Draw deterministic QR pattern grid based on string hash
        val hash = data.hashCode()
        for (r in 0 until grid) {
            for (c in 0 until grid) {
                // Draw position square markers at corners
                val isFinderTopLeft = r in 0..4 && c in 0..4
                val isFinderTopRight = r in 0..4 && c in (grid - 5) until grid
                val isFinderBottomLeft = r in (grid - 5) until grid && c in 0..4

                val isBlack = when {
                    isFinderTopLeft || isFinderTopRight || isFinderBottomLeft -> {
                        val isEdge = (r == 0 || r == 4 || c == 0 || c == 4 || (r in 0..4 && (c == grid - 5 || c == grid - 1)) || (c in 0..4 && (r == grid - 5 || r == grid - 1)))
                        val isCenter = (r in 1..3 && c in 1..3) || (r in 1..3 && c in (grid - 4)..(grid - 2)) || (r in (grid - 4)..(grid - 2) && c in 1..3)
                        isEdge || isCenter
                    }
                    else -> ((hash xor (r * 31 + c * 17)) and 1) == 0
                }

                if (isBlack) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(c * cellSize, r * cellSize),
                        size = Size(cellSize, cellSize)
                    )
                }
            }
        }
    }
}
