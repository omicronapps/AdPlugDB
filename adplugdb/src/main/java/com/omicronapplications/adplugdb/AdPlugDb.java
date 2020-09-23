package com.omicronapplications.adplugdb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.omicronapplications.adplugdb.IAdPlugDbCallback.dbStatus;

import static com.omicronapplications.adplugdb.IAdPlugDbCallback.dbStatus.*;

public class AdPlugDb extends SQLiteOpenHelper {
    private static final String TAG = "AdPlugDb";
    private static final String DB_NAME = "AdPlugDb";
    private static final String TABLE_NAME = "adplug";
    private static final String KEY_PATH = "path";
    private static final String KEY_NAME = "name";
    private static final String KEY_TYPE = "type";
    private static final String KEY_TITLE = "title";
    private static final String KEY_AUTHOR = "author";
    private static final String KEY_DESC = "description";
    private static final String KEY_LENGTH = "length";
    private static final String KEY_SONGLENGTH = "songlength";
    private static final String KEY_SUBSONGS = "subsongs";
    private static final String KEY_VALID = "valid";
    private static final String KEY_DIR = "dir";
    private static final String KEY_PLAYLIST = "playlist";
    private static final int DB_VERSION = 1;
    private static final String[] ALLPLAYERS = {
            "hsc", "sng", "imf", "wlf", "adlib", "a2m", "sng", "amd", "bam", "cmf",
            "d00", "dfm", "hsp", "ksm", "mad", "mus", "ims", "mdi", "mid", "sci",
            "laa", "mkj", "cff", "dmo", "s3m", "dtm", "sng", "mtk", "rad", "rac",
            "raw", "sat", "sa2", "xad", "bmf", "xad", "xad", "xad", "xad", "xad",
            "lds", "m", "rol", "xsm", "dro", "dro", "msc", "rix", "adl", "jbm",
            "got", "cmf", "vgm", "vgz", "sop", "hsq", "sqx", "sdb", "agd", "ha2"};
    private final ReentrantLock mLock = new ReentrantLock();
    private IAdPlugDbCallback mCallback;
    private IAdPlugDbCallback.dbStatus mStatus;
    private String mPath;
    private boolean mOnList;
    private boolean mHide;
    private int mOrder;
    private List<String> mListSongs;
    private List<String> mIndexSongs;

