package com.ads.narayan.admob;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowMetrics;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.ads.narayan.R;
import com.ads.narayan.ads.nativeAds.AdmobRecyclerAdapter;
import com.ads.narayan.ads.nativeAds.NarayanAdPlacer;
import com.ads.narayan.ads.nativeAds.NarayanAdPlacerSettings;
import com.ads.narayan.billing.AppPurchase;
import com.ads.narayan.dialog.PrepareLoadingAdsDialog;
import com.ads.narayan.event.NarayanLogEventManager;
import com.ads.narayan.funtion.AdCallback;
import com.ads.narayan.funtion.AdType;
import com.ads.narayan.funtion.AdmodHelper;
import com.ads.narayan.funtion.RewardCallback;
import com.ads.narayan.util.AppUtil;
import com.ads.narayan.util.SharePreferenceUtils;
import com.applovin.mediation.AppLovinExtras;
import com.applovin.mediation.ApplovinAdapter;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.VideoOptions;
//import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdOptions;

import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import com.jirbo.adcolony.AdColonyAdapter;
import com.jirbo.adcolony.AdColonyBundleBuilder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Admob {
    private static final String TAG = "NarayanAdmob";
    private static Admob instance;
    private int currentClicked = 0;
    private String nativeId;
    private int numShowAds = 3;

    private int maxClickAds = 100;
    private Handler handlerTimeout;
    private Runnable rdTimeout;
    private Handler handlerTimeoutHighFloor;
    private Runnable rdTimeoutHighFloor;
    private PrepareLoadingAdsDialog dialog;
    private boolean isTimeout;
    private boolean isTimeoutHighFloor;
    private boolean disableAdResumeWhenClickAds = false;
    private boolean isShowLoadingSplash = false;
    private boolean isFan;
    private boolean isAdcolony;
    private boolean isAppLovin;
    boolean isTimeDelay = false;
    boolean isTimeDelayHighFloor = false;
    private boolean openActivityAfterShowInterAds = false;
    private Context context;

    public static final String BANNER_INLINE_SMALL_STYLE = "BANNER_INLINE_SMALL_STYLE";
    public static final String BANNER_INLINE_LARGE_STYLE = "BANNER_INLINE_LARGE_STYLE";
//    private final int MAX_SMALL_INLINE_BANNER_HEIGHT = 50;
    private final int MAX_SMALL_INLINE_BANNER_HEIGHT = 150;

    InterstitialAd mInterstitialSplash;
    InterstitialAd mInterstitialSplashHighFloor;
    InterstitialAd interstitialAd;
    private boolean isShowInterstitialSplashHighFloor = false;

    public void setFan(boolean fan) {
        isFan = fan;
    }

    public void setColony(boolean adcolony) {
        isAdcolony = adcolony;
    }

    public void setAppLovin(boolean appLovin) {
        isAppLovin = appLovin;

    }


    public void setMaxClickAdsPerDay(int maxClickAds) {
        this.maxClickAds = maxClickAds;
    }


    public static Admob getInstance() {
        if (instance == null) {
            instance = new Admob();
            instance.isShowLoadingSplash = false;
        }
        return instance;
    }

    private Admob() {

    }

    public void setNumToShowAds(int numShowAds) {
        this.numShowAds = numShowAds;
    }

    public void setNumToShowAds(int numShowAds, int currentClicked) {
        this.numShowAds = numShowAds;
        this.currentClicked = currentClicked;
    }


    public void setDisableAdResumeWhenClickAds(boolean disableAdResumeWhenClickAds) {
        this.disableAdResumeWhenClickAds = disableAdResumeWhenClickAds;
    }


    public void init(Context context, List<String> testDeviceList) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String processName = Application.getProcessName();
            String packageName = context.getPackageName();
            if (!packageName.equals(processName)) {
                WebView.setDataDirectorySuffix(processName);
            }
        }
        MobileAds.initialize(context, initializationStatus -> {
            Map<String, AdapterStatus> statusMap = initializationStatus.getAdapterStatusMap();
            for (String adapterClass : statusMap.keySet()) {
                AdapterStatus status = statusMap.get(adapterClass);
                Log.e(TAG, String.format("Adapter name: %s, Description: %s, Latency: %d",
                        adapterClass, status.getDescription(), status.getLatency()));
            }
        });
        MobileAds.setRequestConfiguration(new RequestConfiguration.Builder().setTestDeviceIds(testDeviceList).build());

        this.context = context;
    }

    public void init(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String processName = Application.getProcessName();
            String packageName = context.getPackageName();
            if (!packageName.equals(processName)) {
                WebView.setDataDirectorySuffix(processName);
            }
        }

        MobileAds.initialize(context, initializationStatus -> {
            Map<String, AdapterStatus> statusMap = initializationStatus.getAdapterStatusMap();
            for (String adapterClass : statusMap.keySet()) {
                AdapterStatus status = statusMap.get(adapterClass);
                Log.e(TAG, String.format("Adapter name: %s, Description: %s, Latency: %d",
                        adapterClass, status.getDescription(), status.getLatency()));
            }
        });
        this.context = context;
    }


    public boolean isShowLoadingSplash() {
        return isShowLoadingSplash;
    }

    private String getProcessName(Context context) {
        if (context == null) return null;
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
            if (processInfo.pid == android.os.Process.myPid()) {
                return processInfo.processName;
            }
        }
        return null;
    }


    public void setOpenActivityAfterShowInterAds(boolean openActivityAfterShowInterAds) {
        this.openActivityAfterShowInterAds = openActivityAfterShowInterAds;
    }

    public AdRequest getAdRequest() {
        AdRequest.Builder builder = new AdRequest.Builder();
        //AV Language
        Bundle extras = new Bundle();
        extras.putString("npa", "1");
        extras.putString("GoogleConsent", "en"); // Replace "en" with the desired language code
        builder.addNetworkExtrasBundle(AdMobAdapter.class, extras);
        //AV Language

        if (isAdcolony) {
            AdColonyBundleBuilder.setShowPrePopup(true);
            AdColonyBundleBuilder.setShowPostPopup(true);
            builder.addNetworkExtrasBundle(AdColonyAdapter.class, AdColonyBundleBuilder.build());
        }

        if (isAppLovin) {
            Bundle extras1 = new AppLovinExtras.Builder()
                    .setMuteAudio(true)
                    .build();
            builder.addNetworkExtrasBundle(ApplovinAdapter.class, extras1);
        }
        return builder.build();
    }

    private void requestInterstitialAds(InterstitialAd mInterstitialAd, String id, InterstitialAdLoadCallback callback) {
        if (mInterstitialAd == null) {

        }
    }

    public boolean interstitialSplashLoaded() {
        return mInterstitialSplash != null;
    }

    public boolean interstitialSplashHighFloorLoaded() {
        return mInterstitialSplashHighFloor != null;
    }

    public InterstitialAd getmInterstitialSplash() {
        return mInterstitialSplash;
    }


    public void loadSplashInterstitialAds(final Context context, String id, long timeOut, long timeDelay, AdCallback adListener) {
        isTimeDelay = false;
        isTimeout = false;
        Log.i(TAG, "loadSplashInterstitalAds  start time loading:" + Calendar.getInstance().getTimeInMillis() + "    ShowLoadingSplash:" + isShowLoadingSplash);

        if (AppPurchase.getInstance().isPurchased(context)) {
            if (adListener != null) {
                adListener.onNextAction();
            }
            return;
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //check delay show ad splash
                if (mInterstitialSplash != null) {
                    Log.i(TAG, "loadSplashInterstitalAds:show ad on delay ");
                    onShowSplash((AppCompatActivity) context, adListener);
                    return;
                }
                Log.i(TAG, "loadSplashInterstitalAds: delay validate");
                isTimeDelay = true;
            }
        }, timeDelay);

        if (timeOut > 0) {
            handlerTimeout = new Handler();
            rdTimeout = new Runnable() {
                @Override
                public void run() {
                    Log.e(TAG, "loadSplashInterstitalAds: on timeout");
                    isTimeout = true;
                    if (mInterstitialSplash != null) {
                        Log.i(TAG, "loadSplashInterstitalAds:show ad on timeout ");
                        onShowSplash((AppCompatActivity) context, adListener);
                        return;
                    }
                    if (adListener != null) {
                        adListener.onNextAction();
                        isShowLoadingSplash = false;
                    }
                }
            };
            handlerTimeout.postDelayed(rdTimeout, timeOut);
        }


        isShowLoadingSplash = true;
        getInterstitialAds(context, id, new AdCallback() {
            @Override
            public void onInterstitialLoad(InterstitialAd interstitialAd) {
                super.onInterstitialLoad(interstitialAd);
                Log.e(TAG, "loadSplashInterstitalAds  end time loading success:" + Calendar.getInstance().getTimeInMillis() + "     time limit:" + isTimeout);
                if (isTimeout)
                    return;
                if (interstitialAd != null) {
                    mInterstitialSplash = interstitialAd;
                    if (isTimeDelay) {
                        onShowSplash((AppCompatActivity) context, adListener);
                        Log.i(TAG, "loadSplashInterstitalAds:show ad on loaded ");
                    }
                }
            }


            @Override
            public void onAdFailedToLoad(LoadAdError i) {
                super.onAdFailedToLoad(i);
                isShowLoadingSplash = false;
                Log.e(TAG, "loadSplashInterstitalAds  end time loading error:" + Calendar.getInstance().getTimeInMillis() + "     time limit:" + isTimeout);
                if (isTimeout)
                    return;
                if (adListener != null) {
                    if (handlerTimeout != null && rdTimeout != null) {
                        handlerTimeout.removeCallbacks(rdTimeout);
                    }
                    if (i != null)
                        Log.e(TAG, "loadSplashInterstitalAds: load fail " + i.getMessage());
                    adListener.onAdFailedToLoad(i);
                    adListener.onNextAction();
                }
            }

            @Override
            public void onAdFailedToShow(@Nullable AdError adError) {
                super.onAdFailedToShow(adError);
                if (adListener != null) {
                    adListener.onAdFailedToShow(adError);
                    adListener.onNextAction();
                }
            }
        });

    }


    public void loadSplashInterstitialAds(final Context context, String id, long timeOut, long timeDelay, boolean showSplashIfReady, AdCallback adListener) {
        isTimeDelay = false;
        isTimeout = false;
        Log.i(TAG, "loadSplashInterstitalAds  start time loading:" + Calendar.getInstance().getTimeInMillis() + "    ShowLoadingSplash:" + isShowLoadingSplash);

        if (AppPurchase.getInstance().isPurchased(context)) {
            if (adListener != null) {
                adListener.onNextAction();
            }
            return;
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //check delay show ad splash
                if (mInterstitialSplash != null) {
                    Log.i(TAG, "loadSplashInterstitalAds:show ad on delay ");
                    if (showSplashIfReady)
                        onShowSplash((AppCompatActivity) context, adListener);
                    else
                        adListener.onAdSplashReady();
                    return;
                }
                Log.i(TAG, "loadSplashInterstitalAds: delay validate");
                isTimeDelay = true;
            }
        }, timeDelay);

        if (timeOut > 0) {
            handlerTimeout = new Handler();
            rdTimeout = new Runnable() {
                @Override
                public void run() {
                    Log.e(TAG, "loadSplashInterstitalAds: on timeout");
                    isTimeout = true;
                    if (mInterstitialSplash != null) {
                        Log.i(TAG, "loadSplashInterstitalAds:show ad on timeout ");
                        if (showSplashIfReady)
                            onShowSplash((AppCompatActivity) context, adListener);
                        else
                            adListener.onAdSplashReady();
                        return;
                    }
                    if (adListener != null) {
                        adListener.onNextAction();
                        isShowLoadingSplash = false;
                    }
                }
            };
            handlerTimeout.postDelayed(rdTimeout, timeOut);
        }

