package com.maxchehab.fingerprinter;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Timer;
import java.util.TimerTask;

import static com.maxchehab.fingerprinter.FingerprintActivity.authenticate;
import static com.maxchehab.fingerprinter.FingerprintActivity.authenticateLock;

/**
 * Created by maxchehab on 7/17/17.
 */

public class ServerService extends Service {

    public int counter = 0;

    private Timer timer;
    private TimerTask timerTask;

    private static SharedPreferences sharedPreferences;

    private static Context applicationContext;

    private static int notificationCounter;

    private static boolean connected = false;
    private static int currentClient = 0;

    static ServerSocket serverSocket;


    public ServerService(Context applicationContext){
        super();

        Log.i("ServerService","initialized");
    }

    public ServerService(){
        Log.i("ServerService","initialized default");
    }

    private static class ServerInitializer extends Thread{
        public void run(){
            int clientNumber = 0;
            try {
                serverSocket = new ServerSocket(61597,1);
                try{
                    while(true){
                        new ServerListener(serverSocket.accept(),clientNumber++).start();
                    }
                }finally {
                    serverSocket.close();
                }
            }catch(IOException e){
                e.printStackTrace();
            }

        }
    }

    private static class ServerListener extends Thread{
        private Socket socket;
        private int clientNumber;

        public ServerListener(Socket socket, int clientNumber){
            this.socket = socket;
            this.clientNumber = clientNumber;

            Log.i("ServerListener","New connection with client #" + clientNumber + " @ " + socket);
        }

        public void run(){

            try{

                String hardwareID =  Settings.Secure.getString(
                        applicationContext.getContentResolver(),
                        Settings.Secure.ANDROID_ID);
                String deviceName = BluetoothAdapter.getDefaultAdapter().getName();


                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);


                if(connected){
                    try{
                        writer.println("{\"success\":false,\"message\":\"i am already connected\"}");
                        socket.close();
                    }catch(IOException e){
                        e.printStackTrace();
                    }
                    return;
                }else{
                    currentClient = clientNumber;
                    connected = true;
                    writer.println("{\"success\":true,\"hardwareID\":\"" + hardwareID + "\",\"deviceName\":\"" + deviceName + "\"}");
                }

                while(true){
                    String input = reader.readLine();
                    if(input == null){
                        break;
                    }

                    Log.i("ServerListener","client #" + clientNumber + " responded with the message {" + input + "}");

                    try {
                        JsonParser jp = new JsonParser();
                        JsonElement root = jp.parse(input);
                        JsonObject rootobj = root.getAsJsonObject();

                        String applicationID = null;
                        String uniqueKey = null;
                        String label = null;



                        switch (rootobj.get("command").getAsString()) {
                            case "knock-knock":
                                writer.println("{\"success\":true,\"hardwareID\":\"" + hardwareID + "\",\"deviceName\":\"" + deviceName + "\"}");
                                break;
                            case "pair":
                                applicationID = rootobj.get("applicationID").getAsString();
                                label = rootobj.get("label").getAsString();
                                uniqueKey = bin2hex(getHash(rootobj.get("salt").getAsString() + hardwareID));

                                if(sharedPreferences.contains(applicationID)){
                                    writer.println("{\"success\":false,\"message\":\"already paired\"}");
                                    break;
                                }

                                boolean pairResponse = authenticate(applicationID,"pair",label);
                                if(pairResponse){
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString(applicationID, "{\"uniqueKey\":\"" + uniqueKey + "\",\"label\":\"" + label + "\"}");
                                    editor.commit();
                                }
                                writer.println("{\"success\":" + pairResponse + ",\"message\":\"ran pair\",\"uniqueKey\":\"" + uniqueKey + "\",\"hardwareID\":\"" + hardwareID + "\"}");

                                synchronized (authenticateLock) {
                                    authenticateLock.notify();
                                }
                                break;
                            case "authenticate":
                                applicationID = rootobj.get("applicationID").getAsString();
                                if(!sharedPreferences.contains(applicationID)){
                                    writer.println("{\"success\":false,\"message\":\"i do not know that applicationID\"}");
                                    break;
                                }

                                String data = sharedPreferences.getString(applicationID,null);
                                JsonParser jsonParser = new JsonParser();
                                JsonElement jsonElement = jsonParser.parse(data);
                                JsonObject jsonObject = jsonElement.getAsJsonObject();

                                uniqueKey = jsonObject.get("uniqueKey").getAsString();

                                boolean response = authenticate(applicationID, "authenticate",null);
                                writer.println("{\"success\":" + response + ",\"message\":\"ran authentication\",\"uniqueKey\":\"" + uniqueKey + "\"}");

                                synchronized (authenticateLock) {
                                    authenticateLock.notify();
                                }

                                break;
                            default:
                                writer.println("{\"success\":false,\"message\":\"i do not understand that command\"}");
                        }
                    }catch(IllegalStateException | NullPointerException | JsonSyntaxException e){

                        StringWriter errors = new StringWriter();
                        e.printStackTrace(new PrintWriter(errors));
                        writer.println("{\"success\":false,\"message\":\"" + errors.toString() + "\"}");
                    }
                }
            }catch(IOException e){
                e.printStackTrace();
            }finally {
                try{
                    if(currentClient == clientNumber){
                        connected = false;
                    }
                    socket.close();
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }
    }



    public static byte[] getHash(String password) {
        MessageDigest digest=null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        digest.reset();
        return digest.digest(password.getBytes());
    }
    static String bin2hex(byte[] data) {
        return String.format("%0" + (data.length*2) + "X", new BigInteger(1, data));
    }

    public static boolean authenticate(String applicationID, String action, String label){
        notificationCounter++;

        if(action == "authenticate"){
            String data = sharedPreferences.getString(applicationID,null);
            JsonParser jp = new JsonParser();
            JsonElement root = jp.parse(data);
            JsonObject rootobj = root.getAsJsonObject();
            label = rootobj.get("label").getAsString();
        }


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(applicationContext)
                        .setSmallIcon(R.mipmap.ic_fingerprint)
                        .setContentTitle(label + " requests your fingerprint to " + action + ".")
                        .setContentText("Tap to authenticate");

        Intent resultIntent = new Intent(applicationContext, FingerprintActivity.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        applicationContext,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotifyMgr = (NotificationManager) applicationContext.getSystemService(NOTIFICATION_SERVICE);
        mBuilder.setAutoCancel(true);
        mNotifyMgr.notify(notificationCounter, mBuilder.build());

        try{
            synchronized (authenticateLock) {
                authenticateLock.wait();
            }
        }catch (InterruptedException e){
            e.printStackTrace();
        }

        return authenticate;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        super.onStartCommand(intent, flags, startId);



        new ServerInitializer().start();
        this.applicationContext = this;
        sharedPreferences = applicationContext.getSharedPreferences("data", MODE_PRIVATE);
        startTimer();
        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.i("ServerService", "onDestroy()");
        Intent broadcastIntent = new Intent(".RestartServer");
        sendBroadcast(broadcastIntent);
        stopTimerTask();
    }

    public void startTimer(){
        timer = new Timer();
        initilizeTimerTask();
        timer.schedule(timerTask,1000,1000);
    }

    public void initilizeTimerTask(){
        timerTask = new TimerTask(){
            public void run(){
                Log.i("TimerTask", "timer: " + (counter++));
            }
        };
    }

    public void stopTimerTask(){
        if(timer != null){
            timer.cancel();
            timer = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent){
        return null;
    }

}
