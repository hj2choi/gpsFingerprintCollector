package hk.ust.event;

import java.util.List;
import hk.ust.bean.ApSignal;

/**
 * Created by Steve on 18/9/2017.
 */

public class WiFiSignalCollectedEvent {

    private List<ApSignal> mApSignalList;

    public WiFiSignalCollectedEvent(List<ApSignal> signalList) {
        mApSignalList = signalList;
    }

    public List<ApSignal> getApSignalList() {
        return mApSignalList;
    }
}