    AdPlugDb(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        mCallback = null;
        mStatus = UNINITIALIZED;
        mPath = null;
        mOnList = false;
        mHide = false;
        mOrder = IAdPlugDb.SORT_NONE;
        mListSongs = new ArrayList<>();
        mIndexSongs = new ArrayList<>();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE VIRTUAL TABLE " + TABLE_NAME + " USING fts4(" +
                KEY_PATH + " TEXT, " +
                KEY_NAME + " TEXT, " +
                KEY_TYPE + " TEXT, " +
                KEY_TITLE + " TEXT, " +
                KEY_AUTHOR + " TEXT, " +
                KEY_DESC + " TEXT, " +
                KEY_LENGTH + " INTEGER, " +
                KEY_SONGLENGTH + " INTEGER, " +
                KEY_SUBSONGS + " INTEGER, " +
                KEY_VALID + " INTEGER, " +
                KEY_DIR + " INTEGER, " +
                KEY_PLAYLIST + " INTEGER " +
                ")";
        try{
            db.execSQL(sql);
        } catch (android.database.SQLException e) {
            Log.e(TAG, "onCreate: SQLException: " + e.getMessage());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    void setCallback(IAdPlugDbCallback callback) {
        mCallback = callback;
    }

    void getStatus() {
        if (mCallback != null) {
            mCallback.onStatus(mStatus);
        }
    }

    void index(File root, boolean quick) {
        boolean found = hasPath(root);
        if (found && quick) {
            Log.i(TAG, "index: path already exists in database" + root);
            updateStatus(INITIALIZED);
            return;
        } else {
            updateStatus(UNINITIALIZED);
        }

        try {
            if (!mLock.isHeldByCurrentThread()) {
                mLock.lock();
            }
            mIndexSongs.clear();
        } finally {
            mLock.unlock();
        }

        recursiveIndex(root, false);
        updateStatus(INDEXING);

        boolean initialized = false;
        try {
            if (!mLock.isHeldByCurrentThread()) {
                mLock.lock();
            }
            if (mIndexSongs.size() == 0) {
                initialized = true;
            }
        } finally {
            mLock.unlock();
        }

        if (initialized) {
            updateStatus(INITIALIZED);
        }
    }

    void deleteDB() {
        SQLiteDatabase db = getWritableDatabase();

        File databaseFile = new File(db.getPath());
        boolean deleted = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            deleted = SQLiteDatabase.deleteDatabase(databaseFile);
        } else {
            deleted = databaseFile.delete();
        }
        if (deleted) {
            updateStatus(UNINITIALIZED);
        } else {
            Log.e(TAG, "deleteDB: failed to delete database: " + databaseFile.getAbsolutePath());
        }
    }

    void list(File path, int order, boolean quick, boolean hide) {
        boolean found = hasPath(path);
        mHide = hide;
        if (found && quick) {
            onList();
            return;
        }

        mPath = path.getAbsolutePath();
        mOrder = order;

        try {
            if (!mLock.isHeldByCurrentThread()) {
                mLock.lock();
            }
            mListSongs.clear();
        } finally {
            mLock.unlock();
        }

        recursiveIndex(path, true);

        boolean onlist = false;
        try {
            if (!mLock.isHeldByCurrentThread()) {
                mLock.lock();
            }
            mLock.lock();
            mOnList = true;
            if (mListSongs.size() == 0) {
                mOnList = false;
                onlist = true;
            }
        } finally {
            mLock.unlock();
        }

        if (onlist) {
            onList();
        }
    }

    AdPlugFile add(String name, long length, boolean onlist) {
        AdPlugFile song = null;
        boolean contains = false;
        try {
            if (!mLock.isHeldByCurrentThread()) {
                mLock.lock();
            }
            contains = mIndexSongs.contains(name);
            if (!contains) {
                if (onlist) {
                    mListSongs.add(name);
                }
                mIndexSongs.add(name);
            }
        } finally {
            mLock.unlock();
        }

        if (!contains) {
            if (mCallback != null) {
                // Request song information from AdPlug
                mCallback.requestInfo(name, length);
            } else {
                // Unable to get get song information from AdPlug
                File f = new File(name);
                song = new AdPlugFile(f.getParent(), f.getName(), null, null, null, null, 0, -1, -1, false, isPlaylist(f));
                addToDB(song);
            }
        }
        return song;
    }

    void remove(String name) {
        // Remove file from DB
        File f = new File(name);
        AdPlugFile song = new AdPlugFile(f.getParent(), f.getName(), null, null, null, null, 0, -1, -1, false, isPlaylist(f));
        deleteFromDB(song);
    }

    void getCount() {
        SQLiteDatabase db = getReadableDatabase();

        long count = DatabaseUtils.queryNumEntries(db, TABLE_NAME);

        if (mCallback != null) {
            mCallback.onGetCount(count);
        }
    }

    void onSongInfo(String name, String type, String title, String author, String desc, long length, long songlength, int subsongs, boolean valid, boolean playlist) {
        // Add song to DB
        File f = new File(name);
        AdPlugFile song = new AdPlugFile(f.getParent(), f.getName(), type, title, author, desc, length, songlength, subsongs, valid, playlist);
        addToDB(song);
        boolean initialized = false;
        boolean onlist = false;
        try {
            if (!mLock.isHeldByCurrentThread()) {
                mLock.lock();
            }
            if (mIndexSongs.contains(name)) {
                mIndexSongs.remove(name);
                if ((mIndexSongs.size() == 0) && mStatus == INDEXING) {
                    initialized = true;
                }
            } else {
                Log.e(TAG, "onSongInfo: " + name + " NOT FOUND!!!");
            }
            if (mListSongs.contains(name)) {
                mListSongs.remove(name);
                if (mListSongs.size() == 0 && mOnList) {
                    mOnList = false;
                    onlist = true;
                }
            }
        } finally {
            mLock.unlock();
        }

        if (initialized) {
            updateStatus(INITIALIZED);
        }

       if (onlist) {
            onList();
        }
    }

    private void onList() {
        if (mCallback != null) {
            List<AdPlugFile> dbFiles = queryDB(KEY_PATH, mPath);
            if (mOrder == IAdPlugDb.SORT_ASCENDING || mOrder == IAdPlugDb.SORT_DESCENDING) {
                java.util.Collections.sort(dbFiles, new IgnoreCaseComparator());
            }
            if (mOrder == IAdPlugDb.SORT_DESCENDING) {
                java.util.Collections.sort(dbFiles, Collections.reverseOrder());
            }
            if (mHide) {
                Iterator<AdPlugFile> it = dbFiles.iterator();
                while (it.hasNext()) {
                    AdPlugFile file = it.next();
                    if (file != null && !file.valid && !file.dir && !file.playlist) {
                        it.remove();
                    }
                }
            }
            mCallback.onList(dbFiles);
        }
    }

    private static class IgnoreCaseComparator implements Comparator<AdPlugFile> {
        @Override
        public int compare(AdPlugFile o1, AdPlugFile o2) {
            String s1 = (o1 != null && o1.name != null) ? o1.name.toLowerCase() : "";
            String s2 = (o2 != null && o2.name != null) ? o2.name.toLowerCase() : "";
            return s1.compareTo(s2);
        }
    }

    private boolean hasPath(File path) {
        String name = "";
        if (path != null) {
            name = path.getName().replace("'", "''");
        }
        SQLiteDatabase db = getWritableDatabase();
        boolean found = false;

        String sql = "SELECT * FROM " + TABLE_NAME;
        sql = sql + " WHERE " + KEY_PATH + " = " + "'" + name + "'" + " LIMIT 1";
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(sql, null);
        } catch (android.database.SQLException e) {
            Log.e(TAG, "hasPath: SQLException: " + e.getMessage());
        }
        if (cursor != null) {
            found = cursor.moveToFirst();
            cursor.close();
        }
        return found;
    }

    private List<AdPlugFile> recursiveIndex(File path, boolean onlist) {
        File[] fs  = path.listFiles();
        List<AdPlugFile> dbFiles = queryDB(KEY_PATH, path.getAbsolutePath());

        // Delete non-existent files from DB
        if (dbFiles.size() > 0) {
            for (AdPlugFile dbFile : dbFiles) {
                if (fs == null || !contains(fs, dbFile)) {
                    deleteFromDB(dbFile);
                }
            }
        }

        // Add directories and songs to DB
        if (fs != null && fs.length > 0) {
            for (File f : fs) {
                if (f.isDirectory()) {
                    // Recursively add directories to DB
                    AdPlugFile song = new AdPlugFile(f.getParent(), f.getName());
                    addToDB(song);
                    dbFiles.add(song);
                    if (!onlist) {
                        recursiveIndex(f, onlist);
                    }
                } else if (f.isFile()) {
                    // Request song information from AdPlug
                    String name = f.getAbsolutePath();
                    long length = f.length();
                    AdPlugFile song = add(name, length, onlist);
                    if (song != null) {
                        dbFiles.add(song);
                    }
                }
            }
        }

        return dbFiles;
    }

    private List<AdPlugFile> queryDB(String sql) {
        SQLiteDatabase db = getWritableDatabase();

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(sql, null);
        } catch (android.database.SQLException e) {
            Log.e(TAG, "queryDB: SQLException: " + e.getMessage());
        }
        List<AdPlugFile> sids = new ArrayList<>();
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    AdPlugFile sid = new AdPlugFile();
                    sid.path = cursor.getString(0);
                    sid.name = cursor.getString(1);
                    sid.type = cursor.getString(2);
                    sid.title = cursor.getString(3);
                    sid.author = cursor.getString(4);
                    sid.desc = cursor.getString(5);
                    sid.length = cursor.getLong(6);
                    sid.songlength = cursor.getLong(7);
                    sid.subsongs = cursor.getInt(8);
                    sid.valid = cursor.getInt(9) != 0;
                    sid.dir = cursor.getInt(10) != 0;
                    sid.playlist = cursor.getInt(11) != 0;
                    sids.add(sid);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return sids;
    }

