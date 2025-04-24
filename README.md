<div align="center">
  <img src="common/src/main/resources/assets/remote_player_waypoints_for_xaero/icon.png" alt="icon" width="10%" height="auto" />

# Remote player waypoints for Xaero's Map

<a href="https://modrinth.com/mod/remote-player-waypoints-for-xaeros-map"><img alt="Modrinth Downloads" src="https://img.shields.io/modrinth/dt/remote-player-waypoints-for-xaeros-map?logo=modrinth"></a>
<a href="https://github.com/thebuildcraft/RemotePlayerWaypointsForXaero/blob/main/LICENSE"><img src="https://img.shields.io/github/license/thebuildcraft/RemotePlayerWaypointsForXaero?style=flat&color=900c3f" alt="License"></a>
<img src="https://img.shields.io/badge/environment-client-1976d2">

<img alt="fabric" src="https://img.shields.io/badge/mod%20loader-Fabric-dbb18e"/>
<img alt="forge" src="https://img.shields.io/badge/mod%20loader-Forge-959eef"/>
<img alt="neoforge" src="https://img.shields.io/badge/mod%20loader-NeoForge-f99e6b"/>
<img alt="quilt" src="https://img.shields.io/badge/mod%20loader-Quilt-c796f9"/>

---

This is a small client mod that allows you to see other players from further away on servers and see online-map-markers in game.
It does this by getting the position data from Dynmap, Bluemap, Squaremap or Pl3xMap running on the server and displaying it with waypoints and icons in [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) and [Worldmap](https://modrinth.com/mod/xaeros-world-map).

The AFK display feature can be used without having a map mod installed.

</div>

### Features
- see player positions as **waypoints** and **player-head-icons** on Xaero's Minimap and Worldmap
- see online-map-markers in game
- AFK display in tab list
- friend-list features (for example overwrite color of waypoints of friends)
- configurable trough Modmenu and Cloth Config Api

### Supported Maps
- Dynmap
- Bluemap
- Squaremap
- Pl3xMap
- LiveAtlas with multiple servers

### How to use
You just have to add the server you want to use this mod on to the list in the config:<br>
"server ip" is the ip you use to connect to the minecraft server<br>
"online map link" is the weblink to the map online  (just copy it from the browser)<br>
And make sure to set the "map-type" correctly!

### Compatibility
- Fabric, Quilt, Forge, NeoForge
- Xaero's Minimap and Xaero's Better PvP + Fair Play Versions
- Xaero's Worldmap

### Massive thanks to
- [ewpratten](https://github.com/ewpratten) for making [RemotePlayers](https://github.com/ewpratten/remoteplayers) which this mod was originally based on
- [MeerBiene](https://github.com/MeerBiene) for having the idea and helping with the AFK-time formatting
- [eatmyvenom](https://github.com/eatmyvenom) for having the idea and helping with implementing the marker support
- [James Seibel](https://gitlab.com/jeseibel) for making Distance Horizons: I used a lot of the multi-version build scripts from there.
- [TheMrEngMan](https://github.com/TheMrEngMan) for letting me use the features from his own fork of the original RemotePlayers mod

_This mod is not officially affiliated with Dynmap, Bluemap, Squaremap, Pl3xMap, LiveAtlas or Xaero in any way._
