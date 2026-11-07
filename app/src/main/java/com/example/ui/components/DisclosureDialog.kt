package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.loc
import com.example.ui.theme.*

@Composable
fun ProminentAccessibilityDisclosureDialog(
    onDismiss: () -> Unit,
    onAcceptAndContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("accessibility_disclosure_dialog"),
        shape = RoundedCornerShape(20.dp),
        containerColor = LuxurySurface,
        titleContentColor = GoldAccent,
        textContentColor = TextPrimary,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = loc(
                        en = "Accessibility Prominent Disclosure",
                        ar = "إفصاح خدمة إمكانية الوصول"
                    ),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = loc(
                        en = "Locate Assistant requires the Android Accessibility Service to act as an automated driving co-pilot for road safety.",
                        ar = "يتطلب تطبيق مساعد لوكيت تفعيل خدمة إمكانية الوصول للعمل كمساعد قيادة آلي للحفاظ على سلامة السائق على الطريق."
                    ),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = TextPrimary
                )

                DisclosurePoint(
                    icon = Icons.Default.Speed,
                    title = loc(en = "1. Safe Driving Purpose", ar = "١. الغرض وسلامة القيادة"),
                    description = loc(
                        en = "Automatically evaluates incoming delivery orders so drivers do not touch their phone or take their eyes off the road while driving.",
                        ar = "تقييم وقبول الطلبات تلقائياً لتفادي تشتت السائق أثناء القيادة دون الحاجة للمس الشاشة."
                    )
                )

                DisclosurePoint(
                    icon = Icons.Default.PrivacyTip,
                    title = loc(en = "2. Data Processed", ar = "٢. البيانات المقروءة"),
                    description = loc(
                        en = "Reads only on-screen delivery parameters (restaurant distance, customer distance, item count, and delivery fee) on target apps.",
                        ar = "قراءة نصوص الطلب المعروضة على الشاشة فقط (مسافة المتجر، مسافة العميل، عدد الأصناف، وقيمة التوصيل)."
                    )
                )

                DisclosurePoint(
                    icon = Icons.Default.Security,
                    title = loc(en = "3. Zero Data Collection (100% On-Device)", ar = "٣. انعدام جمع البيانات (محلي بالكامل)"),
                    description = loc(
                        en = "Processing is strictly 100% on-device in under 3ms. We NEVER collect, transmit, store, or share your personal data or screen content.",
                        ar = "المعالجة تتم محلياً بالكامل على هاتفك في أقل من 3 مللي ثانية. لا نجمع ولا نشارك ولا نخزن أي بيانات خاصة بك."
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAcceptAndContinue,
                modifier = Modifier.testTag("agree_disclosure_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent,
                    contentColor = LuxuryBackground
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = loc(en = "Agree & Enable", ar = "موافق والمتابعة"),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_disclosure_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextSecondary
                )
            ) {
                Text(text = loc(en = "Cancel", ar = "إلغاء"))
            }
        }
    )
}

@Composable
private fun DisclosurePoint(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(LuxurySurfaceVariant)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyanTurbo,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Column {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = GoldAccent
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}
