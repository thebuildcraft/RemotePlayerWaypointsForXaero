/*
 *    This file is part of the Remote player waypoints for Xaero's Map mod
 *    licensed under the GNU GPL v3 License.
 *    (some parts of this file are originally from "RemotePlayers" by ewpratten)
 *
 *    Copyright (C) 2024  Leander Knüttel
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

package de.the_build_craft.remote_player_waypoints_for_xaero.common;

import xaero.common.minimap.waypoints.Waypoint;
#if MC_VER != MC_1_17_1
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
#endif

/**
 * A wrapper to improve creating temp waypoints for markers
 *
 * @author ewpratten
 * @author eatmyvenom
 * @author Leander Knüttel
 * @version 29.06.2025
 */
public class FixedWaypoint extends Waypoint {
    public FixedWaypoint(WaypointPosition wp) {
        this(wp.x, wp.y, wp.z, wp.name);
    }

    public FixedWaypoint(int x, int y, int z, String name) {
        super(x, y, z, name, getAbbreviation(name),
                #if MC_VER == MC_1_17_1
                CommonModConfig.Instance.markerWaypointColor(), 0, true);
                #else
                WaypointColor.fromIndex(CommonModConfig.Instance.markerWaypointColor()), WaypointPurpose.NORMAL, true);
                #endif
    }

    public static String getAbbreviation(String name){
        return PlayerWaypoint.getAbbreviation(name);
    }
}