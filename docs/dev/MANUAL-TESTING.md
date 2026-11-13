# Manual testing

What the gametests cannot see, written as a checklist to run by hand before a release. Everything
else is automated: see [src/gametest/java/AGENTS.md](../../src/gametest/java/AGENTS.md) for what the
gametests cover, and run them first.

```bash
./gradlew ":26.3.x:runGametest"
```

A gametest runs on a headless dedicated server with mock players. That rules out four kinds of
check, and every item below belongs to one of them:

| Why it is manual | What falls under it |
|---|---|
| **No client.** A gametest never opens a window, never renders, never receives a packet. | the HUD, sprites, tooltips as drawn, the config screen, anything synced to a client |
| **Mock players cannot be hurt.** They report creative mode, and vanilla refuses to damage them. | dehydration damage, the death message, armour against dehydration |
| **One world, one biome, one dimension.** Tests run in a flat overworld patch. | the Nether drain, climate, `nether_drink`, loot found in real structures |
| **Nothing on a real timer.** A test body runs inside one tick. | drain over minutes of play, peaceful refill over time, rain filling a cauldron in the open |

## How to run a pass

1. `./gradlew ":<node>:runClient"`, where `<node>` is `26.3.x`, `26.2.x`, `26.1.x`, `1.21.11`,
   `1.21.1` or `1.20.1`, one of the first five with a `-neoforge` suffix, or `1.20.1-forge`. The dev client already has AppleSkin, Cloth
   Config and Jade, and Mod Menu on the Fabric nodes. `26.3.x-neoforge` has no Cloth Config, which has
   no NeoForge build for 26.3, so AppleSkin's own settings screen is missing there.
2. Create a new **survival** world on **Normal**, cheats on. Keep the world per version; saves do
   not move between versions.
3. Run the agent scripts below, then work down the general checklist, then the section for that
   version.
4. Note the version, the date and anything that failed in the release PR. Results go there, not in
   this file.

Useful commands while testing:

```
/thirst set @s <thirst> <quenched>
/thirst enable @s false
/effect give @s minecraft:fire_resistance
/give @s thirstwastaken2:waterskin
/time set night
/weather rain
```

### Agent scripts

A check with an exact answer belongs to the agent client, not to a pair of eyes. These scripts live
in `tools/agent/`; its [AGENTS.md](../../tools/agent/AGENTS.md) lists them all and says which world
each needs.

