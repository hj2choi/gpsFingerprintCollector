package hk.ust.gpsfingerprintcollector;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import hk.ust.bean.ApSignal;
import hk.ust.event.GPSCollectedEvent;
import hk.ust.event.WiFiSignalCollectedEvent;
import hk.ust.utils.FileUtils;
import hk.ust.utils.HttpJSONUtils;

/**
 * Created by HongJoon on 21-Apr-18.
 */

public class DataCollectionManager {

    private static final String SERVER_UPLOAD_URL = "http://eek123.ust.hk:2989/api/v1/add_record";

    private Context context;
    private WiFiSignalManager mWifiSignalManager;
    private GPSManager mGPSManager;

    private WiFiSignalCollectedEvent lastReceivedWifiEvent;
    private long lastReceivedWifiTimestamp;
    private GPSCollectedEvent lastReceivedGPSEvent;
    private long lastReceivedGPSTimestamp;

    private ArrayList<WiFiSignalCollectedEvent> wifiRecordsBuffer;
    private ArrayList<GPSCollectedEvent> gpsRecordsBuffer;

    private long lastUploadTime;
    Toast mPrevToast;

    private boolean collectionStarted;
    private String currentFilePath;

    public DataCollectionManager(Context context) {
        this.context = context;
        EventBus.getDefault().register(this);

        lastReceivedWifiEvent = null;
        lastReceivedWifiTimestamp = 0;
        lastReceivedGPSEvent = null;
        lastReceivedGPSTimestamp = 0;
        wifiRecordsBuffer = new ArrayList<WiFiSignalCollectedEvent>();
        gpsRecordsBuffer = new ArrayList<GPSCollectedEvent>();
        lastUploadTime = 0;

        mWifiSignalManager = new WiFiSignalManager(context);
        mWifiSignalManager.startCollection();
        mGPSManager = new GPSManager(context);

        lastUploadTime = 0;
        mPrevToast = null;

        collectionStarted = false;

        //mGPSManager.startCollection();
        Log.d("DataCollectionManager", "Init()");
    }


    @Subscribe
    public void OnWifiSignalCollected(WiFiSignalCollectedEvent event) {
        //Log.d("DataCollectionManager", "wifi received "+System.currentTimeMillis());
        lastReceivedWifiEvent = event;
        lastReceivedWifiTimestamp = System.currentTimeMillis();
    }


    @Subscribe
    public void OnGPSSignalCollected(GPSCollectedEvent event) {
        //Log.d("DataCollectionManager", "GPS received");

        // no GPS data yet
        if (lastReceivedGPSEvent==null) {
            lastReceivedGPSEvent = event;
            lastReceivedGPSTimestamp = System.currentTimeMillis();
            return;
        }
        // ignore data
        if (lastReceivedGPSEvent!=null &&
                (//event.getLocation().distanceTo(lastReceivedGPSEvent.getLocation()) == 0 ||  // too close from last fingerprint
                    Math.abs(System.currentTimeMillis()-lastReceivedWifiTimestamp) > 2000// || // wifi and gps not up to date
                    //event.getLocation().getAccuracy() > 80 // imperfect label
                    //Math.abs(System.currentTimeMillis()-lastReceivedGPSTimestamp) > 2000   // too frequent upload
                )
        ) {
            lastReceivedGPSEvent = event;
            lastReceivedGPSTimestamp = System.currentTimeMillis();
            return;
        }

        // notification
        if (mPrevToast !=null) {
            mPrevToast.cancel();
            mPrevToast = null;
        }
        mPrevToast = Toast.makeText((Activity)context,
                (int)event.getLocation().getAltitude()+(" [sat="+event.getLocation().getExtras().getInt("satellitesInFix")+"] acc="+event.getLocation().getAccuracy()+" uploads:"+wifiRecordsBuffer.size()),
                Toast.LENGTH_SHORT);
        mPrevToast.show();


        if (collectionStarted) {
            wifiRecordsBuffer.add(lastReceivedWifiEvent);
            gpsRecordsBuffer.add(event);
        }

        if (System.currentTimeMillis() - 5000 > lastUploadTime){
            uploadData();
            lastUploadTime = System.currentTimeMillis();
        }

        lastReceivedGPSEvent = event;
        lastReceivedGPSTimestamp = System.currentTimeMillis();
    }

