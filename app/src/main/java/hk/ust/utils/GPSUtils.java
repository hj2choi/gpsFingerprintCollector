package hk.ust.utils;

import android.content.Context;
import android.location.Location;
import android.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by hjchoi on 11/10/2017.
 */

public class GPSUtils {
    public static JSONObject gps2Json(Location location) throws JSONException {
        return new JSONObject().put("timestamp", location.getTime())
                .put("latitude", location.getLatitude())
                .put("longitude", location.getLongitude())
                .put("altitude", location.getAltitude())
                .put("accuracy", location.getAccuracy())
                .put("satellites", location.getExtras().getInt("satellitesInFix"))
                .put("ave_snr", location.getExtras().getFloat("averageSNR"));
    }

    /*
        UTILS
     */
    public static final String KEY_REQUESTING_LOCATION_UPDATES = "requesting_locaction_updates";

    /**
     * Returns true if requesting location updates, otherwise returns false.
     *
     * @param context The {@link Context}.
     */
    public static boolean requestingLocationUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_REQUESTING_LOCATION_UPDATES, false);
    }

    /**
     * Stores the location updates state in SharedPreferences.
     * @param requestingLocationUpdates The location updates state.
     */
    public static void setRequestingLocationUpdates(Context context, boolean requestingLocationUpdates) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_REQUESTING_LOCATION_UPDATES, requestingLocationUpdates)
                .apply();
    }

    /*public void writeToFile(Location location) {
        try {
            String jsonString = new JSONObject().put("latitude", location.getLatitude())
                    .put("timestamp", location.getTime())
                    .put("longitude", location.getLongitude())
                    .put("altitude", location.getAltitude())
                    .put("accuracy", location.getAccuracy()).toString();
            Log.d("Debug:LocationService: ",FileUtils.getGPSFilePath(this));
            FileUtils.writeLine(jsonString, FileUtils.getGPSFilePath(this));
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("Debug:LocationService: ","error writing to file");
        }
    }*/

    /**
     * Returns the {@code location} object as a human readable string.
     * @param location  The {@link Location}.
     */
    public static String getLocationText(Location location) {
        return location == null ? "Unknown location" :
                "(" + location.getLatitude() + ", " + location.getLongitude() + "), acc = " + location.getAccuracy();
    }

    public static String getLocationTitle(Context context) {
        return "Location updated: "+ DateFormat.getDateTimeInstance().format(new Date());
    }
}
