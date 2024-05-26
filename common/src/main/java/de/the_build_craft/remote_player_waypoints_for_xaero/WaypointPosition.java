/*      Remote player waypoints for Xaero's Map
        Copyright (C) 2024  Leander Knüttel
        (some parts of this file are originally from "RemotePlayers" by ewpratten)

        This program is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License
        along with this program.  If not, see <https://www.gnu.org/licenses/>.*/

package de.the_build_craft.remote_player_waypoints_for_xaero;

/**
 * A player's auth profile and position
 */
public class WaypointPosition {
    public final String name;
    public final int x;
    public final int y;
    public final int z;
    public final String world;

    public WaypointPosition(String name, int x, int y, int z, String world) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
    }

    public boolean CompareCords(WaypointPosition otherPosition){
        return (this.x == otherPosition.x) && (this.y == otherPosition.y) && (this.z == otherPosition.z);
    }
}