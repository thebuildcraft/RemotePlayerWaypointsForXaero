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

package de.the_build_craft.remote_player_waypoints_for_xaero.common.mapUpdates;

/**
 * JSON object from dynmap API. Send in update requests
 *
 * @author ewpratten
 * @author Leander Knüttel
 * @author eatmyvenom
 * @version 26.06.2025
 */
public class DynmapPlayerUpdate {
    public static class Player {
        public String account;
        public String world;
        public float x;
        public float y;
        public float z;
    }

    public Player[] players = new Player[0];
}