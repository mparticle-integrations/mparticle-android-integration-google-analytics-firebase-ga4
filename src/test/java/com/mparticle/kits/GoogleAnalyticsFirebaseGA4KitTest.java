package com.mparticle.kits;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.Promotion;
import com.mparticle.commerce.TransactionAttributes;
import com.mparticle.identity.IdentityApi;
import com.mparticle.internal.CoreCallbacks;
import com.mparticle.testutils.TestingUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class GoogleAnalyticsFirebaseGA4KitTest {
    GoogleAnalyticsFirebaseGA4Kit kitInstance;
    FirebaseAnalytics firebaseSdk;
    Random random = new Random();

    @Before
    public void before() throws JSONException {
        FirebaseAnalytics.clearInstance();
        FirebaseAnalytics.setFirebaseId("firebaseId");
        kitInstance = new GoogleAnalyticsFirebaseGA4Kit();
        MParticle.setInstance(Mockito.mock(MParticle.class));
        Mockito.when(MParticle.getInstance().Identity()).thenReturn(Mockito.mock(IdentityApi.class));
        KitManagerImpl kitManager = new KitManagerImpl(Mockito.mock(Context.class), null, emptyCoreCallbacks, null);
        kitInstance.setKitManager(kitManager);
        kitInstance.setConfiguration(KitConfiguration.createKitConfiguration(new JSONObject().put("id", "-1")));
        kitInstance.onKitCreate(new HashMap<String, String>(), Mockito.mock(Context.class));
        firebaseSdk = FirebaseAnalytics.getInstance(null);
    }

    /**
     * make sure that all MPEvents are getting translating their getInfo() value to the bundle of the Firebase event.
     * MPEvent.getName() should be the firebase event name in all cases, except when the MPEvent.type is MPEvent.Search
     */
    @Test
    public void testEmptyEvent() {
        kitInstance.logEvent(new MPEvent.Builder("eventName", MParticle.EventType.Other).build());
        assertEquals(1, firebaseSdk.getLoggedEvents().size());
        Map.Entry<String, Bundle> firebaseEvent = firebaseSdk.getLoggedEvents().get(0);
        assertEquals("eventName", firebaseEvent.getKey());
        assertEquals(0, firebaseEvent.getValue().size());

        for (int i = 0; i < 10; i++) {
            MPEvent event = TestingUtils.getInstance().getRandomMPEventRich();
            firebaseSdk.clearLoggedEvents();
            kitInstance.logEvent(event);
            assertEquals(1, firebaseSdk.getLoggedEvents().size());
            firebaseEvent = firebaseSdk.getLoggedEvents().get(0);
            if (event.getEventType() != MParticle.EventType.Search) {
                assertEquals(kitInstance.standardizeName(event.getEventName(), true), firebaseEvent.getKey());
            } else {
                assertEquals("search", firebaseEvent.getKey());
            }
            if (event.getCustomAttributeStrings() != null) {
                assertEquals(event.getCustomAttributeStrings().size(), firebaseEvent.getValue().size());
                for (Map.Entry<String, String> entry : event.getCustomAttributeStrings().entrySet()) {
                    String key = kitInstance.standardizeName(entry.getKey(), true);
                    String value = kitInstance.standardizeValue(entry.getValue(), true);
                    assertEquals(value, firebaseEvent.getValue().getString(key));
                }
            }
        }
    }

    @Test
    public void testPromotionCommerceEvent() {
        Promotion promotion = new Promotion();
        promotion.setCreative("asdva");
        promotion.setId("1234");
        promotion.setName("1234asvd");
        promotion.setPosition("2");
        CommerceEvent event = new CommerceEvent.Builder(Promotion.CLICK, promotion).build();
        kitInstance.logEvent(event);
        assertEquals(1, firebaseSdk.getLoggedEvents().size());
    }

    @Test
    public void testShippingInfoCommerceEvent() {
        CommerceEvent event = new CommerceEvent.Builder(Product.CHECKOUT_OPTION, new Product.Builder("asdv", "asdv", 1.3).build())
                .addCustomFlag(GoogleAnalyticsFirebaseGA4Kit.CF_GA4COMMERCE_EVENT_TYPE, FirebaseAnalytics.Event.ADD_SHIPPING_INFO)
                .addCustomFlag(GoogleAnalyticsFirebaseGA4Kit.CF_GA4_SHIPPING_TIER, "overnight")
                .build();
        kitInstance.logEvent(event);

        assertEquals(1, firebaseSdk.getLoggedEvents().size());
        assertEquals("add_shipping_info", firebaseSdk.getLoggedEvents().get(0).getKey());
        assertEquals("overnight", firebaseSdk.getLoggedEvents().get(0).getValue().getString("shipping_tier"));
    }

    @Test
    public void testPaymentInfoCommerceEvent() {
        CommerceEvent event = new CommerceEvent.Builder(Product.CHECKOUT_OPTION, new Product.Builder("asdv", "asdv", 1.3).build())
                .addCustomFlag(GoogleAnalyticsFirebaseGA4Kit.CF_GA4COMMERCE_EVENT_TYPE, FirebaseAnalytics.Event.ADD_PAYMENT_INFO)
                .addCustomFlag(GoogleAnalyticsFirebaseGA4Kit.CF_GA4_PAYMENT_TYPE, "visa")
                .build();
        kitInstance.logEvent(event);

        assertEquals(1, firebaseSdk.getLoggedEvents().size());
        assertEquals("add_payment_info", firebaseSdk.getLoggedEvents().get(0).getKey());
        assertEquals("visa", firebaseSdk.getLoggedEvents().get(0).getValue().getString("payment_type"));
    }

    @Test
    public void testCheckoutOptionCommerceEvent() {
        String[] customEventTypes = new String[]{FirebaseAnalytics.Event.ADD_PAYMENT_INFO, FirebaseAnalytics.Event.ADD_SHIPPING_INFO};
        for (String customEventType: customEventTypes) {
            CommerceEvent event = new CommerceEvent.Builder(Product.CHECKOUT_OPTION, new Product.Builder("asdv", "asdv", 1.3).build())
                    .addCustomFlag(GoogleAnalyticsFirebaseGA4Kit.CF_GA4COMMERCE_EVENT_TYPE, customEventType)
                    .build();
            kitInstance.logEvent(event);

            assertEquals(1, firebaseSdk.getLoggedEvents().size());
            assertEquals(customEventType, firebaseSdk.getLoggedEvents().get(0).getKey());
            firebaseSdk.clearLoggedEvents();
        }
    }

    @Test
    public void testCommerceEvent() throws IllegalAccessException {
        for (Field field: Product.class.getFields()) {
            if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
                firebaseSdk.clearLoggedEvents();
                String eventType = field.get(null).toString();
                if (!eventType.equals("remove_from_wishlist") && !eventType.equals("checkout_option")) {
                    CommerceEvent event = new CommerceEvent.Builder(eventType, new Product.Builder("asdv", "asdv", 1.3).build())
                            .transactionAttributes(new TransactionAttributes().setId("235").setRevenue(23.3).setAffiliation("231"))
                            .build();
                    kitInstance.logEvent(event);
                    assertEquals("failed for event type: " + eventType, 1, firebaseSdk.getLoggedEvents().size());
                }
            }
        }
    }


    @Test
    public void testNameStandardization() {
        String[] badPrefixes = new String[]{"firebase_event_name", "google_event_name", "ga_event_name"};
        for (String badPrefix: badPrefixes) {
            String clean = kitInstance.standardizeName(badPrefix, random.nextBoolean());
            assertEquals("event_name", clean);
        }

        String emptySpace1 = "event name";
        String emptySpace2 = "event_name ";
        String emptySpace3 = "event  name ";
        String emptySpace4 = "event - name ";

        assertEquals("event_name", kitInstance.standardizeName(emptySpace1, random.nextBoolean()));
        assertEquals("event_name_", kitInstance.standardizeName(emptySpace2, random.nextBoolean()));
        assertEquals("event_name_", kitInstance.standardizeName(emptySpace3, random.nextBoolean()));
        assertEquals("event_name_", kitInstance.standardizeName(emptySpace4, random.nextBoolean()));


        String[] badStarts = new String[]{
                "!@#$%^&*()_+=[]{}|'\"?><:;event_name",
                "_event_name",
                "   event_name",
                "_event_name"};

        for (String badStart: badStarts) {
            String clean = kitInstance.standardizeName(badStart, random.nextBoolean());
            assertEquals("event_name", clean);
        }

        String tooLong = "abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890";

        String sanitized = kitInstance.standardizeName(tooLong, true);
        assertEquals(40, sanitized.length());
        assertTrue(tooLong.startsWith(sanitized));

        sanitized = kitInstance.standardizeName(tooLong, false);
        assertEquals(24, sanitized.length());
        assertTrue(tooLong.startsWith(sanitized));

        sanitized = kitInstance.standardizeValue(tooLong, true);
        assertEquals(100, sanitized.length());
        assertTrue(tooLong.startsWith(sanitized));

        sanitized = kitInstance.standardizeValue(tooLong, false);
        assertEquals(36, sanitized.length());
        assertTrue(tooLong.startsWith(sanitized));
    }

    @Test
    public void testScreenNameSanitized() {
        String name = "some long screen name";
        kitInstance.logScreen(name, new HashMap<String, String>(){{ put("param1","value1");}});
        assertEquals(1, FirebaseAnalytics.getInstance(null).getLoggedEvents().size());
        Map.Entry<String, Bundle> entry = FirebaseAnalytics.getInstance(null).getLoggedEvents().get(0);
        assertEquals(FirebaseAnalytics.Event.SCREEN_VIEW, FirebaseAnalytics.getInstance(null).getLoggedEvents().get(0).getKey());
        assertEquals("value1", FirebaseAnalytics.getInstance(null).getLoggedEvents().get(0).getValue().getString("param1"));
        assertEquals("some_long_screen_name", entry.getValue().getString(FirebaseAnalytics.Param.SCREEN_NAME));
        assertEquals("android.app.Activity", entry.getValue().getString(FirebaseAnalytics.Param.SCREEN_CLASS));
    }



    CoreCallbacks emptyCoreCallbacks = new CoreCallbacks() {
        Activity activity = new Activity();
        @Override
        public boolean isBackgrounded() {
            return false;
        }

        @Override
        public int getUserBucket() {
            return 0;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public void setIntegrationAttributes(int i, Map<String, String> map) {

        }

        @Override
        public Map<String, String> getIntegrationAttributes(int i) {
            return null;
        }

        @Override
        public WeakReference<Activity> getCurrentActivity() {
            return new WeakReference<Activity>(activity);
        }

        @Override
        public JSONArray getLatestKitConfiguration() {
            return null;
        }

        @Override
        public MParticleOptions.DataplanOptions getDataplanOptions() {
            return null;
        }

        @Override
        public boolean isPushEnabled() {
            return false;
        }

        @Override
        public String getPushSenderId() {
            return null;
        }

        @Override
        public String getPushInstanceId() {
            return null;
        }

        @Override
        public Uri getLaunchUri() {
            return null;
        }

        @Override
        public String getLaunchAction() {
            return null;
        }

        @Override
        public KitListener getKitListener() {
            return KitListener.EMPTY;
        }


    };

}