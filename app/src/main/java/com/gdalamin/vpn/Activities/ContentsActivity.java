package com.gdalamin.vpn.Activities;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;

import com.anchorfree.partner.api.callback.Callback;
import com.anchorfree.partner.api.response.RemainingTraffic;
import com.anchorfree.partner.exceptions.PartnerRequestException;
import com.anchorfree.sdk.UnifiedSDK;
import com.anchorfree.vpnsdk.exceptions.VpnException;
import com.anchorfree.vpnsdk.vpnservice.VPNState;
import com.gdalamin.vpn.BuildConfig;
import com.gdalamin.vpn.R;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.formats.MediaView;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.navigation.NavigationView;
import com.gdalamin.vpn.Config;
import com.gdalamin.vpn.Utils.LocalFormatter;
import com.gdalamin.vpn.speed.Speed;
//import com.onesignal.OneSignal;
import com.pixplicity.easyprefs.library.Prefs;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import es.dmoral.toasty.Toasty;

public abstract class ContentsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    LottieAnimationView lottieAnimationView;
    boolean vpn_toast_check = true;
    private Handler mHandler = new Handler();
    private long mStartRX = 0;
    private long mStartTX = 0;
    Handler handlerTrafic = null;

    protected static final String TAG = MainActivity.class.getSimpleName();

    private int adCount = 0;
    VPNState state;
    int progressBarValue = 0;
    Handler handler = new Handler();
    private Handler customHandler = new Handler();
    private long startTime = 0L;
    long timeInMilliseconds = 0L;
    long timeSwapBuff = 0L;
    long updatedTime = 0L;


    @BindView(R.id.downloading)
    TextView textDownloading;

    @BindView(R.id.testShowMeterupload)
    TextView textUploading;



    @BindView(R.id.vpn_details)
    ImageView vpn_detail_image;


    @BindView(R.id.tv_timer)
    TextView timerTextView;

    @BindView(R.id.connect_btn)
    ImageView connectBtnTextView;

    @BindView(R.id.connection_state)
    TextView connectionStateTextView;


    @BindView(R.id.flag_image)
    ImageView imgFlag;



    @BindView(R.id.flag_name)
    TextView flagName;



    RelativeLayout relativeLayout ;






//    ImageView i_connection_status_image = (ImageView) findViewById(R.id.countrycode);

    //admob native advance)
    private UnifiedNativeAd nativeAd;
    public InterstitialAd mInterstitialAd;
    private String STATUS;
    private DrawerLayout drawer;

    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Lottie animation to show animation in the project


        LottieAnimationView lottieAnimationView2 = (LottieAnimationView) findViewById(R.id.animation_view1);

        lottieAnimationView2.pauseAnimation();



        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);


    lottieAnimationView = findViewById(R.id.animation_view);
        relativeLayout = (RelativeLayout) findViewById(R.id.liner);




        ButterKnife.bind(this);
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        vpn_detail_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                showServerList();
                startActivity(new Intent(ContentsActivity.this, Servers.class));
            }
        });


        if (getResources().getBoolean(R.bool.ads_switch) && (!Config.ads_subscription && !Config.all_subscription && !Config.vip_subscription)) {
            // Initialize the Mobile Ads SDK.

            MobileAds.initialize(ContentsActivity.this, getString(R.string.admob_appid));

            //interstitial
            mInterstitialAd = new InterstitialAd(this);
            mInterstitialAd.setAdUnitId(getString(R.string.admob_intersitail));
            mInterstitialAd.loadAd(new AdRequest.Builder()
                    .addTestDevice("91b511f6-d4ab-4a6b-94fa-e538dfbee85f")
                    .build());

        }

        if (Prefs.contains("connectStart") && Prefs.getString("connectStart", "").equals("on")) {

            isConnected(new Callback<Boolean>() {
                @Override
                public void success(@NonNull Boolean aBoolean) {
                    if (aBoolean) {
                        STATUS = "Disconnect";

                        if (mInterstitialAd.isLoaded()) {
                            mInterstitialAd.show();
                        } else {
                            disconnectAlert();
                        }
                    } else {

                        STATUS = "Connect";

                        if (mInterstitialAd.isLoaded()) {
//                        Interstitial Ad loaded successfully...
                            mInterstitialAd.show();
                        } else {

                            updateUI();
                            connectToVpn();
                        }
                    }
                }

                @Override
                public void failure(PartnerRequestException error) {
                    Toast.makeText(getApplicationContext(), "" + error.getMessage(), Toast.LENGTH_LONG).show();

                }

            });
        }

