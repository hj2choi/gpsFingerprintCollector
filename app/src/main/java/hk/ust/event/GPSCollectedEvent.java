package hk.ust.event;

import android.location.Location;

/**
 * Created by hjchoi on 12/16/2017.
 */

public class GPSCollectedEvent {

    private Location mCollectedLocation;

    public GPSCollectedEvent(Location location){mCollectedLocation = location;}

    public Location getLocation(){return mCollectedLocation;}

}