| Script | Checks |
|---|---|
| `ui/hud-layout.jsonl` | thirst, food and air draw rectangles: same width, same right edge, air above thirst above food |
| `ui/hud-hidden.jsonl` | F1 stops the thirst row being drawn, and showing the HUD brings it back |
| `ui/hud-death-screen.jsonl` | the bar keeps being drawn on the death screen, with the values the player died with |
| `ui/config-screen.jsonl` | the config screen's page buttons, "Open thirstwastaken2.json", Done and Cancel |
| `gameplay/client-sync.jsonl` | what the client is told: `/thirst set`, the sprint gate at 6 and 7, rejoining, respawning, the Nether |
| `gameplay/parched.jsonl` | Parched from sea water through the real right click, its drain by level, its name in each language |
| `gameplay/waterskin-stack.jsonl` | a waterskin filled from a stack of bottles on the cursor puts the empty bottle in the inventory |
| `gameplay/loot-and-boil.jsonl` | water bottles in the seeded chests and barters, and `boil_water` from a furnace and a smoker, both boiling Clean |
| `gameplay/canteen.jsonl` | the Copper Canteen and Iron Flask boiling Clean in hand on a campfire, salt refused, and both crafting recipes |
| `gameplay/hanging-pot.jsonl` | a pot over a lit campfire boiling Clean, salt staying salty, the campfire taken away |
| `gameplay/distiller.jsonl` | the Copper Distiller placed, built from its parts, piped, cooled and distilling to Pure |
| `smoke/boot.jsonl` | a client without an optional mod comes up and stays up, see [Without the optional mods](#without-the-optional-mods) |

The fifth sync check, two clients each seeing only their own bar, needs `runServer`, `runManualA` and
`runManualB`; [.../dev/agent/AGENTS.md](../../src/dev/java/com/thirstwastaken2/dev/agent/AGENTS.md)
says how. `-Pagent` answers a file but reads none of its `expect` lines, so an unattended run is
checked afterwards:

```bash
python tools/agent/drive.py run/26.3.x/agent/client tools/agent/gameplay/loot-and-boil.jsonl --verify
```

Add `-Pdriven` when the agent client is doing the work and you want the machine back: the client opens
maximised and never takes the mouse pointer. Leave it off for the items below, which are played by
hand or by computer use (the `manual-testing` skill): without the grab there is no mouse look and no
click reaches the world.

```bash
./gradlew ":26.1.x-neoforge:runManualA" -Pdriven
```

## General checklist (every version)

### HUD

- [x] `/thirst set @s 13 0`: six and a half droplets, the half droplet on the left end.
- [ ] With quenched at 0, sprinting leaves the droplets whole until a point is spent, as vanilla's
      food bar does, and the bar shakes the way hunger does at zero saturation.
- [x] `/thirst set @s 20 20`: the quenched outline is drawn over every droplet, cyan by default.
- [x] Mod Menu → ThirstWasTaken2 → AppleSkin: Quenched Outline reads "Quenched Outline: Diamond"
      (the name once) and cycles Diamond, Ice, Gold, AppleSkin, Legacy and Off. The preview and the real
      bar change at once. Off removes the outline from the bar; the tooltip row falls back to the plain
      blue outline.
- [x] The bar is hidden in creative and spectator, and while riding a horse, pig or strider (a living
      mount's hearts take its place). It stays while in a boat or minecart.
- [x] With AppleSkin's exhaustion underlay on, a translucent strip grows under the thirst bar while
      sprinting; turning the AppleSkin option off removes it. Nothing drawn after it (air bubbles,
      the hotbar) is left tinted.
- [x] `/thirst enable @s false` hides the bar; `true` brings it back.

### Data pack thirst values

Not covered by the agent yet. Use a dedicated server (`runServer`) with a client joined to it, not
singleplayer, where the client shares the server's copy and would pass without any packet. Bread is in
neither config nor any `c:drinks` tag, so a data pack is the only thing that can give it a value. Add
one to the server's world, `world/datapacks/api-test/`, with a `pack.mcmeta` and
`data/test/thirstwastaken2/drinks/test.json` holding
`{"values": {"minecraft:bread": {"thirst": 1, "quenched": 0}}}`:

- [ ] After `/reload`, bread's tooltip shows a single half-droplet (AppleSkin installed), and eating it
      at 10 thirst leaves 11.
- [ ] Change the file to `"thirst": 3`, `/reload` again: the tooltip follows without rejoining.
- [ ] Quit and rejoin: the value is still there, so it is sent on join and not only on `/reload`.
- [ ] The server log says `Loaded thirst values for 1 items from data packs` at startup and on each
      `/reload`; the client log never says it.

### Tooltips

- [x] A water bottle, a filled bowl and a filled waterskin show a coloured grade line (Dirty, Murky,
      Clean, Pure) and two rows of droplet glyphs, not boxes or letters.
- [x] Turning Tooltip Droplets off removes both droplet rows and keeps the grade line.
- [x] The lines look the same in JEI, EMI or REI if one of them is installed.

### Jade

- [x] Looking at river water, sea water, a waterlogged block and a cauldron filled by rain shows the
      grade (or Salty) under the block name, in the grade's colour.
- [x] Placing mud next to the water being looked at lowers the grade within half a second.
- [x] Turning Water Purity off in Jade's plugin settings removes the line.
- [x] A hanging pot shows its grade, and nothing when it is empty.

### Hanging pots

Filling, drawing, boil times, topping up, rain and the Nether are gametests. What is left is how it
looks and sounds.

- [x] The copper pot's recipe shows in the recipe book once a copper ingot is held, the iron pot's
      once an iron ingot is.
- [x] Placed on a campfire, the pot hangs from its frame, with the chain drawn cut out rather than as
      black squares. Placed on stone, it stands on its own without the frame. The crossbar runs across
      the placing player's view. The iron pot is dark iron with the same frame and chain.
- [x] The water surface rises and falls with the level, and its colour matches the grade.
- [x] Over a lit campfire the water bubbles and steams, and turns Clean's blue with a short brewing
      sound when done.
- [ ] An iron pot holding six drinks looks a third, two thirds and full as it fills; a pot from an
      older save keeps its water.
- [x] In the Nether, pouring into a pot hisses and smokes.
- [x] Breaking the campfire drops the pot.

### Sprites and sounds

- [x] A filled bowl's water colour changes with its grade: four fresh colours and a sea colour.
- [ ] A waterskin's sprite shows empty, a third, two thirds or full, at any capacity, and its bar is
      coloured by grade.
- [x] Drinking a bowl or a waterskin plays the drinking animation and sound.
- [x] Crouch with an empty hand and use water at less than full thirst: the drinking sound is heard
      by the drinker.
- [x] Crouch with an empty hand and use water whose floor is out of reach (deep water, a waterfall):
      it is drunk all the same, and an item in the other hand is still used.
- [x] Scooping with a bowl plays the bucket sound, with a waterskin the bottle sound.
- [x] After a drink of sea water, the Parched icon (a dry tongue) shows at the top right of the HUD
      and in the inventory's effect list, the thirst bar turns sandy, empty droplets included, and no
      particles swirl around the player. `gameplay/parched.jsonl` captures both screens.
- [x] With Upset Stomach the thirst bar turns venom green, empty droplets included, and wins over
      Parched.
- [ ] Sneaking and clicking water with an empty hand splashes droplets on the water.
- [ ] Sneaking and using a filled waterskin on a block empties it with a splash sound, droplets and
      the bottle sound, like a water bottle poured on dirt.

### Config screen

- [x] Mod Menu → ThirstWasTaken2 → Configure opens the root screen: the animated preview (tooltip on
      the left, thirst bar above the food bar on the right), eight page buttons with hover text, the
      server note, and Cancel and Done.
- [x] The three Sickness pages show "Used when Sickness is Realistic", then a header per grade (Dirty,
      Murky, Clean) with Poisoning, Upset Stomach and Upset Stomach Level, no label cut off, in
      English and Vietnamese.
- [ ] On each difficulty tab of the Sickness page: add an effect by id (the grey completion is taken),
      edit its chance, seconds and level, remove it, and reset a grade. Done, relaunch: the edits are
      kept. A drink of Dirty water gives exactly what the tab for the world's difficulty lists.
- [x] Every page opens, every slider and toggle has a tooltip, and no slider can go outside its range.
- [x] Reset to Defaults puts that page back and leaves the other pages alone.
- [x] Change a value, Done, relaunch: it is kept. Change a value, Cancel: it is back, in the HUD too.
- [x] Resize the window with a page open and after returning to the root screen: nothing is drawn
      twice and the preview stays centred.

### Damage and the world

- [x] `/thirst set @s 0 0` on Normal: half a heart of damage every two seconds, down to death; the death
      message names dehydration. On Easy it stops at five hearts. Armour does not reduce it.
- [x] Standing still in a desert drains faster than in a snowy biome; in the Nether faster again.
- [x] Drinking in the Nether earns "nether_drink".
- [x] On Peaceful, thirst does not drop and slowly refills.
- [x] Rain on an empty cauldron in the open fills it.
- [x] A supply chest in a real dungeon, mineshaft or shipwreck sometimes holds water bottles.
- [x] The advancement tab has its icon and terracotta background, and the recipe book lists the
      purification recipes once a bottle, bowl or bucket is held.

### Kaleidoscope Tavern

`tools/agent/integrations/kaleidoscope-tavern.jsonl` (and `-1.20.1`) runs these; its header says how.
In a world with the mod, on one node per loader:

- [ ] A Dirty bucket poured into a Barrel comes back out Dirty in a fresh bucket; Pure then Dirty
      comes back Dirty.
- [ ] A sea-water bucket is refused by the Barrel, stays full in the hand and spills nothing.
- [ ] A canteen goes into the Barrel and stays a canteen, on every loader.
- [ ] A Tap on a Dirty cauldron fills a Dirty cauldron; on a waterlogged slab in the ocean, a salty one.
- [ ] A Tap over a placed Empty Bottle makes a Water Bottle that drops Dirty when broken.
- [ ] A sea-water bottle shift-placed and picked up again by hand is still salty.
- [ ] The Shaker refuses a sea-water bottle and takes a Dirty one.
- [ ] Jade shows the Barrel's grade looking at a side block.

### Without the optional mods

- [x] Remove Mod Menu, AppleSkin and Cloth Config from the run: the game loads and the bar draws
      without the exhaustion strip or the quenched outline, and tooltips have no droplet rows.
- [x] A dedicated server (`runServer`) starts and a client joins it without a crash on either side.
- [x] A dedicated server with Jade in its mods folder starts without a crash, and loads the plugin.
      `runServer` with Jade copied into `run/<node>/mods` is enough on both loaders: the Fabric dev
      server has the client classes on its classpath, as the released jar does.
- [ ] A client **with Jade and without** each integration's mod reaches the title screen and stays up,
      on every node that builds that integration. This is the shape 1.0.9 crashed in: Jade loads every
      plugin it is given, and FML every `@Mod` class, whether or not the mod it is for is there.
      `./gradlew ":<node>:runClient" -Pagent=tools/agent/smoke/boot.jsonl -PwithoutOptional=<name>`, then
      `python tools/agent/drive.py run/<node>/agent/client tools/agent/smoke/boot.jsonl --verify --max-age 10`
      (without `--max-age` it reads the last run's answers if this one crashed). Leaving out
      a library leaves out the mods that need it too.
      - `sophisticated-core` on every NeoForge node
      - `moonlight` (Supplementaries goes with it) on both 1.21.1 nodes
      - `create` on `1.21.1-neoforge` and `1.20.1-forge`, and on the Fabric nodes that build Create Fly
      - `farmersdelight` on every Fabric node (the Cooking Pot fix mixes into Refabricated) and on
        `1.20.1-forge`. Run on 2026-09-28 on `1.20.1`, `1.20.1-forge`, `1.21.1` and `26.3.x`, with
        `create` and `all` on `1.20.1-forge` and `all` on `1.20.1`: every one came up and stayed up
      - `kaleidoscope_cookery` on `1.21.1-neoforge` and every Fabric node
      - `kaleidoscope_tavern` on `1.21.1-neoforge`, `1.20.1-forge` and every Fabric node but `1.21.11`
- [ ] The same with `-PwithoutOptional=all`, on one node per loader.
- [ ] A world **opens** with an integration's mod and without a mod its data names, for every
      integration that ships data about another mod. Reaching the title screen is not enough: a broken
      data file only shows when a world loads. Add `-Pquickplay=<world>`; `boot.jsonl` then fails when
      the world never opened. 1.3.0 and 1.4.0 opened no world with Cold Sweat and without Farmer's
      Delight or Kaleidoscope Cookery.
      - Cold Sweat on `1.21.1-neoforge` and `1.20.1-forge` (uncomment its `runClientMod` line), with
        `-PwithoutOptional=farmersdelight,kaleidoscope_cookery`

`checkOptionalSeam` is the static half of these checks for classes, and `checkDataConditions` for data
files; CI runs both on every node.

## Purification rework

The rework's playtests. The numbers they check are in
[WATER-REFERENCE.md](mechanics/WATER-REFERENCE.md). They judge balance, so they are played, not scripted. Record the numbers each asks for in the release
PR.

- [ ] Furnace and smoker times on screen: a bottle 8 s and 4 s, a bucket 24 s and 12 s, a half-full
      canteen half a full one's time; no experience; `enableFurnaceBoiling` off and `/reload` leaves
      no water recipe in the recipe book.
- [ ] Upset Stomach's tooltip or effect line makes it clear that food does not heal while it lasts.
- [ ] Upset Stomach II's cramps hurt in survival, half a heart at random times, more often on harder
      difficulties, and stop at half a heart, on Peaceful too. The gametest checks the timing, not
      the hit, since vanilla will not hurt its mock player.
- [ ] The tooltip's droplet rows show the item's value, the same at a full bar as in Creative: none
      of quenched for Dirty water, a quarter of a serving for Murky.
- [ ] Twenty-minute routes with each vessel: starter survival, building, ordinary exploration,
      repeated combat and the Nether. Count drinks, treatment time, slots, and health, food and water
      spent. Ordinary travel should not need a drink more than about once a minute.
- [ ] Three matched mining runs: food and untreated cave water, food and prepared Clean water, food
      and boiling in the field. Raw water should lose; prepared water should let the player heal.
- [ ] A long trip with a stack of three bowls, a waterskin, a canteen and a flask, counting the fuel
      and time to prepare them.
- [ ] One raw drink in an emergency, then shelter and Clean water: the player recovers without a
      distiller.

## Per version

What differs between the nodes, and so what has to be looked at on that node in particular. Why
each one differs is in [VERSION-DIFFERENCES.md](VERSION-DIFFERENCES.md). F1, the HUD layout and the
config file button are the agent scripts above; run them on every node, since each version reaches
them through different code.

### 26.3

- [ ] A sea-water bottle and bucket look like ordinary water, and their tooltip says "Salty". A
      bottle or bucket filled with sea water on 1.4 or earlier, in an old world, still draws vanilla's
      sprite rather than a missing model.
- [x] The "Open thirstwastaken2.json" button is one full-width row. 26.3 moved the call it makes
      (`Blaze3D.openPath`); `ui/config-screen.jsonl` presses it.
- [x] Breaking a hanging pot drops it. 26.3 removed the block codec it used to carry.
- [x] `gameplay/waterskin-stack.jsonl` and `gameplay/loot-and-boil.jsonl` pass: 26.3 changed
      `placeItemBackInInventory` and the loot and recipe datapack shapes they reach.

### 26.2

- [x] F1 hides the bar. 26.2 moved the "HUD hidden" state into the HUD object itself, a code path of
      its own.
- [x] The config file button is one full-width row (26.2 opens screens through a different call).
- [ ] Sea-water bottle and bucket look like ordinary water, as on 26.3.
- [x] Create Fly (Fabric 26.1.x and 26.2.x): a Spout fills waterskins and terracotta bowls with its
      grade, a waterskin of another grade passes through, and an Item Drain empties them with their
      grade. `integrations/createfly-waterskin.jsonl` runs two waterskins in a row, which catches a
      Spout refusing every waterskin after its first fill.

### 26.1.x

- [x] F1 hides the bar (read from the options, unlike 26.2).
- [x] The config file button is a row of its own, even though 26.1 has no full-width row call.
- [ ] Sea-water bottle and bucket look like ordinary water, as on 26.3.

### 1.21.11

- [x] F1 hides the bar, as on 26.1.
- [x] The config file button, as on 26.1.
- [ ] Sea-water bottle and bucket look like ordinary water, as on 26.3.
- [x] The droplet glyphs have no shadow.

### 1.21.1

The version with the most of its own code, and the only one where several things are known to look
different on purpose. Check all of these on every release that ships a 1.21.1 jar.

- [x] **Sync first.** Fabric API's attachment sync on 1.21.1 is a backport the gametests cannot reach.
      Run `gameplay/client-sync.jsonl` on this version before anything else.
- [x] **Sprinting is gated by a client mixin** (`LocalPlayerMixin`), because 1.21.1 keeps the food
      check a sprint needs on LocalPlayer. `client-sync.jsonl` checks it at 6 and 7.
- [x] **The HUD is drawn by a mixin**, not by Fabric API's HUD registry, which 1.21.1 does not have.
      Check that it disappears with the hearts in creative and that a living mount's health replaces it.
- [x] **Tinting uses render state.** With AppleSkin's underlay on, the air bubbles and the hotbar
      drawn after the strip must not come out faded.
- [x] **Drinking is implemented by the item itself**, not by the consumable component. The animation
      must be drinking, not eating; the sound must play; a creative player keeps the waterskin's
      servings and the bowl.
- [x] **Known, by design:** the tooltip droplets are drawn with a shadow (styles cannot turn it off
      before 1.21.4). Check that they are still legible, not that the shadow is gone.
- [x] A sea-water bottle and bucket look like ordinary water, as on every version. Check that the
      tooltip says "Salty" and that the bowl still changes to the sea colour.
- [x] **The config screen's headings are text rows**, not vanilla headings, which 1.21.1 lacks. Check
      that they are centred and readable and that the list still scrolls.
- [x] The config preview's food icons and droplet outlines have transparent corners, not black ones
      (1.21.1 needs blending turned on around each draw).
- [x] The water cauldron's name is "Water Cauldron" in the F3 target and in `/give` suggestions. On
      1.21.1 the mod has to identify the cauldron before its name exists, and a mistake there would
      rename it.
- [x] The advancement tab background is terracotta, not a missing texture.
- [x] The jar loads on Minecraft 1.21 as well as 1.21.1.

### 1.20.1 (Fabric and Forge)

Everything in the 1.21.1 section holds here too, including its two known differences, and 1.20.1 has
more of its own: water on an item is its NBT tag, the purification recipes are the mod's own types,
and there is no GUI sprite atlas. Run both nodes. The worlds `new_world.py` makes for 1.20.1 do not
allow cheats, so the agent scripts read block entities through `server.command`.

- [ ] **Sync first,** on both nodes: `gameplay/client-sync.jsonl`, and the two-client check on
      `runServer`. Fabric 1.20.1 syncs through its own channel, Forge through a `SimpleChannel`.
- [ ] The thirst bar, its droplets and the quenched outline draw from their texture sheet, with no
      missing-texture squares, in every state `ui/hud-layout.jsonl` sets up.
- [ ] A smoker and a furnace each boil a Murky bottle, bucket and bowl Clean, a furnace a canteen and
      a flask too, and the recipe book shows the purification recipes on both loaders.
- [ ] A water bowl, a waterskin and a canteen keep their grade across a save and reload.
- [ ] Mods → ThirstWasTaken2 → Config opens the settings screen on Forge; Mod Menu does on Fabric.
- [x] The shipped Forge jar (`reobfJar`) boots on a real Forge 47.4.10 server with no mixin or data
      error, alone and with Farmer's Delight, Serene Seasons and Create. Checked on 2026-09-28. The
      client side of the shipped jar is still to be seen in a real Forge install.
- [x] `integrations/farmers-delight-1.20.1.jsonl` on both nodes, `integrations/serene-seasons.jsonl`
      on both, and `integrations/create-water-1.20.1.jsonl` on Forge. Checked on 2026-09-28.

### NeoForge (every node)

For what the loader changes: the HUD layer, the config screen entry, the client mixins, the event
hooks and the attachment. Everything else is the same code as the Fabric node of the same version.
Run it on each `-neoforge` node, which has no Mod Menu and none of the Fabric-only integrations.
Sneaking has to be held by a real key press for hand drinking: a Shift modifier on a single click is
released before the server sees the player crouch.

- [x] **Sync first on 1.21.1 and 1.21.11.** NeoForge 21.1 and 21.11 only sync thirst to a connection
      that negotiated the attachment channel. Run `gameplay/client-sync.jsonl` and the two-client check
      on `runServer` with `runManualA` and `runManualB`, and watch for a sync error on join.
- [x] The bar and the air bubbles stack by `Gui.rightHeight` (1.21.x, 26.1) or NeoForge's shared
      height, not Fabric's registry: `ui/hud-layout.jsonl`.
- [x] In creative the thirst bar disappears with the hearts and hunger, and a living mount's health
      replaces it, as on Fabric.
- [x] With AppleSkin, the quenched outline and the exhaustion strip draw on the thirst bar. Setting
      `showFoodExhaustionHudUnderlay = false` in `config/appleskin-client.toml` while in game removes
      the strip from both bars at once, since NeoForge reloads the file.
- [x] Item tooltips show the purity line and the droplet rows after NeoForge's own lines, together
      and in order.
- [x] Mods → ThirstWasTaken2 → Config opens the mod's config screen, and Done returns to the mods list.
- [x] Jade shows the grade on river water, sea water and a water cauldron.
- [x] Drinking by hand works from water under the crosshair and from water the crosshair misses
      (`MinecraftMixin`). F3 showing no Targeted Block is the way to be sure the crosshair missed.
- [x] Filling a bottle from a cauldron and scooping with a bowl keep working after a second right
      click on the same block (the use events are cancelled with a result, not just stopped).
- [x] `runServer` starts without an error, once as it is and once with Jade in
      `run/<node>/mods`, and loads the mod's Jade plugin.
- [x] On `1.21.1-neoforge`, the purification recipes show in the recipe book: their JSON is
      translated differently there (`type` and `items`).
- [ ] On `1.21.1-neoforge`, run `integrations/cold-sweat.jsonl` and look at its
      `cold-sweat-hud.png`: the thirst bar and Cold Sweat's body temperature gauge do not overlap. Then
      put a Dirty water bottle in a Boiler by hand, through its screen, and see it rise to Clean and stop.

## When this file changes

Anything a gametest can reach belongs in a gametest, and anything with an exact answer in an agent
script, not here. When a check moves into one, delete it from this list. When a new version node is
added, give it a section, starting from what its row in `platform/AGENTS.md` says it lacks. Record the
results of a pass in the release PR, not in this file.
