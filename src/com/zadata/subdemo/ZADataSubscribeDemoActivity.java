package com.zadata.subdemo;

import java.io.IOException;
import java.net.Socket;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

public class ZADataSubscribeDemoActivity extends Activity {
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final TextView tv = new TextView(this);
        Handler handler = new Handler() {
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                tv.setText(bundle.getString("MSG"));
            }
        };
        //Log.i("DEBUG", e.toString());
        tv.setText("Hello, Android");
        setContentView(tv);
        new Thread(new PahoClass(handler)).start();
    }
}

class NetworkTestThread implements Runnable {
    Handler handler;
    private Bundle bundle = new Bundle();

    public NetworkTestThread(Handler handler) {
        this.handler = handler;
    }

    public void run() {
        try {
            new Socket("mqtt.zadata.com", 1883);
            updateText("Connected via plain socket");
        }
        catch (IOException ee) {
            updateText("Failed3 via plain socket: " + ee.getMessage());             
        }
    }

    void updateText(String str) {
        Message m = Message.obtain();
        bundle.clear();
        bundle.putString("MSG", str);
        m.setData(bundle);
        handler.sendMessage(m);
    }
}

class PahoClass implements MqttCallback, Runnable {
    Handler handler;
    MqttClient client;

    static final string MQTT_USER = "your_mqtt_username";
    static final string MQTT_PWD  = "your_mqtt_password";
    static final int MQTT_KEEP_ALIVE_IN_SECS = 25;
    static final int RECONNECT_INTERVAL_MS = 1000; 

    public PahoClass(Handler handler) {
        this.handler = handler;
    }

    void connectClient() {
        try {
            /* check which mqtt client line fails */
            /*SocketFactory factory = SocketFactory.getDefault();
            try {
                Socket so = factory.createSocket("mqtt.zadata.com", 1883);
                so.setTcpNoDelay(true);
            } catch (Exception e) {
                Log.i("DEBUG", e.toString());
                for (StackTraceElement elem : e.getStackTrace()) {
                    Log.i("DEBUG", elem.toString());
                }               
            }*/
            MqttClient client = new MqttClient("tcp://mqtt.zadata.com:1883", "paho000", null);
            client.setCallback(this);
            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setKeepAliveInterval(MQTT_KEEP_ALIVE_IN_SECS);
            opts.setUserName(MQTT_USER);
            opts.setPassword(MQTT_PWD.toCharArray());
            client.connect(opts);
            client.subscribe("paho_test", 0);       
        } catch (MqttException e) {
            updateText("Failed to connect mqtt: " + e.getReasonCode() + " " + e.getCause());
            for (StackTraceElement elem : e.getStackTrace()) {
                Log.i("DEBUG", elem.toString());
            }
            try { Thread.sleep(RECONNECT_INTERVAL_MS); } catch (InterruptedException ee) {};
            updateText("Retrying...");
            connectClient();
        }       
    }

    public void run(){
        connectClient();
    }
    
    @Override
    public void connectionLost(Throwable arg0) {
        updateText("Connection lost");
        try { Thread.sleep(RECONNECT_INTERVAL_MS); } catch (InterruptedException e) {};
        updateText("Reconnecting...");
        connectClient();
    }

    @Override
    public void deliveryComplete(MqttDeliveryToken arg0) {
    }

    @Override
    public void messageArrived(MqttTopic arg0, MqttMessage arg1)
            throws Exception {
        
        updateText("Got message to topic " + arg0.getName() + ", " + arg1.toString());
    }

    void updateText(String str) {
        Message m = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.clear();
        bundle.putString("MSG", str);
        m.setData(bundle);
        handler.sendMessage(m);
    }
}
