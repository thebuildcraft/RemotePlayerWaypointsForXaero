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

import de.the_build_craft.maplink.common.AbstractModInitializer;
import de.the_build_craft.maplink.common.waypoints.Color;
import net.minecraft.client.Minecraft;
#if MC_VER >= MC_26_1_0
import net.minecraft.client.multiplayer.ClientLevel;
#endif
import net.minecraft.core.BlockPos;
#if MC_VER <= MC_1_21_1 && MC_VER > MC_1_19_2
import net.minecraft.core.HolderLookup;
#endif
import net.minecraft.core.Registry;
#if MC_VER > MC_1_19_2
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
#else
import net.minecraft.data.BuiltinRegistries;
#endif
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import xaero.lib.client.level.block.BlockTextureColorUtils;
import xaero.lib.platform.ClientOnlyServices;
import xaero.map.MapProcessor;
import xaero.map.MapWriter;
import xaero.map.WorldMapSession;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
#if MC_VER > MC_1_21_1 || MC_VER <= MC_1_19_2
import java.util.Map;
#endif

/**
 * @author Leander Knüttel
 * @version 06.08.2026
 */
public class BlockCache {
    private static List<FakeBlock> idToBlock;
    private static ConcurrentHashMap<Integer, Short> rgbToId;
    private static KDTree okLabToIdTree;
    private static short currId;

    static void clear() {
        idToBlock = new ArrayList<>();
        rgbToId = new ConcurrentHashMap<>();
        okLabToIdTree = new KDTree();
        currId = Short.MIN_VALUE;
    }

    private static Iterable<Block> getBlockRegistry() {
        #if MC_VER > MC_1_19_2
        return BuiltInRegistries.BLOCK;
        #else
        return Registry.BLOCK;
        #endif
    }

