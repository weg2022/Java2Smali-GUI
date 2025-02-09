package weg.ide.tools.java2smali.common;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static android.text.InputType.TYPE_CLASS_NUMBER;
import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.inputmethod.EditorInfo.IME_FLAG_NO_ENTER_ACTION;
import static android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI;
import static java.lang.Integer.parseInt;
import static java.util.List.of;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.text.Editable;
import android.text.InputType;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.List;


public abstract class MessageBox {
    private static Dialog sDialog;

    @NonNull
    protected abstract Dialog buildDialog(@NonNull Activity activity);

    public static boolean show(@NonNull Activity activity, @NonNull MessageBox messageBox) {
        dismiss();
        if (sDialog == null || !sDialog.isShowing()) {
            sDialog = messageBox.buildDialog(activity);
            sDialog.show();
            return true;
        }
        return false;
    }


    public static void show() {
        if (sDialog != null && !sDialog.isShowing()) {
            sDialog.show();
        }
    }

    public static void hide() {
        if (sDialog != null) {
            sDialog.hide();
        }
    }

    public static boolean isHidden() {
        return sDialog != null && !sDialog.isShowing();
    }

    public static void dismiss() {
        if (sDialog != null) {
            sDialog.dismiss();
            sDialog = null;
        }
    }

    public static boolean isDismissed() {
        return sDialog == null;
    }

    public static boolean isShowing() {
        return sDialog != null && sDialog.isShowing();
    }

    public static boolean showMessage(@NonNull Activity activity, @Nullable String title,
                                      @NonNull String message) {
        return showMessage(activity, title, message, null);
    }

    public static boolean showMessage(@NonNull Activity activity,
                                      @Nullable String title,
                                      @NonNull String message,
                                      @Nullable Runnable okRun) {
        return showMessage(activity, title, message, okRun, null);
    }

    public static boolean showMessage(@NonNull Activity activity,
                                      @Nullable String title,
                                      @NonNull String message,
                                      @Nullable Runnable okRun,
                                      @Nullable Runnable cancelRun) {
        return showMessage(activity, title, message, true, okRun, cancelRun);
    }

    public static boolean showMessage(@NonNull Activity activity,
                                      @Nullable String title,
                                      @NonNull String message,
                                      boolean cancelable,
                                      @Nullable Runnable okRun,
                                      @Nullable Runnable cancelRun) {
        return showMessage(activity, title, message, cancelable,
                activity.getString(android.R.string.ok), okRun,
                null, cancelRun);
    }

    public static boolean showMessage(@NonNull Activity activity,
                                      @Nullable String title,
                                      @NonNull String message,
                                      boolean cancelable,
                                      @NonNull String ok,
                                      @Nullable Runnable okRun,
                                      @Nullable String cancel,
                                      @Nullable Runnable cancelRun) {
        return show(activity, new MessageBox() {
            @NonNull
            @Override
            public Dialog buildDialog(@NonNull Activity context) {
                return new AlertDialog.Builder(context)
                        .setTitle(title)
                        .setMessage(message)
                        .setCancelable(cancelable)
                        .setPositiveButton(ok, (dialog, which) -> {
                            dialog.dismiss();
                            if (okRun != null)
                                okRun.run();
                        })
                        .setNegativeButton(cancel, (dialog, which) -> {
                            dialog.dismiss();
                            if (cancelRun != null)
                                cancelRun.run();
                        })
                        .setOnCancelListener(dialog -> {
                            dialog.dismiss();
                            if (cancelRun != null)
                                cancelRun.run();
                        })
                        .create();
            }
        });
    }

    public static boolean queryYesNo(@NonNull Activity activity,
                                     @StringRes int title,
                                     @StringRes int message,
                                     @NonNull List<String> list,
                                     @Nullable Runnable yesRun,
                                     @Nullable Runnable noRun) {
        StringBuilder listBuilder = new StringBuilder("\n");
        for (String line : list) {
            listBuilder.append("\n").append(line);
        }
        return queryYesNo(activity, activity.getString(title),
                activity.getString(message) + listBuilder,
                activity.getString(android.R.string.no), noRun,
                activity.getString(android.R.string.yes), yesRun);
    }

