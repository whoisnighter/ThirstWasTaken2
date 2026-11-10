# Kaleidoscope Cookery integration plan

What ThirstWasTaken2 should do with Kaleidoscope Cookery (mod id `kaleidoscope_cookery`): the
official [Kaleidoscope Cookery](https://modrinth.com/mod/kaleidoscope-cookery) on **NeoForge 1.21.1**,
and the unofficial
[Kaleidoscope Cookery Refabricated](https://modrinth.com/mod/kaleidoscope-cookery-refabricated) on the
**Fabric** nodes. This file sets the order of work, what each step needs and how each one is checked.
Once the work is built, how it works goes in `src/main/kaleidoscope/AGENTS.md`.

Written on 2026-09-22 from:

- the official repository [KaleidoscopeMods/KaleidoscopeCookery](https://github.com/KaleidoscopeMods/KaleidoscopeCookery),
  branch `1.21.1-neoforge` (`1.5.0-neoforge+mc1.21.1`);
- the port [NightEpiphany/KaleidoscopeCookery](https://github.com/NightEpiphany/KaleidoscopeCookery),
  branches `1.21.1-fabric`, `1.21.11-fabric`, `26.1.2-fabric`, `26.2-fabric` and `26.3-fabric`.

## Which build for which node

The official Fabric build stopped at `1.0.1-fabric+mc1.21.1` (2025-07-28), with no teapot and nothing
added since. **It is not supported.** Refabricated is a fork of the same code, published with the
official team's permission (shown on its Modrinth page), under the **same mod id**, the same package
(`com.github.ysbbbbbb.kaleidoscopecookery`) and the same licences. It is kept up to date with the
NeoForge release.

| Node | Mod | Version | Modrinth id | Teapot | Dripstone into teapot |
|---|---|---|---|---|---|
| `1.21.1-neoforge` | official | `1.6.0-neoforge+mc1.21.1` (published 2026-09-22, same targets as 1.5.0) | `g8w0b2ly` | yes | yes |
| `1.21.1` | Refabricated | `1.6.0.8-fabric+mc1.21.1` | `AQoKaczp` | yes | yes |
| `1.20.1-forge` | official | `1.6.0-forge+mc1.20.1` | `Ghp0qCKY` | yes | yes |
| `1.20.1` | Refabricated | `1.6.0.8-fabric+mc1.20.1` | `V20zzvcs` | yes | yes |
| `1.21.11` | Refabricated | `1.3.0.10-fabric+mc1.21.11`, hotfixes only from 1.3.0.10 on ("no longer update future contents") | `T5QYqq9P` | yes | no |
| `26.1.x` | Refabricated | `1.6.0.8-fabric+mc26.1.2` | `ay4H3A23` | yes | no |
| `26.2.x` | Refabricated | `1.6.0.8-fabric+mc26.2` | `qDGUWEp4` | yes | no |
| `26.3.x` | Refabricated | `1.6.0.8-fabric+mc26.3` | `t4YYs2U3` | yes | no |
| `26.x-neoforge` | none | — | — | — | — |

The official mod has no NeoForge build past 1.21.1, so the NeoForge nodes past 1.21.1 get nothing.

**Why one integration can serve six builds.** The same class and method names were checked on all six
branches:

- `StockpotBlockEntity.addSoupBase` / `removeSoupBase(Level, LivingEntity, ItemStack)`;
- `TeapotBlockEntity.addTeaFluid` / `removeTeaFluid(Level, LivingEntity, ItemStack)` and `getDrops()`;
- `ItemUtils.getItemToLivingEntity(LivingEntity, ItemStack[, int])`, plus
  `giveItemToPlayer(Player, ItemStack[, int])` on Fabric.

**Both loaders hand back the filled bucket the same way**, through `ItemUtils.getItemToLivingEntity`:
the stockpot calls it directly in `removeSoupBase`, and the teapot through `FluidUtils.fillItem`. The
fluid API underneath differs (NeoForge `IFluidHandler`, Fabric Transfer API with their own
`CustomFluidTank`), but the integration **never touches it**. It reads the grade off the bucket before
it goes in, and stamps the bucket that comes out.

What differs between versions is only `saveAdditional` / `loadAdditional` (`CompoundTag` on 1.21.1,
`ValueOutput` / `ValueInput` from 1.21.11), how `getDrops` builds its tag (`CompoundTag` on 1.21.1,
`TagValueOutput` from 1.21.11), and `receiveDripstoneFluid`, which exists only on the two 1.21.1
builds.

## Where water lives in Kaleidoscope Cookery

| Block | How water gets in | How water gets out | What it keeps |
|---|---|---|---|
| **Stockpot** | `addSoupBase`: a bucket that `ISoupBase.isSoupBase` accepts (`Items.WATER_BUCKET`) | `removeSoupBase`: an empty bucket, before any ingredient goes in, gets `new ItemStack(Items.WATER_BUCKET)` | only `soupBaseId` |
| **Teapot** | `addTeaFluid`: an item holding **at least 1000 mB** of fluid (in practice a bucket); on 1.21.1 also `receiveDripstoneFluid` | `removeTeaFluid`: fills the item from a brand-new fluid stack with no components. On Fabric the result even goes through `getDefaultInstance()` | only `teaFluidId` |
| Teapot as an item | `getDrops` builds a **new** tag holding only `TeaFluidId` | placing it loads that data again | only `TeaFluidId` |

Both blocks keep **a fluid id and nothing else**, so what the mod knows about the water (the
`water_purity` and `water_salty` components) is lost on the way in. **Water gets laundered**: a Dirty
bucket poured in and taken back out comes out Clean, and sea water comes out fresh.

## Status

| # | Item | Kind | Nodes | Status |
|---|---|---|---|---|
| 1 | Build dependency and gate | build | the six in the table | **Done** (2026-09-22) |
| 2 | Thirst values for teas, milk tea, soups | data | all (config) | **Done** (2026-09-22) |
| 3 | Stockpot keeps the water's grade | bug | six | **Done** (NeoForge 2026-09-22, Fabric 2026-09-23) |
| 4 | Teapot keeps the water's grade (bucket, picked up, dripstone, scooped) | bug | six (dripstone: 1.21.1 only) | **Done** (NeoForge 2026-09-22, Fabric 2026-09-23) |
| 5 | Salt water in the teapot and the stockpot | decision | — | **Decided** (2026-09-22): as recommended |
| 6 | The grade is visible: Jade line on the stockpot and the teapot | feature | six | **Done** (NeoForge 2026-09-22, Fabric 2026-09-23) |
| 7 | Changelog and player docs | docs | — | **Done** (2026-09-22) |
| 8 | Nothing crashes without the mod: `checkOptionalSeam`, `-PwithoutOptional`, `boot.jsonl` | test | all | **Done** (2026-09-22), for every integration |

Item 8 came **first** and is in: the static check and the flag exist before a single line of the
integration. `checkOptionalSeam` was run against the 1.0.9 `SophisticatedClientEntrypoint` and fails
on it, naming `DrinkingUpgradeTab extends UpgradeSettingsTab`, the class that crashed the client; it
passes on all ten nodes as they are. When item 1 adds the dependency, its `runClientMod` call takes the
names `kaleidoscope-cookery`, `kaleidoscope-cookery-refabricated` and `kaleidoscope_cookery`, and
`KaleidoscopeMixinPlugin` and the Jade reader are roots the check already finds by themselves. See
[Testing that nothing crashes without Kaleidoscope Cookery](#testing-that-nothing-crashes-without-kaleidoscope-cookery).

**Items 3 to 6 were built on `1.21.1-neoforge` first, then turned on for the five Fabric nodes** on
2026-09-23. What Fabric needed beyond adding `Loader.FABRIC` to the row, and why, is in
[src/main/kaleidoscope/AGENTS.md](../../../../src/main/kaleidoscope/AGENTS.md): `remap = true` on every
injection that names Minecraft (Refabricated's 1.21.x jars are in intermediary), version forks for
`saveAdditional` / `loadAdditional`, `getDrops` and `pickupBlock`, a plugin check for methods only some
builds have (`receiveDripstoneFluid`, `giveItemToPlayer`), and `InventoryMixin`, because Refabricated's
teapot fills a bucket through the Fabric Transfer API, in the player's slot, and never through
`ItemUtils`. The agent script passes on all six nodes, the dripstone lines only on the two 1.21.1 ones.

**On `1.21.11` the script's teapot lines need a copy** (found 2026-10-09, Cookery 1.3.0.10).
Refabricated 1.3.0.x has no tea bags: barley tea brews from `minecraft:wheat_seeds`, and a recipe with
no `ingredient_count` there needs twelve (`DEFAULT_INGREDIENT_COUNT = 12`; the 1.6 recipes ask for one bag). A
teapot with no recipe for its ingredient throws it out, so with the script's one tea bag `freshBrews`
and `seaKeepsBag` failed. They were never a bug of ours: with twelve wheat seeds every teapot line
passes there, sea water brewing nothing and keeping its seeds. The script's header says how to make the copy.

Order of work: **build `1.21.1-neoforge` and `1.21.1` first** (one Minecraft version, both loaders, no
version branch), and get items 1–4 working there. Then widen to `26.3.x`, which has every version
difference at once. `26.1.x` and `26.2.x` then cost nothing more. `1.21.11` comes last, and only if its
frozen 1.3.0.9 answers the same way. Item 5 must be decided before items 3 and 4 are finished.

## 1. Build dependency and gate

**Done.** What was built, and how it stays optional, is in
[src/main/kaleidoscope/AGENTS.md](../../../../src/main/kaleidoscope/AGENTS.md). Differences from the plan
below: the mixin config ships with no mixins yet (items 3 and 4 add them); the plugin asks the gate for
each mixin's own target by name, so no per-mixin table is needed; Forge Config API Port is
`deps.forge_config_api_port` on `1.21.1` and `1.21.11`, and its nested Night Config is unpacked by the
same `nestedMods` that unpacks Moonlight's CodecUI. `update_mc_deps.py` got a `frozen` list for the
`1.21.11` pin, and accepts a version comment starting with `v`, as Forge Config API Port spells them.

Checked: `build` and `checkOptionalSeam` on the six nodes; `runGametest` unchanged; `boot.jsonl` on
`1.21.1` and `1.21.1-neoforge`, with the mod and with `-PwithoutOptional=kaleidoscope_cookery`.

### Dependencies

One key, `deps.kaleidoscope_cookery`, in `[neoforge."1.21.1"]` and in the five Fabric tables, each
pinned **by Modrinth version id** with the version number in the comment above it (the Fabric uploads
of different Minecraft versions share one version number). Nothing in the other NeoForge tables.

```toml
# [fabric."26.3.x"]
# Kaleidoscope Cookery Refabricated, the unofficial Fabric port with the official team's permission:
# the official Fabric build stopped at 1.0.1. See docs/dev/integration/cooking/KALEIDOSCOPE-COOKERY-INTEGRATION.md.
# 1.6.0.8-fabric+mc26.3, the Fabric upload.
deps.kaleidoscope_cookery = "t4YYs2U3"
```

Add to `MODRINTH_DEPS` in `../../../../.github/scripts/update_mc_deps.py`, with `by_id=True`, **under two project
slugs**: `kaleidoscope-cookery` for the NeoForge table and `kaleidoscope-cookery-refabricated` for the
Fabric ones. If the script only takes one slug per key, it needs a per-loader slug. Leave `1.21.11` out
of automatic updates, since that build will not change again.

- **Fabric**: `modCompileOnly` + `modLocalRuntime`. It gets mixed into, so it must be remapped, as
  Supplementaries is. On `1.21.1` and `1.21.11` it **requires Forge Config API Port** (`ohNO6lps`), which
  has to go on the `runClient` classpath. On 26.x it is marked optional; check whether it is nested in
  the jar, since Loom does not unpack nested jars into a run.
- **NeoForge**: `compileOnly` + `clientRunMods`.

### Source set

```
src/main/kaleidoscope/java/com/thirstwastaken2/kaleidoscope/       every node that sets the key, both loaders
  KaleidoscopePresence         the gate: isModLoaded, the lowest supported version, a probe per target
  KaleidoscopeMixinPlugin      applies each mixin only where the gate allows it
  ReturnedWater                the grade on its way back out: set by a remove call, used by ItemUtils
  BrewedWaterQuality           the only place that reads and writes the grade on a block entity
  mixin/StockpotBlockEntityMixin
  mixin/TeapotBlockEntityMixin
  mixin/TeapotDripstoneMixin   receiveDripstoneFluid, listed only on the 1.21.1 nodes
  mixin/ItemUtilsMixin         stamps a water bucket handed back while ReturnedWater is set
src/main/kaleidoscope/resources/
  thirstwastaken2.kaleidoscope.mixins.json
```

Like `../../../../src/main/supplementaries`, this is **one directory both loaders compile**. It names no loader and
no fluid API: every piece works on an `ItemStack` through `WaterPurity`, which is common code.
`build.gradle.kts` and `build.neoforge.gradle.kts` add the directory and the mixin config to the
manifest when `deps.kaleidoscope_cookery` is set.

**Version branches** are only where the target's signature changes, as the rules allow: the
`saveAdditional` / `loadAdditional` injection and the `getDrops` tag in `TeapotBlockEntityMixin` and
`StockpotBlockEntityMixin`. The one-line body passes on to `BrewedWaterQuality`, and if reading and
writing one int differs between `CompoundTag` and `ValueOutput`, that difference goes in `Vanilla`, not
in the integration. `ResourceLocation` → `Identifier` is already a replacement in
`../../../../stonecutter.gradle.kts`. The dripstone mixin gets its own class, so the other nodes do not need
`require = 0`.

### How it stays optional

The same three layers as `../../../../src/main/supplementaries`:

1. **Build.** Nothing is compiled unless `deps.kaleidoscope_cookery` is set, so the four NeoForge nodes
   past 1.21.1 are untouched.
2. **Runtime gate.** `KaleidoscopePresence`, written like `SupplementariesPresence`: **every answer is
   a resource lookup** (`getResource(".../TeapotBlockEntity.class")`), never `Class.forName`, which
   would load a mixin target before Mixin has transformed it. It names no class of theirs, no Minecraft
   class and no loader. The official Fabric 1.0.1 has the same mod id but no teapot, so "the teapot
   class is there" is also the version check: without it the whole integration is off and one warning
   is logged ("the official Fabric build is not supported; install Refabricated"). Then one probe per
   target; a failed probe logs and skips. Do not crash.
3. **Mixin plugin.** `shouldApplyMixin` asks the gate, per class, so a renamed teapot does not take the
   stockpot down with it.

Every target is matched **by name alone, with no descriptor**, as in Supplementaries, so it does not
matter that the Fabric jar uses intermediary names for Minecraft types.

**Checked with**: `runServer` on all six nodes, with and without the mod; with the official Fabric
1.0.1 on `1.21.1` (a warning, no crash); `runGametest` unchanged on every node; `checkLoaderSeam` and
`checkVersionSeam` pass.

## 2. Thirst values (data only, no class referenced)

**Done**, in `ThirstConfig.kaleidoscopeCookeryDrinks` / `kaleidoscopeCookeryFoods`, called from the
defaults and from `sanitize()`. The ids were read off the jars' item and lang files rather than `/give`:
the teacups are block items with the ids below, `biluochun` included, on every build but 1.21.11's
1.3.0.9, which has no `mystery_tea`, `butter_tea`, `clay_pot_milk_tea`, `laba_congee` or
`hot_dry_noodles` (an id a build lacks is simply never matched). The Refabricated builds add two bowl
soups the table did not have, `donkey_soup` and `tomato_beef_brisket_soup`, at 4 / 5. The `*_pot_soup`
dishes, `dough_drop_soup`, `four_joy_meatball_soup`, `spicy_blood_stew` and
`buddha_jumps_over_the_wall` are placed blocks, eaten off the block, so they are left out, as below.
`hot_dry_noodles` is a dry dish and is left out too. The gametest
`kaleidoscopeCookeryTeasAndSoupsAreMergedIntoAnOlderConfig` checks the defaults and the merge; drinking
one tea in a dev client is still to do.

Like Farmer's Delight: entries in `config/itemvalues/KaleidoscopeItemValues`, plus
`putIfAbsent` in `sanitize()` so existing config files pick them up. It is common code and the item ids
are the same on both builds (same mod id), so this reaches **every** node, including a node with no
integration and a player on the official Fabric 1.0.1. An id that does not exist is never matched.

**Why this cannot be left to keyword matching**: `enableKeywordMatching` is `false` by default, and
when it is on, `tea` matches `tea_egg` (a food) and misses `tieguanyin`, `biluochun`, `oolong` and
`sakura_fubuki`.

Teacups are drunk through `TeacupItem.finishUsingItem`, which runs inside `ItemStack.finishUsingItem`,
where `ItemStackMixin` already hands out thirst. **No hook is needed.** Soups are `BowlFoodOnlyItem`,
likewise.

Suggested values (to be balanced against `../../mechanics/WATER-REFERENCE.md`; one bucket of water
makes 4 cups):

| Item | Thirst | Quenched |
|---|---|---|
| `barley_tea`, `tieguanyin`, `biluochun`, `oolong`, `sakura_fubuki`, `flower_tea` | 6 | 9 |
| `butter_tea` | 6 | 10 |
| `mystery_tea` (wrong recipe) | 3 | 3 |
| `clay_pot_milk_tea` | 8 | 12 |
| `pork_bone_soup` | 5 | 7 |
| `seafood_miso_soup`, `fearsome_thick_soup`, `lamb_and_radish_soup`, `wild_mushroom_rabbit_soup`, `pufferfish_soup`, `borscht`, `beef_meatball_soup`, `chicken_and_mushroom_stew`, `laba_congee` | 4 | 5 |
| `beef_noodle`, `hui_noodle`, `udon_noodle` | 3 | 4 |
| `tomato` | 2 | 3 |

**To check first**: the exact teacup item ids, read off `/give` in a dev client on `1.21.1-neoforge`
and on `26.3.x`. Also check whether the newer builds (1.5.0.x) add drinks this list does not have.

Not covered: dishes eaten straight off a placed block (`FoodBiteBlock.eat` calls
`player.getFoodData().eat` directly). Those are solid food and get no thirst, which is the right
answer.

**Checked with**: a gametest that asserts `ThirstApi` reads each id from the default config (the
gametest does not need the mod to be installed), plus drinking one tea in the dev client.

## 3. The stockpot keeps the water's grade (bug)

**Done on `1.21.1-neoforge`.** Built as below, except that `addSoupBase` and `removeSoupBase` are
wrapped whole with `@WrapMethod`, which is the `try/finally` the risks section asks for, and nothing
has to be cleared in `takeOutProduct`: the grade is only read while the pot is in `PUT_INGREDIENT`
with water as its soup base, and every way into that state writes it again. `ItemUtilsMixin` is a
`@ModifyVariable` at the head of both overloads, matched by name.

`StockpotBlockEntityMixin`:

- `addSoupBase`, `@Inject(HEAD)`: if the bucket is water, read `WaterPurity` from the stack **before**
  it is spent. Once the call returns true, keep it in `@Unique thirst$quality`.
- `saveAdditional` / `loadAdditional`, `@Inject(TAIL)`: one key, `thirstwastaken2:water_quality`,
  through `BrewedWaterQuality`. Version branch in the signature, as in item 1.
- `removeSoupBase`, `@Inject(HEAD)`: `ReturnedWater.set(thirst$quality)`. `@Inject(RETURN)`: clear it
  (and clear `thirst$quality` if the call succeeded).
- Clear the field wherever the block entity resets `soupBaseId` to `WATER` (`takeOutProduct`).

`ItemUtilsMixin` on `getItemToLivingEntity` (and `giveItemToPlayer` on Fabric), `@Inject(HEAD)`: if
`ReturnedWater` is set and the stack is a water container, `WaterPurity.setQuality(stack, quality)`.
That writes `water_salty: false` and points the model at the right sprite, the two rules that
`SoftFluidStackMixin` taught us not to skip. `ReturnedWater` is only set during a remove call, on the
server thread, so no other item passing through `ItemUtils` is touched.

The soup that comes out is **not** affected by the water's grade. The stockpot is not a purifier, and
food has no grade.

**Checked with** `tools/agent/integrations/kaleidoscope-cookery.jsonl` on `1.21.1-neoforge`, `1.21.1` and `26.3.x`:
pour a Dirty bucket in and take it out, and it is still Dirty; salty stays salty; the block entity
holds the key.

## 4. The teapot keeps the water's grade (bug)

**Done on `1.21.1-neoforge`**, with a fourth way the plan below missed: an empty teapot **item**
scoops water straight out of the world (`TeapotItem.use`, `BucketPickup.pickupBlock` then
`fillFluid`), writing only the fluid id, so a teapot dipped in the sea and emptied into a bucket
turned sea water fresh. `TeapotItemMixin` samples the water before it is picked up, on both sides so
the client agrees about a refusal, refuses sea water and adds the grade to the item's block entity
data. `getDrops` is a `@ModifyArg` on both `setBlockEntityData` calls; a finished teapot holds no
water, so the second one writes nothing.

Three ways water gets laundered:

1. **Bucket in, bucket out.**
2. **Picking the teapot up**: `getDrops` builds a new tag holding only `TeaFluidId`, so breaking the
   teapot and placing it again launders the water.
3. **Dripstone** (1.21.1 only): `receiveDripstoneFluid(Fluids.WATER)` should be `dripstonePurity`, as
   for a cauldron.

`TeapotBlockEntityMixin`:

- `addTeaFluid`, `@Inject(HEAD)`: read the quality off `itemStack` before it is emptied; keep it once
  the call returns true.
- `removeTeaFluid`: `ReturnedWater` at HEAD and RETURN, as with the stockpot. The bucket reaches
  `ItemUtils` through `FluidUtils.fillItem` on both loaders, so `ItemUtilsMixin` stamps it, and the
  Fabric `getDefaultInstance()` stripping the components does not matter, because the stamp comes after
  it.
- `getDrops`: add the key to the tag given to `BlockItem.setBlockEntityData` in the `PUT_INGREDIENT`
  branch. Version branch: `CompoundTag` on 1.21.1, `TagValueOutput` from 1.21.11.
- `saveAdditional` / `loadAdditional`: as with the stockpot.

`TeapotDripstoneMixin` (1.21.1 nodes only): `receiveDripstoneFluid`, `@Inject(RETURN)` when it returns
true and the fluid is water: `dripstonePurity`.

What the integration **cannot** see: a bucket from another mod that this mod does not stamp arrives
with no grade and reads as `defaultPurity`, the same limitation as everywhere else. The waterskin holds
750 mB, below the teapot's 1000, so it never reaches the teapot.

**Tea is made from boiled water, so it is always safe**: a teacup restores its fixed value from item 2
whatever the water's grade. That makes the teapot a purifier of sorts (1 bucket of Dirty water + 1 tea
bag + 240 ticks on heat = 4 safe cups). It is fair, since it costs a tea bag and a heat source, but it
has to be written into `../../mechanics/WATER-REFERENCE.md`.

**Checked with** the same script: a Dirty bucket in and out stays Dirty; break the teapot and place it
again, and the water is still Dirty; on 1.21.1 dripstone gives `dripstonePurity`.

## 5. Salt water (to decide before items 3 and 4)

**Decided as recommended** on 2026-09-22, and built. **Changed on 2026-09-25**: every integration now
takes sea water into its containers, as Brewin' and Chewin's keg does. The teapot takes it from a
bucket and from the world and hands it back salty, and brews nothing from it: `TeapotBlockEntityMixin`
skips the teapot's `tick` while it holds sea water, which is where brewing starts. The refusal and its
message, `thirstwastaken2.message.salt_water_refused`, are gone. What follows is the first decision,
kept as a record.

- **Teapot**: recommend **refusing** a salty bucket in `addTeaFluid` (`@Inject(HEAD)`, cancellable,
  return false), with a message in the action bar using the mod's own lang key
  (`thirstwastaken2.message.salt_water_refused`, in all nine lang files). The Cooking Pot refuses salt
  water too, and tea made from sea water that comes out safe is a free way to purify it.
- **Stockpot**: recommend **letting it through** (a salted soup is reasonable), keeping the flag when
  the bucket comes back out. The soup is not affected.

## 6. The grade is visible (optional)

**Done on `1.21.1-neoforge`**: `KaleidoscopeJade` in `src/client/kaleidoscope`. It names only the
mod's own `BrewedWater` interface, which the two block entities carry once their mixins apply, so it
needs no class of Kaleidoscope Cookery's and no hand-off class.

Both builds have their own Jade plugin for the stockpot and the teapot. Add a reader through
`JadeIntegration.addContainer`, the way `SupplementariesJade` does, from a class in
`src/client/kaleidoscope`, named in the Fabric manifest's `jade` entrypoint and found by its annotation
on NeoForge. It shows `Dirty` / `Sea water` under the stockpot and the teapot. Low priority; items 3 and
4 are what matter.

## 7. Changelog and player docs

**Done**: the CHANGELOG, `docs/docs/integrations/kaleidoscope-cookery.md`, both rows on the installation
page, and a card on the Modrinth and CurseForge pages. The Version Support table there is by Minecraft
version, not by mod, so it got no row; the card says where each part works.

A CHANGELOG entry, the compatible-mods list on the installation page, a
`docs/docs/integrations/kaleidoscope-cookery.md` page (thirst value table, water keeps its grade in the
stockpot and teapot, tea is safe, sea water is refused by the teapot), and the Modrinth and CurseForge
pages. Use the `write-docs` skill. State clearly: **on Fabric, install Refabricated; the official
Fabric 1.0.1 only gets the drink values.** Add a row to the version support matrix in
`../../../MODRINTH.md` and `docs/CURSEFORGE.md`.

## Testing that nothing crashes without Kaleidoscope Cookery

**This is a release blocker, not a nice-to-have.** 1.0.9.1 was a hotfix for exactly this: every
NeoForge client without Sophisticated Core crashed on startup.

### What went wrong in 1.0.9 and why no check saw it

1. **The JVM verifies a class whole when it links it**, before any code in it runs.
   `SophisticatedClientEntrypoint` asked the gate first, but its lambdas named `DrinkingUpgradeTab`,
   whose superclass is Sophisticated's. Verifying the entrypoint loaded that type, so the gate never got
   the chance to say no.
2. **No run looked at a client without the mod.** `runClient` always has every optional mod
   (`clientRunMods` on NeoForge, `modLocalRuntime` on Fabric); `runGametest` and `runServer` are
   servers and never load a `Dist.CLIENT` entrypoint. The `### Without the optional mods` checks in
   `../../MANUAL-TESTING.md` only remove Mod Menu, AppleSkin and Cloth Config.

### Which classes are loaded when Kaleidoscope Cookery is absent

These are the only ones that can crash a game without the mod, so each one must **name no type of
theirs anywhere**: not in a field, a signature, a method body, a lambda or a generic argument.

| Class | Why it is loaded anyway | What it may name |
|---|---|---|
| `KaleidoscopeMixinPlugin` | Mixin loads the plugin of every config listed in the manifest | `KaleidoscopePresence`, strings |
| `KaleidoscopePresence` | the plugin asks it | strings, `ClassLoader.getResource` |
| the Jade reader of item 6 | Jade loads every `jade` entrypoint (Fabric) and `@WailaPlugin` (NeoForge) whenever **Jade** is present, with or without Kaleidoscope Cookery. **This is the 1.0.9 trap exactly.** | the gate, and a call into another class that does the work |
| `ThirstConfig` entries | always | only id strings |

The mixin classes, `ReturnedWater`, `BrewedWaterQuality` and whatever the Jade reader calls are only
reached once the gate has said yes. Nothing outside `src/main/kaleidoscope` and
`src/client/kaleidoscope` refers to them; the manifests name the mixin config and the Jade class, and
nothing else.

### Checks

**A. Static, in CI (new, cheap, catches the class of bug).** A Gradle task `checkOptionalSeam` that
reads the compiled classes, not the source, so lambdas and generics count, and fails when:

- a class that is always loaded (a mixin plugin, a `*Presence`, anything named in a manifest
  entrypoint or carrying `@Mod` / `@WailaPlugin`) references a package outside
  `com.thirstwastaken2`, `net.minecraft`, `java`, the loader, or the Jade API;
- a class in `../../../../src/main/java` or `src/client/java` references `com.thirstwastaken2.kaleidoscope`.

Write it once for **every** optional integration, Sophisticated, Supplementaries, Create and Create
Fly included, and run it next to `checkLoaderSeam` in `build.yml`. It would have failed on 1.0.9.

**B. A client without the mod, on every node that sets the key.** A new Gradle flag
`-PwithoutOptional=<mod,...>` that leaves the named mods out of `clientRunMods` / `modLocalRuntime`,
and an agent script `../../../../tools/agent/smoke/boot.jsonl` that opens a world, waits a few seconds, opens the
inventory and quits. Run each combination on `1.21.1-neoforge`, `1.21.1` and `26.3.x`, and once on
the other three before a release:

| Run | Kaleidoscope Cookery | Jade | Expected |
|---|---|---|---|
| `runClient -PwithoutOptional=kaleidoscope_cookery,jade` | no | no | loads, no warning |
| `runClient -PwithoutOptional=kaleidoscope_cookery` | no | **yes** | loads; this is the 1.0.9 shape |
| `runClient` | yes | yes | loads; items 3 and 4 work |
| `runClient` with the official Fabric 1.0.1 swapped in (`1.21.1` only) | old | yes | loads, one warning, integration off |
| `runServer` with and without the mod | — | — | starts; the log has `ThirstWasTaken2 initialized` |

The same flag lets the existing Sophisticated and Supplementaries integrations be checked the same way,
which `../../MANUAL-TESTING.md` does not do today.

**C. The published jar, once per loader, before a release.** The dev run uses Mojang names on Fabric
and the release uses intermediary, and a mixin plugin sees a different classpath in a launcher. Put
`build/libs/thirstwastaken2-*-<node>.jar` into a clean instance **without** Kaleidoscope Cookery (plus
Fabric API on Fabric) and reach the title screen and a world. Then add Kaleidoscope Cookery and do it
again.

**D. Gametests, unchanged.** `runGametest` runs without the mod on every node and must keep passing
without any edit to a test. That is the check that the server side is untouched when it is absent.

### What goes into `../../MANUAL-TESTING.md`

Under `### Without the optional mods`, one line per integration and not just for this one: "a client
with Jade and without `<mod>` reaches a world", for Sophisticated Core (NeoForge), Supplementaries and
Moonlight, Create, Create Fly and Kaleidoscope Cookery. Tick them on the nodes the table above names.

## Risks

- **Two different upstreams.** The official mod (NeoForge) and Refabricated (Fabric) release
  separately, and a method can be renamed on one and not the other. Pin by id, match by name alone, and
  let the probe fail softly. **Every time a pin moves, re-run the agent script on that node.**
- **Refabricated is an unofficial port.** If it stops, the Fabric nodes keep the last build and nothing
  breaks. If the official team starts shipping Fabric again, compare the two and pick one; do not
  support both.
- **If a node is retired**, this integration does not hold it back: drop the key from the table along
  with the node.
- **1.21.11 takes hotfixes only.** Its 1.3.0.x is older than the others. If its methods differ at all, leave `1.21.11`
  out, rather than write a separate branch for it.
- **`ReturnedWater` is state across calls.** It must be cleared at RETURN every time, even when the call
  fails. Use `@Inject(RETURN)` on every return point, or `try/finally` through `@WrapMethod`, so a
  stamp never leaks onto an unrelated item.
- **No gametest for the behaviour.** Gametests run without the mod. Items 3 and 4 are checked with an
  agent script in a real client; `runGametest` passing unchanged is the check that the mod is unaffected
  when the mod is absent.
- **Licence.** Both builds are code BSD-3-Clause, assets CC BY-NC-SA 4.0. Compiling against them and
  shipping nothing of theirs is fine; nothing of theirs is bundled, and the dependency stays a Modrinth
  coordinate.

## Not doing

- **The official Fabric 1.0.1.** Values only, through the config; no mixins.
- **The NeoForge nodes past 1.21.1.** No build exists.
- **Purifying water in the pot, the stockpot or the steamer.** The mod's purification is 27 recipes and
  one balance pass. Boiling plain water in the teapot to get Pure water is a new mechanic and belongs
  with the Copper Distiller.
- **The bamboo tray's `wetting`, the enamel basin, the oil pot, lava and milk soup bases.** Not water, or
  they do not hold water.
- **Dishes eaten off a placed block** (`FoodBiteBlock`). Solid food, no thirst.
- **Forge 1.20.1, Fabric 1.20.1.** This mod does not ship for them.
