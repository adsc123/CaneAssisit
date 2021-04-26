package com.app.getdirections;

import android.util.Log;

import com.phidget22.Phidget;

public class AttachEventHandler implements Runnable{

    private final String TAG = MapsActivity.class.getSimpleName();
    Phidget ch;

    public AttachEventHandler(Phidget ch) {
        this.ch = ch;
    }
    public void run() {
        Log.i(TAG, "****** AttachEventHandler ********");
           /* TextView attachedTxt = (TextView) findViewById(R.id.attached);
            attachedTxt.setText("Attached");
            try {
                TextView nameTxt = (TextView) findViewById(R.id.phidget);
                TextView serialTxt = (TextView) findViewById(R.id.serialText);
                TextView tagTxt = (TextView) findViewById(R.id.tagValue);
                TextView protocolTxt = (TextView) findViewById(R.id.prototcolValue);
                nameTxt.setText(ch.getDeviceName());
                serialTxt.setText(Integer.toString(ch.getDeviceSerialNumber()));
                tagTxt.setText("");
                protocolTxt.setText("");
            } catch (PhidgetException e) {
                e.printStackTrace();
            }*/
    }
}
