package com.example.core

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.model.ParsedOrder
import java.util.regex.Pattern

/**
 * Ultra-fast native order parsing engine with microsecond Arabic & English text normalization.
 */
object OrderParser {

    private val EASTERN_ARABIC_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    private val WESTERN_DIGITS = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

    // Context validation keywords to confirm order popup
    private val CONTEXT_KEYWORDS = arrayOf(
        "المتجر", "موقعك", "العميل", "التوصيل", "ريال", "SAR", "km", "كم",
        "order", "delivery", "مطعم", "pickup", "dropoff", "عناصر", "items", "عنصر"
    )

    // Action button patterns
    private val ACCEPT_BUTTON_KEYWORDS = arrayOf(
        "قبول", "تأكيد", "وافق", "استلام الطلب", "قبول الطلب", "تأكيد الطلب",
        "accept", "confirm", "take order", "accept order"
    )

    private val REJECT_BUTTON_KEYWORDS = arrayOf(
        "رفض", "تخطي", "تجاهل", "اعتذار", "reject", "skip", "decline", "ignore"
    )

    // Pre-compiled regex patterns for maximum parsing speed
    private val DISTANCE_KM_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:كم|km)", Pattern.CASE_INSENSITIVE)
    private val DISTANCE_METER_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:متر|م\\b|m\\b)", Pattern.CASE_INSENSITIVE)
    private val ITEMS_PATTERN = Pattern.compile("(\\d+)\\s*(?:عناصر|عنصر|أصناف|صنف|items?|item|pcs)", Pattern.CASE_INSENSITIVE)
    private val ITEMS_PREFIX_PATTERN = Pattern.compile("(?:x|عدد)\\s*(\\d+)", Pattern.CASE_INSENSITIVE)
    private val FEE_SAR_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:ريال|ر\\.س|sar|sr)", Pattern.CASE_INSENSITIVE)

    /**
     * Normalizes Eastern Arabic numerals, Arabic decimal separators, and fraction words
     */
    fun normalizeText(input: CharSequence?): String {
        if (input.isNullOrBlank()) return ""
        val text = input.toString()
        val sb = StringBuilder(text.length)

        for (i in text.indices) {
            val c = text[i]
            var converted = false
            for (d in EASTERN_ARABIC_DIGITS.indices) {
                if (c == EASTERN_ARABIC_DIGITS[d]) {
                    sb.append(WESTERN_DIGITS[d])
                    converted = true
                    break
                }
            }
            if (!converted) {
                // Arabic decimal separator '٫' or comma '،' when between digits
                if (c == '٫' || (c == '،' && i > 0 && i < text.length - 1 && text[i - 1].isDigit() && text[i + 1].isDigit())) {
                    sb.append('.')
                } else {
                    sb.append(c)
                }
            }
        }

        var normalized = sb.toString()
            .replace("ثلاثة أرباع", "0.75")
            .replace("نصف", "0.5")
            .replace("ربع", "0.25")
            .trim()

        return normalized
    }

    /**
     * Traverses the AccessibilityNodeInfo tree and attempts to parse a delivery order
     */
    fun parseWindow(rootNode: AccessibilityNodeInfo?, packageName: String = ""): ParsedOrder? {
        if (rootNode == null) return null

        val nodeTexts = ArrayList<Pair<String, Rect>>(32)
        var acceptBounds: Rect? = null
        var rejectBounds: Rect? = null

        // Collect all nodes iteratively to avoid recursion depth issues
        val stack = ArrayDeque<AccessibilityNodeInfo>(32)
        stack.add(rootNode)

        var contextMatches = 0

        while (!stack.isEmpty()) {
            val node = stack.removeLast()
            val text = node.text
            val desc = node.contentDescription
            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            val rawCombined = (text?.toString() ?: "") + " " + (desc?.toString() ?: "")
            if (rawCombined.isNotBlank()) {
                val normalized = normalizeText(rawCombined)
                nodeTexts.add(Pair(normalized, bounds))

                val lower = normalized.lowercase()
                // Fast context match
                for (kw in CONTEXT_KEYWORDS) {
                    if (lower.contains(kw)) {
                        contextMatches++
                        break
                    }
                }

                // Check for action buttons
                if (acceptBounds == null) {
                    for (ab in ACCEPT_BUTTON_KEYWORDS) {
                        if (lower.contains(ab) && (node.isClickable || node.className?.contains("Button", ignoreCase = true) == true || bounds.height() > 30)) {
                            acceptBounds = bounds
                            break
                        }
                    }
                }

                if (rejectBounds == null) {
                    for (rb in REJECT_BUTTON_KEYWORDS) {
                        if (lower.contains(rb) && (node.isClickable || bounds.height() > 30)) {
                            rejectBounds = bounds
                            break
                        }
                    }
                }
            }

            val childCount = node.childCount
            for (i in 0 until childCount) {
                node.getChild(i)?.let { stack.add(it) }
            }
        }

        // Need at least 2 context keywords to be confident it's a delivery order
        if (contextMatches < 2 && acceptBounds == null) {
            return null
        }

        return parseFromTextEntries(nodeTexts, acceptBounds, rejectBounds, packageName)
    }

    /**
     * Parses collected text lines into a structured ParsedOrder
     */
    fun parseFromTextEntries(
        entries: List<Pair<String, Rect>>,
        acceptBounds: Rect?,
        rejectBounds: Rect?,
        packageName: String = ""
    ): ParsedOrder? {
        val fullRaw = StringBuilder()
        var restaurantDistKm: Double? = null
        var customerDistKm: Double? = null
        var totalDistKm: Double? = null
        var itemCount: Int? = null
        var feeSar: Double? = null
        val genericDistances = ArrayList<Double>()

        for ((line, _) in entries) {
            fullRaw.append(line).append("\n")
            val lower = line.lowercase()

            // 1. Check distances
            val dist = extractDistanceKm(line)
            if (dist != null) {
                if (lower.contains("متجر") || lower.contains("مطعم") || lower.contains("استلام") || lower.contains("store") || lower.contains("pickup") || lower.contains("موقعك")) {
                    if (restaurantDistKm == null) restaurantDistKm = dist
                } else if (lower.contains("عميل") || lower.contains("تسليم") || lower.contains("وجهة") || lower.contains("customer") || lower.contains("dropoff") || lower.contains("destination")) {
                    if (customerDistKm == null) customerDistKm = dist
                } else if (lower.contains("كلية") || lower.contains("إجمالي") || lower.contains("total")) {
                    if (totalDistKm == null) totalDistKm = dist
                } else {
                    genericDistances.add(dist)
                }
            }

            // 2. Check item count
            if (itemCount == null) {
                itemCount = extractItemCount(line)
            }

            // 3. Check delivery fee
            if (feeSar == null) {
                feeSar = extractFee(line)
            }
        }

        // Route assignment logic:
        // In Saudi and international delivery apps (Locate, Jahez, Hungerstation, UberEats):
        // First distance is almost always Driver -> Pickup (Restaurant).
        // Second distance is Store -> Customer (Dropoff).
        if (restaurantDistKm == null && genericDistances.isNotEmpty()) {
            restaurantDistKm = genericDistances[0]
            if (customerDistKm == null && genericDistances.size > 1) {
                customerDistKm = genericDistances[1]
            }
        } else if (customerDistKm == null && genericDistances.isNotEmpty()) {
            customerDistKm = genericDistances[0]
        }

        val rest = restaurantDistKm ?: 1.5
        val cust = customerDistKm ?: 4.0
        val tot = totalDistKm ?: (rest + cust)
        val items = itemCount ?: 2

        val fullRawString = fullRaw.toString().trim()
        if (fullRawString.isBlank()) return null

        return ParsedOrder(
            id = "ord_" + System.currentTimeMillis().toString().takeLast(6),
            rawText = fullRawString,
            restaurantDistanceKm = Math.round(rest * 100.0) / 100.0,
            customerDistanceKm = Math.round(cust * 100.0) / 100.0,
            totalDistanceKm = Math.round(tot * 100.0) / 100.0,
            itemCount = items,
            deliveryFeeSar = feeSar ?: 18.0,
            acceptButtonBounds = acceptBounds,
            rejectButtonBounds = rejectBounds,
            packageName = packageName,
            detectedAtTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * Extract distance in km from a text fragment
     */
    fun extractDistanceKm(text: String): Double? {
        val kmMatcher = DISTANCE_KM_PATTERN.matcher(text)
        if (kmMatcher.find()) {
            val numStr = kmMatcher.group(1)
            return numStr?.toDoubleOrNull()
        }
        val meterMatcher = DISTANCE_METER_PATTERN.matcher(text)
        if (meterMatcher.find()) {
            val numStr = meterMatcher.group(1)
            val meters = numStr?.toDoubleOrNull()
            if (meters != null) return meters / 1000.0
        }
        return null
    }

    /**
     * Extract item count
     */
    fun extractItemCount(text: String): Int? {
        if (text.contains("عنصران") || text.contains("عنصرين") || text.contains("وجبتان") || text.contains("وجبتين")) {
            return 2
        }
        if (text.contains("عنصر واحد") || text.contains("وجبة واحدة")) {
            return 1
        }
        val matcher = ITEMS_PATTERN.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.toIntOrNull()
        }
        val prefixMatcher = ITEMS_PREFIX_PATTERN.matcher(text)
        if (prefixMatcher.find()) {
            return prefixMatcher.group(1)?.toIntOrNull()
        }
        return null
    }

    /**
     * Extract fee in SAR
     */
    fun extractFee(text: String): Double? {
        val matcher = FEE_SAR_PATTERN.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.toDoubleOrNull()
        }
        return null
    }
}
