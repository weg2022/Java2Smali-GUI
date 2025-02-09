package weg.ide.tools.java2smali.ui;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Vector;

import weg.ide.tools.java2smali.common.HighlightSpace;
import weg.ide.tools.java2smali.common.StyleSpan;
import weg.ide.tools.java2smali.ui.services.CodeService;
import weg.ide.tools.java2smali.views.editor.EditorModel;

public class HighlightModel extends EditorModel {

    private final Object myStylesLock = new Object();
    private final Object mySemanticStylesLock = new Object();
    private StyleSpan myStyles = new StyleSpan();
    private StyleSpan myStylesGUI = new StyleSpan();
    private StyleSpan mySemanticsStyles = new StyleSpan();
    private StyleSpan mySemanticsStylesGUI = new StyleSpan();

    private final Object myErrorsLock = new Object();
    private List<CodeService.Error> myErrors = new Vector<>();
    private List<CodeService.Error> myErrorsGUI = new Vector<>();

    public HighlightModel() {
        super();
    }

    public void syntaxErrors(List<CodeService.Error> errors) {
        myErrors.clear();
        myErrors.addAll(errors);
        synchronized (myErrorsLock) {
            List<CodeService.Error> cache = myErrorsGUI;
            myErrorsGUI = myErrors;
            myErrors = cache;
        }
    }

    public void highlighting(HighlightSpace space) {
        myStyles.set(space.myTypes, space.myStartLines, space.myStartColumns, space.myEndLines, space.myEndColumns, space.mySize);
        synchronized (myStylesLock) {
            StyleSpan styles = myStylesGUI;
            myStylesGUI = myStyles;
            myStyles = styles;
        }
        // myView.invalidate();
    }

    public void semanticHighlighting(HighlightSpace space) {
        mySemanticsStyles.set(space.myTypes, space.myStartLines, space.myStartColumns, space.myEndLines, space.myEndColumns, space.mySize);
        synchronized (mySemanticStylesLock) {
            StyleSpan styles = mySemanticsStylesGUI;
            mySemanticsStylesGUI = mySemanticsStyles;
            mySemanticsStyles = styles;
        }
        //myView.invalidate();
    }

    @Override
    protected void firePrepareRemove(int offset, int length, @NonNull String removed) {
        super.firePrepareRemove(offset, length, removed);
        int startLine = getLineAtOffset(offset);
        int startColumn = offset - getLineStart(startLine);
        int endLine = getLineAtOffset(offset + length);
        int endColumn = (offset + length) - getLineStart(endLine);
        endColumn--;
        synchronized (myStylesLock) {
            myStylesGUI.remove(startLine, startColumn, endLine, endColumn);
        }
        synchronized (mySemanticStylesLock) {
            mySemanticsStylesGUI.remove(startLine, startColumn, endLine, endColumn);
        }
    }

    @Override
    protected void fireInsertUpdate(int offset, int length, @NonNull String added) {
        int startLine = getLineAtOffset(offset);
        int startColumn = offset - getLineStart(startLine);
        int endLine = getLineAtOffset(offset + length);
        int endColumn = (offset + length) - getLineStart(endLine);
        endColumn--;
        synchronized (myStylesLock) {
            myStylesGUI.insert(startLine, startColumn, endLine, endColumn);
        }
        synchronized (mySemanticStylesLock) {
            mySemanticsStylesGUI.insert(startLine, startColumn, endLine, endColumn);
        }
        super.fireInsertUpdate(offset, length, added);
    }


    @Override
    public boolean hasStyles() {
        return true;
    }

    @Override
    public int getStyle(int line, int column) {
        
        int style = mySemanticsStylesGUI.getStyle(line, column);
        if (style == 0) {
            return myStylesGUI.getStyle(line, column);
        }
        return style;
    }

    @Override
    public boolean isWarning(int line, int column) {
        line++;
        column++;
        for (CodeService.Error error : myErrorsGUI) {
            if (error.level == CodeService.Error.WARNING) {
                if (error.startLine == error.endLine && error.startLine == line) {
                    return column >= error.startColumn && column <= error.endColumn;
                }
                if (error.startLine <= line && error.endLine >= line) {
                    if (line == error.startLine) {
                        return column >= error.startColumn;
                    }
                    if (line == error.endLine) {
                        return column <= error.endColumn;
                    }
                    return true;
                }
            }
        }
        return super.isWarning(line, column);
    }

    @Override
    public boolean isError(int line, int column) {
        line++;
        column++;
        for (CodeService.Error error : myErrorsGUI) {
            if (error.level == CodeService.Error.ERROR) {
                if (error.startLine == error.endLine && error.startLine == line) {
                    return column >= error.startColumn && column <= error.endColumn;
                }
                if (error.startLine <= line && error.endLine >= line) {
                    if (line == error.startLine) {
                        return column >= error.startColumn;
                    }
                    if (line == error.endLine) {
                        return column <= error.endColumn;
                    }
                    return true;
                }
            }
        }
        return super.isError(line, column);
    }

    @Override
    public boolean isDeprecated(int line, int column) {
        line++;
        column++;
        for (CodeService.Error error : myErrorsGUI) {
            if (error.level == CodeService.Error.DEPRECATED) {
                if (error.startLine == error.endLine && error.startLine == line) {
                    return column >= error.startColumn && column <= error.endColumn;
                }
                if (error.startLine <= line && error.endLine >= line) {
                    if (line == error.startLine) {
                        return column >= error.startColumn;
                    }
                    if (line == error.endLine) {
                        return column <= error.endColumn;
                    }
                    return true;
                }
            }
        }

        return super.isDeprecated(line, column);
    }
    
    public void clearErrors() {
        myErrors.clear();
        myErrorsGUI.clear();
    }
}
