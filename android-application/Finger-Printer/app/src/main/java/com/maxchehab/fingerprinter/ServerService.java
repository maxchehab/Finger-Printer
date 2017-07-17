package com.maxchehab.fingerprinter;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.Buffer;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by maxchehab on 7/17/17.
 */

public class ServerService extends Service {

    public int counter = 0;

    private Timer timer;
    private TimerTask timerTask;


    public ServerService(){
        super();

        new ServerInitializer().start();

        Log.i("ServerService","initilized");
    }

    private static class ServerInitializer extends Thread{
        public void run(){
            int clientNumber = 0;
            try {
                ServerSocket serverSocket = new ServerSocket(61597);
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
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

                writer.println("Hello, you have connected!\n");

                while(true){
                    String input = reader.readLine();
                    if(input == null){
                        break;
                    }

                    Log.i("ServerListener","client #" + clientNumber + " responded with the message {" + input + "}");
                }
            }catch(IOException e){
                e.printStackTrace();
            }finally {
                try{
                    socket.close();
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        super.onStartCommand(intent, flags, startId);
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
