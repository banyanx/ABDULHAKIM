package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.DecisionEngine
import com.example.core.OrderParser
import com.example.data.model.DriverRules
import com.example.data.model.OrderDecision
import com.example.data.model.ParsedOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Locate Assistant", appName)
    }

    @Test
    fun testOrderParserNormalization() {
        // Test Eastern Arabic numerals to Western digits
        val easternArabic = "المتجر: ١.٥ كم والعميل ٣.٢ كم وعدد الأصناف: ٤ وقيمة التوصيل ٢٥ ريال"
        val normalized = OrderParser.normalizeText(easternArabic)
        assertTrue(normalized.contains("1.5"))
        assertTrue(normalized.contains("3.2"))
        assertTrue(normalized.contains("25"))

        val dist = OrderParser.extractDistanceKm(normalized)
        assertNotNull(dist)
        assertEquals(1.5, dist!!, 0.01)

        val fee = OrderParser.extractFee(normalized)
        assertNotNull(fee)
        assertEquals(25.0, fee!!, 0.01)
    }

    @Test
    fun testDecisionEngineAcceptAndReject() {
        val rules = DriverRules(
            maxRestaurantDistanceKm = 3.5,
            maxCustomerDistanceKm = 5.0,
            maxQuantity = 2,
            autoAcceptEnabled = true,
            autoRejectEnabled = true
        )

        // Ideal order: both distances within limits
        val idealOrder = ParsedOrder(
            rawText = "Order Al Baik",
            restaurantDistanceKm = 1.2,
            customerDistanceKm = 4.0,
            totalDistanceKm = 5.2,
            itemCount = 2,
            deliveryFeeSar = 20.0
        )
        val acceptResult = DecisionEngine.evaluate(idealOrder, rules, System.nanoTime())
        assertEquals(OrderDecision.ACCEPT, acceptResult.decision)

        // Exceeding restaurant distance (6.0 > 3.5)
        val farRestaurantOrder = ParsedOrder(
            rawText = "Order Far Restaurant",
            restaurantDistanceKm = 6.0,
            customerDistanceKm = 2.0,
            totalDistanceKm = 8.0,
            itemCount = 2,
            deliveryFeeSar = 18.0
        )
        val rejectRestResult = DecisionEngine.evaluate(farRestaurantOrder, rules, System.nanoTime())
        assertEquals(OrderDecision.REJECT, rejectRestResult.decision)

        // Exceeding customer distance (6.5 > 5.0)
        val farCustomerOrder = ParsedOrder(
            rawText = "Order Far Customer",
            restaurantDistanceKm = 2.0,
            customerDistanceKm = 6.5,
            totalDistanceKm = 8.5,
            itemCount = 2,
            deliveryFeeSar = 25.0
        )
        val rejectCustResult = DecisionEngine.evaluate(farCustomerOrder, rules, System.nanoTime())
        assertEquals(OrderDecision.REJECT, rejectCustResult.decision)
    }
}
