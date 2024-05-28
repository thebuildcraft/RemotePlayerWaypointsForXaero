/*      Remote player waypoints for Xaero's Map
        Copyright (C) 2024  Leander Knüttel

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


package de.the_build_craft.remote_player_waypoints_for_xaero.mapUpdates;

import java.util.Map;


public class BlueMapMarkerSet {
    public static class Marker {
        public String type;
        public String label;
        public Position position;
        public int sorting;
        public boolean listed;
    }
    public static class Position {
        public float x;
        public float y;
        public float z;
    }
    public String label;
    public boolean toggleable;
    public boolean defaultHidden;
    public int sorting;
    public Map<String, Marker> markers;
}