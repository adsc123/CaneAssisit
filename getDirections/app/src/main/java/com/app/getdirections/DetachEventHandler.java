package com.app.getdirections;

import android.util.Log;

import com.phidget22.Phidget;

public class DetachEventHandler implements Runnable{
    private final String TAG = MapsActivity.class.getSimpleName();
    Phidget ch;
    public DetachEventHandler(Phidget ch) {
        this.ch = ch;
    }
    public void run() {
        Log.i(TAG, "****** DetachEventHandler ********");
    /*        TextView nameTxt = (TextView) findViewById(R.id.phidget);
            TextView serialTxt = (TextView) findViewById(R.id.serialText);
            TextView attachedTxt = (TextView) findViewById(R.id.attached);
            attachedTxt.setText("Phidget Detachted");
            nameTxt.setText("");
            serialTxt.setText("");
            ((TextView)findViewById(R.id.tagValue)).setText("");
            ((TextView)findViewById(R.id.prototcolValue)).setText("");*/
    }
}
