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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;
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
import static com.maxchehab.fingerprinter.FingerprintActivity.username;

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

    private static Gson gson = new Gson();

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

    private static class AuthenticateHandler extends Thread{

        private String action;
        private String authApplicationID;
        private String authLabel;
        private String authUsername;
        private String uniqueKey;

        private Socket socket;
        private PrintWriter writer;


        public AuthenticateHandler(String applicationID, Socket socket, PrintWriter printWriter){
            this.action = "authenticate";
            this.authApplicationID = applicationID;
            this.socket = socket;
            this.writer = printWriter;
            this.authLabel = getLabel(applicationID);
        }

        public AuthenticateHandler(String applicationID, String label, String username, String uniqueKey, Socket socket, PrintWriter printWriter){
            this.action = "pair";
            this.authApplicationID = applicationID;
            this.authLabel = label;
            this.authUsername = username;
            this.uniqueKey = uniqueKey;
            this.socket = socket;
            this.writer = printWriter;
        }


        public void run(){

            final String hardwareID =  Settings.Secure.getString(
                    applicationContext.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            ExecutorService authExecutor = Executors.newCachedThreadPool();
            Callable<Boolean> authTask = new Callable<Boolean>() {
                public Boolean call() {
                    return authenticate(authApplicationID,action,authLabel,authUsername);
                }
            };
            Future<Boolean> authFuture = authExecutor.submit(authTask);
            try {
                boolean authResponse = authFuture.get(30, TimeUnit.SECONDS);
                Log.i("authResponse","30 seconds or auth");
                if(action == "pair"){
                    if(authResponse){
                        addUser(authApplicationID, authLabel, authUsername, uniqueKey);
                        Log.i("pair-command", "Saved applicationID: " + authApplicationID);
                    }
                    writer.println("{\"success\":" + authResponse + ",\"username\":\"" + username + "\",\"command\":\"pair\",\"message\":\"ran pair\",\"uniqueKey\":\"" + uniqueKey + "\",\"hardwareID\":\"" + hardwareID + "\"}");
                }else{
                    uniqueKey = getUniqueKey(authApplicationID,username);
                    writer.println("{\"success\":" + authResponse + ",\"username\":\"" + username + "\",\"command\":\"authenticate\",\"message\":\"ran authentication\",\"uniqueKey\":\"" + uniqueKey + "\"}");
                }

                synchronized (authenticateLock) {
                    authenticateLock.notify();
                }
            } catch (TimeoutException | InterruptedException | ExecutionException e) {
                notificationManager.cancelAll();
                try{
                    socket.close();
                }catch(IOException ex){
                    ex.printStackTrace();
                }
            } finally {
                authFuture.cancel(true);
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
                                final String pairUsername = rootobj.get("username").getAsString();
                                final String pairLabel = rootobj.get("label").getAsString();
                                uniqueKey = bin2hex(getHash(rootobj.get("salt").getAsString() + hardwareID));

                                if(containsUser(pairApplicationID, pairUsername)){
                                    writer.println("{\"success\":false,\"command\":\"pair\",\"message\":\"already paired\"}");
                                    break;
                                }

                                new AuthenticateHandler(pairApplicationID,pairLabel,pairUsername,uniqueKey,socket,writer).start();
                                break;
                            case "authenticate":
                                final String authApplicationID = rootobj.get("applicationID").getAsString();

                                if(!containsApplication(authApplicationID)){
                                    writer.println("{\"success\":false,\"command\":\"authenticate\",\"message\":\"i do not know that user\"}");
                                    break;
                                }

                                final String authLabel = getLabel(authApplicationID);

                                new AuthenticateHandler(authApplicationID,socket, writer).start();

                                break;
                            default:
                                writer.println("{\"success\":false,\"message\":\"i do not understand that command\"}");
                        }
                    }catch(IllegalStateException | NullPointerException | JsonSyntaxException e){

                        StringWriter errors = new StringWriter();
                        e.printStackTrace(new PrintWriter(errors));
                        writer.println("{\"success\":false,\"message\":\"" + errors.toString().replaceAll("\"", "'").replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "") + "'" + "\"}");
                    }
                }
            }catch(IOException e){
                e.printStackTrace();
            }finally {
                try{
                    Log.i("ServerService","Client " + clientNumber + " leaving...");


                    if(currentClient == clientNumber){
                        notificationManager.cancelAll();
                        connected = false;
                        synchronized (authenticateLock) {
                            authenticateLock.notify();
                        }
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

    public static boolean authenticate(String applicationID, String action, String label, String username){
        notificationCounter++;

        /*if(action == "authenticate"){
            String data = sharedPreferences.getString(applicationID,null);
            JsonParser jp = new JsonParser();
            JsonElement root = jp.parse(data);
            JsonObject rootobj = root.getAsJsonObject();
            label = rootobj.get("label").getAsString();
        }*/


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(applicationContext)
                        .setSmallIcon(R.mipmap.ic_fingerprint)
                        .setContentTitle("Tap to " + action + ".")
                        .setLights(Color.GREEN,500,500)
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setContentText(label + " requests your fingerprint to " + action + ".");


        Intent resultIntent = new Intent(applicationContext, FingerprintActivity.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        resultIntent.putExtra("STARTTIME",System.currentTimeMillis());
        resultIntent.putExtra("APPLICATIONID",applicationID);
        resultIntent.putExtra("ACTION",action);
        if(action.equals("pair")){
            resultIntent.putExtra("USERNAME",username);
        }

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
        sharedPreferences = applicationContext.getSharedPreferences("database", MODE_PRIVATE);
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

    public static boolean containsUser(String applicationID, String username){
        LinkedList<Application> applications = new LinkedList<>(Arrays.asList(gson.fromJson(sharedPreferences.getString("applications","[]"), Application[].class)));
        for (Application application : applications) {
            if(application.applicationID.equals(applicationID)){
                for (User user : application.users) {
                    if(user.username.equals(username)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static String getUniqueKey(String applicationID, String username){
        LinkedList<Application> applications = new LinkedList<>(Arrays.asList(gson.fromJson(sharedPreferences.getString("applications","[]"), Application[].class)));
        for (Application application : applications) {
            if(application.applicationID.equals(applicationID)){
                for (User user : application.users) {
                    if(user.username.equals(username)){
                        return user.uniqueKey;
                    }
                }
            }
        }
        return null;
    }

    public static boolean containsApplication(String applicationID){
        LinkedList<Application> applications = new LinkedList<>(Arrays.asList(gson.fromJson(sharedPreferences.getString("applications","[]"), Application[].class)));
        for (Application application : applications) {
            if(application.applicationID.equals(applicationID)){
               return true;
            }
        }
        return false;
    }

    public static String getLabel(String applicationID){
        LinkedList<Application> applications = new LinkedList<>(Arrays.asList(gson.fromJson(sharedPreferences.getString("applications","[]"), Application[].class)));
        for (Application application : applications) {
            if(application.applicationID.equals(applicationID)){
                return application.label;
            }
        }
        return null;
    }

    public static void addUser(String applicationID, String label, String username, String uniqueKey){
        Log.i("addUser()", "applicationID: " + applicationID + ", username: " + username + ", uniqueKey: " + uniqueKey);
        LinkedList<Application> applications = new LinkedList<>(Arrays.asList(gson.fromJson(sharedPreferences.getString("applications","[]"), Application[].class)));
        for (Application application : applications) {
            if(application.applicationID.equals(applicationID)){
                application.users.add(new User(username,uniqueKey));
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("applications", gson.toJson(applications.toArray()));
                editor.commit();
                return;
            }
        }

        applications.add(new Application(applicationID,label));
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("applications", gson.toJson(applications.toArray()));
        editor.commit();

        addUser(applicationID, label, username,uniqueKey);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent){
        return null;
    }

}
