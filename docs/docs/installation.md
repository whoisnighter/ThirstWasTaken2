# Installation

## Download

Download the mod from [Modrinth](https://modrinth.com/mod/thirst-was-taken-2). It is also on
[CurseForge](https://www.curseforge.com/minecraft/mc-mods/thirst-was-taken-2).

## Supported versions

Every Minecraft version and loader gets its own file. The suffix on the file name is the Minecraft
version, NeoForge files end in `-neoforge` and Forge files in `-forge`, for example
`ThirstWasTaken2-1.0.9+1.21.1.jar` and `ThirstWasTaken2-1.0.9+1.21.1-neoforge.jar`.

### Fabric

| Minecraft | File suffix | Fabric Loader | Fabric API | Java |
|---|---|---|---|---|
| 26.3 | `+26.3` | 0.19.5 or newer | 0.166.0+26.3 or newer | 25 |
| 26.2 | `+26.2` | 0.19.5 or newer | 0.161.0+26.2 or newer | 25 |
| 26.1, 26.1.1, 26.1.2 | `+26.1.2` | 0.19.5 or newer | 0.155.3+26.1.2 or newer | 25 |
| 1.21.11 | `+1.21.11` | 0.19.5 or newer | 0.141.6+1.21.11 or newer | 21 |
| 1.21, 1.21.1 | `+1.21.1` | 0.19.5 or newer | 0.116.18+1.21.1 or newer | 21 |
| 1.20.1 | `+1.20.1` | 0.19.5 or newer | 0.92.12+1.20.1 or newer | 17 |

Fabric API is required and must match the Minecraft version.

### NeoForge

| Minecraft | File suffix | NeoForge | Java |
|---|---|---|---|
| 26.3 | `+26.3-neoforge` | 26.3.0.83 or newer | 25 |
| 26.2 | `+26.2-neoforge` | 26.2.0.93 or newer | 25 |
| 26.1, 26.1.1, 26.1.2 | `+26.1.2-neoforge` | 26.1.2.121 or newer | 25 |
| 1.21.11 | `+1.21.11-neoforge` | 21.11.45 or newer | 21 |
| 1.21.1 | `+1.21.1-neoforge` | 21.1.262 or newer | 21 |

NeoForge 1.21.11 is no longer supported. Its last file is 1.8.0.

NeoForge needs nothing else. The 1.21.1 file does not run on 1.21. NeoForge has only beta builds for
26.3 so far, so that file asks for one.

### Forge

Minecraft 1.20.1 runs on Forge rather than NeoForge.

| Minecraft | File suffix | Forge | Java |
|---|---|---|---|
| 1.20.1 | `+1.20.1-forge` | any 47 build, tested with 47.4.28 | 17 |

Forge needs nothing else.

The Java version is the minimum on every loader. The runtime Minecraft ships with is enough.

### Minecraft 1.20.1 to 1.21.1

- The droplets in item tooltips have a shadow.

## Compatible mods

All optional except Fabric API. Versions are listed in the order 26.3, 26.2, 26.1.x, 1.21.11, 1.21.1,
1.20.1.

### Fabric

| Mod | Versions built with | What it adds |
|---|---|---|
| ![](https://cdn.modrinth.com/data/P7dR8mSH/icon.png){.mod-icon} [Fabric API](https://modrinth.com/mod/fabric-api) | 0.166.0+26.3, 0.161.0+26.2, 0.155.3+26.1.2, 0.141.6+1.21.11, 0.116.18+1.21.1, 0.92.12+1.20.1 | Required. |
| ![](https://cdn.modrinth.com/data/mOgUt4GM/5a20ed1450a0e1e79a1fe04e61bb4e5878bf1d20.png){.mod-icon} [Mod Menu](https://modrinth.com/mod/modmenu) | 21.0.0, 20.0.3, 18.0.2, 17.0.1, 11.0.5, 7.2.2 | A Config button for the [settings screen](/docs/configuration). |
| ![](https://cdn.modrinth.com/data/EsAfCjCV/icon.png){.mod-icon} [AppleSkin](https://modrinth.com/mod/appleskin) | 3.0.10+mc26.3, 3.0.10+mc26.2, 3.0.10+mc26.1.2, 3.0.8+mc1.21.11, 3.0.6+mc1.21, 2.5.2+mc1.20.1 | The quenched outline on the thirst bar, droplet rows in tooltips, and the exhaustion strip. |
| ![](https://cdn.modrinth.com/data/9s6osm5g/ed8a2316cbb6f4fc5f510e8e13a59a85cbbbff4d_96.webp){.mod-icon} [Cloth Config](https://modrinth.com/mod/cloth-config) | 26.3.159, 26.2.155, 26.1.154, 21.11.153, 15.0.140, 11.1.136+fabric | AppleSkin's own settings screen. |
| ![](https://cdn.modrinth.com/data/dKvj0eNn/a1e1ad6f018c3a47cb300edbf0ebebce894bfd45_96.webp){.mod-icon} [Create Fly](https://modrinth.com/mod/create-fly) | 26.2-rc-2-6.0.9-1, 26.1.2-6.0.9-4 | The [Sand Filter](/docs/integrations/create). 26.2 and 26.1.2 only. |
| ![](https://cdn.modrinth.com/data/nvQzSEkH/b04217bc2b7dc524c4d12f81ff42cc1cefb9b0fc_96.webp){.mod-icon} [Jade 🔍](https://modrinth.com/mod/jade) | 26.3.5, 26.2.11, 26.1.11, 21.1.6, 15.10.6, 11.13.3+fabric | The [grade of the water](/docs/features/water-purity#checking-water-with-jade) under the crosshair. Client only. |
| ![](https://cdn.modrinth.com/data/fFEIiSDQ/e9f5f66fa3b67e54acb91258a1428d68311c58bc_96.webp){.mod-icon} [Supplementaries](https://modrinth.com/mod/supplementaries) | 1.21.1-4.0.2 | [Water that keeps its grade](/docs/integrations/supplementaries) in Jars, Goblets and Faucets, and a Jar or Goblet of water that can be drunk. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/Ct11Kuii/819ba69579e76715103825ce28b345781b415393.png){.mod-icon} [Kaleidoscope Cookery<br>Refabricated](https://modrinth.com/mod/kaleidoscope-cookery-refabricated) | 1.6.0.8-fabric+mc26.3, 1.6.0.8-fabric+mc26.2, 1.6.0.8-fabric+mc26.1.2, 1.3.0.10-fabric+mc1.21.11, 1.6.0.8-fabric+mc1.21.1, 1.6.0.8-fabric+mc1.20.1 | Thirst from its [teas and soups](/docs/integrations/kaleidoscope-cookery/), and water that keeps its grade in the Stockpot and the Teapot. The official Fabric build stopped at 1.0.1 and only gets the thirst values. |
| ![](https://cdn.modrinth.com/data/UMblNdlF/bfeeabb4b3f926a0afbc953490c32892a0999122.png){.mod-icon} [Kaleidoscope Tavern<br>Refabricated](https://modrinth.com/mod/kaleidoscope-tavern-refabricated) | 1.2.0.13-fabric+mc26.3, 1.2.0.13-fabric+mc26.2, 1.2.0.13-fabric+mc26.1.2, 1.2.0.6-fabric+mc1.21.11, 1.2.0.13-fabric+mc1.21.1, 1.2.0.13-fabric+mc1.20.1 | Thirst from its [wines, cocktails and juices](/docs/integrations/kaleidoscope-tavern), and water that keeps its grade in the Barrel, through the Tap and in a placed Water Bottle. |
| ![](https://cdn.modrinth.com/data/e0bNACJD/f8b292ea53e0a0ea908570defddc48673d16d7d6.png){.mod-icon} [Serene Seasons](https://modrinth.com/mod/serene-seasons) | 26.1.2.0.7, 26.1.2.0.6, 21.11.0.4, 10.1.0.9, 9.1.0.3 | Thirst that [follows the season](/docs/integrations/serene-seasons). Needs GlitchCore. |
| ![](https://cdn.modrinth.com/data/HJetCzWo/7c6c372629b3efa41409621631d60df12963f005_96.webp){.mod-icon} [Let's Do: Farm & Charm](https://modrinth.com/mod/lets-do-farm-charm) | 1.1.29 | Thirst from its [teas and soups](/docs/integrations/farm-and-charm/), graded water from the Timber Well, and no clean water from the Water Trough. Needs Architectury API. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/qwbArkQk/5e0770c8da0fab82a70bc9c3913c8d3996c53345_96.webp){.mod-icon} [Let's Do: Candlelight](https://modrinth.com/mod/lets-do-candlelight-farmcharm-compat) | 2.1.15 | Thirst from its [soups and salads](/docs/integrations/farm-and-charm/candlelight), and kitchen sinks that give water to boil. Needs Farm & Charm. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/Eh11TaTm/cea48ad39e9323e9e0f5354ee1d4c160f46b50be_96.webp){.mod-icon} [Let's Do: HerbalBrews](https://modrinth.com/mod/lets-do-herbalbrews) | 1.1.4 | Thirst from its [teas and coffees](/docs/integrations/herbalbrews), also from a Jug, and a Tea Kettle that won't brew with sea water. Needs Architectury API. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/GyKzAh3l/41b9b45c365ecd55aced04bcd22af93878f766a0_96.webp){.mod-icon} [Let's Do: Beachparty](https://modrinth.com/mod/lets-do-beachparty) | 2.1.5 | Thirst from its [cocktails and coconuts](/docs/integrations/beachparty), also sipped from a placed glass. Needs Architectury API and Trinkets. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/1DWmBJVA/029aec55be4d860ba0aede4939dd93332b6dafad_96.webp){.mod-icon} [Let's Do: Vinery](https://modrinth.com/mod/lets-do-vinery) | 1.5.6 | Thirst from its [juices, wines and grapes](/docs/integrations/vinery), alcohol less than juice. Needs Architectury API. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/ra7o6Sl7/d7e6f6bcaa82b9af4a670e6dd6cb980c970aa0f2_96.webp){.mod-icon} [Let's Do: Meadow](https://modrinth.com/mod/lets-do-meadow) | 1.4.10 | Its [wooden bucket keeps the water's grade](/docs/integrations/meadow), its wooden cauldron gives Murky water, and sea water won't go into it or the Cooking Pot. Wooden milk buckets restore thirst. Needs Architectury API. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/krskFMfA/465cfcd453c22ee5a09884ede98a0442e97658c5.png){.mod-icon} [Spelunkery](https://modrinth.com/mod/spelunkery) | 1.21.1-0.4.4 | [Pure Spring Water](/docs/integrations/spelunkery), and salt boiled only from sea water. Needs Moonlight Lib. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/YgrSDjlb/f4e10611dcc1791138225224fa444f5a8ef4348f_96.webp){.mod-icon} [Bountiful Fares](https://modrinth.com/mod/bountiful-fares) | 3.0.13 | Cups that keep the [grade of their water](/docs/integrations/bountiful-fares), thirst from its teas, ciders and stews, and no fermenting or tea from sea water. Needs NexusLib. 1.21.1 only. |
| ![](https://media.forgecdn.net/avatars/thumbnails/308/636/64/64/637392485303151332.png){.mod-icon} [Croptopia](https://www.curseforge.com/minecraft/mc-mods/croptopia) | 26.2-4.3.2, 26.1.2-4.3.2, 1.21.1-4.2.4, 1.20.1-4.0.1 | Thirst from its [drinks, soups and fruit](/docs/integrations/croptopia), and recipes that won't take sea water. Needs EpheroLib. On CurseForge only. |
| ![](https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2F7vxePowz%2F26e8448993e9bda4dba92b6e7a1a13d9c4333138.png&trim=1&w=96&h=96&fit=contain&cbg=00000000&output=png){.mod-icon .mod-icon-lg} [Farmer's Delight<br>Refabricated](https://modrinth.com/mod/farmers-delight-refabricated) | 26.3-3.6.30, 26.2-3.6.30, 26.1-3.6.30, 1.21.11-3.6.16, 1.21.1-3.3.6, 1.20.1-2.5.7 | Thirst from its [drinks and meals](/docs/integrations/farmers-delight/), Pure water from the Cooking Pot, and no drain under Nourishment. |
| ![](https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FhIu9KJTT%2Ff7c591a80046859d3d45c04ecbbc54d264483d5e.png&trim=1&w=96&h=96&fit=contain&cbg=00000000&output=png){.mod-icon .mod-icon-lg} [Brewin' and Chewin'](https://modrinth.com/mod/brewin-and-chewin) | v4.5.0+1.21.1-fabric | Thirst from its [brews and soups](/docs/integrations/farmers-delight/brewin-and-chewin), and water that keeps its grade in the Keg. 1.21.1 only. |
| ![](https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FDGiq4ZSW%2F949ba66d6fffb5a984fbb70e3ef4a51f15be3191.png&trim=1&w=96&h=96&fit=contain&cbg=00000000&output=png){.mod-icon .mod-icon-lg} [Ocean's Delight](https://modrinth.com/mod/oceans-delight) | 1.0.3+fabric.1.21.1 | Thirst from its [Guardian Soup and bowls](/docs/integrations/farmers-delight/oceans-delight). 1.21.1 only. |
| ![](https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2Ffoa4fGIH%2Feecc99e281522f2291081c48176f0faa84c107bc.png&trim=1&w=96&h=96&fit=contain&cbg=00000000&output=png){.mod-icon .mod-icon-lg} [Rustic Delight](https://modrinth.com/mod/rustic-delight) | 1.7.0 (26.3, 26.2, 26.1), 1.7.2 (1.21.11), 1.7.1 (1.21.1) | Thirst from its [coffees, soups and bell peppers](/docs/integrations/farmers-delight/rustic-delight). |

### NeoForge

The settings screen opens from the Config button in NeoForge's Mods list, with no extra mod.

| Mod | Versions built with | What it adds |
|---|---|---|
| ![](https://cdn.modrinth.com/data/EsAfCjCV/icon.png){.mod-icon} [AppleSkin](https://modrinth.com/mod/appleskin) | 3.0.10+mc26.3, 3.0.10+mc26.2, 3.0.9+mc26.1, 3.0.9+mc1.21 | The same as on Fabric. |
| ![](https://cdn.modrinth.com/data/9s6osm5g/ed8a2316cbb6f4fc5f510e8e13a59a85cbbbff4d_96.webp){.mod-icon} [Cloth Config](https://modrinth.com/mod/cloth-config) | 26.2.155, 26.1.154, 15.0.140 | AppleSkin's own settings screen. Not on 26.3 yet. |
| ![](https://cdn.modrinth.com/data/nvQzSEkH/b04217bc2b7dc524c4d12f81ff42cc1cefb9b0fc_96.webp){.mod-icon} [Jade 🔍](https://modrinth.com/mod/jade) | 26.3.5, 26.2.10, 26.1.12, 15.10.6 | The same as on Fabric. Client only. |
| ![](https://cdn.modrinth.com/data/LNytGWDc/61d716699bcf1ec42ed4926a9e1c7311be6087e2_96.webp){.mod-icon} [Create](https://modrinth.com/mod/create) | 6.0.10 | The [Sand Filter](/docs/integrations/create). 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/CuAT8bVS/6c7f64edd8244c250a35a1b2cc241fd2d01f0a7d_96.webp){.mod-icon} [Create Pipes n Physics](https://modrinth.com/mod/create-pipes-n-physics) | 3.2.1 | Water keeps its grade through [its pipes and pumps](/docs/integrations/create#create-pipes-n-physics), and the Sand Filter works with it. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/TyCTlI4b/e31c7e2f8769d317339e25b2a8d1b40fbf312729_96.webp){.mod-icon} [Sophisticated Backpacks](https://modrinth.com/mod/sophisticated-backpacks) | Core 26.2-1.5.32.2471, 26.1.2-1.5.34.2469, 1.21.1-1.5.33.2470. Not on 26.3 yet. | The [Drinking Upgrade](/docs/integrations/sophisticated-backpacks), and water that keeps its grade in the Tank and Pump Upgrades. Sophisticated Storage takes the Drinking Upgrade too. |
| ![](https://cdn.modrinth.com/data/fFEIiSDQ/e9f5f66fa3b67e54acb91258a1428d68311c58bc_96.webp){.mod-icon} [Supplementaries](https://modrinth.com/mod/supplementaries) | 1.21.1-4.0.2 | The same as on Fabric. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/Ce6I4WUE/ea185eb1300a64867f89101d4798e71a54ef6bed_96.webp){.mod-icon} [Mekanism](https://modrinth.com/mod/mekanism) | 10.7.19.85 | Water keeps its grade in [its pumps, pipes and tanks](/docs/integrations/mekanism), and the Rotary Condensentrator makes Pure water. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/tIm2nV03/icon.png){.mod-icon} [Immersive Engineering](https://modrinth.com/mod/immersiveengineering) | 12.4.2-194 | Water keeps its grade in [its pumps, pipes, barrels and bottles](/docs/integrations/immersive-engineering). 1.21.1 only. |
| ![](https://media.forgecdn.net/avatars/thumbnails/1361/462/64/64/638884307253099520.png){.mod-icon} [Kaleidoscope Cookery](https://modrinth.com/mod/kaleidoscope-cookery) | 1.6.0-neoforge+mc1.21.1 | Thirst from its [teas and soups](/docs/integrations/kaleidoscope-cookery/), and water that keeps its grade in the Stockpot and the Teapot. 1.21.1 only. |
| ![](https://media.forgecdn.net/avatars/thumbnails/2040/206/64/64/639247202160912813.png){.mod-icon} [Kaleidoscope Flora](https://www.curseforge.com/minecraft/mc-mods/kaleidoscope-flora) | 0.3.4 | Thirst from its [flower teas](/docs/integrations/kaleidoscope-cookery/kaleidoscope-flora). Needs Kaleidoscope Cookery. On CurseForge only. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/cuIIkdlx/748a556a3658f0a5f67068f4d8d5cf0041b794a7.png){.mod-icon} [Kaleidoscope Chinese Food](https://modrinth.com/mod/kaleidoscopechinesefood) | 1.1.14-neoforge+1.21.1 | Thirst from its [teas, soups and noodles](/docs/integrations/kaleidoscope-cookery/kaleidoscope-chinese-food). Needs Kaleidoscope Cookery. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/r9RZvhiJ/b96860512d16e72dcaa30b15cb0b9b25c18a38e8.png){.mod-icon} [Kaleidoscope Tavern](https://modrinth.com/mod/kaleidoscopetavern) | 1.2.0-neoforge+mc1.21.1 | Thirst from its [wines, cocktails and juices](/docs/integrations/kaleidoscope-tavern), and water that keeps its grade in the Barrel, through the Tap and in a placed Water Bottle. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/uXhSmPjd/bf55420556c30d44d2f5cf7b8915705b9214b4ef.png){.mod-icon} [Cold Sweat](https://modrinth.com/mod/cold-sweat) | 2.4.5 | Thirst that follows [its temperature](/docs/integrations/cold-sweat), a graded Waterskin, and a Boiler that purifies water. |
| ![](https://cdn.modrinth.com/data/e0bNACJD/f8b292ea53e0a0ea908570defddc48673d16d7d6.png){.mod-icon} [Serene Seasons](https://modrinth.com/mod/serene-seasons) | 26.1.2.0.7, 26.1.2.0.6, 21.11.0.4, 10.1.0.9 | The same as on Fabric. |
| ![](https://cdn.modrinth.com/data/HJetCzWo/7c6c372629b3efa41409621631d60df12963f005_96.webp){.mod-icon} [Let's Do: Farm & Charm](https://modrinth.com/mod/lets-do-farm-charm) | 1.1.29 | The same as on Fabric. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/qwbArkQk/5e0770c8da0fab82a70bc9c3913c8d3996c53345_96.webp){.mod-icon} [Let's Do: Candlelight](https://modrinth.com/mod/lets-do-candlelight-farmcharm-compat) | 2.1.15 | The same as on Fabric. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/Eh11TaTm/cea48ad39e9323e9e0f5354ee1d4c160f46b50be_96.webp){.mod-icon} [Let's Do: HerbalBrews](https://modrinth.com/mod/lets-do-herbalbrews) | 1.1.4 | The same as on Fabric. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/GyKzAh3l/41b9b45c365ecd55aced04bcd22af93878f766a0_96.webp){.mod-icon} [Let's Do: Beachparty](https://modrinth.com/mod/lets-do-beachparty) | 2.1.5 | The same as on Fabric, with Curios API in place of Trinkets. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/1DWmBJVA/029aec55be4d860ba0aede4939dd93332b6dafad_96.webp){.mod-icon} [Let's Do: Vinery](https://modrinth.com/mod/lets-do-vinery) | 1.5.6 | The same as on Fabric. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/ra7o6Sl7/d7e6f6bcaa82b9af4a670e6dd6cb980c970aa0f2_96.webp){.mod-icon} [Let's Do: Meadow](https://modrinth.com/mod/lets-do-meadow) | 1.4.10 | The same as on Fabric. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/krskFMfA/465cfcd453c22ee5a09884ede98a0442e97658c5.png){.mod-icon} [Spelunkery](https://modrinth.com/mod/spelunkery) | 1.21.1-0.4.4 | The same as on Fabric. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/YgrSDjlb/f4e10611dcc1791138225224fa444f5a8ef4348f_96.webp){.mod-icon} [Bountiful Fares](https://modrinth.com/mod/bountiful-fares) | 3.0.13 | The same as on Fabric. 1.21.1 only. |
| ![](https://media.forgecdn.net/avatars/thumbnails/308/636/64/64/637392485303151332.png){.mod-icon} [Croptopia](https://www.curseforge.com/minecraft/mc-mods/croptopia) | 26.2-4.3.2, 26.1.2-4.3.2, 1.21.1-4.2.4 | The same as on Fabric. |
| ![](https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FR2OftAxM%2F8e7aa38ab94d94bb0a2894a218b69beb49002b34.png&trim=1&w=96&h=96&fit=contain&cbg=00000000&output=png){.mod-icon .mod-icon-lg} [Farmer's Delight](https://modrinth.com/mod/farmers-delight) | 1.21.1-1.3.4 | The same as on Fabric. 1.21.1 only. |
| ![](https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FhIu9KJTT%2Ff7c591a80046859d3d45c04ecbbc54d264483d5e.png&trim=1&w=96&h=96&fit=contain&cbg=00000000&output=png){.mod-icon .mod-icon-lg} [Brewin' and Chewin'](https://modrinth.com/mod/brewin-and-chewin) | v4.5.0+1.21.1-neoforge | The same as on Fabric. 1.21.1 only. |
| ![](https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FYttyNOFA%2Fd857243f0e7dedd3d7f552c4371326773629e42e.png&trim=1&w=96&h=96&fit=contain&cbg=00000000&output=png){.mod-icon .mod-icon-lg} [Cultural Delights](https://modrinth.com/mod/cultural-delights) | 0.19.8-1.21.1 | Thirst from its [drinks and watery foods](/docs/integrations/farmers-delight/cultural-delights), and a Vat that brews nothing from sea water. 1.21.1 only. |
| ![](https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2Fg6sbyCTu%2F4ecc5d554f260b876d21c427aa6c2bdf4457fd5c.png&trim=1&w=96&h=96&fit=contain&cbg=00000000&output=png){.mod-icon .mod-icon-lg} [Fruits Delight](https://modrinth.com/mod/fruits-delight) | 1.2.14 | Thirst from its [juices, teas and fruit](/docs/integrations/farmers-delight/fruits-delight). 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/Ohy0TgO2/4e25b7b5de37ad03845970ec8ef72f5c0b8e4617_96.webp){.mod-icon} [Youkais' Feasts](https://modrinth.com/mod/gensokyo-delight-youkais-feasts) | 1.1.2 | Thirst from its [teas, sakes and soups](/docs/integrations/farmers-delight/youkais-feasts), and a Kettle, Fermentation Tank and iron pots that take no sea water. 1.21.1 only. |
| ![](https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2F8EEEXOzj%2Fe5d9aa8bd6bf5dcbd674f08b92957d4b001229e3.png&trim=1&w=96&h=96&fit=contain&cbg=00000000&output=png){.mod-icon .mod-icon-lg} [Hearth and Harvest](https://modrinth.com/mod/hearth-and-harvest) | 1.4.3 | Thirst from its [juices, wines and stews](/docs/integrations/farmers-delight/hearth-and-harvest), tanks that keep the grade of their water, and salt only from sea water. 1.21.1 only. |
| ![](https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FyRrY3XII%2Fcec2396cf9f6f74a5b0cff196301a4b8b8124e1e.png&trim=1&w=96&h=96&fit=contain&cbg=00000000&output=png){.mod-icon .mod-icon-lg} [Extra Delight](https://modrinth.com/mod/extradelight) | 2.7 | Thirst from its [drinks, soups and popsicles](/docs/integrations/farmers-delight/extra-delight), tanks that keep the grade of their water, a Tap that gives Murky water, and salt only from sea water. 1.21.1 only. |
| ![](https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FqMxbM4BQ%2F0d6f967d3ad184dd296c62a9891e2b2b7d45f61d.png&trim=1&w=96&h=96&fit=contain&cbg=00000000&output=png){.mod-icon .mod-icon-lg} [Miner's Delight](https://modrinth.com/mod/miners-delight) | 1.4.5 | Thirst from its [soups and milk cup](/docs/integrations/farmers-delight/miners-delight), and copper cups that keep the grade of their water. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/kjZCvAn6/958489a1729e9e17a6a5a0728ef249236c07f7b3_96.webp){.mod-icon} [No Man's Land](https://modrinth.com/mod/no-mans-land) | 1.6.3 | Dirty water in [its swamps](/docs/integrations/no-mans-land), salty water at its Mud Beach, thirst from its drinks, and sips from its milk cauldron. 1.21.1 only. |
| ![](https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FDGiq4ZSW%2F949ba66d6fffb5a984fbb70e3ef4a51f15be3191.png&trim=1&w=96&h=96&fit=contain&cbg=00000000&output=png){.mod-icon .mod-icon-lg} [Ocean's Delight](https://modrinth.com/mod/oceans-delight) | 1.0.4 | The same as on Fabric. 1.21.1 only. |
| ![](https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2Fe9V6wFcR%2F4cbbace573b20628290929948a77c74d95ed7a70.png&trim=1&w=96&h=96&fit=contain&cbg=00000000&output=png){.mod-icon .mod-icon-lg} [Expanded Delight](https://modrinth.com/mod/expanded-delight) | 0.1.4 | Thirst from its [juices, soups and salads](/docs/integrations/farmers-delight/expanded-delight), and a Cooking Pot that cooks nothing from sea water. 1.21.1 only. |
| ![](https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2Ffoa4fGIH%2Feecc99e281522f2291081c48176f0faa84c107bc.png&trim=1&w=96&h=96&fit=contain&cbg=00000000&output=png){.mod-icon .mod-icon-lg} [Rustic Delight](https://modrinth.com/mod/rustic-delight) | 1.7.1 | The same as on Fabric. 1.21.1 only. |

### Forge

On Minecraft 1.20.1 only. The settings screen opens from the Config button in Forge's Mods list.

| Mod | Versions built with | What it adds |
|---|---|---|
| ![](https://cdn.modrinth.com/data/EsAfCjCV/icon.png){.mod-icon} [AppleSkin](https://modrinth.com/mod/appleskin) | 2.5.1+mc1.20.1 | The same as on Fabric. |
| ![](https://cdn.modrinth.com/data/9s6osm5g/ed8a2316cbb6f4fc5f510e8e13a59a85cbbbff4d_96.webp){.mod-icon} [Cloth Config](https://modrinth.com/mod/cloth-config) | 11.1.136+forge | AppleSkin's own settings screen. |
| ![](https://cdn.modrinth.com/data/nvQzSEkH/b04217bc2b7dc524c4d12f81ff42cc1cefb9b0fc_96.webp){.mod-icon} [Jade 🔍](https://modrinth.com/mod/jade) | 11.13.3+forge | The same as on Fabric. Client only. |
| ![](https://cdn.modrinth.com/data/LNytGWDc/61d716699bcf1ec42ed4926a9e1c7311be6087e2_96.webp){.mod-icon} [Create](https://modrinth.com/mod/create) | 6.0.8 | The [Sand Filter](/docs/integrations/create). |
| ![](https://cdn.modrinth.com/data/Ce6I4WUE/ea185eb1300a64867f89101d4798e71a54ef6bed_96.webp){.mod-icon} [Mekanism](https://modrinth.com/mod/mekanism) | 10.4.16.80 | The same as on NeoForge. |
| ![](https://cdn.modrinth.com/data/tIm2nV03/icon.png){.mod-icon} [Immersive Engineering](https://modrinth.com/mod/immersiveengineering) | 10.2.0-183 | The same as on NeoForge. |
| ![](https://cdn.modrinth.com/data/e0bNACJD/f8b292ea53e0a0ea908570defddc48673d16d7d6.png){.mod-icon} [Serene Seasons](https://modrinth.com/mod/serene-seasons) | 9.1.0.3 | The same as on Fabric. |
| ![](https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FR2OftAxM%2F8e7aa38ab94d94bb0a2894a218b69beb49002b34.png&trim=1&w=96&h=96&fit=contain&cbg=00000000&output=png){.mod-icon .mod-icon-lg} [Farmer's Delight](https://modrinth.com/mod/farmers-delight) | 1.20.1-1.3.4 | The same as on Fabric. |
| ![](https://media.forgecdn.net/avatars/thumbnails/1361/462/64/64/638884307253099520.png){.mod-icon} [Kaleidoscope Cookery](https://modrinth.com/mod/kaleidoscope-cookery) | 1.6.0-forge+mc1.20.1 | The same as on NeoForge. |
| ![](https://media.forgecdn.net/avatars/thumbnails/2040/206/64/64/639247202160912813.png){.mod-icon} [Kaleidoscope Flora](https://www.curseforge.com/minecraft/mc-mods/kaleidoscope-flora) | 1.20.1-0.3.4 | The same as on NeoForge. |
| ![](https://cdn.modrinth.com/data/cuIIkdlx/748a556a3658f0a5f67068f4d8d5cf0041b794a7.png){.mod-icon} [Kaleidoscope Chinese Food](https://modrinth.com/mod/kaleidoscopechinesefood) | 1.1.14-1.20.1 | The same as on NeoForge. |
| ![](https://cdn.modrinth.com/data/r9RZvhiJ/b96860512d16e72dcaa30b15cb0b9b25c18a38e8.png){.mod-icon} [Kaleidoscope Tavern](https://modrinth.com/mod/kaleidoscopetavern) | 1.2.0-forge+mc1.20.1 | The same as on NeoForge. |
| ![](https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2Fg6sbyCTu%2F4ecc5d554f260b876d21c427aa6c2bdf4457fd5c.png&trim=1&w=96&h=96&fit=contain&cbg=00000000&output=png){.mod-icon .mod-icon-lg} [Fruits Delight](https://modrinth.com/mod/fruits-delight) | 1.1.3 | The same as on NeoForge. |
| ![](https://cdn.modrinth.com/data/Ohy0TgO2/4e25b7b5de37ad03845970ec8ef72f5c0b8e4617_96.webp){.mod-icon} [Youkais' Feasts](https://modrinth.com/mod/gensokyo-delight-youkais-feasts) | 0.5.2 | The same as on NeoForge. |
| ![](https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2F8EEEXOzj%2Fe5d9aa8bd6bf5dcbd674f08b92957d4b001229e3.png&trim=1&w=96&h=96&fit=contain&cbg=00000000&output=png){.mod-icon .mod-icon-lg} [Hearth and Harvest](https://modrinth.com/mod/hearth-and-harvest) | 1.4.3 | The same as on NeoForge. |
| ![](https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FqMxbM4BQ%2F0d6f967d3ad184dd296c62a9891e2b2b7d45f61d.png&trim=1&w=96&h=96&fit=contain&cbg=00000000&output=png){.mod-icon .mod-icon-lg} [Miner's Delight](https://modrinth.com/mod/miners-delight) | 1.20.1-1.4.5-backport | The same as on NeoForge. |
| ![](https://cdn.modrinth.com/data/uXhSmPjd/bf55420556c30d44d2f5cf7b8915705b9214b4ef.png){.mod-icon} [Cold Sweat](https://modrinth.com/mod/cold-sweat) | 2.4.5 | The same as on NeoForge. |
| ![](https://media.forgecdn.net/avatars/thumbnails/308/636/64/64/637392485303151332.png){.mod-icon} [Croptopia](https://www.curseforge.com/minecraft/mc-mods/croptopia) | 1.20.1-4.0.1 | The same as on Fabric. |

Other food mods usually work as they are. Drinks their mod marks as drinks restore thirst, and any
item can be given a value in [Configuration](/docs/configuration).

## Where the mod goes

Put the jar in the `mods` folder of the server and of every client. A client without the mod does
not see the bar.

## Languages

The mod follows each player's game language. Nine are included: English, French, Japanese, Korean,
Polish, Russian, Vietnamese, Simplified Chinese and Traditional Chinese.

![Item tooltips with the game set to Simplified Chinese](/screenshots/water/chinese-tooltips.png)

## First run

The first launch writes `config/thirstwastaken2.json` with the defaults. Existing worlds work, and
every player starts at full thirst.
