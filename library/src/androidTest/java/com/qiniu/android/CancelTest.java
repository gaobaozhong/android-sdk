package com.qiniu.android;

import android.test.InstrumentationTestCase;
import android.util.Log;

import com.qiniu.android.common.Config;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpCancellationSignal;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.storage.persistent.FileRecorder;

import junit.framework.Assert;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by Simon on 2015/4/15.
 */
public class CancelTest extends InstrumentationTestCase {

    final CountDownLatch signal = new CountDownLatch(1);
    final CountDownLatch signal2 = new CountDownLatch(1);
    boolean cancelled;
    boolean failed;
    private UploadManager uploadManager;
    private String key;
    private volatile ResponseInfo info;
    private JSONObject resp;
    private volatile UploadOptions options;

    @Override
    protected void setUp() throws Exception {
        File f = File.createTempFile("qiniutest", "b");
        String folder = f.getParent();
        FileRecorder fr = new FileRecorder(folder);
        uploadManager = new UploadManager(fr);
    }

    public void test400k() throws Throwable {
        templateFile(400, 0.2);
    }

    public void test700k() throws Throwable {
        templateFile(700, 0.2);
    }

    public void test1M() throws Throwable {
        templateFile(1024, 0.51);
    }

    public void test4M() throws Throwable {
        templateFile(4 * 1024, 0.8);
    }

    public void test8M1K() throws Throwable {
        templateFile(8 * 1024 + 1, 0.6);
    }

    public void testD400k() throws Throwable {
        templateData(400, 0.2);
    }

    public void testD700k() throws Throwable {
        templateData(700, 0.2);
    }

    public void testD1M() throws Throwable {
        templateData(1024, 0.51);
    }

    public void testD4M() throws Throwable {
        templateData(4 * 1024, 0.6);
    }


    private void templateFile(final int size, final double pos) throws Throwable {
        final File tempFile = TempFile.createFile(size);
        final String expectKey = "rc=" + size + "k";
        cancelled = false;
        failed = false;
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:a", "test");
        params.put("x:b", "test2");
        options = new UploadOptions(params, null, false, new UpProgressHandler() {
            @Override
            public void progress(String key, double percent) {
                if (percent >= pos) {
                    cancelled = true;
                }
                Log.i("qiniutest", pos + ": progress " + percent);
            }
        }, new UpCancellationSignal() {
            @Override
            public boolean isCancelled() {
                return cancelled;
            }
        });
        runTestOnUiThread(new Runnable() { // THIS IS THE KEY TO SUCCESS
            public void run() {
                uploadManager.put(tempFile, expectKey, TestConfig.token, new UpCompletionHandler() {
                    public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                        Log.i("qiniutest", k + rinfo);
                        key = k;
                        info = rinfo;
                        resp = response;
                        signal.countDown();
                    }
                }, options);
            }
        });

        try {
            signal.await(1200, TimeUnit.SECONDS); // wait for callback
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(expectKey, key);
        Assert.assertTrue(info.isCancelled());
        Assert.assertNull(resp);

//        cancelled = false;
//        options = new UploadOptions(null, null, false, new UpProgressHandler() {
//            @Override
//            public void progress(String key, double percent) {
//                if (percent < pos - Config.CHUNK_SIZE / (size * 1024.0)) {
//                    failed = true;
//                }
//                Log.i("qiniutest", "continue progress " + percent);
//            }
//        }, null);
//
//        runTestOnUiThread(new Runnable() { // THIS IS THE KEY TO SUCCESS
//            public void run() {
//                uploadManager.put(tempFile, expectKey, TestConfig.token, new UpCompletionHandler() {
//                    public void complete(String k, ResponseInfo rinfo, JSONObject response) {
//                        Log.i("qiniutest", k + rinfo);
//                        key = k;
//                        info = rinfo;
//                        resp = response;
//                        signal2.countDown();
//                    }
//                }, options);
//            }
//        });
//
//        try {
//            signal2.await(1200, TimeUnit.SECONDS); // wait for callback
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        Assert.assertEquals(expectKey, key);
//        Assert.assertTrue(info.isOK());
//        Assert.assertTrue(!failed);
//        Assert.assertNotNull(resp);

        TempFile.remove(tempFile);
    }

    private void templateData(final int size, final double pos) throws Throwable {
        final byte[] tempDate = TempFile.getByte(1024 * size);
        final String expectKey = "rc=" + size + "k" + "_byte";
        cancelled = false;
        failed = false;
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:a", "test");
        params.put("x:b", "test2");
        options = new UploadOptions(params, null, false, new UpProgressHandler() {
            @Override
            public void progress(String key, double percent) {
                if (percent >= pos) {
                    cancelled = true;
                }
                Log.i("qiniutest", pos + ": progress " + percent);
            }
        }, new UpCancellationSignal() {
            @Override
            public boolean isCancelled() {
                return cancelled;
            }
        });
        runTestOnUiThread(new Runnable() { // THIS IS THE KEY TO SUCCESS
            public void run() {
                uploadManager.put(tempDate, expectKey, TestConfig.token, new UpCompletionHandler() {
                    public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                        Log.i("qiniutest", k + rinfo);
                        key = k;
                        info = rinfo;
                        resp = response;
                        signal.countDown();
                    }
                }, options);
            }
        });

        try {
            signal.await(1200, TimeUnit.SECONDS); // wait for callback
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(expectKey, key);
        Assert.assertTrue(info.isCancelled());
        Assert.assertNull(resp);

//        cancelled = false;
//        options = new UploadOptions(null, null, false, new UpProgressHandler() {
//            @Override
//            public void progress(String key, double percent) {
//                if (percent < pos - Config.CHUNK_SIZE / (size * 1024.0)) {
//                    failed = true;
//                }
//                Log.i("qiniutest", "continue progress " + percent);
//            }
//        }, null);
//
//        runTestOnUiThread(new Runnable() { // THIS IS THE KEY TO SUCCESS
//            public void run() {
//                uploadManager.put(tempDate, expectKey, TestConfig.token, new UpCompletionHandler() {
//                    public void complete(String k, ResponseInfo rinfo, JSONObject response) {
//                        Log.i("qiniutest", k + rinfo);
//                        key = k;
//                        info = rinfo;
//                        resp = response;
//                        signal2.countDown();
//                    }
//                }, options);
//            }
//        });
//
//        try {
//            signal2.await(1200, TimeUnit.SECONDS); // wait for callback
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        Assert.assertEquals(expectKey, key);
//        Assert.assertTrue(info.isOK());
//        Assert.assertTrue(!failed);
//        Assert.assertNotNull(resp);
    }

}
