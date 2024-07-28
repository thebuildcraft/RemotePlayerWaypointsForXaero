/*
 *    This file is part of the Remote player waypoints for Xaero's Map mod
 *    licensed under the GNU GPL v3 License.
 *    (some parts of this file are originally from "RemotePlayers" by TheMrEngMan)
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

package de.the_build_craft.remote_player_waypoints_for_xaero.mixins.neoforge.mods.xaerominimap;

import de.the_build_craft.remote_player_waypoints_for_xaero.common.AbstractModInitializer;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.CommonModConfig;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.PlayerPosition;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.UpdateTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xaero.common.minimap.radar.MinimapRadar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author TheMrEngMan
 * @author Leander Knüttel
 * @version 28.07.2024
 */

@Pseudo
@Mixin(MinimapRadar.class)
public class MinimapRadarMixin {

    @ModifyVariable(method = "updateRadar", at = @At("STORE"), ordinal = 0)
    private Iterable<Entity> updateRadarEntities(Iterable<Entity> worldEntities) {
        // Don't render if feature not enabled
        if(!CommonModConfig.Instance.enableEntityRadar()) return worldEntities;
        // Don't render if there is no remote players available
        if(UpdateTask.playerPositions == null || UpdateTask.playerPositions.isEmpty()) return worldEntities;
        // Don't render if can't get access to world to check for players in range
        if(Minecraft.getInstance().level == null)  return worldEntities;
        // Don't render if can't get access to cameraEntity to check for player distance
        if(Minecraft.getInstance().cameraEntity == null)  return worldEntities;

        List<AbstractClientPlayer> playerClientEntityList = Minecraft.getInstance().level.players();
        ArrayList<String> renderedPlayerNames = new ArrayList<>();
        for (AbstractClientPlayer playerClientEntity : playerClientEntityList) {
            renderedPlayerNames.add(playerClientEntity.getName().plainCopy().getString());
        }

        Vec3 camPosition = Minecraft.getInstance().cameraEntity.position();

        if(!AbstractModInitializer.fakePlayerEntities.containsKey(Minecraft.getInstance().level)){
            HashMap<String, RemotePlayer> temp = new HashMap<>();
            AbstractModInitializer.fakePlayerEntities.put(Minecraft.getInstance().level, temp);
        }

        // For each remote player
        ArrayList<Entity> playerEntities = new ArrayList<>(UpdateTask.playerPositions.size());
        for (PlayerPosition playerPosition : UpdateTask.playerPositions.values()) {
            // Skip if player has invalid data
            if(playerPosition == null || playerPosition.gameProfile == null) continue;
            // Don't render same player when they are actually in range
            if(renderedPlayerNames.contains(playerPosition.player)) continue;

            boolean isFriend = CommonModConfig.Instance.friendList().contains(playerPosition.player);

            if (CommonModConfig.Instance.onlyShowFriendsIcons() && !isFriend) continue;

            int maxIconDistance;
            if (CommonModConfig.Instance.overwriteFriendDistances() && isFriend) {
                maxIconDistance = CommonModConfig.Instance.maxFriendIconDistance();
            }
            else {
                maxIconDistance = CommonModConfig.Instance.maxIconDistance();
            }

            // If further away than the maximum icon distance,
            // don't show icon
            double d = camPosition.distanceTo(new Vec3(playerPosition.x, playerPosition.y, playerPosition.z));
            if (d > maxIconDistance) continue;

            // Add remote player to list as an entity
            RemotePlayer playerEntity;
            if(AbstractModInitializer.fakePlayerEntities.get(Minecraft.getInstance().level).containsKey(playerPosition.player)){
                playerEntity = AbstractModInitializer.fakePlayerEntities.get(Minecraft.getInstance().level).get(playerPosition.player);
            }
            else {
                #if MC_VER == MC_1_19_2
                playerEntity = new RemotePlayer(Minecraft.getInstance().level, playerPosition.gameProfile, null);
                #else
                playerEntity = new RemotePlayer(Minecraft.getInstance().level, playerPosition.gameProfile);
                #endif
                AbstractModInitializer.fakePlayerEntities.get(Minecraft.getInstance().level).put(playerPosition.player, playerEntity);
            }
            playerEntity.moveTo(playerPosition.x, playerPosition.y, playerPosition.z, 0, 0);
            playerEntities.add(playerEntity);
        }

        // Add all remote player entities to real entities in world
        ArrayList<Entity> worldEntitiesList = new ArrayList<>();
        worldEntities.forEach(worldEntitiesList::add);
        worldEntitiesList.addAll(playerEntities);
        return worldEntitiesList;
    }
}
