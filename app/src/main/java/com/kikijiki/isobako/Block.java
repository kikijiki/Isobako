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

public class Block {
    private final static int[] _blockWidth = new int[]{29, 53, 101};
    private final static int[] _halfWidth = new int[]{_blockWidth[0] / 2, _blockWidth[1] / 2, _blockWidth[2] / 2};
    private final static int[] _blockHeight = new int[]{33, 60, 113};
    private final static int[] _halfHeight = new int[]{18, 34, 63};
    private final static int[] _partialHeight = new int[]{7, 13, 25};
    public static int blockWidth;
    public static int halfWidth;
    public static int blockHeight;
    public static int halfHeight;
    public static int partialHeight;
    public int color = 5;
    public float fallStatus = 0;
    public float fallSpeed = 100.0f;
    public boolean stop = false;
    public boolean visible = true;

    public static void setSize(int size) {
        blockWidth = _blockWidth[size];
        halfWidth = _halfWidth[size];
        blockHeight = _blockHeight[size];
        halfHeight = _halfHeight[size];
        partialHeight = _partialHeight[size];
    }

    public void update(float elapsed) {
        if (fallStatus > 0) {
            fallStatus -= elapsed * fallSpeed;

            if (fallStatus < 0) {
                fallStatus = 0;
                stop = true;
            }
        }
    }

    public static class Point {
        int x, y, z;

        public Point(int xx, int yy, int zz) {
            x = xx;
            y = yy;
            z = zz;
        }
    }
}