//        if (isShowLoadingSplash)
//            return;
        isShowLoadingSplash = true;
        getInterstitialAds(context, id, new AdCallback() {
            @Override
            public void onInterstitialLoad(InterstitialAd interstitialAd) {
                super.onInterstitialLoad(interstitialAd);
                Log.e(TAG, "loadSplashInterstitalAds  end time loading success:" + Calendar.getInstance().getTimeInMillis() + "     time limit:" + isTimeout);
                if (isTimeout)
                    return;
                if (interstitialAd != null) {
                    mInterstitialSplash = interstitialAd;
                    if (isTimeDelay) {
                        if (showSplashIfReady)
                            onShowSplash((AppCompatActivity) context, adListener);
                        else
                            adListener.onAdSplashReady();
                        Log.i(TAG, "loadSplashInterstitalAds:show ad on loaded ");
                    }
                }
            }

            @Override
            public void onAdFailedToShow(@Nullable AdError adError) {
                super.onAdFailedToShow(adError);
                if (adListener != null) {
                    adListener.onAdFailedToShow(adError);
                    adListener.onNextAction();
                }
            }

            @Override
            public void onAdFailedToLoad(LoadAdError i) {
                super.onAdFailedToLoad(i);
                Log.e(TAG, "loadSplashInterstitalAds  end time loading error:" + Calendar.getInstance().getTimeInMillis() + "     time limit:" + isTimeout);
                if (isTimeout)
                    return;
                if (adListener != null) {
                    adListener.onNextAction();
                    if (handlerTimeout != null && rdTimeout != null) {
                        handlerTimeout.removeCallbacks(rdTimeout);
                    }
                    if (i != null)
                        Log.e(TAG, "loadSplashInterstitalAds: load fail " + i.getMessage());
                    adListener.onAdFailedToLoad(i);
                }
            }
        });

    }


    public void loadSplashInterstitialAdsHighFloor(final Context context, String id, long timeOut, long timeDelay, boolean showSplashIfReady, AdCallback adListener) {
        isTimeDelayHighFloor = false;
        isTimeoutHighFloor = false;
        Log.i(TAG, "loadSplashInterstitialAdsHighFloor  start time loading:" + Calendar.getInstance().getTimeInMillis() + "    ShowLoadingSplash:" + isShowLoadingSplash);

        if (AppPurchase.getInstance().isPurchased(context)) {
            if (adListener != null) {
                adListener.onNextAction();
            }
            return;
        }
        new Handler().postDelayed(() -> {
            //check delay show ad splash
            if (mInterstitialSplashHighFloor != null) {
                Log.i(TAG, "loadSplashInterstitialAdsHighFloor:show ad on delay ");
                if (showSplashIfReady)
                    onShowSplashHighFloor((AppCompatActivity) context, adListener);
                else
                    adListener.onAdSplashReady();
                return;
            }
            Log.i(TAG, "loadSplashInterstitialAdsHighFloor: delay validate");
            isTimeDelayHighFloor = true;
        }, timeDelay);

        if (timeOut > 0) {
            handlerTimeoutHighFloor = new Handler();
            rdTimeoutHighFloor = () -> {
                Log.e(TAG, "loadSplashInterstitialAdsHighFloor: on timeout");
                isTimeoutHighFloor = true;
                if (mInterstitialSplash != null) {
                    Log.i(TAG, "loadSplashInterstitialAdsHighFloor:show ad on timeout ");
                    if (showSplashIfReady)
                        onShowSplashHighFloor((AppCompatActivity) context, adListener);
                    else
                        adListener.onAdSplashReady();
                    return;
                }
                if (adListener != null) {
                    adListener.onNextAction();
                    isShowLoadingSplash = false;
                }
            };
            handlerTimeoutHighFloor.postDelayed(rdTimeoutHighFloor, timeOut);
        }

        isShowLoadingSplash = true;
        getInterstitialAds(context, id, new AdCallback() {
            @Override
            public void onInterstitialLoad(InterstitialAd interstitialAd) {
                super.onInterstitialLoad(interstitialAd);
                Log.e(TAG, "loadSplashInterstitialAdsHighFloor  end time loading success:" + Calendar.getInstance().getTimeInMillis() + "     time limit:" + isTimeout);
                if (isTimeoutHighFloor)
                    return;
                if (interstitialAd != null) {
                    mInterstitialSplashHighFloor = interstitialAd;
                    if (isTimeDelayHighFloor) {
                        if (showSplashIfReady)
                            onShowSplashHighFloor((AppCompatActivity) context, adListener);
                        else
                            adListener.onAdSplashReady();
                        Log.i(TAG, "loadSplashInterstitialAdsHighFloor:show ad on loaded ");
                    }
                }
            }

            @Override
            public void onAdFailedToShow(@Nullable AdError adError) {
                super.onAdFailedToShow(adError);
                if (adListener != null) {
                    adListener.onAdFailedToShow(adError);
                    adListener.onNextAction();
                }
            }

            @Override
            public void onAdFailedToLoad(LoadAdError i) {
                super.onAdFailedToLoad(i);
                Log.e(TAG, "loadSplashInterstitialAdsHighFloor  end time loading error:" + Calendar.getInstance().getTimeInMillis() + "     time limit:" + isTimeout);
                if (isTimeoutHighFloor)
                    return;
                if (adListener != null) {
                    adListener.onNextAction();
                    if (handlerTimeoutHighFloor != null && rdTimeoutHighFloor != null) {
                        handlerTimeoutHighFloor.removeCallbacks(rdTimeoutHighFloor);
                    }
                    if (i != null)
                        Log.e(TAG, "loadSplashInterstitialAdsHighFloor: load fail " + i.getMessage());
                    adListener.onAdFailedToLoad(i);
                }
            }
        });
    }


    public void onShowSplashHighFloor(AppCompatActivity activity, AdCallback adListener) {
        isShowLoadingSplash = true;
        Log.e(TAG, "onShowSplashHighFloor: ");

        if (mInterstitialSplashHighFloor == null) {
            adListener.onNextAction();
            return;
        }

        mInterstitialSplashHighFloor.setOnPaidEventListener(adValue -> {
            Log.e(TAG, "OnPaidEvent splash:" + adValue.getValueMicros());

            NarayanLogEventManager.logPaidAdImpression(context,
                    adValue,
                    mInterstitialSplashHighFloor.getAdUnitId(),
                    mInterstitialSplashHighFloor.getResponseInfo()
                            .getMediationAdapterClassName(), AdType.INTERSTITIAL);
        });

        if (handlerTimeoutHighFloor != null && rdTimeoutHighFloor != null) {
            handlerTimeoutHighFloor.removeCallbacks(rdTimeoutHighFloor);
        }

        if (adListener != null) {
            adListener.onAdLoaded();
        }

        mInterstitialSplashHighFloor.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                Log.e(TAG, " Splash:onAdShowedFullScreenContent ");
                AppOpenManager.getInstance().setInterstitialShowing(true);
                AppOpenManager.getInstance().disableAppResume();
                isShowLoadingSplash = false;
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                Log.e(TAG, " Splash:onAdDismissedFullScreenContent ");
                AppOpenManager.getInstance().setInterstitialShowing(false);
                AppOpenManager.getInstance().enableAppResume();
                mInterstitialSplashHighFloor = null;
                if (adListener != null) {
                    if (!openActivityAfterShowInterAds) {
                        adListener.onNextAction();
                    }
                    adListener.onAdClosed();

                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
                isShowLoadingSplash = false;
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "Splash onAdFailedToShowFullScreenContent: " + adError.getMessage());
                mInterstitialSplashHighFloor = null;
                isShowLoadingSplash = false;
                if (adListener != null) {
                    adListener.onAdFailedToShow(adError);
                    if (!openActivityAfterShowInterAds) {
                        adListener.onNextAction();
                    }

                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                if (disableAdResumeWhenClickAds)
                    AppOpenManager.getInstance().disableAdResumeByClickAction();
                NarayanLogEventManager.logClickAdsEvent(context, mInterstitialSplashHighFloor.getAdUnitId());
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                if (adListener != null) {
                    adListener.onAdImpression();
                }
            }
        });

        if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            try {
                if (dialog != null && dialog.isShowing())
                    dialog.dismiss();
                dialog = new PrepareLoadingAdsDialog(activity);
                try {
                    dialog.show();
                } catch (Exception e) {
                    adListener.onNextAction();
                    return;
                }
            } catch (Exception e) {
                dialog = null;
                e.printStackTrace();
            }
            new Handler().postDelayed(() -> {
                if (activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                    if (openActivityAfterShowInterAds && adListener != null) {
                        adListener.onNextAction();
                        new Handler().postDelayed(() -> {
                            if (dialog != null && dialog.isShowing() && !activity.isDestroyed())
                                dialog.dismiss();
                        }, 1500);
                    }
                    if (mInterstitialSplashHighFloor != null) {
                        Log.i(TAG, "start show InterstitialAd " + activity.getLifecycle().getCurrentState().name() + "/" + ProcessLifecycleOwner.get().getLifecycle().getCurrentState().name());
                        mInterstitialSplashHighFloor.show(activity);
                        isShowLoadingSplash = false;
                    } else if (adListener != null) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                        adListener.onNextAction();
                        isShowLoadingSplash = false;
                    }
                } else {
                    if (dialog != null && dialog.isShowing() && !activity.isDestroyed())
                        dialog.dismiss();
                    isShowLoadingSplash = false;
                    Log.e(TAG, "onShowSplash:   show fail in background after show loading ad");
                    adListener.onAdFailedToShow(new AdError(0, " show fail in background after show loading ad", "NarayanAd"));
                }
            }, 800);

        } else {
            isShowLoadingSplash = false;
            Log.e(TAG, "onShowSplash: fail on background");
        }
    }

    public void onShowSplash(AppCompatActivity activity, AdCallback adListener) {
        isShowLoadingSplash = true;
        Log.e(TAG, "onShowSplash: ");

        if (mInterstitialSplash == null) {
            adListener.onNextAction();
            return;
        }

        mInterstitialSplash.setOnPaidEventListener(adValue -> {
            Log.e(TAG, "OnPaidEvent splash:" + adValue.getValueMicros());

            NarayanLogEventManager.logPaidAdImpression(context,
                    adValue,
                    mInterstitialSplash.getAdUnitId(),
                    mInterstitialSplash.getResponseInfo()
                            .getMediationAdapterClassName(), AdType.INTERSTITIAL);
        });

        if (handlerTimeout != null && rdTimeout != null) {
            handlerTimeout.removeCallbacks(rdTimeout);
        }

        if (adListener != null) {
            adListener.onAdLoaded();
        }

        mInterstitialSplash.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                Log.e(TAG, " Splash:onAdShowedFullScreenContent ");
                AppOpenManager.getInstance().setInterstitialShowing(true);
                AppOpenManager.getInstance().disableAppResume();
                isShowLoadingSplash = false;
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                Log.e(TAG, " Splash:onAdDismissedFullScreenContent ");
                AppOpenManager.getInstance().setInterstitialShowing(false);
                AppOpenManager.getInstance().enableAppResume();
                mInterstitialSplash = null;
                if (adListener != null) {
                    if (!openActivityAfterShowInterAds) {
                        adListener.onNextAction();
                    }
                    adListener.onAdClosed();

                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
                isShowLoadingSplash = false;
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "Splash onAdFailedToShowFullScreenContent: " + adError.getMessage());
                mInterstitialSplash = null;
                isShowLoadingSplash = false;
                if (adListener != null) {
                    adListener.onAdFailedToShow(adError);
                    if (!openActivityAfterShowInterAds) {
                        adListener.onNextAction();
                    }

                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                if (disableAdResumeWhenClickAds)
                    AppOpenManager.getInstance().disableAdResumeByClickAction();
                NarayanLogEventManager.logClickAdsEvent(context, mInterstitialSplash.getAdUnitId());
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                if (adListener != null) {
                    adListener.onAdImpression();
                }
            }
        });

        if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            try {
                if (dialog != null && dialog.isShowing())
                    dialog.dismiss();
                dialog = new PrepareLoadingAdsDialog(activity);
                try {
                    dialog.show();
                } catch (Exception e) {
                    adListener.onNextAction();
                    return;
                }
            } catch (Exception e) {
                dialog = null;
                e.printStackTrace();
            }
            new Handler().postDelayed(() -> {
                if (activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                    if (openActivityAfterShowInterAds && adListener != null) {
                        adListener.onNextAction();
                        new Handler().postDelayed(() -> {
                            if (dialog != null && dialog.isShowing() && !activity.isDestroyed())
                                dialog.dismiss();
                        }, 1500);
                    }
                    if (activity != null && mInterstitialSplash != null) {
                        Log.i(TAG, "start show InterstitialAd " + activity.getLifecycle().getCurrentState().name() + "/" + ProcessLifecycleOwner.get().getLifecycle().getCurrentState().name());
                        mInterstitialSplash.show(activity);
                        isShowLoadingSplash = false;
                    } else if (adListener != null) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                        adListener.onNextAction();
                        isShowLoadingSplash = false;
                    }
                } else {
                    if (dialog != null && dialog.isShowing() && !activity.isDestroyed())
                        dialog.dismiss();
                    isShowLoadingSplash = false;
                    Log.e(TAG, "onShowSplash:   show fail in background after show loading ad");
                    adListener.onAdFailedToShow(new AdError(0, " show fail in background after show loading ad", "NarayanAd"));
                }
            }, 800);

        } else {
            isShowLoadingSplash = false;
            Log.e(TAG, "onShowSplash: fail on background");
        }
    }

    public void onCheckShowSplashWhenFail(AppCompatActivity activity, AdCallback callback, int timeDelay) {
        new Handler(activity.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (interstitialSplashLoaded() && !isShowLoadingSplash()) {
                    Log.i(TAG, "show ad splash when show fail in background");
                    Admob.getInstance().onShowSplash(activity, callback);
                }
            }
        }, timeDelay);
    }

    public void onCheckShowSplashHighFloorWhenFail(AppCompatActivity activity, AdCallback callback, int timeDelay) {
        new Handler(activity.getMainLooper()).postDelayed(() -> {
            if (!isShowLoadingSplash()
                    && (interstitialSplashHighFloorLoaded() || interstitialSplashLoaded())) {
                Log.i(TAG, "show ad splash when show fail in background");
                Admob.getInstance().onShowSplashHighFloor(activity, callback);
            }
        }, timeDelay);
    }

    public void loadInterstitialAds(Context context, String id, long timeOut, AdCallback adListener) {
        isTimeout = false;
        if (AppPurchase.getInstance().isPurchased(context)) {
            if (adListener != null) {
                adListener.onNextAction();
            }
            return;
        }
        interstitialAd = null;
        getInterstitialAds(context, id, new AdCallback() {
            @Override
            public void onInterstitialLoad(InterstitialAd interstitialAd) {
                Admob.this.interstitialAd = interstitialAd;

                if (interstitialAd == null) {
                    if (adListener != null) {
                        adListener.onAdFailedToLoad(null);
                    }
                    return;
                }
                if (handlerTimeout != null && rdTimeout != null) {
                    handlerTimeout.removeCallbacks(rdTimeout);
                }
                if (isTimeout) {
                    return;
                }
                if (adListener != null) {
                    if (handlerTimeout != null && rdTimeout != null) {
                        handlerTimeout.removeCallbacks(rdTimeout);
                    }
                    adListener.onInterstitialLoad(interstitialAd);
                }

                if (interstitialAd != null) {
                    interstitialAd.setOnPaidEventListener(adValue -> {

                        Log.e(TAG, "OnPaidEvent loadInterstitialAds:" + adValue.getValueMicros());
                        NarayanLogEventManager.logPaidAdImpression(context,
                                adValue,
                                interstitialAd.getAdUnitId(),
                                interstitialAd.getResponseInfo()
                                        .getMediationAdapterClassName(), AdType.INTERSTITIAL);
                    });
                }
            }

            @Override
            public void onAdFailedToLoad(LoadAdError i) {

                if (adListener != null) {
                    if (handlerTimeout != null && rdTimeout != null) {
                        handlerTimeout.removeCallbacks(rdTimeout);
                    }
                    adListener.onAdFailedToLoad(i);
                }
            }
        });


        if (timeOut > 0) {
            handlerTimeout = new Handler();
            rdTimeout = () -> {
                isTimeout = true;
                if (interstitialAd != null) {
                    adListener.onInterstitialLoad(interstitialAd);
                    return;
                }
                if (adListener != null) {

                    adListener.onNextAction();
                }
            };
            handlerTimeout.postDelayed(rdTimeout, timeOut);
        }
    }


    public void getInterstitialAds(Context context, String id, AdCallback adCallback) {
        if (Arrays.asList(context.getResources().getStringArray(R.array.list_id_test)).contains(id)) {
            showTestIdAlert(context, INTERS_ADS, id);
        }
        Log.e("DRASHTI", "getInterstitialAds: isPurchased=>"+AppPurchase.getInstance().isPurchased(context) );
        Log.e("DRASHTI", "getInterstitialAds: getNumClickAdsPerDay=>"+AdmodHelper.getNumClickAdsPerDay(context, id) );
        Log.e("DRASHTI", "getInterstitialAds: maxClickAds=>"+maxClickAds );
        if (AppPurchase.getInstance().isPurchased(context) || AdmodHelper.getNumClickAdsPerDay(context, id) >= maxClickAds) {
            adCallback.onInterstitialLoad(null);
            return;
        }


        InterstitialAd.load(context, id, getAdRequest(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        if (adCallback != null)
                            adCallback.onInterstitialLoad(interstitialAd);

                        //tracking adjust
                        interstitialAd.setOnPaidEventListener(adValue -> {
                            Log.e(TAG, "OnPaidEvent getInterstitalAds:" + adValue.getValueMicros());

                            NarayanLogEventManager.logPaidAdImpression(context,
                                    adValue,
                                    interstitialAd.getAdUnitId(),
                                    interstitialAd.getResponseInfo()
                                            .getMediationAdapterClassName(), AdType.INTERSTITIAL);
                        });
                        Log.i(TAG, "InterstitialAds onAdLoaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.i(TAG, loadAdError.getMessage());
                        if (adCallback != null)
                            adCallback.onAdFailedToLoad(loadAdError);
                    }

                });

    }


    public void showInterstitialAdByTimes(final Context context, final InterstitialAd mInterstitialAd, final AdCallback callback, long timeDelay) {
        if (timeDelay > 0) {
            handlerTimeout = new Handler();
            rdTimeout = new Runnable() {
                @Override
                public void run() {
                    forceShowInterstitial(context, mInterstitialAd, callback);
                }
            };
            handlerTimeout.postDelayed(rdTimeout, timeDelay);
        } else {
            forceShowInterstitial(context, mInterstitialAd, callback);
        }
    }


    public void showInterstitialAdByTimes(final Context context, InterstitialAd mInterstitialAd, final AdCallback callback) {
        AdmodHelper.setupAdmodData(context);
        if (AppPurchase.getInstance().isPurchased(context)) {
            callback.onNextAction();
            return;
        }
        if (mInterstitialAd == null) {
            if (callback != null) {
                callback.onNextAction();
            }
            return;
        }

        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {

            @Override
            public void onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent();
                // Called when fullscreen content is dismissed.

                AppOpenManager.getInstance().setInterstitialShowing(false);
                if (callback != null) {
                    if (!openActivityAfterShowInterAds) {
                        callback.onNextAction();
                    }
                    callback.onAdClosed();
                }
                if (dialog != null) {
                    dialog.dismiss();
                }
                Log.e(TAG, "onAdDismissedFullScreenContent");
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                super.onAdFailedToShowFullScreenContent(adError);
                Log.e(TAG, "onAdFailedToShowFullScreenContent: " + adError.getMessage());
                // Called when fullscreen content failed to show.
                if (callback != null) {
                    callback.onAdFailedToShow(adError);
                    if (!openActivityAfterShowInterAds) {
                        callback.onNextAction();
                    }

                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
            }

            @Override
            public void onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent();
                Log.e(TAG, "onAdShowedFullScreenContent ");
                SharePreferenceUtils.setLastImpressionInterstitialTime(context);
                AppOpenManager.getInstance().setInterstitialShowing(true);
                // Called when fullscreen content is shown.
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                if (disableAdResumeWhenClickAds)
                    AppOpenManager.getInstance().disableAdResumeByClickAction();
                if (callback != null) {
                    callback.onAdClicked();
                }
                NarayanLogEventManager.logClickAdsEvent(context, mInterstitialAd.getAdUnitId());
            }
        });

        if (AdmodHelper.getNumClickAdsPerDay(context, mInterstitialAd.getAdUnitId()) < maxClickAds) {
            showInterstitialAd(context, mInterstitialAd, callback);
            return;
        }
        if (callback != null) {
            callback.onNextAction();
        }
    }


    public void forceShowInterstitial(Context context, InterstitialAd mInterstitialAd, final AdCallback callback) {
        currentClicked = numShowAds;
        showInterstitialAdByTimes(context, mInterstitialAd, callback);
    }


    private void showInterstitialAd(Context context, InterstitialAd mInterstitialAd, AdCallback callback) {
        currentClicked++;
        if (currentClicked >= numShowAds && mInterstitialAd != null) {
            if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                try {
                    if (dialog != null && dialog.isShowing())
                        dialog.dismiss();
                    dialog = new PrepareLoadingAdsDialog(context);
                    dialog.setCancelable(false);
                    try {
                        callback.onInterstitialShow();
                        dialog.show();
                    } catch (Exception e) {
                        callback.onNextAction();
                        return;
                    }
                } catch (Exception e) {
                    dialog = null;
                    e.printStackTrace();
                }
                new Handler().postDelayed(() -> {
                    if (((AppCompatActivity) context).getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                        if (openActivityAfterShowInterAds && callback != null) {
                            callback.onNextAction();
                            new Handler().postDelayed(() -> {
                                if (dialog != null && dialog.isShowing() && !((Activity) context).isDestroyed())
                                    dialog.dismiss();
                            }, 1500);
                        }
                        Log.i(TAG, "start show InterstitialAd " + ((AppCompatActivity) context).getLifecycle().getCurrentState().name() + "/" + ProcessLifecycleOwner.get().getLifecycle().getCurrentState().name());
                        mInterstitialAd.show((Activity) context);
                    } else {
                        if (dialog != null && dialog.isShowing() && !((Activity) context).isDestroyed())
                            dialog.dismiss();
                        Log.e(TAG, "showInterstitialAd:   show fail in background after show loading ad");
                        callback.onAdFailedToShow(new AdError(0, " show fail in background after show loading ad", "NarayanAd"));
                    }
                }, 800);
            }
            currentClicked = 0;
        } else if (callback != null) {
            if (dialog != null) {
                dialog.dismiss();
            }
            callback.onNextAction();
        }
    }


    public void loadBanner(final Activity mActivity, String id) {
        final FrameLayout adContainer = mActivity.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = mActivity.findViewById(R.id.shimmer_container_banner);
        loadBanner(mActivity, id, adContainer, containerShimmer, null, false, BANNER_INLINE_LARGE_STYLE);
    }


    public void loadBanner(final Activity mActivity, String id, AdCallback callback) {
        final FrameLayout adContainer = mActivity.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = mActivity.findViewById(R.id.shimmer_container_banner);
        loadBanner(mActivity, id, adContainer, containerShimmer, callback, false, BANNER_INLINE_LARGE_STYLE);
    }


    @Deprecated
    public void loadBanner(final Activity mActivity, String id, Boolean useInlineAdaptive) {
        final FrameLayout adContainer = mActivity.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = mActivity.findViewById(R.id.shimmer_container_banner);
        loadBanner(mActivity, id, adContainer, containerShimmer, null, useInlineAdaptive, BANNER_INLINE_LARGE_STYLE);
    }


    public void loadInlineBanner(final Activity activity, String id, String inlineStyle) {
        final FrameLayout adContainer = activity.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = activity.findViewById(R.id.shimmer_container_banner);
        loadBanner(activity, id, adContainer, containerShimmer, null, true, inlineStyle);
    }


    @Deprecated
    public void loadBanner(final Activity mActivity, String id, final AdCallback callback, Boolean useInlineAdaptive) {
        final FrameLayout adContainer = mActivity.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = mActivity.findViewById(R.id.shimmer_container_banner);
        loadBanner(mActivity, id, adContainer, containerShimmer, callback, useInlineAdaptive, BANNER_INLINE_LARGE_STYLE);
    }


    public void loadInlineBanner(final Activity activity, String id, String inlineStyle, final AdCallback callback) {
        final FrameLayout adContainer = activity.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = activity.findViewById(R.id.shimmer_container_banner);
        loadBanner(activity, id, adContainer, containerShimmer, callback, true, inlineStyle);
    }


    public void loadCollapsibleBanner(final Activity mActivity, String id, String gravity, final AdCallback callback) {
        final FrameLayout adContainer = mActivity.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = mActivity.findViewById(R.id.shimmer_container_banner);
        loadCollapsibleBanner(mActivity, id, gravity, adContainer, containerShimmer, callback);
    }


    public void loadBannerFragment(final Activity mActivity, String id, final View rootView) {
        final FrameLayout adContainer = rootView.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = rootView.findViewById(R.id.shimmer_container_banner);
        loadBanner(mActivity, id, adContainer, containerShimmer, null, false, BANNER_INLINE_LARGE_STYLE);
    }


    public void loadBannerFragment(final Activity mActivity, String id, final View rootView, final AdCallback callback) {
        final FrameLayout adContainer = rootView.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = rootView.findViewById(R.id.shimmer_container_banner);
        loadBanner(mActivity, id, adContainer, containerShimmer, callback, false, BANNER_INLINE_LARGE_STYLE);
    }


    @Deprecated
    public void loadBannerFragment(final Activity mActivity, String id, final View rootView, Boolean useInlineAdaptive) {
        final FrameLayout adContainer = rootView.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = rootView.findViewById(R.id.shimmer_container_banner);
        loadBanner(mActivity, id, adContainer, containerShimmer, null, useInlineAdaptive, BANNER_INLINE_LARGE_STYLE);
    }


    public void loadInlineBannerFragment(final Activity activity, String id, final View rootView, String inlineStyle) {
        final FrameLayout adContainer = rootView.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = rootView.findViewById(R.id.shimmer_container_banner);
        loadBanner(activity, id, adContainer, containerShimmer, null, true, inlineStyle);
    }


    @Deprecated
    public void loadBannerFragment(final Activity mActivity, String id, final View rootView, final AdCallback callback, Boolean useInlineAdaptive) {
        final FrameLayout adContainer = rootView.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = rootView.findViewById(R.id.shimmer_container_banner);
        loadBanner(mActivity, id, adContainer, containerShimmer, callback, useInlineAdaptive, BANNER_INLINE_LARGE_STYLE);
    }


    public void loadInlineBannerFragment(final Activity activity, String id, final View rootView, String inlineStyle, final AdCallback callback) {
        final FrameLayout adContainer = rootView.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = rootView.findViewById(R.id.shimmer_container_banner);
        loadBanner(activity, id, adContainer, containerShimmer, callback, true, inlineStyle);
    }


    public void loadCollapsibleBannerFragment(final Activity mActivity, String id, final View rootView, String gravity, final AdCallback callback) {
        final FrameLayout adContainer = rootView.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = rootView.findViewById(R.id.shimmer_container_banner);
        loadCollapsibleBanner(mActivity, id, gravity, adContainer, containerShimmer, callback);
    }

    public void loadBanner(final Activity mActivity, String id,
                           final FrameLayout adContainer, final ShimmerFrameLayout containerShimmer,
                           final AdCallback callback, Boolean useInlineAdaptive, String inlineStyle) {
        if (Arrays.asList(mActivity.getResources().getStringArray(R.array.list_id_test)).contains(id)) {
            showTestIdAlert(mActivity, BANNER_ADS, id);
        }
        Log.e("CHECKBANNER", "loadBanner: isPurchase=>"+AppPurchase.getInstance().isPurchased(mActivity) );
        if (AppPurchase.getInstance().isPurchased(mActivity)) {
            containerShimmer.setVisibility(View.GONE);
            return;
        }

        containerShimmer.setVisibility(View.VISIBLE);
        containerShimmer.startShimmer();
        try {
            AdView adView = new AdView(mActivity);
            adView.setAdUnitId(id);
            adContainer.addView(adView);
            AdSize adSize = getAdSize(mActivity, useInlineAdaptive, inlineStyle);
            int adHeight;
            if (useInlineAdaptive && inlineStyle.equalsIgnoreCase(BANNER_INLINE_SMALL_STYLE)) {
                adHeight = MAX_SMALL_INLINE_BANNER_HEIGHT;
            } else {
                adHeight = adSize.getHeight();
            }
            containerShimmer.getLayoutParams().height = (int) (adHeight * Resources.getSystem().getDisplayMetrics().density + 0.5f);
            adView.setAdSize(adSize);
            adView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            adView.setAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    Log.e("CHECKBANNER", "loadBanner: loading error=>"+loadAdError.getMessage());
                    containerShimmer.stopShimmer();
                    adContainer.setVisibility(View.GONE);
                    containerShimmer.setVisibility(View.GONE);
                }


                @Override
                public void onAdLoaded() {
                    Log.e(TAG, "Banner adapter class name: " + adView.getResponseInfo().getMediationAdapterClassName());
                    containerShimmer.stopShimmer();
                    containerShimmer.setVisibility(View.GONE);
                    adContainer.setVisibility(View.VISIBLE);
                    if (adView != null) {
                        adView.setOnPaidEventListener(adValue -> {
                            Log.e(TAG, "OnPaidEvent banner:" + adValue.getValueMicros());

                            NarayanLogEventManager.logPaidAdImpression(context,
                                    adValue,
                                    adView.getAdUnitId(),
                                    adView.getResponseInfo()
                                            .getMediationAdapterClassName(), AdType.BANNER);
                        });
                    }
                }

                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                    if (disableAdResumeWhenClickAds)
                        AppOpenManager.getInstance().disableAdResumeByClickAction();
                    if (callback != null) {
                        callback.onAdClicked();
                        Log.e(TAG, "onAdClicked");
                    }
                    NarayanLogEventManager.logClickAdsEvent(context, id);
                }

                @Override
                public void onAdImpression() {
                    super.onAdImpression();
                    if (callback != null) {
                        callback.onAdImpression();
                    }
                }
            });

            adView.loadAd(getAdRequest());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadCollapsibleBanner(final Activity mActivity, String id, String gravity, final FrameLayout adContainer,
                                      final ShimmerFrameLayout containerShimmer, final AdCallback callback) {
        if (Arrays.asList(mActivity.getResources().getStringArray(R.array.list_id_test)).contains(id)) {
            showTestIdAlert(mActivity, BANNER_ADS, id);
        }
        if (AppPurchase.getInstance().isPurchased(mActivity)) {
            containerShimmer.setVisibility(View.GONE);
            return;
        }

        containerShimmer.setVisibility(View.VISIBLE);
        containerShimmer.startShimmer();
        try {
            AdView adView = new AdView(mActivity);
            adView.setAdUnitId(id);
            adContainer.addView(adView);
            AdSize adSize = getAdSize(mActivity, false, "");
            containerShimmer.getLayoutParams().height = (int) (adSize.getHeight() * Resources.getSystem().getDisplayMetrics().density + 0.5f);
            adView.setAdSize(adSize);
            adView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            adView.loadAd(getAdRequestForCollapsibleBanner(gravity));
            adView.setAdListener(new AdListener() {

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
                    containerShimmer.stopShimmer();
                    adContainer.setVisibility(View.GONE);
                    containerShimmer.setVisibility(View.GONE);
                    if (callback != null) {
                        callback.onAdFailedToLoad(loadAdError);
                    }
                }

                @Override
                public void onAdLoaded() {
                    Log.e(TAG, "Banner adapter class name: " + adView.getResponseInfo().getMediationAdapterClassName());
                    containerShimmer.stopShimmer();
                    containerShimmer.setVisibility(View.GONE);
                    adContainer.setVisibility(View.VISIBLE);
                    adView.setOnPaidEventListener(adValue -> {
                        Log.e(TAG, "OnPaidEvent banner:" + adValue.getValueMicros());

                        NarayanLogEventManager.logPaidAdImpression(context,
                                adValue,
                                adView.getAdUnitId(),
                                adView.getResponseInfo()
                                        .getMediationAdapterClassName(), AdType.BANNER);
                    });
                    if (callback != null) {
                        callback.onAdLoaded();
                    }
                }

                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                    if (disableAdResumeWhenClickAds)
                        AppOpenManager.getInstance().disableAdResumeByClickAction();
                    NarayanLogEventManager.logClickAdsEvent(context, id);
                    if (callback != null) {
                        callback.onAdClicked();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private AdSize getAdSize(Activity mActivity, Boolean useInlineAdaptive, String inlineStyle) {

        // Step 2 - Determine the screen width (less decorations) to use for the ad width.
        /*Display display = mActivity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;*/

        DisplayMetrics displayMetrics = mActivity.getResources().getDisplayMetrics();
        int widthPixels;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = mActivity.getWindowManager().getCurrentWindowMetrics();
            widthPixels = windowMetrics.getBounds().width();
        } else {
            widthPixels = displayMetrics.widthPixels;
        }

        float density = displayMetrics.density;
        int adWidth = (int) (widthPixels / density);

        // Step 3 - Get adaptive ad size and return for setting on the ad view.
        if (useInlineAdaptive) {
            if (inlineStyle.equalsIgnoreCase(BANNER_INLINE_LARGE_STYLE)) {
                return AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(mActivity, adWidth);
            } else {
                return AdSize.getInlineAdaptiveBannerAdSize(adWidth, MAX_SMALL_INLINE_BANNER_HEIGHT);
            }
        }
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(mActivity, adWidth);

    }

    private AdRequest getAdRequestForCollapsibleBanner(String gravity) {
        AdRequest.Builder builder = new AdRequest.Builder();
        Bundle admobExtras = new Bundle();
        admobExtras.putString("collapsible", gravity);
        admobExtras.putString("npa", "1");
        admobExtras.putString("GoogleConsent", "en");
        builder.addNetworkExtrasBundle(AdMobAdapter.class, admobExtras);
        // no need from facebook sdk ver 6.12.0.0
        /*if (isFan) {
            Bundle extras = new FacebookExtras()
                    .setNativeBanner(true)
                    .build();

            builder.addNetworkExtrasBundle(FacebookAdapter.class, extras);
        }*/


        if (isAdcolony) {
            AdColonyBundleBuilder.setShowPrePopup(true);
            AdColonyBundleBuilder.setShowPostPopup(true);
            builder.addNetworkExtrasBundle(AdColonyAdapter.class, AdColonyBundleBuilder.build());
        }

        if (isAppLovin) {
            Bundle extras1 = new AppLovinExtras.Builder()
                    .setMuteAudio(true)
                    .build();
            builder.addNetworkExtrasBundle(ApplovinAdapter.class, extras1);
        }
        return builder.build();
    }


    public void loadNative(final Activity mActivity, String id) {
        final FrameLayout frameLayout = mActivity.findViewById(R.id.fl_adplaceholder);
        final ShimmerFrameLayout containerShimmer = mActivity.findViewById(R.id.shimmer_container_native);
        loadNative(mActivity, containerShimmer, frameLayout, id, R.layout.custom_native_admob_free_size);
    }

    public void loadNativeFragment(final Activity mActivity, String id, View parent) {
        final FrameLayout frameLayout = parent.findViewById(R.id.fl_adplaceholder);
        final ShimmerFrameLayout containerShimmer = parent.findViewById(R.id.shimmer_container_native);
        loadNative(mActivity, containerShimmer, frameLayout, id, R.layout.custom_native_admob_free_size);
    }

    public void loadSmallNative(final Activity mActivity, String adUnitId) {
        final FrameLayout frameLayout = mActivity.findViewById(R.id.fl_adplaceholder);
        final ShimmerFrameLayout containerShimmer = mActivity.findViewById(R.id.shimmer_container_native);
        loadNative(mActivity, containerShimmer, frameLayout, adUnitId, R.layout.custom_native_admod_medium);
    }

    public void loadSmallNativeFragment(final Activity mActivity, String adUnitId, View parent) {
        final FrameLayout frameLayout = parent.findViewById(R.id.fl_adplaceholder);
        final ShimmerFrameLayout containerShimmer = parent.findViewById(R.id.shimmer_container_native);
        loadNative(mActivity, containerShimmer, frameLayout, adUnitId, R.layout.custom_native_admod_medium);
    }

    public void loadNativeAd(Context context, String id, final AdCallback callback) {
        if (Arrays.asList(context.getResources().getStringArray(R.array.list_id_test)).contains(id)) {
            showTestIdAlert(context, NATIVE_ADS, id);
        }
        if (AppPurchase.getInstance().isPurchased(context)) {
            return;
        }
        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(true)
                .build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .build();
        AdLoader adLoader = new AdLoader.Builder(context, id)
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {

                    @Override
                    public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
                        callback.onUnifiedNativeAdLoaded(nativeAd);
                        nativeAd.setOnPaidEventListener(adValue -> {
                            Log.e(TAG, "OnPaidEvent getInterstitalAds:" + adValue.getValueMicros());

                            NarayanLogEventManager.logPaidAdImpression(context,
                                    adValue,
                                    id,
                                    nativeAd.getResponseInfo().getMediationAdapterClassName(), AdType.NATIVE);
                        });
                    }
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        Log.e(TAG, "NativeAd onAdFailedToLoad: " + error.getMessage());
                        callback.onAdFailedToLoad(error);
                    }

                    @Override
                    public void onAdImpression() {
                        super.onAdImpression();
                        Log.e(TAG, "native onAdImpression");
                        if (callback != null) {
                            callback.onAdImpression();
                        }
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        if (disableAdResumeWhenClickAds)
                            AppOpenManager.getInstance().disableAdResumeByClickAction();
                        if (callback != null) {
                            callback.onAdClicked();
                            Log.e(TAG, "onAdClicked");
                        }
                        NarayanLogEventManager.logClickAdsEvent(context, id);
                    }
                })
                .withNativeAdOptions(adOptions)
                .build();
        adLoader.loadAd(getAdRequest());
    }

    public void loadNativeAds(Context context, String id, final AdCallback callback, int countAd) {
        if (Arrays.asList(context.getResources().getStringArray(R.array.list_id_test)).contains(id)) {
            showTestIdAlert(context, NATIVE_ADS, id);
        }
        if (AppPurchase.getInstance().isPurchased(context)) {
            callback.onAdClosed();
            return;
        }
        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(true)
                .build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .build();
        AdLoader adLoader = new AdLoader.Builder(context, id)
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {

                    @Override
                    public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
                        callback.onUnifiedNativeAdLoaded(nativeAd);
                        nativeAd.setOnPaidEventListener(adValue -> {
                            Log.e(TAG, "OnPaidEvent getInterstitalAds:" + adValue.getValueMicros());

                            NarayanLogEventManager.logPaidAdImpression(context,
                                    adValue,
                                    id,
                                    nativeAd.getResponseInfo().getMediationAdapterClassName(), AdType.NATIVE);
                        });
                    }
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        Log.e(TAG, "NativeAd onAdFailedToLoad: " + error.getMessage());
                        callback.onAdFailedToLoad(error);
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        if (disableAdResumeWhenClickAds)
                            AppOpenManager.getInstance().disableAdResumeByClickAction();
                        if (callback != null) {
                            callback.onAdClicked();
                            Log.e(TAG, "onAdClicked");
                        }
                        NarayanLogEventManager.logClickAdsEvent(context, id);
                    }
                })
                .withNativeAdOptions(adOptions)
                .build();
        adLoader.loadAds(getAdRequest(), countAd);
    }

    private void loadNative(final Context context, final ShimmerFrameLayout containerShimmer, final FrameLayout frameLayout, final String id, final int layout) {
        if (Arrays.asList(context.getResources().getStringArray(R.array.list_id_test)).contains(id)) {
            showTestIdAlert(context, NATIVE_ADS, id);
        }
        if (AppPurchase.getInstance().isPurchased(context)) {
            containerShimmer.setVisibility(View.GONE);
            return;
        }
        frameLayout.removeAllViews();
        frameLayout.setVisibility(View.GONE);
        containerShimmer.setVisibility(View.VISIBLE);
        containerShimmer.startShimmer();

        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(true)
                .build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .build();


        AdLoader adLoader = new AdLoader.Builder(context, id)
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {

                    @Override
                    public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
                        containerShimmer.stopShimmer();
                        containerShimmer.setVisibility(View.GONE);
                        frameLayout.setVisibility(View.VISIBLE);
                        @SuppressLint("InflateParams") NativeAdView adView = (NativeAdView) LayoutInflater.from(context)
                                .inflate(layout, null);
                        nativeAd.setOnPaidEventListener(adValue -> {
                            Log.e(TAG, "OnPaidEvent native:" + adValue.getValueMicros());

                            NarayanLogEventManager.logPaidAdImpression(context,
                                    adValue,
                                    id,
                                    nativeAd.getResponseInfo().getMediationAdapterClassName(), AdType.NATIVE);
                        });
                        populateUnifiedNativeAdView(nativeAd, adView);
                        frameLayout.removeAllViews();
                        frameLayout.addView(adView);
                    }


                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        Log.e(TAG, "onAdFailedToLoad: " + error.getMessage());
                        containerShimmer.stopShimmer();
                        containerShimmer.setVisibility(View.GONE);
                        frameLayout.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        if (disableAdResumeWhenClickAds)
                            AppOpenManager.getInstance().disableAdResumeByClickAction();
                        NarayanLogEventManager.logClickAdsEvent(context, id);
                    }
                })
                .withNativeAdOptions(adOptions)
                .build();

        adLoader.loadAd(getAdRequest());
    }

    private void loadNative(final Context context, final ShimmerFrameLayout containerShimmer, final FrameLayout frameLayout, final String id, final int layout, final AdCallback callback) {
        if (Arrays.asList(context.getResources().getStringArray(R.array.list_id_test)).contains(id)) {
            showTestIdAlert(context, NATIVE_ADS, id);
        }
        if (AppPurchase.getInstance().isPurchased(context)) {
            containerShimmer.setVisibility(View.GONE);
            return;
        }
        frameLayout.removeAllViews();
        frameLayout.setVisibility(View.GONE);
        containerShimmer.setVisibility(View.VISIBLE);
        containerShimmer.startShimmer();

        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(true)
                .build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .build();


        AdLoader adLoader = new AdLoader.Builder(context, id)
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {

                    @Override
                    public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
                        containerShimmer.stopShimmer();
                        containerShimmer.setVisibility(View.GONE);
                        frameLayout.setVisibility(View.VISIBLE);
                        @SuppressLint("InflateParams") NativeAdView adView = (NativeAdView) LayoutInflater.from(context)
                                .inflate(layout, null);
                        nativeAd.setOnPaidEventListener(adValue -> {
                            Log.e(TAG, "OnPaidEvent Native:" + adValue.getValueMicros());

                            NarayanLogEventManager.logPaidAdImpression(context,
                                    adValue,
                                    id,
                                    nativeAd.getResponseInfo().getMediationAdapterClassName(), AdType.NATIVE);
                        });
                        populateUnifiedNativeAdView(nativeAd, adView);
                        frameLayout.removeAllViews();
                        frameLayout.addView(adView);
                    }

                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        Log.e(TAG, "onAdFailedToLoad: " + error.getMessage());
                        containerShimmer.stopShimmer();
                        containerShimmer.setVisibility(View.GONE);
                        frameLayout.setVisibility(View.GONE);
                    }


                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        if (disableAdResumeWhenClickAds)
                            AppOpenManager.getInstance().disableAdResumeByClickAction();
                        if (callback != null) {
                            callback.onAdClicked();
                            Log.e(TAG, "onAdClicked");
                        }
                        NarayanLogEventManager.logClickAdsEvent(context, id);
                    }
                })
                .withNativeAdOptions(adOptions)
                .build();


        adLoader.loadAd(getAdRequest());
    }


    public void populateUnifiedNativeAdView(NativeAd nativeAd, NativeAdView adView) {

        adView.setMediaView(adView.findViewById(R.id.ad_media));

        if (adView.getMediaView() != null) {
            adView.getMediaView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (context != null && AppUtil.VARIANT_DEV) {
                        float sizeMin = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                120,
                                context.getResources().getDisplayMetrics()
                        );
                        Log.e(TAG, "Native sizeMin: " + sizeMin);
                        Log.e(TAG, "Native w/h media : " + adView.getMediaView().getWidth() + "/" + adView.getMediaView().getHeight());
                        if (adView.getMediaView().getWidth() < sizeMin || adView.getMediaView().getHeight() < sizeMin) {
                            Toast.makeText(context, "Size media native not valid", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }, 1000);

        }
        // Set other ad assets.
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setPriceView(adView.findViewById(R.id.ad_price));
        adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
//        adView.setStoreView(adView.findViewById(R.id.ad_store));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

        // The headline is guaranteed to be in every UnifiedNativeAd.
        try {
            ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        try {
            if (nativeAd.getBody() == null) {
                adView.getBodyView().setVisibility(View.INVISIBLE);
            } else {
                adView.getBodyView().setVisibility(View.VISIBLE);
                ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (nativeAd.getCallToAction() == null) {
                Objects.requireNonNull(adView.getCallToActionView()).setVisibility(View.INVISIBLE);
            } else {
                Objects.requireNonNull(adView.getCallToActionView()).setVisibility(View.VISIBLE);
                ((TextView) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (nativeAd.getIcon() == null) {
                Objects.requireNonNull(adView.getIconView()).setVisibility(View.GONE);
            } else {
                ((ImageView) adView.getIconView()).setImageDrawable(
                        nativeAd.getIcon().getDrawable());
                adView.getIconView().setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (nativeAd.getPrice() == null) {
                Objects.requireNonNull(adView.getPriceView()).setVisibility(View.INVISIBLE);
            } else {
                Objects.requireNonNull(adView.getPriceView()).setVisibility(View.VISIBLE);
                ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

//
        try {
            if (nativeAd.getStarRating() == null) {
                Objects.requireNonNull(adView.getStarRatingView()).setVisibility(View.INVISIBLE);
            } else {
                ((RatingBar) Objects.requireNonNull(adView.getStarRatingView())).setRating(nativeAd.getStarRating().floatValue());
                adView.getStarRatingView().setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (nativeAd.getAdvertiser() == null) {
                adView.getAdvertiserView().setVisibility(View.INVISIBLE);
            } else {
                ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
                adView.getAdvertiserView().setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad. The SDK will populate the adView's MediaView
        // with the media content from this native ad.
        adView.setNativeAd(nativeAd);

    }


    private RewardedAd rewardedAd;


    public void initRewardAds(Context context, String id) {
        if (Arrays.asList(context.getResources().getStringArray(R.array.list_id_test)).contains(id)) {
            showTestIdAlert(context, REWARD_ADS, id);
        }
        if (AppPurchase.getInstance().isPurchased(context)) {
            return;
        }
        this.nativeId = id;
        if (AppPurchase.getInstance().isPurchased(context)) {
            return;
        }
        RewardedAd.load(context, id, getAdRequest(), new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                Admob.this.rewardedAd = rewardedAd;
                Admob.this.rewardedAd.setOnPaidEventListener(adValue -> {

                    Log.e(TAG, "OnPaidEvent Reward:" + adValue.getValueMicros());

                    NarayanLogEventManager.logPaidAdImpression(context,
                            adValue,
                            rewardedAd.getAdUnitId(), Admob.this.rewardedAd.getResponseInfo().getMediationAdapterClassName()
                            , AdType.REWARDED);
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                Log.e(TAG, "RewardedAd onAdFailedToLoad: " + loadAdError.getMessage());
            }
        });
    }


    public void initRewardAds(Context context, String id, AdCallback callback) {
        if (Arrays.asList(context.getResources().getStringArray(R.array.list_id_test)).contains(id)) {
            showTestIdAlert(context, REWARD_ADS, id);
        }
        if (AppPurchase.getInstance().isPurchased(context)) {
            return;
        }
        this.nativeId = id;
        if (AppPurchase.getInstance().isPurchased(context)) {
            return;
        }
        RewardedAd.load(context, id, getAdRequest(), new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                callback.onRewardAdLoaded(rewardedAd);
                Admob.this.rewardedAd = rewardedAd;
                Admob.this.rewardedAd.setOnPaidEventListener(adValue -> {
                    Log.e(TAG, "OnPaidEvent Reward:" + adValue.getValueMicros());

                    NarayanLogEventManager.logPaidAdImpression(context,
                            adValue,
                            rewardedAd.getAdUnitId(),
                            Admob.this.rewardedAd.getResponseInfo().getMediationAdapterClassName()
                            , AdType.REWARDED);
                });

            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                callback.onAdFailedToLoad(loadAdError);
                Admob.this.rewardedAd = null;
                Log.e(TAG, "RewardedAd onAdFailedToLoad: " + loadAdError.getMessage());
            }
        });
    }


    public void getRewardInterstitial(Context context, String id, AdCallback callback) {
        if (Arrays.asList(context.getResources().getStringArray(R.array.list_id_test)).contains(id)) {
            showTestIdAlert(context, REWARD_ADS, id);
        }
        if (AppPurchase.getInstance().isPurchased(context)) {
            return;
        }
        this.nativeId = id;
        if (AppPurchase.getInstance().isPurchased(context)) {
            return;
        }
        RewardedInterstitialAd.load(context, id, getAdRequest(), new RewardedInterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedInterstitialAd rewardedAd) {
                callback.onRewardAdLoaded(rewardedAd);
                Log.i(TAG, "RewardInterstitial onAdLoaded ");
                rewardedAd.setOnPaidEventListener(adValue -> {
                    Log.e(TAG, "OnPaidEvent Reward:" + adValue.getValueMicros());
                    NarayanLogEventManager.logPaidAdImpression(context,
                            adValue,
                            rewardedAd.getAdUnitId(),
                            rewardedAd.getResponseInfo().getMediationAdapterClassName()
                            , AdType.REWARDED);
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                callback.onAdFailedToLoad(loadAdError);
                Log.e(TAG, "RewardInterstitial onAdFailedToLoad: " + loadAdError.getMessage());
            }
        });
    }

    public RewardedAd getRewardedAd() {

        return rewardedAd;
    }


    public void showRewardAds(final Activity context, final RewardCallback adCallback) {
        if (AppPurchase.getInstance().isPurchased(context)) {
            adCallback.onUserEarnedReward(null);
            return;
        }
        if (rewardedAd == null) {
            initRewardAds(context, nativeId);

            adCallback.onRewardedAdFailedToShow(0);
            return;
        } else {
            Admob.this.rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent();
                    if (adCallback != null)
                        adCallback.onRewardedAdClosed();

                    AppOpenManager.getInstance().setInterstitialShowing(false);

                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    super.onAdFailedToShowFullScreenContent(adError);
                    if (adCallback != null)
                        adCallback.onRewardedAdFailedToShow(adError.getCode());
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent();

                    AppOpenManager.getInstance().setInterstitialShowing(true);
                    rewardedAd = null;
                }

                public void onAdClicked() {
                    super.onAdClicked();
                    if (disableAdResumeWhenClickAds)
                        AppOpenManager.getInstance().disableAdResumeByClickAction();
                    NarayanLogEventManager.logClickAdsEvent(context, rewardedAd.getAdUnitId());
                }
            });
            rewardedAd.show(context, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    if (adCallback != null) {
                        adCallback.onUserEarnedReward(rewardItem);

                    }
                }
            });
        }
    }


    public void showRewardInterstitial(final Activity activity, RewardedInterstitialAd rewardedInterstitialAd, final RewardCallback adCallback) {
        if (AppPurchase.getInstance().isPurchased(activity)) {
            adCallback.onUserEarnedReward(null);
            return;
        }
        if (rewardedInterstitialAd == null) {
            initRewardAds(activity, nativeId);

            adCallback.onRewardedAdFailedToShow(0);
            return;
        } else {
            rewardedInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent();
                    if (adCallback != null)
                        adCallback.onRewardedAdClosed();

                    AppOpenManager.getInstance().setInterstitialShowing(false);

                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    super.onAdFailedToShowFullScreenContent(adError);
                    if (adCallback != null)
                        adCallback.onRewardedAdFailedToShow(adError.getCode());
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent();

                    AppOpenManager.getInstance().setInterstitialShowing(true);

                }

                public void onAdClicked() {
                    super.onAdClicked();
                    NarayanLogEventManager.logClickAdsEvent(activity, rewardedAd.getAdUnitId());
                    if (disableAdResumeWhenClickAds)
                        AppOpenManager.getInstance().disableAdResumeByClickAction();
                }
            });
            rewardedInterstitialAd.show(activity, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    if (adCallback != null) {
                        adCallback.onUserEarnedReward(rewardItem);
                    }
                }
            });
        }
    }


    public void showRewardAds(final Activity context, RewardedAd rewardedAd, final RewardCallback adCallback) {
        if (AppPurchase.getInstance().isPurchased(context)) {
            adCallback.onUserEarnedReward(null);
            return;
        }
        if (rewardedAd == null) {
            initRewardAds(context, nativeId);

            adCallback.onRewardedAdFailedToShow(0);
            return;
        } else {
            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent();
                    if (adCallback != null)
                        adCallback.onRewardedAdClosed();


                    AppOpenManager.getInstance().setInterstitialShowing(false);

                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    super.onAdFailedToShowFullScreenContent(adError);
                    if (adCallback != null)
                        adCallback.onRewardedAdFailedToShow(adError.getCode());
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent();

                    AppOpenManager.getInstance().setInterstitialShowing(true);
                    initRewardAds(context, nativeId);
                }

                public void onAdClicked() {
                    super.onAdClicked();
                    if (disableAdResumeWhenClickAds)
                        AppOpenManager.getInstance().disableAdResumeByClickAction();
                    if (adCallback != null) {
                        adCallback.onAdClicked();
                    }
                    NarayanLogEventManager.logClickAdsEvent(context, rewardedAd.getAdUnitId());
                }
            });
            rewardedAd.show(context, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    if (adCallback != null) {
                        adCallback.onUserEarnedReward(rewardItem);

                    }
                }
            });
        }
    }


    public AdmobRecyclerAdapter getNativeRepeatAdapter(Activity activity, String id, int layoutCustomNative, int layoutAdPlaceHolder, RecyclerView.Adapter originalAdapter,
                                                       NarayanAdPlacer.Listener listener, int repeatingInterval) {
        NarayanAdPlacerSettings settings = new NarayanAdPlacerSettings(layoutCustomNative, layoutAdPlaceHolder);
        settings.setAdUnitId(id);
        settings.setListener(listener);
        settings.setRepeatingInterval(repeatingInterval);
        AdmobRecyclerAdapter adAdapter = new AdmobRecyclerAdapter(settings, originalAdapter, activity);
        return adAdapter;
    }

    public AdmobRecyclerAdapter getNativeFixedPositionAdapter(Activity activity, String id, int layoutCustomNative, int layoutAdPlaceHolder, RecyclerView.Adapter originalAdapter,
                                                              NarayanAdPlacer.Listener listener, int position) {

        NarayanAdPlacerSettings settings = new NarayanAdPlacerSettings(layoutCustomNative, layoutAdPlaceHolder);
        settings.setAdUnitId(id);
        settings.setListener(listener);
        settings.setFixedPosition(position);
        AdmobRecyclerAdapter adAdapter = new AdmobRecyclerAdapter(settings, originalAdapter, activity);
        return adAdapter;
    }


    @SuppressLint("HardwareIds")
    public String getDeviceId(Activity activity) {
        String android_id = Settings.Secure.getString(activity.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        return md5(android_id).toUpperCase();
    }

    private String md5(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
        }
        return "";
    }

    private void showTestIdAlert(Context context, int typeAds, String id) {
        String content = "";
        switch (typeAds) {
            case BANNER_ADS:
                content = "Banner Ads: ";
                break;
            case INTERS_ADS:
                content = "Interstitial Ads: ";
                break;
            case REWARD_ADS:
                content = "Rewarded Ads: ";
                break;
            case NATIVE_ADS:
                content = "Native Ads: ";
                break;
        }
        content += id;
        Notification notification = new NotificationCompat.Builder(context, "warning_ads")
                .setContentTitle("Found test ad id")
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_warning)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("warning_ads",
                    "Warning Ads",
                    NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(typeAds, notification);

        Log.e(TAG, "Found test ad id on debug : " + AppUtil.VARIANT_DEV);

        if (!AppUtil.VARIANT_DEV) {
            Log.e(TAG, "Found test ad id on environment production. use test id only for develop environment ");
            throw new RuntimeException("Found test ad id on environment production. Id found: " + id);
        }
    }

    public final static int SPLASH_ADS = 0;
    public final static int RESUME_ADS = 1;
    private final static int BANNER_ADS = 2;
    private final static int INTERS_ADS = 3;
    private final static int REWARD_ADS = 4;
    private final static int NATIVE_ADS = 5;

}
