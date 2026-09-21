package com.prime.doraemon.admin.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prime.doraemon.admin.PrimeAdminApp
import com.prime.doraemon.admin.data.api.SupabaseManager
import com.prime.doraemon.admin.data.model.ContentItem
import com.prime.doraemon.admin.data.model.DashboardStats
import com.prime.doraemon.admin.ui.components.*
import com.prime.doraemon.admin.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    onNavigateToChannels: () -> Unit,
    onNavigateToCreateKey: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf(DashboardStats()) }
    var recentItems by remember { mutableStateOf<List<ContentItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isConfigured = PrimeAdminApp.instance.isConfigured()

    fun loadData() {
        if (!isConfigured) {
            isLoading = false
            return
        }
        scope.launch {
            isLoading = true
            errorMessage = null
            val statsResult = SupabaseManager.fetchDashboardStats()
            val contentResult = SupabaseManager.fetchContentItems()

            if (statsResult.isSuccess) {
                stats = statsResult.getOrNull() ?: DashboardStats()
            } else {
                errorMessage = statsResult.exceptionOrNull()?.localizedMessage
            }

            if (contentResult.isSuccess) {
                recentItems = contentResult.getOrNull()?.take(5) ?: emptyList()
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
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
            // Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PRIME DORAEMON ADMIN",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Dashboard Overview",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    IconButton(
                        onClick = { loadData() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(DarkSurface)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = DoraemonBlue)
                    }
                }
            }

            // Notice if not configured
            if (!isConfigured) {
                item {
                    GlassCard(
                        borderColor = AccentAmber,
                        backgroundColor = DarkSurfaceElevated
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AccentAmber)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Supabase Not Configured", color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text("Enter your Supabase URL & Anon Key in Settings to connect.", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateToSettings,
                            colors = ButtonDefaults.buttonColors(containerColor = DoraemonBlue)
                        ) {
                            Text("Open Settings")
                        }
                    }
                }
            }

            // Error notice
            if (errorMessage != null) {
                item {
                    GlassCard(borderColor = AccentCoral) {
                        Text("Connection Error: $errorMessage", color = AccentCoral, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Stat Cards Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Total Keys",
                        value = stats.totalKeys.toString(),
                        icon = Icons.Default.VpnKey,
                        accentColor = DoraemonBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Active Keys",
                        value = stats.activeKeys.toString(),
                        icon = Icons.Default.CheckCircle,
                        accentColor = NeonCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Active Channels",
                        value = stats.activeChannels.toString(),
                        icon = Icons.Default.Campaign,
                        accentColor = AccentAmber,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Deliveries",
                        value = stats.totalDeliveries.toString(),
                        icon = Icons.Default.Send,
                        accentColor = AccentCoral,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Actions
            item {
                Text(
                    text = "QUICK ACTIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onNavigateToCreateKey,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DoraemonBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Key", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onNavigateToChannels,
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DoraemonBlue),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DoraemonBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Channels", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Recent Uploads Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT UPLOADS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = NeonCyan, strokeWidth = 2.dp)
                    }
                }
            }

            if (recentItems.isEmpty() && !isLoading) {
                item {
                    GlassCard {
                        Text(
                            text = "No content uploads yet. Click '+ Create Key' to add your first item.",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(recentItems) { item ->
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = item.key,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    TypeBadge(type = item.contentType)
                                    StatusBadge(active = item.active)
                                }
                                if (!item.caption.isNullOrEmpty() || !item.textContent.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.caption ?: item.textContent ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
