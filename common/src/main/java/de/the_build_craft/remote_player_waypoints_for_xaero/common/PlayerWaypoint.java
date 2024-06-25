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

/**
 * A wrapper to improve creating temp waypoints for players
 *
 * @author ewpratten
 * @author eatmyvenom
 * @author Leander Knüttel
 * @version 25.06.2024
 */
public class PlayerWaypoint extends Waypoint {
    public PlayerWaypoint(PlayerPosition player) {
        this(player.x, player.y, player.z, player.player);
    }

    public PlayerWaypoint(int x, int y, int z, String name) {
        super(x, y, z, name, getAbbreviation(name),
                CommonModConfig.Instance.playerWaypointColor(), 0, true);
    }

    public static String getDisplayName(String name){
        if (name.startsWith("<")) {
            int i = name.indexOf(">");
            if (i != (name.length() - 1)) {
                int j = name.indexOf("<", i);
                name = name.substring(i + 1, j);
            }
        }
        return name;
    }

    public static String getAbbreviation(String name){
        StringBuilder abbreviation = new StringBuilder();
        String[] words = name.split("[ _\\-,:;.]");
        for (String word : words) {
            if (word.isEmpty()) continue;
            abbreviation.append(word.substring(0, 1).toUpperCase());
        }
        return abbreviation.toString();
    }
}