package com.kikijiki.isobako;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class DefaultIconPicker implements OnClickListener {
    View picker;
    LinearLayout ll;
    AlertDialog dialog;
    DefaultIconPickerListener listener;
    @SuppressLint("UseSparseArrays")
    Map<Integer, ArrayList<ImageView>> map = new HashMap<Integer, ArrayList<ImageView>>();

    public DefaultIconPicker(Context context, DefaultIconPickerListener l) {
        listener = l;

        picker = LayoutInflater.from(context).inflate(R.layout.icon_picker, null);
        ll = (LinearLayout) picker.findViewById(R.id.iconListLayout);

        //add images to layout
        int id = 0;
        int inc = 0;

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inScaled = false;

        //Read icons
        while (true) {
            String icon_name = "default_icon_" + inc;
            id = context.getResources().getIdentifier(icon_name, "raw", context.getPackageName());

            if (id == 0)
                break;

            Bitmap b = BitmapFactory.decodeResource(context.getResources(), id, bitmapOptions);

            Bitmap preview = Bitmap.createScaledBitmap(b, 64, 64, false);

            ImageView img = new ImageView(context);
            img.setImageBitmap(preview);
            img.setTag(new int[]{id, inc});
            img.setOnClickListener(this);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 8, 0);
            img.setLayoutParams(lp);

            if (map.get(b.getWidth()) == null) {
                map.put(b.getWidth(), new ArrayList<ImageView>());
            }

            map.get(b.getWidth()).add(img);

            b.recycle();

            inc++;
        }

        SortedSet<Integer> sortedset = new TreeSet<Integer>(map.keySet());
        Iterator<Integer> it = sortedset.iterator();

        while (it.hasNext()) {
            Integer key = it.next();
            ArrayList<ImageView> value = map.get(key);

            TextView t = new TextView(context);
            t.setTextColor(Color.WHITE);

            String tmp = value.size() > 1 ? context.getResources().getString(R.string.galleryPicturesLabel) : context.getResources().getString(R.string.galleryPictureLabel);
            t.setText(key.toString() + "x" + key.toString() + ", " + value.size() + " " + tmp);
            ll.addView(t);

            HorizontalScrollView sw = new HorizontalScrollView(context);
            LinearLayout cll = new LinearLayout(context);
            cll.setOrientation(LinearLayout.HORIZONTAL);

            for (ImageView img : value) {
                cll.addView(img);
            }

            sw.addView(cll);
            ll.addView(sw);
        }

        dialog = new AlertDialog.Builder(context).create();
        dialog.setView(picker);
    }

    public void show() {
        dialog.show();
    }

    @Override
    public void onClick(View arg0) {
        int[] id = (int[]) arg0.getTag();

        ((ImageView) arg0).setAlpha(100);

        listener.onImagePick(id[0], id[1] + 1);

        dialog.dismiss();
    }

    public interface DefaultIconPickerListener {
        public void onImagePick(int id, int number);
    }
}