    static void cacheBlockColors() {
        BlockTextureColorUtils colorUtils = ClientOnlyServices.PLATFORM.getBlockUtils().getTextureColorUtils();

        BlockPos zeroPos = BlockPos.ZERO;
        #if MC_VER >= MC_26_1_0
        ClientLevel level = Minecraft.getInstance().level;
        #else
        Level level = Minecraft.getInstance().level;
        #endif

        WorldMapSession worldMapSession = WorldMapSession.getCurrentSession();
        MapProcessor mapProcessor = worldMapSession.getMapProcessor();
        MapWriter mapWriter = mapProcessor.getMapWriter();
        #if MC_VER > MC_1_21_1
        Registry<Biome> biomeRegistry = level.registryAccess().lookupOrThrow(Registries.BIOME);
        Biome plains = biomeRegistry.getValueOrThrow(Biomes.PLAINS);
        #elif MC_VER > MC_1_19_2
        HolderLookup.RegistryLookup<Biome> biomeRegistry = level.registryAccess().lookupOrThrow(Registries.BIOME);
        Biome plains = biomeRegistry.getOrThrow(Biomes.PLAINS).value();
        #else
        Registry<Biome> biomeRegistry = level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
        Biome plains = biomeRegistry.getOrThrow(Biomes.PLAINS);
        #endif

        List<BlockState> stainedGlassBlocks = new ArrayList<>(17);
        List<Integer> stainedGlassColors = new ArrayList<>(17);
        stainedGlassBlocks.add(null);
        stainedGlassColors.add(0);
        for (Block block : getBlockRegistry()) {
            if (!(block instanceof StainedGlassBlock)) continue;
            BlockState defaultState = block.defaultBlockState();
            stainedGlassBlocks.add(defaultState);
            #if MC_VER > MC_1_19_2
            stainedGlassColors.add(colorUtils.getBlockTextureColor(defaultState, true, level, BuiltInRegistries.BLOCK, zeroPos));
            #else
            stainedGlassColors.add(colorUtils.getBlockTextureColor(defaultState, true, level, zeroPos));
            #endif
        }

        BlockState[] extraGlass = new BlockState[]{
                null,
                #if MC_VER >= MC_26_2_0
                Blocks.STAINED_GLASS.white().defaultBlockState(),
                Blocks.STAINED_GLASS.lightGray().defaultBlockState(),
                Blocks.STAINED_GLASS.gray().defaultBlockState(),
                Blocks.STAINED_GLASS.black().defaultBlockState()
                #else
                Blocks.WHITE_STAINED_GLASS.defaultBlockState(),
                Blocks.LIGHT_GRAY_STAINED_GLASS.defaultBlockState(),
                Blocks.GRAY_STAINED_GLASS.defaultBlockState(),
                Blocks.BLACK_STAINED_GLASS.defaultBlockState()
                #endif
        };
        int[] extraGlassColors = new int[extraGlass.length];
        for (int i = 1; i < extraGlass.length; i++) {
            #if MC_VER > MC_1_19_2
            extraGlassColors[i] = colorUtils.getBlockTextureColor(extraGlass[i], true, level, BuiltInRegistries.BLOCK, zeroPos);
            #else
            extraGlassColors[i] = colorUtils.getBlockTextureColor(extraGlass[i], true, level, zeroPos);
            #endif
        }

        boolean useBiomes = false;

        for (Block block : getBlockRegistry()) {
            if (block instanceof LeavesBlock || isTransparent(block)) continue;

            BlockState defaultState = block.defaultBlockState();
            #if MC_VER > MC_1_21_1
            if (!defaultState.isCollisionShapeFullBlock(level, zeroPos) || !defaultState.getFluidState().isEmpty() || defaultState.getLightEmission() >= 0.5 || !defaultState.isSolidRender()) continue;
            #else
            if (!defaultState.isCollisionShapeFullBlock(level, zeroPos) || !defaultState.getFluidState().isEmpty() || defaultState.getLightEmission() >= 0.5 || !defaultState.isSolidRender(level, zeroPos)) continue;
            #endif

            #if MC_VER > MC_1_19_2
            int argb = colorUtils.getBlockTextureColor(defaultState, true, level, BuiltInRegistries.BLOCK, zeroPos);
            #else
            int argb = colorUtils.getBlockTextureColor(defaultState, true, level, zeroPos);
            #endif
            if (new Color(argb).a < 1) continue;

            for (int extra = 0; extra < extraGlass.length; extra++) {
                for (int i = 0; i < stainedGlassBlocks.size(); i++) {
                    if (extraGlass[extra] != null && stainedGlassBlocks.get(i) != null && extraGlass[extra].getBlock() == stainedGlassBlocks.get(i).getBlock()) continue;
                    if (useBiomes && block instanceof GrassBlock) {
                        #if MC_VER > MC_1_21_1
                        for (Map.Entry<ResourceKey<Biome>, Biome> entry : biomeRegistry.entrySet()) {
                            ResourceKey<Biome> key = entry.getKey();
                            Biome biome = entry.getValue();
                            int tint = biome.getGrassColor(0, 0);
                            addColor(new FakeBlock(defaultState, new BlockState[]{extraGlass[extra], stainedGlassBlocks.get(i)}, key), getTintedColor(argb, tint, new int[]{extraGlassColors[extra], stainedGlassColors.get(i)}));
                        }
                        #elif MC_VER > MC_1_19_2
                        int finalExtra = extra;
                        int finalI = i;
                        biomeRegistry.listElements().forEach(entry -> {
                            ResourceKey<Biome> key = entry.key();
                            Biome biome = entry.value();
                            int tint = biome.getGrassColor(0, 0);
                            addColor(new FakeBlock(defaultState, new BlockState[]{extraGlass[finalExtra], stainedGlassBlocks.get(finalI)}, key), getTintedColor(argb, tint, new int[]{extraGlassColors[finalExtra], stainedGlassColors.get(finalI)}));
                        });
                        #else
                        for (Map.Entry<ResourceKey<Biome>, Biome> entry : biomeRegistry.entrySet()) {
                            ResourceKey<Biome> key = entry.getKey();
                            Biome biome = entry.getValue();
                            int tint = biome.getGrassColor(0, 0);
                            addColor(new FakeBlock(defaultState, new BlockState[]{extraGlass[extra], stainedGlassBlocks.get(i)}, key), getTintedColor(argb, tint, new int[]{extraGlassColors[extra], stainedGlassColors.get(i)}));
                        }
                        #endif
                    } else {
                        int tint = -1;
                        if (block instanceof GrassBlock) tint = plains.getGrassColor(0, 0);
                        addColor(new FakeBlock(defaultState, new BlockState[]{extraGlass[extra], stainedGlassBlocks.get(i)}, Biomes.PLAINS), getTintedColor(argb, tint, new int[]{extraGlassColors[extra], stainedGlassColors.get(i)}));
                    }
                }
            }
        }
    }