//        if (Prefs.contains("noti") && Prefs.getString("noti", "off").equals("off")) {
//            OneSignal.setSubscription(false);
//        } else if (Prefs.contains("noti") && Prefs.getString("noti", "off").equals("on")) {
//            OneSignal.setSubscription(true);
//        } else {
//            OneSignal.setSubscription(false);
//        }
        //loadFreeServers();
        handlerTrafic = new Handler();
        handleTraficData();
    }


    @Override
    protected void onStart() {
        super.onStart();
        if(STATUS == null) return;
        if (STATUS.equals("Connect")) {
            updateUI();
            connectToVpn();
            loadAdAgain();
        } else if (STATUS.equals("Disconnect")) {
            disconnectAlert();
            loadAdAgain();
        }
    }

    private void loadAdAgain() {
//        load Ads for multiple times in background
//        if (getResources().getBoolean(R.bool.ads_switch)) {
//            mInterstitialAd.loadAd(new AdRequest.Builder().build());
//        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_upgrade) {
//            upgrade application is available...
            //startActivity(new Intent(this, Servers.class));
        } /*else if (id == R.id.nav_unlock) {
            startActivity(new Intent(this, PurchasesActivity.class));
        }*/ else if (id == R.id.nav_helpus) {
//            find help about the application
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:"));
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"youremail@gmail.com"}); // Put Your Support Email Here
            intent.putExtra(Intent.EXTRA_SUBJECT, "Send Bug Report");
            intent.putExtra(Intent.EXTRA_TEXT, "Please Give Your Feedback ");

            try {
                startActivity(Intent.createChooser(intent, "send mail"));
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(this, "No mail app found!!!", Toast.LENGTH_SHORT);
            } catch (Exception ex) {
                Toast.makeText(this, "Unexpected Error!!!", Toast.LENGTH_SHORT);
            }
        } else if (id == R.id.nav_rate) {
//            rate application...
            rateUs();
        } else if (id == R.id.nav_share) {
//            share the application...
            try {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "share app");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "I'm using this Free VPN App, it's provide all servers free https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID);
                startActivity(Intent.createChooser(shareIntent, "choose one"));
            } catch (Exception e) {
            }

        } /*else if (id == R.id.nav_setting) {
//            Application settings...
            startActivity(new Intent(this, Settings.class));
        }*/ /*else if (id == R.id.nav_faq) {
            startActivity(new Intent(this, Faq.class));
        }*/ else if (id == R.id.nav_policy) {
            Uri uri = Uri.parse(getResources().getString(R.string.privacy_policy_link)); // missing 'http://' will cause crashed
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private Handler mUIHandler = new Handler(Looper.getMainLooper());
    final Runnable mUIUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            updateUI();
            checkRemainingTraffic();
            mUIHandler.postDelayed(mUIUpdateRunnable, 10000);
        }
    };

    @Override
    protected void onResume() {
//        if the application again available from background state...
        super.onResume();
        isConnected(new Callback<Boolean>() {
            @Override
            public void success(@NonNull Boolean aBoolean) {
                if (aBoolean) {
                    startUIUpdateTask();
                }
            }

            @Override
            public void failure(PartnerRequestException error) {

            }


        });
    }

    @Override
    protected void onPause() {
//        application in the background state...
        super.onPause();
        stopUIUpdateTask();
    }

    @Override
    protected void onDestroy() {
        if (nativeAd != null) {
            nativeAd.destroy();
        }
        super.onDestroy();
    }

    protected abstract void loginToVpn();

    @OnClick(R.id.animation_view1)
    public void onConnectBtnClick(View v) {

        isConnected(new Callback<Boolean>() {
            @Override
            public void success(@NonNull Boolean aBoolean) {
                if (aBoolean) {
                    STATUS = "Disconnect";
                    if (mInterstitialAd != null) mInterstitialAd.setAdListener(new AdListener() {
                        @Override
                        public void onAdLoaded() {
                            // Code to be executed when an ad finishes loading.
                            if (mInterstitialAd.isLoaded()) {
                                mInterstitialAd.show();
                            } else {
                                AdRequest request = new AdRequest.Builder()
                                        .addTestDevice("91b511f6-d4ab-4a6b-94fa-e538dfbee85f")
                                        .build();
                                mInterstitialAd.loadAd(request);
                            }
                        }

                        @Override
                        public void onAdFailedToLoad(int errorCode) {
                            // Code to be executed when an ad request fails.
                            disconnectAlert();
                        }

                        @Override
                        public void onAdOpened() {
                            // Code to be executed when the ad is displayed.
                        }

                        @Override
                        public void onAdClicked() {
                            // Code to be executed when the user clicks on an ad.
                        }

                        @Override
                        public void onAdLeftApplication() {
                            // Code to be executed when the user has left the app.
                        }

                        @Override
                        public void onAdClosed() {
                            // Code to be executed when the interstitial ad is closed.
                            disconnectAlert();
                        }
                    });
                    if (getResources().getBoolean(R.bool.ads_switch) && (!Config.ads_subscription && !Config.all_subscription && !Config.vip_subscription)) {

                        if (mInterstitialAd.isLoaded()) {
                            mInterstitialAd.show();
                        } else {
                            AdRequest request = new AdRequest.Builder()
                                    .addTestDevice("91b511f6-d4ab-4a6b-94fa-e538dfbee85f")
                                    .build();
                            mInterstitialAd.loadAd(request);

                  /*  Intent intent=new Intent(ContentsActivity.this,SpeedBoosterActivity.class);
                    startActivity(intent);*/
                        }

                    } else {
                        disconnectAlert();
                    }
                } else {

                    STATUS = "Connect";

                    if (getResources().getBoolean(R.bool.ads_switch) && (!Config.ads_subscription && !Config.all_subscription && !Config.vip_subscription)) {
//                        Interstitial Ad loaded successfully...
                        mInterstitialAd.setAdListener(new AdListener() {
                            @Override
                            public void onAdLoaded() {
                                // Code to be executed when an ad finishes loading.
                                if (mInterstitialAd.isLoaded()) {
                                    mInterstitialAd.show();
                                } else {
                                    AdRequest request = new AdRequest.Builder()
                                            .addTestDevice("91b511f6-d4ab-4a6b-94fa-e538dfbee85f")
                                            .build();
                                    mInterstitialAd.loadAd(request);
                                }
                            }

                            @Override
                            public void onAdFailedToLoad(int errorCode) {
                                // Code to be executed when an ad request fails.
                            }

                            @Override
                            public void onAdOpened() {
                                // Code to be executed when the ad is displayed.
                            }

                            @Override
                            public void onAdClicked() {
                                // Code to be executed when the user clicks on an ad.
                            }

                            @Override
                            public void onAdLeftApplication() {
                                // Code to be executed when the user has left the app.
                            }

                            @Override
                            public void onAdClosed() {
                                // Code to be executed when the interstitial ad is closed.
                                updateUI();
                                connectToVpn();
                            }
                        });
                        if (mInterstitialAd.isLoaded()) {
                            mInterstitialAd.show();
                        } else {
                            AdRequest request = new AdRequest.Builder()
                                    .addTestDevice("91b511f6-d4ab-4a6b-94fa-e538dfbee85f")
                                    .build();
                            mInterstitialAd.loadAd(request);
                            updateUI();
                            connectToVpn();
                        }


                    } else {
                        updateUI();
                        connectToVpn();
                    }
                }
            }

            @Override
            public void failure(PartnerRequestException error) {
                Toast.makeText(getApplicationContext(), "" + error.getMessage(), Toast.LENGTH_LONG).show();

            }


        });

    }

    /*  Different functions defining the state of the vpn
     *  */
    protected abstract void isConnected(Callback<Boolean> callback);

    protected abstract void connectToVpn();

    protected abstract void disconnectFromVnp();

    protected abstract void chooseServer();

    protected abstract void getCurrentServer(Callback<String> callback);

    protected void startUIUpdateTask() {
        stopUIUpdateTask();
        mUIHandler.post(mUIUpdateRunnable);
    }

    protected void stopUIUpdateTask() {
        mUIHandler.removeCallbacks(mUIUpdateRunnable);
        updateUI();
    }

    protected abstract void checkRemainingTraffic();
