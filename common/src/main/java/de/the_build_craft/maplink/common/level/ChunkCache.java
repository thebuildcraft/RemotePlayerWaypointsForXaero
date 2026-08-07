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

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

/**
 * @author Leander Knüttel
 * @version 06.08.2026
 */
public class ChunkCache {
    //[readyBit | light: 4 bits | height: 11 bits | fakeBlockId: 16 bits]
    //index = chunkPosX * 16*16*chunksInZDirection + chunkPosZ * 16*16 + localX * 16 + localZ
    private static int[] blocks;
    private static boolean[] chunksToRead;
    private static int startChunkX;
    private static int startChunkZ;
    private static int endChunkX;
    private static int endChunkZ;
    private static int chunksInZDirection;
    private static int yMin;
    private static int yMax;

    static void init(AreaSelection areaSelection) {
        Level level = Minecraft.getInstance().level;
        if (level == null) throw new IllegalStateException();
        #if MC_VER > MC_1_21_1
        yMin = level.getMinY();
        yMax = level.getMaxY() - 2;
        #elif MC_VER > MC_1_16_5
        yMin = level.getMinBuildHeight();
        yMax = level.getMaxBuildHeight() - 2;
        #else
        yMin = 0;
        yMax = level.getMaxBuildHeight() - 2;
        #endif
        startChunkX = areaSelection.minChunkX;
        startChunkZ = areaSelection.minChunkZ;
        endChunkX = areaSelection.maxChunkX;
        endChunkZ = areaSelection.maxChunkZ;
        chunksInZDirection = areaSelection.chunksZ;
        blocks = new int[areaSelection.chunksX * areaSelection.chunksZ * 256];
        chunksToRead = new boolean[areaSelection.chunksX * areaSelection.chunksZ];
    }

    private static int calcIndex(int x, int z) {
        return (((x >> 4) - startChunkX) * chunksInZDirection + ((z >> 4) - startChunkZ)) * 256 + (x & 15) * 16 + (z & 15);
    }

    private static int pack(int light, int height, short fakeBlockId) {
        return (1 << 31) | ((light & 0b1111) << 27) | (((Math.min(height, yMax) - yMin) & 0b11111111111) << 16) | ((((int)fakeBlockId) - Short.MIN_VALUE) & 0b1111111111111111);
    }

    private static int unpackLight(int packedBlock) {
        return (packedBlock >> 27) & 0b1111;
    }

    private static int unpackHeight(int packedBlock) {
        return ((packedBlock >> 16) & 0b11111111111) + yMin;
    }

    private static short unpackFakeBlockId(int packedBlock) {
        return (short) ((packedBlock & 0b1111111111111111) + Short.MIN_VALUE);
    }

    static void writeBlock(int x, int z, int light, int height, short fakeBlockId) {
        if (x >> 4 < startChunkX || x >> 4 > endChunkX || z >> 4 < startChunkZ || z >> 4 > endChunkZ) return;
        int index = calcIndex(x, z);
        if (index >= blocks.length || index < 0) return;
        blocks[index] = pack(light, height, fakeBlockId);
    }

    public static BlockState getBlockState(int startIndex, int x, int y, int z) {
        int packedBlock = blocks[startIndex + x * 16 + z];
        return BlockCache.getBlockState(unpackFakeBlockId(packedBlock), y - unpackHeight(packedBlock));
    }

    public static int getLight(int startIndex, int x, int z) {
        return unpackLight(blocks[startIndex + x * 16 + z]);
    }

    public static int getHeight(int startIndex, int x, int z) {
        int packedBlock = blocks[startIndex + x * 16 + z];
        return unpackHeight(packedBlock) + BlockCache.getFakeBlock(unpackFakeBlockId(packedBlock)).additionalHeight;
    }

    public static ResourceKey<Biome> getBiome(int startIndex, int x, int z) {
        return BlockCache.getFakeBlock(unpackFakeBlockId(blocks[startIndex + x * 16 + z])).biome;
    }

    public static FakeChunk getFakeChunk(Level level, int x, int z, boolean edge) {
        if (x < startChunkX || x > endChunkX || z < startChunkZ || z > endChunkZ) return null;
        int startIndex = ((x - startChunkX) * chunksInZDirection + (z - startChunkZ)) * 256;
        if (startIndex >= blocks.length || startIndex < 0) return null;
        if (blocks[startIndex] == 0
                || blocks[startIndex + 15 * 16] == 0
                || blocks[startIndex + 15] == 0
                || blocks[startIndex + 15 * 16 + 15] == 0) {
            return null;
        }
        return new FakeChunk(level, x, z, startIndex, edge);
    }

    private static boolean checkChunk(int x, int z) {
        if (x < startChunkX || x > endChunkX || z < startChunkZ || z > endChunkZ) return false;
        int startIndex = ((x - startChunkX) * chunksInZDirection + (z - startChunkZ)) * 256;
        if (startIndex >= blocks.length || startIndex < 0) return false;
        return blocks[startIndex] != 0
                && blocks[startIndex + 15 * 16] != 0
                && blocks[startIndex + 15] != 0
                && blocks[startIndex + 15 * 16 + 15] != 0;
    }

    public static void countChunks() {
        for (int x = startChunkX + 1; x < endChunkX; x++) {
            for (int z = startChunkZ + 1; z < endChunkZ; z++) {
                if (!checkChunk(x, z)) continue;
                ProgressCounter.totalChunks.incrementAndGet();
                chunksToRead[(x - startChunkX) * chunksInZDirection + (z - startChunkZ)] = true;
            }
        }
    }

    public static void setDone(int startIndex) {
        int tempIndex = startIndex / 256;
        if (chunksToRead == null || tempIndex < 0 || tempIndex >= chunksToRead.length) return;
        if (chunksToRead[tempIndex]) {
            chunksToRead[tempIndex] = false;
            if (ProgressCounter.renderedChunks.incrementAndGet() >= ProgressCounter.totalChunks.get()) TileConverter.clear();
        }
    }

    public static void setDone(int x, int z) {
        int startIndex = ((x - startChunkX) * chunksInZDirection + (z - startChunkZ)) * 256;
        setDone(startIndex);
    }

    static void clear() {
        blocks = new int[0];
        chunksToRead = new boolean[0];
    }
}
