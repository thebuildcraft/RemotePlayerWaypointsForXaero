/*
 *    This file is part of the Map Link mod
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2025 - 2026  Leander Knüttel and contributors
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

package de.the_build_craft.maplink.common.clientMapHandlers;

import de.the_build_craft.maplink.common.AbstractModInitializer;
import de.the_build_craft.maplink.common.waypoints.AreaMarker;
import de.the_build_craft.maplink.common.waypoints.Color;
import de.the_build_craft.maplink.common.waypoints.MathUtils;
import de.the_build_craft.maplink.common.wrappers.Text;
import net.minecraft.client.Minecraft;
#if MC_VER >= MC_1_19_4
import net.minecraft.core.registries.Registries;
#else
import net.minecraft.core.Registry;
#endif
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import xaero.map.WorldMapSession;
import xaero.map.highlight.ChunkHighlighter;
import xaero.map.world.MapDimension;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.the_build_craft.maplink.common.CommonModConfig.*;

/**
 * @author Leander Knüttel
 * @version 06.08.2026
 */
public class AreaMarkerHighlighter extends ChunkHighlighter implements MapHighlightClearer {
    public AreaMarkerHighlighter() {
        super(true);
        XaeroClientMapHandler.mapHighlightClearer = this;
    }

    @Override
    public void clearHashCache() {
        if (Minecraft.getInstance().level == null) return;
        try {
            MapDimension mapDim = WorldMapSession.getCurrentSession().getMapProcessor().getMapWorld()
                    #if MC_VER >= MC_1_21_11
                    .getDimension(Minecraft.getInstance().level.dimension());
                    #elif MC_VER >= MC_1_19_4
                    .getDimension(ResourceKey.create(Registries.DIMENSION, Minecraft.getInstance().level.dimension().location()));
                    #else
                    .getDimension(ResourceKey.create(Registry.DIMENSION_REGISTRY, Minecraft.getInstance().level.dimension().location()));
                    #endif
            if (mapDim != null) mapDim.getHighlightHandler().clearCachedHashes();
        } catch (NullPointerException ignored) {
        } catch (Exception e) {
            AbstractModInitializer.LOGGER.error("Could not clear Xaero Highlight Hash!", e);
        }
    }

    private List<Set<AreaMarker>> getDirections(int chunkX, int chunkZ) {
        if (XaeroClientMapHandler.currentlyRasterising) return null;
        Set<AreaMarker> areaMarkerSet = XaeroClientMapHandler.chunkHighlightMap.get(MathUtils.combineIntsToLong(chunkX, chunkZ));
        if (areaMarkerSet == null) return null;

        Set<AreaMarker> top = XaeroClientMapHandler.chunkHighlightMap
                .getOrDefault(MathUtils.combineIntsToLong(chunkX, chunkZ - 1), Collections.emptySet());
        Set<AreaMarker> bottom = XaeroClientMapHandler.chunkHighlightMap
                .getOrDefault(MathUtils.combineIntsToLong(chunkX, chunkZ + 1), Collections.emptySet());
        Set<AreaMarker> left = XaeroClientMapHandler.chunkHighlightMap
                .getOrDefault(MathUtils.combineIntsToLong(chunkX - 1, chunkZ), Collections.emptySet());
        Set<AreaMarker> right = XaeroClientMapHandler.chunkHighlightMap
                .getOrDefault(MathUtils.combineIntsToLong(chunkX + 1, chunkZ), Collections.emptySet());

        return Arrays.asList(areaMarkerSet, top, right, bottom, left);
    }

    @Override
    protected int[] getColors(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        List<Set<AreaMarker>> directions = getDirections(chunkX, chunkZ);
        if (directions == null) return null;
        Set<AreaMarker> areaMarkerSet = directions.get(0);

        int fillColor = Color.combineColors(
                areaMarkerSet.stream().map(a -> a.fillColor).collect(Collectors.toList()),
                config.general.areaFillAlphaMul / 100f,
                config.general.areaFillAlphaMin / 100f,
                config.general.areaFillAlphaMax / 100f).getAsBGRA();

        int[] colors = new int[5];
        colors[0] = fillColor;
        for (int i = 1; i < directions.size(); i++) {
            Set<AreaMarker> direction = directions.get(i);
            List<Color> lineColors = areaMarkerSet.stream().filter(a -> !direction.contains(a))
                    .map(a -> a.lineColor).collect(Collectors.toList());
            if (lineColors.isEmpty()) {
                colors[i] = fillColor;
            } else {
                colors[i] = Color.combineColors(
                        lineColors,
                        config.general.areaLineAlphaMul / 100f,
                        config.general.areaLineAlphaMin / 100f,
                        config.general.areaLineAlphaMax / 100f).getAsBGRA();
            }
        }
        return colors;
    }

    public Component getChunkHighlightTooltip(int chunkX, int chunkZ, Function<AreaMarker, String> toName) {
        List<Set<AreaMarker>> directions = getDirections(chunkX, chunkZ);
        if (directions == null) return null;
        Set<AreaMarker> areaMarkerSet = directions.get(0);

        Set<String> names = new TreeSet<>();
        for (int i = 1; i < directions.size(); i++) {
            Set<AreaMarker> direction = directions.get(i);
            areaMarkerSet.stream().filter(a -> !direction.contains(a)).map(toName).forEach(names::add);
        }

        for (AreaMarker areaMarker : areaMarkerSet) {
            if (Mth.clamp(areaMarker.fillColor.a * (config.general.areaFillAlphaMul / 100f),
                    config.general.areaFillAlphaMin / 100f,
                    config.general.areaFillAlphaMax / 100f) > 0) {
                names.add(toName.apply(areaMarker));
            }
        }

        if (names.isEmpty()) return null;
        return Text.literal(String.join(" | ", names));
    }

    @Override
    public Component getChunkHighlightSubtleTooltip(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        return getChunkHighlightTooltip(chunkX, chunkZ, areaMarker -> areaMarker.layer.name);
    }

    @Override
    public Component getChunkHighlightBluntTooltip(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        return getChunkHighlightTooltip(chunkX, chunkZ, areaMarker -> areaMarker.name);
    }

    @Override
    public int calculateRegionHash(ResourceKey<Level> dimension, int regionX, int regionZ) {
        return 1 + XaeroClientMapHandler.chunkHighlightHash;
    }

    @Override
    public boolean regionHasHighlights(ResourceKey<Level> dimension, int regionX, int regionZ) {
        if (XaeroClientMapHandler.currentlyRasterising) return false;
        return XaeroClientMapHandler.regionsWithChunkHighlights.contains(MathUtils.combineIntsToLong(regionX, regionZ));
    }

    @Override
    public boolean chunkIsHighlit(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        List<Set<AreaMarker>> directions = getDirections(chunkX, chunkZ);
        if (directions == null) return false;
        Set<AreaMarker> areaMarkerSet = directions.get(0);

        for (AreaMarker areaMarker : areaMarkerSet) {
            if (Mth.clamp(areaMarker.fillColor.a * (config.general.areaFillAlphaMul / 100f),
                    config.general.areaFillAlphaMin / 100f,
                    config.general.areaFillAlphaMax / 100f) > 0) {
                return true;
            }
        }

        for (int i = 1; i < directions.size(); i++) {
            Set<AreaMarker> direction = directions.get(i);
            if (areaMarkerSet.stream().anyMatch(a -> !direction.contains(a))) return true;
        }

        return false;
    }

    @Override
    public void addMinimapBlockHighlightTooltips(List<Component> list, ResourceKey<Level> dimension, int blockX, int blockZ, int width) {
    }
}
