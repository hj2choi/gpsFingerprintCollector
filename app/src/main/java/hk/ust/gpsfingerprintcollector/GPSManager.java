package hk.ust.gpsfingerprintcollector;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import hk.ust.bean.Position;
import hk.ust.event.GPSCollectedEvent;
import hk.ust.utils.FileUtils;
import hk.ust.utils.GPSUtils;
import hk.ust.utils.GeneralUtils;

/**
 * Created by hjchoi on 12/15/2017.
 */

public class GPSManager {

    public static final int MINIMUM_SATELLITE_SNR = 5;
    public static final int MINIMUM_SATELLITES_COUNT = 0;
    public static final int MINIMUM_GPS_UPDATE_MILLIS = 2000;

    private Context context;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private GpsStatusListener mLegacyGpsStatusListener;
    private GpsStatus mLegacyStatus;

    private int satellitesCount;
    private int satellitesInFix;
    private int rawSatellitesInFix;
    private float averageSNR;

    private boolean mCollectionStarted;
    private ArrayList<Location> mCollectedLocations;

    private class GpsStatusListener implements GpsStatus.Listener{
        @Override
        public void onGpsStatusChanged(int event) {
            //Log.d("Debug:LocationService: ","OnGPSStatusChanged()");
            try {
                mLegacyStatus = mLocationManager.getGpsStatus(mLegacyStatus);
                satellitesCount = 0;
                //int timetofix = mLocationManager.getGpsStatus(null).getTimeToFirstFix();
                satellitesInFix = 0;
                rawSatellitesInFix = 0;
                averageSNR = 0;
                for (GpsSatellite sat : mLocationManager.getGpsStatus(null).getSatellites()) {
                    if(sat.usedInFix()) {
                        rawSatellitesInFix++;
                        averageSNR+=sat.getSnr();
                        if (sat.getSnr() > MINIMUM_SATELLITE_SNR) {
                            satellitesInFix++;
                        }
                    }
                    satellitesCount++;
                }
                if (satellitesInFix>0)
                    averageSNR /= satellitesInFix;
            }
            catch(SecurityException e) {
            }
        }
    }

    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            // Called when a new location is found by the gps location provider.

            Bundle extras = new Bundle();
            extras.putInt("satellitesInFix",satellitesInFix);
            extras.putInt("rawSatellitesInFix", rawSatellitesInFix);
            extras.putInt("satellitesCount",satellitesCount);
            extras.putFloat("averageSNR",averageSNR);
            location.setExtras(extras);
            /*Toast.makeText((Activity)context,
                    GPSUtils.getLocationText(location)+(" [sat="+location.getExtras().getInt("satellitesInFix")+"]"),
                    Toast.LENGTH_SHORT).show();*/
            if (satellitesInFix>=MINIMUM_SATELLITES_COUNT) {
                EventBus.getDefault().post(new GPSCollectedEvent(location));
                if (mCollectionStarted) {
                    mCollectedLocations.add(location);
                }
            }

        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    }

    public GPSManager(Context context) {
        this.context = context;

        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = new MyLocationListener();
        mLegacyGpsStatusListener = new GpsStatusListener();

        satellitesInFix = 0;
        rawSatellitesInFix = 0;
        averageSNR = 0;
        mCollectionStarted = false;
        mCollectedLocations = new ArrayList<>();

        mLocationManager.addGpsStatusListener(mLegacyGpsStatusListener);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MINIMUM_GPS_UPDATE_MILLIS, 0, mLocationListener);
            Log.i("GPSManager", "Requested location updates");
        } else {
            Log.e("GPSManager", "Location permission failed");
        }

    }

    public void startCollection() {
        mCollectionStarted = true;
    }

    public void stopCollectionAndSave(String filePath, List<Position> posList) {
        mCollectionStarted = false;
        //Log.d("GPSManager:","stopCollectionAndSave(): gps list size = "+mCollectedLocations.size());
        List<String> gpsLocationList = new ArrayList<>();
        gpsLocationList.add("Started:" + GeneralUtils.getUuid());
        for (Location location: mCollectedLocations) {
            String locationStr = "";
            try {
                locationStr = (GPSUtils.gps2Json(location)).toString();
            } catch (JSONException e) {
                Log.e("GPSManager:","GPSUtils.gps2Json() JSON parse error");
            }
            gpsLocationList.add(locationStr);
        }
        try {
            FileUtils.writeLines(gpsLocationList, filePath);
        } catch (Exception e) {
            Log.e("GPSManager:","Error on saving files");
        } finally {
            mCollectedLocations.clear();
        }
        //Log.d("GPSManager:","stopCollectionAndSave(): successfully saved");
    }
}
