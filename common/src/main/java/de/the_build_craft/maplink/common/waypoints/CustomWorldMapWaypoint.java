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

package de.the_build_craft.maplink.common.waypoints;

import de.the_build_craft.maplink.common.clientMapHandlers.ClientMapHandler;
#if MC_VER >= MC_1_21_11
import net.minecraft.resources.Identifier;
#else
import net.minecraft.resources.ResourceLocation;
#endif
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.map.mods.gui.Waypoint;

import static de.the_build_craft.maplink.common.CommonModConfig.config;
import static de.the_build_craft.maplink.common.CommonModConfig.getPlayerWaypointColor;

/**
 * @author Leander Knüttel
 * @version 06.08.2026
 */
public class CustomWorldMapWaypoint extends Waypoint {
    public static final int[] XAERO_COLORS = new int[]{-16777216, -16777046, -16733696, -16733526, -5636096, -5635926, -22016, -5592406, -11184811, -11184641, -11141291, -11141121, -65536, -43521, -171, -1};
    public final String id;
    private final String name;
    public int color;
    public int x;
    public int y;
    public int z;
    private WaypointState waypointState;

    public CustomWorldMapWaypoint(Position p, WaypointState waypointState) {
        super(new Object(), false, ClientMapHandler.waypointPrefix, 1);
        this.id = p.id;
        this.name = p.name;
        this.color = waypointState.isPlayer ? XAERO_COLORS[getPlayerWaypointColor(name)] : XAERO_COLORS[config.general.markerWaypointColor.ordinal()];
        this.x = (int) Math.floor(p.pos.x);
        this.y = (int) Math.floor(p.pos.y);
        this.z = (int) Math.floor(p.pos.z);
        this.waypointState = waypointState;
    }

    public WaypointState getWaypointState() {
        if (waypointState.isOld) waypointState = ClientMapHandler.getWaypointState(id);
        return waypointState;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getZ() {
        return z;
    }

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public int getColor() {
        return color;
    }

    @Override
    public boolean isGlobal() {
        return false;
    }

    @Override
    public boolean isTemporary() {
        return true;
    }

    @Override
    public boolean isDisabled() {
        return false;
    }

    @Override
    public WaypointPurpose getPurpose() {
        return WaypointPurpose.NORMAL;
    }

    @Override
    public int getYaw() {
        return 0;
    }

    @Override
    public boolean isRotation() {
        return false;
    }

    @Override
    public String getSymbol() {
        return waypointState.abbreviation;
    }

    @Override
    public boolean isyIncluded() {
        return true;
    }

    #if MC_VER >= MC_1_21_11
    @Override
    public Identifier getThirdPartyOrigin() {
        return null;
    }
    #else
    @Override
    public ResourceLocation getThirdPartyOrigin() {
        return null;
    }
    #endif

    @Override
    public boolean isThirdParty() {
        return false;
    }

    @Override
    public boolean isThirdPartyDeleted() {
        return false;
    }
}
