package weg.ide.tools.java2smali.ui;

import static weg.ide.tools.java2smali.ui.App.init;
import static weg.ide.tools.java2smali.ui.App.postExec;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import weg.ide.tools.java2smali.common.IoUtils;
import weg.ide.tools.java2smali.common.KeyStrokeDetector;
import weg.ide.tools.java2smali.common.LineEndingNormalizedReader;
import weg.ide.tools.java2smali.common.MessageBox;
import weg.ide.tools.java2smali.ui.services.CodeService;
import weg.ide.tools.java2smali.views.EditorView;
import weg.ide.tools.java2smali.views.editor.CaretListener;
import weg.ide.tools.java2smali.views.editor.Editor;
import weg.ide.tools.java2smali.views.editor.SelectionListener;

public class MainUI extends Activity implements TextModel.TextModelListener,
        CaretListener,
        SelectionListener,
        CodeService.ErrorListener {


    private KeyStrokeDetector myKeyStrokeDetector;
    private EditorView myEditorView;
    private HighlightModel myModel;
    private final StringBuffer myBuffer = new StringBuffer();
    private final Map<String, StringBuffer> myCachedBuffers = new HashMap<>();

    private QuickKeyBar myQuickKeyBar;
    private boolean myJavaMode = true;
    private long myLastPressed = -1;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init(this);
        myKeyStrokeDetector = new KeyStrokeDetector(this);
        setContentView(R.layout.main);
        myEditorView = findViewById(R.id.editorView);
        myQuickKeyBar = new QuickKeyBar(this);

        myQuickKeyBar.setKeys(myEditorView.getQuickKeys());
        myQuickKeyBar.showQuickKeyBar(true);
        myEditorView.setKeyStrokeDetector(myKeyStrokeDetector);
        myEditorView.setCaretVisible(true);
        myEditorView.setInsertTabsAsSpaces(false);
        myEditorView.setIndentationEnabled(true);
       //myEditorView.setWhitespaceVisible(true);
        myModel = new HighlightModel();
        App.getCodeService().setErrorListener(this);
        postExec(() -> {
            myModel.setUndoEnabled(false);
            try {
                String buffer = IoUtils.readString(new LineEndingNormalizedReader(new InputStreamReader(getAssets().open("Main.java"))), true);
                myModel.insert(0, buffer);
            } catch (IOException e) {
                myModel.insert(0, Objects.requireNonNull(e.getLocalizedMessage()));
            }
            myModel.setUndoEnabled(true);

            runOnUiThread(() -> {
                myEditorView.setModel(myModel);
                myEditorView.addModelListener(MainUI.this);
                myEditorView.addCaretListener(MainUI.this);
                myEditorView.addSelectionListener(MainUI.this);

                App.getCodeService().highlightJava(myModel);
            });
        });
        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(false);
    }

    @Override
    public void onBackPressed() {
        if (myJavaMode) {
            if (System.currentTimeMillis() - myLastPressed <= 2000) {
                super.onBackPressed();
                return;
            }
            myLastPressed = System.currentTimeMillis();
            return;
        } else {
            runOnUiThread(() -> {
                myJavaMode = true;
                myModel.clearUndoHistory();
                myModel.setUndoEnabled(false);
                myModel.remove(0, myModel.getCharCount());
                myModel.insert(0, myBuffer.toString());
                myModel.setUndoEnabled(true);
                myEditorView.setEditable(true);
                myEditorView.moveCaret(0);
                myEditorView.showIme();
                myQuickKeyBar.showQuickKeyBar(true);
                if (getActionBar() != null) {
                    getActionBar().setDisplayHomeAsUpEnabled(false);
                    getActionBar().setTitle(R.string.app_name);
                }
                invalidateOptionsMenu();
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        App.shutdown();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (myEditorView.isSelectionMode()) {
            getMenuInflater().inflate(R.menu.editor_menu, menu);
            return true;
        }
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        var files = menu.findItem(R.id.commandFileList);
        if (files != null) {
            files.setVisible(!myJavaMode);
        }
        var paste = menu.findItem(R.id.commandPaste);
        if (paste != null) {
            paste.setVisible(myJavaMode);
        }
        var undo = menu.findItem(R.id.commandUndo);
        var cut = menu.findItem(R.id.commandCut);
        if (cut != null)
            cut.setVisible(myJavaMode);
        var redo = menu.findItem(R.id.commandRedo);
        if (undo != null) {
            undo.setVisible(myJavaMode);
        }
        if (redo != null) {
            redo.setVisible(myJavaMode);
        }
        var run = menu.findItem(R.id.commandRun);
        if (run != null) {
            run.setVisible(myJavaMode);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.commandFileList) {
            var list = new ArrayList<>(myCachedBuffers.keySet());
            runOnUiThread(() -> {
                MessageBox.queryFromList(MainUI.this, "Disassemble List", list, value -> {
                    myModel.clearUndoHistory();
                    myModel.clearErrors();
                    myModel.setUndoEnabled(false);
                    myJavaMode = false;
                    myEditorView.setEditable(false);
                    myModel.remove(0, myModel.getCharCount());
                    myModel.insert(0, myCachedBuffers.get(value).toString());
                    myModel.setUndoEnabled(true);
                    myEditorView.moveCaret(0);
                    myEditorView.hideIme();
                    myQuickKeyBar.showQuickKeyBar(false);
                    if (getActionBar() != null) {
                        getActionBar().setDisplayHomeAsUpEnabled(true);
                        getActionBar().setTitle(value);
                    }
                    invalidateOptionsMenu();
                });
            });
            return true;
        }
        if (item.getItemId() == android.R.id.home) {

            runOnUiThread(() -> {
                myJavaMode = true;
                myModel.clearErrors();
                myModel.clearUndoHistory();
                myModel.setUndoEnabled(false);
                myModel.remove(0, myModel.getCharCount());
                myModel.insert(0, myBuffer.toString());
                myModel.setUndoEnabled(true);
                myEditorView.setEditable(true);
                myEditorView.moveCaret(0);
                myEditorView.showIme();
                myQuickKeyBar.showQuickKeyBar(true);
                if (getActionBar() != null) {
                    getActionBar().setDisplayHomeAsUpEnabled(false);
                    getActionBar().setTitle(R.string.app_name);
                }
                invalidateOptionsMenu();
            });


            return true;
        }
        if (item.getItemId() == R.id.commandCopy) {
            if (myEditorView.canCopy())
                myEditorView.copy();
            return true;
        } else if (item.getItemId() == R.id.commandCut) {
            if (myEditorView.canCut())
                myEditorView.cut();
            return true;
        } else if (item.getItemId() == R.id.commandPaste) {
            if (myEditorView.canPaste())
                myEditorView.paste();
            return true;
        } else if (item.getItemId() == R.id.commandSelectAll) {
            if (myEditorView.canSelectAll())
                myEditorView.selectAll();
            return true;
        } else if (item.getItemId() == R.id.commandUnSelect) {
            if (myEditorView.canUnselectAll())
                myEditorView.unselectAll();
            return true;
        } else if (item.getItemId() == R.id.commandUndo) {
            if (myEditorView.canUndo())
                myEditorView.undo();
            return true;
        } else if (item.getItemId() == R.id.commandRedo) {
            if (myEditorView.canRedo())
                myEditorView.redo();
            return true;
        } else if (item.getItemId() == R.id.commandRun) {
            App.getCodeService().compile(myModel, new CodeService.CompilerCallback() {
                @Override
                public void compileSuccessful(List<String> filePaths) {
                    App.postExec(() -> {
                        myCachedBuffers.clear();
                        try {
                            for (String filePath : filePaths) {
                                File file = new File(filePath);
                                String text = IoUtils.readString(new LineEndingNormalizedReader(new InputStreamReader(new FileInputStream(file))), true);
                                StringBuffer buffer = new StringBuffer(text);
                                myCachedBuffers.put(file.getName(), buffer);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (filePaths.size() == 1) {

                            try {
                                String buffer = IoUtils.readString(new LineEndingNormalizedReader(new InputStreamReader(new FileInputStream(filePaths.get(0)))), true);
                                runOnUiThread(() -> {
                                    myBuffer.setLength(0);
                                    myBuffer.append(myModel.getText());
                                    myModel.clearErrors();
                                    myModel.clearUndoHistory();
                                    myModel.setUndoEnabled(false);
                                    myJavaMode = false;
                                    myEditorView.setEditable(false);
                                    myModel.remove(0, myModel.getCharCount());
                                    myModel.insert(0, buffer);
                                    myModel.setUndoEnabled(true);
                                    myEditorView.moveCaret(0);
                                    myEditorView.hideIme();
                                    myQuickKeyBar.showQuickKeyBar(false);
                                    if (getActionBar() != null) {
                                        getActionBar().setDisplayHomeAsUpEnabled(true);
                                        if (getActionBar() != null) {
                                            getActionBar().setDisplayHomeAsUpEnabled(true);
                                            getActionBar().setTitle(new File(filePaths.get(0)).getName());
                                        }
                                    }
                                    invalidateOptionsMenu();
                                });
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            var list = new ArrayList<>(myCachedBuffers.keySet());
                            runOnUiThread(() -> {
                                MessageBox.queryFromList(MainUI.this, "Open Disassemble File", list, value -> {
                                    myBuffer.setLength(0);
                                    myBuffer.append(myModel.getText());
                                    myModel.clearErrors();
                                    myModel.clearUndoHistory();
                                    myModel.setUndoEnabled(false);
                                    myJavaMode = false;
                                    myEditorView.setEditable(false);
                                    myModel.remove(0, myModel.getCharCount());
                                    myModel.insert(0, myCachedBuffers.get(value).toString());
                                    myModel.setUndoEnabled(true);
                                    myEditorView.moveCaret(0);
                                    myEditorView.hideIme();
                                    myQuickKeyBar.showQuickKeyBar(false);
                                    if (getActionBar() != null) {
                                        getActionBar().setDisplayHomeAsUpEnabled(true);
                                        getActionBar().setTitle(value);
                                    }
                                    invalidateOptionsMenu();
                                });
                            });
                        }
                    });
                }

                @Override
                public void compileFailure(String msg) {
                    runOnUiThread(() -> {
                        MessageBox.showMessage(MainUI.this, "Compile Error", msg);
                    });
                }
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        myKeyStrokeDetector.configChange(this);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        myKeyStrokeDetector.activityKeyUp(keyCode, event);
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        myKeyStrokeDetector.activityKeyDown(keyCode, event);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void insertUpdate(@NonNull TextModel textModel, int offset, int length, @NonNull String added) {
        
        if (myJavaMode) {
            App.getCodeService().highlightJava(myModel);
        } else {
            App.getCodeService().highlightSmali(myModel);
        }
        invalidateOptionsMenu();
    }

    @Override
    public void removeUpdate(@NonNull TextModel textModel, int offset, int length, @NonNull String removed) {
        if (myJavaMode) {
            App.getCodeService().highlightJava(myModel);
        } else {
            App.getCodeService().highlightSmali(myModel);
        }
        invalidateOptionsMenu();
    }

    @Override
    public void caretUpdate(@NonNull Editor view, int caretOffset, boolean typing) {
        invalidateOptionsMenu();
    }

    @Override
    public void selectionUpdate(@NonNull Editor view, boolean selectMode, int selectStart, int selectEnd) {
        if (getActionBar() != null)
            getActionBar().setDisplayShowTitleEnabled(!selectMode);

        invalidateOptionsMenu();
    }

    @Override
    public void syntaxErrors(List<CodeService.Error> errors) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                myModel.syntaxErrors(errors);
            }
        });

    }
    
    public QuickKeyBar getQuickKeyBar() {
        return myQuickKeyBar;
    }
}
