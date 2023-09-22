package com.dreamexample.android.weatherdataviewer.tasks;

import static com.dreamexample.android.weatherdataviewer.functions.MyLogging.DEBUG_OUT;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import com.dreamexample.android.weatherdataviewer.constants.RequestDevice;

/**
 Active Network connectivity judgment utility.
*/
public class NetworkUtil {
    private static final String TAG = "NetworkUtil";

    private NetworkUtil() {}

    public static RequestDevice getActiveNetworkDevice(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context
                .getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        DEBUG_OUT.accept(TAG, "Build.VERSION.SDK_INT: " + Build.VERSION.SDK_INT);
        // https://developer.android.com/training/basics/network-ops/reading-network-state?hl=ja
        Network currentNetwork = manager.getActiveNetwork();
        NetworkCapabilities caps = manager.getNetworkCapabilities(currentNetwork);
        DEBUG_OUT.accept(TAG, "NetworkCapabilities: " + caps);
        if (caps != null) {
            boolean isWifiConnect = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
            boolean isMobileConnect = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
            DEBUG_OUT.accept(TAG, "isWifiConnect: " + isWifiConnect);
            DEBUG_OUT.accept(TAG, "isMobileConnect: " + isMobileConnect);
            if (isWifiConnect) {
                return RequestDevice.WIFI;
            }
            if (isMobileConnect) {
                return RequestDevice.MOBILE;
            }
        }

        return RequestDevice.NONE;
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context
                .getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        DEBUG_OUT.accept(TAG, "NetworkInfo: " + info);
        return (info != null && info.isConnected());
    }

}
