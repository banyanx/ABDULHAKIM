package com.example.core

import com.example.data.model.DriverRules
import com.example.data.model.OrderDecision
import com.example.data.model.OrderEvaluationResult
import com.example.data.model.ParsedOrder

object DecisionEngine {

    /**
     * Evaluates parsed order against active driver rules in microsecond execution time
     */
    fun evaluate(order: ParsedOrder, rules: DriverRules, startTimeNs: Long): OrderEvaluationResult {
        val restDist = order.restaurantDistanceKm
        val custDist = order.customerDistanceKm
        val totalDist = order.totalDistanceKm
        val items = order.itemCount

        var isReject = false
        var reasonEn = ""
        var reasonAr = ""

        // Independent Check 1: Restaurant distance (Store / Pickup)
        if (restDist > rules.maxRestaurantDistanceKm) {
            isReject = true
            reasonEn = "Restaurant distance (${restDist} km) exceeds limit (${rules.maxRestaurantDistanceKm} km)"
            reasonAr = "مسافة المتجر (${restDist} كم) تتجاوز الحد الأقصى (${rules.maxRestaurantDistanceKm} كم)"
        }
        // Independent Check 2: Customer distance (Destination / Dropoff)
        else if (custDist > rules.maxCustomerDistanceKm) {
            isReject = true
            reasonEn = "Customer distance (${custDist} km) exceeds limit (${rules.maxCustomerDistanceKm} km)"
            reasonAr = "مسافة العميل (${custDist} كم) تتجاوز الحد الأقصى (${rules.maxCustomerDistanceKm} كم)"
        }
        // Check 3: Item quantity
        else if (items > rules.maxQuantity) {
            isReject = true
            reasonEn = "Item count ($items) exceeds limit (${rules.maxQuantity})"
            reasonAr = "عدد الأصناف ($items) يتجاوز الحد الأقصى (${rules.maxQuantity})"
        }

        val decision: OrderDecision = if (isReject) {
            if (rules.autoRejectEnabled) OrderDecision.REJECT else OrderDecision.MANUAL
        } else {
            if (rules.autoAcceptEnabled) OrderDecision.ACCEPT else OrderDecision.MANUAL
        }

        if (!isReject) {
            reasonEn = "Optimal order: Store ${restDist}km, Dropoff ${custDist}km, $items items"
            reasonAr = "طلب مطابق للشروط: المتجر ${restDist} كم، العميل ${custDist} كم، $items عناصر"
        }

        val elapsedMs = (System.nanoTime() - startTimeNs) / 1_000_000L

        return OrderEvaluationResult(
            order = order,
            decision = decision,
            reasonEn = reasonEn,
            reasonAr = reasonAr,
            processingTimeMs = if (elapsedMs <= 0) 1 else elapsedMs
        )
    }
}
