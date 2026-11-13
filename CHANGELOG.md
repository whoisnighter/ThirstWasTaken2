# Changelog

All notable changes to ThirstWasTaken2 are documented in this file.

## [Unreleased]

### Added

- Extra Delight, on NeoForge 1.21.1: its new strawberry drinks and treats restore thirst, from Strawberry
  Juice, Pink Lemonade, Strawberry Milk and the Strawberry Milkshake to the popsicle, ice cream and
  custard.
- Cultural Delights, on NeoForge 1.21.1: Ice Cream restores a little thirst.
- Little Logistics, on NeoForge 1.21.1 and Forge 1.20.1: a bucket filled from or poured into a Fluid
  Tank Car, a Fluid Barge or, on 1.20.1, a Fluid Hopper keeps the grade of the water. It used to come
  out Clean whatever the tank held.
- Sophisticated Backpacks and Sophisticated Storage now work on NeoForge 26.3 too, Drinking Upgrade
  included.

### Fixed

- On Fabric 1.20.1 and 1.21.1, a canteen or flask boiled in a furnace comes out full of Clean water. It
  used to come out empty.
- Let's Do: Candlelight 2.1.14 or newer no longer crashes the game at launch on Fabric 1.21.1. Sea
  water still cooks nothing in its Large Cooking Pot.
- Kaleidoscope Cookery Refabricated, on Fabric: a teapot filled by a Recipe Item keeps the grade of the
  water it took. It used to keep the grade of whatever water was in it before.
- Sophisticated Backpacks: a canteen or flask boiled in a Smelting, Smoking or Blasting upgrade no
  longer comes out empty with older versions of Sophisticated Core.
- Sophisticated Backpacks: the game no longer crashes at launch with Sophisticated Core 1.5.35 or newer
  on 1.21.1, or the matching Cores on 26.1 and 26.2. Water in a Tank upgrade keeps its grade on old and
  new Cores alike.

## [1.10.1] - 2026-10-27

Gensokyo Delight ~ Youkais' Feasts joins on NeoForge 1.21.1 and Forge 1.20.1, and Immersive Engineering's
barrels and tanks stop turning any water into Clean water.

### Added

- Gensokyo Delight ~ Youkais' Feasts, on NeoForge 1.21.1 and Forge 1.20.1:
  - Teas, juices, sakes, wines, soups and popsicles restore thirst.
  - Sea water won't go into the Kettle or the Fermentation Tank, won't start a soup in an iron pot, and
    won't cook Bamboo Mizuyokan.

### Fixed

- Immersive Engineering, on NeoForge 1.21.1 and Forge 1.20.1: a bucket filled from a barrel or a Fluid
  Tank keeps the grade of the water in it, and so does a bucket poured in. It used to come out Clean
  whatever the tank held, which purified water for free.

## [1.10.0] - 2026-10-21

Water is now finite when the world says it should be: filling a bottle or a waterskin takes the source
block, as a bucket does. Bountiful Fares and Let's Do: Meadow join on 1.21.1, and Hearth and Harvest's
water blocks keep their grade on Forge 1.20.1 too.

### Added

- Finite water. In a world where water doesn't refill itself, filling a bottle, bowl, waterskin, canteen
  or flask from the world takes the water source block, as a bucket does, and a waterskin, canteen or
  flask draws three drinks from it. It follows the `waterSourceConversion` game rule on its own, and
  works alongside Thirsty Bottles.

- Bountiful Fares, on Fabric and NeoForge 1.21.1:
  - Cups filled from water keep its grade, through cauldrons, the Hanging Pot and the Copper Distiller
    too, and a Water Cup is drunk like a water bottle.
  - Teas, ciders, wines, mead, stews, compotes and the juicier fruit restore thirst.
  - Sea water won't go into the Fermentation Vessel, and won't brew tea or bake a Sponge Cake.

- Let's Do: Meadow, on Fabric and NeoForge 1.21.1:
  - The wooden bucket keeps the grade of the water it scoops, through cauldrons, the Hanging Pot and
    the Copper Distiller too.
  - The wooden cauldron gives Murky water, however it was filled, and won't take sea water.
  - Sea water won't go into the Cooking Pot.
  - Wooden milk buckets restore thirst like a milk bucket.

- Hearth and Harvest 1.4.1, on Forge 1.20.1:
  - Jugs, Troughs, the Sprinkler, the Stomping Basin and the Keg keep the grade of the water poured in,
    and the Sink gives Murky water, the same as on NeoForge 1.21.1.
  - Only a bottle of sea water boils down to salt in a Cooking Pot.

### Fixed

- With Hearth and Harvest 1.4.0 or newer, sea water no longer ferments into mead, beer, moonshine or
  root beer in the Keg. It still makes brine.

<details>
<summary>Configuration file details</summary>

- New `waterFillTakesSource`: `"AUTO"` (default) takes the water source only when the
  `waterSourceConversion` game rule is off, `"ON"` always, `"OFF"` never.

</details>

## [1.9.0] - 2026-10-15

This release adds two late-game flasks and brings water grades to Mekanism, Immersive Engineering and
Hearth and Harvest's machines. The settings screen also looks more at home next to Minecraft's own.

### Added

- Diamond Flask, made from six diamonds, holds 10 drinks and boils in 2 seconds a drink.
- Netherite Flask, upgraded from the Diamond Flask at a smithing table, holds 15 drinks and doesn't
  burn.
- Kaleidoscope Tavern now works on Fabric 1.21.11 too, with Kaleidoscope Tavern Refabricated 1.2.0.6.
- Mekanism, on NeoForge 1.21.1 and Forge 1.20.1:
  - The Electric Pump keeps the grade of the water it pumps, so a pump in the sea gives sea water.
  - Water condensed from water vapour in the Rotary Condensentrator is Pure, even from sea water.
  - Water keeps its grade through Mekanism's pipes and tanks.
- Immersive Engineering, on NeoForge 1.21.1 and Forge 1.20.1:
  - The Fluid Pump keeps the grade of the water it pumps, so a pump in the sea gives sea water.
  - Bottles filled in the Bottling Machine get the grade of the water in it.
  - Water keeps its grade through pipes, barrels, tanks and the Jerrycan, and two grades never mix.
