package com.example.kurs2week1task2progect;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;

import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.os.Messenger;

public class MainActivity extends AppCompatActivity {


    private static int progress = 0;

    private Intent intent;
    private ProgressBar mProgressBar;
    private Button mButtonDrop;

    private Messenger mService = null;

    final Messenger mMessenger = new Messenger(new IncomingHandler());

    private ServiceConnection sConn  = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            mService = new Messenger(service);

            try {
                Message msg = Message.obtain(null,1 );
                msg.replyTo = mMessenger;
                mService.send(msg);
                }
            catch (RemoteException e) {
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case SimpleProgressBarBoundService.MSG_SHOW_TOAST:
                    progress = msg.arg1;
                    mProgressBar.setProgress(msg.arg1);
                    Toast.makeText(MainActivity.this,"Done", Toast.LENGTH_SHORT).show();
                case SimpleProgressBarBoundService.MSG_SET_PROGRESS_VALUE:
                    progress = msg.arg1;
                    mProgressBar.setProgress(msg.arg1);
            }

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgressBar = findViewById(R.id.progressBar);
        mButtonDrop = findViewById(R.id.btn_drop_progress_bar);

        intent = new Intent(this,SimpleProgressBarBoundService.class);

        mButtonDrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if((progress - 50) >= 0){
                    progress = progress - 50;
                    mProgressBar.setProgress(progress);
                    try {
                        Message msg = Message.obtain(null, SimpleProgressBarBoundService.MSG_SET_PROGRESS_VALUE, progress,0);
                        msg.replyTo = mMessenger;
                        mService.send(msg);
                    }
                    catch (RemoteException e) {
                        Toast.makeText(MainActivity.this, "Send message 'drop' fail", Toast.LENGTH_SHORT).show();

                    }
                }

            }
        });


        startService(new Intent(MainActivity.this, SimpleProgressBarBoundService.class));
        bindService(intent,sConn, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            Message msg = Message.obtain(null, 3);
            msg.replyTo = mMessenger;
            mService.send(msg);
        }
        catch (RemoteException e) {

        }
        unbindService(sConn);
    }
}
