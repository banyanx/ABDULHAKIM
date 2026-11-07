package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.loc
import com.example.ui.components.ProminentAccessibilityDisclosureDialog
import com.example.ui.theme.*

@Composable
fun HelpScreen() {
    val context = LocalContext.current
    var showDisclosureDialog by remember { mutableStateOf(false) }

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

        Text(
            text = loc(en = "Help & Permission Guide", ar = "الدليل وإرشادات الصلاحيات"),
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = GoldAccent
        )

        // 1. Accessibility Service Card
        PermissionGuideCard(
            title = loc(en = "1. Accessibility Service", ar = "١. خدمة إمكانية الوصول"),
            description = loc(
                en = "Enables automatic on-screen reading of delivery cards and instant burst acceptance without driver distraction.",
                ar = "تتيح قراءة نصوص وتفاصيل الطلب وقبوله بسرعة فائقة دون تشتيت السائق أثناء القيادة."
            ),
            icon = Icons.Default.AccessibilityNew,
            buttonText = loc(en = "View Disclosure & Setup", ar = "عرض الإفصاح والتفعيل"),
            onClick = { showDisclosureDialog = true }
        )

        // 2. Floating Window Overlay
        PermissionGuideCard(
            title = loc(en = "2. Display Over Other Apps", ar = "٢. الظهور فوق التطبيقات"),
            description = loc(
                en = "Allows the circular floating widget to stay on top of Locate Go and navigation maps for quick toggling.",
                ar = "تسمح بإظهار الزر العائم فوق تطبيقات التوصيل والخرائط للتحكم السريع بالسحب واللمس."
            ),
            icon = Icons.Default.Layers,
            buttonText = loc(en = "Open Overlay Settings", ar = "فتح إعدادات النافذة"),
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            }
        )

        // 3. Battery Optimization Exemption
        PermissionGuideCard(
            title = loc(en = "3. Disable Battery Restrictions", ar = "٣. استثناء من توفير البطارية"),
            description = loc(
                en = "Prevents Android from putting Locate Assistant to sleep during lengthy driving sessions.",
                ar = "يمنع نظام أندرويد من إيقاف المساعد في الخلفية أثناء نوبات التوصيل الطويلة."
            ),
            icon = Icons.Default.BatteryChargingFull,
            buttonText = loc(en = "Battery Settings", ar = "إعدادات البطارية"),
            onClick = {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to app settings
                    val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    context.startActivity(fallback)
                }
            }
        )

        // 4. Privacy & Zero-Data Guarantee
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LuxurySurface),
            shape = RoundedCornerShape(14.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(GreenAccept.copy(alpha = 0.5f), LuxuryCardBorder)))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = GreenAccept)
                    Text(
                        text = loc(en = "100% On-Device Privacy Guarantee", ar = "ضمان الخصوصية المحلية ١٠٠٪"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Text(
                    text = loc(
                        en = "Locate Assistant processes all accessibility events strictly on-device in under 3 milliseconds. No screenshots, no driver personal information, and no order data is ever saved or transmitted to external third-party servers.",
                        ar = "يقوم مساعد لوكيت بمعالجة كافة أحداث إمكانية الوصول محلياً على جهازك في أقل من 3 مللي ثانية. لا يتم تسجيل أي لقطات شاشة أو إرسال أي بيانات شخصية إلى خوادم خارجية."
                    ),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 17.sp
                )
            }
        }

        // 5. Frequently Asked Questions
        Text(
            text = loc(en = "Frequently Asked Questions", ar = "الأسئلة الشائعة"),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = GoldAccent
        )

        FaqItem(
            question = loc(en = "Why is Burst Hardware Tap better?", ar = "لماذا يعتبر النقر الفيزيائي الثلاثي أفضل؟"),
            answer = loc(
                en = "Many delivery apps use animations (fade-in or slide-in) where normal accessibility clicks get dropped. Burst Tap sends 3 micro-touches at 0ms, +35ms, and +75ms to guarantee immediate order capture.",
                ar = "تستخدم العديد من تطبيقات التوصيل رسوماً متحركة عند ظهور الطلب تؤدي لتجاهل النقر العادي. يرسل النقر الثلاثي لمسات متتابعة تضمن قبول الطلب فور ظهوره دون أي تأخير."
            )
        )

        FaqItem(
            question = loc(en = "Does the app support Arabic numbers?", ar = "هل يدعم التطبيق الأرقام المشرقية (٠-٩)؟"),
            answer = loc(
                en = "Yes! Locate Assistant includes native normalization for Eastern Arabic numerals, Arabic decimal commas (٫)، and fractions like (نصف / ربع).",
                ar = "نعم! يحتوي التطبيق على محرك تطبيع فوري يدعم الأرقام المشرقية، الفواصل العشرية، والكسور العربية مثل (نصف، ربع)."
            )
        )

        // Support Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LuxurySurface),
            shape = RoundedCornerShape(14.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(GoldAccent.copy(alpha = 0.5f), LuxuryCardBorder)))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(24.dp))
                    Text(
                        text = loc(en = "Technical Support & Activation", ar = "الدعم الفني وتفعيل الاشتراكات"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Text(
                    text = loc(
                        en = "For subscriptions, device unbinding, or technical help, contact our official support hotline.",
                        ar = "للاشتراكات، فك ربط الأجهزة، أو المساعدة الفنية، تواصل مباشرة مع الدعم الفني المعتمد."
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
                        colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF25D366), contentColor = androidx.compose.ui.graphics.Color.White),
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

                // Copy Phone Number row
                Surface(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
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
private fun PermissionGuideCard(
    title: String,
    description: String,
    icon: ImageVector,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LuxurySurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(LuxuryCardBorder, LuxurySurfaceVariant)))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(22.dp))
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Text(text = description, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = LuxurySurfaceVariant, contentColor = GoldAccent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = buttonText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = LuxurySurface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(LuxuryCardBorder, LuxurySurfaceVariant)))
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
                Text(text = question, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = GoldAccent
                )
            }
            AnimatedVisibility(visible = isExpanded) {
                Text(text = answer, fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
