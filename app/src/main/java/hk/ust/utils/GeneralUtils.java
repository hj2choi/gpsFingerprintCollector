package hk.ust.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;

import java.util.Date;

/**
 * Created by Steve on 27/9/2017.
 */

public class GeneralUtils {

    public static String buildingID;//UUID = buildingID_floorID_deviceID_startX_startY_endX_endY_timestamp1
    public static String floorID;
    public static String deviceID;
    public static String startX_startY;
    public static String endX_endY;
    public static String timestamp;
    private static int screenW = -1, screenH = -1;

    public static int getScreenW(Context context) {
        if (screenW < 0) {
            initScreenDisplayParams(context);
        }
        return screenW;
    }

    public static int getScreenH(Context context) {
        if (screenH < 0) {
            initScreenDisplayParams(context);
        }
        return screenH;
    }

    private static void initScreenDisplayParams(Context context) {
        DisplayMetrics dm = context.getApplicationContext().getResources().getDisplayMetrics();
        screenW = dm.widthPixels;
        screenH = dm.heightPixels;
    }

    public static String getCurrentTimeString() {
        return DateFormat.format("yyyy_MM_dd_HH_mm_ss", new Date()).toString();
    }

    public static Drawable resize(Context context, Drawable image, int size) {
        Bitmap b = ((BitmapDrawable)image).getBitmap();
        Bitmap bitmapResized = Bitmap.createScaledBitmap(b, size, size, false);
        return new BitmapDrawable(context.getApplicationContext().getResources(), bitmapResized);
    }

    public static String getUuid(){
        return buildingID + "_" + floorID + "_" +deviceID + "_" +startX_startY + "_" +endX_endY + "_" +timestamp;
    }

}
