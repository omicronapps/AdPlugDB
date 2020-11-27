package com.omicronapplications.adplugdb;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.File;

public class AdPlugDbService extends Service implements IAdPlugDb {
    private static final String TAG = "AdPlugDbService";
    private static final int ADPLUGDB_CALLBACK = 1;
    private static final int ADPLUGDB_STATUS = 2;
    private static final int ADPLUGDB_INDEX = 3;
    private static final int ADPLUGDB_DELETE = 4;
    private static final int ADPLUGDB_LIST = 5;
    private static final int ADPLUGDB_PLAYLIST = 6;
    private static final int ADPLUGDB_ADD = 7;
    private static final int ADPLUGDB_REMOVE = 8;
    private static final int ADPLUGDB_GETCOUNT = 9;
    private static final int ADPLUGDB_ONSONGINFO = 10;
    private static final String BUNDLE_PATH = "path";
    private static final String BUNDLE_QUICK = "quick";
    private static final String BUNDLE_HIDE = "hide";
    private static final String BUNDLE_RANDOM = "random";
    private static final String BUNDLE_SONG = "song";
    private static final String BUNDLE_TYPE = "type";
    private static final String BUNDLE_TITLE = "title";
    private static final String BUNDLE_AUTHOR = "author";
    private static final String BUNDLE_DESC = "desc";
    private static final String BUNDLE_LENGTH = "length";
    private static final String BUNDLE_SONGLENGTH = "songlength";
    private static final String BUNDLE_SUBSONGS = "subsongs";
    private static final String BUNDLE_VALID = "valid";
    private static final String BUNDLE_PLAYLIST = "playlist";
    private static final String BUNDLE_SORTBY = "sortby";
    private static final String BUNDLE_ORDER = "order";
    private AdPlugDbBinder mBinder;
    private HandlerThread mThread;
    private Handler.Callback mHandlerCallback;
    private Handler mHandler;
    private HandlerThread mIndexThread;
    private Runnable mIndexRunner;
    private Handler mIndexHandler;
    private IAdPlugDbCallback mCallback;
    // DbIndexRunner/DbIndexRunner variables
    private AdPlugDb mDB;
    private File mRoot;
    private boolean mQuick;

    public final class AdPlugDbBinder extends Binder {
        AdPlugDbService getService() {
            return AdPlugDbService.this;
        }
    }

    private class DbIndexRunner implements Runnable {
        @Override
        public void run() {
            if (mDB != null) {
                mDB.index(mRoot, mQuick);
            }
        }
    }

