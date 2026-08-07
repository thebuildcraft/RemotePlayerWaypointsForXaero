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

package de.the_build_craft.maplink.mixins.common.mods.xaeroworldmap;

import de.the_build_craft.maplink.common.clientMapHandlers.AreaMarkerHighlighter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.highlight.AbstractHighlighter;
import xaero.map.highlight.HighlighterRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Leander Knüttel
 * @version 06.08.2026
 */
@Pseudo
@Mixin(HighlighterRegistry.class)
public class HighlighterRegistryMixin {
    @Unique
    private static final AreaMarkerHighlighter maplink$areaMarkerHighlighter = new AreaMarkerHighlighter();

    @Shadow
    private List<AbstractHighlighter> highlighters;

    @Inject(method = "getHighlighters", at = @At("RETURN"), cancellable = true, remap = false)
    private void injected(CallbackInfoReturnable<List<AbstractHighlighter>> cir) {
        if (highlighters.contains(maplink$areaMarkerHighlighter)) return;
        List<AbstractHighlighter> newList = new ArrayList<>(highlighters);
        newList.add(maplink$areaMarkerHighlighter);
        highlighters = newList;
        cir.setReturnValue(newList);
    }
}
