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
import net.minecraft.client.gui.screens.Screen;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

import java.util.ArrayList;

/**
 * @author Leander Knüttel
 * @version 06.08.2026
 */
public class DimensionSelectionRightClickableElement implements IRightClickableElement {
    private final AreaSelection areaSelection;

    public DimensionSelectionRightClickableElement(AreaSelection areaSelection) {
        this.areaSelection = areaSelection;
    }

    @Override
    public ArrayList<RightClickOption> getRightClickOptions() {
        ArrayList<RightClickOption> options = new ArrayList<>();
        if (AbstractModInitializer.getConnection() == null) return options;
        long requiredBytes = (long)areaSelection.chunksX * areaSelection.chunksZ * 16 * 16 * 4;
        long availableBytes = Runtime.getRuntime().maxMemory() - (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        if (requiredBytes > availableBytes) {
            System.gc();
            availableBytes = Runtime.getRuntime().maxMemory() - (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        }
        String memWarning = "";
        if (requiredBytes > availableBytes || requiredBytes > Runtime.getRuntime().maxMemory() * 0.8f) memWarning = " (⚠ Low Memory ⚠)";
        options.add(new RightClickOption(String.format("Memory required: ~ %.2f GB" + memWarning, requiredBytes * 1e-9f), 0, this) {
            @Override
            public void onAction(Screen screen) {}
        });
        options.add(new RightClickOption("Select a map to continue:", 0, this) {
            @Override
            public void onAction(Screen screen) {}
        });
        for (String[] map : AbstractModInitializer.getConnection().getPossibleTileMaps()) {
            options.add(new RightClickOption(map[1], 0, this) {
                @Override
                public void onAction(Screen screen) {
                    new Thread(() -> {
                        AbstractModInitializer.getConnection().downloadTiles(map[0], areaSelection);
                    }).start();
                }
            });
        }
        return options;
    }

    @Override
    public boolean isRightClickValid() {
        return true;
    }

    @Override
    public int getRightClickTitleBackgroundColor() {
        return -10461088;
    }
}
