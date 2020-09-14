package com.omicronapplications.adplugdb;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class AdPlugDbController {
    private static final String TAG = "AdPlugDbController";
    private final IAdPlugDbCallback mCallback;
    private final Context mContext;
    private AdPlugDbService mService;
    private AdPlugDbConnection mConnection;

    public AdPlugDbController(IAdPlugDbCallback callback, Context context) {
        mCallback = callback;
        mContext = context;
    }

    public void startDB() {
        if (mContext == null) {
            Log.e(TAG, "startDB: failed to set up ServiceConnection");
            return;
        }
        mConnection = new AdPlugDbConnection();
        Intent intent = new Intent(mContext, AdPlugDbService.class);
        if (!mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "startDB: failed to bind to service");
        }
    }

    public void stopDB() {
        mContext.unbindService(mConnection);
        mConnection = null;
    }

    public IAdPlugDb getService() {
        return mService;
    }

    private class AdPlugDbConnection implements ServiceConnection {
        private AdPlugDbService.AdPlugDbBinder mBinder;

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinder = (AdPlugDbService.AdPlugDbBinder) service;
            mService = mBinder.getService();
            mService.setCallback(mCallback);
            if (mCallback != null) {
                mCallback.onDBServiceConnected();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (mCallback != null) {
                mCallback.onDBServiceDisconnected();
            }
            mBinder = null;
            mService.setCallback(null);
            mService = null;
        }

        @Override
        public void onBindingDied(ComponentName name) {
            if (mConnection != null) {
                mContext.unbindService(mConnection);
                mConnection = null;
            }
        }

        @Override
        public void onNullBinding(ComponentName name) {
            if (mConnection != null) {
                mContext.unbindService(mConnection);
                mConnection = null;
            }
        }
    }
}
