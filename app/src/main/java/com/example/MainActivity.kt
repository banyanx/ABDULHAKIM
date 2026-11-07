package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.LanguageManager
import com.example.core.ProvideLocalizedLayout
import com.example.core.ServiceStateHolder
import com.example.core.loc
import com.example.ui.screens.*
import com.example.ui.theme.*

enum class NavigationScreen {
    DASHBOARD,
    SETTINGS,
    HISTORY,
    LICENSE,
    HELP
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ServiceStateHolder.init(applicationContext)

        setContent {
            LocateAssistantTheme {
                ProvideLocalizedLayout {
                    MainAppScaffold()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold() {
    var currentScreen by remember { mutableStateOf(NavigationScreen.DASHBOARD) }
    val rules by ServiceStateHolder.rulesFlow.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = LuxuryBackground,
        contentColor = TextPrimary,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_super_clicker_logo),
                            contentDescription = "Super Clicker Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Column {
                            Text(
                                text = loc(en = "Locate Assistant", ar = "سوبر كليكر"),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldAccent
                            )
                            Text(
                                text = loc(en = "Super Clicker • 0ms", ar = "Locate Assistant • 0ms"),
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                actions = {
                    // Monitoring Active Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (rules.isMonitoringActive) GreenAccept.copy(alpha = 0.15f) else LuxurySurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = if (rules.isMonitoringActive) "⚡ " + loc(en = "ACTIVE", ar = "نشط") else "💤 " + loc(en = "PAUSED", ar = "متوقف"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (rules.isMonitoringActive) GreenAccept else TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Bilingual Toggle Button (AR / EN)
                    IconButton(
                        onClick = { LanguageManager.toggleLanguage() },
                        modifier = Modifier.testTag("language_toggle_button")
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LuxurySurfaceVariant
                        ) {
                            Text(
                                text = if (LanguageManager.isArabic) "EN" else "عربي",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LuxuryBackground,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = LuxurySurface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationItem(
                    selected = currentScreen == NavigationScreen.DASHBOARD,
                    onClick = { currentScreen = NavigationScreen.DASHBOARD },
                    icon = Icons.Default.Dashboard,
                    unselectedIcon = Icons.Outlined.Dashboard,
                    label = loc(en = "Home", ar = "الرئيسية"),
                    tag = "nav_dashboard"
                )
                NavigationItem(
                    selected = currentScreen == NavigationScreen.SETTINGS,
                    onClick = { currentScreen = NavigationScreen.SETTINGS },
                    icon = Icons.Default.Tune,
                    unselectedIcon = Icons.Outlined.Tune,
                    label = loc(en = "Rules", ar = "الشروط"),
                    tag = "nav_settings"
                )
                NavigationItem(
                    selected = currentScreen == NavigationScreen.HISTORY,
                    onClick = { currentScreen = NavigationScreen.HISTORY },
                    icon = Icons.Default.History,
                    unselectedIcon = Icons.Outlined.History,
                    label = loc(en = "History", ar = "السجل"),
                    tag = "nav_history"
                )
                NavigationItem(
                    selected = currentScreen == NavigationScreen.LICENSE,
                    onClick = { currentScreen = NavigationScreen.LICENSE },
                    icon = Icons.Default.Key,
                    unselectedIcon = Icons.Outlined.Key,
                    label = loc(en = "License", ar = "الترخيص"),
                    tag = "nav_license"
                )
                NavigationItem(
                    selected = currentScreen == NavigationScreen.HELP,
                    onClick = { currentScreen = NavigationScreen.HELP },
                    icon = Icons.Default.HelpOutline,
                    unselectedIcon = Icons.Outlined.HelpOutline,
                    label = loc(en = "Help", ar = "المساعدة"),
                    tag = "nav_help"
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                when (screen) {
                    NavigationScreen.DASHBOARD -> DashboardScreen(
                        onNavigateToLicense = { currentScreen = NavigationScreen.LICENSE },
                        onNavigateToSettings = { currentScreen = NavigationScreen.SETTINGS }
                    )
                    NavigationScreen.SETTINGS -> SettingsScreen()
                    NavigationScreen.HISTORY -> HistoryScreen()
                    NavigationScreen.LICENSE -> LicenseScreen()
                    NavigationScreen.HELP -> HelpScreen()
                }
            }
        }
    }
}

@Composable
private fun RowScope.NavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    unselectedIcon: ImageVector,
    label: String,
    tag: String
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.testTag(tag),
        icon = {
            Icon(
                imageVector = if (selected) icon else unselectedIcon,
                contentDescription = label,
                tint = if (selected) GoldAccent else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        },
        label = {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) GoldAccent else TextSecondary
            )
        },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = GoldAccent.copy(alpha = 0.15f),
            selectedIconColor = GoldAccent,
            unselectedIconColor = TextSecondary,
            selectedTextColor = GoldAccent,
            unselectedTextColor = TextSecondary
        )
    )
}
