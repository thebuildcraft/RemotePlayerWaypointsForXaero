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

package de.the_build_craft.maplink.mixins.common.mods.xaeroworldmap;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.the_build_craft.maplink.common.level.ChunkCache;
import de.the_build_craft.maplink.common.level.FakeChunk;
import de.the_build_craft.maplink.common.level.ProgressCounter;
import de.the_build_craft.maplink.common.level.TileConverter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
#if MC_VER > MC_1_20_4
import net.minecraft.world.level.chunk.status.ChunkStatus;
#else
import net.minecraft.world.level.chunk.ChunkStatus;
#endif
import net.minecraft.world.level.material.FluidState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.MapProcessor;
import xaero.map.MapWriter;
import xaero.map.biome.BiomeColorCalculator;
import xaero.map.biome.BlockTintProvider;
import xaero.map.region.MapBlock;
import xaero.map.region.MapUpdateFastConfig;
import xaero.map.region.OverlayManager;

/**
 * @author Leander Knüttel
 * @version 06.08.2026
 */
@Pseudo
@Mixin(MapWriter.class)
public class MapWriterMixin {
    #if MC_VER > MC_1_20_4
    @WrapOperation(method = "writeChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;"))
    private ChunkAccess getFakeChunk(Level instance, int x, int z, ChunkStatus chunkStatus, boolean require, Operation<ChunkAccess> original) {
    #else
    @WrapOperation(method = "writeChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getChunk(IILnet/minecraft/world/level/chunk/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;"))
    private ChunkAccess getFakeChunk(Level instance, int x, int z, ChunkStatus chunkStatus, boolean require, Operation<ChunkAccess> original) {
    #endif
        ChunkAccess realChunk = original.call(instance, x, z, chunkStatus, require);
        if (!ProgressCounter.readyForRender.get()) return realChunk;
        if (realChunk != null && !(realChunk instanceof EmptyLevelChunk)) {
            ChunkCache.setDone(x, z);
            return realChunk;
        }
        try {
            return ChunkCache.getFakeChunk(instance, x, z, false);
        } catch (Exception ignored) {
            return realChunk;
        }
    }

    @WrapOperation(method = "writeChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getChunk(II)Lnet/minecraft/world/level/chunk/LevelChunk;"))
    private LevelChunk getFakeChunk(Level instance, int chunkX, int chunkZ, Operation<LevelChunk> original) {
        LevelChunk realChunk = original.call(instance, chunkX, chunkZ);
        if (!ProgressCounter.readyForRender.get()) return realChunk;
        if (realChunk != null && !(realChunk instanceof EmptyLevelChunk)) return realChunk;
        try {
            return ChunkCache.getFakeChunk(instance, chunkX, chunkZ, true);
        } catch (Exception ignored) {
            return realChunk;
        }
    }

    #if MC_VER > MC_1_19_2
    @WrapOperation(method = "writeChunk", at = @At(value = "INVOKE", target = "Lxaero/map/MapWriter;loadPixel(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/Registry;Lxaero/map/region/MapBlock;Lxaero/map/region/MapBlock;Lnet/minecraft/world/level/chunk/LevelChunk;IIIIZZIZZLnet/minecraft/core/Registry;ZILnet/minecraft/core/BlockPos$MutableBlockPos;)V"))
    private void fakeHeight(MapWriter instance, Level world, Registry<Block> blockRegistry, MapBlock pixel, MapBlock currentPixel, LevelChunk bchunk, int insideX, int insideZ, int highY, int lowY, boolean cave, boolean fullCave, int mappedHeight, boolean canReuseBiomeColours, boolean ignoreHeightmaps, Registry<Biome> biomeRegistry, boolean flowers, int worldBottomY, BlockPos.MutableBlockPos mutableBlockPos3, Operation<Void> original) {
        if (ProgressCounter.readyForRender.get() && bchunk instanceof FakeChunk) {
            try {
                highY = ((FakeChunk) bchunk).heightAtPos(insideX, insideZ);
            } catch (Exception ignored) {}
        }
        original.call(instance, world, blockRegistry, pixel, currentPixel, bchunk, insideX, insideZ, highY, lowY, cave, fullCave, mappedHeight, canReuseBiomeColours, ignoreHeightmaps, biomeRegistry, flowers, worldBottomY, mutableBlockPos3);
    }
    #elif MC_VER >= MC_1_18_2
    @WrapOperation(method = "writeChunk", at = @At(value = "INVOKE", target = "Lxaero/map/MapWriter;loadPixel(Lnet/minecraft/world/level/Level;Lxaero/map/region/MapBlock;Lxaero/map/region/MapBlock;Lnet/minecraft/world/level/chunk/LevelChunk;IIIIZZIZZLnet/minecraft/core/Registry;ZILnet/minecraft/core/BlockPos$MutableBlockPos;)V"))
    private void fakeHeight(MapWriter instance, Level world, MapBlock pixel, MapBlock currentPixel, LevelChunk bchunk, int insideX, int insideZ, int highY, int lowY, boolean cave, boolean fullCave, int mappedHeight, boolean canReuseBiomeColours, boolean ignoreHeightmaps, Registry<Biome> biomeRegistry, boolean flowers, int worldBottomY, BlockPos.MutableBlockPos mutableBlockPos3, Operation<Void> original) {
        if (ProgressCounter.readyForRender.get() && bchunk instanceof FakeChunk) {
            try {
                highY = ((FakeChunk) bchunk).heightAtPos(insideX, insideZ);
            } catch (Exception ignored) {}
        }
        original.call(instance, world, pixel, currentPixel, bchunk, insideX, insideZ, highY, lowY, cave, fullCave, mappedHeight, canReuseBiomeColours, ignoreHeightmaps, biomeRegistry, flowers, worldBottomY, mutableBlockPos3);
    }
    #else
    @WrapOperation(method = "writeChunk", at = @At(value = "INVOKE", target = "Lxaero/map/MapWriter;loadPixel(Lnet/minecraft/world/level/Level;Lxaero/map/region/MapBlock;Lxaero/map/region/MapBlock;Lnet/minecraft/world/level/chunk/LevelChunk;IIIIZZIZZLnet/minecraft/core/WritableRegistry;ZLnet/minecraft/core/BlockPos$MutableBlockPos;)V"))
    private void fakeHeight(MapWriter instance, Level world, MapBlock pixel, MapBlock currentPixel, LevelChunk bchunk, int insideX, int insideZ, int highY, int lowY, boolean cave, boolean fullCave, int mappedHeight, boolean canReuseBiomeColours, boolean ignoreHeightmaps, WritableRegistry<Biome> biomeRegistry, boolean flowers, BlockPos.MutableBlockPos mutableBlockPos3, Operation<Void> original) {
        if (ProgressCounter.readyForRender.get() && bchunk instanceof FakeChunk) {
            try {
                highY = ((FakeChunk) bchunk).heightAtPos(insideX, insideZ);
            } catch (Exception ignored) {}
        }
        original.call(instance, world, pixel, currentPixel, bchunk, insideX, insideZ, highY, lowY, cave, fullCave, mappedHeight, canReuseBiomeColours, ignoreHeightmaps, biomeRegistry, flowers, mutableBlockPos3);
    }
    #endif

    @Inject(method = "getWriteDistance", at = @At(value = "RETURN"), cancellable = true)
    private void fakeDistance(CallbackInfoReturnable<Integer> cir) {
        if (ProgressCounter.readyForRender.get()) cir.setReturnValue(TileConverter.fakeRange);
    }

    #if MC_VER > MC_1_19_2
    @WrapOperation(method = "loadPixel", at = @At(value = "INVOKE", target = "Lxaero/map/region/MapBlock;write(Lnet/minecraft/world/level/block/state/BlockState;IILnet/minecraft/resources/ResourceKey;BZZ)V"))
    private void fakeWrite(MapBlock instance, BlockState state, int height, int topHeight, ResourceKey<Biome> biomeIn, byte light, boolean glowing, boolean cave, Operation<Void> original, Level world, Registry<Block> blockRegistry, MapBlock pixel, MapBlock currentPixel, LevelChunk bchunk, int insideX, int insideZ) {
        if (ProgressCounter.readyForRender.get() && bchunk instanceof FakeChunk) {
            try {
                biomeIn = ((FakeChunk) bchunk).biomeAtPos(insideX, insideZ);
                light = (byte) ((FakeChunk)bchunk).lightAtPos(insideX, insideZ);
                original.call(instance, state, height, topHeight, biomeIn, light, glowing, cave);
                if (insideX == 15 && insideZ == 15) ((FakeChunk) bchunk).done();
            } catch (Exception ignored) {}
        } else {
            original.call(instance, state, height, topHeight, biomeIn, light, glowing, cave);
        }
    }
    #else
    @WrapOperation(method = "loadPixel", at = @At(value = "INVOKE", target = "Lxaero/map/region/MapBlock;write(Lnet/minecraft/world/level/block/state/BlockState;IILnet/minecraft/resources/ResourceKey;BZZ)V"))
    private void fakeWrite(MapBlock instance, BlockState state, int height, int topHeight, ResourceKey<Biome> biomeIn, byte light, boolean glowing, boolean cave, Operation<Void> original, Level world, MapBlock pixel, MapBlock currentPixel, LevelChunk bchunk, int insideX, int insideZ) {
        if (ProgressCounter.readyForRender.get() && bchunk instanceof FakeChunk) {
            try {
                biomeIn = ((FakeChunk) bchunk).biomeAtPos(insideX, insideZ);
                light = (byte) ((FakeChunk)bchunk).lightAtPos(insideX, insideZ);
                original.call(instance, state, height, topHeight, biomeIn, light, glowing, cave);
                ((FakeChunk) bchunk).done();
            } catch (Exception ignored) {}
        } else {
            original.call(instance, state, height, topHeight, biomeIn, light, glowing, cave);
        }
    }
    #endif

    #if MC_VER > MC_1_19_2
    @WrapOperation(method = "loadPixel", at = @At(value = "INVOKE", target = "Lxaero/map/MapWriter;loadPixelHelp(Lxaero/map/region/MapBlock;Lxaero/map/region/MapBlock;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/Registry;Lnet/minecraft/world/level/block/state/BlockState;BBLnet/minecraft/world/level/chunk/LevelChunk;IIIZZLnet/minecraft/world/level/material/FluidState;Lnet/minecraft/core/Registry;IZZZ)Z"))
    private boolean fakeOverlayLight(MapWriter instance, MapBlock pixel, MapBlock currentPixel, Level world, Registry<Block> blockRegistry, BlockState state, byte light, byte skyLight, LevelChunk bchunk, int insideX, int insideZ, int h, boolean canReuseBiomeColours, boolean cave, FluidState fluidFluidState, Registry<Biome> biomeRegistry, int transparentSkipY, boolean shouldExtendTillTheBottom, boolean flowers, boolean underair, Operation<Boolean> original) {
        if (ProgressCounter.readyForRender.get() && bchunk instanceof FakeChunk) {
            try {
                light = (byte) ((FakeChunk)bchunk).lightAtPos(insideX, insideZ);
            } catch (Exception ignored) {}
        }
        return original.call(instance, pixel, currentPixel, world, blockRegistry, state, light, skyLight, bchunk, insideX, insideZ, h, canReuseBiomeColours, cave, fluidFluidState, biomeRegistry, transparentSkipY, shouldExtendTillTheBottom, flowers, underair);
    }
    #elif MC_VER >= MC_1_18_2
    @WrapOperation(method = "loadPixel", at = @At(value = "INVOKE", target = "Lxaero/map/MapWriter;loadPixelHelp(Lxaero/map/region/MapBlock;Lxaero/map/region/MapBlock;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;BBLnet/minecraft/world/level/chunk/LevelChunk;IIIZZLnet/minecraft/world/level/material/FluidState;Lnet/minecraft/core/Registry;IZZZ)Z"))
    private boolean fakeOverlayLight(MapWriter instance, MapBlock pixel, MapBlock currentPixel, Level world, BlockState state, byte light, byte skyLight, LevelChunk bchunk, int insideX, int insideZ, int h, boolean canReuseBiomeColours, boolean cave, FluidState fluidFluidState, Registry<Biome> biomeRegistry, int transparentSkipY, boolean shouldExtendTillTheBottom, boolean flowers, boolean underair, Operation<Boolean> original) {
        if (ProgressCounter.readyForRender.get() && bchunk instanceof FakeChunk) {
            try {
                light = (byte) ((FakeChunk)bchunk).lightAtPos(insideX, insideZ);
            } catch (Exception ignored) {}
        }
        return original.call(instance, pixel, currentPixel, world, state, light, skyLight, bchunk, insideX, insideZ, h, canReuseBiomeColours, cave, fluidFluidState, biomeRegistry, transparentSkipY, shouldExtendTillTheBottom, flowers, underair);
    }
    #else
    @WrapOperation(method = "loadPixel", at = @At(value = "INVOKE", target = "Lxaero/map/MapWriter;loadPixelHelp(Lxaero/map/region/MapBlock;Lxaero/map/region/MapBlock;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;BBLnet/minecraft/world/level/chunk/LevelChunk;IIIZZLnet/minecraft/world/level/material/FluidState;Lnet/minecraft/core/WritableRegistry;IZZZ)Z"))
    private boolean fakeOverlayLight(MapWriter instance, MapBlock pixel, MapBlock currentPixel, Level world, BlockState state, byte light, byte skyLight, LevelChunk bchunk, int insideX, int insideZ, int h, boolean canReuseBiomeColours, boolean cave, FluidState fluidFluidState, WritableRegistry<Biome> biomeRegistry, int transparentSkipY, boolean shouldExtendTillTheBottom, boolean flowers, boolean underair, Operation<Boolean> original) {
        if (ProgressCounter.readyForRender.get() && bchunk instanceof FakeChunk) {
            try {
                light = (byte) ((FakeChunk)bchunk).lightAtPos(insideX, insideZ);
            } catch (Exception ignored) {}
        }
        return original.call(instance, pixel, currentPixel, world, state, light, skyLight, bchunk, insideX, insideZ, h, canReuseBiomeColours, cave, fluidFluidState, biomeRegistry, transparentSkipY, shouldExtendTillTheBottom, flowers, underair);
    }
    #endif

    @WrapOperation(method = "onRender", at = @At(value = "FIELD", target = "Lxaero/map/MapProcessor;mainPlayerX:D", opcode = Opcodes.GETFIELD))
    private double fakePlayerLocationX(MapProcessor instance, Operation<Double> original) {
        if (ProgressCounter.readyForRender.get()) return TileConverter.fakePlayerLocationX;
        return original.call(instance);
    }

    @WrapOperation(method = "onRender", at = @At(value = "FIELD", target = "Lxaero/map/MapProcessor;mainPlayerZ:D", opcode = Opcodes.GETFIELD))
    private double fakePlayerLocationZ(MapProcessor instance, Operation<Double> original) {
        if (ProgressCounter.readyForRender.get()) return TileConverter.fakePlayerLocationZ;
        return original.call(instance);
    }
}
