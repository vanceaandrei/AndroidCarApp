package com.example.clash.rccarapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.lang.ref.WeakReference;

public class MainActivity extends Activity implements SensorEventListener  {

    private float lastX, lastY, lastZ;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private long lastUpdate = 0;
    private static final int SHAKE_THRESHOLD = 600;

    private BtuletoothConnection bl = null;

    private Button btn_forward,btn_backward,btn_left,btn_right;

    private String address;
    private boolean BT_is_connect;
//    private String command;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Intent in = getIntent();
        address = in.getStringExtra("address");

        loadPref();

        bl = new BtuletoothConnection(this, mHandler);
        bl.checkBTState();

        setButtons();

        mHandler.postDelayed(sRunnable, 600000);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            // success! we have an accelerometer

            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
//            vibrateThreshold = accelerometer.getMaximumRange() / 2;
        } else {
            // fai! we dont have an accelerometer!
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            long curTime = System.currentTimeMillis();

            if(x>5.0) { //lean phone left side
                bl.sendData("L");
            }else if (x<-5.0){  //lean phone right side
                bl.sendData("R");
            }

            if(y<-4.0) {    //lean phone forward
                bl.sendData("F");
            }else if(y>4.0){    //lean phone backward
                bl.sendData("B");
            }
            if((x>-5.0 && x<5.0) && (y<4.0 && y>-4.0)){
                bl.sendData("S");
            }
            if ((curTime - lastUpdate) > 100) {
                lastUpdate = curTime;

                lastX = x;
                lastY = y;
                lastZ = z;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case BtuletoothConnection.BL_NOT_AVAILABLE:
                        Log.d(BtuletoothConnection.TAG, "Bluetooth is not available. Exit");
                        Toast.makeText(activity.getBaseContext(), "Bluetooth is not available", Toast.LENGTH_SHORT).show();
                        activity.finish();
                        break;
                    case BtuletoothConnection.BL_INCORRECT_ADDRESS:
                        Log.d(BtuletoothConnection.TAG, "Incorrect MAC address");
                        Toast.makeText(activity.getBaseContext(), "Incorrect Bluetooth address", Toast.LENGTH_SHORT).show();
                        break;
                    case BtuletoothConnection.BL_REQUEST_ENABLE:
                        Log.d(BtuletoothConnection.TAG, "Request Bluetooth Enable");
                        BluetoothAdapter.getDefaultAdapter();
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        activity.startActivityForResult(enableBtIntent, 1);
                        break;
                    case BtuletoothConnection.BL_SOCKET_FAILED:
                        Toast.makeText(activity.getBaseContext(), "Socket failed", Toast.LENGTH_SHORT).show();
                        //activity.finish();
                        break;
                }
            }
        }
    }

    private final MyHandler mHandler = new MyHandler(this);

    private final static Runnable sRunnable = new Runnable() {
        public void run() { }
    };

    private void loadPref(){
        SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        address = mySharedPreferences.getString("pref_MAC_address", address);			// the first time we load the default values (������ ��� ��������� ��������� ��������)
    }

    @Override
    protected void onResume() {
        super.onResume();
        BT_is_connect = bl.BT_Connect(address, false);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    protected void onPause() {
        super.onPause();
        bl.BT_onPause();
        sensorManager.unregisterListener(this);

    }
    @Override
     protected void onDestroy() {
        //sensorManager.unregisterListener(this);
        bl.BT_onPause();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        loadPref();
    }

    private void setButtons(){
        btn_forward = (Button) findViewById(R.id.forward);
        btn_backward = (Button) findViewById(R.id.backward);
        btn_left = (Button) findViewById(R.id.left);
        btn_right = (Button) findViewById(R.id.right);

        btn_forward.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    if (BT_is_connect) {
                        sendData("F");

                        Log.e("TRANSMISSION","Sending .....F.............");
                    }
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {

                        if (BT_is_connect) {
                            sendData("S");
                            Log.e("TRANSMISSION", "Sending .....S.............");
                        }
                    }
                return false;
            }
        });

        btn_backward.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    if (BT_is_connect) {
                        sendData("B");
                        Log.e("TRANSMISSION", "Sending .....B.............");
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP) {

                    if (BT_is_connect) {
                        sendData("S");
                        Log.e("TRANSMISSION", "Sending .....S.............");

                    }

                }
                return false;
            }
        });
        btn_left.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (BT_is_connect) {
                        sendData("L");
                        Log.e("TRANSMISSION", "Sending .....L.............");
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP) {

                    if (BT_is_connect) {
                        sendData("S");
                        Log.e("TRANSMISSION", "Sending .....S.............");
                    }
                }
                return false;
            }
        });
        btn_right.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    if (BT_is_connect) {
                        sendData("R");
                        Log.e("TRANSMISSION", "Sending .....R.............");
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP) {

                    if (BT_is_connect) {
                        sendData("S");
                        Log.e("TRANSMISSION", "Sending .....S.............");
                    }
                }
                return false;
            }
        });
    }
    private void sendData(String message){
        bl.sendData(String.valueOf(message));
    }
}