    public static boolean queryYesNo(@NonNull Activity activity,
                                     @StringRes int title,
                                     @StringRes int message,
                                     @Nullable Runnable yesRun,
                                     @Nullable Runnable noRun) {
        return queryYesNo(activity, activity.getString(title),
                activity.getString(message),
                activity.getString(android.R.string.yes), yesRun,
                activity.getString(android.R.string.no), noRun);
    }

    public static boolean queryYesNo(@NonNull Activity activity,
                                     @Nullable String title,
                                     @NonNull String message,
                                     @Nullable Runnable yesRun,
                                     @Nullable Runnable noRun) {
        return queryYesNo(activity, title, message,
                activity.getString(android.R.string.yes), yesRun,
                activity.getString(android.R.string.no), noRun);
    }

    public static boolean queryYesNo(@NonNull Activity activity,
                                     @Nullable String title,
                                     @NonNull String message,
                                     @NonNull String yes,
                                     @Nullable Runnable yesRun,
                                     @NonNull String no,
                                     @Nullable Runnable noRun) {
        return queryYesNo(activity, title,
                message, false, yes, yesRun, no, noRun);
    }

    public static boolean queryYesNo(@NonNull Activity activity,
                                     @Nullable String title,
                                     @NonNull String message,
                                     boolean cancelable,
                                     @NonNull String yes,
                                     @Nullable Runnable yesRun,
                                     @NonNull String no,
                                     @Nullable Runnable noRun) {
        return show(activity, new MessageBox() {
            @NonNull
            @Override
            public Dialog buildDialog(@NonNull Activity context) {
                return new AlertDialog.Builder(context)
                        .setTitle(title)
                        .setMessage(message)
                        .setCancelable(cancelable)
                        .setPositiveButton(yes, (dialog, which) -> {
                            dialog.dismiss();
                            if (yesRun != null)
                                yesRun.run();
                        })
                        .setNegativeButton(no, (dialog, which) -> {
                            dialog.dismiss();
                            if (noRun != null)
                                noRun.run();
                        })
                        .create();
            }
        });
    }


    public static boolean queryFromList(@NonNull Activity activity,
                                        @Nullable String title,
                                        @NonNull List<String> values,
                                        @NonNull ValueRunnable<String> ok) {
        return queryFromList(activity, title, values, ok, null);
    }

    public static boolean queryFromList(@NonNull Activity activity,
                                        @Nullable String title,
                                        @NonNull List<String> values,
                                        @NonNull ValueRunnable<String> okRun,
                                        @Nullable Runnable cancelRun) {
        return queryFromList(activity, title, values, false, okRun, cancelRun);
    }

    public static boolean queryFromList(@NonNull Activity activity,
                                        @Nullable String title,
                                        @NonNull List<String> values,
                                        boolean cancelable,
                                        @NonNull ValueRunnable<String> okRun,
                                        @Nullable Runnable cancelRun) {
        return show(activity, new MessageBox() {
            @NonNull
            @Override
            public Dialog buildDialog(@NonNull Activity context) {
                return new AlertDialog.Builder(context)
                        .setTitle(title)
                        .setCancelable(cancelable)
                        .setItems(values.toArray(new CharSequence[0]),
                                (dialog, which) -> {
                                    dialog.dismiss();
                                    if (which>=0 &&which < values.size())
                                        okRun.run(values.get(which));
                                })
                        .setPositiveButton(android.R.string.ok,
                                (dialog, which) -> {
                                    dialog.dismiss();
                                    if (which>=0 &&which < values.size())
                                        okRun.run(values.get(which));
                                })
                        .setNegativeButton(cancelRun == null ? null :
                                        context.getString(android.R.string.cancel),
                                (dialog, which) -> {
                                    dialog.dismiss();
                                    if (cancelRun != null)
                                        cancelRun.run();
                                })
                        .setOnCancelListener(dialog -> {
                            dialog.dismiss();
                            if (cancelRun != null)
                                cancelRun.run();
                        })
                        .create();
            }
        });
    }

