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
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.WaypointRenderInfo;
import xaero.hud.minimap.waypoint.WaypointVisibilityType;
import xaero.map.mods.gui.Waypoint;

import static de.the_build_craft.maplink.common.CommonModConfig.config;
import static de.the_build_craft.maplink.common.CommonModConfig.getPlayerWaypointColor;

/**
 * @author Leander Knüttel
 * @version 15.02.2026
 */
public class CustomWorldMapWaypoint extends Waypoint {
    public static final int[] XAERO_COLORS = new int[]{-16777216, -16777046, -16733696, -16733526, -5636096, -5635926, -22016, -5592406, -11184811, -11184641, -11141291, -11141121, -65536, -43521, -171, -1};
    public final String id;
    private WaypointState waypointState;

    public CustomWorldMapWaypoint(Position p, WaypointState waypointState) {
        super(
                new xaero.common.minimap.waypoints.Waypoint(
                        (int) Math.floor(p.pos.x),
                        (int) Math.floor(p.pos.y),
                        (int) Math.floor(p.pos.z),
                        p.name,
                        p.id,
                        WaypointColor.fromIndex(waypointState.isPlayer ? getPlayerWaypointColor(p.name) : config.general.markerWaypointColor.ordinal()),
                        WaypointPurpose.NORMAL,
                        true,
                        true
                ),
                false,
                ClientMapHandler.waypointPrefix,
                1
        );
        this.id = p.id;
        this.waypointState = waypointState;
    }

    public WaypointState getWaypointState() {
        if (waypointState.isOld) waypointState = ClientMapHandler.getWaypointState(id);
        return waypointState;
    }
}
