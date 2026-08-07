<div align="center">

![Map Link](docs/assets/ModBanner.png)

<a href="https://modrinth.com/mod/maplink"><img alt="Modrinth Downloads" src="https://img.shields.io/modrinth/dt/maplink?logo=modrinth"></a>
<a href="https://github.com/thebuildcraft/MapLink/blob/main/LICENSE.txt"><img src="https://img.shields.io/github/license/thebuildcraft/MapLink?style=flat&color=900c3f" alt="License"></a>
<img src="https://img.shields.io/badge/environment-client-1976d2">

<img alt="fabric" src="https://img.shields.io/badge/mod%20loader-Fabric-dbb18e"/>
<img alt="forge" src="https://img.shields.io/badge/mod%20loader-Forge-959eef"/>
<img alt="neoforge" src="https://img.shields.io/badge/mod%20loader-NeoForge-f99e6b"/>
<img alt="quilt" src="https://img.shields.io/badge/mod%20loader-Quilt-c796f9"/>

---

Map Link is a client mod that can download and convert tiles and synchronizes player positions, marker positions and area overlays from Web-Maps like Bluemap, Dynmap, LiveAtlas, Pl3xMap or Squaremap with [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) and [Worldmap](https://modrinth.com/mod/xaeros-world-map).
It can also show how long a player has been AFK based on movement data.

_Formerly known as "Remote Player Waypoints for Xaero's Map"_

</div>

### Features
- Synchronizes player positions in real-time as **player-head-icons** (Xaero's "Tracked Players") or **waypoints**
- Download and convert map-tiles (Bluemap only; other maps in future updates)
- Displays **web-map-markers** with **custom icons**
- Displays **web-map-area-overlays** aka claims
- AFK-time display in tab list
- Friend-list to override some settings for friends
- Very customizable with lots of config settings

### Supported Web Maps
- Bluemap
- Dynmap
- LiveAtlas
- Pl3xMap
- Squaremap

### How to use
1. Open the mod config menu (requires Mod Menu on Fabric/Quilt)
2. Add a new server-entry in the config: [General -> Server Entries]
3. Select the correct [Web Map Type]
4. Put in the exact server-ip you use to connect to the server
5. Put in the link to the web map (just copy it from the browser)

### How to download and convert map tiles
_Bluemap only for now_
1. Right-Click-Drag on the Worldmap to select the chunks
2. Select "Download Map Tiles"
3. Select the correct world

_...and then just wait until it's finished :)_

### Compatibility
- Xaero's Minimap and Xaero's Better PvP + Fair Play Versions
- Xaero's Worldmap

### Massive thanks to
- [ewpratten](https://github.com/ewpratten) for making [RemotePlayers](https://github.com/ewpratten/remoteplayers) which this mod was originally based on
- [MeerBiene](https://github.com/MeerBiene) for having the idea and helping with the AFK-time formatting
- [eatmyvenom](https://github.com/eatmyvenom) for having the idea and helping with implementing the marker support
- [James Seibel](https://gitlab.com/jeseibel) for making Distance Horizons: I used a lot of the multi-version build scripts from there.
- [TheMrEngMan](https://github.com/TheMrEngMan) for letting me use the features from his own fork of the original RemotePlayers mod
- [NotRyken](https://github.com/NotRyken) for helping with the 26.1 and 26.2 port

_This mod is not officially affiliated with Bluemap, Dynmap, LiveAtlas, Pl3xMap, Squaremap or Xaero in any way._
