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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

public class WallPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    public static boolean changed = false;
    String surfacePath;
    int blockColor = -1;
    int backgroundColorValue = -1;
    int defaultIcon = 0;
    int iconNumber = 0;
    int verticalOffsetValue = 0;
    int rainAmountValue = 5;
    int fallSpeedValue = 20;

    ListPreference blockSize;
    Preference fallSpeed;
    ListPreference fillMode;
    ListPreference fallStyle;
    CheckBoxPreference blockLimit;
    ListPreference paintMode;
    Preference colorSelector;
    Preference openGallery;
    ListPreference fps;
    Preference verticalOffset;
    Preference rainAmount;
    Preference backgroundColor;
    EditTextPreference waitTime;
    Preference showLicense;
    AlertDialog.Builder builder;
    private int SELECT_PICTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.wall_preferences);

        blockSize = (ListPreference) findPreference("blockSize");
        blockLimit = (CheckBoxPreference) findPreference("blockLimit");
        fallSpeed = (Preference) findPreference("fallSpeed");
        fillMode = (ListPreference) findPreference("fillMode");
        fallStyle = (ListPreference) findPreference("fallStyle");
        paintMode = (ListPreference) findPreference("paintMode");
        colorSelector = (Preference) findPreference("colorSelector");
        openGallery = (Preference) findPreference("openGallery");
        fps = (ListPreference) findPreference("fps");
        verticalOffset = (Preference) findPreference("verticalOffset");
        rainAmount = (Preference) findPreference("rainAmount");
        waitTime = (EditTextPreference) findPreference("waitTime");
        backgroundColor = (Preference) findPreference("backgroundColor");
        showLicense = (Preference) findPreference("showLicence");

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);

        blockColor = p.getInt("blockColor", -1);
        backgroundColorValue = p.getInt("backgroundColorValue", Color.BLACK);
        defaultIcon = p.getInt("defaultIcon", this.getResources().getIdentifier("default_icon_21", "raw", this.getPackageName()));
        iconNumber = p.getInt("iconNumber", 3);
        verticalOffsetValue = p.getInt("verticalOffsetValue", 0);
        rainAmountValue = p.getInt("rainAmountValue", 24);
        fallSpeedValue = p.getInt("fallSpeedValue", 20);

        surfacePath = p.getString("surfacePath", "");

        builder = new AlertDialog.Builder(WallPreferences.this);

        showLicense.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                AlertDialog dialog = builder.create();
                View content = LayoutInflater.from(WallPreferences.this).inflate(R.layout.licence, null);
                dialog.setView(content);
                dialog.show();
                return true;
            }
        });

        backgroundColor.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                ColorPickerDialog picker = new ColorPickerDialog(WallPreferences.this, backgroundColorValue, new ColorPickerDialog.ColorPickerListener() {
                    @Override
                    public void onCancel(ColorPickerDialog dialog) {
                    }

                    @Override
                    public void onOk(ColorPickerDialog dialog, int color) {
                        backgroundColorValue = color;
                        savePreferences();
                    }
                });
                picker.show();

                return true;
            }
        });

        waitTime.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                int time = Integer.parseInt(waitTime.getText());

                if (time < 0)
                    time = 0;

                return true;
            }
        });

        rainAmount.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                AmountPicker p = new AmountPicker(WallPreferences.this, rainAmountValue, 1, 64, new AmountPicker.offsetPickerListener() {
                    @Override
                    public void onOffsetPick(int value) {
                        rainAmountValue = value;
                        savePreferences();
                    }
                });

                p.show();

                return true;
            }

        });

        fallSpeed.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                AmountPicker p = new AmountPicker(WallPreferences.this, fallSpeedValue, 1, 500, new AmountPicker.offsetPickerListener() {
                    @Override
                    public void onOffsetPick(int value) {
                        fallSpeedValue = value;
                        savePreferences();
                    }
                });

                p.show();

                return true;
            }

        });

        verticalOffset.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                OffsetPicker p = new OffsetPicker(WallPreferences.this, verticalOffsetValue, new OffsetPicker.offsetPickerListener() {
                    @Override
                    public void onOffsetPick(int offset) {
                        verticalOffsetValue = offset;
                        savePreferences();
                    }
                });

                p.show();

                return true;
            }

        });

        openGallery.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                if (paintMode.getValue().equals("custom")) {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
                }

                if (paintMode.getValue().equals("default")) {
                    DefaultIconPicker picker = new DefaultIconPicker((Context) WallPreferences.this, new DefaultIconPicker.DefaultIconPickerListener() {
                        @Override
                        public void onImagePick(int id, int number) {
                            defaultIcon = id;
                            iconNumber = number;
                            savePreferences();
                        }
                    });

                    picker.show();
                }
                return true;
            }
        });

        colorSelector.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                ColorPickerDialog picker = new ColorPickerDialog(WallPreferences.this, blockColor, new ColorPickerDialog.ColorPickerListener() {
                    @Override
                    public void onCancel(ColorPickerDialog dialog) {
                    }

                    @Override
                    public void onOk(ColorPickerDialog dialog, int color) {
                        blockColor = color;
                        savePreferences();
                    }
                });
                picker.show();

                return true;
            }
        });

        updatePreferences();
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("paintMode")) {
            if (paintMode.getValue().equals("custom")) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
            }

            if (paintMode.getValue().equals("default")) {
                DefaultIconPicker picker = new DefaultIconPicker(WallPreferences.this, new DefaultIconPicker.DefaultIconPickerListener() {
                    @Override
                    public void onImagePick(int id, int number) {
                        defaultIcon = id;
                        iconNumber = number;
                        openGallery.setSummary("icon");
                        savePreferences();
                    }
                });

                picker.show();
            }
        }

        updatePreferences();
        savePreferences();
    }

    private void savePreferences() {
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);

        Editor e = p.edit();

        e.putInt("blockColor", blockColor);
        e.putInt("backgroundColorValue", backgroundColorValue);
        e.putString("surfacePath", surfacePath);
        e.putInt("defaultIcon", defaultIcon);
        e.putInt("iconNumber", iconNumber);
        e.putInt("verticalOffsetValue", verticalOffsetValue);
        e.putInt("rainAmountValue", rainAmountValue);
        e.putInt("fallSpeedValue", fallSpeedValue);
        e.commit();

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    private void updatePreferences() {
        blockSize.setSummary(blockSize.getEntry());

        fillMode.setSummary(fillMode.getEntry());

        fallStyle.setSummary(fallStyle.getEntry());

        paintMode.setSummary(paintMode.getEntry());

        if (paintMode.getValue().equals("color")) {
            colorSelector.setEnabled(true);
        } else {
            colorSelector.setSummary(paintMode.getValue());
            colorSelector.setEnabled(false);
        }

        if (paintMode.getValue().equals("custom") || paintMode.getValue().equals("default")) {
            fillMode.setEnabled(true);
            openGallery.setEnabled(true);
            openGallery.setSummary(paintMode.getValue().equals("custom") ? surfacePath : "n." + iconNumber);
            blockLimit.setEnabled(true);
        } else {
            fillMode.setEnabled(false);
            openGallery.setEnabled(false);
            blockLimit.setEnabled(false);
        }

        if (fallStyle.getValue().equals("fixed")) {
            fallSpeed.setEnabled(false);
        } else {
            fallSpeed.setEnabled(true);
        }

        if (fallStyle.getValue().equals("rain")) {
            rainAmount.setEnabled(true);
        } else {
            rainAmount.setEnabled(false);
        }

        fps.setSummary(fps.getEntry());
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();

                if (getPath(selectedImageUri) != null && getPath(selectedImageUri) != "") {
                    surfacePath = getPath(selectedImageUri);
                    openGallery.setSummary(surfacePath);
                    savePreferences();
                }
            }
        }
    }

    public String getPath(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    @SuppressWarnings("unused")
    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    @SuppressWarnings("unused")
    private void toast(int text) {
        Toast.makeText(this, this.getResources().getString(text), Toast.LENGTH_LONG).show();
    }

    @SuppressWarnings("unused")
    private void unused() {
        builder.setMessage("Your Message");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        AlertDialog d = builder.create();
        d.show();
    }
}
