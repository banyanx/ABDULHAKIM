package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ServiceStateHolder
import com.example.core.loc
import com.example.data.model.DriverRules
import com.example.ui.theme.*

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val currentRules by ServiceStateHolder.rulesFlow.collectAsState()

    var restDistanceText by remember(currentRules) {
        mutableStateOf(String.format(java.util.Locale.US, "%.1f", currentRules.maxRestaurantDistanceKm))
    }
    var custDistanceText by remember(currentRules) {
        mutableStateOf(String.format(java.util.Locale.US, "%.1f", currentRules.maxCustomerDistanceKm))
    }
    var quantityText by remember(currentRules) {
        mutableStateOf(currentRules.maxQuantity.toString())
    }

    var autoAccept by remember(currentRules) { mutableStateOf(currentRules.autoAcceptEnabled) }
    var autoReject by remember(currentRules) { mutableStateOf(currentRules.autoRejectEnabled) }
    var burstTap by remember(currentRules) { mutableStateOf(currentRules.burstTapEnabled) }
    var sound by remember(currentRules) { mutableStateOf(currentRules.soundNotification) }
    var vibrate by remember(currentRules) { mutableStateOf(currentRules.vibrateOnDecision) }

    fun commitRules() {
        val restDist = restDistanceText.toDoubleOrNull() ?: currentRules.maxRestaurantDistanceKm
        val custDist = custDistanceText.toDoubleOrNull() ?: currentRules.maxCustomerDistanceKm
        val qty = quantityText.toIntOrNull() ?: currentRules.maxQuantity

        val updated = DriverRules(
            maxRestaurantDistanceKm = Math.round(restDist * 10.0) / 10.0,
            maxCustomerDistanceKm = Math.round(custDist * 10.0) / 10.0,
            maxQuantity = if (qty > 0) qty else 1,
            autoAcceptEnabled = autoAccept,
            autoRejectEnabled = autoReject,
            burstTapEnabled = burstTap,
            soundNotification = sound,
            vibrateOnDecision = vibrate,
            isMonitoringActive = currentRules.isMonitoringActive
        )
        ServiceStateHolder.updateRules(context, updated)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxuryBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = loc(en = "Order Criteria & Thresholds", ar = "شروط ومعايير الطلبات"),
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = GoldAccent
        )

        // 1. Precise Numeric Input Card (NO SLIDERS)
        SettingsSectionCard(
            title = loc(en = "Distance & Quantity Thresholds", ar = "حدود المسافات والكميات (إدخال يدوي دقيق)"),
            icon = Icons.Default.Straighten
        ) {
            // Field 1: Restaurant Distance
            NumericInputField(
                label = loc(
                    en = "Max Restaurant Distance (km)",
                    ar = "الحد الأقصى لمسافة المطعم (كم)"
                ),
                valueText = restDistanceText,
                onValueChange = {
                    restDistanceText = it
                    commitRules()
                },
                onIncrement = {
                    val current = restDistanceText.toDoubleOrNull() ?: 3.5
                    val next = Math.round((current + 0.5) * 10.0) / 10.0
                    restDistanceText = String.format(java.util.Locale.US, "%.1f", next)
                    commitRules()
                },
                onDecrement = {
                    val current = restDistanceText.toDoubleOrNull() ?: 3.5
                    val next = Math.max(0.5, Math.round((current - 0.5) * 10.0) / 10.0)
                    restDistanceText = String.format(java.util.Locale.US, "%.1f", next)
                    commitRules()
                },
                quickPresets = listOf("1.5", "2.0", "3.0", "3.5", "5.0"),
                unit = "km",
                isDecimal = true,
                testTag = "input_restaurant_distance"
            )

            HorizontalDivider(color = LuxuryCardBorder.copy(alpha = 0.4f))

            // Field 2: Customer Distance
            NumericInputField(
                label = loc(
                    en = "Max Customer Distance (km)",
                    ar = "الحد الأقصى لمسافة العميل (كم)"
                ),
                valueText = custDistanceText,
                onValueChange = {
                    custDistanceText = it
                    commitRules()
                },
                onIncrement = {
                    val current = custDistanceText.toDoubleOrNull() ?: 5.0
                    val next = Math.round((current + 0.5) * 10.0) / 10.0
                    custDistanceText = String.format(java.util.Locale.US, "%.1f", next)
                    commitRules()
                },
                onDecrement = {
                    val current = custDistanceText.toDoubleOrNull() ?: 5.0
                    val next = Math.max(1.0, Math.round((current - 0.5) * 10.0) / 10.0)
                    custDistanceText = String.format(java.util.Locale.US, "%.1f", next)
                    commitRules()
                },
                quickPresets = listOf("3.0", "4.0", "5.0", "7.0", "10.0"),
                unit = "km",
                isDecimal = true,
                testTag = "input_customer_distance"
            )

            HorizontalDivider(color = LuxuryCardBorder.copy(alpha = 0.4f))

            // Field 3: Max Quantity
            NumericInputField(
                label = loc(
                    en = "Max Item Quantity",
                    ar = "الحد الأقصى للكمية (عدد الأصناف)"
                ),
                valueText = quantityText,
                onValueChange = {
                    quantityText = it
                    commitRules()
                },
                onIncrement = {
                    val current = quantityText.toIntOrNull() ?: 2
                    quantityText = (current + 1).toString()
                    commitRules()
                },
                onDecrement = {
                    val current = quantityText.toIntOrNull() ?: 2
                    quantityText = Math.max(1, current - 1).toString()
                    commitRules()
                },
                quickPresets = listOf("2", "4", "6", "8", "12"),
                unit = loc(en = "items", ar = "عناصر"),
                isDecimal = false,
                testTag = "input_max_quantity"
            )
        }

        // 2. Execution Engine Card
        SettingsSectionCard(
            title = loc(en = "Turbo Execution Engine", ar = "محرك النقر الفائق والإجراء التلقائي"),
            icon = Icons.Default.Bolt
        ) {
            ToggleSetting(
                title = loc(en = "Instant Auto-Accept", ar = "قبول تلقائي فوري"),
                subtitle = loc(
                    en = "Automatically accept orders matching your distance criteria without touch",
                    ar = "قبول الطلب آلياً عند مطابقة كافة الشروط دون الحاجة للمس الشاشة"
                ),
                checked = autoAccept,
                accentColor = GreenAccept,
                onCheckedChange = {
                    autoAccept = it
                    commitRules()
                }
            )

            ToggleSetting(
                title = loc(en = "Instant Auto-Reject", ar = "تخطي تلقائي للطلبات المخالفة"),
                subtitle = loc(
                    en = "Dismiss bad orders immediately to allow new delivery orders to appear",
                    ar = "تخطي الطلبات البعيدة فوراً لإتاحة المجال لظهور طلبات جديدة"
                ),
                checked = autoReject,
                accentColor = RedReject,
                onCheckedChange = {
                    autoReject = it
                    commitRules()
                }
            )

            ToggleSetting(
                title = loc(en = "Hardware Burst Tap (Dual Execution)", ar = "النقر الفيزيائي المتسارع (3x)"),
                subtitle = loc(
                    en = "Dispatches 3 physical micro-touches at 0ms, +35ms, and +75ms to penetrate animations",
                    ar = "إرسال 3 لمسات متتالية في 0 و 35 و 75 مللي ثانية لتجاوز تأخير الرسوم المتحركة"
                ),
                checked = burstTap,
                accentColor = CyanTurbo,
                onCheckedChange = {
                    burstTap = it
                    commitRules()
                }
            )
        }

        // 3. Alerts & Driver Feedback
        SettingsSectionCard(
            title = loc(en = "Driver Alerts & Haptics", ar = "التنبيهات الصوتية والاهتزاز"),
            icon = Icons.Default.Notifications
        ) {
            ToggleSetting(
                title = loc(en = "Audible Chime", ar = "صوت التنبيه"),
                subtitle = loc(en = "Play notification tone when order is accepted", ar = "تشغيل نغمة فور التقاط وقبول الطلب"),
                checked = sound,
                accentColor = GoldAccent,
                onCheckedChange = {
                    sound = it
                    commitRules()
                }
            )

            ToggleSetting(
                title = loc(en = "Vibration Feedback", ar = "الاهتزاز اللمسي"),
                subtitle = loc(en = "Vibrate phone when co-pilot executes an action", ar = "اهتزاز فوري عند تنفيذ النقر"),
                checked = vibrate,
                accentColor = GoldAccent,
                onCheckedChange = {
                    vibrate = it
                    commitRules()
                }
            )
        }

        // Reset to Defaults Button
        OutlinedButton(
            onClick = {
                val defaults = DriverRules(
                    maxRestaurantDistanceKm = 3.5,
                    maxCustomerDistanceKm = 5.0,
                    maxQuantity = 2
                )
                restDistanceText = "3.5"
                custDistanceText = "5.0"
                quantityText = "2"
                autoAccept = defaults.autoAcceptEnabled
                autoReject = defaults.autoRejectEnabled
                burstTap = defaults.burstTapEnabled
                sound = defaults.soundNotification
                vibrate = defaults.vibrateOnDecision
                ServiceStateHolder.updateRules(context, defaults)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
        ) {
            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = loc(en = "Reset to Recommended Defaults (3.5km / 5.0km / 2 items)", ar = "استعادة القيم الموصى بها (3.5 كم / 5.0 كم / 2 صنف)"))
        }
    }
}