    public static boolean querySingleChoiceFromList(@NonNull Activity activity,
                                                    @Nullable String title,
                                                    @NonNull List<String> values,
                                                    @NonNull String selectedValue,
                                                    @NonNull ValueRunnable<String> okRun) {
        return querySingleChoiceFromList(activity, title, values,
                selectedValue, false, okRun, null);
    }

    public static boolean querySingleChoiceFromList(@NonNull Activity activity,
                                                    @Nullable String title,
                                                    @NonNull List<String> values,
                                                    @NonNull String selectedValue,
                                                    boolean cancelable,
                                                    @NonNull ValueRunnable<String> okRun,
                                                    @Nullable Runnable cancelRun) {
        return show(activity, new MessageBox() {
            @NonNull
            @Override
            public Dialog buildDialog(@NonNull Activity context) {
                int checkedItem = values.indexOf(selectedValue);
                return new AlertDialog.Builder(context)
                        .setTitle(title)
                        .setCancelable(cancelable)
                        .setSingleChoiceItems(values.toArray(new CharSequence[0]),
                                checkedItem,
                                (dialog, which) -> {

                                })
                        .setPositiveButton(android.R.string.ok,
                                (dialog, which) -> {
                                    dialog.dismiss();
                                    SparseBooleanArray items = ((AlertDialog) dialog)
                                            .getListView()
                                            .getCheckedItemPositions();

                                    if (items != null) {
                                        for (int i = 0; i < values.size(); i++) {
                                            if (items.get(i))
                                                okRun.run(values.get(i));
                                        }
                                    }
                                })
                        .setNegativeButton(cancelRun == null ? null :
                                        context.getString(android.R.string.cancel),
                                (dialog, which) -> {
                                    dialog.dismiss();
                                    if (cancelRun != null)
                                        cancelRun.run();
                                })
                        .setOnCancelListener(dialog -> {
                            dialog.dismiss();
                            if (cancelRun != null)
                                cancelRun.run();
                        })
                        .create();
            }
        });
    }

    public static boolean queryMultipleChoiceFromList(@NonNull Activity activity,
                                                      @Nullable String title,
                                                      @NonNull List<String> values,
                                                      @NonNull List<Boolean> selectedValues,
                                                      @NonNull ValueRunnable<List<Integer>> okRun) {
        return queryMultipleChoiceFromList(activity,
                title,
                values,
                selectedValues,
                false,
                okRun, null);
    }

    public static boolean queryMultipleChoiceFromList(@NonNull Activity activity,
                                                      @Nullable String title,
                                                      @NonNull List<String> values,
                                                      @NonNull List<Boolean> selectedValues,
                                                      boolean cancelable,
                                                      @NonNull ValueRunnable<List<Integer>> okRun,
                                                      @Nullable Runnable cancelRun) {
        return queryMultipleChoiceFromList(activity,
                title,
                values,
                selectedValues,
                cancelable,
                null,
                okRun, null, cancelRun);
    }

