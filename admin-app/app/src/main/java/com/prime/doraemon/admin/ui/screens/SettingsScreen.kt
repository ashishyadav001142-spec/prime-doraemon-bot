package com.prime.doraemon.admin.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prime.doraemon.admin.PrimeAdminApp
import com.prime.doraemon.admin.data.api.SupabaseManager
import com.prime.doraemon.admin.data.model.BotSettings
import com.prime.doraemon.admin.ui.components.GlassCard
import com.prime.doraemon.admin.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var supabaseUrl by remember { mutableStateOf(PrimeAdminApp.instance.getSupabaseUrl()) }
    var supabaseAnonKey by remember { mutableStateOf(PrimeAdminApp.instance.getSupabaseAnonKey()) }

    var botName by remember { mutableStateOf("PRIME DORAEMON BOT") }
    var welcomeMessage by remember { mutableStateOf("Please join all required channels to continue.") }
    var keyPrompt by remember { mutableStateOf("Paste Your Key") }

    var isTestingConnection by remember { mutableStateOf(false) }
    var isSavingBotSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (PrimeAdminApp.instance.isConfigured()) {
            val res = SupabaseManager.fetchBotSettings()
            if (res.isSuccess) {
                val settings = res.getOrNull()
                if (settings != null) {
                    botName = settings.botName
                    welcomeMessage = settings.welcomeMessage
                    keyPrompt = settings.keyPrompt
                }
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
                Column {
                    Text(
                        text = "CONFIGURATION & BRANDING",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Supabase API Setup Card
            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudQueue, contentDescription = null, tint = DoraemonBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SUPABASE CONNECTION",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = supabaseUrl,
                        onValueChange = { supabaseUrl = it },
                        label = { Text("Supabase URL") },
                        placeholder = { Text("https://your-project.supabase.co") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DoraemonBlue,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = supabaseAnonKey,
                        onValueChange = { supabaseAnonKey = it },
                        label = { Text("Supabase Anon (Public) Key") },
                        placeholder = { Text("eyJh...") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DoraemonBlue,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                PrimeAdminApp.instance.setSupabaseUrl(supabaseUrl)
                                PrimeAdminApp.instance.setSupabaseAnonKey(supabaseAnonKey)
                                Toast.makeText(context, "Credentials saved securely in app storage!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DoraemonBlue)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Keys")
                        }

                        OutlinedButton(
                            onClick = {
                                PrimeAdminApp.instance.setSupabaseUrl(supabaseUrl)
                                PrimeAdminApp.instance.setSupabaseAnonKey(supabaseAnonKey)
                                scope.launch {
                                    isTestingConnection = true
                                    val res = SupabaseManager.fetchDashboardStats()
                                    isTestingConnection = false
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Connection Successful!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Error: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                            enabled = !isTestingConnection
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonCyan)
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test Connection")
                            }
                        }
                    }
                }
            }

            // Bot Dynamic Settings Card
            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SmartToy, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BOT BRANDING & MESSAGES",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = botName,
                        onValueChange = { botName = it },
                        label = { Text("Bot Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DoraemonBlue,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = welcomeMessage,
                        onValueChange = { welcomeMessage = it },
                        label = { Text("Welcome Prompt") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DoraemonBlue,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = keyPrompt,
                        onValueChange = { keyPrompt = it },
                        label = { Text("Key Input Prompt") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DoraemonBlue,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                isSavingBotSettings = true
                                val newSettings = BotSettings(
                                    id = "default",
                                    botName = botName.trim(),
                                    welcomeMessage = welcomeMessage.trim(),
                                    keyPrompt = keyPrompt.trim()
                                )
                                val res = SupabaseManager.updateBotSettings(newSettings)
                                isSavingBotSettings = false
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Bot Settings synchronized!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Update failed: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DoraemonBlue),
                        enabled = !isSavingBotSettings
                    ) {
                        if (isSavingBotSettings) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TextPrimary)
                        } else {
                            Text("Update Bot Settings")
                        }
                    }
                }
            }

            // Security Note
            item {
                GlassCard(
                    backgroundColor = DarkSurfaceElevated,
                    borderColor = CardBorder
                ) {
                    Text(
                        text = "SECURITY ARCHITECTURE",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "This mobile app only uses the public Supabase Anon key and adheres to Supabase Row Level Security (RLS). The sensitive Supabase Service Role Key is never bundled inside this APK, ensuring maximum security.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}
