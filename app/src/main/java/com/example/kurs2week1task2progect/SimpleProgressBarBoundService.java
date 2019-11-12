package com.example.kurs2week1task2progect;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;


import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleProgressBarBoundService extends Service {

    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_SET_PROGRESS_VALUE = 2;
    static final int MSG_UNREGISTER_CLIENT = 3;
    static final int MSG_SHOW_TOAST = 1;

    private String channelId = "my_channel";

    private NotificationManager mManager;
    private NotificationCompat.Builder mBuilder;
    private NotificationChannel channel;
    private static Lock lock = new ReentrantLock();

    private static int progress;

    private ScheduledExecutorService mScheduledExecutorService;

    private static ArrayList<Messenger> mClient = new ArrayList<>();
    final Messenger mMessenger = new Messenger(new IncomingHandler());


    public SimpleProgressBarBoundService() {
    }

    @Override
    public IBinder onBind(Intent intent) {

        return mMessenger.getBinder();
    }

    private static class IncomingHandler extends Handler{

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClient.add(msg.replyTo);
                    break;
                case MSG_SET_PROGRESS_VALUE:
                    lock.lock();
                    progress =  msg.arg1;
                    lock.unlock();
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClient.remove(msg.replyTo);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void sendMessageToUI(int value){
        for (int i= mClient.size()-1; i>=0; i--) {
            try {
                if (value == 100)
                    mClient.get(i).send(Message.obtain(null, MSG_SHOW_TOAST, value, 0));

                else
                    mClient.get(i).send(Message.obtain(null, MSG_SET_PROGRESS_VALUE, value,0));
            }
            catch (RemoteException e) {
                mClient.remove(i);
            }

        }
    }

    private NotificationCompat.Builder getNotificationBuilder(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){


            if (mManager.getNotificationChannel(channelId) == null) {
                channel = new NotificationChannel(channelId, "Users channel", NotificationManager.IMPORTANCE_DEFAULT);
                mManager.createNotificationChannel(channel);
            }

            return new NotificationCompat.Builder(this, channelId);
        }
        else
            return new NotificationCompat.Builder(this);
    }
    private Notification getNotification(String contentText) {
        return mBuilder.setContentText(contentText).build();
    }


    @Override
    public void onCreate() {

        mScheduledExecutorService = Executors.newScheduledThreadPool(1);
        mManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mBuilder = getNotificationBuilder();
        mBuilder.setContentTitle("Count service notification").setSmallIcon(R.drawable.ic_launcher_foreground).setProgress(100,0,true);
        startForeground(123, getNotification("start notification"));


    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        startForeground(123, getNotification("start notification"));


        mScheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if(progress <=100){
                    progress +=5;
                    sendMessageToUI(progress);
                    if (progress < 100) {
                        mBuilder.setProgress(100,progress,false).setContentText(progress + " of 100");
                        mManager.notify(123, mBuilder.build());
                    }
                    else{
                        mBuilder.setProgress(0,100,false).setContentText("Completed");
                        mManager.notify(123,mBuilder.build());
                    }
                }

            }},1000,200, TimeUnit.MILLISECONDS);

        return START_STICKY;

    }

    @Override
    public void onDestroy() {
        mScheduledExecutorService.shutdownNow();
        mManager.cancel(123);
    }



}
