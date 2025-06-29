/*
 *    This file is part of the Remote player waypoints for Xaero's Map mod
 *    licensed under the GNU GPL v3 License.
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
 * @author Leander Knüttel
 * @version 28.06.2025
 */
public class Pl3xMapPlayerUpdate {
    public static class Player {
        public String name;
        public String world;
        public Position position;
        public static class Position{
            public int x;
            public int z;
        }
    }

    public static class WorldSetting {
        public String name;
    }

    public Player[] players = new Player[0];
    public WorldSetting[] worldSettings = new WorldSetting[0];
}