/**
 * Clean Numeric Text Input Field with Increment/Decrement and Quick Presets
 * Strictly replaces sliders for exact decimal and integer precision.
 */
@Composable
fun NumericInputField(
    label: String,
    valueText: String,
    onValueChange: (String) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    quickPresets: List<String>,
    unit: String,
    isDecimal: Boolean,
    testTag: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Decrement Button (-)
            FilledIconButton(
                onClick = onDecrement,
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = LuxurySurfaceVariant,
                    contentColor = GoldAccent
                )
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrement", modifier = Modifier.size(20.dp))
            }

            // Numeric Input Box
            OutlinedTextField(
                value = valueText,
                onValueChange = { input ->
                    // Filter to only digits and dot
                    val filtered = if (isDecimal) {
                        input.filter { it.isDigit() || it == '.' }
                    } else {
                        input.filter { it.isDigit() }
                    }
                    onValueChange(filtered)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag(testTag),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isDecimal) KeyboardType.Decimal else KeyboardType.Number
                ),
                trailingIcon = {
                    Text(
                        text = unit,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldAccent,
                    unfocusedBorderColor = LuxuryCardBorder,
                    cursorColor = GoldAccent
                )
            )

            // Increment Button (+)
            FilledIconButton(
                onClick = onIncrement,
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = LuxurySurfaceVariant,
                    contentColor = GoldAccent
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increment", modifier = Modifier.size(20.dp))
            }
        }

        // Quick Preset Chips for 1-tap setting
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickPresets.forEach { preset ->
                val isSelected = valueText == preset
                Surface(
                    onClick = { onValueChange(preset) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) GoldAccent else LuxurySurfaceVariant,
                    modifier = Modifier.height(28.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) LuxuryBackground else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LuxurySurface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(LuxuryCardBorder, LuxurySurfaceVariant)))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            HorizontalDivider(color = LuxuryCardBorder.copy(alpha = 0.4f))
            content()
        }
    }
}

@Composable
private fun ToggleSetting(
    title: String,
    subtitle: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LuxuryBackground,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = LuxurySurfaceVariant
            )
        )
    }
}
