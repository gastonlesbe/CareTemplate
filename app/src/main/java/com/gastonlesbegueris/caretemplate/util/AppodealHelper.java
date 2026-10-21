package com.gastonlesbegueris.caretemplate.util;

import android.app.Activity;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.appodeal.ads.Appodeal;
import com.appodeal.ads.BannerView;
import com.appodeal.ads.BannerCallbacks;
import com.appodeal.ads.InterstitialCallbacks;

/**
 * Helper class for Appodeal banner ads integration
 * Replaces AdMob banner functionality
 */
public class AppodealHelper {
    private static final String TAG = "AppodealHelper";
    private static boolean isInitialized = false;
    private static String initializedAppKey = null;

    /**
     * Initialize Appodeal SDK with app key
     * Call this once in Application class or MainActivity onCreate
     */
    public static void initialize(Activity activity, String appKey) {
        // Allow re-initialization if app key changed (for different flavors)
        if (isInitialized && initializedAppKey != null && initializedAppKey.equals(appKey)) {
            Log.d(TAG, "Appodeal already initialized with same app key");
            return;
        }
        
        // If initialized with different app key, reset and re-initialize
        if (isInitialized && initializedAppKey != null && !initializedAppKey.equals(appKey)) {
            Log.d(TAG, "Appodeal initialized with different app key, re-initializing...");
            isInitialized = false;
            initializedAppKey = null;
        }

        try {
            Log.d(TAG, "Initializing Appodeal with app key: " + (appKey != null ? appKey.substring(0, Math.min(10, appKey.length())) + "..." : "null"));
            
            // Real ads in release; test mode in debug (appodeal_testing resValue in the
            // debug buildType) so dev/emulator runs never serve or count live ads.
            try {
                boolean testing = activity.getResources().getBoolean(com.gastonlesbegueris.caretemplate.R.bool.appodeal_testing);
                Appodeal.setTesting(testing);
                Log.d(TAG, "Appodeal test mode " + (testing ? "enabled" : "disabled"));
            } catch (Exception e) {
                Log.w(TAG, "Could not set Appodeal test mode", e);
            }
            
            // Set banner callbacks before initialization
            Appodeal.setBannerCallbacks(new BannerCallbacks() {
                @Override
                public void onBannerLoaded(int height, boolean isPrecache) {
                    Log.d(TAG, "Banner loaded successfully, height: " + height + ", isPrecache: " + isPrecache);
                    // Try to show banner when it's loaded
                    try {
                        String packageName = activity.getPackageName();
                        Log.d(TAG, "Attempting to show banner for package: " + packageName);
                        Appodeal.show(activity, Appodeal.BANNER);
                    } catch (Exception e) {
                        Log.e(TAG, "Error showing banner after load", e);
                    }
                }

                @Override
                public void onBannerFailedToLoad() {
                    Log.w(TAG, "Banner failed to load - Possible causes:");
                    Log.w(TAG, "  1. App key not registered for this package name in Appodeal dashboard");
                    Log.w(TAG, "  2. App not active/approved in Appodeal");
                    Log.w(TAG, "  3. No ads available for this app");
                    Log.w(TAG, "  Package name: " + activity.getPackageName());
                    // Try to cache banner again after a delay
                    activity.getWindow().getDecorView().postDelayed(() -> {
                        try {
                            Log.d(TAG, "Retrying to cache banner...");
                            Appodeal.cache(activity, Appodeal.BANNER);
                        } catch (Exception e) {
                            Log.e(TAG, "Error retrying banner cache", e);
                        }
                    }, 5000);
                }

                @Override
                public void onBannerShown() {
                    Log.d(TAG, "Banner shown");
                }

                @Override
                public void onBannerShowFailed() {
                    Log.w(TAG, "Banner show failed");
                }

                @Override
                public void onBannerClicked() {
                    Log.d(TAG, "Banner clicked");
                }

                @Override
                public void onBannerExpired() {
                    Log.d(TAG, "Banner expired");
                }
            });
            
            // Initialize Appodeal only for banners (interstitials are handled by AdMob)
            Appodeal.initialize(activity, appKey, Appodeal.BANNER);
            isInitialized = true;
            initializedAppKey = appKey;
            Log.d(TAG, "Appodeal initialized successfully with app key: " + (appKey != null ? appKey.substring(0, Math.min(10, appKey.length())) + "..." : "null"));
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Appodeal", e);
        }
    }

