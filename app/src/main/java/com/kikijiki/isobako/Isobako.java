/*
Copyright 2011 Matteo Bernacchia <android@kikijiki.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.kikijiki.isobako;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import android.widget.Toast;

import com.kikijiki.isobako.Block.Point;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public class Isobako extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new wallEngine();
    }

    private class wallEngine extends Engine implements OnSharedPreferenceChangeListener {
        private final Handler handler = new Handler();
        private final Runnable drawRunner = new Runnable() {
            @Override
            public void run() {
                draw();
            }
        };
        boolean visible = true;
        Paint paint;
        Bitmap block;
        Bitmap surface;
        Bitmap fix;
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        //preferences
        SharedPreferences prefs;
        int fillMode;
        String fallStyle;
        String surfacePath;
        String paintMode;
        float fallSpeed = 20.0f;
        int blockColor = -1;
        int backgroundColor = Color.BLACK;
        int blockSize = 0;
        boolean blockLimit = false;
        int blockAppLimit = 32;
        int defaultIcon = 0;
        int verticalOffset = 0;
        int rainAmount = 24;
        int waitTime = 2000;
        int resetTime = 2000;
        boolean warning = false;
        String loadingMessage = "Loading...";
        boolean needInit = true;
        int cntx = 0;
        int cnty = 0;
        int offset = 0;
        int scrWidth = 0;
        float scrOffset = 0;
        float wideOffset = -1.0f;
        int scrHeight = 0;
        int mapWidth = 0;
        int mapHeight = 0;
        int mapZ = 0;
        Block map[][][];
        long time = 0;
        long reset = 0;
        boolean fade = false;
        int FPS = 30;
        int nextFrame = 33;
        int palette_max = 64;
        boolean fillSpecular = true;
        ColorFilter[] colors;

        public wallEngine() {
            prefs = PreferenceManager.getDefaultSharedPreferences(Isobako.this);
            prefs.registerOnSharedPreferenceChangeListener(this);
            loadingMessage = Isobako.this.getResources().getString(R.string.loadingMessage);
        }

        private void init() {
            String block_size = prefs.getString("blockSize", "medium");
            String fill_mode_tmp = prefs.getString("fillMode", "left");
            fallStyle = prefs.getString("fallStyle", "rain");
            surfacePath = prefs.getString("surfacePath", "");
            paintMode = prefs.getString("paintMode", "default");
            blockColor = prefs.getInt("blockColor", -1);
            backgroundColor = prefs.getInt("backgroundColorValue", Color.BLACK);
            String fps = prefs.getString("fps", "medium");
            blockLimit = prefs.getBoolean("blockLimit", false);
            defaultIcon = prefs.getInt("defaultIcon", Isobako.this.getResources().getIdentifier("default_icon_21", "raw", Isobako.this.getPackageName()));
            verticalOffset = prefs.getInt("verticalOffsetValue", 0);
            rainAmount = prefs.getInt("rainAmountValue", 24);
            fallSpeed = prefs.getInt("fallSpeedValue", 20);

            bitmapOptions.inScaled = false;

            wideOffset = -1.0f;
            fade = false;

            try {
                waitTime = Integer.parseInt(prefs.getString("waitTime", "2")) * 1000;
            } catch (NumberFormatException e) {
                waitTime = 2000;
            }

            if (fix != null) {
                fix.recycle();
                fix = null;
            }

            if (block_size.equals("small")) {

                block = BitmapFactory.decodeResource(getResources(), R.raw.block_small, bitmapOptions);
                blockSize = 0;
            }

            if (block_size.equals("medium")) {
                block = BitmapFactory.decodeResource(getResources(), R.raw.block_medium, bitmapOptions);
                blockSize = 1;
            }

            if (block_size.equals("big")) {
                block = BitmapFactory.decodeResource(getResources(), R.raw.block_big, bitmapOptions);
                blockSize = 2;
            }

            Block.setSize(blockSize);

            if (fps.equals("slow")) FPS = 10;
            if (fps.equals("medium")) FPS = 20;
            if (fps.equals("fast")) FPS = 30;

            nextFrame = 1000 / FPS;

            cntx = scrWidth / 2 - Block.halfWidth;
            cnty = scrHeight / 2;

            int size = Math.min(scrHeight, scrWidth) / Block.blockWidth;

            mapWidth = size;
            mapHeight = size;
            mapZ = size;

            paint = new Paint();
            paint.setAntiAlias(false);

            //Settings
            if (fill_mode_tmp.equals("left")) {
                fillMode = 2;
                fillSpecular = true;
            }

            if (fill_mode_tmp.equals("right")) {
                fillMode = 3;
                fillSpecular = true;
            }

            if (fill_mode_tmp.equals("front")) {
                fillMode = 0;
                fillSpecular = false;
            }

            if (fill_mode_tmp.equals("front_specular")) {
                fillMode = 0;
                fillSpecular = true;
            }

            if (fill_mode_tmp.equals("top")) {
                fillMode = 1;
                fillSpecular = false;
            }

            if (paintMode.equals("custom")) {
                surface = BitmapFactory.decodeFile(surfacePath, bitmapOptions);

                if (surface == null) {
                    if (surfacePath != null && surfacePath.length() > 0)
                        toast(R.string.surfaceLoadError);

                    paintMode = "color";
                    blockColor = -1;
                    loadColor();
                } else {
                    loadSurface();
                }
            }

            if (paintMode.equals("random")) {
                loadRandom();
            }

            if (paintMode.equals("default")) {
                if (defaultIcon == 0) {
                    toast(R.string.surfaceLoadError);

                    paintMode = "color";
                    blockColor = -1;

                    loadColor();
                } else {
                    loadDefaultIcon();
                }
            }

            if (paintMode.equals("color")) {
                loadColor();
            }

            //Fall mode
            if (fallStyle.equals("one")) {
                loadMapOne();
            }

            if (fallStyle.equals("rain")) {
                loadMapRain(rainAmount);
            }

            if (fallStyle.equals("fixed")) {
                reset = -1;
                loadMapFixed();
            }

            time = System.currentTimeMillis();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            this.visible = false;
            handler.removeCallbacks(drawRunner);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            scrWidth = width;
            scrOffset = scrWidth * .1f;
            scrHeight = height;

            needInit = true;

            super.onSurfaceChanged(holder, format, width, height);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;

            if (visible) {
                time = System.currentTimeMillis();
                handler.post(drawRunner);
            } else {
                handler.removeCallbacks(drawRunner);
            }
        }

        private void draw() {
            if(!this.visible){ return; }
            SurfaceHolder holder = getSurfaceHolder();
            Canvas c;

            long now = System.currentTimeMillis();
            long milli = (now - time);
            float ela = (float) milli * .001f;
            time = now;

            try {
                c = holder.lockCanvas();

                c.drawColor(backgroundColor);

                if (needInit) {
                    if (paint == null)
                        paint = new Paint();

                    paint.setColorFilter(null);
                    paint.setAlpha(255);
                    paint.setTextSize(20.0f);
                    paint.setColor(Color.rgb(255 - Color.red(backgroundColor), 255 - Color.green(backgroundColor), 255 - Color.blue(backgroundColor)));

                    c.drawText(loadingMessage, 100, 100, paint);
                    holder.unlockCanvasAndPost(c);

                    init();

                    needInit = false;
                } else {
                    drawMap(c, ela, milli);
                    holder.unlockCanvasAndPost(c);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            handler.removeCallbacks(drawRunner);

            long frameTime = System.currentTimeMillis() - now;
            int next = (int) Math.max(0, nextFrame - frameTime);
            handler.postDelayed(drawRunner, next);
        }

        private void drawFixed(Canvas c) {
            int halfMap = (fix.getWidth() - scrWidth) / 2;

            paint.setColorFilter(null);
            c.drawBitmap(fix, offset - halfMap, 0, paint);
        }

        private void drawMap(Canvas c, float ela, long milli) {
            if (this.isPreview())
                offset = 0;

            if (fade) {
                reset -= milli;
                paint.setAlpha(255);
                paint.setColorFilter(null);

                if (reset > 0) {
                    if (fix == null)
                        renderToSurface();

                    if (reset < resetTime) {
                        float alpha = (float) reset / (float) resetTime;

                        paint.setColorFilter(null);
                        paint.setAlpha((int) (alpha * 255));

                        drawFixed(c);

                        return;
                    } else {
                        drawFixed(c);
                        return;
                    }
                } else {
                    reset = 0;
                    if (fix != null) {
                        fix.recycle();
                        fix = null;
                    }

                    paint.setAlpha(255);

                    needInit = true;
                    return;
                }
            }

            if (fix != null) {
                drawFixed(c);
                return;
            }

            int sx = cntx + offset;
            int sy = cnty - verticalOffset;
            int cx = sx;
            int cy = sy;
            int fx = 0;
            int fy = 0;
            int dx = Block.halfWidth;
            int dy = Block.partialHeight;

            boolean end = true;

            for (int i = 0; i < mapWidth; i++) {
                for (int j = 0; j < mapHeight; j++) {
                    for (int k = 0; k < mapZ; k++) {
                        Block cur = map[i][j][k];

                        if (cur.visible) {
                            cur.update(ela);

                            updateBlock(i, j, k, cur);

                            fy = cy - Block.halfHeight * k;
                            fx = cx;

                            if (!cur.stop) {
                                fy -= (int) cur.fallStatus;
                                end = false;
                            }

                            if (fy > scrHeight) {
                                cur.visible = false;
                            } else if ((fy + Block.blockHeight) > 0 &&
                                    fx < scrWidth &&
                                    fx + Block.blockWidth >= 0) {
                                paint.setColorFilter(colors[cur.color]);
                                c.drawBitmap(block, fx, fy, paint);
                            }
                        }
                    }

                    cx += dx;
                    cy += dy;
                }

                sx -= dx;
                sy += dy;
                cx = sx;
                cy = sy;
            }

            paint.setAlpha(255);
            paint.setColorFilter(null);

            if (end) {
                reset = resetTime + waitTime;
                fade = true;
            }
        }

        //Remove if not visible
        private void updateBlock(int x, int y, int z, Block b) {
            if (x == (mapWidth - 1) || y == (mapHeight - 1) || z == (mapZ - 1) || !b.stop)
                return;

            Block bl = map[x + 1][y][z];
            Block br = map[x][y + 1][z];
            Block t = map[x][y][z + 1];

            Boolean del = (bl != null) && bl.stop;
            del = del && (br != null) && br.stop;
            del = del && (t != null) && t.stop;

            b.visible = !del;
        }

        private void loadColor() {
            colors = new LightingColorFilter[]{new LightingColorFilter(blockColor, 1)};

            map = new Block[mapWidth][mapHeight][mapZ];

            for (int i = 0; i < mapWidth; i++) {
                for (int j = 0; j < mapHeight; j++) {
                    for (int k = 0; k < mapZ; k++) {
                        Block cur = new Block();
                        map[i][j][k] = cur;

                        cur.color = 0;
                    }
                }
            }
        }

        private void loadRandom() {
            loadDefaultPalette();

            map = new Block[mapWidth][mapHeight][mapZ];

            Random r = new Random(time);

            for (int i = 0; i < mapWidth; i++) {
                for (int j = 0; j < mapHeight; j++) {
                    for (int k = 0; k < mapZ; k++) {
                        Block cur = new Block();
                        map[i][j][k] = cur;

                        cur.color = r.nextInt(colors.length);
                    }
                }
            }
        }

        private void loadDefaultIcon() {
            surface = BitmapFactory.decodeResource(getResources(), defaultIcon, bitmapOptions);

            if (surface == null) {
                toast(R.string.surfaceLoadError);

                paintMode = "color";
                blockColor = -1;

                loadColor();
            } else {
                loadSurface();
            }
        }

        private void loadSurface() {
            int width = surface.getWidth();
            int height = surface.getHeight();

            //only square textures
            if (width != height) {
                int min = Math.min(width, height);
                width = height = min;
            }

            int old = width;
            if (width > mapWidth) {
                if (blockLimit) {
                    width = Math.min(mapWidth, blockAppLimit);
                } else {
                    width = Math.min(width, blockAppLimit);
                }

                if (old != width) {
                    if (!warning && isPreview()) {
                        toast(Isobako.this.getResources().getString(R.string.resizeWarning));
                        warning = true;
                    }
                }

                height = width;
                Bitmap tmp = Bitmap.createScaledBitmap(surface, width, height, false);
                surface = tmp;
            }

            wideOffset = (((float) (width + 2) * (float) Block.blockWidth) - (float) scrWidth);

            int pix[] = new int[width * height];

            surface.getPixels(pix, 0, width, 0, 0, width, height);

            int pix2[] = pix.clone();
            int palette[] = new int[palette_max];

            Arrays.sort(pix);
            palette[0] = pix[0];
            int last = pix[0];
            int palette_index = 1;

            map = new Block[width][width][height];
            mapWidth = width;
            mapHeight = width;
            mapZ = height;

            height = width;

            //generate palette
            for (int i = 1; i < pix.length && palette_index < 32; i++) {
                if (pix[i] != last) {
                    palette[palette_index] = pix[i];
                    last = pix[i];
                    palette_index++;
                }
            }

            colors = new LightingColorFilter[palette_index];

            for (int i = 0; i < palette_index; i++) {
                colors[i] = new LightingColorFilter((Integer) palette[i], 1);
            }

            //fill map
            for (int i = 0; i < pix.length; i++) {
                int x = i % width;
                int z = height - i / width - 1;

                int color = 0;

                //find color from palette
                for (int j = 0; j < palette_index; j++) {
                    if (palette[j] == pix2[i]) {
                        color = j;
                        break;
                    }
                }

                switch (fillMode) {
                    case 0: //le due facce frontali
                    {
                        int fx = x;

                        if (!fillSpecular) {
                            fx = width - 1 - x;
                        }

                        map[width - 1][x][z] = new Block();
                        map[width - 1][x][z].color = color;

                        map[fx][width - 1][z] = new Block();
                        map[fx][width - 1][z].color = color;

                        for (int j = 0; j < width - 1; j++) {
                            if (x != width - 1) {
                                map[j][x][z] = new Block();
                                map[j][x][z].color = color;

                                map[fx][j][z] = new Block();
                                map[fx][j][z].color = color;
                            }
                        }
                    }
                    break;

                    case 1: //la faccia superiore
                    {
                        for (int h = 0; h < height; h++) {
                            map[height - 1 - z][x][h] = new Block();
                            map[height - 1 - z][x][h].color = color;
                        }
                    }
                    break;

                    case 2: {
                        for (int j = 0; j < width; j++) {

                            map[j][x][z] = new Block();
                            map[j][x][z].color = color;
                        }
                    }
                    break;

                    case 3: {
                        for (int j = 0; j < width; j++) {
                            map[x][j][z] = new Block();
                            map[x][j][z].color = color;
                        }
                    }
                }
            }
        }

        private void loadMapFixed() {
            for (int i = 0; i < mapWidth; i++) {
                for (int j = 0; j < mapHeight; j++) {
                    for (int k = 0; k < mapZ; k++) {
                        Block b = map[i][j][k];

                        b.fallStatus = 0;
                        b.stop = true;
                    }
                }
            }

            renderToSurface();
        }

        private void renderToSurface() {
            int sw = Block.blockWidth * mapWidth;

            fix = Bitmap.createBitmap(sw, scrHeight, Bitmap.Config.ARGB_8888);
            Canvas tmp = new Canvas(fix);
            tmp.setDensity(block.getDensity());

            paint.setColorFilter(null);

            int sx = sw / 2 - Block.halfWidth;
            int sy = cnty - verticalOffset;
            int cx = sx;
            int cy = sy;
            int fx = 0;
            int fy = 0;
            int dx = Block.halfWidth;
            int dy = Block.partialHeight;

            for (int i = 0; i < mapWidth; i++) {
                for (int j = 0; j < mapHeight; j++) {
                    for (int k = 0; k < mapZ; k++) {
                        Block cur = map[i][j][k];

                        if (cur.visible) {
                            fy = cy - Block.halfHeight * k;
                            fx = cx;

                            paint.setColorFilter(colors[cur.color]);
                            tmp.drawBitmap(block, fx, fy, paint);
                        }
                    }

                    cx += dx;
                    cy += dy;
                }

                sx -= dx;
                sy += dy;
                cx = sx;
                cy = sy;
            }

            paint.setAlpha(255);
            paint.setColorFilter(null);
        }

        private void loadMapRain(int amount) {
            Point[] rain = new Point[mapWidth * mapHeight * mapZ];

            int index = 0;
            for (int i = 0; i < mapWidth; i++) {
                for (int j = 0; j < mapHeight; j++) {
                    for (int k = 0; k < mapZ; k++) {
                        Point p = new Point(i, j, 0);
                        rain[index] = p;
                        index++;
                    }
                }
            }

            Collections.shuffle(Arrays.asList(rain));
            int[][] zorder = new int[mapWidth][mapHeight];
            int fall = 1;

            for (int i = 0; i < rain.length; i++) {
                Point p = rain[i];
                int z = zorder[p.x][p.y];

                Block cur = map[p.x][p.y][z];

                cur.fallStatus = (scrHeight * fall) / amount + verticalOffset + scrHeight / 2;
                cur.fallSpeed = fallSpeed;
                cur.stop = false;
                cur.visible = true;

                zorder[p.x][p.y]++;
                fall++;
            }
        }

        private void loadMapOne() {
            Point[] rain = new Point[mapWidth * mapHeight * mapZ];

            int index = 0;
            for (int i = 0; i < mapWidth; i++) {
                for (int j = 0; j < mapHeight; j++) {
                    for (int k = 0; k < mapZ; k++) {
                        Point p = new Point(i, j, 0);
                        rain[index] = p;
                        index++;
                    }
                }
            }

            Collections.shuffle(Arrays.asList(rain));
            int[][] zorder = new int[mapWidth][mapHeight];
            int fall = 1;

            for (int i = 0; i < rain.length; i++) {
                Point p = rain[i];
                int z = zorder[p.x][p.y];

                Block cur = map[p.x][p.y][z];

                cur.fallStatus = scrHeight * fall * (i == 0 ? .5f : 1.0f) + verticalOffset;
                cur.fallSpeed = fallSpeed;
                cur.stop = false;
                cur.visible = true;

                zorder[p.x][p.y]++;
                fall++;
            }
        }

        private void loadDefaultPalette() {
            Random rnd = new Random(time);

            int palette = rnd.nextInt(8);

            colors = new LightingColorFilter[palette];

            for (int i = 0; i < palette; i++) {
                int r = rnd.nextInt(256);
                int g = rnd.nextInt(256);
                int b = rnd.nextInt(256);

                colors[i] = new LightingColorFilter(Color.argb(255, r, g, b), 1);
            }
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {
            offset = (int) ((xOffset - .5f) * (wideOffset > .0f ? wideOffset : scrOffset) * -1.0f);

            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences arg0, String arg1) {
            warning = false;
            needInit = true;
        }

        private void toast(String text) {
            Toast.makeText(Isobako.this, text, Toast.LENGTH_LONG).show();
        }

        private void toast(int text) {
            Toast.makeText(Isobako.this, Isobako.this.getResources().getString(text), Toast.LENGTH_LONG).show();
        }
    }

}
