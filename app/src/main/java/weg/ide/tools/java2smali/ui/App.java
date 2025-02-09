package weg.ide.tools.java2smali.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import weg.ide.tools.java2smali.ui.services.CodeService;

@SuppressLint("StaticFieldLeak")
public final class App {
    private static Context sContext;
    private static App sApp;
    private static MainUI sUI;
    private static Handler sHandler;
    private static ExecutorService sService;

    private final CodeService myCodeService=new CodeService();
    private App() {
    }


    public static CodeService getCodeService(){
        return sApp.myCodeService;
    }

    public static void initApp(Context context) {
        sContext = context;
    }

    public static void init(MainUI mainUI) {
        sApp = new App();
        sUI = mainUI;
        sHandler = new Handler(Looper.getMainLooper());
        sService = Executors.newFixedThreadPool(4);
    }

    public static void shutdown() {
        sApp.myCodeService.shutdown();
        sApp = null;
    }

    public static boolean isShutdown() {
        return sApp == null;
    }

    public static MainUI getUI() {
        return sUI;
    }

    public static Context getContext() {
        return sContext;
    }

    public static boolean post(Runnable runnable) {
        return sHandler.post(() -> {
            if (runnable != null)
                runnable.run();
        });
    }

    public static void postExec(Runnable runnable) {
        sService.execute(() -> {
            if (runnable != null) {
                runnable.run();
            }
        });
    }

    public static void postExec(Runnable runnable, Runnable uiRun) {
        sService.execute(() -> {
            if (runnable != null)
                runnable.run();
            post(uiRun);
        });
    }
}
