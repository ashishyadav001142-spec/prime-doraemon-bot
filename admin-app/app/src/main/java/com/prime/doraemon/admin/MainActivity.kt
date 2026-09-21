package com.prime.doraemon.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.prime.doraemon.admin.ui.screens.*
import com.prime.doraemon.admin.ui.theme.*

enum class AppTab(val title: String, val icon: ImageVector) {
    Dashboard("Dashboard", Icons.Default.Dashboard),
    Channels("Channels", Icons.Default.Campaign),
    CreateKey("Create Key", Icons.Default.AddCircle),
    Library("Library", Icons.Default.Folder),
    Settings("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrimeDoraemonAdminTheme {
                MainAppContainer()
            }
        }
    }
}

@Composable
fun MainAppContainer() {
    var currentTab by remember { mutableStateOf(AppTab.Dashboard) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp
            ) {
                AppTab.values().forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (selected) NeonCyan else TextMuted
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                color = if (selected) NeonCyan else TextMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = DoraemonBlue.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            color = DarkBackground
        ) {
            when (currentTab) {
                AppTab.Dashboard -> DashboardScreen(
                    onNavigateToChannels = { currentTab = AppTab.Channels },
                    onNavigateToCreateKey = { currentTab = AppTab.CreateKey },
                    onNavigateToSettings = { currentTab = AppTab.Settings }
                )
                AppTab.Channels -> ChannelsScreen()
                AppTab.CreateKey -> CreateKeyScreen()
                AppTab.Library -> ContentLibraryScreen()
                AppTab.Settings -> SettingsScreen()
            }
        }
    }
}
