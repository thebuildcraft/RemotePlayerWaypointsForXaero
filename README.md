<div align="center">
  <img src="common/src/main/resources/assets/remote_player_waypoints_for_xaero/icon.png" alt="icon" width="10%" height="auto" />

# Remote player waypoints for Xaero's Map

<a href="https://modrinth.com/mod/remote-player-waypoints-for-xaeros-map"><img alt="Modrinth Downloads" src="https://img.shields.io/modrinth/dt/remote-player-waypoints-for-xaeros-map?logo=modrinth"></a>
[![CodeFactor Grade](https://img.shields.io/codefactor/grade/github/thebuildcraft/RemotePlayerWaypointsForXaero/main?logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8%2F9hAAABhWlDQ1BJQ0MgcHJvZmlsZQAAKJF9kT1Iw0AcxV9bxSIVh1YQcYhQnSyIiuimVShChVArtOpgcukXNGlIUlwcBdeCgx%2BLVQcXZ10dXAVB8APE0clJ0UVK%2FF9SaBHjwXE%2F3t173L0D%2FPUyU82OMUDVLCOViAuZ7KrQ9YogwujDEGYkZupzopiE5%2Fi6h4%2BvdzGe5X3uz9Gj5EwG%2BATiWaYbFvEG8dSmpXPeJ46woqQQnxOPGnRB4keuyy6%2FcS447OeZESOdmieOEAuFNpbbmBUNlXiSOKqoGuX7My4rnLc4q%2BUqa96TvzCU01aWuU5zEAksYgkiBMioooQyLMRo1UgxkaL9uId%2FwPGL5JLJVQIjxwIqUCE5fvA%2F%2BN2tmZ8Yd5NCcaDzxbY%2FhoGuXaBRs%2B3vY9tunACBZ%2BBKa%2FkrdWD6k%2FRaS4seAb3bwMV1S5P3gMsdoP9JlwzJkQI0%2Ffk88H5G35QFwrdA95rbW3Mfpw9AmrpK3gAHh8BIgbLXPd4dbO%2Ft3zPN%2Fn4Ax9dyyerighsAAAAGYktHRAAAAAAAAPlDu38AAAAJcEhZcwAADdcAAA3XAUIom3gAAAAHdElNRQfmCBMVKAA5pS6%2BAAABlElEQVQ4y82PP2gVQRDGf7N3t%2Bvdixpi0N5OELFKJ1iohBciKlgYJLX6YkBbC0sVooVFBAvBPw%2BFZzrJs7DR2iYHRhBsxNI8VLwUx92MRXJGxKCp9AfL7DfDfPutFO3z5wy5DuRlWU2OvLj7hduLYXh0ZSEkOh4SjUKiBK%2BEZP34Gu%2FtbebLE86Qa8BO4FDwyWmAbPjzMWACiNgEMdun6macwfJ6z2qxZYBI6ndAxR%2BRN%2FL1ZGeXlDqFkm%2Fv33nZjHZ0u2OZrw%2F7pBYf16Re8UEJ8VpNE33fP3BxgX%2BOFOOdtjmuGpoPtT51pNcrMZORx4%2FmslQnslAlWahItymZrz%2Bmqc4%2B2z%2B71BjE5uwesEeQsaLY%2FQp42LrfPUqwy2DNO03ZK9hN4Ehj4IDBjzjKCoC5aMDG9q%2BhBz%2BrWCN3KqptBtG89Xx%2BEWB1%2Bszr8OTBFMgkSLKWQAA%2BVCU3%2BK%2BQb%2B0LB4FLGHmrP39LNv3773Ei9IBphLnVduf4VhM4M9JGqGzc%2F5bYnDsrqlcQloaK0adbNfgOUn6NRlZZ46YAAAAASUVORK5CYII%3D)](https://www.codefactor.io/repository/github/thebuildcraft/remoteplayerwaypointsforxaero/overview/main)
<a href="https://github.com/thebuildcraft/RemotePlayerWaypointsForXaero/blob/main/LICENSE.txt"><img src="https://img.shields.io/github/license/thebuildcraft/RemotePlayerWaypointsForXaero?style=flat&color=900c3f" alt="License"></a>
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
