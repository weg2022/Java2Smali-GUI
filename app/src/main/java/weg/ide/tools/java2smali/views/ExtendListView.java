package weg.ide.tools.java2smali.views;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;

public class ExtendListView extends android.widget.ListView {
    public ExtendListView(Context context) {
        super(context);
        initView();
    }
    
    public ExtendListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }
    
    public ExtendListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }
    
    public interface OnKeyEventListener{
        boolean onKeyDown(int keyCode, KeyEvent keyEvent);
        
        boolean onKeyUp(int keyCode, KeyEvent keyEvent);
    }
    
    
    private OnKeyEventListener myKeyEventListener;
    private void initView() {
    
    }
    
    public void setOnKeyEventListener(OnKeyEventListener keyEventListener) {
        myKeyEventListener = keyEventListener;
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (myKeyEventListener!=null && myKeyEventListener.onKeyDown(keyCode,event)){
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (myKeyEventListener!=null && myKeyEventListener.onKeyUp(keyCode,event)){
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
    
    public boolean superOnKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }
    
    public boolean superOnKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }
    
    @Override
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        if (getFocusedChild()==null){
            return false;
        }

        return super.requestFocus(direction, previouslyFocusedRect);
    }


}
