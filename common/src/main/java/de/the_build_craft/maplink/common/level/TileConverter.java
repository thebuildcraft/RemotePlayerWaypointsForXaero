/*
 *    This file is part of the Map Link mod
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2026  Leander Knüttel and contributors
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.the_build_craft.maplink.common.level;

/**
 * @author Leander Knüttel
 * @version 06.08.2026
 */
public class TileConverter {
    public static int fakePlayerLocationX;
    public static int fakePlayerLocationZ;
    public static int fakeRange;

    public static final Object conversionLock = new Object();

    public static void clear() {
        synchronized (conversionLock) {
            ProgressCounter.clear();
            ChunkCache.clear();
            BlockCache.clear();
        }
    }

    public static void init(AreaSelection areaSelection) {
        synchronized (conversionLock) {
            clear();
            ProgressCounter.init();

            fakePlayerLocationX = areaSelection.centerX;
            fakePlayerLocationZ = areaSelection.centerZ;
            fakeRange = Math.max(areaSelection.chunksX, areaSelection.chunksZ) / 2 + 2;

            ChunkCache.init(areaSelection);
            BlockCache.cacheBlockColors();
        }
    }

    public static void writeBlock(int x, int z, int light, int height, int pixelRgb) {
        ChunkCache.writeBlock(x, z, light, height, BlockCache.convertColorToBlockId(pixelRgb));
    }
}