    public static boolean queryMultipleChoiceFromList(@NonNull Activity activity,
                                                      @Nullable String title,
                                                      @NonNull List<String> values,
                                                      @NonNull List<Boolean> selectedValues,
                                                      boolean cancelable,
                                                      @Nullable String ok,
                                                      @NonNull ValueRunnable<List<Integer>> okRun,
                                                      @Nullable String cancel,
                                                      @Nullable Runnable cancelRun) {
        return show(activity, new MessageBox() {
            @NonNull
            @Override
            public Dialog buildDialog(@NonNull Activity context) {
                boolean[] checkedItems = new boolean[selectedValues.size()];
                for (int i = 0; i < checkedItems.length; i++) {
                    checkedItems[i] = selectedValues.get(i);
                }
                return new AlertDialog.Builder(context)
                        .setTitle(title)
                        .setCancelable(cancelable)
                        .setMultiChoiceItems(values.toArray(new CharSequence[0]),
                                checkedItems,
                                (dialog, which, isChecked) -> {

                                })
                        .setPositiveButton(ok == null ?
                                context.getString(android.R.string.ok)
                                : ok, (dialog, which) -> {

                            dialog.dismiss();
                            ArrayList<Integer> result = new ArrayList<>();
                            SparseBooleanArray items = ((AlertDialog) dialog)
                                    .getListView()
                                    .getCheckedItemPositions();
                            if (items != null) {
                                for (int i = 0; i < values.size(); i++) {
                                    if (items.get(i))
                                        result.add(i);
                                }
                            }
                            okRun.run(result);
                        })
                        .setNegativeButton(cancelRun == null ? null : cancel == null ?
                                        context.getString(android.R.string.cancel) :
                                        cancel,

                                (dialog, which) -> {
                                    dialog.dismiss();
                                    if (cancelRun != null)
                                        cancelRun.run();
                                })
                        .setOnCancelListener(dialog -> {
                            dialog.dismiss();
                            if (cancelRun != null)
                                cancelRun.run();
                        })
                        .create();
            }
        });
    }

    public static boolean queryIndexFromList(@NonNull Activity activity,
                                             @Nullable String title,
                                             @NonNull List<String> values,
                                             @NonNull ValueRunnable<Integer> okRun) {
        return queryIndexFromList(activity, title, values,
                activity.getString(android.R.string.ok), okRun,
                null, null);
    }

    public static boolean queryIndexFromList(@NonNull Activity activity,
                                             @Nullable String title,
                                             @NonNull List<String> values,
                                             @NonNull ValueRunnable<Integer> okRun,
                                             @Nullable Runnable cancelRun) {
        return queryIndexFromList(activity, title, values,
                activity.getString(android.R.string.ok), okRun,
                activity.getString(android.R.string.cancel), cancelRun);
    }

    public static boolean queryIndexFromList(@NonNull Activity activity,
                                             @Nullable String title,
                                             @NonNull List<String> values,
                                             @NonNull String ok,
                                             @NonNull ValueRunnable<Integer> okRun,
                                             @Nullable String cancel,
                                             @Nullable Runnable cancelRun) {
        return queryIndexFromList(activity, title, values, false, ok, okRun,
                cancel, cancelRun);
    }

    public static boolean queryIndexFromList(@NonNull Activity activity,
                                             @Nullable String title,
                                             @NonNull List<String> values,
                                             boolean cancelable,
                                             @NonNull String ok,
                                             @NonNull ValueRunnable<Integer> okRun,
                                             @Nullable String cancel,
                                             @Nullable Runnable cancelRun) {
        return show(activity, new MessageBox() {
            @NonNull
            @Override
            public Dialog buildDialog(@NonNull Activity context) {
                return new AlertDialog.Builder(context)
                        .setTitle(title)
                        .setCancelable(cancelable)
                        .setItems(values.toArray(new CharSequence[0]),
                                (dialog, which) -> {
                                    dialog.dismiss();
                                    okRun.run(which);
                                })
                        .setPositiveButton(ok, (dialog, which) -> {
                            dialog.dismiss();
                            okRun.run(which);
                        })
                        .setNegativeButton(cancel, (dialog, which) -> {
                            dialog.dismiss();
                            if (cancelRun != null)
                                cancelRun.run();
                        })
                        .setOnCancelListener(dialog -> {
                            dialog.dismiss();
                            if (cancelRun != null)
                                cancelRun.run();
                        })
                        .create();
            }
        });
    }

    public static boolean queryInt(@NonNull Activity activity,
                                   @Nullable String title,
                                   @NonNull String message,
                                   int value,
                                   @NonNull ValueRunnable<Integer> okRun) {
        return queryInt(activity, title, message,
                value, false,
                okRun, null);
    }

