package com.handwerkcloud.client;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class LoggingExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final static String TAG = LoggingExceptionHandler.class.getSimpleName();
    private final static String ERROR_FILE = "exceptions.error";

    private final Context context;
    private final Thread.UncaughtExceptionHandler rootHandler;

    public LoggingExceptionHandler(Context context) {
        this.context = context;
        // we should store the current exception handler -- to invoke it for all not handled exceptions ...
        rootHandler = Thread.getDefaultUncaughtExceptionHandler();
        // we replace the exception handler now with us -- we will properly dispatch the exceptions ...
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(final Thread thread, final Throwable ex) {
        try {
            Log.d(TAG, "called for " + ex.getClass());
            // assume we would write each error in one file ...
            File f = new File(Environment.getExternalStorageDirectory() + File.separator + "HandwerkCloud", ERROR_FILE);
            // log this exception ...
            Writer writer = new StringWriter();
            ex.printStackTrace(new PrintWriter(writer));
            String s = writer.toString();
            FileUtils.writeStringToFile(f, s + " " + System.currentTimeMillis() + "\n", true);
        } catch (Exception e) {
            Log.e(TAG, "Exception Logger failed!", e);
        }
    }

    public static List<String> readExceptions(Context context) {
        List<String> exceptions = new ArrayList<>();
        File f = new File(Environment.getExternalStorageDirectory()+File.separator+"HandwerkCloud", ERROR_FILE);
        if (f.exists()) {
            try {
                exceptions = FileUtils.readLines(f);
            } catch (IOException e) {
                Log.e(TAG, "readExceptions failed!", e);
            }
        }
        return exceptions;
    }
}
