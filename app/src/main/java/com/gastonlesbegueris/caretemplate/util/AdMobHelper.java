package com.gastonlesbegueris.caretemplate.util;

import android.app.Activity;
import android.util.Log;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

/**
 * Helper class for AdMob interstitial ads integration
 */
public class AdMobHelper {
    private static final String TAG = "AdMobHelper";
    private static InterstitialAd interstitialAd = null;
    private static String currentInterstitialId = null;

    /**
     * Load an interstitial ad
     * @param activity The activity context
     * @param adUnitId The AdMob interstitial ad unit ID
     */
    public static void loadInterstitial(Activity activity, String adUnitId) {
        if (activity == null || adUnitId == null || adUnitId.isEmpty()) {
            Log.w(TAG, "Cannot load interstitial: activity or adUnitId is null");
            return;
        }

        // If already loaded with the same ad unit ID, don't reload
        if (interstitialAd != null && adUnitId.equals(currentInterstitialId)) {
            Log.d(TAG, "Interstitial already loaded with same ad unit ID");
            return;
        }

        Log.d(TAG, "Loading interstitial ad with ID: " + adUnitId);
        
        AdRequest adRequest = new AdRequest.Builder().build();
        
        InterstitialAd.load(activity, adUnitId, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(InterstitialAd ad) {
                Log.d(TAG, "Interstitial ad loaded successfully for ID: " + adUnitId);
                interstitialAd = ad;
                currentInterstitialId = adUnitId;
                
                // Set callback for when ad is shown/closed
                ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Interstitial ad dismissed");
                        interstitialAd = null;
                        currentInterstitialId = null;
                        // Reload for next time
                        loadInterstitial(activity, adUnitId);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) {
                        Log.e(TAG, "Interstitial ad failed to show: " + adError.getMessage());
                        interstitialAd = null;
                        currentInterstitialId = null;
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        Log.d(TAG, "Interstitial ad showed");
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                Log.e(TAG, "Interstitial ad failed to load for ID: " + adUnitId);
                Log.e(TAG, "Error code: " + loadAdError.getCode() + ", Message: " + loadAdError.getMessage());
                if (loadAdError.getCause() != null) {
                    Log.e(TAG, "Cause: " + loadAdError.getCause().getMessage());
                }
                interstitialAd = null;
                currentInterstitialId = null;
            }
        });
    }

    /**
     * Show interstitial ad with callback
     * @param activity The activity where interstitial will be shown
     * @param adUnitId The AdMob interstitial ad unit ID
     * @param onAdClosed Runnable to execute when ad is closed (or if ad is not available)
     */
    public static void showInterstitial(Activity activity, String adUnitId, Runnable onAdClosed) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.w(TAG, "Cannot show interstitial: activity is null or finishing");
            if (onAdClosed != null) {
                onAdClosed.run();
            }
            return;
        }

        // Load ad if not already loaded or if ad unit ID changed
        if (interstitialAd == null || !adUnitId.equals(currentInterstitialId)) {
            Log.d(TAG, "Interstitial not loaded, loading now with ID: " + adUnitId);
            loadInterstitial(activity, adUnitId);
            
            // Set callback to show ad when loaded
            final Runnable finalCallback = onAdClosed;
            final Activity finalActivity = activity;
            final String finalAdUnitId = adUnitId;
            
            // Try multiple times with increasing delays
            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            
            // First attempt after 2 seconds
            handler.postDelayed(() -> {
                if (interstitialAd != null && finalAdUnitId.equals(currentInterstitialId)) {
                    Log.d(TAG, "Interstitial loaded after 2 seconds, showing now");
                    showInterstitialNow(finalActivity, finalCallback);
                } else {
                    Log.d(TAG, "Interstitial not ready after 2 seconds, waiting more...");
                    // Second attempt after 3 more seconds (total 5 seconds)
                    handler.postDelayed(() -> {
                        if (interstitialAd != null && finalAdUnitId.equals(currentInterstitialId)) {
                            Log.d(TAG, "Interstitial loaded after 5 seconds, showing now");
                            showInterstitialNow(finalActivity, finalCallback);
                        } else {
                            Log.w(TAG, "Interstitial not loaded after 5 seconds, executing callback without ad");
                            if (finalCallback != null) {
                                finalCallback.run();
                            }
                        }
                    }, 3000);
                }
            }, 2000); // Wait 2 seconds for first attempt
            return;
        }

        showInterstitialNow(activity, onAdClosed);
    }

    /**
     * Show the currently loaded interstitial ad
     */
    private static void showInterstitialNow(Activity activity, Runnable onAdClosed) {
        if (interstitialAd == null) {
            Log.w(TAG, "Interstitial ad is null, executing callback");
            if (onAdClosed != null) {
                onAdClosed.run();
            }
            return;
        }

        try {
            // Set callback for when ad is closed
            interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed");
                    interstitialAd = null;
                    currentInterstitialId = null;
                    if (onAdClosed != null) {
                        onAdClosed.run();
                    }
                }

                @Override
                public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) {
                    Log.e(TAG, "Interstitial ad failed to show: " + adError.getMessage());
                    interstitialAd = null;
                    currentInterstitialId = null;
                    if (onAdClosed != null) {
                        onAdClosed.run();
                    }
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad showed");
                }
            });

            interstitialAd.show(activity);
        } catch (Exception e) {
            Log.e(TAG, "Error showing interstitial", e);
            interstitialAd = null;
            currentInterstitialId = null;
            if (onAdClosed != null) {
                onAdClosed.run();
            }
        }
    }

    /**
     * Check if interstitial is loaded
     */
    public static boolean isInterstitialLoaded() {
        return interstitialAd != null;
    }
}

