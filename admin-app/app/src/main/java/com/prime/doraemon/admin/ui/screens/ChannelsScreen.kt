package com.prime.doraemon.admin.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prime.doraemon.admin.data.api.SupabaseManager
import com.prime.doraemon.admin.data.model.ChannelItem
import com.prime.doraemon.admin.ui.components.*
import com.prime.doraemon.admin.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ChannelsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var channels by remember { mutableStateOf<List<ChannelItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Dialog form state
    var newChannelName by remember { mutableStateOf("") }
    var newChannelId by remember { mutableStateOf("") }
    var newInviteLink by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    fun loadChannels() {
        scope.launch {
            isLoading = true
            val result = SupabaseManager.fetchChannels()
            if (result.isSuccess) {
                channels = result.getOrNull() ?: emptyList()
            } else {
                Toast.makeText(context, "Failed to load channels: ${result.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadChannels()
    }

    Scaffold(
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = DoraemonBlue,
                contentColor = TextPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Channel")
            }
        }
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
                            text = "PRIME DORAEMON BOT",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Mandatory Channels",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    IconButton(
                        onClick = { loadChannels() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(DarkSurface)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = DoraemonBlue)
                    }
                }
            }

            item {
                GlassCard(
                    backgroundColor = DarkSurfaceElevated,
                    borderColor = DoraemonBlue.copy(alpha = 0.3f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = DoraemonBlue)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Users must join all active channels below before the bot permits key redemption. Any changes take effect immediately without updating bot.py.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonCyan)
                    }
                }
            } else if (channels.isEmpty()) {
                item {
                    GlassCard {
                        Text(
                            text = "No channels added yet. Tap '+ Add Channel' to create your first mandatory channel.",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(channels, key = { it.id }) { channel ->
                    GlassCard {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = channel.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "ID: ${channel.channelId}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = NeonCyan
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = channel.inviteLink,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted,
                                        maxLines = 1
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = channel.active,
                                        onCheckedChange = { newStatus ->
                                            scope.launch {
                                                val res = SupabaseManager.updateChannelActive(channel.id, newStatus)
                                                if (res.isSuccess) {
                                                    loadChannels()
                                                } else {
                                                    Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = TextPrimary,
                                            checkedTrackColor = DoraemonBlue,
                                            uncheckedThumbColor = TextMuted,
                                            uncheckedTrackColor = DarkSurfaceElevated
                                        )
                                    )

                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                val res = SupabaseManager.deleteChannel(channel.id)
                                                if (res.isSuccess) {
                                                    Toast.makeText(context, "Channel removed", Toast.LENGTH_SHORT).show()
                                                    loadChannels()
                                                } else {
                                                    Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentCoral)
                                    }
                                }
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

    // Add Channel Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Mandatory Channel", color = TextPrimary, fontWeight = FontWeight.Bold) },
            containerColor = DarkSurface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newChannelName,
                        onValueChange = { newChannelName = it },
                        label = { Text("Channel Name") },
                        placeholder = { Text("e.g. PRIME Updates") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DoraemonBlue,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newChannelId,
                        onValueChange = { newChannelId = it },
                        label = { Text("Telegram Channel ID") },
                        placeholder = { Text("e.g. -1001234567890 or @mychannel") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DoraemonBlue,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newInviteLink,
                        onValueChange = { newInviteLink = it },
                        label = { Text("Invite / Join Link") },
                        placeholder = { Text("https://t.me/mychannel") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DoraemonBlue,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newChannelName.isBlank() || newChannelId.isBlank() || newInviteLink.isBlank()) {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch {
                            isSaving = true
                            val newItem = ChannelItem(
                                name = newChannelName.trim(),
                                channelId = newChannelId.trim(),
                                inviteLink = newInviteLink.trim(),
                                active = true
                            )
                            val res = SupabaseManager.createChannel(newItem)
                            isSaving = false
                            if (res.isSuccess) {
                                Toast.makeText(context, "Channel added successfully!", Toast.LENGTH_SHORT).show()
                                showAddDialog = false
                                newChannelName = ""
                                newChannelId = ""
                                newInviteLink = ""
                                loadChannels()
                            } else {
                                Toast.makeText(context, "Failed: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DoraemonBlue),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TextPrimary)
                    } else {
                        Text("Add Channel")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}
