package com.mparticle.kits

import android.app.Activity
import android.content.Context
import android.net.Uri
import com.google.firebase.analytics.FirebaseAnalytics
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticleOptions.DataplanOptions
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.commerce.Promotion
import com.mparticle.commerce.TransactionAttributes
import com.mparticle.identity.IdentityApi
import com.mparticle.internal.CoreCallbacks
import com.mparticle.internal.CoreCallbacks.KitListener
import com.mparticle.testutils.TestingUtils
import junit.framework.TestCase
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.lang.ref.WeakReference
import java.lang.reflect.Modifier
import java.util.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class GoogleAnalyticsFirebaseGA4KitTest {
    private lateinit var kitInstance: GoogleAnalyticsFirebaseGA4Kit
    private lateinit var firebaseSdk: FirebaseAnalytics
    private var random = Random()

    @Before
    @Throws(JSONException::class)
    fun before() {
        FirebaseAnalytics.clearInstance()
        FirebaseAnalytics.setFirebaseId("firebaseId")
        kitInstance = GoogleAnalyticsFirebaseGA4Kit()
        MParticle.setInstance(Mockito.mock(MParticle::class.java))
        Mockito.`when`(MParticle.getInstance()?.Identity()).thenReturn(
            Mockito.mock(
                IdentityApi::class.java
            )
        )
        val kitManager = KitManagerImpl(
            Mockito.mock(
                Context::class.java
            ), null, emptyCoreCallbacks, null
        )
        kitInstance.kitManager = kitManager
        kitInstance.configuration =
            KitConfiguration.createKitConfiguration(JSONObject().put("id", "-1"))
        firebaseSdk = FirebaseAnalytics.getInstance(null)!!
    }

    /**
     * make sure that all MPEvents are getting translating their getInfo() value to the bundle of the Firebase event.
     * MPEvent.getName() should be the firebase event name in all cases, except when the MPEvent.type is MPEvent.Search
     */
    @Test
    fun testEmptyEvent() {
        kitInstance.logEvent(MPEvent.Builder("eventName", MParticle.EventType.Other).build())
        TestCase.assertEquals(1, firebaseSdk.loggedEvents.size)
        var firebaseEvent = firebaseSdk.loggedEvents[0]
        TestCase.assertEquals("eventName", firebaseEvent.key)
        TestCase.assertEquals(0, firebaseEvent.value.size())

        for (i in 0..9) {
            val event = TestingUtils.getInstance().randomMPEventRich
            firebaseSdk.clearLoggedEvents()
            kitInstance.logEvent(event)
            TestCase.assertEquals(1, firebaseSdk.loggedEvents.size)
            firebaseEvent = firebaseSdk.loggedEvents[0]
            if (event.eventType != MParticle.EventType.Search) {
                TestCase.assertEquals(
                    kitInstance.standardizeName(event.eventName, true),
                    firebaseEvent.key
                )

            } else {
                TestCase.assertEquals("search", firebaseEvent.key)
            }
            event.customAttributeStrings?.let {
                TestCase.assertEquals(
                    it.size,
                    firebaseEvent.value.size()
                )
                for (customAttEvent in it) {
                    val key = kitInstance.standardizeName(customAttEvent.key, true)
                    val value = kitInstance.standardizeValue(customAttEvent.value, true)
                    if (key != null) {
                        TestCase.assertEquals(
                            value, firebaseEvent.value.getString(
                                key
                            )
                        )
                    }
                }
            }
        }
    }

    @Test
    fun testPromotionCommerceEvent() {
        val promotion = Promotion()
        promotion.creative = "asdva"
        promotion.id = "1234"
        promotion.name = "1234asvd"
        promotion.position = "2"
        val event = CommerceEvent.Builder(Promotion.CLICK, promotion).build()
        kitInstance.logEvent(event)
        TestCase.assertEquals(1, firebaseSdk.loggedEvents.size)
    }

    @Test
    fun testShippingInfoCommerceEvent() {
        val event = CommerceEvent.Builder(
            Product.CHECKOUT_OPTION,
            Product.Builder("asdv", "asdv", 1.3).build()
        )
            .addCustomFlag(
                GoogleAnalyticsFirebaseGA4Kit.CF_GA4COMMERCE_EVENT_TYPE,
                FirebaseAnalytics.Event.ADD_SHIPPING_INFO
            )
            .addCustomFlag(GoogleAnalyticsFirebaseGA4Kit.CF_GA4_SHIPPING_TIER, "overnight")
            .build()
        kitInstance.logEvent(event)
        TestCase.assertEquals(1, firebaseSdk.loggedEvents.size)
        TestCase.assertEquals("add_shipping_info", firebaseSdk.loggedEvents[0].key)
        TestCase.assertEquals(
            "overnight",
            firebaseSdk.loggedEvents[0].value.getString("shipping_tier")
        )
    }

    @Test
    fun testPaymentInfoCommerceEvent() {
        val event = CommerceEvent.Builder(
            Product.CHECKOUT_OPTION,
            Product.Builder("asdv", "asdv", 1.3).build()
        )
            .addCustomFlag(
                GoogleAnalyticsFirebaseGA4Kit.CF_GA4COMMERCE_EVENT_TYPE,
                FirebaseAnalytics.Event.ADD_PAYMENT_INFO
            )
            .addCustomFlag(GoogleAnalyticsFirebaseGA4Kit.CF_GA4_PAYMENT_TYPE, "visa")
            .build()
        kitInstance.logEvent(event)
        TestCase.assertEquals(1, firebaseSdk.loggedEvents.size)
        TestCase.assertEquals("add_payment_info", firebaseSdk.loggedEvents[0].key)
        TestCase.assertEquals("visa", firebaseSdk.loggedEvents[0].value.getString("payment_type"))
    }

    @Test
    fun testCheckoutOptionCommerceEvent() {
        val customEventTypes = arrayOf(
            FirebaseAnalytics.Event.ADD_PAYMENT_INFO,
            FirebaseAnalytics.Event.ADD_SHIPPING_INFO
        )
        for (customEventType in customEventTypes) {
            val event = CommerceEvent.Builder(
                Product.CHECKOUT_OPTION,
                Product.Builder("asdv", "asdv", 1.3).build()
            )
                .addCustomFlag(
                    GoogleAnalyticsFirebaseGA4Kit.CF_GA4COMMERCE_EVENT_TYPE,
                    customEventType
                )
                .build()
            kitInstance.logEvent(event)
            TestCase.assertEquals(1, firebaseSdk.loggedEvents.size)
            TestCase.assertEquals(customEventType, firebaseSdk.loggedEvents[0].key)
            firebaseSdk.clearLoggedEvents()
        }
    }

    @Test
    @Throws(IllegalAccessException::class)
    fun testCommerceEvent() {
        for (field in Product::class.java.fields) {
            if (Modifier.isPublic(field.modifiers) && Modifier.isStatic(field.modifiers)) {
                firebaseSdk.clearLoggedEvents()
                val eventType = field?.get(null).toString()
                if (eventType != "remove_from_wishlist" && eventType != "checkout_option") {
                    val event = CommerceEvent.Builder(
                        eventType,
                        Product.Builder("asdv", "asdv", 1.3).build()
                    )
                        .transactionAttributes(
                            TransactionAttributes().setId("235").setRevenue(23.3)
                                .setAffiliation("231")
                        )
                        .build()
                    kitInstance.logEvent(event)
                    TestCase.assertEquals(
                        "failed for event type: $eventType",
                        1,
                        firebaseSdk.loggedEvents.size
                    )
                }
            }
        }
    }

    @Test
    fun testNameStandardization() {
        val badPrefixes = arrayOf("firebase_event_name", "google_event_name", "ga_event_name")
        for (badPrefix in badPrefixes) {
            val clean = kitInstance.standardizeName(badPrefix, true)
            TestCase.assertEquals("event_name", clean)
        }
        val emptySpace1 = "event name"
        val emptySpace2 = "event_name "
        val emptySpace3 = "event  name "
        val emptySpace4 = "event - name "
        TestCase.assertEquals(
            "event_name",
            kitInstance.standardizeName(emptySpace1, true)
        )
        TestCase.assertEquals(
            "event_name_",
            kitInstance.standardizeName(emptySpace2, true)
        )
        TestCase.assertEquals(
            "event__name_",
            kitInstance.standardizeName(emptySpace3, true)
        )
        TestCase.assertEquals(
            "event___name_",
            kitInstance.standardizeName(emptySpace4, true)
        )
        TestCase.assertEquals(
            "event_name ",
            kitInstance.standardizeName(emptySpace2, false)
        )
        TestCase.assertEquals(
            "event  name ",
            kitInstance.standardizeName(emptySpace3, false)
        )
        TestCase.assertEquals(
            "event - name ",
            kitInstance.standardizeName(emptySpace4, false)
        )
        TestCase.assertEquals(
            "!event - name !",
            kitInstance.standardizeName("!event - name !", false)
        )
        TestCase.assertEquals(
            "!@#\$%^&*()_+=[]{}|'\"?>",
            kitInstance.standardizeName("!@#\$%^&*()_+=[]{}|'\"?>", false)
        )

        val badStarts = arrayOf(
            "!@#$%^&*()_+=[]{}|'\"?><:;event_name",
            "_event_name",
            "   event_name",
            "_event_name"
        )
        for (badStart in badStarts) {
            val clean = kitInstance.standardizeName(badStart, true)
            TestCase.assertEquals("event_name", clean)
        }
        val justFine =
            "abcdefghijklmnopqrstuvwx"
        var sanitized: String = kitInstance.standardizeName(justFine, true).toString()
        TestCase.assertEquals(24, sanitized.length)
        TestCase.assertTrue(justFine.startsWith(sanitized))
        sanitized = kitInstance.standardizeName(justFine, false).toString()
        TestCase.assertEquals(24, sanitized.length)
        TestCase.assertTrue(justFine.startsWith(sanitized))
        sanitized = kitInstance.standardizeValue(justFine, true)
        TestCase.assertEquals(24, sanitized.length)
        TestCase.assertTrue(justFine.startsWith(sanitized))
        sanitized = kitInstance.standardizeValue(justFine, false)
        TestCase.assertEquals(24, sanitized.length)
        TestCase.assertTrue(justFine.startsWith(sanitized))

        val tooLong =
            "abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890"
        sanitized  = kitInstance.standardizeName(tooLong, true).toString()
        TestCase.assertEquals(40, sanitized.length)
        TestCase.assertTrue(tooLong.startsWith(sanitized))
        sanitized = kitInstance.standardizeName(tooLong, false).toString()
        TestCase.assertEquals(24, sanitized.length)
        TestCase.assertTrue(tooLong.startsWith(sanitized))
        sanitized = kitInstance.standardizeValue(tooLong, true)
        TestCase.assertEquals(100, sanitized.length)
        TestCase.assertTrue(tooLong.startsWith(sanitized))
        sanitized = kitInstance.standardizeValue(tooLong, false)
        TestCase.assertEquals(36, sanitized.length)
        TestCase.assertTrue(tooLong.startsWith(sanitized))

        val emptyStrings = arrayOf(
            "!@#$%^&*()_+=[]{}|'\"?><:;",
            "_1234567890",
            " ",
            ""
        )
        for (emptyString in emptyStrings) {
            val empty = kitInstance.standardizeName(emptyString, true)
            TestCase.assertEquals("invalid_ga4_key", empty)
        }
    }

    @Test
    fun testMaxStandardization() {
        val testSucccessAttributes = mapOf(
            "test1" to "parameter",
            "test2" to "parameter",
            "test3" to "parameter",
            "test4" to "parameter",
            "test5" to "parameter",
            "test6" to "parameter",
            "test7" to "parameter",
            "test8" to "parameter",
            "test9" to "parameter",
            "test10" to "parameter",
            "test11" to "parameter",
            "test12" to "parameter",
            "test13" to "parameter",
            "test14" to "parameter",
            "test15" to "parameter",
            "test16" to "parameter",
            "test17" to "parameter",
            "test18" to "parameter",
            "test19" to "parameter",
            "test20" to "parameter",
            "test21" to "parameter",
            "test22" to "parameter",
            "test23" to "parameter",
            "test24" to "parameter"
        )
        val testTruncatedAttributes = mapOf(
            "test1" to "parameter",
            "test2" to "parameter",
            "test3" to "parameter",
            "test4" to "parameter",
            "test5" to "parameter",
            "test6" to "parameter",
            "test7" to "parameter",
            "test8" to "parameter",
            "test9" to "parameter",
            "test10" to "parameter",
            "test11" to "parameter",
            "test12" to "parameter",
            "test13" to "parameter",
            "test14" to "parameter",
            "test15" to "parameter",
            "test16" to "parameter",
            "test17" to "parameter",
            "test18" to "parameter",
            "test19" to "parameter",
            "test20" to "parameter",
            "test21" to "parameter",
            "test22" to "parameter",
            "test23" to "parameter",
            "test24" to "parameter",
            "z1" to "parameter",
            "z2" to "parameter",
            "z3" to "parameter",
            "z4" to "parameter"
        )
        val testFinalAttributes = mapOf(
            "test1" to "parameter",
            "test2" to "parameter",
            "test3" to "parameter",
            "test4" to "parameter",
            "test5" to "parameter",
            "test6" to "parameter",
            "test7" to "parameter",
            "test8" to "parameter",
            "test9" to "parameter",
            "test10" to "parameter",
            "test11" to "parameter",
            "test12" to "parameter",
            "test13" to "parameter",
            "test14" to "parameter",
            "test15" to "parameter",
            "test16" to "parameter",
            "test17" to "parameter",
            "test18" to "parameter",
            "test19" to "parameter",
            "test20" to "parameter",
            "test21" to "parameter",
            "test22" to "parameter",
            "test23" to "parameter",
            "test24" to "parameter",
            "currency" to "USD"
        )
        val event = CommerceEvent.Builder(
            Product.CHECKOUT_OPTION,
            Product.Builder("asdv", "asdv", 1.3).build()
        )
            .build()
        event.customAttributes = testSucccessAttributes
        kitInstance.logEvent(event)
        TestCase.assertEquals(1, firebaseSdk.loggedEvents.size)
        TestCase.assertEquals(25, firebaseSdk.loggedEvents[0].value.size())
        TestCase.assertEquals(testFinalAttributes, firebaseSdk.loggedEvents[0].value)
        firebaseSdk.clearLoggedEvents()

        event.customAttributes = testTruncatedAttributes
        kitInstance.logEvent(event)
        TestCase.assertEquals(1, firebaseSdk.loggedEvents.size)
        TestCase.assertEquals(25, firebaseSdk.loggedEvents[0].value.size())
        TestCase.assertEquals(testFinalAttributes, firebaseSdk.loggedEvents[0].value)
        firebaseSdk.clearLoggedEvents()
//
//        MPProduct *product1 = [[MPProduct alloc] initWithName:@"William Hartnell" sku:@"1who" quantity:@1 price:@42.0];
//        MPProduct *product2 = [[MPProduct alloc] initWithName:@"Patrick Troughton" sku:@"2who" quantity:@1 price:@42.0];
//        MPProduct *product3 = [[MPProduct alloc] initWithName:@"Jon Pertwee" sku:@"3who" quantity:@1 price:@42.0];
//        MPProduct *product4 = [[MPProduct alloc] initWithName:@"Tom Baker" sku:@"4who" quantity:@1 price:@42.0];
//        MPProduct *product5 = [[MPProduct alloc] initWithName:@"Peter Davison" sku:@"5who" quantity:@1 price:@42.0];
//        MPProduct *product6 = [[MPProduct alloc] initWithName:@"Colin Baker" sku:@"6who" quantity:@1 price:@42.0];
//        MPProduct *product7 = [[MPProduct alloc] initWithName:@"Sylvester McCoy" sku:@"7who" quantity:@1 price:@42.0];
//        MPProduct *product8 = [[MPProduct alloc] initWithName:@"Paul McGann" sku:@"8who" quantity:@1 price:@42.0];
//        MPProduct *product9 = [[MPProduct alloc] initWithName:@"Christopher Eccleston" sku:@"9who" quantity:@1 price:@42.0];
//        MPProduct *product10 = [[MPProduct alloc] initWithName:@"David Tennant" sku:@"10who" quantity:@1 price:@42.0];
//        MPProduct *product11 = [[MPProduct alloc] initWithName:@"Matt Smith" sku:@"11who" quantity:@1 price:@42.0];
//        MPProduct *product12 = [[MPProduct alloc] initWithName:@"Peter Capaldi" sku:@"12who" quantity:@1 price:@42.0];
//        MPProduct *product13 = [[MPProduct alloc] initWithName:@"Jodie Whittaker" sku:@"13who" quantity:@1 price:@42.0];
//
//        MPCommerceEvent *purchaseEvent = [[MPCommerceEvent alloc] initWithAction:MPCommerceEventActionPurchase];
//        purchaseEvent.products = @[product1, product2, product3, product4, product5, product6, product7, product8, product9, product10, product11, product12, product13];
//
//        parameters = [exampleKit getParameterForCommerceEvent:purchaseEvent];
//        XCTAssertEqual([parameters count], 2);
//        XCTAssertEqual([parameters[@"items"] count], 13);
//        XCTAssertTrue([parameters[@"items"][0] count] <= 10);
//
//        execStatus = [exampleKit logBaseEvent:purchaseEvent];
//        XCTAssertTrue(execStatus.success);
    }

    @Test
    fun testScreenNameSanitized() {
        kitInstance.logScreen("Some long Screen name", null)
        TestCase.assertEquals(
            "Some_long_Screen_name",
            FirebaseAnalytics.getInstance(null)?.currentScreenName
        )
    }

    private var emptyCoreCallbacks: CoreCallbacks = object : CoreCallbacks {
        var activity = Activity()
        override fun isBackgrounded(): Boolean = false

        override fun getUserBucket(): Int = 0

        override fun isEnabled(): Boolean = false

        override fun setIntegrationAttributes(i: Int, map: Map<String, String>) {}

        override fun getIntegrationAttributes(i: Int): Map<String, String>? = null

        override fun getCurrentActivity(): WeakReference<Activity> = WeakReference(activity)

        override fun getLatestKitConfiguration(): JSONArray? = null

        override fun getDataplanOptions(): DataplanOptions? = null

        override fun isPushEnabled(): Boolean = false

        override fun getPushSenderId(): String? = null

        override fun getPushInstanceId(): String? = null

        override fun getLaunchUri(): Uri? = null

        override fun getLaunchAction(): String? = null

        override fun getKitListener(): KitListener = KitListener.EMPTY

    }
}
