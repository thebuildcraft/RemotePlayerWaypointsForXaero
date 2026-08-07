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
public class AreaSelection {
    public final int minChunkX;
    public final int minChunkZ;
    public final int maxChunkX;
    public final int maxChunkZ;

    public final int minX;
    public final int minZ;
    public final int maxX;
    public final int maxZ;

    public final int centerX;
    public final int centerZ;

    public final int centerChunkX;
    public final int centerChunkZ;

    public final int chunksX;
    public final int chunksZ;

    public AreaSelection(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        this.minChunkX = minChunkX;
        this.minChunkZ = minChunkZ;
        this.maxChunkX = maxChunkX;
        this.maxChunkZ = maxChunkZ;

        minX = minChunkX * 16;
        minZ = minChunkZ * 16;
        maxX = maxChunkX * 16 + 15;
        maxZ = maxChunkZ * 16 + 15;

        centerX = (minX + maxX) / 2;
        centerZ = (minZ + maxZ) / 2;

        centerChunkX = (maxChunkX + minChunkX) / 2;
        centerChunkZ = (maxChunkZ + minChunkZ) / 2;

        chunksX = maxChunkX - minChunkX + 1;
        chunksZ = maxChunkZ - minChunkZ + 1;
    }

    public static AreaSelection fromCenter(int centerChunkX, int centerChunkZ, int chunksX, int chunksZ) {
        return new AreaSelection(centerChunkX - chunksX / 2, centerChunkZ - chunksZ / 2,
                centerChunkX + chunksX / 2, centerChunkZ + chunksZ / 2);
    }
}
