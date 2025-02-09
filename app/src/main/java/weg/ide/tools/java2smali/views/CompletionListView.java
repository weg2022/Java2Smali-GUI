package weg.ide.tools.java2smali.views;

import static android.text.InputType.TYPE_NULL;
import static android.view.KeyEvent.KEYCODE_BUTTON_A;
import static android.view.KeyEvent.KEYCODE_DPAD_CENTER;
import static android.view.KeyEvent.KEYCODE_DPAD_DOWN;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;
import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.KeyEvent.KEYCODE_MOVE_END;
import static android.view.KeyEvent.KEYCODE_MOVE_HOME;
import static android.view.KeyEvent.KEYCODE_PAGE_DOWN;
import static android.view.KeyEvent.KEYCODE_PAGE_UP;
import static android.view.inputmethod.EditorInfo.IME_FLAG_NO_ENTER_ACTION;
import static android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import weg.ide.tools.java2smali.common.KeyStrokeDetector;


public class CompletionListView extends ExtendListView {
    public CompletionListView(Context context) {
        super(context);
        initView();
    }
    
    public CompletionListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }
    
    public CompletionListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }
    
    private KeyStrokeDetector myKeyStrokeDetector;
    private KeyStrokeDetector.KeyStrokeHandler myKeyStrokeHandler;
    private void initView() {
        
    }
    
    public void setKeyStrokeDetector(KeyStrokeDetector keyStrokeDetector) {
        myKeyStrokeDetector = keyStrokeDetector;
    }
    
    public void setKeyStrokeHandler(KeyStrokeDetector.KeyStrokeHandler keyStrokeHandler) {
        myKeyStrokeHandler = keyStrokeHandler;
    }
    
    @Override
    public boolean onCheckIsTextEditor() {
        return myKeyStrokeDetector!=null;
    }
    
    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.inputType = TYPE_NULL;
        outAttrs.imeOptions = IME_FLAG_NO_EXTRACT_UI |
                IME_FLAG_NO_ENTER_ACTION;
        return myKeyStrokeDetector.createInputConnection(this, myKeyStrokeHandler);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode){
            case KEYCODE_DPAD_UP:
            case KEYCODE_DPAD_DOWN:
            case KEYCODE_DPAD_CENTER:
            case KEYCODE_ENTER:
            case KEYCODE_BUTTON_A:
            case KEYCODE_PAGE_UP:
            case KEYCODE_PAGE_DOWN:
            case KEYCODE_MOVE_HOME:
            case KEYCODE_MOVE_END:
                return superOnKeyDown(keyCode,event);
        }
        return super.onKeyDown(keyCode, event);
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode){
            case KEYCODE_DPAD_UP:
            case KEYCODE_DPAD_DOWN:
            case KEYCODE_DPAD_CENTER:
            case KEYCODE_ENTER:
            case KEYCODE_BUTTON_A:
            case KEYCODE_PAGE_UP:
            case KEYCODE_PAGE_DOWN:
            case KEYCODE_MOVE_HOME:
            case KEYCODE_MOVE_END:
                return superOnKeyUp(keyCode,event);
        }
        return super.onKeyUp(keyCode, event);
    }
}
