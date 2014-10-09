/*
 * Copyright 2014 Matteo Bernacchia <kikijikispaccaspecchi@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kikijiki.isobako;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class OffsetPicker implements OnClickListener, OnDismissListener, SeekBar.OnSeekBarChangeListener {
    AlertDialog dialog;
    View picker;
    SeekBar seek;
    TextView progress;
    Button okButton;
    String up, down;
    offsetPickerListener listener;

    public OffsetPicker(Context c, int current, offsetPickerListener l) {
        listener = l;

        up = c.getResources().getString(R.string.verticalOffsetUp);
        down = c.getResources().getString(R.string.verticalOffsetDown);

        picker = LayoutInflater.from(c).inflate(R.layout.offset_picker, null);
        seek = (SeekBar) picker.findViewById(R.id.verticalOffsetSeek);
        progress = (TextView) picker.findViewById(R.id.verticalOffsetPickerProgress);
        okButton = (Button) picker.findViewById(R.id.verticalOffsetOkButton);

        progress.setText("0px");
        seek.setMax(999 * 2);
        seek.setProgress(current + 999);
        this.onProgressChanged(seek, current + 999, false);
        seek.setOnSeekBarChangeListener(this);

        Rect displayRectangle = new Rect();
        Window window = ((Activity) c).getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);
        picker.setMinimumWidth((int) (displayRectangle.width() * 0.9f));
        //picker.setMinimumHeight((int)(displayRectangle.height() * 0.9f));

        okButton.setOnClickListener(this);

        dialog = new AlertDialog.Builder(c).create();
        dialog.setView(picker);
        dialog.setOnDismissListener(this);
    }

    public void show() {
        dialog.show();
    }

    @Override
    public void onClick(View arg0) {
        if (arg0.equals(okButton)) {
            dialog.dismiss();
        }
    }

    @Override
    public void onDismiss(DialogInterface arg0) {
        listener.onOffsetPick(seek.getProgress() - 999);
    }

    @Override
    public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
        int value = arg1 - 999;

        String msg;

        if (value == 0) {
            msg = String.valueOf(Math.abs(value)) + "px";
        } else {
            msg = String.valueOf(Math.abs(value)) + "px " + (value < 0 ? down : up);
        }

        progress.setText(msg);
    }

    @Override
    public void onStartTrackingTouch(SeekBar arg0) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar arg0) {
    }

    public interface offsetPickerListener {
        void onOffsetPick(int offset);
    }
}
