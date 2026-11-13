# Let's Do: Candlelight integration plan

What ThirstWasTaken2 should do with [Let's Do: Candlelight - Farm&Charm compat](https://modrinth.com/mod/lets-do-candlelight-farmcharm-compat)
(mod id `candlelight`, package `net.satisfy.candlelight`), the kitchen and dining addon of the Let's Do
collection: furniture, stoves, kitchen sinks, a large cooking pot, a cooking pan and restaurant dishes.
It **requires Farm & Charm** and runs on Farm & Charm's machines and recipe types, so this plan builds on
[FARM-AND-CHARM-INTEGRATION.md](FARM-AND-CHARM-INTEGRATION.md) and does nothing until that one's step 1
is done. Once the work is built, how it works goes where the steps below say.

Written on 2026-10-01 from:

- the repository [Let-s-Do-Collection/Candlelight](https://github.com/Let-s-Do-Collection/Candlelight),
  branch `1.21.1` at `c750a9e` (2026-09-19), which is the released `2.1.13`, built against Farm & Charm
  `1.1.20`;
- the Modrinth project `qwbArkQk`.

## Which build for which node

**Only the two 1.21.1 nodes**, as for Farm & Charm. The 1.20.1 line stopped at `2.0.5` (2025-08-05,
Fabric and Quilt only) and is not updated any more, so `1.20.1` and `1.20.1-forge` are left out on
purpose. There is no build newer than 1.21.1.

| Node | Build | Modrinth id | Requires on `runClient` |
|---|---|---|---|
| `1.21.1` | `2.1.13` Fabric | `DEKqjSw9` | Farm & Charm, Architectury API, Cloth Config: all already there after Farm & Charm's step 1 |
| `1.21.1-neoforge` | `2.1.13` NeoForge | `Cxu0FvZB` | Farm & Charm, Architectury API |
| every other node | none | — | — |

## What Candlelight is, for water

Candlelight has **no water logic of its own**. Every water path in it is a Farm & Charm class that
Candlelight registers or reuses:

| Candlelight block | Its class | What it means for us |
|---|---|---|
| **Kitchen sinks**, eleven of them (`cobblestone_`, `sandstone_`, `stone_bricks_`, `deepslate_`, `granite_`, `end_`, `mud_`, `quartz_`, `red_nether_bricks_`, `basalt_`, `bamboo_kitchen_sink`) | Farm & Charm's `SinkBlock` | the one block Farm & Charm itself leaves unregistered. See below: **an endless source of Clean water from nothing** |
| Stoves (one per stone, and bamboo) | `CStoveBlock extends` Farm & Charm's `StoveBlock` | Farm & Charm's `stove` recipes; covered by Farm & Charm's step 6 |
| `cooking_pot` (Large Cooking Pot) | its own `LargeCookingPotBlockEntity`, running Farm & Charm's `pot_cooking` recipes (`CookingPotRecipe`) | the same recipes and matcher as Farm & Charm's pot; covered by Farm & Charm's step 6 |
| `cooking_pan` | its own `CookingPanBlockEntity`, running Farm & Charm's `roaster` recipes | no water in any of them |

Candlelight's own recipes add to Farm & Charm's types (7 pot, 7 roaster, 7 stove, 7 crafting bowl).
**Only one takes water**: `mozzarella` in the Crafting Bowl, milk plus `#candlelight:water_bottles`
(`#c:water_bottles` and `minecraft:water_bucket`, so again the bucket only). Mozzarella is not a drink.

### The kitchen sink

Farm & Charm's `SinkBlock.useItemOn`, a two-block block with one `FILLED` boolean, as it was up to Farm
& Charm 1.1.27. From 1.1.28 the sink is Foundation's and holds a level; see step 8.


| Click on an empty sink with | Result |
|---|---|
| an **empty hand** | the sink fills, **from nothing** |
| a water bucket | the sink fills, an empty bucket back |
| a **glass bottle** | the sink fills and the bottle comes back: also from nothing |

| Click on a filled sink with | Result |
|---|---|
| a bucket | `minecraft:water_bucket`, unstamped |
| a glass bottle | a `minecraft:potion` of `Potions.WATER`, unstamped |

So a sink is an endless tap. Today everything it hands out is unstamped, which reads as
`defaultPurity` (Clean by default): **a Candlelight kitchen makes purifying water pointless.** It also
launders: a Dirty or salty bucket poured in comes back Clean. Both are worse than the Farm & Charm
trough, because the sink needs no water at all to start with.

## The drinks and foods

Candlelight has **no drinks**: its `wine_glass` is empty glassware, and the wine in its dish names
comes from other Let's Do mods (Vinery), which are not part of this plan. Only a few of its dishes are
wet enough to count. In `ThirstConfig` by id, a `candlelightFoods` merged with `putMissing`, like the
other addons. Proposed (thirst, quenched):

| Group | Items | Proposed |
|---|---|---|
| Soups, a bowl | `tomato_soup`, `mushroom_soup` | 4, 5, as Farmer's Delight's soups and Farm & Charm's |
| Salads, a bowl | `salad`, `beetroot_salad` | 4, 5, as Farmer's Delight's mixed salad |
| | `fresh_garden_salad` (a tray of four servings when placed) | 4, 5 per serving, if eating from the placed tray goes through the item; see step 3 |
| | `tomato_mozzarella_salad` | 3, 4 |
| Dessert | `chocolate_mousse` | 2, 3, as Farmer's Delight's glow berry custard |
| Left out | `mozzarella`, the pastas, `lasagne`, `bolognese`, roasts, steaks, `beef_wellington`, `pork_ribs`, `khinkali`, `chicken_teriyaki`, `omelet`, `tropical_fish_supreme`, the wine-cooked dishes (the wine is cooked off) | — |

The ids come from Candlelight's `ObjectRegistry`. Step 3 confirms each one resolves.

## Status

| # | Item | Kind | Nodes | Status |
|---|---|---|---|---|
| 1 | The mod on the `runClient` classpath | build | both 1.21.1 | **Done** (2026-10-01) |
| 2 | Thirst values for the dishes | data | all (config) | **Done** (2026-10-01), values as proposed |
| 3 | What happens to a grade at a sink, in game | investigation | both 1.21.1 | **Done** (2026-10-01) |
| 4 | The kitchen sink stops being a free Clean tap | decision, then code | both 1.21.1 | **Decided** (2026-10-01): (a), and **done** |
| 5 | Sea water and Candlelight's pot, stove and bowl | code | both 1.21.1 | **Done** (2026-10-01), with its own target |
| 6 | Nothing crashes without the mod | test | both 1.21.1 | **Done** (2026-10-01) |
| 7 | Changelog, player docs, store pages | docs | — | **Done** (2026-10-01) |
| 8 | Foundation's sink, with a tap and a level | re-check | both 1.21.1 | **Done** (2026-11-13): the mixin still holds, the script followed |

How it works now is in [src/main/farmandcharm/AGENTS.md](../../../../src/main/farmandcharm/AGENTS.md).

## 1. The mod on the `runClient` classpath (done)

`deps.candlelight` beside Farm & Charm's keys, pinned by Modrinth id, in `MODRINTH_DEPS`, and one
`runClientMod` line in each loader script. No row in the integration table and no directory of its
own: the code is in `src/main/farmandcharm`. Candlelight `2.1.13` runs with Farm & Charm `1.1.26`,
though it was built against `1.1.20`.

## 2. Thirst values (done)

`candlelightFoods` in `config/itemvalues/LetsDoItemValues`, checked by `farmAndCharmDrinksAreMergedIntoAnOlderConfig` beside
Farm & Charm's. `fresh_garden_salad` is eaten from the hand like the others: from thirst 4 it lands on 8.

## 3. Investigation (done)

In [tools/agent/integrations/farm-and-charm.jsonl](../../../../tools/agent/integrations/farm-and-charm.jsonl),
passed whole on both nodes on 2026-10-01:

| Case | Found |
|---|---|
| The eleven sink ids, Candlelight's cooking pot | all exist |
| A salty bucket on an empty sink | refused, the bucket kept |
| An empty hand, then a glass bottle, then a bucket | the sink fills from nothing; the bottle and the bucket are Murky |
| Nettle tea in the Large Cooking Pot, plain and salty bucket | tea from the plain one, nothing from the salty one |
| Tomato soup, fresh garden salad, chocolate mousse from thirst 4 | 8, 8 and 6 (10, 6 and 5 since the values were rebalanced to 6, 2 and 1 on 2026-10-21) |

## 4. Decision: the kitchen sink (done, (a))

`SinkMixin` on Farm & Charm's `SinkBlock`: a salty water bucket is refused at the head of `useItemOn`,
and whatever goes to the player through `Player.addItem` is stamped Murky if it is water, the same
`StandingWater.STANDING` as the trough. The empty-hand fill stays, as Candlelight meant it. The other
options, for the record: (b) a sink that holds only what is poured in, with a grade on its blockstate,
which changes how Candlelight's own block plays; (c) nothing.

## 5. Sea water in Candlelight's cooking (done)

The plan assumed Farm & Charm's step 6 would reach Candlelight through `GeneralUtil.matchesRecipe`. It
does not: the Large Cooking Pot has its own copy of the Cooking Pot's matcher. So
`SeaWaterIngredientMixin` names it as a fourth target, by string, and the plugin skips it when
Candlelight is absent. Candlelight's stoves extend Farm & Charm's and are covered by its target. The
Crafting Bowl's mozzarella is refused too, through Farm & Charm's bowl.

**From 2.1.14** (2026-10-08) the Large Cooking Pot extends Farm & Charm's `CookingPotBlockEntity` and
overrides only its lit and cooking properties and its extra output, so it has no `matchesInventory` of
its own and the Cooking Pot's target covers it. The plugin skips the pot's own target when its class
declares no `matchesInventory`, quietly, since that is the new shape and not a break. On Fabric that
check never ran up to 1.10.1: Loom writes the string target with slashes, the plugin looked it
up dotted, missed, and applied the mixin unchecked, which crashed every Fabric client at launch with
Candlelight 2.1.14 or 2.1.15 ([issue #56](https://github.com/whoisnighter/ThirstWasTaken2/issues/56)).
NeoForge passes the name dotted and only logged a warning. The plugin now turns slashes into dots, and
the injector has `require = 0`.

## 6. Optional seam (done)

`tools/agent/smoke/boot.jsonl` with `-PwithoutOptional=candlelight` comes up and stays up on both nodes,
with Farm & Charm still loaded, and so does `-PwithoutOptional=farm-and-charm`, which leaves both out.

## 7. Docs (done)

An addon page under Farm & Charm's, as Farmer's Delight's addons have, [integrations/farm-and-charm/candlelight](../../../docs/integrations/farm-and-charm/candlelight.md),
with a picture of a kitchen sink between a stove and counters; a changelog entry of its own; rows in
`docs/docs/installation.md`; Candlelight named in Farm & Charm's row on the Modrinth and CurseForge
pages and in their versions tables.

## 8. Foundation's sink (done, 2026-11-13)

From Farm & Charm 1.1.28 the kitchen sink is `net.satisfy.foundation.block.SinkBlock` (Foundation 1.0.6,
1.0.7 in Farm & Charm 1.1.29, 1.0.9 in Meadow 1.4.10, the same bytecode), with a `SinkBlockEntity` on the
lower half. `FILLED` is only read once and turned into a full basin. Read with `javap`:

| Click with | Result |
|---|---|
| an **empty hand** | opens or closes the tap; open, the basin rises a level every 60 ticks, up to 3, **from nothing** |
| a water bucket (or bottle), basin below 3 | pours it in: up to 3 (one level), an empty bucket (bottle) back |
| a glass bottle, basin 1 or more | one level, a `Potions.WATER` bottle |
| a bucket, basin 3 | all three, a water bucket |
| anything else, basin 1 or more | washes it, one level |

Every result still goes through `Inventory.add`, or `Player.drop` when full, and a salty bucket or bottle
still meets the head of `useItemOn`, so `SinkMixin` applies and works unchanged: what comes out is
Murky, sea water is refused. The script's sink cases were written for the old flag (an empty hand
setting `filled=true`) and failed only for that; they now read the block entity's `WaterLevel` and
`Open`, wait for the tap, and pour a plain bucket in to show it comes back Murky. Passed whole on both
1.21.1 nodes, Fabric with Foundation 1.0.9 added to `runClient` by hand (Loom does not load the copy
nested in Farm & Charm), NeoForge with the nested 1.0.7.

## Not planned

- **1.20.1, Forge, and any version past 1.21.1.** The 1.20.1 line is no longer updated.
- **Vinery, Bakery, Brewery and the other Let's Do mods**, whose wines, juices and beers are the real
  drinks of the collection. Each is its own plan if wanted.
- **The Cooking Pan.** Roaster recipes take no water.
- **Furniture** (tables, cabinets, the dinner bell): no water.
