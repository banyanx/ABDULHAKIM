package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.LicenseManager
import com.example.core.LicenseResult
import com.example.core.ServiceStateHolder
import com.example.core.loc
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LicenseScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val license by ServiceStateHolder.licenseFlow.collectAsState()

    var inputCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val deviceId = remember { LicenseManager.getHardwareDeviceId(context) }
    val remainingDays = remember(license) { LicenseManager.getRemainingDays() }
    val isLicenseValid = remember(license) { LicenseManager.isLicenseActive() }

    val formattedExpiry = remember(license.expiresAt) {
        SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date(license.expiresAt))
    }

    fun executeActivation(codeToActivate: String) {
        if (codeToActivate.isBlank()) return
        isLoading = true
        feedbackMessage = null
        scope.launch {
            val result = LicenseManager.activateCode(context, codeToActivate)
            isLoading = false
            when (result) {
                is LicenseResult.Success -> {
                    feedbackMessage = Pair(true, loc(en = result.messageEn, ar = result.messageAr))
                    Toast.makeText(context, loc(en = result.messageEn, ar = result.messageAr), Toast.LENGTH_SHORT).show()
                }
                is LicenseResult.Error -> {
                    feedbackMessage = Pair(false, loc(en = result.messageEn, ar = result.messageAr))
                }
            }
        }
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
            text = loc(en = "License & Device Binding", ar = "الترخيص وربط الجهاز"),
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = GoldAccent
        )

        // 1. Current Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LuxurySurface),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(
                    listOf(if (isLicenseValid) GreenAccept else RedReject, LuxuryCardBorder)
                )
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isLicenseValid) {
                                loc(en = "ACTIVE SUBSCRIPTION", ar = "اشتراك نشط")
                            } else {
                                loc(en = "EXPIRED SUBSCRIPTION", ar = "اشتراك منتهي")
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isLicenseValid) GreenAccept else RedReject
                        )
                        Text(
                            text = license.code,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Surface(
                        color = (if (isLicenseValid) GreenAccept else RedReject).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isLicenseValid) "$remainingDays " + loc(en = "days left", ar = "يوم متبقي") else loc(en = "Renew", ar = "تجديد"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLicenseValid) GreenAccept else RedReject,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                HorizontalDivider(color = LuxuryCardBorder.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = loc(en = "Driver Name", ar = "اسم السائق"), fontSize = 11.sp, color = TextSecondary)
                        Text(text = license.userName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = loc(en = "Expires On", ar = "تاريخ الانتهاء"), fontSize = 11.sp, color = TextSecondary)
                        Text(text = formattedExpiry, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    }
                }
            }
        }

        // 2. Hardware Device ID Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LuxurySurface),
            shape = RoundedCornerShape(14.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(LuxuryCardBorder, LuxurySurfaceVariant)))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = loc(en = "Hardware Device UUID", ar = "معرف الجهاز الفيزيائي (UUID)"),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = deviceId,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanTurbo
                    )
                }

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Device ID", deviceId))
                        Toast.makeText(context, loc(en = "Device ID copied!", ar = "تم نسخ معرف الجهاز!"), Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy ID",
                        tint = GoldAccent
                    )
                }
            }
        }

        // 3. Activation Code Input
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LuxurySurface),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(LuxuryCardBorder, LuxurySurfaceVariant)))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = loc(en = "Enter Activation Code", ar = "إدخال كود التفعيل"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                OutlinedTextField(
                    value = inputCode,
                    onValueChange = { inputCode = it.uppercase() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("activation_code_input"),
                    placeholder = {
                        Text(text = loc(en = "e.g. VIP-TURBO or DEMO2026", ar = "مثال: DEMO2026 أو VIP-TURBO"), color = TextMuted)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { executeActivation(inputCode) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = LuxuryCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = GoldAccent
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Feedback Banner
                feedbackMessage?.let { (isSuccess, msg) ->
                    Surface(
                        color = (if (isSuccess) GreenAccept else RedReject).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            color = if (isSuccess) GreenAccept else RedReject,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Button(
                    onClick = { executeActivation(inputCode) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("activate_code_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = LuxuryBackground),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading && inputCode.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LuxuryBackground, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = loc(en = "Verify & Activate", ar = "التحقق والتفعيل الآن"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. Quick Demo Codes
        Text(
            text = loc(en = "Quick Test Activation Codes:", ar = "أكواد تجريبية سريعة:"),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DemoCodeChip(
                modifier = Modifier.weight(1f),
                code = "DEMO2026",
                label = loc(en = "7-Day Trial", ar = "تجربة 7 أيام"),
                onClick = {
                    inputCode = "DEMO2026"
                    executeActivation("DEMO2026")
                }
            )
            DemoCodeChip(
                modifier = Modifier.weight(1f),
                code = "VIP-TURBO",
                label = loc(en = "30-Day VIP", ar = "اشتراك شهر"),
                onClick = {
                    inputCode = "VIP-TURBO"
                    executeActivation("VIP-TURBO")
                }
            )
            DemoCodeChip(
                modifier = Modifier.weight(1f),
                code = "SAUDI-FAST",
                label = loc(en = "14-Day Pass", ar = "14 يوم سريع"),
                onClick = {
                    inputCode = "SAUDI-FAST"
                    executeActivation("SAUDI-FAST")
                }
            )
        }

        // 5. Admin Contact Shortcuts
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LuxurySurface),
            shape = RoundedCornerShape(14.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(LuxuryCardBorder, LuxurySurfaceVariant)))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = loc(en = "Get Your Official License Code", ar = "الحصول على كود اشتراك رسمي"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = loc(
                        en = "Contact our official support to purchase instant access or renew your driver subscription.",
                        ar = "تواصل مع الإدارة عبر الواتساب لتفعيل حسابك وتجديد اشتراكك فوراً."
                    ),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // WhatsApp Chat button
                    Button(
                        onClick = {
                            val msg = "السلام عليكم، أرغب بالحصول على رمز تفعيل لتطبيق سوبر كليكر"
                            val url = "https://wa.me/966545135644?text=${Uri.encode(msg)}"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366), contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = loc(en = "WhatsApp Chat", ar = "محادثة واتساب"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Direct Call button
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:0545135644"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = LuxuryBackground)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = loc(en = "Direct Call", ar = "اتصال مباشر"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Copy Admin Number row
                Surface(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Admin Phone", "0545135644"))
                        Toast.makeText(context, loc(en = "Admin phone copied: 0545135644", ar = "تم نسخ رقم الإدارة: 0545135644"), Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = LuxurySurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                            Text(text = loc(en = "Admin Phone: 0545135644", ar = "رقم الإدارة: 0545135644"), fontSize = 12.sp, color = TextPrimary)
                        }
                        Text(text = loc(en = "Tap to Copy", ar = "اضغط للنسخ"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoCodeChip(
    modifier: Modifier = Modifier,
    code: String,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = LuxurySurfaceVariant,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = code, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
            Text(text = label, fontSize = 10.sp, color = TextSecondary)
        }
    }
}
