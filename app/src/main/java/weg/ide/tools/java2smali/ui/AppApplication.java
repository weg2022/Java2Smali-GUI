package weg.ide.tools.java2smali.ui;

import android.app.Application;

public class AppApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        App.initApp(getApplicationContext());
    }
}