    /**
     * Show banner ad in the specified container
     * @param activity The activity where banner will be shown
     * @param containerId The ID of the container ViewGroup (e.g., FrameLayout)
     */
    public static void showBanner(Activity activity, int containerId) {
        if (!isInitialized) {
            Log.w(TAG, "Appodeal not initialized. Call initialize() first.");
            Log.w(TAG, "Package name: " + activity.getPackageName());
            return;
        }

        try {
            ViewGroup container = activity.findViewById(containerId);
            if (container == null) {
                Log.e(TAG, "Container not found with ID: " + containerId);
                Log.e(TAG, "Package name: " + activity.getPackageName());
                return;
            }

            Log.d(TAG, "Attempting to show banner in container ID: " + containerId);
            Log.d(TAG, "Package name: " + activity.getPackageName());
            Log.d(TAG, "App key: " + (initializedAppKey != null ? initializedAppKey.substring(0, Math.min(10, initializedAppKey.length())) + "..." : "null"));
            
            // Remove any existing banner
            removeBanner(container);

            // Check if banner is loaded
            boolean isLoaded = Appodeal.isLoaded(Appodeal.BANNER);
            Log.d(TAG, "Banner loaded status: " + isLoaded);

            // Always cache banner first (similar to Pets implementation)
            Log.d(TAG, "Caching banner...");
            Appodeal.cache(activity, Appodeal.BANNER);

            // Try to get banner view and add to container
            BannerView bannerView = Appodeal.getBannerView(activity);
            if (bannerView != null) {
                Log.d(TAG, "Banner view obtained, adding to container");
                container.addView(bannerView);
                // Show banner - Appodeal will display it when ready
                Appodeal.show(activity, Appodeal.BANNER);
                Log.d(TAG, "Banner show() called");
            } else {
                Log.w(TAG, "Banner view is null, calling show() anyway (Appodeal will load automatically)");
                // Even if bannerView is null, Appodeal.show() will load and show the banner automatically
                Appodeal.show(activity, Appodeal.BANNER);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing banner", e);
        }
    }

    /**
     * Remove banner from container
     */
    private static void removeBanner(ViewGroup container) {
        for (int i = 0; i < container.getChildCount(); i++) {
            if (container.getChildAt(i) instanceof BannerView) {
                container.removeViewAt(i);
                break;
            }
        }
    }

    /**
     * Hide banner ad
     */
    public static void hideBanner(Activity activity) {
        try {
            Appodeal.hide(activity, Appodeal.BANNER);
        } catch (Exception e) {
            Log.e(TAG, "Error hiding banner", e);
        }
    }

    /**
     * Show interstitial ad with callback
     * @param activity The activity where interstitial will be shown
     * @param onAdClosed Runnable to execute when ad is closed
     */
    public static void showInterstitial(Activity activity, Runnable onAdClosed) {
        if (!isInitialized) {
            Log.w(TAG, "Appodeal not initialized");
            if (onAdClosed != null) {
                onAdClosed.run();
            }
            return;
        }

        try {
            if (Appodeal.isLoaded(Appodeal.INTERSTITIAL)) {
                // Set callback for when ad is closed
                Appodeal.setInterstitialCallbacks(new InterstitialCallbacks() {
                    @Override
                    public void onInterstitialLoaded(boolean isPrecache) {
                        Log.d(TAG, "Interstitial loaded");
                    }

                    @Override
                    public void onInterstitialFailedToLoad() {
                        Log.d(TAG, "Interstitial failed to load");
                        if (onAdClosed != null) {
                            // Asegurar que el callback se ejecute en el hilo principal
                            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                                activity.runOnUiThread(() -> {
                                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                                        onAdClosed.run();
                                    }
                                });
                            } else {
                            onAdClosed.run();
                            }
                        }
                    }

                    @Override
                    public void onInterstitialShown() {
                        Log.d(TAG, "Interstitial shown");
                    }

                    @Override
                    public void onInterstitialClosed() {
                        Log.d(TAG, "Interstitial closed");
                        if (onAdClosed != null) {
                            // Asegurar que el callback se ejecute en el hilo principal
                            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                                activity.runOnUiThread(() -> {
                                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                                        onAdClosed.run();
                                    }
                                });
                            } else {
                                // Si la actividad ya no está disponible, ejecutar directamente
                            onAdClosed.run();
                            }
                        }
                    }

                    @Override
                    public void onInterstitialClicked() {
                        Log.d(TAG, "Interstitial clicked");
                    }

                    @Override
                    public void onInterstitialExpired() {
                        Log.d(TAG, "Interstitial expired");
                    }

                    @Override
                    public void onInterstitialShowFailed() {
                        Log.d(TAG, "Interstitial show failed");
                        if (onAdClosed != null) {
                            // Asegurar que el callback se ejecute en el hilo principal
                            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                                activity.runOnUiThread(() -> {
                                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                                        onAdClosed.run();
                                    }
                                });
                            } else {
                            onAdClosed.run();
                            }
                        }
                    }
                });
                
                Appodeal.show(activity, Appodeal.INTERSTITIAL);
            } else {
                Log.d(TAG, "Interstitial ad not loaded yet, executing callback");
                if (onAdClosed != null) {
                    // Asegurar que el callback se ejecute en el hilo principal
                    if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                        activity.runOnUiThread(() -> {
                            if (!activity.isFinishing() && !activity.isDestroyed()) {
                                onAdClosed.run();
                            }
                        });
                    } else {
                    onAdClosed.run();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing interstitial", e);
            if (onAdClosed != null) {
                // Asegurar que el callback se ejecute en el hilo principal
                if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                    activity.runOnUiThread(() -> {
                        if (!activity.isFinishing() && !activity.isDestroyed()) {
                            onAdClosed.run();
                        }
                    });
                } else {
                onAdClosed.run();
                }
            }
        }
    }

    /**
     * Show interstitial ad (simple version without callback)
     */
    public static void showInterstitial(Activity activity) {
        showInterstitial(activity, null);
    }

    /**
     * Check if interstitial is loaded
     */
    public static boolean isInterstitialLoaded() {
        return isInitialized && Appodeal.isLoaded(Appodeal.INTERSTITIAL);
    }
}
