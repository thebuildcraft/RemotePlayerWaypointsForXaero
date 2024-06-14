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

package de.the_build_craft.remote_player_waypoints_for_xaero.common;

/**
 * @author Leander Knüttel
 * @version 26.05.2024
 */
public enum LoaderType {
    Fabric{
        @Override
        boolean isFabricLike() { return true; }

        @Override
        boolean isForgeLike() { return false; }

        @Override
        boolean isBukkitFork() { return false; }
    },
    Quilt{
        @Override
        boolean isFabricLike() { return true; }

        @Override
        boolean isForgeLike() { return false; }

        @Override
        boolean isBukkitFork() { return false; }
    },
    Forge{
        @Override
        boolean isFabricLike() { return false; }

        @Override
        boolean isForgeLike() { return true; }

        @Override
        boolean isBukkitFork() { return false; }
    },
    NeoForge{
        @Override
        boolean isFabricLike() { return false; }

        @Override
        boolean isForgeLike() { return true; }

        @Override
        boolean isBukkitFork() { return false; }
    },
    Bukkit{
        @Override
        boolean isFabricLike() { return false; }

        @Override
        boolean isForgeLike() { return false; }

        @Override
        boolean isBukkitFork() { return true; }
    },
    Spigot{
        @Override
        boolean isFabricLike() { return false; }

        @Override
        boolean isForgeLike() { return false; }

        @Override
        boolean isBukkitFork() { return true; }
    },
    Paper{
        @Override
        boolean isFabricLike() { return false; }

        @Override
        boolean isForgeLike() { return false; }

        @Override
        boolean isBukkitFork() { return true; }
    },
    Folia{
        @Override
        boolean isFabricLike() { return false; }

        @Override
        boolean isForgeLike() { return false; }

        @Override
        boolean isBukkitFork() { return true; }
    },
    Sponge{
        @Override
        boolean isFabricLike() { return false; }

        @Override
        boolean isForgeLike() { return false; }

        @Override
        boolean isBukkitFork() { return false; }
    },
    Purpur{
        @Override
        boolean isFabricLike() { return false; }

        @Override
        boolean isForgeLike() { return false; }

        @Override
        boolean isBukkitFork() { return true; }
    };
    LoaderType(){
    }
    abstract boolean isFabricLike();
    abstract boolean isForgeLike();
    abstract boolean isBukkitFork();
}