    private JSONObject toJSON(ArrayList<WiFiSignalCollectedEvent> wifiRecordsBuffer, ArrayList<GPSCollectedEvent> gpsRecordsBuffer) {
        JSONObject resultJSON = null;
        Log.d("DataCollectionManager","toJSON()");
        try {
            resultJSON = new JSONObject();
            JSONArray jsonDataArray = new JSONArray();

            for (int i=0; i<wifiRecordsBuffer.size(); ++i) {
                JSONArray jsonWifiArray = new JSONArray();
                ArrayList<ApSignal> wifiList = (ArrayList) wifiRecordsBuffer.get(i).getApSignalList();
                for (ApSignal w: wifiList) {
                    jsonWifiArray.put(new JSONObject().put("macAddress", w.getApAddress())
                            .put("RSSI", w.getRssi())
                    );
                }

                Location loc = gpsRecordsBuffer.get(i).getLocation();
                jsonDataArray.put(new JSONObject().put("timestamp", loc.getTime())
                        .put("latitude", loc.getLatitude())
                        .put("longitude", loc.getLongitude())
                        .put("altitude", loc.getAltitude())
                        .put("accuracy", loc.getAccuracy())
                        .put("satellites", loc.getExtras().getInt("satellitesInFix"))
                        .put("ave_snr", loc.getExtras().getFloat("averageSNR"))
                        .put("wifiAPs", jsonWifiArray)
                );
            }
            resultJSON.put("data",jsonDataArray);
        } catch (Exception e) {
            Log.d("Debug:LocationService:","JSON parse error:"+e);
            e.printStackTrace();

        }

        return resultJSON;
    }

    private List<String> toJSONLines(ArrayList<WiFiSignalCollectedEvent> wifiRecordsBuffer, ArrayList<GPSCollectedEvent> gpsRecordsBuffer) {
        List<String> lines = new ArrayList<String>();
        Log.d("DataCollectionManager","toJSON()");
        try {

            for (int i=0; i<wifiRecordsBuffer.size(); ++i) {
                JSONArray jsonWifiArray = new JSONArray();
                ArrayList<ApSignal> wifiList = (ArrayList) wifiRecordsBuffer.get(i).getApSignalList();
                for (ApSignal w: wifiList) {
                    jsonWifiArray.put(new JSONObject().put("macAddress", w.getApAddress())
                            .put("RSSI", w.getRssi())
                    );
                }

                Location loc = gpsRecordsBuffer.get(i).getLocation();
                lines.add(new JSONObject().put("timestamp", loc.getTime())
                        .put("latitude", loc.getLatitude())
                        .put("longitude", loc.getLongitude())
                        .put("altitude", loc.getAltitude())
                        .put("accuracy", loc.getAccuracy())
                        .put("satellites", loc.getExtras().getInt("satellitesInFix"))
                        .put("ave_snr", loc.getExtras().getFloat("averageSNR"))
                        .put("wifiAPs", jsonWifiArray).toString()
                );
            }
        } catch (Exception e) {
            Log.d("Debug:LocationService:","JSON parse error:"+e);
            e.printStackTrace();

        }

        return lines;
    }

    public boolean isCollectionStarted() {
        return collectionStarted;
    }

    public void startCollection() {
        collectionStarted = true;
        currentFilePath = FileUtils.generateFilePath();
        Toast.makeText((Activity)context,
                "start write to "+currentFilePath,
                Toast.LENGTH_SHORT).show();
    }

    public void stopCollectionAndSave() {
        uploadData();
        collectionStarted = false;
        currentFilePath = null;
    }

    private void uploadData() {
        if (wifiRecordsBuffer.size()==0 || currentFilePath == null) {
            return;
        }
        Toast.makeText((Activity)context,
                "write to "+currentFilePath,
                Toast.LENGTH_SHORT).show();

        try {
            FileUtils.writeLines(toJSONLines(wifiRecordsBuffer, gpsRecordsBuffer), currentFilePath);
        } catch (Exception e) {
            Toast.makeText((Activity)context,
                    "write failed: "+e,
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        /*HttpJSONUtils.sendJSONRequest(SERVER_UPLOAD_URL,
                toJSON(wifiRecordsBuffer, gpsRecordsBuffer), new HttpJSONUtils.ResponseHandler() {
                    @Override
                    public void callback(int responseCode, String response) {
                        Log.d("Debug:LocationService:","response from server: "+response);
                        Toast.makeText((Activity)context,
                                ("response from server: "+response),
                                Toast.LENGTH_SHORT).show();

                        //in case of upload failure, keep buffer in memory for another shot.
                        if (response.contains("OK")) {
                            wifiRecordsBuffer.clear();
                            gpsRecordsBuffer.clear();
                        }
                    }
                }
        );*/

        wifiRecordsBuffer.clear();
        gpsRecordsBuffer.clear();
    }

}
