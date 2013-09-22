package net.qvex.dommel.data;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

/**
 * Created by dvhaeren on 09/08/13.
 */
public class DataServiceResultReceiver extends ResultReceiver {

    private Receiver mReceiver;

    public DataServiceResultReceiver(Handler handler) {
            super(handler);
    }

    public void setReceiver(Receiver receiver) {
        mReceiver = receiver;
    }

    public interface Receiver {
        public void onReceiveResult(int resultCode, Bundle resultData);
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (mReceiver != null) {
            mReceiver.onReceiveResult(resultCode, resultData);
        }
    }
}
