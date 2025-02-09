package weg.ide.tools.java2smali.common;

import static android.content.res.Configuration.KEYBOARD_NOKEYS;
import static android.view.KeyEvent.FLAG_SOFT_KEYBOARD;
import static android.view.KeyEvent.KEYCODE_ALT_LEFT;
import static android.view.KeyEvent.KEYCODE_ALT_RIGHT;
import static android.view.KeyEvent.KEYCODE_BACK;
import static android.view.KeyEvent.KEYCODE_CTRL_LEFT;
import static android.view.KeyEvent.KEYCODE_CTRL_RIGHT;
import static android.view.KeyEvent.KEYCODE_DEL;
import static android.view.KeyEvent.KEYCODE_HOME;
import static android.view.KeyEvent.KEYCODE_SEARCH;
import static android.view.KeyEvent.KEYCODE_SHIFT_LEFT;
import static android.view.KeyEvent.KEYCODE_SHIFT_RIGHT;
import static android.view.KeyEvent.KEYCODE_UNKNOWN;
import static java.lang.Character.isISOControl;
import static java.lang.Character.isUpperCase;
import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;

import android.content.Context;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class KeyStrokeDetector {
    public static boolean DEBUG = false;
    private static final String TAG = "KeyStrokeDetector";
    private boolean altLeftDown;
    private boolean altRightDown;
    private boolean ctrlLeftDown;
    private boolean ctrlRightDown;
    private boolean isSoftKeyboard;
    private KeyCharacterMap keyCharacterMap;
    private int lastComposingTextLength;
    private boolean realShiftLeftDown;
    private boolean realShiftRightDown;
    private boolean shiftLeftDown;
    private boolean shiftRightDown;
    private boolean unknownDown;
    
    public interface KeyStrokeHandler {
        boolean onKeyStroke(@NonNull KeyStroke keyStroke);
    }
    
    public static class KeyStoreInputConnection extends BaseInputConnection {
        
        private final KeyStrokeDetector myDetector;
        private final KeyStrokeHandler myHandler;
        
        public KeyStoreInputConnection(@NonNull KeyStrokeDetector detector, @NonNull KeyStrokeHandler handler, @NonNull View targetView, boolean fullEditor) {
            super(targetView, fullEditor);
            myDetector = detector;
            myHandler = handler;
        }
        
        @NonNull
        public KeyStrokeDetector getKeyStrokeDetector() {
            return myDetector;
        }
        
        @NonNull
        public KeyStrokeHandler getKeyStoreHandler() {
            return myHandler;
        }
        
        @Override
        public boolean setComposingText(CharSequence text, int newCursorPosition) {
            myDetector.d("setComposingText '" + text + "'");
            for (int i = 0; i < myDetector.lastComposingTextLength; i++) {
                myHandler.onKeyStroke(new KeyStroke(KEYCODE_DEL, false, false, false));
            }
            myDetector.setComposingTextLength(text.length());
            sendAsCharKeyStrokes(text, myDetector.isSoftKeyboard);
            return true;
        }
        
        
        @Override
        public boolean finishComposingText() {
            myDetector.d("finishComposingText");
            myDetector.newWordStarted();
            return super.finishComposingText();
        }
        
        @Override
        public boolean commitText(CharSequence text, int newCursorPosition) {
            myDetector.d("commitText '" + text + "'");
            for (int i = 0; i < myDetector.lastComposingTextLength; i++) {
                myHandler.onKeyStroke(new KeyStroke(KEYCODE_DEL, false, false, false));
            }
            myDetector.newWordStarted();
            if (text.length() == 1 && text.charAt(0) == '\n') {
                sendAsKeyEvents(text, myDetector.isSoftKeyboard);
            } else {
                sendAsCharKeyStrokes(text, myDetector.isSoftKeyboard);
            }
            return true;
        }
        
        @Override
        public boolean deleteSurroundingText(int leftLength, int rightLength) {
            myDetector.d("deleteSurroundingText " + leftLength + " " + rightLength);
            myDetector.newWordStarted();
            for (int i = 0; i < leftLength; i++) {
                myHandler.onKeyStroke(new KeyStroke(KEYCODE_DEL, false, false, false));
            }
            return true;
        }
        
        
        protected void sendAsCharKeyStrokes(@NonNull CharSequence text, boolean isSoftKeyboard) {
            for (int j = 0; j < text.length(); j++) {
                char ch = text.charAt(j);
                if (!isSoftKeyboard) {
                    if (!myDetector.realShiftLeftDown && !myDetector.realShiftRightDown) {
                        ch = toLowerCase(ch);
                    } else
                        ch = toUpperCase(ch);
                }
                myHandler.onKeyStroke(myDetector.makeKeyStroke(ch));
            }
        }
        
        protected void sendAsKeyEvents(@NonNull CharSequence text, boolean isSoftKeyboard) {
            if (myDetector.keyCharacterMap == null) {
                myDetector.keyCharacterMap = KeyCharacterMap.load(0);
            }
            for (int j = 0; j < text.length(); j++) {
                char ch = text.charAt(j);
                if (!isSoftKeyboard) {
                    if (!myDetector.realShiftLeftDown && !myDetector.realShiftRightDown) {
                        ch = toLowerCase(ch);
                    } else {
                        ch = toUpperCase(ch);
                    }
                }
                char[] chars = {ch};
                KeyEvent[] events = myDetector.keyCharacterMap.getEvents(chars);
                if (events != null) {
                    for (KeyEvent event : events) {
                        sendKeyEvent(event);
                    }
                }
            }
        }
        
        protected KeyEvent transformEvent(@NonNull KeyEvent event) {
            return new KeyEvent(event.getDownTime(), event.getEventTime(), event.getAction(), event.getKeyCode(), event.getRepeatCount(), event.getMetaState(), event.getDeviceId(), event.getScanCode(), event.getFlags() | 4 | 2);
        }
        
        @Override
        public boolean sendKeyEvent(@NonNull KeyEvent event) {
            myDetector.d("sendKeyEvent " + event.getKeyCode());
            myDetector.newWordStarted();
            return super.sendKeyEvent(transformEvent(event));
        }
        
        @Override
        public CharSequence getTextBeforeCursor(int length, int flags) {
            int min = Math.min(length, 1024);
            StringBuilder sb = new StringBuilder(min);
            for (int i = 0; i < min; i++) {
                sb.append(' ');
            }
            return sb;
        }
    }
    
    public KeyStrokeDetector(@NonNull Context context) {
        isSoftKeyboard = context.getResources().getConfiguration().keyboard == KEYBOARD_NOKEYS;
        d("new KeyStrokeDetector() - isSoftKeyboard: " + isSoftKeyboard);
    }
    
    public void configChange(@NonNull Context context) {
        isSoftKeyboard = context.getResources().getConfiguration().keyboard == KEYBOARD_NOKEYS;
        d("KeyStrokeDetector.configChange - isSoftKeyboard: " + isSoftKeyboard);
        keyCharacterMap = null;
    }
    
    public boolean isSoftKeyboard() {
        return isSoftKeyboard;
    }
    
    public void newWordStarted() {
        this.lastComposingTextLength = 0;
    }
    
    public void setComposingTextLength(int composingLength) {
        this.lastComposingTextLength = composingLength;
    }
    
    public boolean isCtrlPressed() {
        return this.ctrlLeftDown || this.ctrlRightDown;
    }
    
    @NonNull
    public KeyStoreInputConnection createInputConnection(@NonNull View view, @NonNull KeyStrokeHandler keyStrokeHandler) {
        return new KeyStoreInputConnection(this, keyStrokeHandler, view, true);
    }
    
    public boolean keyDown(int keyCode, @NonNull KeyEvent event, @NonNull KeyStrokeHandler handler) {
        debugOnKey("onKeyDown", keyCode, event);
        int oldKeyCode = keyCode;
        if (keyCode == KEYCODE_SEARCH)
            keyCode = KEYCODE_ALT_LEFT;
        
        handleMetaKeysDown(keyCode, (event.getFlags() & FLAG_SOFT_KEYBOARD) != 0);
        KeyStroke keyStroke = makeKeyStroke(keyCode, event);
        if (keyStroke == null||!handler.onKeyStroke(keyStroke))
            return false;
        
        debugOnKeyStroke(keyStroke);
        if (oldKeyCode==KEYCODE_SEARCH)
            return true;
        
        return true;
    }
    
    public void d(@NonNull String msg) {
        if (DEBUG)
            Log.d(TAG, msg);
    }
    
    private void debugOnKeyStroke(@NonNull KeyStroke keyStroke) {
        d("onKeyStroke " + keyStroke);
    }
    
    private void debugOnKey(String method, int keyCode, @NonNull KeyEvent event) {
        d(method + " " + keyCode + "  " + event.getFlags() +
                (event.isAltPressed() ? " alt" : "") +
                (event.isShiftPressed() ? " shift" : "") +
                " " +
                (isCtrl(event.getMetaState()) ? " ctrl" : ""));
    }
    
    private boolean isCtrl(int metaState) {
        return (metaState & 12288) != 0;
    }
    
    public boolean keyUp(int keyCode, @NonNull KeyEvent event) {
        debugOnKey("onKeyUp", keyCode, event);
        int oldKeyCode = keyCode;
        if (keyCode == KEYCODE_SEARCH)
            keyCode = KEYCODE_ALT_LEFT;
        
        handleMetaKeysUp(keyCode, (event.getFlags() & FLAG_SOFT_KEYBOARD) != 0);
    
        return oldKeyCode == KEYCODE_SEARCH;
    }
    
    @NonNull
    public KeyStroke makeKeyStroke(char c) {
        return new KeyStroke(-1, c,
                isUpperCase(c) | realShiftLeftDown | realShiftRightDown,
                false, false);
    }
    
    @Nullable
    private KeyStroke makeKeyStroke(int keyCode, @NonNull KeyEvent event) {
        switch (keyCode) {
            case KEYCODE_UNKNOWN,
                    KEYCODE_HOME,
                    KEYCODE_BACK,
                    KEYCODE_ALT_LEFT,
                    KEYCODE_ALT_RIGHT,
                    KEYCODE_SHIFT_LEFT,
                    KEYCODE_SHIFT_RIGHT,
                    KEYCODE_CTRL_LEFT,
                    KEYCODE_CTRL_RIGHT -> {
                AppLog.i(this,"NULL");
                return null;
            }
            default -> {
                boolean shift = shiftLeftDown | shiftRightDown | event.isShiftPressed();
                boolean ctrl = ctrlLeftDown | ctrlRightDown | isCtrl(event.getMetaState());
                boolean alt = altLeftDown | altRightDown | event.isAltPressed();
                char ch = 65535;
                int unicodeChar = event.getUnicodeChar();
                if (unicodeChar != 0 && !isISOControl(unicodeChar))
                    ch = (char) unicodeChar;
                return new KeyStroke(keyCode, ch, shift, ctrl, alt);
            }
        }
    }
    
    public void activityKeyUp(int keyCode, @NonNull KeyEvent event) {
        handleMetaKeysUp(keyCode, (event.getFlags() & FLAG_SOFT_KEYBOARD) != 0);
    }
    
    public void activityKeyDown(int keyCode, @NonNull KeyEvent event) {
        handleMetaKeysDown(keyCode, (event.getFlags() & FLAG_SOFT_KEYBOARD) != 0);
    }
    
    private void handleMetaKeysDown(int keyCode, boolean isSoftKeyboard) {
        d("onMetaKeysDown " + keyCode);
        this.altLeftDown = (keyCode == KEYCODE_ALT_LEFT) | altLeftDown;
        this.altRightDown = (keyCode == KEYCODE_ALT_RIGHT) | altRightDown;
        this.shiftLeftDown = (keyCode == KEYCODE_SHIFT_LEFT) | shiftLeftDown;
        this.shiftRightDown = (keyCode == KEYCODE_SHIFT_RIGHT) | shiftRightDown;
        this.realShiftLeftDown = (keyCode == KEYCODE_SHIFT_LEFT && !isSoftKeyboard) | realShiftLeftDown;
        this.realShiftRightDown = (keyCode == KEYCODE_SHIFT_RIGHT && !isSoftKeyboard) | realShiftRightDown;
        this.unknownDown = (keyCode == KEYCODE_UNKNOWN) | unknownDown;
        this.ctrlLeftDown = (keyCode == KEYCODE_CTRL_LEFT) | ctrlLeftDown;
        this.ctrlRightDown |= keyCode == KEYCODE_CTRL_RIGHT;
    }
    
    private void handleMetaKeysUp(int keyCode, boolean isSoftKeyboard) {
        d("onMetaKeysUp " + keyCode);
        this.altLeftDown = (keyCode != KEYCODE_ALT_LEFT) & altLeftDown;
        this.altRightDown = (keyCode != KEYCODE_ALT_RIGHT) & altRightDown;
        this.shiftLeftDown = (keyCode != KEYCODE_SHIFT_LEFT) & shiftLeftDown;
        this.shiftRightDown = (keyCode != KEYCODE_SHIFT_RIGHT) & shiftRightDown;
        this.realShiftLeftDown = (keyCode != KEYCODE_SHIFT_LEFT || isSoftKeyboard) & realShiftLeftDown;
        this.realShiftRightDown = (keyCode != KEYCODE_SHIFT_RIGHT || isSoftKeyboard) & realShiftRightDown;
        this.unknownDown = (keyCode != KEYCODE_UNKNOWN) & unknownDown;
        this.ctrlLeftDown = (keyCode != KEYCODE_CTRL_LEFT) & ctrlLeftDown;
        this.ctrlRightDown &= keyCode != KEYCODE_CTRL_RIGHT;
    }
}