//Todo work here
    protected void updateUI() {
        //textDownloading.setVisibility(View.INVISIBLE);
        //textUploading.setVisibility(View.INVISIBLE);


        UnifiedSDK.getVpnState(new com.anchorfree.vpnsdk.callbacks.Callback<VPNState>() {
            @Override
            public void success(@NonNull VPNState vpnState) {
                state = vpnState;
                switch (vpnState) {
                    case IDLE: {
//                        vpn is idle...
                        loadIcon();
                        connectBtnTextView.setEnabled(true);
//                        connectionStateTextView.setText(R.string.disconnected);
                        timerTextView.setVisibility(View.GONE);
                        hideConnectProgress();
                        relativeLayout.setVisibility(View.GONE);
                        break;

                    }
                    case CONNECTED: {


                        textDownloading.setVisibility(View.VISIBLE);
                        textUploading.setVisibility(View.VISIBLE);
                        relativeLayout.setVisibility(View.VISIBLE);


                        loadIcon();
                        connectBtnTextView.setEnabled(true);
                        connectionStateTextView.setText(R.string.connected);
                        timer();
                        timerTextView.setVisibility(View.VISIBLE);
                        hideConnectProgress();
                        // mRunnable.run();
                        break;
                    }
                    case CONNECTING_VPN:
                    case CONNECTING_CREDENTIALS:
                    case CONNECTING_PERMISSIONS: {
//                        during connecting vpn
                        loadIcon();
//                        connectionStateTextView.setText(R.string.connecting);
//                        connectBtnTextView.setEnabled(true);
//                        timerTextView.setVisibility(View.GONE);
                        showConnectProgress();
                        break;
                    }
                    case PAUSED: {
//                        vpn paused...
//                        connectBtnTextView.setImageResource(R.drawable.ic_power);
//                        t_connection_status.setText("Not Selected");
//                        connectionStateTextView.setText(R.string.paused);
//                        i_connection_status_image.setImageResource(R.drawable.ic_dot);
                        break;
                    }
                    default: {
//                        gifImageView1.setVisibility(View.INVISIBLE);
//                        gifImageView2.setVisibility(View.INVISIBLE);
                    }

                }
            }

            @Override
            public void failure(@NonNull VpnException e) {

            }
        });

        getCurrentServer(new Callback<String>() {
            //            try to connect to current vpn server...
            @Override
            public void success(@NonNull final String currentServer) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    }
                });
            }

            @Override
            public void failure(PartnerRequestException error) {

            }


        });
    }

    protected void updateTrafficStats(long outBytes, long inBytes) {
//        try to update the traffic state of the vpn...
        String outString = LocalFormatter.easyRead(outBytes, false);
        String inString = LocalFormatter.easyRead(inBytes, false);

    }

    protected void updateRemainingTraffic(RemainingTraffic remainingTrafficResponse) {
        if (remainingTrafficResponse.isUnlimited()) {
        } else {
            String trafficUsed = LocalFormatter.byteCounter(remainingTrafficResponse.getTrafficUsed()) + "Mb";
            String trafficLimit = LocalFormatter.byteCounter(remainingTrafficResponse.getTrafficLimit()) + "Mb";

        }
    }

    protected void showConnectProgress() {
//        Updating progressbar
        new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub

                while (state == VPNState.CONNECTING_VPN || state == VPNState.CONNECTING_CREDENTIALS) {
                    progressBarValue++;

                    handler.post(new Runnable() {

                        @Override
                        public void run() {

                        }
                    });
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    protected void hideConnectProgress() {
        connectionStateTextView.setVisibility(View.VISIBLE);
    }

    protected void showMessage(String msg) {
        Toast.makeText(ContentsActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    protected void rateUs() {
        Uri uri = Uri.parse("market://details?id=" + this.getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flag to intent.
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + this.getPackageName())));
        }
    }

    protected void timer() {
        if (adCount == 0) {
            startTime = SystemClock.uptimeMillis();
            customHandler.postDelayed(updateTimerThread, 0);
            timeSwapBuff += timeInMilliseconds;

        }
    }

    private Runnable updateTimerThread = new Runnable() {

        public void run() {

            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;

            updatedTime = timeSwapBuff + timeInMilliseconds;

            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            int hrs = mins / 60;
            secs = secs % 60;
            int milliseconds = (int) (updatedTime % 1000);
            timerTextView.setText(String.format("%02d", hrs) + ":"
                    + String.format("%02d", mins) + ":"
                    + String.format("%02d", secs));
            customHandler.postDelayed(this, 0);
        }

    };


    protected void loadIcon() {
        if (state == VPNState.IDLE) {
            //Glide.with(this).load(R.drawable.ic_power).into(connectBtnTextView);
//            t_connection_status.setText("Not Selected");
//            i_connection_status_image.setImageResource(R.drawable.connectst);
            connectBtnTextView.setImageResource(R.drawable.connectst);

            lottieAnimationView.setVisibility(View.GONE);

            relativeLayout.setVisibility(View.INVISIBLE);
            mAdView.setVisibility(View.GONE);



        } else if (state == VPNState.CONNECTING_VPN || state == VPNState.CONNECTING_CREDENTIALS)
        {
//            connectBtnTextView.setVisibility(View.VISIBLE);/*INVISIBLE IS CHEANGED TO VISIBLE*/
            lottieAnimationView.setVisibility(View.VISIBLE);
        }
        else if (state == VPNState.CONNECTED)
        {

            mAdView.setVisibility(View.VISIBLE);

            relativeLayout.setVisibility(View.VISIBLE);
            connectBtnTextView.setImageResource(R.drawable.connectkite);


            //Glide.with(this).load(R.drawable.ic_power).into(connectBtnTextView);
//            connectBtnTextView.setVisibility(View.VISIBLE);
//            t_connection_status.setText("Selected");
            lottieAnimationView.setVisibility(View.GONE);
            if (vpn_toast_check == true) {
                Toasty.success(ContentsActivity.this, "Server Connected", Toast.LENGTH_SHORT).show();
                vpn_toast_check = false;
            }
//            i_connection_status_image.setImageResource(R.drawable.ic_dot);

        }
    }

    protected void disconnectAlert() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Do you want to disconnect?");
        builder.setPositiveButton("Disconnect",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        disconnectFromVnp();
                        vpn_toast_check = true;
                        Toasty.success(ContentsActivity.this, "Server Disconnected", Toast.LENGTH_SHORT).show();
                    }
                });
        builder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Toasty.success(ContentsActivity.this, "VPN Remains Connected", Toast.LENGTH_SHORT).show();
                    }
                });
        builder.show();
    }




    /**
     * Creates a request for a new native ad based on the boolean parameters and calls the
     * corresponding "populate" method when one is successfully returned.
     */






    @OnClick(R.id.category)
    void openSideNavigation() {
        drawer.openDrawer(GravityCompat.START, true);
    }

    private long mLastRxBytes = 0;
    private long mLastTxBytes = 0;
    private long mLastTime = 0;

    private Speed mSpeed;


    private void handleTraficData() {

        if (handlerTrafic == null)
            return;


        handlerTrafic.postDelayed(this::setTraficData, 1000);


    }

    private void setTraficData() {
        long currentRxBytes = TrafficStats.getTotalRxBytes();

        long currentTxBytes = TrafficStats.getTotalTxBytes();

        long usedRxBytes = currentRxBytes - mLastRxBytes;
        long usedTxBytes = currentTxBytes - mLastTxBytes;
        long currentTime = System.currentTimeMillis();
        long usedTime = currentTime - mLastTime;

        mLastRxBytes = currentRxBytes;
        mLastTxBytes = currentTxBytes;
        mLastTime = currentTime;

        mSpeed = new Speed(this);
        mSpeed.calcSpeed(usedTime, usedRxBytes, usedTxBytes);

//            mIndicatorNotification.updateNotification(mSpeed);
        Log.e("speed-->>", "down-->>" + mSpeed.down.speedValue + "    upload-->>" + mSpeed.up.speedValue);


        if (state != null &&mSpeed != null && mSpeed.up != null && mSpeed.down != null && state.equals(VPNState.CONNECTED)) {


            textDownloading.setText(mSpeed.down.speedValue + " " + mSpeed.down.speedUnit);
            textUploading.setText(mSpeed.up.speedValue + " " + mSpeed.up.speedUnit);

//            sendBroadcast(traffic);
        } else {
            textDownloading.setText("0  " + mSpeed.down.speedUnit);
            textUploading.setText("0  " + " " + mSpeed.up.speedUnit);
        }

        handleTraficData();
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                .build();
        mInterstitialAd.loadAd(adRequest);
    }

}