    private static void addColor(FakeBlock fakeBlock, Color color) {
        int argb = color.getAsARGB();
        if (rgbToId.containsKey(argb)) return;
        if (currId == Short.MAX_VALUE) {
            AbstractModInitializer.LOGGER.warn("Maximum color id reached!");
            return;
        }
        rgbToId.put(argb, currId);
        okLabToIdTree.insert(OKLab.sRgbToOkLab(color), currId);
        idToBlock.add(fakeBlock);
        currId++;
    }

    private static Color getTintedColor(int baseArgb, int biomeColor, int[] overlayArgbs) {
        float rMult = ((baseArgb >> 16) & 255) / 255.0f;
        float gMult = ((baseArgb >> 8) & 255) / 255.0f;
        float bMult = (baseArgb & 255) / 255.0f;

        int br = (biomeColor >> 16) & 255;
        int bg = (biomeColor >> 8) & 255;
        int bb = biomeColor & 255;

        float r = br * rMult;
        float g = bg * gMult;
        float b = bb * bMult;

        float t = 127.0f / 255.0f;

        float currentTransMult = 1.0f;
        float overlayR = 0, overlayG = 0, overlayB = 0;

        // index 0 is the highest block
        for (int overlayColor : overlayArgbs) {
            if (overlayColor == 0) continue;

            float oR = (overlayColor >> 16) & 255;
            float oG = (overlayColor >> 8) & 255;
            float oB = overlayColor & 255;

            float intensity = t * currentTransMult;

            overlayR += oR * intensity;
            overlayG += oG * intensity;
            overlayB += oB * intensity;

            currentTransMult *= (1.0f - t);
        }

        int finalR = (int)(r * currentTransMult + overlayR);
        int finalG = (int)(g * currentTransMult + overlayG);
        int finalB = (int)(b * currentTransMult + overlayB);

        return new Color(finalR, finalG, finalB, 1);
    }

    private static boolean isTransparent(Block block) {
        return block instanceof HalfTransparentBlock || block instanceof LiquidBlock || block instanceof StainedGlassPaneBlock || block instanceof AirBlock || block instanceof BarrierBlock;
    }

    static BlockState getBlockState(short id, int y) {
        FakeBlock fakeBlock = getFakeBlock(id);
        BlockState temp = null;
        if (y <= 0) temp = fakeBlock.bottom;
        if (y > 0 && y <= fakeBlock.top.length) temp = fakeBlock.top[fakeBlock.top.length - y];
        return temp != null ? temp : Blocks.AIR.defaultBlockState();
    }

    static FakeBlock getFakeBlock(short id) {
        return idToBlock.get(id - Short.MIN_VALUE);
    }

    static short convertColorToBlockId(int pixelRgb) {
        return rgbToId.computeIfAbsent(pixelRgb, rgb -> okLabToIdTree.searchNearest(OKLab.sRgbToOkLab(new Color(rgb))));
    }
}
