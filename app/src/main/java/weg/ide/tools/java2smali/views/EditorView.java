package weg.ide.tools.java2smali.views;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.AttributeSet;

import weg.ide.tools.java2smali.ui.App;
import weg.ide.tools.java2smali.views.editor.Editor;
import weg.ide.tools.java2smali.views.editor.Theme;

public class EditorView extends Editor {
    public EditorView(Context context) {
        super(context);
        initView();
    }

    public EditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public EditorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private ClipboardManager myClipboardManager;

    private void initView() {
        setSaveEnabled(true);
        setEditable(true);
        setCaretVisible(true);
        setFontSize(14);
        applyColorScheme(Theme.LightColorScheme);
    }


    @Override
    public void setFontSize(int fontSize) {
        if (fontSize<10 || fontSize>40)return;
        super.setFontSize(fontSize);
    }

    public void insert(int offset, String text) {
        getModel().insert(offset, text);
    }

    public void remove(int offset, int length) {
        getModel().remove(offset, length);
    }

    public void replace(int offset, int length, String text) {
        getModel().replace(offset, length, text);
    }

    public void insertText(String text) {
        selectionRemove();
        getModel().insert(getCaretOffset(), text);
    }

    public void copy() {
        getClipboardManager().setPrimaryClip(ClipData.newPlainText(null, getSelectionText()));
        unselectAll();
    }

    public boolean canCopy() {
        return isSelectionMode() && getSelectionLength() > 0;
    }

    public void cut() {
        getClipboardManager().setPrimaryClip(ClipData.newPlainText(null, getSelectionText()));
        selectionRemove();
    }

    public boolean canCut() {
        return isSelectionMode() && getSelectionLength() > 0 && isEditable();
    }

    public void paste() {
        if (getClipboardManager().getPrimaryClip() != null) {
            if (getClipboardManager().getPrimaryClip().getItemCount() > 0) {
                ClipData.Item item = getClipboardManager().getPrimaryClip().getItemAt(0);
                if (getSelectionLength() > 0)
                    selectionRemove();
                getModel().insert(getCaretOffset(), item.getText().toString());
            }
        }
    }

    public boolean isSelectColor() {
        if (!isSelectionMode()) return false;

        try {
            return android.graphics.Color.parseColor(getSelectionText()) != -1;
        } catch (Exception ignored) {

        }
        return false;
    }

    public boolean canPaste() {
        return isSelectionMode() && isEditable();
    }

    public void selectAll() {
        setSelection(0, getCharCount());
    }

    public void unselectAll() {
        cancelSelection();
    }

    public boolean canUnselectAll() {
        return isSelectionMode();
    }

    public boolean canSelectAll() {
        return !isSelectionMode() || (getSelectionStart() > 0 || getSelectionEnd() < getCharCount());
    }

    public ClipboardManager getClipboardManager() {
        if (myClipboardManager == null)
            myClipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        return myClipboardManager;
    }

    public boolean isImeShowing() {
        return getInputMethodManager().isActive();
    }

    public void showIme() {
        if (isEditable())
            getInputMethodManager().showSoftInput(this, 0);
    }

    public void hideIme() {
        getInputMethodManager().hideSoftInputFromWindow(getWindowToken(), 0);
    }

    public String[] getQuickKeys() {
        StringBuilder indent = new StringBuilder();
        int indentationSize = getIndentationSize();
        int index = 0;
        if (indentationSize % getTabSize() == 0) {
            while (index < indentationSize / getTabSize()) {
                indent.append("\t");
                index++;
            }
        } else {
            while (index < indentationSize) {
                indent.append("s");
                index++;
            }
        }
        return new String[]{
                indent.toString(),
                "{", "}",
                "(", ")",
                ";", ",", ".",
                "=",
                "\"", "'",
                "|", "&", "!",
                "[", "]",
                "<", ">",
                "+", "-",
                "/", "*",
                "?", ":",
                "_"
        };
    }
    
    @Override
    protected int getQuickKeyBarHeight() {
       var bar = App.getUI().getQuickKeyBar();
       if (bar!=null){
           return bar.getHeight();
       }
        return super.getQuickKeyBarHeight();
    }
}
