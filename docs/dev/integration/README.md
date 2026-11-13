# Integration plans

One plan per mod: what it does with water, the decisions taken and what was found in game. Once a plan
is built, how the integration works lives in its source directory's `AGENTS.md`; the plan stays as the
record. Small integrations with only thirst values in `ThirstConfig` have no plan.

| Folder | What is in it |
|---|---|
| [`lets-do/`](lets-do/) | The Let's Do collection, all on Architectury, all on the two 1.21.1 nodes only |
| [`cooking/`](cooking/) | Farmer's Delight addons, Kaleidoscope Cookery and Tavern, Croptopia and Bountiful Fares: cooking pots, kegs, vats, teapots, crafted dishes |
| [`storage/`](storage/) | Mods that move water in tanks, jars and upgrades |
| [`climate/`](climate/) | Mods that change the drain: temperature and seasons |
| [`world/`](world/) | Overworld overhauls: new biomes whose water we grade, and what they add to drink |

| Mod | Plan | Code | Nodes | State |
|---|---|---|---|---|
| Let's Do: Farm & Charm | [lets-do/FARM-AND-CHARM](lets-do/FARM-AND-CHARM-INTEGRATION.md) | `src/main/farmandcharm` | both 1.21.1 | done |
| Let's Do: Candlelight | [lets-do/CANDLELIGHT](lets-do/CANDLELIGHT-INTEGRATION.md) | `src/main/farmandcharm` | both 1.21.1 | done |
| Let's Do: HerbalBrews | [lets-do/HERBALBREWS](lets-do/HERBALBREWS-INTEGRATION.md) | `src/main/herbalbrews` | both 1.21.1 | done |
| Let's Do: Beachparty | [lets-do/BEACHPARTY](lets-do/BEACHPARTY-INTEGRATION.md) | `src/main/beachparty` | both 1.21.1 | done |
| Let's Do: Meadow | [lets-do/MEADOW](lets-do/MEADOW-INTEGRATION.md) | `src/main/meadow` | both 1.21.1 | done |
| Brewin' and Chewin' | [cooking/BREWIN-AND-CHEWIN](cooking/BREWIN-AND-CHEWIN-INTEGRATION.md) | `src/main/brewinandchewin` | both 1.21.1 | see plan |
| Cultural Delights | [cooking/CULTURAL-DELIGHTS](cooking/CULTURAL-DELIGHTS-INTEGRATION.md) | `src/main/culturaldelights` | `1.21.1-neoforge` | see plan |
| Fruits Delight | [cooking/FRUITS-DELIGHT](cooking/FRUITS-DELIGHT-INTEGRATION.md) | `src/main/fruitsdelight` | `1.21.1-neoforge`, `1.20.1-forge` | see plan |
| Youkais' Feasts | [cooking/YOUKAIS-FEASTS](cooking/YOUKAIS-FEASTS-INTEGRATION.md) | `src/main/youkaisfeasts` | `1.21.1-neoforge`, `1.20.1-forge` | see plan |
| Miner's Delight | [cooking/MINERS-DELIGHT](cooking/MINERS-DELIGHT-INTEGRATION.md) | `src/main/minersdelight` | `1.21.1-neoforge`, `1.20.1-forge` | built |
| Hearth and Harvest | [cooking/HEARTH-AND-HARVEST](cooking/HEARTH-AND-HARVEST-INTEGRATION.md) | `src/main/hearthandharvest`, `hearthandharvestforge` | `1.21.1-neoforge`, `1.20.1-forge` | done |
| Extra Delight | [cooking/EXTRA-DELIGHT](cooking/EXTRA-DELIGHT-INTEGRATION.md) | `src/main/extradelight` | `1.21.1-neoforge` | done |
| Croptopia | [cooking/CROPTOPIA](cooking/CROPTOPIA-INTEGRATION.md) | `src/main/croptopia` | 26.2, 26.1.x, 1.21.1 both loaders; 1.20.1 Fabric and Forge | built |
| Bountiful Fares | [cooking/BOUNTIFUL-FARES](cooking/BOUNTIFUL-FARES-INTEGRATION.md) | `src/main/bountifulfares` | both 1.21.1 | built |
| Kaleidoscope Cookery | [cooking/KALEIDOSCOPE-COOKERY](cooking/KALEIDOSCOPE-COOKERY-INTEGRATION.md) | `src/main/kaleidoscope` | see plan | see plan |
| Kaleidoscope Tavern | [cooking/KALEIDOSCOPE-TAVERN](cooking/KALEIDOSCOPE-TAVERN-INTEGRATION.md) | `src/main/kaleidoscopetavern` | `1.21.1-neoforge`, `1.20.1-forge`, every Fabric node but `1.21.11` | built |
| Sophisticated Backpacks and Storage | [storage/SOPHISTICATED](storage/SOPHISTICATED-INTEGRATION.md) | `src/main/sophisticated` | NeoForge | see plan |
| Create Pipes n Physics | [storage/PIPES-N-PHYSICS](storage/PIPES-N-PHYSICS-INTEGRATION.md) | `src/main/create` | `1.21.1-neoforge` | built; upstream #89, #90 open |
| Mekanism | [storage/MEKANISM](storage/MEKANISM-INTEGRATION.md) | `src/main/mekanism`, `mekanismforge` | `1.21.1-neoforge`, `1.20.1-forge` | built |
| Immersive Engineering | [storage/IMMERSIVE-ENGINEERING](storage/IMMERSIVE-ENGINEERING-INTEGRATION.md) | `src/main/immersiveengineering`, `immersiveengineeringforge` | `1.21.1-neoforge`, `1.20.1-forge` | built |
| Supplementaries | [storage/SUPPLEMENTARIES](storage/SUPPLEMENTARIES-INTEGRATION.md) | `src/main/supplementaries` | both 1.21.1 | see plan |
| Cold Sweat | [climate/COLD-SWEAT](climate/COLD-SWEAT-INTEGRATION.md) | `src/main/coldsweat`, `coldsweatforge` | `1.21.1-neoforge`, `1.20.1-forge` | see plan |
| Serene Seasons | [climate/SERENE-SEASONS](climate/SERENE-SEASONS-INTEGRATION.md) | `src/main/sereneseasons` | every node | see plan |
| Spelunkery | [world/SPELUNKERY](world/SPELUNKERY-INTEGRATION.md) | `src/main/spelunkery` | both 1.21.1 | see plan |
| No Man's Land | [world/NO-MANS-LAND](world/NO-MANS-LAND-INTEGRATION.md) | `src/main/nomansland` | `1.21.1-neoforge` | built |

On 1.20.1, Sophisticated, Supplementaries and Brewin' and Chewin' have Forge builds but are not
integrated there: they wait for someone to ask.

A new plan goes in the folder of its family, gets a row here and a row under "Where to look" in the
root `AGENTS.md`.
