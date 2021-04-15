package com.app.rfid_test;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.phidget22.*;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    RFID rfid;
    Manager manager;
    int minDataInterval;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        try{

            manager = new Manager();

            //Allow direct USB connection of Phidgets
            if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST))
                com.phidget22.usb.Manager.Initialize(this);

            //Enable server discovery to list remote Phidgets
            this.getSystemService(Context.NSD_SERVICE);
            Net.enableServerDiscovery(ServerType.DEVICE_REMOTE);

            //CSCM79 Advice
            //Add a specific network server to communicate with Phidgets remotely
            Net.addServer("Asus", "192.168.1.177", 5661, "", 0);

            rfid = new RFID();

            //Enable server discovery to list remote Phidgets
            this.getSystemService(Context.NSD_SERVICE);
            Net.enableServerDiscovery(ServerType.DEVICE_REMOTE);


            rfid.addAttachListener(new AttachListener() {
                public void onAttach(final AttachEvent attachEvent) {
                    AttachEventHandler handler = new AttachEventHandler(rfid);
                    runOnUiThread(handler);
                }
            });

			rfid.addDetachListener(new DetachListener() {
                    public void onDetach(final DetachEvent detachEvent) {
                        DetachEventHandler handler = new DetachEventHandler(rfid);
                        runOnUiThread(handler);
                    }
                });

			rfid.addErrorListener(new ErrorListener() {
                    public void onError(final ErrorEvent errorEvent) {
                        ErrorEventHandler handler = new ErrorEventHandler(rfid, errorEvent);


                    }
                });

			rfid.addTagListener(new RFIDTagListener() {
                    public void onTag(RFIDTagEvent tagEvent) {
                        RFIDTagEventHandler handler = new RFIDTagEventHandler(rfid, tagEvent);
                        runOnUiThread(handler);
                    }
                });

			rfid.addTagLostListener(new RFIDTagLostListener() {
                    public void onTagLost(RFIDTagLostEvent tagLostEvent) {
                        RFIDTagLostEventHandler handler = new RFIDTagLostEventHandler(rfid, tagLostEvent);
                        runOnUiThread(handler);
                    }
                });

			rfid.open();

            }
        catch(PhidgetException e)
        {
            e.printStackTrace();
        }
    }


    class AttachEventHandler implements Runnable{

        Phidget ch;

        public AttachEventHandler(Phidget ch) {
            this.ch = ch;
        }

        public void run() {

            TextView attachedTxt = (TextView) findViewById(R.id.attached);

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
            }

            //notify that we're done
//            synchronized(this)
//            {
//                this.notify();
//            }
        }

    }


    class DetachEventHandler implements Runnable{

        Phidget ch;
        public DetachEventHandler(Phidget ch) {
            this.ch = ch;
        }
        public void run() {
            TextView nameTxt = (TextView) findViewById(R.id.phidget);
            TextView serialTxt = (TextView) findViewById(R.id.serialText);
            TextView attachedTxt = (TextView) findViewById(R.id.attached);
            attachedTxt.setText("Phidget Detachted");
            nameTxt.setText("");
            serialTxt.setText("");
            ((TextView)findViewById(R.id.tagValue)).setText("");
            ((TextView)findViewById(R.id.prototcolValue)).setText("");
        }
    }

    class RFIDTagEventHandler implements Runnable{
        Phidget ch;
        RFIDTagEvent tagEvent;

        public RFIDTagEventHandler(Phidget ch, RFIDTagEvent tagEvent) {
            this.ch = ch;
            this.tagEvent = tagEvent;
        }

        public void run() {
            TextView tagTxt = (TextView)findViewById(R.id.tagValue);
            TextView protocolTxt = (TextView)findViewById(R.id.prototcolValue);

            tagTxt.setText(tagEvent.getTag());
            protocolTxt.setText(tagEvent.getProtocol().getMessage());

        }
    }

    class RFIDTagLostEventHandler implements Runnable{
        Phidget ch;
        RFIDTagLostEvent tagLostEvent;

        public RFIDTagLostEventHandler(Phidget ch, RFIDTagLostEvent tagLostEvent) {
            this.ch = ch;
            this.tagLostEvent = tagLostEvent;
        }

        public void run() {
            TextView tagTxt = (TextView)findViewById(R.id.tagValue);
            TextView protocolTxt = (TextView)findViewById(R.id.prototcolValue);

            tagTxt.setText("");
            protocolTxt.setText("");
        }
    }


    class ErrorEventHandler {
        Phidget ch;
        ErrorEvent errorEvent;

        public ErrorEventHandler(Phidget ch, ErrorEvent errorEvent) {
            this.ch = ch;
            this.errorEvent = errorEvent;
        }


    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            rfid.close();

        } catch (PhidgetException e) {
            e.printStackTrace();
        }

        //Disable USB connection to Phidgets
        if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST))
            com.phidget22.usb.Manager.Uninitialize();
    }

}