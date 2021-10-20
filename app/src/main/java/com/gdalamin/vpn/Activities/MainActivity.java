package com.gdalamin.vpn.Activities;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.anchorfree.partner.api.auth.AuthMethod;
import com.anchorfree.partner.api.callback.Callback;
import com.anchorfree.partner.api.data.Country;
import com.anchorfree.partner.api.response.User;
import com.anchorfree.partner.exceptions.PartnerRequestException;
import com.anchorfree.reporting.TrackingConstants;
import com.anchorfree.sdk.SessionConfig;
import com.anchorfree.sdk.UnifiedSDK;
import com.anchorfree.sdk.rules.TrafficRule;
import com.anchorfree.vpnsdk.callbacks.CompletableCallback;
import com.anchorfree.vpnsdk.callbacks.TrafficListener;
import com.anchorfree.vpnsdk.callbacks.VpnStateListener;
import com.anchorfree.vpnsdk.exceptions.NetworkRelatedException;
import com.anchorfree.vpnsdk.exceptions.VpnException;
import com.anchorfree.vpnsdk.exceptions.VpnPermissionDeniedException;
import com.anchorfree.vpnsdk.exceptions.VpnPermissionRevokedException;
import com.anchorfree.vpnsdk.exceptions.VpnTransportException;
import com.anchorfree.vpnsdk.transporthydra.HydraTransport;
import com.anchorfree.vpnsdk.transporthydra.HydraVpnTransportException;
import com.anchorfree.vpnsdk.vpnservice.VPNState;
//import com.anjlab.android.iab.v3.BillingProcessor;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.northghost.caketube.CaketubeTransport;
import com.gdalamin.vpn.Config;
import com.gdalamin.vpn.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends ContentsActivity implements TrafficListener, VpnStateListener {

    private String selectedCountry = "";
    private Locale locale;

//    private BillingProcessor bp;

    @Override
    protected void onStart() {
        RequestConfiguration.Builder requestBuilder = new RequestConfiguration.Builder().setTestDeviceIds(
                Collections.singletonList("91b511f6-d4ab-4a6b-94fa-e538dfbee85f")
        );
        MobileAds.setRequestConfiguration(requestBuilder.build());
        MobileAds.initialize(this);
        super.onStart();

//      Try to connect to the vpn server...
        UnifiedSDK.addTrafficListener(this);
        UnifiedSDK.addVpnStateListener(this);
        loginToVpn();

        Intent intent = getIntent();
        selectedCountry = intent.getStringExtra("c");

        if (selectedCountry != null && !VPNState.CONNECTED.equals(true)) {
            if (getResources().getBoolean(R.bool.ads_switch) && (!Config.ads_subscription && !Config.all_subscription && !Config.vip_subscription)) {

            }
            if (mInterstitialAd != null) {
                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();

                    mInterstitialAd.setAdListener(new AdListener() {
                        @Override
                        public void onAdLoaded() {
                            // Code to be executed when an ad finishes loading.

                        }

                        @Override
                        public void onAdFailedToLoad(int errorCode) {
                            // Code to be executed when an ad request fails.
                            // Code to be executed when the interstitial ad is closed.
                            locale = new Locale("", selectedCountry);

                            imgFlag.setImageResource(getResources().getIdentifier("drawable/" + selectedCountry.toLowerCase(), null, getPackageName()));
                            flagName.setText(locale.getDisplayCountry());

                            updateUI();
                            connectToVpn();
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
                            locale = new Locale("", selectedCountry);

                            imgFlag.setImageResource(getResources().getIdentifier("drawable/" + selectedCountry.toLowerCase(), null, getPackageName()));
                            flagName.setText(locale.getDisplayCountry());

                            updateUI();
                            connectToVpn();
                        }
                    });

                } else {
                    locale = new Locale("", selectedCountry);

                    imgFlag.setImageResource(getResources().getIdentifier("drawable/" + selectedCountry.toLowerCase(), null, getPackageName()));
                    flagName.setText(locale.getDisplayCountry());

                    updateUI();
                    connectToVpn();
                }

            } else {
                locale = new Locale("", selectedCountry);

                imgFlag.setImageResource(getResources().getIdentifier("drawable/" + selectedCountry.toLowerCase(), null, getPackageName()));
                flagName.setText(locale.getDisplayCountry());

                updateUI();
                connectToVpn();
            }

        }

    }

    @Override
    protected void onStop() {
//        application stopped...
        super.onStop();
        UnifiedSDK.removeVpnStateListener(this);
        UnifiedSDK.removeTrafficListener(this);
    }

    @Override
    public void onTrafficUpdate(long bytesTx, long bytesRx) {
        updateUI();
        updateTrafficStats(bytesTx, bytesRx);
    }

    @Override
    public void vpnStateChanged(VPNState vpnState) {
        updateUI();
    }

    @Override
    public void vpnError(@NonNull VpnException e) {
        updateUI();
        handleError(e);
    }

    private void handleError(VpnException e) {
        Log.w(TAG, e);
        if (e instanceof NetworkRelatedException) {
            showMessage("Check internet connection");
        } else if (e != null) {
            if (e instanceof VpnPermissionRevokedException) {
                showMessage("User revoked vpn permissions");
            } else if (e instanceof VpnPermissionDeniedException) {
                showMessage("User canceled to grant vpn permissions");
            } else if (e instanceof HydraVpnTransportException) {
                VpnTransportException hydraVpnTransportException = (VpnTransportException) e;
                if (hydraVpnTransportException.getCode() == HydraVpnTransportException.HYDRA_ERROR_BROKEN) {
                    showMessage("Connection with vpn server was lost");
                } else if (hydraVpnTransportException.getCode() == HydraVpnTransportException.HYDRA_DCN_BLOCKED_BW) {
                    showMessage("Client traffic exceeded");
                } else {
                    showMessage("Error in VPN transport");
                }
            } else {
                //showMessage("Error in VPN Service");
            }
        }
    }


    @Override
    protected void loginToVpn() {
//        try to login to the vpn...
        AuthMethod authMethod = AuthMethod.anonymous();
        UnifiedSDK.getInstance().getBackend().login(authMethod, new com.anchorfree.vpnsdk.callbacks.Callback<User>() {
            @Override
            public void success(@NonNull User user) {

            }

            @Override
            public void failure(@NonNull VpnException e) {

            }
        });
    }

    @Override
    protected void isConnected(Callback<Boolean> callback) {
        UnifiedSDK.getVpnState(new com.anchorfree.vpnsdk.callbacks.Callback<VPNState>() {
            @Override
            public void success(@NonNull VPNState vpnState) {
                callback.success(vpnState == VPNState.CONNECTED);

            }

            @Override
            public void failure(@NonNull VpnException e) {
                callback.success(false);

            }

        });
    }

    @Override
    protected void connectToVpn() {
        if (selectedCountry == null) selectedCountry = UnifiedSDK.COUNTRY_OPTIMAL;

        UnifiedSDK.getInstance().getBackend().isLoggedIn(new com.anchorfree.vpnsdk.callbacks.Callback<Boolean>() {
            @Override
            public void success(@NonNull Boolean aBoolean) {
                if (aBoolean) {
                    List<String> fallbackOrder = new ArrayList<>();
                    fallbackOrder.add(HydraTransport.TRANSPORT_ID);
                    fallbackOrder.add(CaketubeTransport.TRANSPORT_ID_TCP);
                    fallbackOrder.add(CaketubeTransport.TRANSPORT_ID_UDP);
                    showConnectProgress();
                    List<String> bypassDomains = new LinkedList<>();
                    bypassDomains.add("*facebook.com");
                    bypassDomains.add("*wtfismyip.com");
                    UnifiedSDK.getInstance().getVPN().start(
                            new SessionConfig.Builder()
                                    .withReason(TrackingConstants.GprReasons.M_UI)
                                    .withVirtualLocation(selectedCountry)
                                    .withTransportFallback(fallbackOrder)
                                    .withTransport(HydraTransport.TRANSPORT_ID)
                                    .addDnsRule(TrafficRule.Builder.bypass().fromDomains(bypassDomains))
                                    .build(), new CompletableCallback() {
                                @Override
                                public void complete() {
                                    hideConnectProgress();
                                    startUIUpdateTask();
                                }

                                @Override
                                public void error(@NonNull VpnException e) {
                                    hideConnectProgress();
                                    updateUI();
                                    handleError(e);
                                }

                            });

                } else {
                    loginToVpn();
                    showConnectProgress();
                    List<String> fallbackOrder = new ArrayList<>();
                    fallbackOrder.add(HydraTransport.TRANSPORT_ID);
                    fallbackOrder.add(CaketubeTransport.TRANSPORT_ID_TCP);
                    fallbackOrder.add(CaketubeTransport.TRANSPORT_ID_UDP);
                    List<String> bypassDomains = new LinkedList<>();
                    bypassDomains.add("*facebook.com");
                    bypassDomains.add("*wtfismyip.com");
                    UnifiedSDK.getInstance().getVPN().start(
                            new SessionConfig.Builder()
                                    .withReason(TrackingConstants.GprReasons.M_UI)
                                    .withVirtualLocation(selectedCountry)
                                    .withTransportFallback(fallbackOrder)
                                    .addDnsRule(TrafficRule.Builder.bypass().fromDomains(bypassDomains))
                                    .build(), new CompletableCallback() {
                                @Override
                                public void complete() {
                                    hideConnectProgress();
                                    startUIUpdateTask();
                                }

                                @Override
                                public void error(@NonNull VpnException e) {
                                    hideConnectProgress();
                                    updateUI();
                                    handleError(e);
                                }

                            });
                }
            }

            @Override
            public void failure(@NonNull VpnException e) {

                handleError(e);

            }


        });
    }

    @Override
    protected void disconnectFromVnp() {
//        Disconnect from vpn server...
        showConnectProgress();
        UnifiedSDK.getInstance().getVPN().stop
                (TrackingConstants.GprReasons.M_UI, new CompletableCallback() {
                    @Override
                    public void complete() {
                        hideConnectProgress();
                        stopUIUpdateTask();
                    }

                    @Override
                    public void error(@NonNull VpnException e) {
                        hideConnectProgress();
                        updateUI();
                        handleError(e);
                    }
                });
    }

    @Override
    protected void chooseServer() {

    }

    @Override
    protected void getCurrentServer(final Callback<String> callback) {
//        try to connect to the current or selected vpn...
        UnifiedSDK.getVpnState(new com.anchorfree.vpnsdk.callbacks.Callback<VPNState>() {
            @Override
            public void success(@NonNull VPNState vpnState) {
                if (state == VPNState.CONNECTED) {
                    callback.success(selectedCountry);
                }
            }

            @Override
            public void failure(@NonNull VpnException e) {
                callback.failure(PartnerRequestException.fromTransport(e));

            }
        });
    }

    @Override
    protected void checkRemainingTraffic() {

    }

  /*  @Override
    protected void checkRemainingTraffic() {
        UnifiedSDK.get.remainingTraffic(new Callback<RemainingTraffic>() {
            @Override
            public void success(RemainingTraffic remainingTraffic) {
                updateRemainingTraffic(remainingTraffic);
            }

            @Override
            public void failure(HydraException e) {
                updateUI();

                handleError(e);
            }
        });
    }
*/



}
