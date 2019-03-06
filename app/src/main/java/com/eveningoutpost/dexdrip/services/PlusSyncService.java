package com.eveningoutpost.dexdrip.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.GoogleDriveInterface;
import com.eveningoutpost.dexdrip.models.*;
import com.eveningoutpost.dexdrip.utilitymodels.UpdateActivity;
import com.eveningoutpost.dexdrip.xdrip;

import java.lang.ref.WeakReference;


public class PlusSyncService extends Service {
    private final static String TAG = PlusSyncService.class.getSimpleName();
    public static long sleepcounter = 5000;
    public static boolean created = false;
    private static boolean keeprunning;
    private static boolean skipnext = false;
    final MyHandler mHandler = new MyHandler(this);
    Context context;
    private SharedPreferences prefs;

    public PlusSyncService() {
    }

    public static void clearandRestartSyncService(Context context) {
        GoogleDriveInterface.invalidate();
        GcmActivity.token = null; // invalidate
        speedup();
        startSyncService(context, "clearAndRestart");
    }

    public synchronized static void startSyncService(Context context, String source) {
        if (created) {
            UserError.Log.i(TAG, "Already created");
            return;
        }
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("disable_all_sync", false))
        {
            keeprunning=false;
            GcmActivity.cease_all_activity = true;
            UserError.Log.i(TAG,"Sync services disabled");
            return;
        }
        if ((GcmActivity.token != null) && (xdrip.getAppContext() != null)) return;
        UserError.Log.i(TAG, "Starting jamorham xDrip-Plus sync service: " + source);
        context.startService(new Intent(context, PlusSyncService.class));
    }

    public static void backoff() {
        if (sleepcounter < 20000) sleepcounter = 20000;
        skipnext = true;
    }

    public static void backoff_a_lot() {
        if (sleepcounter < 60000) sleepcounter = 60000;
        skipnext = true;
    }

    public static void speedup() {
        sleepcounter = 3000;
        skipnext = false;
    }

    @Override
    public void onCreate() {
        context = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        created = true;
        UserError.Log.i(TAG, "jamorham xDrip-Plus sync service onStart command called");
        keeprunning = true;
        sleepcounter = 5000;
        new Thread(() -> {

            while (keeprunning) {
                try {
                    Thread.sleep(sleepcounter);
                    if (!skipnext) {
                        mHandler.sendEmptyMessage(0);
                    } else {
                        skipnext = false;
                    }
                    if (sleepcounter < 600000) {
                        sleepcounter = sleepcounter + 500;
                    }

                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
            created = false;
        }).start();
        UserError.Log.i(TAG, "jamorham xDrip-Plus sync service onStart command complete");
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onLowMemory() {
        UserError.Log.e(TAG, "Low memory trigger!");
        keeprunning = false;
    }

    @Override
    public void onDestroy() {
        keeprunning = false;
        created = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private class MyHandler extends Handler {
        private final WeakReference<PlusSyncService> mActivity;

        public MyHandler(PlusSyncService activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            PlusSyncService activity = mActivity.get();
            if (activity != null) {
                if (GoogleDriveInterface.getDriveIdentityString() == null) {
                    boolean iunderstand = prefs.getBoolean("I_understand", false);
                    if ((GoogleDriveInterface.isRunning) || (!iunderstand)) {
                        UserError.Log.i(TAG, "Drive interface is running or blocked");
                    } else {
                     /*   UserError.Log.i(TAG, "Calling Google Drive Interface");
                        Intent dialogIntent = new Intent(context, GoogleDriveInterface.class);
                        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        dialogIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);
                        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
                        startActivity(dialogIntent);*/
                    }
                } else if (GcmActivity.token == null) {
                    // also needs isrunning here
                    if (GcmActivity.cease_all_activity) {
                        UserError.Log.i(TAG, "GCM cease all activity flag set!");
                        updateCheckThenStop();
                    } else {
                        UserError.Log.i(TAG, "Calling Google Cloud Interface");
                        //Intent dialogIntent = new Intent(context, GcmActivity.class);
                        //dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        //dialogIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                        //dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        //startActivity(dialogIntent);

                        new GcmActivity().jumpStart();
                    }
                } else {
                    UserError.Log.i(TAG, "Got our token - stopping polling");
                    updateCheckThenStop();
                }
            }
        }

        private void updateCheckThenStop() {
            keeprunning = false;
            skipnext = true;
            UpdateActivity.checkForAnUpdate(context);
            try {
                UserError.Log.i(TAG, "Shutting down");
                stopSelf();
            } catch (Exception e) {
                UserError.Log.e(TAG, "Exception with stop self");
            }
        }
    }

}
