package com.prime.doraemon.admin.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prime.doraemon.admin.data.api.SupabaseManager
import com.prime.doraemon.admin.data.model.ContentItem
import com.prime.doraemon.admin.ui.components.*
import com.prime.doraemon.admin.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentLibraryScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var contentItems by remember { mutableStateOf<List<ContentItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    var selectedItemForDetail by remember { mutableStateOf<ContentItem?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    fun loadContent() {
        scope.launch {
            isLoading = true
            val res = SupabaseManager.fetchContentItems()
            if (res.isSuccess) {
                contentItems = res.getOrNull() ?: emptyList()
            } else {
                Toast.makeText(context, "Error: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadContent()
    }

    val filteredItems = remember(contentItems, searchQuery) {
        if (searchQuery.isBlank()) contentItems
        else {
            val q = searchQuery.lowercase()
            contentItems.filter {
                it.key.lowercase().contains(q) ||
                (it.caption ?: "").lowercase().contains(q) ||
                (it.textContent ?: "").lowercase().contains(q) ||
                it.contentType.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "STORED KEYS & ASSETS",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Content Library",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    IconButton(
                        onClick = { loadContent() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(DarkSurface)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = DoraemonBlue)
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by key, caption, type...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DoraemonBlue,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonCyan)
                    }
                }
            } else if (filteredItems.isEmpty()) {
                item {
                    GlassCard {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No items matched '$searchQuery'." else "Content library is empty. Generate a key to get started.",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(filteredItems, key = { it.id }) { item ->
                    GlassCard(
                        onClick = { selectedItemForDetail = item }
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = item.key,
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    TypeBadge(type = item.contentType)
                                }
                                StatusBadge(active = item.active, isExpired = item.expiresAt != null)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            val previewText = item.caption ?: item.textContent ?: (if (item.storagePath != null) "Cloud Storage Media" else "No description")
                            Text(
                                text = previewText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                maxLines = 2
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Expires: ${item.expiresAt?.take(10) ?: "Never"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                Text(
                                    text = "Details →",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DoraemonBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Detail Bottom Sheet / Dialog
    selectedItemForDetail?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedItemForDetail = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = item.key, color = TextPrimary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    TypeBadge(type = item.contentType)
                }
            },
            containerColor = DarkSurface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "DETAILS & ACTIONS", style = MaterialTheme.typography.labelSmall, color = TextMuted)

                    if (!item.caption.isNullOrEmpty()) {
                        Text(text = "Caption: ${item.caption}", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }

                    if (!item.textContent.isNullOrEmpty()) {
                        Text(text = "Text Content: ${item.textContent}", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }

                    if (!item.storagePath.isNullOrEmpty()) {
                        Text(text = "Storage URL: ${item.storagePath}", color = NeonCyan, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                    }

                    if (!item.telegramFileId.isNullOrEmpty()) {
                        Text(text = "Telegram File ID: ${item.telegramFileId}", color = TextMuted, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Status:", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = item.active,
                            onCheckedChange = { newStatus ->
                                scope.launch {
                                    val res = SupabaseManager.updateContentItemActive(item.id, newStatus)
                                    if (res.isSuccess) {
                                        selectedItemForDetail = item.copy(active = newStatus)
                                        loadContent()
                                    }
                                }
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Key", item.key))
                                Toast.makeText(context, "Key copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DoraemonBlue)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Key")
                        }

                        if (!item.storagePath.isNullOrEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.storagePath))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Open Link")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val res = SupabaseManager.deleteContentItem(item.id)
                            if (res.isSuccess) {
                                Toast.makeText(context, "Content and key deleted", Toast.LENGTH_SHORT).show()
                                selectedItemForDetail = null
                                loadContent()
                            } else {
                                Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCoral)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Content & Key")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedItemForDetail = null }) {
                    Text("Close", color = TextMuted)
                }
            }
        )
    }
}