- Hearth and Harvest 1.4.0, on NeoForge 1.21.1:
  - Beer and the Goat Milk Bucket restore thirst.
  - The Keg keeps the grade of water poured in and drawn back out.
  - A Sprinkler filling itself gets the grade of the water beside or below it, and Clean water from rain.

### Changed

- The settings screen looks more like Minecraft's own menus, with far less white.
- With Kaleidoscope Tavern Refabricated 1.2.0.12 or newer (1.2.0.6 on 1.21.11), the Barrel takes
  Waterskins, Canteens, Flasks and terracotta bowls on Fabric too, and hands them back as they were.

### Fixed

- With Keyword Matching on, a keyword only matches a whole word of an item's name, so a Steak
  Sandwich no longer counts as tea. Toast and sandwiches are now on the keyword blacklist; an existing
  config file keeps its own list, so add `toast|sandwich` to `keywordBlacklist` there.
- With Hearth and Harvest 1.4.0, only a bottle of sea water boils down to salt in a Cooking Pot again.

### Removed

- NeoForge 1.21.11 is no longer supported. Minecraft 1.21.11 is still supported on Fabric.

## [1.8.0] - 2026-10-09

### Added

- Kaleidoscope Tavern, on NeoForge 1.21.1, Forge 1.20.1 and Fabric (Refabricated) except 1.21.11:
  - Wines, cocktails, juices and grapes restore thirst. Spirits and vinegar restore none.
  - Water poured into the Barrel and taken back out keeps its grade. The Barrel won't take sea water.
  - On Fabric, the Barrel won't take Waterskins, Canteens, Flasks or terracotta bowls, which it would
    hand back as a bucket. They'll be allowed once
    [Refabricated #29](https://github.com/NightEpiphany/KaleidoscopeTavern-Refabricated/issues/29) is fixed.
  - The Tap passes on the grade of the cauldron it draws from, and a Tap on a waterlogged block in the
    sea draws sea water.
  - A Water Bottle put down as a block keeps its grade when picked up or broken again.
  - The Shaker won't take a sea-water bottle.
  - With Jade installed, looking at a Barrel of water shows its grade.

### Fixed

- With AppleSkin, an item's tooltip shows how much thirst and quenched it restores again, even when
  the thirst bar is full. In Survival the droplets used to shrink or disappear as the bar filled.
- With Kaleidoscope Chinese Food and Kaleidoscope Tavern, a Tap filling a Kaleidoscope Cookery Stockpot
  or Teapot keeps the water's grade, and the Teapot brews no tea from sea water it drew.
- With Miner's Delight, a Water Cup pours into a hanging pot or a distiller and an empty Copper Cup
  draws from one, a bucket's worth at a time, keeping the water's grade.
- With Cold Sweat, its Waterskin pours into a hanging pot or a distiller and an empty one draws from
  them, one drink at a time.
- With Create Pipes n Physics on NeoForge 1.21.1, a pump draws water from the world and from
  cauldrons through an open pipe end again, and water fed into a Sand Filter comes out of its bottom.

### Notes

- Kaleidoscope Tavern Refabricated 1.2.0.5 crashes the game on Minecraft 1.21.11, so this mod has no
  Kaleidoscope Tavern support there yet. Reported upstream as
  [Refabricated #28](https://github.com/NightEpiphany/KaleidoscopeTavern-Refabricated/issues/28).

## [1.7.0] - 2026-10-07

This release reworks how water is cleaned and how much it's worth. Boiling now makes water Clean, which
is safe, and Pure water comes from the new Copper Distiller. Every container restores the same per drink,
thirst drains a little even standing still, and food and water each need the other to heal.

### Added

- The Copper Distiller, a two-block still that turns any water into Pure water, sea water included.
  It burns furnace fuel and works with hoppers. Craft it from its four parts, or build it in the world
  and join it with a Copper Pipe. Its cooling tub needs filling once, with any water.
- Desert pyramids and desert and savanna village houses can hold water bottles. The water there is
  Dirty or Murky, so it needs boiling.
- Novice Leatherworkers can sell a Waterskin, and novice Clerics a Clean water bottle, so emeralds buy
  water before a player can boil any.
- When another mod adds salt, distilling sea water leaves some of it behind.
- Using a water cauldron with an empty hand drinks one serving from it, a bottle's worth, at the
  cauldron's grade.
- Filled terracotta water bowls stack to three, as long as their water is the same.
- The Copper Canteen can go in a furnace too, like the Iron Flask.
- With Jade installed, looking at a distiller shows both its tanks and whether its cooling tub is dry.
- Miner's Delight, on NeoForge 1.21.1 and Forge 1.20.1:
  - Its soups and the Milk Cup restore thirst. A cup of soup restores half what a bowl does.
  - Copper cups keep the grade of their water, like a bucket. A cup filled from the sea stays salty
    instead of pouring into a cauldron as Clean water. On 1.20.1 the same goes for Create's Spout and
    Item Drain.
- Kaleidoscope Flora, on NeoForge 1.21.1 and Forge 1.20.1: its flower teas restore thirst, like Kaleidoscope Cookery's
  Flower Tea.
- Extra Delight, on NeoForge 1.21.1:
  - Its juices, ades, milkshakes, coffee, tea, soups and popsicles restore thirst.
  - Jars, Kegs, the Vat, the Mixing Bowl and the Chiller keep the grade of their water, by bottle or
    bucket, and sea water stays salty.
  - The Tap and the Sinks give Murky water, and a Funnel grades the water it takes up.
  - The Vat and the Mixing Bowl make nothing from sea water, and the Evaporator only turns sea water into
    salt.
- Kaleidoscope Chinese Food, on NeoForge 1.21.1 and Forge 1.20.1: its teas, soups, porridges and
  noodles restore thirst, like Kaleidoscope Cookery's.

### Changed

- Boiling stops at Clean. A furnace, a smoker, a campfire-boiled canteen or flask, a Hanging Pot, the
  Farmer's Delight Cooking Pot and the Cold Sweat Boiler all make Dirty and Murky water Clean, never
  Pure. Clean water is always safe to drink.
- Water no longer cooks on a campfire's slots. A furnace takes 8 seconds a bottle or bowl, 24 a bucket,
  and gives no experience. A canteen or flask takes its boil time for each drink inside.
- Every drink of water restores 6 thirst, from a bottle, bowl, waterskin, canteen or flask alike. Its
  quenched depends on the grade: none for Dirty, 1 for Murky, 2 for Clean and 4 for Pure.
- Drinks, soups and food from every supported mod were rebalanced. Prepared drinks and soups give 6
  thirst and 4 quenched, alcohol less, and solid food a little thirst but no quenched. Drinks another
  mod only tags as drinks get 6 and 4 too.
- Thirst past a full bar is lost instead of turning into quenched. Clean and Pure water can still be
  drunk with a full bar while quenched isn't full, to build a reserve.
- Thirst drains about a point a minute even when standing still. The drain rises smoothly with
  temperature, and the Nether drains three times as fast.
- Food heals only while the thirst bar is at least half full, and quenched heals only while the food
  bar is. Quenched now waits for saturation to run out before it heals, instead of healing alongside
  it, and Upset Stomach always stops both.
- Quenched heals exactly like saturation: just as fast, and spending quenched the way saturation is
  spent.
- Water in a cave or under a roof is at best Murky. Deep water is no longer cleaner.
- Dirty and Murky water no longer taste bad: no more Nausea after every drink. Upset Stomach and Poison
  now come from one roll, so Poison only ever comes with Upset Stomach. Clean water never makes the
  player ill.
- Upset Stomach no longer drains thirst or cuts saturation. Instead, cramps cost half a heart at random
  times, more often at level II and on harder difficulties. Like Poison, they stop at half a heart. It
  no longer warps the screen, milk cures it, and Dirty and Murky water cause it less often and for
  less time.
- Bad water poisons less often on every difficulty.
- Dirty and Murky water can make the player ill on Peaceful too, more mildly than on Easy.
- On the config screen's Sickness page, Reset to Defaults now puts back every difficulty's table too,
  not just the General tab.
- On the Item Values page, Reset to Defaults now puts back every item's value at once.
- The config screen takes on the look of vanilla's own options screens.
- Parched drains 4 thirst a minute, 8 at level II.
- The Waterskin holds four drinks and is made of four leather. The Copper Canteen takes three copper
  and a string, and the Iron Flask three iron and an iron nugget. The Copper Hanging Pot holds three drinks and the Iron Hanging Pot six, and both boil faster.
- Three clay balls make three clay bowls. The Distiller Boiler takes an iron ingot instead of gold.
- The Boil Your Water advancement is also earned by making a canteen, flask or hanging pot.

### Fixed

- The furnace and smoker recipe books show one water bottle, bowl, bucket, canteen and flask each,
  instead of one per grade of water.
- A bottle, bowl or other container that a cauldron doesn't take no longer changes the grade of the
  water inside, for example a Dirty bottle clicked on a full cauldron of Pure water.

### Notes

- This update rebalances almost every number in the mod, but an existing config keeps its old values.
  Resetting to the new defaults is strongly recommended: press Reset to Defaults on every page of the
  config screen, or delete `config/thirstwastaken2.json`, on a server too.

<details>
<summary>Configuration file details</summary>

- New: `enableCopperDistiller`, `distillerServingSeconds` (default `8`), `distillerTankServings`
  (default `9`) and `distillerSaltItem`, which picks one salt when several mods add one.
- New: `enableFurnaceBoiling`, `plainWaterValue` (`[6, 4]`), `plainWaterDrinkTicks` (`32`),
  `foodHealMinThirstPercent`, `quenchedHealMinFoodPercent`, `waterskinCapacity`, `terracottaWaterBowlStackSize`, `copperHangingPotCapacity` and `ironHangingPotCapacity`. Every
  container count goes from 1 to 64.
- `defaultPurity`, `rainwaterPurity` and `dripstonePurity` are now `defaultQuality`,
  `rainwaterQuality` and `dripstoneQuality`. An old file is converted on load.
- `quenchedHealMinFood`, in half shanks, is replaced by `quenchedHealMinFoodPercent`.
- `quenchedHealthRegen` is removed: quenched always heals at saturation's speed.
- Sickness effects take an optional `group`: effects in one group share a roll.

</details>

## [1.6.2] - 2026-10-03

Five more mods join: Spelunkery, Hearth and Harvest, No Man's Land, Let's Do: Vinery and Croptopia.
Croptopia works on almost every version.

### Added

- Spelunkery, on Fabric and NeoForge 1.21.1:
  - Spring Water is always Pure.
  - Only sea water boils down to salt in a furnace. Fresh water is purified there as usual.
- Hearth and Harvest, on NeoForge 1.21.1 and Forge 1.20.1:
  - Juices, milks, wines and stews restore thirst. Alcohol restores less than juice.
  - Only a bottle of sea water boils down to salt in a Cooking Pot. A fresh bottle is purified there
    instead.
  - Jugs keep the grade of the water poured in, so nothing comes back out cleaner than it went in. On
    1.21.1, so do Troughs, the Sprinkler and the Stomping Basin.
  - A Jug filled from the sea holds sea water, and the Sink's endless water is Murky. 1.21.1 only.
  - The Cask won't age drinks from sea water. 1.21.1 only.
- No Man's Land, on NeoForge 1.21.1:
  - Water in the Bog, Bayou, Dark Swamp and Blackwater River comes out Dirty, the same as any other
    swamp.
  - The sea at the Mud Beach is salty, like at any other beach.
  - Pear juice, maple syrup, pesto, witch stew and pears restore thirst.
  - Each sip from a milk cauldron restores a quarter of a milk bucket.
- Let's Do: Vinery, on Fabric and NeoForge 1.21.1: juices, wines, grapes and cherries restore thirst.
  Wine restores less than the juice it's made from.
- Croptopia, on Fabric and NeoForge 26.2, 26.1 and 1.21.1, and on Fabric and Forge 1.20.1:
  - Juices, smoothies, teas, coffees, soups, salads and the juicier fruit restore thirst.
  - A bucket of sea water won't craft its water bottles, tea or anything else that takes water.

### Fixed

- The feet and the fork at the top of the Hanging Pot's stand no longer look paler than the rest of
  the wood.

## [1.6.1] - 2026-10-02

Two more Let's Do mods join on 1.21.1: HerbalBrews and Beachparty.

### Added

- Let's Do: HerbalBrews, on Fabric and NeoForge 1.21.1:
  - Teas and coffees restore thirst.
  - Drinking from a placed Jug counts every cup in it.
  - The Tea Kettle won't brew with sea water.
- Let's Do: Beachparty, on Fabric and NeoForge 1.21.1:
  - Cocktails and open coconuts restore thirst.
  - A placed cocktail gives three sips, each worth a third of the glass.

## [1.6.0] - 2026-10-01

This release is mostly about other mods. Farm & Charm and Candlelight join on 1.21.1, Terralith's
swamps now count as swamps, and Cold Sweat finally reaches Forge 1.20.1. The Hanging Pot also got a
proper stand.

### Added

- Let's Do: Farm & Charm, on Fabric and NeoForge 1.21.1:
  - Teas, soups, salads and the juicier crops restore thirst.
  - The Timber Well draws up whatever groundwater lies below it. Near the coast, that means sea water.
  - The Water Trough gives Murky water and won't take sea water.
  - The Cooking Pot, the Stove and the Crafting Bowl won't cook anything with sea water.
- Let's Do: Candlelight, on Fabric and NeoForge 1.21.1:
  - Soups, salads and the chocolate mousse restore thirst.
  - Kitchen sinks give Murky water and won't accept sea water.
  - Like Farm & Charm's, its Cooking Pot won't cook with sea water.
- Terralith, on every version and loader: water in the Orchid Swamp and the Ice Marsh comes out Dirty,
  the same as any other swamp.
- Cold Sweat on Forge 1.20.1, with everything it already did on NeoForge 1.21.1. Temperature drives
  thirst, the Waterskin keeps track of purity, the Boiler cleans water and hot drinks warm the player up.

### Changed

- The Hanging Pot has a new stand that looks a lot more like something built at a camp.
- A Hanging Pot can now be placed before there is a campfire, with room underneath to add one later.

## [1.5.0] - 2026-09-28

### Added

- Minecraft 1.20.1, on Fabric and on Forge. Both files have every feature of the later versions. The
  Forge file runs on any Forge 47 build.
- Compatibility with Farmer's Delight on Fabric and Forge 1.20.1:
  - Its drinks and meals restore thirst, the Cooking Pot boils water Pure, and Nourishment stops
    thirst draining.
- Compatibility with Serene Seasons on Fabric and Forge 1.20.1:
  - Thirst follows the season, the same as on the later versions.
- Compatibility with Kaleidoscope Cookery on Fabric and Forge 1.20.1:
  - The Stockpot and the Teapot keep the grade of the water poured in, and the Teapot brews nothing
    from sea water, the same as on the later versions.
- Compatibility with Fruits Delight on Forge 1.20.1:
  - Sea water makes no juice and no fruit cauldron, the same as on NeoForge 1.21.1.
- Compatibility with Create on Forge 1.20.1:
  - The Sand Filter cleans water pumped through it by one grade.
  - Water keeps its grade through pipes, pumps, Spouts and Item Drains.
- A Sickness page in the config screen. For each difficulty and water grade, any effect can be added
  or removed, each with its own chance, duration and level.
- Drinking bad water while still ill makes the effects last longer, up to twice as long. It can be
  switched off on the Sickness page.

### Fixed

- With Farmer's Delight on Fabric, the Cooking Pot boiled salt water, and any other potion, into Pure
  water. It now refuses them, as it always did on NeoForge.

### Changed

- Sea water in a bottle or bucket now looks like ordinary water on every version, as it always did on
  1.20.1 and 1.21.1. Its tooltip still reads Salty, and a bowl of sea water keeps its own colour.
- The Sickness setting and its Realistic and Classic choices are gone. Bad water now gives what the
  Sickness page lists, each effect rolled on its own. See the details below.
- Bad water poisons more often on every difficulty, and Clean water now can too. Illness from Clean
  water is shorter.

<details>
<summary>Configuration file details</summary>

- `sicknessPreset` is removed and ignored if still in the file.
- `sicknessEffects` holds the effects: by difficulty, then by grade, a list of `effect`, `chance`,
  `seconds` and `level`.
- `extendSicknessEffects`, on by default, makes a repeated effect last longer.

</details>

## [1.4.1] - 2026-09-28

### Fixed

- With Cold Sweat on NeoForge 1.21.1, worlds failed to create or load with a data pack error unless
  both Farmer's Delight and Kaleidoscope Cookery were installed.

### Notes

- Released for NeoForge 1.21.1 only. Every other version is unchanged and stays on 1.4.0.

## [1.4.0] - 2026-09-27

### Added

- Quenched now heals like saturation, at half the speed, while thirst is full and food is at least 5
  shanks. It stacks with healing from food. The speed and the food it needs are settings on the
  Thirst page.
- Compatibility with Serene Seasons on Fabric and NeoForge, every supported version:
  - Thirst drains faster in summer and slower in winter, changing smoothly through the year.
  - Tropical biomes such as jungles, savannas and deserts follow the wet and dry seasons instead: the
    dry season drains thirst faster than the wet one.
  - A Seasons tab on the Thirst settings page sets the speed for each season, or turns this off.
- Compatibility with Ocean's Delight on Fabric and NeoForge 1.21.1:
  - Its Guardian Soup, Braised Sea Pickle and Seagrass Salad restore thirst.
- Compatibility with Expanded Delight on NeoForge 1.21.1:
  - Its juices, goat milk, soups and salads restore thirst.
  - The Cooking Pot cooks nothing from a bucket of sea water, so the sea cannot be cooked into soup.
- Compatibility with Rustic Delight on Fabric, every supported version, and NeoForge 1.21.1:
  - Its coffees, soups, sweet salad and raw bell peppers restore thirst.

### Changed

- Mushroom stew and rabbit stew restore 4 thirst and 5 quenched, as much as the other stews. Existing
  config files keep their old values.
- The thirst bar shows whole points only, like the food bar. It no longer empties the last droplet a
  little ahead of time, which made a full bar look short of full while drinking was still refused.
- In the settings screen's item list, an open mod group shows the thirst and quenched droplets above
  its two columns, and its heading stays at the top while its items scroll past.
- The Cold Sweat Climate setting only appears in the settings screen when Cold Sweat is installed.
- The Thirst settings page is split into Drain and Seasons tabs. The Seasons tab only appears with
  Serene Seasons installed.

### Notes

- With both Cold Sweat and Serene Seasons on NeoForge 1.21.1, Cold Sweat's temperature already follows
  the season, so the season speeds are not applied on top of it.

## [1.3.0] - 2026-09-26

### Added

- The settings screen can now edit what each item restores. Items are listed by mod, each with its
  thirst and quenched, a switch that turns it off, and a box to add another item.
- A Mod Items page switches off the bowls, the Waterskin, the Copper Canteen, the Iron Flask or either
  Hanging Pot, for a modpack that brings its own. A switched-off item can no longer be crafted and
  leaves the creative tab.
- New Water settings: how much each grade quenches, how long sea water's effects last, and the grade
  of rain and dripstone water. Sea water and rain collection can each be switched off.
- A Containers page sets the canteen and flask capacity and every boil time, and can switch off
  boiling in hand.
- Compatibility with Cold Sweat on NeoForge 1.21.1:
  - Thirst drains by the temperature Cold Sweat shows around the player instead of the biome's, so
    Hearths, shade, night and altitude count. `coldSweatClimate` turns it off.
  - Its Waterskin takes the grade of the water it is filled with, and a sip restores thirst like a
    bottle of water. Pouring it over the player restores none.
  - The Boiler purifies any water container a grade at a time, up to Pure. The Waterskin also purifies
    on a campfire, in a furnace and in a smoker. Sea water stays salty.
  - Hot Cocoa from Farmer's Delight and the hot teas from Kaleidoscope Cookery warm the player a little.
- Compatibility with Cultural Delights on NeoForge 1.21.1:
  - Its drinks, cucumbers and salads restore thirst. The stronger a drink, the less it restores, and
    spirits restore none.
  - The Vat brews nothing while it holds a bucket of sea water. Drinks brewed from fresh water are safe
    whatever its grade.
- Compatibility with Fruits Delight on NeoForge 1.21.1:
  - Its juices, teas, jellos, popsicles and juicy fruits restore thirst.
  - A bottle of sea water makes no juice, and a cauldron of sea water takes no lemon slice or jam.

### Changed

- Settings screen pages that cover several topics are split into tabs along the top, one per topic.
- Every language now has a translation for every line of the settings screen.

- Water in a Hanging Pot now looks like water in a cauldron. Each water grade and sea water keeps
  its own colour.

### Notes

- Recipe viewers such as JEI and EMI still list a switched-off item.

<details>
<summary>Configuration file details</summary>

- New keys, all defaulting to the old behaviour: `quenchedPercent`, `enableSeaWater`,
  `seaWaterNauseaSeconds`, `seaWaterParchedSeconds`, `enableRainCollection`, `rainwaterPurity`,
  `dripstonePurity`, `copperCanteenCapacity`, `ironFlaskCapacity`, `enableBoilingInHand`,
  `copperCanteenBoilSeconds`, `ironFlaskBoilSeconds`, `copperHangingPotBoilSeconds`,
  `ironHangingPotBoilSeconds`, `enableBowls`, `enableWaterskin`, `enableCopperCanteen`,
  `enableIronFlask`, `enableCopperHangingPot` and `enableIronHangingPot`.
- Values in `drinks` and `foods` are now kept between 0 and 20, and an entry without exactly two
  numbers is dropped.

</details>

## [1.2.1] - 2026-09-25

### Added

- Compatibility with Brewin' and Chewin' on Fabric and NeoForge 1.21.1:
  - Its brews and soups restore thirst. The stronger a drink, the less it restores, and spirits
    restore none.
  - The Keg keeps the grade of the water poured into it from a bucket or a bottle, including a Keg
    picked up and placed again. A Keg holding one grade refuses another.
  - The Keg takes sea water and hands it back still salty, but nothing ferments from it.
  - Drinks brewed from fresh water are safe whatever its grade.
  - With Jade installed, looking at a Keg shows the grade of the water inside.

### Changed

- Kaleidoscope Cookery: the Teapot now takes sea water, from a bucket or dipped into the sea, and
  hands it back still salty. It brews no tea from it.

## [1.2.0] - 2026-09-23

### Added

- Copper Canteen and Iron Flask, metal water containers that hold more than a waterskin. Holding use
  on a lit campfire boils the water inside Pure. The canteen boils faster, the flask holds more and
  can also go in a furnace.
- Compatibility with Kaleidoscope Cookery Refabricated on Fabric:
  - The Stockpot and the Teapot keep the grade of the water poured into them, including a Teapot
    picked up and placed again or dipped into water.
  - The Teapot refuses sea water. The Stockpot takes it and hands it back still salty.
  - With Jade installed, looking at a Stockpot or a Teapot shows the grade of the water inside.

### Fixed

- A Spout from Create on NeoForge 1.21.1, or Create Fly on Fabric 26.1 and 26.2, fills a stack of
  terracotta bowls one by one. Before, it never poured into a stack.

### Notes

- The Teapot fills from dripstone only on Minecraft 1.21.1, where the water it gets is Pure.

## [1.1.0] - 2026-09-23

### Added

- Data packs can set what any item restores, in `data/<namespace>/thirstwastaken2/drinks/`. When
  the config also lists an item, its config value takes priority. Item tooltips show the values the
  server uses.
- Other mods can now read and change thirst, check water purity and adjust what drinking restores,
  without needing a compatibility patch in this mod. See the
  [developer pages](https://whoisnighter.github.io/ThirstWasTaken2/docs/developers/data-packs).
- Compatibility with Kaleidoscope Cookery on NeoForge 1.21.1:
  - Its teas, milk tea and soups restore thirst.
  - The Stockpot and the Teapot keep the grade of the water poured into them, including a Teapot
    picked up and placed again, dipped into water, or filled by dripstone. Before, both handed back
    Clean water whatever went in.
  - The Teapot refuses sea water, so the sea cannot be brewed into safe tea. The Stockpot takes it and
    hands it back still salty.
  - With Jade installed, looking at a Stockpot or a Teapot shows the grade of the water inside.
- Upset Stomach, a new illness from bad water. Thirst drains faster, the screen warps now and then,
  and food gives less saturation. The thirst bar turns green while it lasts.
- Poisoning, a worse illness from bad water. It poisons for 10 seconds on Easy, 20 on Normal and 30 on
  Hard. Milk cures it.
- On Fabric, pipes and tanks from other mods can fill and empty waterskins and terracotta bowls, as
  they already could on NeoForge. The water keeps its grade.
- Compatibility with Create Fly on Fabric 26.1 and 26.2:
  - A Spout fills waterskins and terracotta bowls, and an Item Drain empties them.

### Changed

- Bad water makes players ill by difficulty. Each drink can give Upset Stomach or Poisoning, more
  often on harder difficulties. Pure water is always safe, and on Peaceful Dirty and Murky water only
  cause a short Nausea.
- Drinking again while ill makes the illness last longer, and Upset Stomach I becomes II.
- Every drink of fresh water now quenches thirst, even one that makes the player ill.
- Nausea from sea water lasts eight seconds instead of five, long enough for the screen to warp.
- The old Nausea and Poison can be brought back with the new Classic sickness setting. The sickness
  settings changed. See the details below.
- A waterskin fills in one go from water, a cauldron or a hanging pot. From a cauldron or a pot it
  takes only as much as there is.
- Pouring out a waterskin splashes like pouring a water bottle on dirt.
- Dirty water gives no quenched and Murky water half, from any container or by hand. Bad water no
  longer keeps the bar topped up, and Upset Stomach also cuts the quenched of every drink.
- Drinking by hand restores 3 thirst a sip instead of 2, and splashes the water.
- A new config screen, with fewer settings. Each group of settings has a tab on the left, and a search
  box finds a setting on any page. Changed settings are shown in amber, each with its own reset
  button. The AppleSkin page keeps the live preview. Rarely changed values are now fixed. See the
  details below.

### Notes

- Data packs and the API change nothing until a data pack or another mod uses them.
- On Fabric, Kaleidoscope Cookery's teas and soups restore thirst too, but its Stockpot and Teapot do
  not keep the water's grade yet. Kaleidoscope Cookery Refabricated is the supported Fabric build.
- Settings removed from the config file are not carried over. Their old defaults apply.

<details>
<summary>Configuration file details</summary>

- Removed `quenchWhenDebuffed`, `nauseaChance`, `poisonChance` and `nauseaSeconds`, replaced by
  `sicknessPreset`, `REALISTIC` or `CLASSIC`.
- Removed, now fixed at their old defaults: `netherThirstDepletionModifier` (3.0),
  `fireResistanceDehydrationPercent` (50), `depletesWhenNauseous` (on),
  `drinkByHandNeedsBothHandsEmpty` (off), `extraThirstConvertsToQuenched` (on), `rainwaterPurity`
  (Clean), `dripstonePurity` (Pure), `copperPotSecondsPerServing` (4) and `ironPotSecondsPerServing`
  (6). `handDrinkingThirst` and `handDrinkingQuenched` are removed too, fixed at 3 and 2.
- Removed keys are ignored if still in the file.

</details>

## [1.0.9.1] - 2026-09-21

### Fixed

- NeoForge clients no longer crash on startup when Sophisticated Core is missing. Sophisticated
  Backpacks and Sophisticated Storage both ship it, so the crash hit any client without one of them.

### Notes

- NeoForge only. The Fabric files are unchanged, and dedicated servers were never affected.
- Minecraft 26.3 was not affected, because Sophisticated Backpacks has no build for it yet.

## [1.0.9] - 2026-09-20

### Added

- Support for Minecraft 26.3 on Fabric and NeoForge.
- Supplementaries support on Minecraft 1.21.1, Fabric and NeoForge. Water keeps its grade in Jars,
  Goblets and Faucets, and sea water stays sea water. Before, a Jar handed out Clean water whatever
  went in.
- A Jar or a Goblet of water can be drunk with an empty hand.
- A Faucet fills and empties Copper and Iron Hanging Pots, and grades the water it draws from a lake
  or a pool the way filling a bottle there does.
- Terracotta bowls fill and empty in Jars, Goblets and Faucets like a glass bottle.
- The water in a Jar or a Goblet is coloured by its grade. A Jar broken while it holds water names the
  grade on its tooltip, and Jade names it under the crosshair.

### Notes

- The 26.3 NeoForge file needs a NeoForge beta, because no release build exists for 26.3 yet.
- On 26.3, Sophisticated Backpacks, Create Fly and the Sand Filter are unavailable until those mods
  build for it.
- Supplementaries is supported on Minecraft 1.21.1 only, because it has no build for a newer version.
- Nothing changed on 26.2, 26.1.x or 1.21.11. Existing worlds and config files need no changes.

## [1.0.8] - 2026-09-20

### Added

- Farmer's Delight support on NeoForge for Minecraft 1.21.1: its drinks and meals restore thirst, the
  Cooking Pot boils water Pure, and Nourishment stops the thirst bar from draining.
- Sophisticated Backpacks support on NeoForge: the Drinking Upgrade and Advanced Drinking Upgrade
  drink from the backpack when the thirst bar is low, the cleanest water first. They also fit
  Sophisticated Storage.
- In a Sophisticated backpack, water keeps its grade in the Tank and Pump Upgrades, and the Feeding
  and Alchemy Upgrades restore thirst.
- Smokers, and the Smoking Upgrade, purify water in half the time of a furnace.
- On NeoForge, other mods' tanks, pumps and Spouts can fill and empty waterskins and terracotta water
  bowls. The water keeps its grade.
- The Parched effect. Thirst drains faster while it lasts, and the thirst bar turns the colour of dry
  sand.
- A Legacy quenched outline, the blue one from the original Thirst Was Taken.

### Changed

- Bad water no longer causes Hunger.
- Sea water also causes Parched II.
- The advancement tab is now named ThirstWasTaken2, and advancements have plainer names and
  descriptions.
- The creative tab icon is now a waterskin.
- Nausea from bad water lasts longer the worse the water: 12 seconds from Dirty water and 8 from
  Murky, instead of 5. Its extra thirst drain lasts as long.
- Drinking by hand restores 2 thirst and 2 quenched instead of 1 each.

### Fixed

- After `/thirst set` with a quenched of 0, the top droplet no longer shows as partly drained.

### Notes

- Farmer's Delight on the other NeoForge versions is not supported yet.
- Sophisticated Backpacks is supported on every NeoForge version and on no Fabric version.
- Existing config files keep their hand drinking values. Set `handDrinkingThirst` and
  `handDrinkingQuenched` to 2 for the new default.

<details>
<summary>Configuration file details</summary>

- New `nauseaSeconds`: `[12, 8, 5, 5]` by default, one value per grade from Dirty to Pure.
- `handDrinkingThirst` and `handDrinkingQuenched` now default to `2`.

</details>

## [1.0.7] - 2026-09-18

### Added

- Copper and Iron Hanging Pots. Placed on a lit campfire, they boil water into Pure water. The iron
  pot boils slower.
- With Jade installed, looking at a hanging pot shows the water grade inside.
- The Sand Filter on NeoForge for Minecraft 1.21.1, with Create installed. Water also keeps its grade
  through Create's pipes, pumps, Spouts and drains.

### Changed

- The license changed from MIT to the GNU General Public License v3.0.

### Notes

- The Copper and Iron Hanging Pots are adapted from the campfire cauldron in Dehydration by Globox1997.
- Create is optional and must be installed on both the client and the server.
- Existing worlds and config files need no changes.

<details>
<summary>Configuration file details</summary>

- New keys `copperPotSecondsPerServing`, default `4`, and `ironPotSecondsPerServing`, default `6`:
  how long each serving in that hanging pot takes to boil.

</details>

## [1.0.6] - 2026-09-16

### Added

- A NeoForge version for every supported Minecraft version: 26.2, 26.1, 26.1.1, 26.1.2, 1.21.11 and
  1.21.1. It has the same features as the Fabric version, with the config screen opened from the
  NeoForge mods list. The NeoForge file has `-neoforge` after the Minecraft version in its name.
- AppleSkin and Jade work on NeoForge too.

### Fixed

- The thirst bar vanished on the death screen. It now stays on screen with the hunger bar, showing
  the thirst the player died with.

### Notes

- Create Fly and Farmer's Delight have no NeoForge release, so the Sand Filter and the Farmer's
  Delight support are Fabric only for now.
- On NeoForge, Minecraft 1.21 is not covered. The 1.21.1 file is for 1.21.1 only, where the Fabric
  one covers both.
- A world made with the Fabric version and then opened with the NeoForge version, or the other way
  round, starts every player at full thirst. Everything else in the world is unchanged.
- Nothing changes for existing Fabric worlds or config files.

## [1.0.5] - 2026-09-14

### Added

- With Jade installed, looking at water, a waterlogged block or a water cauldron shows its grade, or
  Salty for sea water. It can be turned off in Jade's plugin settings.
- Drinks from other mods now restore thirst without a config entry, as long as their mod marks them
  as drinks.
- Farmer's Delight support. Its milk bottle, hot cocoa, bone broth, onion soup, glow berry custard and
  tomato now restore thirst, alongside the drinks and meals already covered.
- With Farmer's Delight, the Cooking Pot boils a fresh water bottle or water bowl pure in one pass.
- With Farmer's Delight, Nourishment stops the thirst bar from draining.
- The Sand Filter is back, now with Create Fly on Minecraft 26.1.2 and 26.2. Water pumped through it comes out one
  grade cleaner.
- With Create Fly, water keeps its grade through pumps, pipes, Spouts, Basins and Item Drains.

### Changed

- Built-in thirst values and water grades for Farmer's Respite, Brewin' and Chewin', Collector's Reap
  and Tough As Nails were removed. Farmer's Delight is the only food mod with built-in values for now.

### Notes

- Jade is optional and only needed on the client.
- Create Fly is optional and must be installed on both the client and the server. The other Minecraft
  versions ignore it for now.
- Existing config files get the new Farmer's Delight values automatically. Values for the removed
  mods stay in existing config files until deleted by hand.

<details>
<summary>Configuration file details</summary>

- New `enableDrinkTagMatching`: `true` by default.
- New `drinkTagValue`: `[6, 8]` by default.

</details>

## [1.0.4] - 2026-09-13

### Added

- Support for Minecraft 1.21 and 1.21.1.
- The quenched outline on the thirst bar now comes in four colours: Diamond, Ice, Gold and
  AppleSkin's own gold. Turning it off also hides the exhaustion strip.
- A setting to hide the thirst and quenched droplets in item tooltips.
- The config screen is split into pages, shows a live preview of the thirst bar and a drink's
  tooltip, and has Cancel and Reset to Defaults buttons.

### Changed

- Chests and Piglin bartering that a data pack replaced now get the mod's water bottles as well.
- The quenched outline and the tooltip droplets now appear only with AppleSkin installed, the same
  way AppleSkin shows saturation and food values. The outline is cyan by default.

### Fixed

- Mineshaft chests held no water bottles in worlds created with the Villager Trade Rebalance
  experiment.
- The quenched outline had stray pixels outside the droplet at a quarter, half and three quarters
  full.
- Low thirst did not stop sprinting. Players can no longer sprint at 6 thirst or below, as intended.
- Dehydration damage knocked players back. It now hurts without knockback, like drowning.
- Drinking by hand did not work on deep water or a waterfall when no block was within reach.

### Notes

- On Minecraft 1.21 and 1.21.1, sea water in bottles and buckets looks like ordinary water, and
  the droplets in tooltips have a shadow. The Salty tooltip line and the sea-coloured bowl are
  unchanged.
- No action is needed for existing worlds or config files.

<details>
<summary>Configuration file details</summary>

- New `appleskinQuenchedOverlay`: `DIAMOND` (default), `ICE`, `GOLD`, `APPLESKIN` or `OFF`.
- New `appleskinTooltipDroplets`: `true` by default.

</details>

## [1.0.3] - 2026-09-12

### Added

- Milk buckets and honey bottles restore thirst.
- An advancement tab for the mod, from the first drink of water to drinking in the Nether.
- Rain and pointed dripstones fill cauldrons with a quality of their own. Rain is clean and
  dripstone water is pure, where both used to fall back to the default quality.

### Changed

- Sea water is no longer a grade of fresh water. Bottles, buckets and bowls of it look different
  from fresh ones, its tooltip reads Salty where fresh water shows a grade, and it shows no
  thirst droplets, because it restores nothing.
- The grades of fresh water are now Dirty, Murky, Clean and Pure, in all nine languages, with
  tooltip colours that are easier to tell apart.
- A waterskin now averages the grades of the drinks inside it, rounded down. One salty drink still
  turns all of it into sea water, and cauldrons still keep the worse of the two.
- The clay bowl now says it has to be smelted before it can hold water.
- Thirst uses less server time and memory and sends far fewer updates, which matters most on busy
  servers.
- The partly drained droplet and AppleSkin's exhaustion strip behind the thirst bar now move in
  small steps instead of every tick.
- Renamed the "hydration" wording to "thirst" everywhere it named the bar's value, so the mod uses
  one word for it. Two config keys changed with it: `handDrinkingHydration` is now
  `handDrinkingThirst` and `extraHydrationConvertsToQuenched` is now `extraThirstConvertsToQuenched`.
  Custom values for those two settings reset to their defaults on first launch; every other setting
  is untouched. The `ThirstApi.hydration(...)` methods were also renamed to `ThirstApi.thirstValues(...)`.

### Fixed

- Recipes added by the mod never appeared in the recipe book, so the clay bowl, the bowls, the
  waterskin and the purification recipes had to be looked up outside the game.
- Water from structure chests and Piglin bartering could not be boiled.
- Sea water could report a grade, so water from a frozen ocean read as the cleanest water in the
  game while still being undrinkable.

### Notes

- Servers and players must run the same version. A mismatch shows the wrong blocks in the world.
- Existing worlds and config files need no action. Sea water left in a cauldron in an older world
  becomes fresh water of that cauldron's grade; sea water in containers is unaffected. Two settings,
  `rainwaterPurity` and `dripstonePurity`, are added to the config file on first launch.

## [1.0.2] - 2026-09-10

### Added

- Support for Minecraft 26.1 and 1.21.11, alongside 26.2.

### Changed

- Redesigned water droplet sprites on the thirst HUD and item tooltips.
- Redesigned waterskin sprites across all filling stages.
- A filled terracotta water bowl no longer stacks, matching water bottles and buckets. One bowl is
  one drink, so the waterskin and the three it holds are worth carrying again.

### Removed

- All Create support. The Sand Filter and its recipe are gone. The Sand Filter never worked, so nothing playable is lost.

### Notes

- Create support is planned to return in a future release.
- No action is needed for existing worlds or config files.
- Each Minecraft version has its own download, named after it, for example
  `ThirstWasTaken2-1.0.2+1.21.11.jar`.

## [1.0.1] - 2026-08-11

### Added

- A waterskin that holds three drinks, mixes water of different purity, fills from cauldrons and
  trades water with bottles and buckets.
- Filled waterskins can now be emptied by sneaking and using them on a block.
- Water is now graded by its surroundings when it is collected, and salt water stays salty wherever
  it is moved.
- Water bowls now look different at each purity level.
- Optional AppleSkin integration that shows thirst exhaustion behind the thirst bar whenever
  AppleSkin's exhaustion-underlay option is enabled.

### Changed

- Drinking directly from water with an empty hand is now enabled by default.
- Water quality now starts from the biome and is adjusted by temperature, altitude, whether the water
  flows, and nearby mud, farmland or composters, instead of treating all high, deep or flowing water
  as clean.
- Ocean water no longer hydrates and cannot be made drinkable by cooking. It stays salty through
  waterskins, cauldrons and the sand filter.
- The creative tab icon and its water bowl now show purified water, and purity tooltip colors match
  the original mod.
- Filled waterskins are easier to read at a glance, so the three serving levels can be told apart.
  The empty waterskin is unchanged.
- Item tooltips now show thirst as filled droplets, with quenched as outlined droplets on a
  second row.
- Plain water now follows vanilla food behaviour and cannot be consumed while the thirst bar is
  full. Potions and foods with other uses remain available.

### Removed

- Outdoor rain drinking and its `canDrinkRain` configuration option.

## [1.0.0] - 2026-08-11

Initial release of ThirstWasTaken2, a Fabric fork of
[Thirst Was Taken](https://github.com/ghen-git/Thirst-Mod) for Minecraft 26.2.

### Added

- Rebuilt the original Forge mod for Fabric on Minecraft 26.2.
- Thirst, quenched and exhaustion systems, including faster dehydration while sprinting,
  fighting, taking damage, or travelling through hot and dry environments.
- Dehydration penalties: damage, disabled sprinting and disabled natural health regeneration when
  out of water.
- Peaceful-mode thirst regeneration and protection from dehydration while riding a mount.
- Drinking from potions, supported modded drinks, watery foods, water bowls, water sources and rain.
- Four water-purity levels, with nausea and poison risks from unsafe water.
- Water purification in furnaces and on campfires for bottles, bowls and buckets.
- Purity-aware cauldrons that retain the quality of water poured into them.
- Clay bowls, terracotta bowls and terracotta water bowls, including support for collecting flowing
  water with a terracotta bowl.
- Water bottles in dungeon, mineshaft, shipwreck, Nether fortress and bastion loot, as well as
  Piglin bartering.
- `/thirst` commands for querying and setting thirst or enabling and disabling thirst per player.
- Built-in droplet tooltips for food and drink thirst values; AppleSkin is no longer required.
- A five-stage droplet sprite set for smoother exhaustion feedback.
- A permanently visible quenched reserve outline on the thirst bar.
- A Mod Menu configuration screen covering every setting.
- JSON configuration at `config/thirstwastaken2.json` for manual editing.
- Configurable thirst-bar positioning anywhere on the screen.
- An option to require both hands to be empty before drinking directly from water.
- A dedicated ThirstWasTaken2 creative inventory tab.
- Vietnamese localization alongside English, French, Japanese, Korean, Polish, Russian, Simplified
  Chinese and Traditional Chinese.

### Changed from the original

- Drinking directly from a water source now restores **1 thirst** and **1 quenched**, reduced from
  3 thirst and 2 quenched. Both values remain configurable.
- Empty thirst droplets now use the same dark shade as empty hunger icons.
- Server gameplay settings are authoritative; only HUD settings are controlled by each client.

### Known issues and unavailable integrations

- The Create Sand Filter is disabled because Create Fly integration is not yet available for this
  Minecraft/Fabric version.
- Jade does not currently display water purity.
- Cold Sweat, Farmer's Respite, Brewin' and Chewin', Tough As Nails, Supplementaries and Botania
  integrations are awaiting compatible Minecraft 26.2 Fabric releases.
