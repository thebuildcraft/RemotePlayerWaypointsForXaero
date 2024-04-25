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


package de.the_build_craft.remote_player_waypoints_for_xaero;

import java.util.List;

public abstract class CommonModConfig {
    public CommonModConfig() {
        Instance = this;
    }
    public static CommonModConfig Instance;

    public abstract boolean enabled();
    public abstract int updateDelay();
    public abstract int minDistance();
    public abstract int maxDistance();
    public abstract int defaultY();
    public abstract int timeUntilAfk();
    public abstract int unknownAfkStateColor();
    public abstract int AfkColor();
    public abstract boolean showAfkTimeInTabList();
    public abstract boolean debugMode();
    public abstract List<String> ignoredServers();
    public abstract List<ServerEntry> serverEntries();

    public static class ServerEntry {
        public Maptype maptype;
        public String ip;
        public String link;

        public ServerEntry() {
            this("", "", Maptype.Dynmap);
        }

        public ServerEntry(String ip, String link, Maptype maptype) {
            this.ip = ip;
            this.link = link;
            this.maptype = maptype;
        }

        public enum Maptype {
            Dynmap,
            Squaremap,
            Bluemap;

            Maptype() {
            }
        }
    }
}
