package com.mparticle.kits;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Impression;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.Promotion;
import com.mparticle.commerce.TransactionAttributes;
import com.mparticle.consent.ConsentState;
import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.Logger;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoogleAnalyticsFirebaseGA4Kit extends KitIntegration implements KitIntegration.EventListener, KitIntegration.IdentityListener, KitIntegration.CommerceListener, KitIntegration.UserAttributeListener {
    final static String SHOULD_HASH_USER_ID = "hashUserId";
    final static String FORWARD_REQUESTS_SERVER_SIDE = "forwardWebRequestsServerSide";
    final static String EXTERNAL_USER_IDENTITY_TYPE = "externalUserIdentityType";
    final static String EXTERNAL_USER_IDENTITY_CUSTOMER_ID = "CustomerId";
    final static String EXTERNAL_USER_IDENTITY_MPID = "mpid";
    final static String EXTERNAL_USER_IDENTITY_OTHER = "Other";
    final static String EXTERNAL_USER_IDENTITY_OTHER2 = "Other2";
    final static String EXTERNAL_USER_IDENTITY_OTHER3 = "Other3";
    final static String EXTERNAL_USER_IDENTITY_OTHER4 = "Other4";
    final static String EXTERNAL_USER_IDENTITY_OTHER5 = "Other5";
    final static String EXTERNAL_USER_IDENTITY_OTHER6 = "Other6";
    final static String EXTERNAL_USER_IDENTITY_OTHER7 = "Other7";
    final static String EXTERNAL_USER_IDENTITY_OTHER8 = "Other8";
    final static String EXTERNAL_USER_IDENTITY_OTHER9 = "Other9";
    final static String EXTERNAL_USER_IDENTITY_OTHER10 = "Other10";

    public final static String CF_GA4COMMERCE_EVENT_TYPE = "GA4.CommerceEventType";
    public final static String CF_GA4_PAYMENT_TYPE = "GA4.PaymentType";
    public final static String CF_GA4_SHIPPING_TIER = "GA4.ShippingTier";

    private static String instanceIdIntegrationKey = "app_instance_id";
    private static String[] forbiddenPrefixes = new String[]{"google_", "firebase_", "ga_"};
    private static int eventMaxLength = 40;
    private static int userAttributeMaxLength = 24;

    private static int eventValMaxLength = 100;
    private static int userAttributeValMaxLength = 36;

    @Override
    public String getName() {
        return "Google Analytics for Firebase";
    }

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> map, Context context) throws IllegalArgumentException {
        Logger.info(getName() + " Kit relies on a functioning instance of Firebase Analytics. If your Firebase Analytics instance is not configured properly, this Kit will not work");
        if (forwardRequestsServerSide()) {
            updateInstanceIDIntegration();
        }
        return null;
    }

    @Override
    public List<ReportingMessage> setOptOut(boolean b) {
        return null;
    }

    @Override
    public List<ReportingMessage> leaveBreadcrumb(String s) {
        return null;
    }

    @Override
    public List<ReportingMessage> logError(String s, Map<String, String> map) {
        return null;
    }

    @Override
    public List<ReportingMessage> logException(Exception e, Map<String, String> map, String s) {
        return null;
    }

    @Override
    public List<ReportingMessage> logEvent(MPEvent mpEvent) {
        if (forwardRequestsServerSide()) {
            return null;
        }

        FirebaseAnalytics.getInstance(getContext())
                .logEvent(getFirebaseEventName(mpEvent), toBundle(mpEvent.getInfo()));

        return Collections.singletonList(ReportingMessage.fromEvent(this, mpEvent));
    }

    @Override
    public List<ReportingMessage> logScreen(String s, Map<String, String> map) {
        if (forwardRequestsServerSide()) {
            return null;
        }

        Activity activity = getCurrentActivity().get();
        if (activity != null) {
            FirebaseAnalytics.getInstance(getContext()).setCurrentScreen(activity, standardizeName(s, true), null);
            return Collections.singletonList(new ReportingMessage(this, ReportingMessage.MessageType.SCREEN_VIEW, System.currentTimeMillis(), null));
        }
        return null;
    }

    @Override
    public List<ReportingMessage> logLtvIncrease(BigDecimal bigDecimal, BigDecimal bigDecimal1, String s, Map<String, String> map) {
        return null;
    }

    @Override
    public List<ReportingMessage> logEvent(CommerceEvent commerceEvent) {
        if (forwardRequestsServerSide()) {
            return null;
        }

        FirebaseAnalytics instance = FirebaseAnalytics.getInstance(getContext());
        String eventName;
        Bundle bundle;
        if (commerceEvent == null) {
            return null;
        }

        if (commerceEvent.getPromotionAction() != null) {
            if (commerceEvent.getPromotionAction() == Promotion.CLICK) {
                eventName = FirebaseAnalytics.Event.SELECT_PROMOTION;
            } else {
                eventName = FirebaseAnalytics.Event.VIEW_PROMOTION;
            }
            for (Promotion promotion : commerceEvent.getPromotions()) {
                bundle = getPromotionCommerceEventBundle(promotion).getBundle();
    
                instance.logEvent(eventName, bundle);
            }
        } else if (commerceEvent.getImpressions() != null) {
            eventName = FirebaseAnalytics.Event.VIEW_ITEM_LIST;
            for (Impression impression : commerceEvent.getImpressions()) {
                bundle = getImpressionCommerceEventBundle(impression.getListName(), impression.getProducts()).getBundle();
            
                instance.logEvent(eventName, bundle);
            }
        } else if (commerceEvent.getProductAction() != null) {
            bundle = getCommerceEventBundle(commerceEvent).getBundle();
            switch (commerceEvent.getProductAction()) {
                case Product.ADD_TO_CART:
                    eventName = FirebaseAnalytics.Event.ADD_TO_CART;
                    break;
                case Product.ADD_TO_WISHLIST:
                    eventName = FirebaseAnalytics.Event.ADD_TO_WISHLIST;
                    break;
                case Product.CHECKOUT:
                    eventName = FirebaseAnalytics.Event.BEGIN_CHECKOUT;
                    break;
                case Product.PURCHASE:
                    eventName = FirebaseAnalytics.Event.PURCHASE;
                    break;
                case Product.REFUND:
                    eventName = FirebaseAnalytics.Event.REFUND;
                    break;
                case Product.REMOVE_FROM_CART:
                    eventName = FirebaseAnalytics.Event.REMOVE_FROM_CART;
                    break;
                case Product.CLICK:
                    eventName = FirebaseAnalytics.Event.SELECT_ITEM;
                    break;
                case Product.CHECKOUT_OPTION:
                    Map<String, List<String>> customFlags = commerceEvent.getCustomFlags();
                    if (customFlags != null && customFlags.containsKey(CF_GA4COMMERCE_EVENT_TYPE)) {
                        List<String> commerceEventTypes = customFlags.get(CF_GA4COMMERCE_EVENT_TYPE);
                        if (!commerceEventTypes.isEmpty()) {
                            String commerceEventType = commerceEventTypes.get(0);
                            if (commerceEventType.equals(FirebaseAnalytics.Event.ADD_SHIPPING_INFO.toString())) {
                                eventName = FirebaseAnalytics.Event.ADD_SHIPPING_INFO;
                            } else if (commerceEventType.equals(FirebaseAnalytics.Event.ADD_PAYMENT_INFO.toString())) {
                                eventName = FirebaseAnalytics.Event.ADD_PAYMENT_INFO;
                            } else {
                                Logger.warning("You used an unsupported value for the custom flag 'GA4.CommerceEventType'. Please review the mParticle documentation. The event will be sent to Firebase with the deprecated SET_CHECKOUT_OPTION event type.");
                                eventName = FirebaseAnalytics.Event.SET_CHECKOUT_OPTION;
                            }
                        } else {
                            Logger.warning("Setting a CHECKOUT_OPTION now requires a custom flag of 'GA4.CommerceEventType'. Please review the mParticle documentation.  The event will be sent to Firebase with the deprecated SET_CHECKOUT_OPTION event type.");
                            eventName = FirebaseAnalytics.Event.SET_CHECKOUT_OPTION;
                        }
                    } else {
                        Logger.warning("Setting a CHECKOUT_OPTION now requires a custom flag of 'GA4.CommerceEventType'. Please review the mParticle documentation.  The event will be sent to Firebase with the deprecated SET_CHECKOUT_OPTION event type.");
                        eventName = FirebaseAnalytics.Event.SET_CHECKOUT_OPTION;
                    }

                    break;
                case Product.DETAIL:
                    eventName = FirebaseAnalytics.Event.VIEW_ITEM;
                    break;
                default:
                    return null;
            }
        instance.logEvent(eventName, bundle);
        } else {
            return null;
        }

        return Collections.singletonList(ReportingMessage.fromEvent(this, commerceEvent));
    }

    @Override
    public void onIdentifyCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest filteredIdentityApiRequest) {
        if (!forwardRequestsServerSide()) {
            setUserId(mParticleUser);
        }
    }

    @Override
    public void onLoginCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest filteredIdentityApiRequest) {
        if (!forwardRequestsServerSide()) {
            setUserId(mParticleUser);
        }
    }

    @Override
    public void onLogoutCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest filteredIdentityApiRequest) {
        if (!forwardRequestsServerSide()) {
            setUserId(mParticleUser);
        }
    }

    @Override
    public void onModifyCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest filteredIdentityApiRequest) {
        if (!forwardRequestsServerSide()) {
            setUserId(mParticleUser);
        }
    }

    @Override
    public void onUserIdentified(MParticleUser mParticleUser) {
        if (!forwardRequestsServerSide()) {
            setUserId(mParticleUser);
        }
    }

    private boolean forwardRequestsServerSide() {
        return "true".equalsIgnoreCase(getSettings().get(FORWARD_REQUESTS_SERVER_SIDE));
    }

    /**
     * Sets the GA4 Instance ID as an mParticle integration attribute.
     */
    private void updateInstanceIDIntegration() {
        FirebaseAnalytics instance = FirebaseAnalytics.getInstance(getContext());
        Task<String> task = instance.getAppInstanceId();
        task.addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String appInstanceID) {
                if (!KitUtils.isEmpty(appInstanceID)) {
                    HashMap<String, String> integrationAttributes = new HashMap<String, String>(1);
                    integrationAttributes.put(instanceIdIntegrationKey, appInstanceID);
                    setIntegrationAttributes(integrationAttributes);
                }
            }
        });
    }

    private void setUserId(MParticleUser user) {
        String userId = null;
        if (user != null) {
            if (EXTERNAL_USER_IDENTITY_CUSTOMER_ID.equalsIgnoreCase(getSettings().get(EXTERNAL_USER_IDENTITY_TYPE))) {
                userId = user.getUserIdentities().get(MParticle.IdentityType.CustomerId);
            } else if (EXTERNAL_USER_IDENTITY_MPID.equalsIgnoreCase(getSettings().get(EXTERNAL_USER_IDENTITY_TYPE))) {
                userId = Long.toString(user.getId());
            } else if (EXTERNAL_USER_IDENTITY_OTHER.equalsIgnoreCase(getSettings().get(EXTERNAL_USER_IDENTITY_TYPE))) {
                userId = user.getUserIdentities().get(MParticle.IdentityType.Other);
            } else if (EXTERNAL_USER_IDENTITY_OTHER2.equalsIgnoreCase(getSettings().get(EXTERNAL_USER_IDENTITY_TYPE))) {
                userId = user.getUserIdentities().get(MParticle.IdentityType.Other2);
            } else if (EXTERNAL_USER_IDENTITY_OTHER3.equalsIgnoreCase(getSettings().get(EXTERNAL_USER_IDENTITY_TYPE))) {
                userId = user.getUserIdentities().get(MParticle.IdentityType.Other3);
            } else if (EXTERNAL_USER_IDENTITY_OTHER4.equalsIgnoreCase(getSettings().get(EXTERNAL_USER_IDENTITY_TYPE))) {
                userId = user.getUserIdentities().get(MParticle.IdentityType.Other4);
            } else if (EXTERNAL_USER_IDENTITY_OTHER5.equalsIgnoreCase(getSettings().get(EXTERNAL_USER_IDENTITY_TYPE))) {
                userId = user.getUserIdentities().get(MParticle.IdentityType.Other5);
            } else if (EXTERNAL_USER_IDENTITY_OTHER6.equalsIgnoreCase(getSettings().get(EXTERNAL_USER_IDENTITY_TYPE))) {
                userId = user.getUserIdentities().get(MParticle.IdentityType.Other6);
            } else if (EXTERNAL_USER_IDENTITY_OTHER7.equalsIgnoreCase(getSettings().get(EXTERNAL_USER_IDENTITY_TYPE))) {
                userId = user.getUserIdentities().get(MParticle.IdentityType.Other7);
            } else if (EXTERNAL_USER_IDENTITY_OTHER8.equalsIgnoreCase(getSettings().get(EXTERNAL_USER_IDENTITY_TYPE))) {
                userId = user.getUserIdentities().get(MParticle.IdentityType.Other8);
            } else if (EXTERNAL_USER_IDENTITY_OTHER9.equalsIgnoreCase(getSettings().get(EXTERNAL_USER_IDENTITY_TYPE))) {
                userId = user.getUserIdentities().get(MParticle.IdentityType.Other9);
            } else if (EXTERNAL_USER_IDENTITY_OTHER10.equalsIgnoreCase(getSettings().get(EXTERNAL_USER_IDENTITY_TYPE))) {
                userId = user.getUserIdentities().get(MParticle.IdentityType.Other10);
            }

            if (!KitUtils.isEmpty(userId)) {
                if ("true".equalsIgnoreCase(getSettings().get(SHOULD_HASH_USER_ID))) {
                    userId = KitUtils.hashFnv1a(userId.getBytes()).toString();
                }
                FirebaseAnalytics.getInstance(getContext()).setUserId(userId);
            }
        }
    }

    String getFirebaseEventName(MPEvent event) {
        switch (event.getEventType()) {
            case Search:
                return FirebaseAnalytics.Event.SEARCH;
        }
        if (event.isScreenEvent()) {
            return FirebaseAnalytics.Event.VIEW_ITEM;
        }
        return standardizeName(event.getEventName(), true);
    }

    Bundle toBundle(Map<String, String> map) {
        Bundle bundle = new Bundle();
        map = standardizeAttributes(map, true);
        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                bundle.putString(entry.getKey(), entry.getValue());
            }
        }
        return bundle;
    }

    PickyBundle getPromotionCommerceEventBundle(Promotion promotion) {
        PickyBundle pickyBundle = new PickyBundle();

        if (promotion == null) {
            return pickyBundle;
        }
        return new PickyBundle()
                .putString(FirebaseAnalytics.Param.PROMOTION_ID, promotion.getId())
                .putString(FirebaseAnalytics.Param.CREATIVE_NAME, promotion.getCreative())
                .putString(FirebaseAnalytics.Param.PROMOTION_NAME, promotion.getName())
                .putString(FirebaseAnalytics.Param.CREATIVE_SLOT, promotion.getPosition());
    }

    PickyBundle getImpressionCommerceEventBundle(String impressionKey, List<Product> products) {
        return new PickyBundle()
                .putString(FirebaseAnalytics.Param.ITEM_LIST_ID, impressionKey)
                .putString(FirebaseAnalytics.Param.ITEM_LIST_NAME, impressionKey)
                .putBundleList(FirebaseAnalytics.Param.ITEMS, getProductBundles(products));
    }

    PickyBundle getCommerceEventBundle(CommerceEvent commerceEvent) {
        PickyBundle pickyBundle = getTransactionAttributesBundle(commerceEvent);
        String currency = commerceEvent.getCurrency();
        if (currency == null) {
            Logger.info("Currency field required by Firebase was not set, defaulting to 'USD'");
            currency = "USD";
        }

        // Google Analytics 4 introduces 2 new event types - add_shipping_info and add_payment_info
        // each of these has an extra parameter that is optional
        Map<String, List<String>> customFlags = commerceEvent.getCustomFlags();
        if (customFlags != null && customFlags.containsKey(CF_GA4COMMERCE_EVENT_TYPE)) {
            List<String> commerceEventTypeList = customFlags.get(CF_GA4COMMERCE_EVENT_TYPE);
            if (commerceEventTypeList != null && commerceEventTypeList.size() > 0) {
                String commerceEventType = commerceEventTypeList.get(0);
                if (commerceEventType.equals(FirebaseAnalytics.Event.ADD_SHIPPING_INFO.toString())) {
                    List<String> shippingTier = customFlags.get(CF_GA4_SHIPPING_TIER);
                    if (shippingTier != null && shippingTier.size() > 0) {
                        pickyBundle.putString(FirebaseAnalytics.Param.SHIPPING_TIER, shippingTier.get(0));
                    }
                } else if (commerceEventType.equals(FirebaseAnalytics.Event.ADD_PAYMENT_INFO.toString())) {
                    List<String> paymentType = customFlags.get(CF_GA4_PAYMENT_TYPE);
                    if (paymentType != null && paymentType.size() > 0) {
                        pickyBundle.putString(FirebaseAnalytics.Param.PAYMENT_TYPE, paymentType.get(0));
                    }
                }
            }
        }
        return pickyBundle
                .putString(FirebaseAnalytics.Param.CURRENCY, currency)
                .putBundleList(FirebaseAnalytics.Param.ITEMS, getProductBundles(commerceEvent))
                .putString(FirebaseAnalytics.Event.SET_CHECKOUT_OPTION, commerceEvent.getCheckoutOptions())
                .putInt(FirebaseAnalytics.Event.CHECKOUT_PROGRESS, commerceEvent.getCheckoutStep());
    }

    Bundle[] getProductBundles(CommerceEvent commerceEvent) {
        List<Product> products = commerceEvent.getProducts();
        return getProductBundles(products);
    }

    Bundle[] getProductBundles(List<Product> products) {
        if (products != null) {
            Bundle[] bundles = new Bundle[products.size()];
            int i = 0;
            for (Product product: products) {
                PickyBundle bundle = getBundle(product);
                bundles[i] = bundle.getBundle();
                i++;
            }
            return bundles;
        }
        return new Bundle[0];
    }

    PickyBundle getTransactionAttributesBundle(CommerceEvent commerceEvent) {
        PickyBundle pickyBundle = new PickyBundle();
        TransactionAttributes transactionAttributes = commerceEvent.getTransactionAttributes();

        if (commerceEvent.getTransactionAttributes() == null) {
            return pickyBundle;
        }
        return pickyBundle
                .putString(FirebaseAnalytics.Param.TRANSACTION_ID, transactionAttributes.getId())
                .putDouble(FirebaseAnalytics.Param.VALUE, transactionAttributes.getRevenue())
                .putDouble(FirebaseAnalytics.Param.TAX, transactionAttributes.getTax())
                .putDouble(FirebaseAnalytics.Param.SHIPPING, transactionAttributes.getShipping())
                .putString(FirebaseAnalytics.Param.COUPON, transactionAttributes.getCouponCode());
    }

    PickyBundle getBundle(Product product) {
        return new PickyBundle()
                .putLong(FirebaseAnalytics.Param.QUANTITY, (long) product.getQuantity())
                .putString(FirebaseAnalytics.Param.ITEM_ID, product.getSku())
                .putString(FirebaseAnalytics.Param.ITEM_NAME, product.getName())
                .putString(FirebaseAnalytics.Param.ITEM_CATEGORY, product.getCategory())
                .putDouble(FirebaseAnalytics.Param.PRICE, product.getUnitPrice());
    }

    private Double getValue(CommerceEvent commerceEvent) {
        double value = 0;
        List<Product> products = commerceEvent.getProducts();
        if (products == null) {
            return null;
        }
        for (Product product: products) {
            value += product.getQuantity() * product.getUnitPrice();
        }
        return value;
    }

    @Override
    public void onIncrementUserAttribute(String key, int incrementedBy, String value, FilteredMParticleUser filteredMParticleUser) {
        FirebaseAnalytics.getInstance(getContext()).setUserProperty(standardizeName(key, false), value);
    }

    @Override
    public void onRemoveUserAttribute(String key, FilteredMParticleUser filteredMParticleUser) {
        FirebaseAnalytics.getInstance(getContext()).setUserProperty(standardizeName(key, false), null);
    }

    /**
     * We are going to ignore Lists here, since Firebase only supports String "user property" values
     */
    @Override
    public void onSetUserAttribute(String key, Object value, FilteredMParticleUser filteredMParticleUser) {
        if (value instanceof String) {
            FirebaseAnalytics.getInstance(getContext()).setUserProperty(standardizeName(key, false), standardizeValue((String)value, false));
        }
    }

    @Override
    public void onSetUserTag(String s, FilteredMParticleUser filteredMParticleUser) {

    }

    @Override
    public void onSetUserAttributeList(String s, List<String> list, FilteredMParticleUser filteredMParticleUser) {

    }

    @Override
    public void onSetAllUserAttributes(Map<String, String> userAttributes, Map<String, List<String>> userAttributeLists, FilteredMParticleUser filteredMParticleUser) {
        userAttributes = standardizeAttributes(userAttributes, false);
        for (Map.Entry<String, String> entry: userAttributes.entrySet()) {
            FirebaseAnalytics.getInstance(getContext()).setUserProperty(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public boolean supportsAttributeLists() {
        return false;
    }

    @Override
    public void onConsentStateUpdated(ConsentState consentState, ConsentState consentState1, FilteredMParticleUser filteredMParticleUser) {

    }

    Map<String, String> standardizeAttributes(Map<String, String> attributes, boolean event) {
        if (attributes == null) {
            return null;
        }
        Map<String, String> attributeCopy = new HashMap<>();
        for (Map.Entry<String, String> entry: attributes.entrySet()) {
            attributeCopy.put(standardizeName(entry.getKey(), event), standardizeValue(entry.getValue(), event));
        }
        return attributeCopy;
    }

    final String standardizeValue(String value, boolean event) {
        if (value == null) {
            return value;
        }
        if (event) {
            if (value.length() > eventValMaxLength) {
                value = value.substring(0, eventValMaxLength);
            }
        } else {
            if (value.length() > userAttributeValMaxLength) {
                value = value.substring(0, userAttributeValMaxLength);
            }
        }
        return value;
    }

    final String standardizeName(String name, boolean event) {
        if (name == null) {
            return null;
        }
        name = name.replaceAll("[^a-zA-Z0-9_\\s]", "");
        name = name.replaceAll("[\\s]+", "_");

        for(String forbiddenPrefix: forbiddenPrefixes) {
            if (name.startsWith(forbiddenPrefix)) {
                name = name.replaceFirst(forbiddenPrefix, "");
            }
        }

        while(name.length() > 0 && !Character.isLetter(name.toCharArray()[0])) {
            name = name.substring(1);
        }
        if (event) {
            if (name.length() > eventMaxLength) {
                name = name.substring(0, eventMaxLength);
            }
        } else {
            if (name.length() > userAttributeMaxLength) {
                name = name.substring(0, userAttributeMaxLength);
            }
        }
        return name;
    }

    class PickyBundle {
        private Bundle bundle = new Bundle();

        PickyBundle putString(String key, String value) {
            if (value != null) {
                bundle.putString(key, value);
            }
            return this;
        }

        PickyBundle putDouble(String key, Double value) {
            if (value != null) {
                bundle.putDouble(key, value);
            }
            return this;
        }

        PickyBundle putLong(String key, Long value) {
            if (value != null) {
                bundle.putLong(key, value);
            }
            return this;
        }

        PickyBundle putInt(String key, Integer value) {
            if (value != null) {
                bundle.putInt(key, value);
            }
            return this;
        }

        PickyBundle putBundleList(String key, Bundle[] value) {
            if (value != null) {
                bundle.putParcelableArray(key, value);
            }
            return this;
        }

        Bundle getBundle() {
            return bundle;
        }
    }
}
