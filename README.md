<div align="center">

<img src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/.github/assets/banner.png" alt="ThirstWasTaken2 banner" width="420">

[![modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_64h.png)](https://modrinth.com/mod/thirst-was-taken-2)
[![curseforge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/curseforge_64h.png)](https://www.curseforge.com/minecraft/mc-mods/thirst-was-taken-2)
[![ghpages](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/documentation/ghpages_64h.png)](https://whoisnighter.github.io/ThirstWasTaken2/)

A thirst bar, water purity and drinking for **Fabric**, **NeoForge** and **Forge**.

</div>

## Overview
* **Thirst and Quenched:** A thirst bar with a saturation-like reserve, drained faster by activity, heat and the Nether.
* **Water Quality:** Four fresh grades (Dirty, Murky, Clean, Pure) plus Salty sea water, sampled from the world when water is collected.
* **Purification:** Boiling in a furnace, a smoker or a Hanging Pot, or a Copper Canteen or Iron Flask over a campfire, makes water Clean. The Copper Distiller makes Pure water, from sea water too.
* **Containers:** Terracotta Bowl, Waterskin, Copper Canteen, Iron Flask and Copper and Iron Hanging Pots.

Every mechanic is explained on the [documentation site](https://whoisnighter.github.io/ThirstWasTaken2/).

<div align="center">
<table>
  <tr>
    <td align="center"><img src="docs/public/screenshots/hud/thirst-food-bars.png" alt="The thirst bar above the hunger bar" width="372"></td>
    <td align="center"><img src="docs/public/screenshots/water/water-tooltips.gif" alt="A water bottle tooltip showing each grade" width="272"></td>
  </tr>
</table>
</div>

## Requirements

| Minecraft | Java | Fabric Loader (min.) | Fabric API | NeoForge / Forge |
| :--- | :---: | :--- | :--- | :--- |
| 26.3 | 25 | 0.19.5 | 0.166.0+26.3 | 26.3.0.83 |
| 26.2 | 25 | 0.19.5 | 0.161.0+26.2 | 26.2.0.93 |
| 26.1 – 26.1.2 | 25 | 0.19.5 | 0.155.3+26.1.2 | 26.1.2.121 |
| 1.21.11 | 21 | 0.19.5 | 0.141.6+1.21.11 | 21.11.45, up to 1.8.0, no longer supported |
| 1.21.1 | 21 | 0.19.5 | 0.116.18+1.21.1 | 21.1.262 |
| 1.21 | 21 | 0.19.5 | 0.116.18+1.21.1 | – |
| 1.20.1 | 17 | 0.19.5 | 0.92.12+1.20.1 | Forge 47.4.28 |

Versions listed are minimums, except Forge, where any 47 build works. Install on the server **and** every client; NeoForge jars end in `-neoforge`, Forge jars in `-forge`.

## Compatibility
All integrations are soft dependencies: nothing is required, and nothing is loaded unless the mod is present.

| Mod | Fabric | NeoForge | Forge (1.20.1) |
| :--- | :--- | :--- | :---: |
| <img alt="AppleSkin icon" src="https://cdn.modrinth.com/data/EsAfCjCV/icon.png" width="20" height="20" align="absmiddle"> [AppleSkin](https://modrinth.com/mod/appleskin)<br><img alt="Jade icon" src="https://cdn.modrinth.com/data/nvQzSEkH/b04217bc2b7dc524c4d12f81ff42cc1cefb9b0fc_96.webp" width="20" height="20" align="absmiddle"> [Jade](https://modrinth.com/mod/jade)<br><img alt="Serene Seasons icon" src="https://cdn.modrinth.com/data/e0bNACJD/f8b292ea53e0a0ea908570defddc48673d16d7d6.png" width="20" height="20" align="absmiddle"> [Serene Seasons](https://modrinth.com/mod/serene-seasons)<br><img alt="Terralith icon" src="https://cdn.modrinth.com/data/8oi3bsk5/1959d924a1088944bbf07a06ba523726112d7e7a_96.webp" width="20" height="20" align="absmiddle"> [Terralith](https://modrinth.com/mod/terralith) | every version | every version | yes |
| <img alt="Farmer's Delight icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FR2OftAxM%2F8e7aa38ab94d94bb0a2894a218b69beb49002b34.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> [Farmer's Delight](https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/) | every version (Refabricated) | 1.21.1 | yes |
| <img alt="Create icon" src="https://cdn.modrinth.com/data/LNytGWDc/61d716699bcf1ec42ed4926a9e1c7311be6087e2_96.webp" width="20" height="20" align="absmiddle"> [Create](https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/create) | 26.1.x, 26.2 (Create Fly) | 1.21.1 | yes |
| <img alt="Create Pipes n Physics icon" src="https://cdn.modrinth.com/data/CuAT8bVS/6c7f64edd8244c250a35a1b2cc241fd2d01f0a7d_96.webp" width="20" height="20" align="absmiddle"> [Create Pipes n Physics](https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/create#create-pipes-n-physics) | – | 1.21.1 | – |
| <img alt="Sophisticated Backpacks icon" src="https://cdn.modrinth.com/data/TyCTlI4b/e31c7e2f8769d317339e25b2a8d1b40fbf312729_96.webp" width="20" height="20" align="absmiddle"> [Sophisticated Backpacks](https://modrinth.com/mod/sophisticated-backpacks)<br><img alt="Sophisticated Storage icon" src="https://media.forgecdn.net/avatars/thumbnails/543/206/64/64/637872959580005837.png" width="20" height="20" align="absmiddle"> [Sophisticated Storage](https://modrinth.com/mod/sophisticated-storage) | – | 1.21.1, 26.1.x, 26.2 | – |
| <img alt="Kaleidoscope Cookery icon" src="https://media.forgecdn.net/avatars/thumbnails/1361/462/64/64/638884307253099520.png" width="20" height="20" align="absmiddle"> [Kaleidoscope Cookery](https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/kaleidoscope-cookery/) | every version (Refabricated) | 1.21.1 | yes |
| <img alt="Kaleidoscope Tavern icon" src="https://cdn.modrinth.com/data/r9RZvhiJ/b96860512d16e72dcaa30b15cb0b9b25c18a38e8.png" width="20" height="20" align="absmiddle"> [Kaleidoscope Tavern](https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/kaleidoscope-tavern) | every version but 1.21.11 (Refabricated) | 1.21.1 | yes |
| <img alt="Croptopia icon" src="https://media.forgecdn.net/avatars/thumbnails/308/636/64/64/637392485303151332.png" width="20" height="20" align="absmiddle"> [Croptopia](https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/croptopia) | 1.20.1, 1.21.1, 26.1.x, 26.2 | 1.21.1, 26.1.x, 26.2 | yes |
| <img alt="Rustic Delight icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2Ffoa4fGIH%2Feecc99e281522f2291081c48176f0faa84c107bc.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> [Rustic Delight](https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/rustic-delight) | 1.21.1 and newer | 1.21.1 | – |
| <img alt="Supplementaries icon" src="https://cdn.modrinth.com/data/fFEIiSDQ/e9f5f66fa3b67e54acb91258a1428d68311c58bc_96.webp" width="20" height="20" align="absmiddle"> [Supplementaries](https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/supplementaries)<br><img alt="Brewin' and Chewin' icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FhIu9KJTT%2Ff7c591a80046859d3d45c04ecbbc54d264483d5e.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> [Brewin' and Chewin'](https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/brewin-and-chewin)<br><img alt="Ocean's Delight icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FDGiq4ZSW%2F949ba66d6fffb5a984fbb70e3ef4a51f15be3191.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> [Ocean's Delight](https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/oceans-delight)<br><img alt="Let's Do: Farm &amp; Charm icon" src="https://cdn.modrinth.com/data/HJetCzWo/7c6c372629b3efa41409621631d60df12963f005_96.webp" width="20" height="20" align="absmiddle"> [Let's Do: Farm & Charm](https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farm-and-charm/)<br><img alt="Let's Do: Candlelight icon" src="https://cdn.modrinth.com/data/qwbArkQk/5e0770c8da0fab82a70bc9c3913c8d3996c53345_96.webp" width="20" height="20" align="absmiddle"> [Let's Do: Candlelight](https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farm-and-charm/candlelight)<br><img alt="Let's Do: HerbalBrews icon" src="https://cdn.modrinth.com/data/Eh11TaTm/cea48ad39e9323e9e0f5354ee1d4c160f46b50be_96.webp" width="20" height="20" align="absmiddle"> [Let's Do: HerbalBrews](https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/herbalbrews)<br><img alt="Let's Do: Beachparty icon" src="https://cdn.modrinth.com/data/GyKzAh3l/41b9b45c365ecd55aced04bcd22af93878f766a0_96.webp" width="20" height="20" align="absmiddle"> [Let's Do: Beachparty](https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/beachparty)<br><img alt="Let's Do: Vinery icon" src="https://cdn.modrinth.com/data/1DWmBJVA/029aec55be4d860ba0aede4939dd93332b6dafad_96.webp" width="20" height="20" align="absmiddle"> [Let's Do: Vinery](https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/vinery)<br><img alt="Spelunkery icon" src="https://cdn.modrinth.com/data/krskFMfA/465cfcd453c22ee5a09884ede98a0442e97658c5.png" width="20" height="20" align="absmiddle"> [Spelunkery](https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/spelunkery) | 1.21.1 | 1.21.1 | – |
| <img alt="Fruits Delight icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2Fg6sbyCTu%2F4ecc5d554f260b876d21c427aa6c2bdf4457fd5c.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> [Fruits Delight](https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/fruits-delight)<br><img alt="Hearth and Harvest icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2F8EEEXOzj%2Fe5d9aa8bd6bf5dcbd674f08b92957d4b001229e3.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> [Hearth and Harvest](https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/hearth-and-harvest)<br><img alt="Miner's Delight icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FqMxbM4BQ%2F0d6f967d3ad184dd296c62a9891e2b2b7d45f61d.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> [Miner's Delight](https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/miners-delight)<br><img alt="Cold Sweat icon" src="https://cdn.modrinth.com/data/uXhSmPjd/bf55420556c30d44d2f5cf7b8915705b9214b4ef.png" width="20" height="20" align="absmiddle"> [Cold Sweat](https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/cold-sweat)<br><img alt="Kaleidoscope Flora icon" src="https://media.forgecdn.net/avatars/thumbnails/2040/206/64/64/639247202160912813.png" width="20" height="20" align="absmiddle"> [Kaleidoscope Flora](https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/kaleidoscope-cookery/kaleidoscope-flora)<br><img alt="Kaleidoscope Chinese Food icon" src="https://cdn.modrinth.com/data/cuIIkdlx/748a556a3658f0a5f67068f4d8d5cf0041b794a7.png" width="20" height="20" align="absmiddle"> [Kaleidoscope Chinese Food](https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/kaleidoscope-cookery/kaleidoscope-chinese-food) | – | 1.21.1 | yes |
| <img alt="Cultural Delights icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FYttyNOFA%2Fd857243f0e7dedd3d7f552c4371326773629e42e.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> [Cultural Delights](https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/cultural-delights)<br><img alt="Expanded Delight icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2Fe9V6wFcR%2F4cbbace573b20628290929948a77c74d95ed7a70.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> [Expanded Delight](https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/expanded-delight)<br><img alt="Extra Delight icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FyRrY3XII%2Fcec2396cf9f6f74a5b0cff196301a4b8b8124e1e.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> [Extra Delight](https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/extra-delight)<br><img alt="No Man's Land icon" src="https://cdn.modrinth.com/data/kjZCvAn6/958489a1729e9e17a6a5a0728ef249236c07f7b3_96.webp" width="20" height="20" align="absmiddle"> [No Man's Land](https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/no-mans-land) | – | 1.21.1 | – |

Tested versions of each mod are in the [installation guide](https://whoisnighter.github.io/ThirstWasTaken2/docs/installation#compatible-mods).

## Configuration
* **File:** `config/thirstwastaken2.json`, or in game through <img alt="Mod Menu icon" src="https://cdn.modrinth.com/data/mOgUt4GM/5a20ed1450a0e1e79a1fe04e61bb4e5878bf1d20.png" width="20" height="20" align="absmiddle"> [Mod Menu](https://modrinth.com/mod/modmenu) (Fabric) or the Mods list (NeoForge and Forge).
* **Scope:** Gameplay settings are server-side and synced; HUD settings are per client.
* **Reference:** Every key is documented in the [configuration page](https://whoisnighter.github.io/ThirstWasTaken2/docs/configuration).

## Commands
Require permission level 2.

| Command | Description |
| :--- | :--- |
| `/thirst query <player>` | Show thirst and quenched values |
| `/thirst set <players> <thirst> <quenched>` | Set thirst and quenched values |
| `/thirst enable <players> <true\|false>` | Enable or disable thirst |

## For Developers

**Data Packs:** Give any item a thirst value with one JSON file in `data/<namespace>/thirstwastaken2/drinks/`, no dependency needed:

```json
{
  "values": {
    "mymod:lemonade": { "thirst": 6, "quenched": 8 },
    "mymod:iced_tea": { "thirst": 8, "quenched": 12 }
  }
}
```

**Java API:** `com.thirstwastaken2.api` reads and changes thirst, hooks drinking and reads water purity, with one signature on every loader and version:

```java
// Salty snacks restore no thirst.
ThirstEvents.DRINK.register((player, stack, drink) -> {
    if (stack.is(MyItems.SALTY_SNACK)) drink.cancel();
});

// Give a player a bottle of Pure water.
player.addItem(ThirstApi.waterBottle(ThirstApi.maxPurity()));
```

See the [data pack](https://whoisnighter.github.io/ThirstWasTaken2/docs/developers/data-packs) and [Java API](https://whoisnighter.github.io/ThirstWasTaken2/docs/developers/java-api) guides.

## Building
One source tree built for every version with [Stonecutter](https://stonecutter.kikugie.dev). Gradle downloads the Java version each node needs.

```bash
git clone https://github.com/whoisnighter/ThirstWasTaken2.git
cd ThirstWasTaken2
./gradlew buildAndCollect
```

Jars land in `build/libs/`, one per version and loader. Nodes are `26.3.x`, `26.2.x`, `26.1.x`, `1.21.11`, `1.21.1`, `1.20.1`, the first five but `1.21.11` again with `-neoforge`, and `1.20.1-forge`:

| Task | Command |
| :--- | :--- |
| Build one node | `./gradlew ":26.3.x:build"` |
| Dev client / server | `./gradlew ":26.3.x:runClient"` / `":26.3.x:runServer"` |
| In-game tests | `./gradlew ":26.3.x:runGametest"` |
| Regenerate data | `./gradlew ":26.3.x:runDatagen"` (Fabric nodes) |

Contributions are welcome, see [CONTRIBUTING.md](CONTRIBUTING.md). Nine languages are included; translation PRs are welcome too.

## Credits
* Based on [Thirst Was Taken](https://github.com/ghen-git/Thirst-Mod) by [ghen](https://github.com/ghen-git), under the MIT License.
* Hanging Pot models adapted from [Dehydration](https://github.com/Globox1997/Dehydration) by [Globox1997](https://github.com/Globox1997), under the GPL-3.0.
* Licensed under the [GPL-3.0](LICENSE). Full credits in [CREDITS.md](CREDITS.md).