    public static boolean queryInt(@NonNull Activity activity,
                                   @Nullable String title,
                                   @NonNull String message,
                                   int value,
                                   boolean cancelable,
                                   @NonNull ValueRunnable<Integer> okRun,
                                   @Nullable Runnable cancelRun) {
        return show(activity, new MessageBox() {
            @NonNull
            @SuppressLint("SetTextI18n")
            @Override
            public Dialog buildDialog(@NonNull final Activity context) {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);

                AlertDialog[] dialogs = new AlertDialog[1];
                EditText input = new EditText(context) {
                    @Override
                    public boolean onKeyDown(int keyCode, KeyEvent event) {
                        if (keyCode == KEYCODE_ENTER) return true;
                        return super.onKeyDown(keyCode, event);
                    }

                    @Override
                    public boolean onKeyUp(int keyCode, KeyEvent event) {
                        if (keyCode == KEYCODE_ENTER) {
                            imm.hideSoftInputFromWindow(getWindowToken(), 0);
                            Editable text = getText();
                            if (text != null) {
                                try {
                                    okRun.run(parseInt(text.toString().trim()));
                                } catch (NumberFormatException ignored) {
                                    okRun.run(-1);
                                }
                            }
                            dialogs[0].dismiss();
                            return true;
                        }
                        return super.onKeyUp(keyCode, event);
                    }
                };
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setView(input)
                        .setTitle(title)
                        .setMessage(message)
                        .setCancelable(cancelable)
                        .setPositiveButton(
                                context.getString(android.R.string.ok),
                                (dialog, id) -> {
                                    imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                                    Editable text = input.getText();
                                    if (text != null) {
                                        try {
                                            okRun.run(parseInt(text.toString().trim()));
                                        } catch (NumberFormatException ignored) {
                                            okRun.run(-1);
                                        }
                                    }
                                })
                        .setNegativeButton(
                                context.getString(android.R.string.cancel),
                                (dialog, id) -> {
                                    imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                                    dialog.dismiss();
                                    if (cancelRun != null)
                                        cancelRun.run();
                                })
                        .setOnCancelListener(dialog -> {
                            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                            dialog.dismiss();
                            if (cancelRun != null)
                                cancelRun.run();
                        });

                dialogs[0] = builder.create();
                dialogs[0].setOnShowListener(dialog -> {
                    imm.showSoftInput(input, 1);
                    input.selectAll();
                });
                input.setText(String.valueOf(value));
                input.setImeOptions(IME_FLAG_NO_EXTRACT_UI | IME_FLAG_NO_ENTER_ACTION);
                input.setInputType(TYPE_CLASS_NUMBER);
                input.setOnEditorActionListener((v, actionId, event) -> {
                    if (actionId == 6) {
                        imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                        dialogs[0].dismiss();
                        Editable text = input.getText();
                        if (text != null) {
                            try {
                                okRun.run(parseInt(text.toString().trim()));
                            } catch (NumberFormatException ignored) {
                                okRun.run(-1);
                            }
                        }
                        return true;
                    }
                    return false;
                });
                return dialogs[0];
            }
        });
    }


    public static boolean queryText(@NonNull Activity activity,
                                    @Nullable String title,
                                    @NonNull String message,
                                    @NonNull String value,
                                    @NonNull ValueRunnable<String> okRun) {
        return queryText(activity, title, message, value, okRun, null);
    }

    public static boolean queryText(@NonNull Activity activity,
                                    @Nullable String title,
                                    @NonNull String message,
                                    @NonNull String value,
                                    @NonNull ValueRunnable<String> okRun,
                                    @Nullable Runnable cancelRun) {
        return queryText(activity, title, message, value, true, okRun, cancelRun);
    }

    public static boolean queryText(@NonNull Activity activity,
                                    @Nullable String title,
                                    @NonNull String message,
                                    @NonNull String value,
                                    boolean cancelable,
                                    @NonNull ValueRunnable<String> okRun,
                                    @Nullable Runnable cancelRun) {
        return show(activity, new MessageBox() {
            @NonNull
            @Override
            public Dialog buildDialog(@NonNull final Activity context) {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);

                AlertDialog[] dialogs = new AlertDialog[1];
                EditText input = new EditText(context) {
                    @Override
                    public boolean onKeyDown(int keyCode, KeyEvent event) {
                        if (keyCode == KEYCODE_ENTER) return true;
                        return super.onKeyDown(keyCode, event);
                    }

                    @Override
                    public boolean onKeyUp(int keyCode, KeyEvent event) {
                        if (keyCode == KEYCODE_ENTER) {
                            imm.hideSoftInputFromWindow(getWindowToken(), 0);
                            dialogs[0].dismiss();
                            Editable text = getText();
                            okRun.run(text == null ? "" : text.toString().trim());
                            return true;
                        }
                        return super.onKeyUp(keyCode, event);
                    }
                };
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setView(input)
                        .setTitle(title)
                        .setMessage(message)
                        .setCancelable(cancelable)
                        .setPositiveButton(context.getString(android.R.string.ok),
                                (dialog, id) -> {
                                    imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                                    dialog.dismiss();
                                    Editable text = input.getText();
                                    okRun.run(text == null ? null : text.toString().trim());
                                })
                        .setNegativeButton(context.getString(android.R.string.cancel),
                                (dialog, id) -> {
                                    imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                                    dialog.dismiss();
                                    if (cancelRun != null)
                                        cancelRun.run();
                                })
                        .setOnCancelListener(dialog -> {
                            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                            dialog.dismiss();
                            if (cancelRun != null)
                                cancelRun.run();
                        });

                dialogs[0] = builder.create();
                dialogs[0].setOnShowListener(dialog -> {
                    imm.showSoftInput(input, 1);
                    input.selectAll();
                });
                input.setImeOptions(IME_FLAG_NO_EXTRACT_UI | IME_FLAG_NO_ENTER_ACTION);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setText(value);
                input.setOnEditorActionListener((v, actionId, event) -> {
                    if (actionId == 6) {
                        imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                        dialogs[0].dismiss();
                        Editable text = input.getText();
                        okRun.run(text == null ? null : text.toString().trim());
                        return true;
                    }
                    return false;
                });
                return dialogs[0];
            }
        });
    }

    public static boolean queryMultipleValues(@NonNull Activity activity,
                                              @Nullable String title,
                                              @NonNull List<String> values,
                                              @NonNull List<String> displayValues,
                                              @Nullable String value,
                                              @NonNull ValueRunnable<String> okRun) {
        return queryMultipleValues(activity, title, values, displayValues,
                value, false, okRun, null);
    }

    public static boolean queryMultipleValues(@NonNull Activity activity,
                                              @Nullable String title,
                                              @NonNull List<String> values,
                                              @NonNull List<String> displayValues,
                                              @Nullable String value,
                                              boolean cancelable,
                                              @NonNull ValueRunnable<String> okRun,
                                              @Nullable Runnable cancelRun) {
        List<Boolean> isSelected = new ArrayList<>();
        List<String> selectedValues = value == null ?
                of() : of(value.split("\\|"));

        for (String v : values) {
            isSelected.add(selectedValues.contains(v));
        }

        return queryMultipleChoiceFromList(activity,
                title,
                displayValues,
                isSelected,
                cancelable,
                t -> {
                    if (t.isEmpty()) {
                        okRun.run("");
                        return;
                    }
                    StringBuilder newValue = new StringBuilder();
                    for (Integer num : t) {
                        int i = num;
                        if (newValue.length() > 0)
                            newValue.append("|");

                        newValue.append(values.get(i));
                    }
                    okRun.run(newValue.toString());
                }, cancelRun);
    }
}
