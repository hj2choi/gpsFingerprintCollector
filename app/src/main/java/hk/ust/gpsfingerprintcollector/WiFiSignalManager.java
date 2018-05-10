package hk.ust.gpsfingerprintcollector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import hk.ust.bean.WiFiVector;
import hk.ust.bean.ApSignal;
import hk.ust.event.WiFiSignalCollectedEvent;
import hk.ust.utils.WiFiUtils;

/**
 * Created by Steve on 18/9/2017.
 */

public class WiFiSignalManager {

    private Context mContext;
    private WifiManager mWiFiManager;
    private WiFiBroadcastReceiver mReceiver;
    private WiFiScanThread mWiFiScanThread;
    private List<WiFiVector> mWiFiVectors;
    private List<Long> mBreakPoints;
    private String mAndroidID;

    public WiFiSignalManager(Context context) {
        mContext = context;
        mWiFiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mAndroidID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        mWiFiVectors = new ArrayList<>();
        mBreakPoints = new ArrayList<>();
        mReceiver = new WiFiBroadcastReceiver();
        mWiFiScanThread = new WiFiScanThread();
        mWiFiScanThread.start();
        Log.d("WifiManager","Init()");
    }

    public void startCollection() {
        mWiFiVectors.clear();
        mBreakPoints.clear();
        mBreakPoints.add(System.currentTimeMillis()); // Starting index
        mContext.registerReceiver(mReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    public void stopCollection() {
        try {
            mContext.unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public void nextPointReached() {
        mBreakPoints.add(System.currentTimeMillis());
    }

    public void undoPointReached() {
        if (mBreakPoints.size() > 0) {
            mBreakPoints.remove(mBreakPoints.size() - 1);
        }
    }

    /*public void writeToFile(String filePath, List<Position> posList) {
        List<String> path = new ArrayList<>();
        path.add("Started:" + GeneralUtils.getUuid());
        // Store the coordinates of anchor points.
        String posStr = "";
        for (Position pos : posList) {
            posStr += pos.getX() + "," + pos.getY() + " ";
        }
        path.add(posStr);

        // Store the sample index when people reach anchor points.
        String breakPoints = "";
        for (Long index : mBreakPoints) {
            breakPoints += index + " ";
        }
        path.add(breakPoints);

        try {
            FileUtils.writeLines(path, filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<String> signalList = new ArrayList<>();
        for (WiFiVector wifi: mWiFiVectors) {
            String signalStr = "";
            for (ApSignal signal: wifi.apSignalList) {
                if ("".equals(signalStr)) {
                    signalStr += signal.getMicroTimestamp() + " ";
                }
                signalStr += signal.getApAddress() + "," + signal.getRssi() + " ";
            }
            signalList.add(signalStr);
        }

        try {
            FileUtils.writeLines(signalList, filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    public void destroy() {
        try {
            mContext.unregisterReceiver(mReceiver);
        }catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
        mWiFiScanThread.interrupt();
        mWiFiScanThread = null;
    }

    private class WiFiBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent intent) {
            WiFiVector wiFiVector = new WiFiVector();
            for (ScanResult result : mWiFiManager.getScanResults()) {
                String wifiApAdd = WiFiUtils.uniformApAddress(result.BSSID);
                wiFiVector.addApSignal(new ApSignal(wifiApAdd, result.SSID, result.level, System.currentTimeMillis()));
            }
            EventBus.getDefault().post(new WiFiSignalCollectedEvent(wiFiVector.apSignalList));
            mWiFiVectors.add(wiFiVector);

            Log.d("WifiManager","received wifi broadcast");
        }
    }

    private class WiFiScanThread extends Thread {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                mWiFiManager.startScan();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
