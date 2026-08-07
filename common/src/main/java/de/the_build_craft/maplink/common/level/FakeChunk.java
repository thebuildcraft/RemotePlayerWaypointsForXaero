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

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * @author Leander Knüttel
 * @version 06.08.2026
 */
public class FakeChunk extends LevelChunk {
    private final int startIndex;
    private final boolean edge;

    public FakeChunk(Level level, int x, int z, int startIndex, boolean edge) {
        #if MC_VER >= MC_1_18_2
        super(level, new ChunkPos(x, z));
        #else
        super(level, new ChunkPos(x, z), null);
        #endif
        this.startIndex = startIndex;
        this.edge = edge;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return ChunkCache.getBlockState(startIndex, pos.getX(), pos.getY(), pos.getZ());
    }

    public int lightAtPos(int x, int z) {
        return ChunkCache.getLight(startIndex, x, z);
    }

    public int heightAtPos(int x, int z) {
        return ChunkCache.getHeight(startIndex, x, z);
    }

    public ResourceKey<Biome> biomeAtPos(int x, int z) {
        return ChunkCache.getBiome(startIndex, x, z);
    }

    public void done() {
        if (!edge) ChunkCache.setDone(startIndex);
    }
}
