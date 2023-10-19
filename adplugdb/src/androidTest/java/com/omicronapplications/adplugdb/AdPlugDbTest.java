package com.omicronapplications.adplugdb;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AdPlugDbTest {
    private static final long TEST_TIMEOUT = 1000; // ms
    private IAdPlugDbCallback mCallback;
    private AdPlugDb mDb;
    private CountDownLatch mLatch;
    private Map<String, AdPlugFile> mExpected = new HashMap<String, AdPlugFile>() {{
        put("en_lille_test.d00", new AdPlugFile(getCacheDir().getAbsolutePath(), "en_lille_test.d00", "EdLib packed (version 4)", "En lille test", "Morten Sigaard", "", 2563, 60324, 1, true, false));
        put("playlist.m3u", new AdPlugFile(getCacheDir().getAbsolutePath(), "playlist.m3u", "", "", "", "", 0, -1, -1, false, true));
        put("d00", new AdPlugFile(getCacheDir().getAbsolutePath(), "d00"));
        put("songs.m3u", new AdPlugFile(new File(getCacheDir(), "d00").getAbsolutePath(), "songs.m3u", "", "", "", "", 0, -1, -1, false, true));
        put("fresh.d00", new AdPlugFile(new File(getCacheDir(), "d00").getAbsolutePath(), "fresh.d00", "EdLib packed (version 4)", "Fresh", "Morten Sigaard", "", 1497, 21342, 1, true, false));
        put("gone.d00", new AdPlugFile(new File(getCacheDir(), "d00").getAbsolutePath(), "gone.d00", "EdLib packed (version 1)", "", "",  "\"GONE...\" by DRAX - converted by JCH, 13/1-1992. Player & music (C) Vibrants, 1992.", 1758, 76886, 1, true, false));
        put("edlib", new AdPlugFile(getCacheDir().getAbsolutePath(), "edlib"));
        put("test.m3u", new AdPlugFile(new File(getCacheDir(), "edlib").getAbsolutePath(), "test.m3u", "", "", "", "", 0, -1, -1, false, true));
        put("super_nova.d00", new AdPlugFile(new File(getCacheDir(), "edlib").getAbsolutePath(), "super_nova.d00", "EdLib packed (version 4)", "Super Nova", "Metal & Drax (V)", "", 3272, 68956, 1, true, false));
        put("the_alibi.d00", new AdPlugFile(new File(getCacheDir(), "edlib").getAbsolutePath(), "the_alibi.d00", "EdLib packed (version 1)", "", "",  "Music originally composed by LAXITY on the Commodore 64 (in his own routine), and then later converted to the PC by JCH.  AdLib Player (C) Copyright 1992 Jens-Christian Huus.", 3753, 186624, 1, true, false));
    }};
    private List<AdPlugFile> mActual;
    private long mCount;

    private class TestCallback implements IAdPlugDbCallback {
        @Override
        public void onDBServiceConnected() {
        }

        @Override
        public void onDBServiceDisconnected() {
        }

        @Override
        public void onStatusChanged(dbStatus status) {
            if (status == dbStatus.UNINITIALIZED || status == dbStatus.INITIALIZED) {
                mLatch.countDown();
            }
        }

        @Override
        public void requestInfo(String name, long length) {
            File f = new File(name);
            AdPlugFile song = mExpected.get(f.getName());
            assertNotNull(song);
            mDb.onSongInfo(song.path + File.separator + song.name, song.type, song.title, song.author, song.desc, song.length, song.songlength, song.subsongs, song.valid, song.playlist);
        }

        @Override
        public void onList(List<AdPlugFile> songs) {
            mActual = songs;
            mLatch.countDown();
        }

        @Override
        public void onPlaylist(List<AdPlugFile> songs) {
            mActual = songs;
            mLatch.countDown();
        }

        @Override
        public void onStatus(dbStatus status) {
        }

        @Override
        public void onGetCount(long count) {
            mCount = count;
            mLatch.countDown();
        }

        @Override
        public void onSearch(List<AdPlugFile> songs) {
            mActual = songs;
            mLatch.countDown();
        }
    }

    @Before
    public void setup() {
        mCallback = new TestCallback();
        deleteCacheDir(getCacheDir());
        cacheFiles();

        mDb = new AdPlugDb(InstrumentationRegistry.getInstrumentation().getContext());
        mDb.setCallback(mCallback);
    }

    @After
    public void shutdown() {
    }

    private void prewait(int count) {
        mLatch = new CountDownLatch(count);
    }

    private void await() {
        if (mLatch != null) {
            try {
                assertTrue(mLatch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                assertFalse(e.getMessage(), false);
            }
        }
    }

    private File getCacheDir() {
        return InstrumentationRegistry.getInstrumentation().getContext().getCacheDir();
    }

    private void deleteCacheDir(File dir) {
        File[] list = dir.listFiles();
        if (list == null || list.length == 0) {
            return;
        }
        for (File f : list) {
            if (f.isDirectory()) {
                deleteCacheDir(f);
            }
            f.delete();
        }
    }

    private File mkdir(String path) {
        File cacheDir = getCacheDir();
        File f = null;
        if (path != null && !path.isEmpty()) {
            f = new File(cacheDir, path);
            f.mkdir();
        } else {
            f = cacheDir;
        }
        return f;
    }

    private File fileFromAssets(File path, String fileName) {
        File f = new File(path, fileName);
        try {
            InputStream is = InstrumentationRegistry.getInstrumentation().getContext().getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            int i = is.read(buffer);
            if (i != size) {
                assertFalse("failed to read file: " + fileName, false);
            }
            is.close();
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(buffer);
            fos.close();
        } catch (IOException e) {
            assertFalse(e.getMessage(), false);
        }
        return f;
    }

    private void cacheFiles() {
        File cacheDir = mkdir(null);
        fileFromAssets(cacheDir, "en_lille_test.d00");
        fileFromAssets(cacheDir, "playlist.m3u");
        File d00 = mkdir("d00");
        fileFromAssets(d00, "songs.m3u");
        fileFromAssets(d00, "fresh.d00");
        fileFromAssets(d00, "gone.d00");
        File edlib = mkdir("edlib");
        fileFromAssets(edlib, "test.m3u");
        fileFromAssets(edlib, "super_nova.d00");
        fileFromAssets(edlib, "the_alibi.d00");
    }

    private void compare(File[] files) {
        if (files == null) {
            return;
        }
        for (File f : files) {
            AdPlugFile actualSong = null;
            for (AdPlugFile song : mActual) {
                if (f.getName().equals(song.name)) {
                    actualSong = song;
                    break;
                }
            }
            assertNotNull("Missing: " + f.getName(), actualSong);
            AdPlugFile expectedSong = mExpected.get(f.getName());
            assertEquals(actualSong, expectedSong);
        }
    }

    private void limitedCompare(File[] files) {
        if (files == null) {
            return;
        }
        for (File f : files) {
            AdPlugFile actualSong = null;
            for (AdPlugFile song : mActual) {
                if (f.getName().equals(song.name)) {
                    actualSong = song;
                    break;
                }
            }
            assertNotNull("missing: " + f.getName(), actualSong);
            AdPlugFile expectedSong = mExpected.get(f.getName());
            assertEquals(actualSong.path, expectedSong.path);
            assertEquals(actualSong.name, expectedSong.name);
        }
    }

    @Test
    public void index() {
        File cacheDir = getCacheDir();
        prewait(2);
        mDb.index(cacheDir, false);
        await();
        prewait(1);
        mDb.getCount();
        await();
        assertEquals(10, mCount);
    }

    @Test
    public void delete() {
        File cacheDir = getCacheDir();
        prewait(2);
        mDb.index(cacheDir, false);
        await();

        prewait(1);
        mDb.delete();
        await();

        prewait(1);
        mDb.getCount();
        await();
        assertEquals(0, mCount);
    }

    @Test
    public void index_and_list() {
        File cacheDir = getCacheDir();
        prewait(2);
        mDb.index(cacheDir, false);
        await();

        File[] files = cacheDir.listFiles();
        prewait(1);
        mDb.list(cacheDir, IAdPlugDb.SORTBY_NONE, IAdPlugDb.ORDER_ASCENDING, false, false, false);
        await();
        compare(files);

        cacheDir = new File(getCacheDir(), "d00");
        files = cacheDir.listFiles();
        prewait(1);
        mDb.list(cacheDir, IAdPlugDb.SORTBY_NONE, IAdPlugDb.ORDER_ASCENDING, false, false, false);
        await();
        compare(files);

        cacheDir = new File(getCacheDir(), "edlib");
        files = cacheDir.listFiles();
        prewait(1);
        mDb.list(cacheDir, IAdPlugDb.SORTBY_NONE, IAdPlugDb.ORDER_ASCENDING, false, false, false);
        await();
        compare(files);
    }

    @Test
    public void list() {
        File cacheDir = getCacheDir();
        File[] files = cacheDir.listFiles();
        prewait(1);
        mDb.list(cacheDir, IAdPlugDb.SORTBY_NONE, IAdPlugDb.ORDER_ASCENDING, false, false, false);
        await();
        limitedCompare(files);

        cacheDir = new File(getCacheDir(), "d00");
        files = cacheDir.listFiles();
        prewait(1);
        mDb.list(cacheDir, IAdPlugDb.SORTBY_NONE, IAdPlugDb.ORDER_ASCENDING, false, false, false);
        await();
        limitedCompare(files);

        cacheDir = new File(getCacheDir(), "edlib");
        files = cacheDir.listFiles();
        prewait(1);
        mDb.list(cacheDir, IAdPlugDb.SORTBY_NONE, IAdPlugDb.ORDER_ASCENDING, false, false, false);
        await();
        limitedCompare(files);
    }

    @Test
    public void reindex() {
        File cacheDir = getCacheDir();
        File f = new File(cacheDir, "d00" + File.separator + "fresh.d00");
        assertTrue("delete: fresh.d00", f.delete());
        f = new File(cacheDir, "edlib" + File.separator + "the_alibi.d00");
        assertTrue("delete: fresh.d00", f.delete());
        prewait(2);
        mDb.index(cacheDir, false);
        await();
        prewait(1);
        mDb.getCount();
        await();
        assertEquals(8, mCount);

        f = fileFromAssets(new File(cacheDir, "d00"), "fresh.d00");
        assertTrue("exists: fresh.d00", f.exists());
        f = fileFromAssets(new File(cacheDir, "edlib"), "the_alibi.d00");
        assertTrue("exists: the_alibi.d00", f.exists());
        prewait(1);
        mDb.index(cacheDir, false);
        await();
        prewait(1);
        mDb.getCount();
        await();
        assertEquals(10, mCount);
    }

    @Test
    public void relist() {
        File cacheDir = getCacheDir();
        File f = new File(cacheDir, "d00" + File.separator + "fresh.d00");
        assertTrue("delete: fresh.d00", f.delete());
        f = new File(cacheDir, "edlib" + File.separator + "the_alibi.d00");
        assertTrue("delete: fresh.d00", f.delete());
        prewait(2);
        mDb.index(cacheDir, false);
        await();

        cacheDir = new File(getCacheDir(), "d00");
        File[] files = cacheDir.listFiles();
        assertEquals(2, files.length);
        prewait(1);
        mDb.list(cacheDir, IAdPlugDb.SORTBY_NONE, IAdPlugDb.ORDER_ASCENDING, false, false, false);
        await();
        compare(files);

        cacheDir = new File(getCacheDir(), "edlib");
        files = cacheDir.listFiles();
        assertEquals(2, files.length);
        prewait(1);
        mDb.list(cacheDir, IAdPlugDb.SORTBY_NONE, IAdPlugDb.ORDER_ASCENDING, false, false, false);
        await();
        compare(files);

        f = fileFromAssets(new File(getCacheDir(), "d00"), "fresh.d00");
        assertTrue("exists: fresh.d00", f.exists());
        f = fileFromAssets(new File(getCacheDir(), "edlib"), "the_alibi.d00");
        assertTrue("exists: the_alibi`.d00`", f.exists());

        cacheDir = new File(getCacheDir(), "d00");
        files = cacheDir.listFiles();
        assertEquals(3, files.length);
        prewait(1);
        mDb.list(cacheDir, IAdPlugDb.SORTBY_NONE, IAdPlugDb.ORDER_ASCENDING, false, false, false);
        await();
        limitedCompare(files);

        cacheDir = new File(getCacheDir(), "edlib");
        files = cacheDir.listFiles();
        assertEquals(3, files.length);
        prewait(1);
        mDb.list(cacheDir, IAdPlugDb.SORTBY_NONE, IAdPlugDb.ORDER_ASCENDING, false, false, false);
        await();
        limitedCompare(files);
    }

    @Test
    public void playlist() {
        File cacheDir = getCacheDir();
        prewait(2);
        mDb.index(cacheDir, false);
        await();
        prewait(1);
        mDb.getCount();
        await();
        assertEquals(10, mCount);

        prewait(1);
        mDb.playlist();
        await();
        assertEquals(3, mActual.size());

        prewait(1);
        mDb.search("playlist");
        await();
        assertEquals("playlist.m3u", mActual.get(0).name);

        prewait(1);
        String before = new File(getCacheDir(), "playlist.m3u").getAbsolutePath();
        String after = new File(getCacheDir(), "myplaylist.m3u").getAbsolutePath();
        mDb.rename(before, after);
        mDb.search("myplaylist");
        await();
        assertEquals(1, mActual.size());
        assertEquals("myplaylist.m3u", mActual.get(0).name);
    }

    @Test
    public void add_remove() {
        prewait(1);
        mDb.delete();
        await();

        mDb.add(new File(new File(getCacheDir(), "edlib"), "super_nova.d00").getAbsolutePath(), 1758, false);
        prewait(1);
        mDb.getCount();
        await();
        assertEquals(1, mCount);

        mDb.add(new File(new File(getCacheDir(), "d00"), "gone.d00").getAbsolutePath(), 1758, false);
        mDb.add(new File(getCacheDir().getAbsolutePath(), "super_nova.d00").getAbsolutePath(), 3272, false);
        prewait(1);
        mDb.getCount();
        await();
        assertEquals(2, mCount);

        mDb.remove(new File(new File(getCacheDir(), "edlib"), "super_nova.d00").getAbsolutePath());
        prewait(1);
        mDb.getCount();
        await();
        assertEquals(1, mCount);

        mDb.remove("no.such.file");
        prewait(1);
        mDb.getCount();
        await();
        assertEquals(1, mCount);
    }

    @Test
    public void search() {
        File cacheDir = getCacheDir();
        prewait(1);
        mDb.search("m3u");
        await();
        assertEquals(3, mActual.size());

        prewait(1);
        mDb.search("1992");
        await();
        assertEquals(2, mActual.size());

        prewait(1);
        mDb.search("drax");
        await();
        assertEquals(2, mActual.size());
    }
}
