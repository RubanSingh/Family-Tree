package dna.familytree;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.ArrayList;
import java.util.List;

import dna.familytree.util.AnalyticsUtil;
import dna.familytree.util.LoggerUtils;

public class ProductLayout extends LinearLayout {

    private final Activity activity;
    private BillingClient billingClient;
    private static final String PRODUCT_ID = "family_tree_premium";
    private List<ProductDetails> productDetails; // Product(s) found on Google Play server
    Runnable conclusion;
    private final String TAG = ProductLayout.class.getSimpleName();

    public ProductLayout(Context context, AttributeSet set) {
        super(context, set);
        activity = (Activity)context;
    }

    void initialize(Runnable conclusion) {
        addView(inflate(activity, R.layout.product_layout, null), this.getLayoutParams());
        this.conclusion = conclusion;
        // Purchase button
        findViewById(R.id.product_button).setOnClickListener(view -> {
            // Launches billing flow if client is connected, reconnecting if necessary.
            LoggerUtils.EventLog(TAG, "Connection state:" + billingClient.getConnectionState());
            int state = billingClient.getConnectionState();
            if (state == BillingClient.ConnectionState.CONNECTED) {
                launchFlow();
            } else if (state == BillingClient.ConnectionState.DISCONNECTED
                    || state == BillingClient.ConnectionState.CLOSED) {
                connectGoogleBilling();
            }
        });
        connectGoogleBilling();
    }