    private List<AdPlugFile> queryDB(String key, String value) {
        String sql = "SELECT * FROM " + TABLE_NAME;
        if (key == null) {
            Log.e(TAG, "queryDB: invalid key");
            return new ArrayList<>();
        }
        if (value != null) {
            sql = sql + " WHERE " + key + " = " + "'" + value + "'";
        } else {
            sql = sql + " WHERE " + key + " IS NULL";
        }
        return queryDB(sql);
    }

    private void updateStatus(dbStatus status) {
        mStatus = status;
        if (mCallback != null) {
            mCallback.onStatusChanged(status);
        }
    }

    private boolean addToDB(AdPlugFile song) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_PATH, song.path);
        values.put(KEY_NAME, song.name);
        values.put(KEY_TYPE, song.type);
        values.put(KEY_TITLE, song.title);
        values.put(KEY_AUTHOR, song.author);
        values.put(KEY_DESC, song.desc);
        values.put(KEY_LENGTH, song.length);
        values.put(KEY_SONGLENGTH, song.songlength);
        values.put(KEY_SUBSONGS, song.subsongs);
        values.put(KEY_VALID, (int) (song.valid ? 1 : 0));
        values.put(KEY_DIR, (int) (song.dir ? 1 : 0));
        values.put(KEY_PLAYLIST, (int) (song.playlist ? 1: 0));

        // Duplicate files are not allowed, always replace any existing entry in DB
        deleteFromDB(song);
        long row = 0;
        try {
            row = db.insert(TABLE_NAME, null, values);
        } catch (android.database.SQLException e) {
            Log.e(TAG, "addToDB: SQLException: " + e.getMessage());
        }

        return (row >= 0);
    }

    private boolean deleteFromDB(AdPlugFile song) {
        SQLiteDatabase db = getWritableDatabase();

        String path = song.path.replace("'", "''");
        String name = song.name.replace("'", "''");
        String whereClause = KEY_PATH + " = " + "'" + path + "'" + " AND " + KEY_NAME + " = " + "'" + name + "'";

        int rows = 0;
        try {
            rows = db.delete(TABLE_NAME, whereClause, null);
        } catch (android.database.SQLException e) {
            Log.e(TAG, "delete: SQLException: " + e.getMessage());
        }
        return (rows >= 1);
    }

    private static boolean isPlaylist(File file) {
        return (file != null) && !file.isDirectory() && file.getName().endsWith(".m3u");
    }

    private static boolean contains(File[] array, AdPlugFile value) {
        for (File element : array) {
            File f = new File(value.path, value.name);
            if (element != null && element.equals(f)) {
                return true;
            }
        }
        return false;
    }

    private static boolean contains(List<AdPlugFile> array, File value) {
        for (AdPlugFile element : array) {
            if (element != null) {
                File f = new File(element.path, element.name);
                if (f.equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }
}