    private class DbHandlerCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            if (mDB == null || mIndexHandler == null) {
                Log.w(TAG, "handleMessage failed, mDB: " + mDB + ", " + ", mIndexHandler:" + mIndexHandler);
                return false;
            }
            switch (msg.what) {
                case ADPLUGDB_CALLBACK:
                    mDB.setCallback(mCallback);
                    break;
                case ADPLUGDB_STATUS:
                    mDB.getStatus();
                    break;
                case ADPLUGDB_INDEX:
                    Bundle data = msg.getData();
                    String root = data.getString(BUNDLE_PATH);
                    mRoot = getFile(root);
                    mQuick = data.getBoolean(BUNDLE_QUICK);
                    mIndexHandler.post(mIndexRunner);
                    break;
                case ADPLUGDB_DELETE:
                    mDB.deleteDB();
                    break;
                case ADPLUGDB_LIST:
                    data = msg.getData();
                    String path = data.getString(BUNDLE_PATH);
                    int sortby = data.getInt(BUNDLE_SORTBY);
                    int order = data.getInt(BUNDLE_ORDER);
                    boolean quick = data.getBoolean(BUNDLE_QUICK);
                    boolean hide = data.getBoolean(BUNDLE_HIDE);
                    boolean random = data.getBoolean(BUNDLE_RANDOM);
                    File f = getFile(path);
                    mDB.list(f, sortby, order, quick, hide, random);
                    break;
                case ADPLUGDB_PLAYLIST:
                    mDB.playlist();
                    break;
                case ADPLUGDB_ADD:
                    data = msg.getData();
                    String name = data.getString(BUNDLE_SONG);
                    long length = data.getLong(BUNDLE_LENGTH);
                    mDB.add(name, length, true);
                    break;
                case ADPLUGDB_REMOVE:
                    data = msg.getData();
                    name = data.getString(BUNDLE_SONG);
                    mDB.remove(name);
                    break;
                case ADPLUGDB_GETCOUNT:
                    mDB.getCount();
                    break;
                case ADPLUGDB_ONSONGINFO:
                    data = msg.getData();
                    name = data.getString(BUNDLE_SONG);
                    String type = data.getString(BUNDLE_TYPE);
                    String title = data.getString(BUNDLE_TITLE);
                    String author = data.getString(BUNDLE_AUTHOR);
                    String desc = data.getString(BUNDLE_DESC);
                    length = data.getLong(BUNDLE_LENGTH);
                    long songlength = data.getLong(BUNDLE_SONGLENGTH);
                    int subsongs = data.getInt(BUNDLE_SUBSONGS);
                    boolean valid = data.getBoolean(BUNDLE_VALID);
                    boolean playlist = data.getBoolean(BUNDLE_PLAYLIST);
                    mDB.onSongInfo(name, type, title, author, desc, length, songlength, subsongs, valid, playlist);
                    break;
                default:
                    Log.w(TAG, "handleMessage: illegal request: " + msg.what);
                    break;
            }
            return true;
        }
    }

    @Override
    public void onCreate() {
        mDB = new AdPlugDb(getApplicationContext());

        mBinder = new AdPlugDbBinder();
        mThread = new HandlerThread("DbHandlerCallback");
        try {
            mThread.start();
        } catch (IllegalThreadStateException e) {
            Log.e(TAG, "onCreate: IllegalThreadStateException: " + e.getMessage());
        }
        Looper looper = mThread.getLooper();
        mHandlerCallback = new DbHandlerCallback();
        mHandler = new Handler(looper, mHandlerCallback);

        mIndexThread = new HandlerThread("DbIndexRunner");
        try {
            mIndexThread.start();
        } catch (IllegalThreadStateException e) {
            Log.e(TAG, "onCreate: IllegalThreadStateException: " + e.getMessage());
        }
        looper = mIndexThread.getLooper();
        mIndexRunner = new DbIndexRunner();
        mIndexHandler = new Handler(looper);
    }

    @Override
    public void onDestroy() {
        mDB = null;

        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        if (mThread != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mThread.quitSafely();
            } else {
                mThread.quit();
            }
            mThread = null;
        }
        mHandlerCallback = null;

        if (mIndexHandler != null) {
            mIndexHandler.removeCallbacksAndMessages(null);
            mIndexHandler = null;
        }
        if (mIndexThread != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mIndexThread.quitSafely();
            } else {
                mIndexThread.quit();
            }
            mIndexThread = null;
        }
        mIndexRunner = null;

        mBinder = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    private void sendMessageToAdPlugDb(int what, Bundle data) {
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage(what);
            if (data != null) {
                msg.setData(data);
            }
            if (what == ADPLUGDB_STATUS || what == ADPLUGDB_INDEX ||
                    what == ADPLUGDB_DELETE || what == ADPLUGDB_PLAYLIST ||
                    what == ADPLUGDB_LIST || what == ADPLUGDB_GETCOUNT) {
                mHandler.removeMessages(what);
            }
            mHandler.sendMessage(msg);
        } else {
            Log.w(TAG, "sendMessageToAdPlugDb: no handler: " + what);
        }
    }

    public void setCallback(IAdPlugDbCallback callback) {
        mCallback = callback;
        sendMessageToAdPlugDb(ADPLUGDB_CALLBACK, null);
    }

    @Override
    public void getStatus() {
        sendMessageToAdPlugDb(ADPLUGDB_STATUS, null);
    }

    @Override
    public void index(String root, boolean quick) {
        Bundle data = new Bundle();
        data.putString(BUNDLE_PATH, root);
        data.putBoolean(BUNDLE_QUICK, quick);
        sendMessageToAdPlugDb(ADPLUGDB_INDEX, data);
    }

    @Override
    public void delete() {
        sendMessageToAdPlugDb(ADPLUGDB_DELETE, null);
    }

    @Override
    public void list(String path, int sortby, int order, boolean quick, boolean hide, boolean random) {
        Bundle data = new Bundle();
        data.putString(BUNDLE_PATH, path);
        data.putInt(BUNDLE_SORTBY, sortby);
        data.putInt(BUNDLE_ORDER, order);
        data.putBoolean(BUNDLE_QUICK, quick);
        data.putBoolean(BUNDLE_HIDE, hide);
        data.putBoolean(BUNDLE_RANDOM, random);
        sendMessageToAdPlugDb(ADPLUGDB_LIST, data);
    }

    @Override
    public void playlist() {
        sendMessageToAdPlugDb(ADPLUGDB_PLAYLIST, null);
    }

    @Override
    public void add(String song, long length) {
        Bundle data = new Bundle();
        data.putString(BUNDLE_SONG, song);
        data.putLong(BUNDLE_LENGTH, length);
        sendMessageToAdPlugDb(ADPLUGDB_ADD, data);
    }

    @Override
    public void remove(String song) {
        Bundle data = new Bundle();
        data.putString(BUNDLE_SONG, song);
        sendMessageToAdPlugDb(ADPLUGDB_REMOVE, data);
    }

    @Override
    public void getCount() {
        sendMessageToAdPlugDb(ADPLUGDB_GETCOUNT, null);
    }

    @Override
    public void onSongInfo(String song, String type, String title, String author, String desc, long length, long songlength, int subsongs, boolean valid, boolean playlist) {
        Bundle data = new Bundle();
        data.putString(BUNDLE_SONG, song);
        data.putString(BUNDLE_TYPE, type);
        data.putString(BUNDLE_TITLE, title);
        data.putString(BUNDLE_AUTHOR, author);
        data.putString(BUNDLE_DESC, desc);
        data.putLong(BUNDLE_LENGTH, length);
        data.putLong(BUNDLE_SONGLENGTH, songlength);
        data.putInt(BUNDLE_SUBSONGS, subsongs);
        data.putBoolean(BUNDLE_VALID, valid);
        data.putBoolean(BUNDLE_PLAYLIST, playlist);
        sendMessageToAdPlugDb(ADPLUGDB_ONSONGINFO, data);
    }

    private static File getFile(String name) {
        File f = null;
        if (name == null || name.isEmpty()) {
            Log.w(TAG, "getFile: invalid directory name: " + name);
            return f;
        }
        f = new File(name);
        if (!f.exists()) {
            Log.w(TAG, "getFile: directory does not exist: " + f);
        }
        if (!f.isDirectory()) {
            Log.w(TAG, "getFile: not a directory: " + f);
        }
        return f;
    }
}
