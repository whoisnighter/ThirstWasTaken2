# Supplementaries integration plan

What ThirstWasTaken2 does and still has to do with
[Supplementaries](https://modrinth.com/mod/supplementaries) on **both loaders, on Minecraft 1.21.1
only**. This file is the order of work, what each step needs and what it is checked with; how the
finished parts work is in
[src/main/supplementaries/AGENTS.md](../../../../src/main/supplementaries/AGENTS.md).

Written on 2026-09-20 from Supplementaries `1.21.1-3.9.9` and Moonlight Lib `1.21.1-3.6.5`, read from
the `1.21.1` branch of [Supplementaries](https://github.com/MehVahdJukaar/Supplementaries) and the
`1.21` branch of [Moonlight](https://github.com/MehVahdJukaar/Moonlight), which is that exact release.

## Why 1.21.1, and why both loaders

Supplementaries' newest Minecraft version is **1.21.1**. It has no 1.21.11, 26.1, 26.2 or 26.3 build,
so `1.21.1` and `1.21.1-neoforge` are the only two nodes this can touch, and the 2026-09 changelog
line saying the Supplementaries integration awaits compatible releases stays true for every other
node. Both 1.21.1 uploads exist, Fabric and NeoForge, from the same source.

That makes this the **first optional integration that both loaders compile**. Everything it touches is
in Moonlight Lib, Supplementaries' required library, which is multi-loader: `SoftFluidTank`,
`SoftFluidStack` and `SoftFluid` are the same classes with the same signatures on Fabric and on
NeoForge. One source directory serves both, and since it is one Minecraft version it carries no
Stonecutter branch either. Create Fly (Fabric) and Sophisticated (NeoForge) each needed a directory
per loader because the mods themselves differ; this one does not.

It also needs no core-code fork, so it does not push against the rule in `../../../AGENTS.md` that 1.21.1 is
retired rather than forked for. If 1.21.1 is ever retired, this integration goes with it and nothing
else moves.

## Where water lives in Supplementaries

Supplementaries does not use Minecraft's fluids for its containers. It uses Moonlight's **soft fluid**
system, a data-driven registry (`moonlight:soft_fluid`) whose entries are JSON, counted in **bottles**
rather than millibuckets: one bottle, two to a bowl, four to a bucket. On NeoForge a bottle converts to
**250 mB**, the same serving the mod's own waterskin and bowls already move.

| Piece | What it is |
|---|---|
| `SoftFluid` | a registry entry: textures, colour, `containers` (filled item to empty item pairs), an optional `food`, and `preserved_components_from_item` |
| `SoftFluidStack` | a fluid, a count in bottles, and a `DataComponentPatch`. It implements `DataComponentHolder` |
| `SoftFluidTank` | what a block holds. `interactWithPlayer`, `fillItem`, `drainItem`, `tryDrinkUpFluid` |
| `ISoftFluidTankProvider` | the interface a block entity implements so a faucet can reach its tank |
| `FaucetBehaviorsManager` | the faucet's source and target list, first match wins |

Three blocks matter: the **Jar** (capacity from `JAR_CAPACITY`, drinking gated on `JAR_EAT`), the
**Goblet** (one bottle, drinking gated on `GOBLET_DRINK`) and the **Faucet**, which moves fluid between
a block behind it and a block below, vanilla water cauldrons included.

**`SoftFluidStack` already carries data components**, and `moonlight:water` already lists a third
party's thirst component in `preserved_components_from_item`:

```json
"preserved_components_from_item": ["thirst:purity"]
```

That is not this mod. It is the other Thirst mod, and it is the proof that the author treats this list
as the place a thirst mod's grade goes.

## Status

| # | Item | Kind | Status |
|---|---|---|---|
| 1 | Water keeps its grade through jars, goblets and faucets | bug | **Done**, both loaders |
| 2 | Sea water stays sea water | bug | **Done**, both loaders |
| 3 | Faucet and water cauldron keep the cauldron's grade | bug | **Done**, both loaders |
| 4 | Drink from a jar or a goblet | feature | **Done**, both loaders |
| 10 | Faucet on world water samples it | bug | **Done**, both loaders |
| 5 | Faucet fills and drains the hanging pot | feature | **Done**, both loaders |
| 6 | The mod's own bowls as containers Moonlight knows | data | **Done**, both loaders |
| 7 | The grade is visible: tooltip, tint, Jade | feature | **Done**, both loaders |
| 8 | Upstream: the two components in Moonlight's `water.json` | upstream | **Merged** (MehVahdJukaar/Moonlight#490), released in 1.21.1-3.8.2; our copies stay |
| 9 | Changelog and player docs | docs | **Done** |

Order as with Sophisticated: **the bugs first**, item 1 before the others because it is the root cause
of most of them, then the cheapest feature, then the rest. Item 8 runs in parallel and blocks nothing.
Item 9 last.

Everything is in, on both 1.21.1 nodes. How it works is in
[src/main/supplementaries/AGENTS.md](../../../../src/main/supplementaries/AGENTS.md); what each one turned
out to need is below. Item 10 was found while building items 1 to 4 and is a bug of the same family,
so it sits with them. Item 8 is a change to somebody else's repository and is only done when they
merge it; nothing here waits on that.

## The bugs, and the one cause under them

The seven finished items were checked in a real client on both 1.21.1 nodes with
[tools/agent/integrations/supplementaries.jsonl](../../../../tools/agent/integrations/supplementaries.jsonl), which pours a dirty
bottle, a salty one and a terracotta bowl through a jar and draws each back, pours a pure bottle into a
goblet and drinks it, and runs five faucets: two between cauldrons, one over a pool, one draining a
hanging pot and one filling another. Both nodes answer the same. The gametests, which run without
either mod, still pass on every node, which is the other half of the check.

With both mods installed today, **a jar launders water**. A dirty bottle poured into a jar and drawn
back out is a plain water bottle, which the mod reads as `defaultPurity`, Clean by default. The same
holds for the goblet, for a bucket, and for a faucet pointed at a cauldron. That is a free purification
chain built out of two decorative blocks, and it is worse than the Sophisticated Tank bug was, because
the jar is cheap and early.

Every part of it is one thing: **nothing carries the grade across the item to fluid boundary.**
`SoftFluidStack.fromItem` copies only the components in `SoftFluid.getPreservedComponents()`, and
`moonlight:water` does not list this mod's two. The same list is consulted when a filled item is made
again (`createFilledStacks`), and on NeoForge when a `SoftFluidStack` is converted to a `FluidStack`
and back (`SoftFluidStackImpl.fromForgeFluid` and `toForgeFluid`). One list, four paths.

Preserving the components is necessary but **not sufficient**, for two reasons that are this mod's own
rules and not Moonlight's fault:

- **A fresh container must write `water_salty: false`.** All 27 purification recipes match on it, so a
  bottle that leaves it out silently stops being cookable. That already went wrong once with looted
  bottles, and `PurificationGameTest` watches for it.
- **Sprites are part of the contract.** `WaterPurity.setQuality` runs `syncModel`; a bottle built by
  Moonlight through `PotionContents.createItemStack` never goes through it, so a salty bottle out of a
  jar would look like ordinary water.

So the fix is two injections, not one, and the second is where the mod's own rules are applied.

### 1. Water keeps its grade (done)

`SoftFluidMixin` on `getPreservedComponents`: when the fluid is water, return the entry's list plus
`thirstwastaken2:water_purity` and `thirstwastaken2:water_salty`. One return-value inject, body one
line, and it covers all four paths above at once, the NeoForge `FluidStack` bridge included. The
waterskin and the terracotta bowls already carry the item fluid capability on NeoForge
(`purity/AGENTS.md`), so that bridge is how a faucet reaches them.

`SoftFluidStackMixin` on `splitToItem`, the one choke point every container a tank fills comes out of:
run the resulting stack through `WaterPurity.setQuality` with the quality read off the fluid **before**
the call, since that call spends the serving and a stack spent to nothing reads as empty. That is what
writes `water_salty: false`, clears a grade off salt water and points the model at the right item
definition.

A third hook was needed that the plan did not foresee. `SoftFluidTankMixin` on `addFluid` stamps water
entering a tank with the grade it already reads as, before the tank decides whether it fits: water that
arrives without a grade would otherwise refuse to share a tank with water that has one, although both
read as `defaultPurity` everywhere else. It also comes down to the one component the tank compares on,
since a container arrives carrying both.

Every target is matched **by name alone**, with no descriptor, so nothing here depends on Loom
remapping Minecraft names inside a mixin annotation for a class that is not Minecraft's.

One helper, `SoftFluidQuality`, is the only place that maps a `SoftFluidStack` to a `WaterQuality` and
back, the way `WaterFluids` is for NeoForge `FluidStack`s.

**A consequence worth stating:** once the components are on the fluid, `isFluidCompatible` refuses to
mix grades, because it compares components. A jar holding Murky water will not take Clean water. That
is the same rule the NeoForge containers already keep, and the same rule pouring by hand keeps.

Checked with `../../../../tools/agent/integrations/supplementaries.jsonl` on both nodes: after a dirty bottle is poured in, the
jar's own block data reads `{components: {"thirstwastaken2:water_purity": 0}, id: "moonlight:water"}`,
and the bottle drawn back out of it is `water_purity 0, water_salty false`.

### 2. Sea water stays sea water (done)

A sea-water bottle is a plain `minecraft:potion` holding water, told apart only by `water_salty`.
`SoftFluidStack.fromItem` sees `Potions.WATER` and converts it to `moonlight:water`, so without item 1
sea water becomes fresh water in a jar, which is the one conversion the whole `WaterQuality` design
exists to prevent. Item 1 carries the flag, and the `toItem` half gives the bottle back its sprite and
keeps it out of the cooking recipes.

What still needs deciding: **whether the goblet and the jar should show sea water as turquoise**, as
the mod's own bowls do. That is item 7 and can wait; the data being right is what matters here.

Checked in the same script: a salty bottle poured into a jar leaves `{"thirstwastaken2:water_salty":
1b}` on the fluid, and the bottle drawn back out is salty and carries no grade at all.

### 3. Faucet and water cauldron (done)

`WaterCauldronInteraction` in Supplementaries is both a faucet source and a faucet target for vanilla
cauldrons, and it knows nothing about the `purity` blockstate value this mod adds:

- draining offers `MLBuiltinSoftFluids.WATER` with no components, so a dirty cauldron fills a jar with
  water that reads Clean;
- filling an empty cauldron sets `Blocks.WATER_CAULDRON.defaultBlockState()`, which drops the property
  back to `0`, unset, so whatever was poured in reads `defaultPurity`.

The cauldron is the one carrier whose quality is **not** a component, so item 1 does not reach it.
`WaterCauldronInteractionMixin` stamps the offer from `WaterPurity.storedQuality(state)` on the way out,
and on the way in writes `WaterQuality.worse(held, poured)` into the state, the same rule pouring by
hand and rain already keep. The class is package-private, so the mixin targets it by name with
`@Mixin(targets = "...")`.

Two alternatives were looked at and rejected. A **data faucet interaction**
(`data/<ns>/faucet_interactions/*.json`) does take priority over the built-ins, since
`FaucetBehaviorsManager.apply` registers data interactions before its own, but `DataFluidInteraction`
matches an exact blockstate through a `RuleTest` and replaces it with a fixed one, so covering three
levels times six purity values means eighteen entries that still drain the cauldron in one go instead
of a layer at a time, and it cannot fill at all. Registering a replacement through
`addRegisterFaucetInteractions` does not work either: listeners run **after** the built-ins and the
faucet takes the first match.

Checked in the same script, with two faucets: a full dirty cauldron drained into an empty one arrives
as `water_cauldron[level=3,purity=1]`, dirty, and a pure cauldron poured into a dirty one leaves it
dirty, which is `WaterQuality.worse` doing what pouring by hand does.

### 10. Faucet on world water samples it (done)

Found while building the four above, and a bug of the same family rather than a feature. A faucet
pointed at a water source block in the world offers plain water, which the tank then reads as
`defaultPurity`, so a faucet over a swamp fills a jar with Clean water where a bottle filled there by
hand comes out Dirty. Nothing here made that worse, but item 1 is what makes it stand out: every other
way into a jar now carries a real grade and this one does not.

`LiquidBlockInteractionMixin` stamps what `getProvidedFluid` offers with `WaterPurity.sampleAt`.
**Sampling must never land on a tick path**, and a faucet runs on one, so the answer is kept for 100
ticks per position, the same span Create's pumps and Sophisticated's use.

This copy of `SampledWater` remembers a few positions rather than one. The other two remember one each
because a pump is one machine; here a single behaviour serves every faucet in the world, and two
faucets over different water would throw the one remembered answer away and take it again every tick.
It hangs off the behaviour, which Supplementaries builds again on every data pack reload, so nothing
outlives a world.

Checked in the script with a pool whose biome is pinned to plains by `/fillbiome`, so both nodes'
worlds score it the same: 55 for an ordinary biome, less 5 for being above y 100, which is grade 1.
That is not `defaultPurity`, so the cauldron reading grade 1 tells a real sample apart from the
fallback.

## The features

### 4. Drink from a jar or a goblet (done)

**Today you cannot drink water from either.** `SoftFluidTank.tryDrinkUpFluid` runs only when
`containsFood()`, which asks the entry for a `food` item, and `moonlight:water` has none. Only potions
and soups can be drunk. For a thirst mod this is the obvious feature: a goblet on a table and a jar in
a cellar should be somewhere water is kept and drunk.

`SoftFluidTankMixin` on `tryDrinkUpFluid`, `@At("HEAD")` and cancellable: if the tank holds water, drink
one serving through the mod's own path, so the grade decides what it restores and one roll drives nausea
and poison exactly as drinking by hand does, then shrink the tank by one and return true. Injecting at
the head is deliberate: it runs before the `containsFood` test, so no `food` entry is needed and nothing
Moonlight does for other fluids changes.

Both call sites are already gated by Supplementaries' own config (`JAR_EAT`, `GOBLET_DRINK`), so a
server that turned jar eating off keeps it off. Drinking from a goblet awards Supplementaries' own
`nether/goblet` advancement, which stays as it is.

Open question: **whether a jar of sea water can be drunk at all.** Drinking by hand lets a player drink
it and be punished for it, so the consistent answer is yes.

Checked in the same script: a goblet filled from a Pure bottle takes thirst from 4 to 10 and is left
empty. Drinking a jar or goblet of sea water, and what a Murky one gives, are left to the manual pass
for now.

### 5. Faucet fills and drains the hanging pot (done)

The copper and iron hanging pots are the mod's own water block, and a faucet above one is the obvious
build. The pot keeps its quality in a blockstate with no block entity, so `HangingPotFaucet` is a
`FaucetSource.BlState` and a `FaucetTarget.BlState`. A serving of the pot is a bottle of soft fluid,
which makes the two counts the same number, and what the pot then holds is the pot's own:
`HangingPotBlock.withPoured` keeps the worse of the two grades and what has boiled so far, exactly as
pouring a container in by hand does.

The plan said this was the first item to **need an entry point**, and that it would cost two one-class
directories, one per loader, to add a listener from. It does not. `FaucetBehaviorsManagerMixin`
registers it where Supplementaries builds its own list, which is the same place at the same moment a
listener would run, so it inherits the same order, and order does not matter for a block no built-in
behaviour claims. The integration is still mixins only, and items 7 and 5 no longer make a case for an
entry point between them.

Checked in the script with two faucets: one drains a pot of dirty water into an empty cauldron, which
arrives dirty and leaves the pot empty, and one fills an empty pot from a Murky cauldron, which ends
with the pot full at that grade and the cauldron empty.

### 6. The mod's own bowls as containers Moonlight knows (done)

`moonlight:water`'s `containers` list is how Moonlight learns that an item is a water container. It
already names a handful of other mods' cups and buckets. `thirstwastaken2:terracotta_bowl` to
`thirstwastaken2:terracotta_water_bowl` joins them, and the mod's bowl now works in a jar, a goblet and
a faucet the way a vanilla bottle does.

**At one serving, not Moonlight's `BOWL`, which is two.** The plan said `BOWL`; every other part of the
mod says a terracotta water bowl holds one serving, and `WaterContainerFluids` on NeoForge already
moves it as 250 mB.

It is added through Moonlight's own protected call, from `SoftFluidInternal`, where a load finishes:
the list has to be written **before Moonlight builds the map from item to fluid**, which is what
decides whether an item can be poured out at all. That is also the one place the integration looks
water up by registry key, and both had to move there, twice:

- the first attempt added the bowl from `SoftFluid.afterInit`, which is where Moonlight adds the
  buckets of a fluid's equivalents. It never fired, because it asked the entry whether it was water and
  a soft fluid's account of itself is the `c:water` fluid tag, which is not bound that early;
- the second looked water up by key at the head of the post-init instead, which broke every jar in the
  game. Moonlight remembers the holder it resolves for a key, so a key looked up before the registry
  has finished is remembered wrong for the rest of the load.

Water is now looked up once, where the item map is built, and the entry is **marked** rather than
remembered in a field of this mod's own: a client and the server it plays on each load the registry
for themselves, so there are two water entries and a single field can only be right about one of
them. That was the third failure, and it is what `WaterSoftFluid` exists for.

Checked in the script: a Murky terracotta water bowl poured into a jar leaves the jar holding Murky
water and the player holding an empty terracotta bowl, and using it again fills the bowl back up
Murky.

The waterskin is deliberately left out: it holds three servings with a fill level of its own, and
`FluidContainerList` maps one empty item to one filled item with a fixed capacity, which cannot express
it. On NeoForge the waterskin is already reachable through its fluid capability, so the faucet path
covers it there anyway.

It is deliberately **not** a data pack copy of `moonlight:water`. The entry is a whole registry object,
so shipping our own `data/moonlight/moonlight/soft_fluid/water.json` replaces Moonlight's in full,
which would silently drop every other mod's containers the day Moonlight adds one, and which pack wins
is not something to rely on. Item 8 is where that file should change, upstream.

### 7. The grade is visible (done)

Three small pieces, none of them load bearing:

- **the tooltip** of a jar broken while it held water, which keeps what it held in a
  `SOFT_FLUID_CONTENT` component. `SoftFluidTankViewMixin` puts the grade line under Supplementaries'
  own fluid line, so the item reads `Water: 4 mBtl` then `Dirty`. The goblet keeps nothing when broken,
  so there is nothing to say about its item;
- **the colour** of the water in a placed jar or goblet, by grade rather than by biome, through
  Moonlight's own still-colour call rather than by replacing textures. The five colours are read off
  the terracotta water bowl's sprites, so a jar of water looks like a bowl of the same water. **Not**
  the tooltip palette: that one is tuned for a dark tooltip and puts sea water in pale cream, which is
  right for a line of text and wrong for a block of water;
- **Jade**, which already covered world water, cauldrons and the hanging pots and now covers a jar and
  a goblet too.

Jade's part was going to be a second plugin, and is not. A second plugin means a second entry in Jade's
settings, which means a second name translated into nine languages, for a line that says exactly what
the first one says. Instead `client/compat/JadeIntegration` gained a list of container readers, which
is a hook that names no foreign class, and the integration adds one.

That leaves **one entry point** for the whole integration, `SupplementariesJade`, and Jade's is the
right one to borrow: it is called on a client exactly when there is a Jade to show anything. On Fabric
it costs one line in the manifest, on NeoForge nothing at all.

The tint and the Jade line are the two things a picture checks better than a number, so the script
leaves two screenshots behind: five jars side by side, one of each grade and one of sea water, with
Jade naming the one under the crosshair. The tooltip is a number after all: `client.tooltip` learned to
read the stack a player is holding rather than build one from an item id, since an id carries no
components and a grade only exists on a real stack.

## Upstream and docs

### 8. Upstream: the two components in Moonlight's `water.json` (merged)

A pull request to Moonlight adding `thirstwastaken2:water_purity` and `thirstwastaken2:water_salty` to
`preserved_components_from_item`, and the terracotta bowl pair to `containers` at a capacity of one
bottle. The file already carries
`thirst:purity` and seven other mods' containers, so this is a change the author has taken before.
`CodecUtils.lenientHomogeneousList` is what parses both fields, so entries for mods that are not
installed are skipped rather than failing the load.

Opened as [MehVahdJukaar/Moonlight#490](https://github.com/MehVahdJukaar/Moonlight/pull/490) against
the `1.21` branch and merged on 2026-11-04. 1.21.1-3.8.2 (2026-11-05) is the first release certain to
carry it; 3.8.1 went out 25 minutes after the merge and was not checked. 3.8.3, which the build pins,
has all of it in `water.json`. It is one
file: the two components added to `preserved_components_from_item` beside `thirst:purity`, and the
terracotta bowl pair added to `containers` at a capacity of one bottle.

If it is merged, the `getPreservedComponents` half of item 1 and the container half of item 6 can be
deleted. **Do not delete them when it merges, delete them when the oldest Moonlight the build accepts
has it**, since a player may have any release installed. The water mark stays either way: it is what
tells the two registry entries apart, and every hook that has an entry rather than a stack needs it. If it is not, nothing is blocked. **Do not wait on it**, and do not make a
released version depend on a Moonlight release that does not exist yet.

### 9. Changelog and player docs (done)

A CHANGELOG entry, the installation page's compatible-mods list, a section on the feature page that
explains where water keeps its grade, and the Modrinth and CurseForge pages, in the plain style
`../../../AGENTS.md` and the `write-docs` skill ask for.

The line in the 2026-09 changelog listing Supplementaries among the integrations awaiting a compatible
release is left as it was: it was true of that release, and a changelog is what happened rather than
what is true now.

## What the build needs

### Dependencies

Two new keys, in **both** 1.21.1 tables of `../../../../stonecutter.properties.toml`, since the whole integration is
gated on `deps.supplementaries` being set:

```toml
deps.supplementaries = "<Modrinth version id>"
deps.moonlight       = "<Modrinth version id>"
```

Both are **pinned by Modrinth version id, not version number**, for the reason AppleSkin is: the Fabric
and NeoForge uploads share one version number. For `1.21.1-3.9.9` and `1.21.1-3.6.5` those are
`h8FyJ2as` and `Grv7RNwq` on Fabric, `WrZWfRjP` and `yYx5Qs1i` on NeoForge, each with the version number
written in the comment above it, which `update_mc_deps.py` reads back. Both need adding to
`MODRINTH_DEPS` in `../../../../.github/scripts/update_mc_deps.py` with `by_id=True`, or the daily dependency PR
will never offer them.

Moonlight is named separately because Supplementaries' jar does not contain it, and every class this
integration touches is Moonlight's.

On Fabric both have to be **remapped mods** (`modCompileOnly`) rather than plain libraries the way
Create Fly is, since they are mixed into and a published Fabric jar is in intermediary names. That
brought one surprise: **Loom does not unpack a dependency's nested jars into a run**, which is why
Cloth Config is already named by hand for AppleSkin, and Moonlight bundles CodecUI and reads it on its
first line. CodecUI is published nowhere else, so it is taken out of Moonlight's own jar, while the
build is configured rather than by a task: Loom resolves the mod configurations then, and a jar written
afterwards is not there to be remapped.

### Source sets

```
src/main/supplementaries/java/com/thirstwastaken2/supplementaries/     both 1.21.1 nodes
  SupplementariesPresence     the gate: a classpath probe for each of the two mods
  SupplementariesMixinPlugin  applies each mixin only where its target is installed
  SoftFluidQuality            the only map between SoftFluidStack and WaterQuality
  WaterSoftFluid              the mark that says which registry entry is water
  SoftFluidDrinking           a serving out of a jar or a goblet
  CauldronQuality             what a faucet and a water cauldron say about the water between them
  HangingPotFaucet            the hanging pots as a faucet source and target
  SampledWater                the grade of world water a faucet draws, kept for a few seconds
  mixin/SoftFluidMixin                  the two components onto water, for Moonlight's own conversions
  mixin/SoftFluidStackMixin             the mod's own stamping rules on every container a tank fills
  mixin/SoftFluidTankMixin              what goes into a tank, and drinking out of one
  mixin/SoftFluidInternalMixin          where a load finishes: the water mark and the bowl
  mixin/FluidContainerListAccessor      the one protected call that adds a container to a fluid
  mixin/WaterCauldronInteractionMixin   the faucet and the cauldron's purity
  mixin/LiquidBlockInteractionMixin     the faucet and water in the world
  mixin/FaucetBehaviorsManagerMixin     registers the hanging pots with the faucet
src/main/supplementaries/resources/
  thirstwastaken2.supplementaries.mixins.json
src/client/supplementaries/java/...     item 7 only: tooltip lines and the fluid tint
```

`../../../../build.gradle.kts` and `build.neoforge.gradle.kts` each add these directories and append the mixin
config to the built manifest when `deps.supplementaries` is set, the way both already do for Create Fly
and Sophisticated. The Fabric side also needs the mods on the `runClient` classpath; the NeoForge side
the same through `clientRunMods`.

### The entry point, and why nothing needs one yet

A Fabric entrypoint class must implement `ModInitializer` and a NeoForge one must be annotated `@Mod`,
and both name their loader, so neither can live in a directory both loaders compile. Nor can
`../../../../src/main/java` call into this directory, since the nodes without Supplementaries would then not
compile.

**Everything that changes what water does is mixins only**, reached through the mixin config and the
config plugin. Item 5 was expected to need an entry point, since it registers a behaviour with the
faucet, and it would have cost two one-class directories, `src/main/supplementaries-fabric` and
`src/main/supplementaries-neoforge`, each a shim calling one `init()`. Registering from a mixin into
the place Supplementaries builds that list costs nothing and happens at the same moment, so it did not.

Item 7's Jade half does need one, and borrows Jade's: `SupplementariesJade` in
`../../../../src/client/supplementaries`, named in the Fabric manifest's `jade` entrypoint and found by its
annotation on NeoForge. Both happen whether or not Supplementaries is installed, so it asks the gate
before it loads anything of Moonlight's. Jade is a client-only dependency, which is why that one class
compiles with the client rather than with `main` like the rest of the integration.

### How it stays optional

The same three layers as `../../../../src/main/create` and `src/main/sophisticated`:

1. **Build.** Nothing is compiled and no mixin config is named unless `deps.supplementaries` is set, so
   the other eight nodes are untouched.
2. **Runtime gate.** `SupplementariesPresence` asks `Loader.isModLoaded` for both mod ids and then
   probes for `SoftFluidTank`, since Supplementaries can be present with a Moonlight too old to have
   it. A failed probe logs a warning and skips, rather than crashing on a missing mixin target.
3. **Mixin plugin.** `SupplementariesMixinPlugin` asks the gate before a single Moonlight class is
   named.

No hard dependency, ever, and the two 1.21.1 jars must behave identically with neither mod installed.

## Risks

- **This mixes into a library, not a mod.** Moonlight is a dependency of many mods, not only
  Supplementaries. The two mixins from item 1 change what every soft fluid tank does with water, so a
  jar from some other Moonlight mod inherits the behaviour too. That is the right answer for a thirst
  mod, but it needs saying out loud, and it is why the gate checks Moonlight rather than only
  Supplementaries.
- **Two moving targets.** Supplementaries and Moonlight release often and together, and Moonlight's
  1.21.1 branch carries `@Deprecated(forRemoval = true)` methods in exactly the classes this touches.
  Pinned ids and the runtime probe keep a crash from reaching a player, but this needs rebuilding more
  often than the Sophisticated integration does.
- **Loom remapping.** Mixins into a remapped mod on Fabric need that mod on the compile classpath for
  the refmap to resolve. It cost nothing in the end because every target is matched by name alone: the
  remapped jar keeps `method=["tryDrinkUpFluid"]` as written while the handler's own signature comes
  out in intermediary, which is exactly what is wanted. **A descriptor in a mixin annotation would put
  that back on the table**, so keep matching by name here.
- **The experimental settings prompt.** With these mods on the classpath a Fabric dev client calls
  every world experimental and stops in front of one, so `-Pquickplay` and `-Pagent` went nowhere until
  the driven client learned to press through it. That is `ClientWindow.passWorldPrompt`, and it is
  loader independent, so a NeoForge client that starts doing the same is already covered. Nothing about
  it reaches a player's game.
- **No gametest.** Gametests run without Supplementaries on the classpath, so every check above is an
  agent script in a real client, on both 1.21.1 nodes, as with Sophisticated and Farmer's Delight.
  `runGametest` must keep passing unchanged, which is itself the check that the mod is unaffected when
  the mods are absent.
- **Anything looked up by registry key is looked up late.** Moonlight remembers the holder it resolves
  for a key, per registry, so a key asked for before a load has finished is remembered wrong for the
  rest of that load, and a wrong `moonlight:water` breaks every jar in the game. The integration asks
  once, where the item map is built, and marks the entry it gets. Cost most of an afternoon; do not
  move it earlier.
- **A client and its server each load the registry.** There are two of every soft fluid entry, even in
  singleplayer, so nothing about one of them may be kept in a field of this mod's own.
- **Licence.** Supplementaries and Moonlight are under the Supplementaries Team License. Compiling
  against them and shipping nothing of theirs is fine; nothing of theirs may be bundled or
  redistributed, and the dependency stays a Modrinth coordinate.

## Not doing

- **Supplementaries' other blocks.** Soap, urns, ash, pancakes, sugar cubes and bamboo spikes have
  nothing to do with water quality, and Supplementaries registers no food or drink item that would want
  a value in `ThirstConfig`.
- **Purifying water inside a jar.** Purification is 27 cooking recipes over item stacks. A jar over a
  campfire is a different mechanism and would need its own balance pass; the Copper Distiller is where
  that conversation belongs.
- **Lumisene and Supplementaries' other fluids.** Not water, no grade, nothing to keep.
- **Forge.** Supplementaries still publishes a Forge 1.21.1 jar. This mod does not, and that does not
  change.