    /**
     * Creates the billing client and establishes connection with Google Play billing system.
     */
    private void connectGoogleBilling() {
        PurchasesUpdatedListener purchasesUpdatedListener = (billingResult, purchases) -> {
            LoggerUtils.EventLog(TAG, "On purchases updated:" + billingResult);
            LoggerUtils.EventLog(TAG, "Purchases:" + purchases);
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                // Product just purchased
                for (Purchase purchase : purchases) {
                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                        verifyPurchase(purchase);
                    }
                }
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                // In this case Purchases is null, so we have to trust Google without verifying the purchase on our server
                becomePremium();
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                // User clicks outside the Google Play dialog box
                LoggerUtils.EventLog(TAG, "User cancelled flow");
            } else {
                // User not signed in to Google Account
                LoggerUtils.EventLog(TAG, "Other error");
            }
        };
        // Creates client instance, necessary if already created and connection ended
        billingClient = BillingClient.newBuilder(getContext()).enablePendingPurchases()
                .setListener(purchasesUpdatedListener).build();
        // Starts connection
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                LoggerUtils.EventLog(TAG, "Billing setup finished:" + billingResult);
                // The BillingClient is ready
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    if (productDetails != null && !productDetails.isEmpty()) // The Premium product is already visible and we can buy it
                        launchFlow();
                    else // Query purchases from the past
                        queryHistory();
                } // BILLING_UNAVAILABLE, Debug Message: Google Play In-app Billing API version is less than 3
                else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                    activity.runOnUiThread(() -> findViewById(R.id.product_wheel).setVisibility(GONE));
                    AppUtils.toast(activity, R.string.update_playstore);
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection to Google Play
                LoggerUtils.EventLog(TAG, "Billing service disconnected");
                connectGoogleBilling();
            }
        });
    }

    /**
     * acknowledge purchase token
     */
    private void verifyPurchase(Purchase purchase) {
        AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken()).build();
        billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
            LoggerUtils.EventLog(TAG, "On acknowledge purchase response:" + billingResult);
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                becomePremium();
                if (activity instanceof BaseController)
                    AnalyticsUtil.logEventSortPurchaseCompleted(((BaseController)activity).getFirebaseAnalytics());
            } else {
                AppUtils.toast(activity, R.string.something_wrong);
            }
        });
    }

    /**
     * Checks if the product was already purchased to immediately activate Premium or make it available to buy.
     */
    private void queryHistory() {
        // Returns purchases details for currently owned items bought within the app
        QueryPurchasesParams queryPurchasesParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP).build();
        billingClient.queryPurchasesAsync(queryPurchasesParams, (billingResult, purchases) -> {
                    LoggerUtils.EventLog(TAG, "On query purchases:" + billingResult);
                    LoggerUtils.EventLog(TAG, "Purchases:" + purchases);
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        if (!purchases.isEmpty() && purchases.get(0).getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                            // Premium was already been bought
                            verifyPurchase(purchases.get(0));
                        } else {
                            queryProducts(); // To display the product and buy it
                        }
                    } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ERROR) {
                        // Handle the error response.
                        AppUtils.toast(activity, R.string.something_wrong);
                    } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.DEVELOPER_ERROR) {
                        // Handle the developer error response.
                    } else {
                        // Other error codes are available, but are never returned by the Appstore SDK.
                    }
                }
        );
    }

    /**
     * Gets product details from Google Play to display it on layout.
     */
    private void queryProducts() {
        // Old versions of Google Play app give the error FEATURE_NOT_SUPPORTED: billing client does not support ProductDetails
        BillingResult billingResult = billingClient.isFeatureSupported(BillingClient.FeatureType.PRODUCT_DETAILS);
        LoggerUtils.EventLog(TAG, "Is product details supported:" + billingResult);
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED) {
            AppUtils.toast(activity, R.string.update_playstore);
            return;
        }
        List<QueryProductDetailsParams.Product> products = new ArrayList<>();
        products.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build());
        QueryProductDetailsParams queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(products).build();
        billingClient.queryProductDetailsAsync(queryProductDetailsParams, (billingResult1, productDetailsList) -> {
                    LoggerUtils.EventLog(TAG, "On product details:" + billingResult1);
                    LoggerUtils.EventLog(TAG, "Product details list:" + productDetailsList);
                    int response = billingResult1.getResponseCode();
                    if (response == BillingClient.BillingResponseCode.OK && !productDetailsList.isEmpty()) {
                        // Displays product details on layout
                        ProductDetails details = productDetailsList.get(0);
                        activity.runOnUiThread(() -> {
                            ((TextView)findViewById(R.id.product_title)).setText(details.getName());
                            findViewById(R.id.product_wheel).setVisibility(GONE);
                            TextView description = findViewById(R.id.product_description);
                            description.setText(details.getDescription().replace("\n", "").replace(". ", ".\n"));
                            description.setVisibility(VISIBLE);
                            ((TextView)findViewById(R.id.product_price)).setText(
                                    details.getOneTimePurchaseOfferDetails().getFormattedPrice());
                            ((TextView)findViewById(R.id.product_lifetime)).setText(R.string.lifetime);
                            findViewById(R.id.product_offer).setVisibility(VISIBLE);
                            findViewById(R.id.product_button).setEnabled(true);
                        });
                    } else if (productDetailsList.isEmpty() || response == BillingClient.BillingResponseCode.ERROR
                            || response == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE) {
                        // Can't retrieve products, maybe for network error
                        activity.runOnUiThread(() -> findViewById(R.id.product_wheel).setVisibility(GONE));
                        AppUtils.toast(activity, R.string.something_wrong);
                    }
                    productDetails = productDetailsList;
                }
        );
    }

    /**
     * The final goal of the entire purchase process.
     * Can be called after the purchase flow or because the Premium product results already been bought on Google Play.
     */
    private void becomePremium() {
        Global.settings.premium = true;
        Global.settings.save();
        conclusion.run();
        AppUtils.toast(R.string.premium_activated);
    }

    /**
     * Displays the Google Play dialog to purchase the product.
     */
    void launchFlow() {
        LoggerUtils.EventLog(TAG, "Product details:" + productDetails);
        if (productDetails == null || productDetails.isEmpty()) return;
        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
        for (ProductDetails details : productDetails) {
            productDetailsParamsList.add(BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details).build());
        }
        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();
        // Launches the billing flow
        BillingResult billingResult = billingClient.launchBillingFlow(activity, billingFlowParams);
        LoggerUtils.EventLog(TAG, "Launch billing flow:" + billingResult);
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            // User not signed in to Google Account ends here
            // Also if product is already owned ends here
            LoggerUtils.EventLog(TAG, "Response OK");
        } else {
            // Never seen
            LoggerUtils.EventLog(TAG, "Response KO");
        }
    }
}
