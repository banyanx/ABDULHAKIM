package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.LicenseManager
import com.example.core.ServiceStateHolder
import com.example.core.loc
import com.example.service.OverlayService
import com.example.ui.components.ProminentAccessibilityDisclosureDialog
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    onNavigateToLicense: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val rules by ServiceStateHolder.rulesFlow.collectAsState()
    val license by ServiceStateHolder.licenseFlow.collectAsState()
    val stats by ServiceStateHolder.sessionStatsFlow.collectAsState()

    var showDisclosureDialog by remember { mutableStateOf(false) }

    // Check service & permission states
    var isOverlayPermissionGranted by remember {
        mutableStateOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true)
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            isOverlayPermissionGranted = Settings.canDrawOverlays(context)
        }
    }

    if (showDisclosureDialog) {
        ProminentAccessibilityDisclosureDialog(
            onDismiss = { showDisclosureDialog = false },
            onAcceptAndContinue = {
                showDisclosureDialog = false
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxuryBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // 1. Hero Status Card (Monitoring Master Switch)
        HeroStatusCard(
            isMonitoring = rules.isMonitoringActive,
            isAccessibilityActive = ServiceStateHolder.isAccessibilityActive,
            isOverlayActive = ServiceStateHolder.isOverlayActive,
            licenseDays = LicenseManager.getRemainingDays(),
            isLicenseValid = LicenseManager.isLicenseActive(),
            onToggleMonitoring = {
                ServiceStateHolder.toggleMonitoring(context)
            },
            onAccessibilityClick = {
                if (!ServiceStateHolder.isAccessibilityActive) {
                    showDisclosureDialog = true
                }
            },
            onOverlayClick = {
                if (!isOverlayPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                } else {
                    if (ServiceStateHolder.isOverlayActive) {
                        OverlayService.stop(context)
                    } else {
                        OverlayService.start(context)
                    }
                }
            },
            onLicenseClick = onNavigateToLicense
        )

        // 2. Real-Time Performance & Metrics Grid
        Text(
            text = loc(en = "Live Session Metrics", ar = "إحصائيات الجلسة المباشرة"),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = GoldAccent
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = loc(en = "Accepted", ar = "المقبولة"),
                value = stats.acceptedCount.toString(),
                icon = Icons.Default.CheckCircle,
                accentColor = GreenAccept,
                subtext = loc(en = "Burst Tapped", ar = "نقرات فائقة")
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = loc(en = "Rejected", ar = "المرفوضة"),
                value = stats.rejectedCount.toString(),
                icon = Icons.Default.Cancel,
                accentColor = RedReject,
                subtext = loc(en = "Filtered Out", ar = "مستبعدة")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = loc(en = "Avg Latency", ar = "زمن الاستجابة"),
                value = "${stats.avgLatencyMs} ms",
                icon = Icons.Default.Speed,
                accentColor = CyanTurbo,
                subtext = loc(en = "Sub-3ms Target", ar = "هدف 0 مللي ثانية")
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = loc(en = "Evaluated", ar = "تم فحصها"),
                value = stats.totalEvaluated.toString(),
                icon = Icons.Default.Analytics,
                accentColor = GoldAccent,
                subtext = loc(en = "All Orders", ar = "إجمالي الطلبات")
            )
        }

        // 3. Floating Overlay Controller Card
        OverlayControlCard(
            isOverlayActive = ServiceStateHolder.isOverlayActive,
            isPermissionGranted = isOverlayPermissionGranted,
            onToggleOverlay = {
                if (!isOverlayPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                } else {
                    if (ServiceStateHolder.isOverlayActive) {
                        OverlayService.stop(context)
                    } else {
                        OverlayService.start(context)
                    }
                }
            }
        )

        // 4. Quick Rule Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LuxurySurface),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(LuxuryCardBorder, LuxurySurfaceVariant)))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = loc(en = "Active Acceptance Rules", ar = "شروط القبول الحالية"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    TextButton(onClick = onNavigateToSettings) {
                        Text(
                            text = loc(en = "Edit Rules", ar = "تعديل"),
                            color = GoldAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RuleChip(label = loc(en = "Max Store", ar = "أقصى متجر"), value = "${rules.maxRestaurantDistanceKm} km")
                    RuleChip(label = loc(en = "Max Customer", ar = "أقصى عميل"), value = "${rules.maxCustomerDistanceKm} km")
                    RuleChip(label = loc(en = "Max Items", ar = "أقصى أصناف"), value = "${rules.maxQuantity}")
                }
            }
        }

        // Clear Session button
        TextButton(
            onClick = { ServiceStateHolder.clearStats() },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = loc(en = "Reset Session Counters", ar = "تصفير عدادات الجلسة"),
                color = TextMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun HeroStatusCard(
    isMonitoring: Boolean,
    isAccessibilityActive: Boolean,
    isOverlayActive: Boolean,
    licenseDays: Long,
    isLicenseValid: Boolean,
    onToggleMonitoring: () -> Unit,
    onAccessibilityClick: () -> Unit,
    onOverlayClick: () -> Unit,
    onLicenseClick: () -> Unit
) {
    val animatedGlowColor by animateColorAsState(
        targetValue = if (isMonitoring) GreenAccept else TextMuted,
        label = "glow"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_status_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LuxurySurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(listOf(animatedGlowColor.copy(alpha = 0.5f), LuxuryCardBorder))
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = loc(en = "Co-Pilot Automation", ar = "المساعد الآلي الذكي"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isMonitoring) {
                            loc(en = "Active • Detecting orders in <3ms", ar = "نشط • التقاط الطلبات في أقل من 3 مللي ثانية")
                        } else {
                            loc(en = "Paused • Tap switch to activate", ar = "متوقف مؤقتاً • اضغط للتفعيل")
                        },
                        fontSize = 12.sp,
                        color = if (isMonitoring) GreenAccept else TextSecondary
                    )
                }

                Switch(
                    checked = isMonitoring,
                    onCheckedChange = { onToggleMonitoring() },
                    modifier = Modifier.testTag("master_monitoring_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = LuxuryBackground,
                        checkedTrackColor = GreenAccept,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = LuxurySurfaceVariant
                    )
                )
            }

            HorizontalDivider(color = LuxuryCardBorder.copy(alpha = 0.5f))

            // Sub-status badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusBadge(
                    label = loc(en = "Accessibility", ar = "إمكانية الوصول"),
                    isActive = isAccessibilityActive,
                    activeText = loc(en = "Active", ar = "مفعل"),
                    inactiveText = loc(en = "Grant", ar = "تفعيل"),
                    onClick = onAccessibilityClick
                )

                StatusBadge(
                    label = loc(en = "Overlay", ar = "النافذة العائمة"),
                    isActive = isOverlayActive,
                    activeText = loc(en = "Visible", ar = "ظاهرة"),
                    inactiveText = loc(en = "Start", ar = "تشغيل"),
                    onClick = onOverlayClick
                )

                StatusBadge(
                    label = loc(en = "License", ar = "الترخيص"),
                    isActive = isLicenseValid,
                    activeText = "$licenseDays d",
                    inactiveText = loc(en = "Renew", ar = "تجديد"),
                    onClick = onLicenseClick
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(
    label: String,
    isActive: Boolean,
    activeText: String,
    inactiveText: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = LuxurySurfaceVariant,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isActive) GreenAccept else GoldAccent)
            )
            Column {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
                Text(
                    text = if (isActive) activeText else inactiveText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) GreenAccept else GoldAccent
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    subtext: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = LuxurySurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(LuxuryCardBorder, LuxurySurfaceVariant)))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = subtext,
                fontSize = 10.sp,
                color = accentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun OverlayControlCard(
    isOverlayActive: Boolean,
    isPermissionGranted: Boolean,
    onToggleOverlay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LuxurySurface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(LuxuryCardBorder, LuxurySurfaceVariant)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isOverlayActive) GreenGlow else LuxurySurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = if (isOverlayActive) GreenAccept else GoldAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = loc(en = "Floating Co-Pilot Widget", ar = "الزر العائم فوق التطبيقات"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (!isPermissionGranted) {
                            loc(en = "Permission required to show over delivery apps", ar = "يتطلب إذن الظهور فوق التطبيقات")
                        } else if (isOverlayActive) {
                            loc(en = "Draggable widget active on screen", ar = "الزر العائم نشط وقابل للتحريك")
                        } else {
                            loc(en = "Displays quick toggle over Locate Go", ar = "يظهر أزرار تحكم سريعة فوق لوكيت")
                        },
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Button(
                onClick = onToggleOverlay,
                modifier = Modifier.testTag("toggle_overlay_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOverlayActive) RedReject else GoldAccent,
                    contentColor = LuxuryBackground
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isOverlayActive) loc(en = "Stop", ar = "إيقاف") else loc(en = "Start", ar = "تشغيل"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun RuleChip(label: String, value: String) {
    Surface(
        color = LuxurySurfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 10.sp, color = TextSecondary)
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
        }
    }
}
