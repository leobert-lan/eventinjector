package net.pocketmagic.android.utils;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

/**
 * <p><b>Package:</b> net.pocketmagic.android.utils </p>
 * <p><b>Project:</b> eventinjector </p>
 * <p><b>Classname:</b> MainThread </p>
 * <p><b>Description:</b> TODO </p>
 * Created by leobert on 2021/11/26.
 */
public class MainThread {
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void post(@NonNull Runnable runnable) {
        mainHandler.post(runnable);
    }

    public static void delay(long millis, @NonNull Runnable runnable) {
        mainHandler.postDelayed(runnable, millis);
    }
}
