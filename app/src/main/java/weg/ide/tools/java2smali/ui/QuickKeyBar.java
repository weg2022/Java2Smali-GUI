package weg.ide.tools.java2smali.ui;

import static android.view.LayoutInflater.from;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams;

import android.annotation.SuppressLint;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class QuickKeyBar {
    
    
    private final MainUI myMainUI;
    private boolean myIsOpened=true;
    private KeyCharacterMap myKeyCharacterMap;

    private View myView;
    
    public QuickKeyBar(MainUI mainUI) {
        myMainUI = mainUI;
        ViewGroup container=myMainUI.findViewById(R.id.quickKeyBarContainer);
        myView = LayoutInflater.from(mainUI).inflate(R.layout.quickkeybar,container,true);

        
        myView.findViewById(R.id.quickKeyBarOpenButton).setOnClickListener(v -> {
            open(true);
        });
        
        myView.findViewById(R.id.quickKeyBarCloseButton).setOnClickListener(v -> {
            open(false);
        });
        
    }
    
    @SuppressLint("InflateParams")
    public void setKeys(String[] keys) {
        ViewGroup quickKeyBarList = myView.findViewById(R.id.quickKeyBarList);
        quickKeyBarList.removeAllViews();
   
        if (keys==null){
            return;
        }
        
        int keyWidth = (int) (40.0f * myMainUI.getResources().getDisplayMetrics().density);
        int keyHeight = (int) (40.0f * myMainUI.getResources().getDisplayMetrics().density);
    
        for (String key : keys) {
            String keyText = key.replace("s", "");
            var KeyView = (TextView) from(myMainUI)
                    .inflate(R.layout.quickkeybar_key, null);
            if (keyText.trim().length() == 0) {
                KeyView.setText("⇥");
            } else {
                KeyView.setText(keyText);
            }
            quickKeyBarList.addView(KeyView, new LayoutParams(keyWidth, keyHeight));
            KeyView.setOnClickListener(v -> {
                if (myKeyCharacterMap == null)
                    myKeyCharacterMap = KeyCharacterMap.load(-1);
                
                KeyEvent[] events = myKeyCharacterMap
                        .getEvents(keyText.toCharArray());
                
                if (events != null) {
                    for (KeyEvent event : events) {
                        myMainUI.dispatchKeyEvent(event);
                    }
                }
            });
        }
    }
    
    private void open(boolean showing) {
        myIsOpened = showing;
        
        myView.findViewById(R.id.quickKeyBarOpenButtonContainer)
                .setVisibility(myIsOpened ? GONE : VISIBLE);
        
        myView.findViewById(R.id.quickKeyBarKeysContainer)
                .setVisibility(myIsOpened ? VISIBLE : GONE);
    }
    
    public void showQuickKeyBar(boolean showing) {
        myView.findViewById(R.id.quickKeyBar)
                .setVisibility(showing ? VISIBLE : GONE);
    }
    
    
    public int getHeight() {
        if (myIsOpened) {
            return (int) (myMainUI.getResources().getDisplayMetrics().density * 40.0f);
        }
        return 0;
    }
}
