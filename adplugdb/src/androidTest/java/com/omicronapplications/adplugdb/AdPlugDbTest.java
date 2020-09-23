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
    private IAdPlugDb mService;
    private IAdPlugDbCallback mCallback;
    private AdPlugDbController mController;
    private CountDownLatch mLatch;
    private Map<String, AdPlugFile> mExpected = new HashMap<String, AdPlugFile>() {{
        put("en_lille_test.d00", new AdPlugFile(getCacheDir().getAbsolutePath(), "en_lille_test.d00", "EdLib packed (version 4)", "En lille test", "Morten Sigaard", "", 2563, 60324, 1, true, false));
        put("d00", new AdPlugFile(getCacheDir().getAbsolutePath(), "d00"));
        put("fresh.d00", new AdPlugFile(new File(getCacheDir(), "d00").getAbsolutePath(), "fresh.d00", "EdLib packed (version 4)", "Fresh", "Morten Sigaard", "", 1497, 21342, 1, true, false));
        put("gone.d00", new AdPlugFile(new File(getCacheDir(), "d00").getAbsolutePath(), "gone.d00", "EdLib packed (version 1)", "", "",  "\"GONE...\" by DRAX - converted by JCH, 13/1-1992. Player & music (C) Vibrants, 1992.", 1758, 76886, 1, true, false));
        put("edlib", new AdPlugFile(getCacheDir().getAbsolutePath(), "edlib"));
        put("super_nova.d00", new AdPlugFile(new File(getCacheDir(), "edlib").getAbsolutePath(), "super_nova.d00", "EdLib packed (version 4)", "Super Nova", "Metal & Drax (V)", "", 3272, 68956, 1, true, false));
        put("the_alibi.d00", new AdPlugFile(new File(getCacheDir(), "edlib").getAbsolutePath(), "the_alibi.d00", "EdLib packed (version 1)", "", "",  "Music originally composed by LAXITY on the Commodore 64 (in his own routine), and then later converted to the PC by JCH.  AdLib Player (C) Copyright 1992 Jens-Christian Huus.", 3753, 186624, 1, true, false));
    }};
    private List<AdPlugFile> mActual;
    private long mCount;

    private class TestCallback implements IAdPlugDbCallback {
        @Override
        public void onDBServiceConnected() {
            mService = mController.getService();
            assertNotNull("IAdPlugDb", mService);
            mService.delete();
            if (mLatch != null) {
                mLatch.countDown();
            }
        }

        @Override
        public void onDBServiceDisconnected() {
            mService = null;
        }

        @Override
        public void onStatusChanged(dbStatus status) {
            if (status == dbStatus.INITIALIZED) {
                mLatch.countDown();
            }
        }

        @Override
        public void requestInfo(String name, long length) {
            File f = new File(name);
            AdPlugFile song = mExpected.get(f.getName());
            assertNotNull(song);
            mService.onSongInfo(song.path + File.separator + song.name, song.type, song.title, song.author, song.desc, song.length, song.songlength, song.subsongs, song.valid, song.playlist);
        }

        @Override
        public void onList(List<AdPlugFile> songs) {
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
    }

    @Before
    public void setup() {
        mCallback = new TestCallback();
        deleteCacheDir(getCacheDir());
        cacheFiles();

        mLatch = new CountDownLatch(1);
        mController = new AdPlugDbController(mCallback, InstrumentationRegistry.getInstrumentation().getTargetContext());
        mController.startDB();
        await();
    }

    @After
    public void shutdown() {
        if (mController != null) {
            mController.stopDB();
            mController = null;
        }
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
        File d00 = mkdir("d00");
        fileFromAssets(d00, "fresh.d00");
        fileFromAssets(d00, "gone.d00");
        File edlib = mkdir("edlib");
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
        mLatch = new CountDownLatch(1);
        mService.index(cacheDir.getAbsolutePath(), false);
        await();
        mLatch = new CountDownLatch(1);
        mService.getCount();
        await();
        assertEquals(7, mCount);
    }

    @Test
    public void index_and_list() {
        File cacheDir = getCacheDir();
        mLatch = new CountDownLatch(1);
        mService.index(cacheDir.getAbsolutePath(), false);
        await();

        File[] files = cacheDir.listFiles();
        mService.list(cacheDir.getAbsolutePath(), IAdPlugDb.SORT_NONE, false, false);
        mLatch = new CountDownLatch(1);
        await();
        compare(files);

        cacheDir = new File(getCacheDir(), "d00");
        files = cacheDir.listFiles();
        mService.list(cacheDir.getAbsolutePath(), IAdPlugDb.SORT_NONE, false, false);
        mLatch = new CountDownLatch(1);
        await();
        compare(files);

        cacheDir = new File(getCacheDir(), "edlib");
        files = cacheDir.listFiles();
        mService.list(cacheDir.getAbsolutePath(), IAdPlugDb.SORT_NONE, false, false);
        mLatch = new CountDownLatch(1);
        await();
        compare(files);
    }

    @Test
    public void list() {
        File cacheDir = getCacheDir();
        File[] files = cacheDir.listFiles();
        mService.list(cacheDir.getAbsolutePath(), IAdPlugDb.SORT_NONE, false, false);
        mLatch = new CountDownLatch(1);
        await();
        limitedCompare(files);

        cacheDir = new File(getCacheDir(), "d00");
        files = cacheDir.listFiles();
        mService.list(cacheDir.getAbsolutePath(), IAdPlugDb.SORT_NONE, false, false);
        mLatch = new CountDownLatch(1);
        await();
        limitedCompare(files);

        cacheDir = new File(getCacheDir(), "edlib");
        files = cacheDir.listFiles();
        mService.list(cacheDir.getAbsolutePath(), IAdPlugDb.SORT_NONE, false, false);
        mLatch = new CountDownLatch(1);
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
        mLatch = new CountDownLatch(1);
        mService.index(cacheDir.getAbsolutePath(), false);
        await();
        mLatch = new CountDownLatch(1);
        mService.getCount();
        await();
        assertEquals(5, mCount);

        f = fileFromAssets(new File(cacheDir, "d00"), "fresh.d00");
        assertTrue("exists: fresh.d00", f.exists());
        f = fileFromAssets(new File(cacheDir, "edlib"), "the_alibi.d00");
        assertTrue("exists: the_alibi.d00", f.exists());
        mLatch = new CountDownLatch(1);
        mService.index(cacheDir.getAbsolutePath(), false);
        await();
        mLatch = new CountDownLatch(1);
        mService.getCount();
        await();
        assertEquals(7, mCount);
    }

    @Test
    public void relist() {
        File cacheDir = getCacheDir();
        File f = new File(cacheDir, "d00" + File.separator + "fresh.d00");
        assertTrue("delete: fresh.d00", f.delete());
        f = new File(cacheDir, "edlib" + File.separator + "the_alibi.d00");
        assertTrue("delete: fresh.d00", f.delete());
        mLatch = new CountDownLatch(1);
        mService.index(cacheDir.getAbsolutePath(), false);
        await();

        cacheDir = new File(getCacheDir(), "d00");
        File[] files = cacheDir.listFiles();
        assertEquals(1, files.length);
        mService.list(cacheDir.getAbsolutePath(), IAdPlugDb.SORT_NONE, false, false);
        mLatch = new CountDownLatch(1);
        await();
        compare(files);

        cacheDir = new File(getCacheDir(), "edlib");
        files = cacheDir.listFiles();
        assertEquals(1, files.length);
        mService.list(cacheDir.getAbsolutePath(), IAdPlugDb.SORT_NONE, false, false);
        mLatch = new CountDownLatch(1);
        await();
        compare(files);

        f = fileFromAssets(new File(getCacheDir(), "d00"), "fresh.d00");
        assertTrue("exists: fresh.d00", f.exists());
        f = fileFromAssets(new File(getCacheDir(), "edlib"), "the_alibi.d00");
        assertTrue("exists: the_alibi.d00", f.exists());

        cacheDir = new File(getCacheDir(), "d00");
        files = cacheDir.listFiles();
        assertEquals(2, files.length);
        mService.list(cacheDir.getAbsolutePath(), IAdPlugDb.SORT_NONE, false, false);
        mLatch = new CountDownLatch(1);
        await();
        limitedCompare(files);

        cacheDir = new File(getCacheDir(), "edlib");
        files = cacheDir.listFiles();
        assertEquals(2, files.length);
        mService.list(cacheDir.getAbsolutePath(), IAdPlugDb.SORT_NONE, false, false);
        mLatch = new CountDownLatch(1);
        await();
        limitedCompare(files);
    }
}
