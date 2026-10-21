# Migration Guide: AdMob to Appodeal

## Overview
This guide explains how to replace AdMob banners with Appodeal banners in the app.

## What Needs to Change

### 1. Dependencies (build.gradle)
- Remove: `com.google.android.gms:play-services-ads`
- Add: Appodeal SDK dependency
- Remove: `com.google.gms.google-services` plugin (if only used for AdMob)

### 2. Activities to Update
- MainActivity
- SubjectListActivity
- ExpensesActivity
- AgendaActivity
- AgendaMonthActivity
- SubjectHistoryActivity

### 3. Layout Files
- Replace `com.google.android.gms.ads.AdView` with Appodeal banner view
- Or use FrameLayout and add banner programmatically

### 4. String Resources
- Replace `admob_banner_id` with `appodeal_app_key`
- Add Appodeal app key for each flavor

### 5. AndroidManifest
- Remove AdMob permissions if not needed
- Add Appodeal permissions if required

## Appodeal Setup Requirements

1. **Appodeal Account**: Create account at https://appodeal.com
2. **App Key**: Get your Appodeal app key from dashboard
3. **SDK Integration**: Add Appodeal SDK to build.gradle

## Benefits of Appodeal

- **Mediation**: Aggregates multiple ad networks (AdMob, Facebook, Unity, etc.)
- **Higher eCPM**: Often provides better revenue than single network
- **Automatic Optimization**: Chooses best performing ads
- **Multiple Ad Formats**: Banner, Interstitial, Rewarded, Native

## Next Steps

After migration:
1. Test banner display in all activities
2. Test interstitial ads (if using)
3. Monitor revenue in Appodeal dashboard
4. Compare performance with previous AdMob setup

