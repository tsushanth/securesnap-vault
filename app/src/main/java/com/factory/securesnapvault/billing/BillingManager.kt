package com.factory.securesnapvault.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BillingManager(
    context: Context,
    private val premiumManager: PremiumManager
) {
    companion object {
        private const val TAG = "BillingManager"

        // Subscription product IDs
        const val PRODUCT_WEEKLY = "com.factory.securesnapvault.subscription.weekly"
        const val PRODUCT_MONTHLY = "com.factory.securesnapvault.subscription.monthly"
        const val PRODUCT_YEARLY = "com.factory.securesnapvault.subscription.yearly"
        const val PRODUCT_LIFETIME = "com.factory.securesnapvault.subscription.lifetime"

        // In-app purchase product IDs
        const val PRODUCT_SMALL_IAP = "com.factory.securesnapvault.small_iap"

        val SUBSCRIPTION_PRODUCTS = listOf(
            PRODUCT_WEEKLY, PRODUCT_MONTHLY, PRODUCT_YEARLY, PRODUCT_LIFETIME
        )

        val IAP_PRODUCTS = listOf(PRODUCT_SMALL_IAP)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _productDetails = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetails: StateFlow<Map<String, ProductDetails>> = _productDetails.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        scope.launch {
            handlePurchaseResult(billingResult, purchases)
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    init {
        connect()
    }

    fun connect() {
        if (billingClient.isReady) {
            _isConnected.value = true
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isConnected.value = true
                    scope.launch {
                        queryProductDetails()
                        queryExistingPurchases()
                    }
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                    _isConnected.value = false
                }
            }

            override fun onBillingServiceDisconnected() {
                _isConnected.value = false
                Log.w(TAG, "Billing service disconnected, will retry on next operation")
            }
        })
    }

    private suspend fun ensureConnected(): Boolean {
        if (billingClient.isReady) return true

        return suspendCancellableCoroutine { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    val connected = billingResult.responseCode == BillingClient.BillingResponseCode.OK
                    _isConnected.value = connected
                    if (continuation.isActive) {
                        continuation.resume(connected)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _isConnected.value = false
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            })
        }
    }

    private suspend fun queryProductDetails() {
        val allDetails = mutableMapOf<String, ProductDetails>()

        // Query subscriptions
        val subProducts = SUBSCRIPTION_PRODUCTS.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val subParams = QueryProductDetailsParams.newBuilder()
            .setProductList(subProducts)
            .build()

        val subDetailsList = suspendCancellableCoroutine<List<ProductDetails>> { continuation ->
            billingClient.queryProductDetailsAsync(subParams) { billingResult: BillingResult, productDetailsList: List<ProductDetails> ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(productDetailsList)
                } else {
                    continuation.resume(emptyList())
                }
            }
        }
        subDetailsList.forEach { details ->
            allDetails[details.productId] = details
        }

        // Query in-app purchases
        val iapProducts = IAP_PRODUCTS.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val iapParams = QueryProductDetailsParams.newBuilder()
            .setProductList(iapProducts)
            .build()

        val iapDetailsList = suspendCancellableCoroutine<List<ProductDetails>> { continuation ->
            billingClient.queryProductDetailsAsync(iapParams) { billingResult: BillingResult, productDetailsList: List<ProductDetails> ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(productDetailsList)
                } else {
                    continuation.resume(emptyList())
                }
            }
        }
        iapDetailsList.forEach { details ->
            allDetails[details.productId] = details
        }

        _productDetails.value = allDetails
        Log.d(TAG, "Loaded ${allDetails.size} product details")
    }

    suspend fun queryExistingPurchases() {
        if (!ensureConnected()) return

        // Check subscriptions
        val subParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val subPurchases = suspendCancellableCoroutine<List<Purchase>> { continuation ->
            billingClient.queryPurchasesAsync(subParams) { billingResult: BillingResult, purchasesList: List<Purchase> ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(purchasesList)
                } else {
                    continuation.resume(emptyList())
                }
            }
        }

        val hasActiveSub = subPurchases.any { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        if (hasActiveSub) {
            premiumManager.setPremium(true)
            subPurchases.forEach { purchase ->
                if (!purchase.isAcknowledged && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    acknowledgePurchase(purchase)
                }
            }
            return
        }

        // Check in-app purchases (lifetime)
        val iapParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val iapPurchases = suspendCancellableCoroutine<List<Purchase>> { continuation ->
            billingClient.queryPurchasesAsync(iapParams) { billingResult: BillingResult, purchasesList: List<Purchase> ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(purchasesList)
                } else {
                    continuation.resume(emptyList())
                }
            }
        }

        val hasLifetime = iapPurchases.any { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                purchase.products.contains(PRODUCT_LIFETIME)
        }
        if (hasLifetime) {
            premiumManager.setPremium(true)
            iapPurchases.forEach { purchase ->
                if (!purchase.isAcknowledged && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    acknowledgePurchase(purchase)
                }
            }
            return
        }

        // No active purchases found
        premiumManager.setPremium(false)
    }

    fun launchPurchaseFlow(activity: Activity, productId: String) {
        val details = _productDetails.value[productId]
        if (details == null) {
            _purchaseState.value = PurchaseState.Error("Product not available. Please try again.")
            return
        }

        val productDetailsParamsList = if (details.productType == BillingClient.ProductType.SUBS) {
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken == null) {
                _purchaseState.value = PurchaseState.Error("Subscription offer not available.")
                return
            }
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .setOfferToken(offerToken)
                    .build()
            )
        } else {
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .build()
            )
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        _purchaseState.value = PurchaseState.Loading
        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _purchaseState.value = PurchaseState.Error("Failed to launch purchase: ${result.debugMessage}")
        }
    }

    private suspend fun handlePurchaseResult(
        billingResult: BillingResult,
        purchases: List<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    when (purchase.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> {
                            acknowledgePurchase(purchase)
                            premiumManager.setPremium(true)
                            _purchaseState.value = PurchaseState.Success
                        }
                        Purchase.PurchaseState.PENDING -> {
                            _purchaseState.value = PurchaseState.Pending
                        }
                        else -> {
                            _purchaseState.value = PurchaseState.Error("Purchase failed")
                        }
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseState.value = PurchaseState.Cancelled
            }
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
                _purchaseState.value = PurchaseState.Error("Network error. Please check your connection and try again.")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                premiumManager.setPremium(true)
                _purchaseState.value = PurchaseState.Success
            }
            else -> {
                _purchaseState.value = PurchaseState.Error(
                    "Purchase failed (code: ${billingResult.responseCode}). Please try again."
                )
            }
        }
    }

    private suspend fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) return

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        suspendCancellableCoroutine<Unit> { continuation ->
            billingClient.acknowledgePurchase(params) { billingResult: BillingResult ->
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
                }
                continuation.resume(Unit)
            }
        }
    }

    suspend fun restorePurchases(): Boolean {
        if (!ensureConnected()) {
            _purchaseState.value = PurchaseState.Error("Cannot connect to Google Play. Check your connection.")
            return false
        }

        queryExistingPurchases()
        return premiumManager.isPremiumSync()
    }

    fun resetPurchaseState() {
        _purchaseState.value = PurchaseState.Idle
    }

    fun destroy() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    fun getFormattedPrice(productId: String): String {
        val details = _productDetails.value[productId] ?: return getFallbackPrice(productId)

        return if (details.productType == BillingClient.ProductType.SUBS) {
            details.subscriptionOfferDetails?.firstOrNull()
                ?.pricingPhases?.pricingPhaseList?.firstOrNull()
                ?.formattedPrice ?: getFallbackPrice(productId)
        } else {
            details.oneTimePurchaseOfferDetails?.formattedPrice ?: getFallbackPrice(productId)
        }
    }

    private fun getFallbackPrice(productId: String): String {
        return when (productId) {
            PRODUCT_WEEKLY -> "$4.79"
            PRODUCT_MONTHLY -> "$11.99"
            PRODUCT_YEARLY -> "$17.60"
            PRODUCT_LIFETIME -> "$79.99"
            PRODUCT_SMALL_IAP -> "$0.99"
            else -> ""
        }
    }
}

sealed class PurchaseState {
    data object Idle : PurchaseState()
    data object Loading : PurchaseState()
    data object Success : PurchaseState()
    data object Pending : PurchaseState()
    data object Cancelled : PurchaseState()
    data class Error(val message: String) : PurchaseState()
}
