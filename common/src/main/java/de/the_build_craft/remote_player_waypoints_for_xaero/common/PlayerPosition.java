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

/**
 * A player's auth profile and position
 *
 * @author ewpratten
 * @author Leander Knüttel
 * @version 14.06.2024
 */
public class PlayerPosition {
    public final String player;
    public final int x;
    public final int y;
    public final int z;
    public final String world;

    public PlayerPosition(String username, int x, int y, int z, String world) {
        this.player = username;
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
    }

    public boolean CompareCords(PlayerPosition otherPosition){
        return (x == otherPosition.x) && (y == otherPosition.y) && (z == otherPosition.z);
    }
}