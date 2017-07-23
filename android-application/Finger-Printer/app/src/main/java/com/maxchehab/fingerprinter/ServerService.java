package com.maxchehab.fingerprinter;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
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
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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


    private static NotificationManager notificationManager = null;

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
                    writer.println("{\"success\":true,\"command\":\"knock-knock\",\"hardwareID\":\"" + hardwareID + "\",\"deviceName\":\"" + deviceName + "\"}");
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

                        String uniqueKey = null;
                        String label = null;



                        switch (rootobj.get("command").getAsString()) {
                            case "knock-knock":
                                writer.println("{\"success\":true,\"command\":\"knock-knock\",\"hardwareID\":\"" + hardwareID + "\",\"deviceName\":\"" + deviceName + "\"}");
                                break;
                            case "pair":

                                final String pairApplicationID = rootobj.get("applicationID").getAsString();
                                final String pairLabel = rootobj.get("label").getAsString();
                                uniqueKey = bin2hex(getHash(rootobj.get("salt").getAsString() + hardwareID));

                                if(sharedPreferences.contains(pairApplicationID)){
                                    writer.println("{\"success\":false,\"command\":\"pair\",\"message\":\"already paired\"}");
                                    break;
                                }


                                ExecutorService executor = Executors.newCachedThreadPool();
                                Callable<Boolean> task = new Callable<Boolean>() {
                                    public Boolean call() {
                                        return authenticate(pairApplicationID,"pair",pairLabel);
                                    }
                                };
                                Future<Boolean> future = executor.submit(task);
                                try {
                                    boolean pairResponse = future.get(30, TimeUnit.SECONDS);
                                    if(pairResponse){
                                        SharedPreferences.Editor editor = sharedPreferences.edit();
                                        editor.putString(pairApplicationID, "{\"uniqueKey\":\"" + uniqueKey + "\",\"label\":\"" + label + "\"}");
                                        editor.commit();
                                        Log.i("pair-command", "Saved applicationID: " + pairApplicationID);
                                    }
                                    writer.println("{\"success\":" + pairResponse + ",\"command\":\"pair\",\"message\":\"ran pair\",\"uniqueKey\":\"" + uniqueKey + "\",\"hardwareID\":\"" + hardwareID + "\"}");

                                    synchronized (authenticateLock) {
                                        authenticateLock.notify();
                                    }
                                } catch (TimeoutException | InterruptedException | ExecutionException e) {
                                    notificationManager.cancelAll();
                                    socket.close();
                                } finally {
                                    future.cancel(true); // may or may not desire this
                                }

                                break;
                            case "authenticate":
                                final String authApplicationID = rootobj.get("applicationID").getAsString();
                                if(!sharedPreferences.contains(authApplicationID)){
                                    writer.println("{\"success\":false,\"command\":\"authenticate\",\"message\":\"i do not know that applicationID\"}");
                                    break;
                                }

                                String data = sharedPreferences.getString(authApplicationID,null);
                                JsonParser jsonParser = new JsonParser();
                                JsonElement jsonElement = jsonParser.parse(data);
                                JsonObject jsonObject = jsonElement.getAsJsonObject();

                                uniqueKey = jsonObject.get("uniqueKey").getAsString();

                                final String authLabel = jsonObject.get("label").getAsString();

                                ExecutorService authExecutor = Executors.newCachedThreadPool();
                                Callable<Boolean> authTask = new Callable<Boolean>() {
                                    public Boolean call() {
                                        return authenticate(authApplicationID,"authenticate",authLabel);
                                    }
                                };
                                Future<Boolean> authFuture = authExecutor.submit(authTask);
                                try {
                                    boolean authResponse = authFuture.get(30, TimeUnit.SECONDS);
                                    writer.println("{\"success\":" + authResponse + ",\"command\":\"authenticate\",\"message\":\"ran authentication\",\"uniqueKey\":\"" + uniqueKey + "\"}");

                                    synchronized (authenticateLock) {
                                        authenticateLock.notify();
                                    }
                                } catch (TimeoutException | InterruptedException | ExecutionException e) {
                                    notificationManager.cancelAll();
                                    socket.close();
                                } finally {
                                    authFuture.cancel(true);
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
                    Log.i("ServerService","Client " + clientNumber + " leaving...");

                    notificationManager.cancelAll();

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
                        .setContentTitle("Tap to " + action + ".")
                        .setLights(Color.GREEN,500,500)
                        .setOnlyAlertOnce(true)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setContentText(label + " requests your fingerprint to " + action + ".");


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
        mBuilder.setAutoCancel(true);
        notificationManager.notify(notificationCounter, mBuilder.build());

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

        this.notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);


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
