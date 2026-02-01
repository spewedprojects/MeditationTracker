package com.gratus.meditationtrakcer.utils;

import android.view.View;
import android.widget.EditText;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ClearFocusUtils {

    public static void clearFocusOnKeyboardHide(EditText editText, View rootView) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            if (!imeVisible) {
                editText.clearFocus();
            }
            return insets;
        });
    }
}