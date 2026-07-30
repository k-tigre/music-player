# Task 3 Report — PlayEntitlementsRepository + cache

**Status:** DONE
**Branch:** `feature/play-billing-entitlements`
**Date:** 2026-07-30

## Delivered

- Added pure `AppSku` and `resolveTier(productIds, app)`, including test coverage for Pro, Plus, both subscriptions, and products from the other app.
- Added `PlayEntitlementsRepository` for Android. It initializes from a cached tier, refreshes from active Play purchases, acknowledges unacknowledged subscriptions, writes the resolved tier immediately, and retains the cached tier if a thrown Billing operation fails.
- Added `EntitlementsCache` backed by an application-context `SharedPreferences` file with a Free fallback.
- Added `EntitlementsRemoteConfig`, which installs `EntitlementLimits.remoteConfigDefaults`, fetches and activates for up to 10 seconds, and uses hardcoded limits if a Remote Config value is absent, invalid, or unavailable.
- Added `FreeEntitlementsRepository` for desktop, which exposes Free tier and the hardcoded limits.
- Added Android-only Billing and Firebase Remote Config dependencies to `core:platform:entitlements`; Firebase remains outside `commonMain`.

## Verification

```text
.\gradlew.bat :core:platform:entitlements:desktopTest
BUILD SUCCESSFUL

.\gradlew.bat :core:platform:entitlements:compileAndroidMain
BUILD SUCCESSFUL
```

IDE diagnostics for `core/platform/entitlements`: no errors.

## Scope

- No application graph, UI, paywall, or feature-gate wiring was changed.
- No unrelated generated, analytics, or prior-task files were staged.

## Concern

`BillingService.queryActivePurchases()` currently returns an empty list for both a genuine no-purchase state and an Android Billing connection failure. The repository preserves the cache for thrown failures, but cannot distinguish that empty failure result from a valid Free refresh without a future BillingService result/status API change.
