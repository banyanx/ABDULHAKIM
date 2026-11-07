package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.loc
import com.example.data.local.AppDatabase
import com.example.data.local.OrderHistoryEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.getDatabase(context).orderHistoryDao() }
    val orders by dao.getAllOrders().collectAsState(initial = emptyList())

    var selectedFilter by remember { mutableStateOf("ALL") }
    var showClearDialog by remember { mutableStateOf(false) }

    val filteredOrders = remember(orders, selectedFilter) {
        when (selectedFilter) {
            "ACCEPT" -> orders.filter { it.decision == "ACCEPT" }
            "REJECT" -> orders.filter { it.decision == "REJECT" }
            "MANUAL" -> orders.filter { it.decision == "MANUAL" }
            else -> orders
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = LuxurySurface,
            title = {
                Text(
                    text = loc(en = "Clear History?", ar = "مسح سجل الطلبات؟"),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = loc(
                        en = "Are you sure you want to delete all analyzed order logs?",
                        ar = "هل أنت متأكد من رغبتك في حذف جميع سجلات الطلبات المحفوظة؟"
                    ),
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { dao.clearHistory() }
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedReject, contentColor = TextPrimary)
                ) {
                    Text(text = loc(en = "Clear All", ar = "حذف الكل"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(text = loc(en = "Cancel", ar = "إلغاء"), color = TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxuryBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = loc(en = "Order Decision Log", ar = "سجل قرارات الطلبات"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GoldAccent
                )
                Text(
                    text = loc(
                        en = "${filteredOrders.size} orders analyzed on-device",
                        ar = "${filteredOrders.size} طلب تم فحصها محلياً"
                    ),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            if (orders.isNotEmpty()) {
                IconButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear History",
                        tint = TextMuted
                    )
                }
            }
        }

        // Filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterTabChip(
                label = loc(en = "All", ar = "الكل"),
                count = orders.size,
                isSelected = selectedFilter == "ALL",
                color = GoldAccent,
                onClick = { selectedFilter = "ALL" }
            )
            FilterTabChip(
                label = loc(en = "Accepted", ar = "مقبول"),
                count = orders.count { it.decision == "ACCEPT" },
                isSelected = selectedFilter == "ACCEPT",
                color = GreenAccept,
                onClick = { selectedFilter = "ACCEPT" }
            )
            FilterTabChip(
                label = loc(en = "Rejected", ar = "مرفوض"),
                count = orders.count { it.decision == "REJECT" },
                isSelected = selectedFilter == "REJECT",
                color = RedReject,
                onClick = { selectedFilter = "REJECT" }
            )
            FilterTabChip(
                label = loc(en = "Manual", ar = "يدوي"),
                count = orders.count { it.decision == "MANUAL" },
                isSelected = selectedFilter == "MANUAL",
                color = GoldAccent,
                onClick = { selectedFilter = "MANUAL" }
            )
        }

        // Orders list or empty state
        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HistoryToggleOff,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(54.dp)
                    )
                    Text(
                        text = loc(en = "No orders logged yet", ar = "لا توجد طلبات مسجلة بعد"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = loc(
                            en = "Orders detected on Locate Go or tested in Sandbox will appear here with instant decision metrics.",
                            ar = "الطلبات التي يتم التقاطها في تطبيقات التوصيل أو محاكي الاختبار ستظهر هنا مع سرعة الاستجابة."
                        ),
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 17.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredOrders, key = { it.id }) { order ->
                    OrderHistoryCard(order = order)
                }
            }
        }
    }
}

@Composable
private fun FilterTabChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) color.copy(alpha = 0.2f) else LuxurySurface,
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(color, color))) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) color else TextSecondary
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) color else LuxurySurfaceVariant
            ) {
                Text(
                    text = "$count",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) LuxuryBackground else TextSecondary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun OrderHistoryCard(order: OrderHistoryEntity) {
    val decisionColor = when (order.decision) {
        "ACCEPT" -> GreenAccept
        "REJECT" -> RedReject
        else -> GoldAccent
    }

    val decisionTitle = when (order.decision) {
        "ACCEPT" -> loc(en = "ACCEPTED", ar = "تم القبول")
        "REJECT" -> loc(en = "REJECTED", ar = "تم الرفض")
        else -> loc(en = "MANUAL REVIEW", ar = "مراجعة يدوية")
    }

    val timeFormatted = remember(order.timestamp) {
        SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(order.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LuxurySurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(decisionColor.copy(alpha = 0.4f), LuxuryCardBorder)))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = decisionColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = decisionTitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = decisionColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = LuxurySurfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "⚡ ${order.processingTimeMs} ms",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanTurbo,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = timeFormatted,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            // Key metrics grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = loc(en = "Restaurant", ar = "المتجر"), fontSize = 10.sp, color = TextSecondary)
                    Text(text = "${order.restaurantDistanceKm} km", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Column {
                    Text(text = loc(en = "Customer", ar = "العميل"), fontSize = 10.sp, color = TextSecondary)
                    Text(text = "${order.customerDistanceKm} km", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Column {
                    Text(text = loc(en = "Total", ar = "الإجمالي"), fontSize = 10.sp, color = TextSecondary)
                    Text(text = "${order.totalDistanceKm} km", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                }
                Column {
                    Text(text = loc(en = "Items", ar = "الأصناف"), fontSize = 10.sp, color = TextSecondary)
                    Text(text = "${order.itemCount}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                order.deliveryFeeSar?.let { fee ->
                    Column {
                        Text(text = loc(en = "Fee", ar = "الأجرة"), fontSize = 10.sp, color = TextSecondary)
                        Text(text = "$fee SAR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenAccept)
                    }
                }
            }

            // Reason
            Text(
                text = loc(en = order.reasonEn, ar = order.reasonAr),
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}
