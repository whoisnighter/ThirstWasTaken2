# Water quality

Fresh water has a grade, from Dirty to Pure. Sea water is Salty, has no grade, and never quenches
thirst. Every container shows which one it holds in its tooltip.

![A water bottle tooltip stepping through Dirty, Murky, Clean, Pure and Salty](/screenshots/water/water-tooltips.gif)

Collect water, boil it **Clean**, and distil it **Pure** if a bigger reserve is worth the trouble.
Clean water is already safe to drink.

## The four grades

From worst to best: **Dirty**, **Murky**, **Clean**, **Pure**. Water gets its grade where it is
collected and keeps it.

| Grade | Usually found in | Drinking it |
|---|---|---|
| **Dirty** | Swamps, jungles, savannas, badlands | Usually makes the player ill, no quenched |
| **Murky** | Rivers, most other biomes, every cave | Can make the player ill, a little quenched |
| **Clean** | Mountains, rain | Safe |
| **Pure** | Cold peaks, dripstone | Safe, and the most quenched |
| **Salty** | Oceans and beaches | Never quenches thirst, gives [Parched](#parched) |

How much each grade restores is on the [drinking page](/docs/features/drinking#what-is-worth-drinking),
and how likely Dirty and Murky water are to make the player ill is under
[Drinking bad water](#drinking-bad-water).

![](https://cdn.modrinth.com/data/8oi3bsk5/1959d924a1088944bbf07a06ba523726112d7e7a_96.webp){.mod-icon} [Terralith](https://modrinth.com/mod/terralith)'s Orchid Swamp and Ice Marsh count as swamps, so their water is Dirty.

- Hot biomes make water worse, cold biomes make it better.
- Water above y 100 is a little cleaner. So is flowing water.
- Mud, mangrove roots, farmland or a composter within two blocks make water worse.
- Water the sky can't reach is at best Murky: a cave pool, or a pond under a roof or leaves. Rain
  doesn't renew it, so it needs boiling like any other risky water.

Modpacks can add biomes to the `thirstwastaken2:stagnant_water` tag, coastal biomes whose water is
salty to the `thirstwastaken2:sea_water` tag, and fluids that are always Pure to the
`thirstwastaken2:pure_water` fluid tag. Water with no grade of its own
uses [defaultQuality](/docs/configuration#defaultquality).

## Checking water with Jade

![Jade showing Murky for the river water under the crosshair](/screenshots/water/jade-water.png)

With ![](https://cdn.modrinth.com/data/nvQzSEkH/b04217bc2b7dc524c4d12f81ff42cc1cefb9b0fc_96.webp){.mod-icon} [Jade](https://modrinth.com/mod/jade) installed, looking at water, a waterlogged block, a water
cauldron or a hanging pot shows its grade, or Salty. On a [Copper Distiller](#copper-distiller) it
shows both tanks and whether the cooling tub is dry. Each can be turned off in Jade's plugin settings.

## Salt water

Oceans and beaches give salt water. It has its own icon and tooltip line. On Minecraft 1.21 and
1.21.1 only the bowl has its own icon.

- Drinking it costs thirst, causes eight seconds of Nausea and 30 seconds of Parched II.
- It cannot be boiled clean. Only a [Copper Distiller](#copper-distiller) makes it drinkable.
- One salty drink makes a whole waterskin, cauldron or hanging pot salty.

### Parched

![Parched effect icon](/icons/parched.png){.effect-icon}

Parched makes thirst drain faster, the way Hunger does for food: 4 points a minute at level I, 8 at
level II, on top of the usual drain and whatever the climate. The thirst bar turns the colour of dry
sand while it lasts.

![The thirst bar in dry sand colours while Parched](/screenshots/hud/parched-hud.png)

## Mixing and cauldrons

- A waterskin takes the average grade of its drinks, rounded down.
- A cauldron keeps the worse grade of what it holds and what is poured in.

A cauldron also fills on its own:

| How it filled | Grade |
|---|---|
| Rain | Clean |
| A pointed dripstone dripping into it | Pure |

Neither improves water already in the cauldron.

## Drinking bad water

Fresh water always restores thirst. Clean and Pure water are always safe. Dirty and Murky water can
make the player ill, Peaceful included, more often on harder difficulties.

Bad water also leaves little reserve: Dirty water gives no quenched and Murky water only a quarter
of a serving's, so thirst starts dropping again soon after.

Each drink rolls once. Upset Stomach comes on the chance below, and Poison only on a worse roll, so
Poison always comes with Upset Stomach.

| Chance per drink | Dirty | Murky |
|---|---|---|
| Peaceful | 35% Upset Stomach I, 5% also Poison | 15% Upset Stomach I, 2% also Poison |
| Easy | 50% Upset Stomach I, 8% also Poison | 25% Upset Stomach I, 3% also Poison |
| Normal | 75% Upset Stomach II, 20% also Poison | 50% Upset Stomach I, 8% also Poison |
| Hard | 90% Upset Stomach II, 30% also Poison | 70% Upset Stomach II, 15% also Poison |

Drinking bad water again while still ill makes Upset Stomach last longer, by half the new dose, but
never more than one and a half doses from now. The stronger level is kept. This can be switched off
with [extendSicknessEffects](/docs/configuration#extendsicknesseffects).

Every effect, chance, duration and level can be changed for each difficulty and grade, and any effect
added, with [sicknessEffects](/docs/configuration#sicknesseffects).

### Upset Stomach

![Upset Stomach effect icon](/icons/upset-stomach.png){.effect-icon}

The common one. It stops natural healing until it wears off, however full the bars are, and cramps
cost half a heart at random times. Like Poison, cramps stop at half a heart, so Upset Stomach alone
never kills, and they hurt on Peaceful too.

- The wait between cramps is random, shorter on harder difficulties:

  | | Level I | Level II |
  |---|---|---|
  | Peaceful | 10 to 15 seconds | 7.5 to 11 seconds |
  | Easy | 6 to 15 seconds | 4.5 to 11 seconds |
  | Normal | 3 to 12 seconds | 2.25 to 9 seconds |
  | Hard | half a second to 8 seconds | half a second to 6 seconds |

- Armour doesn't soften a cramp, and like any damage it wakes a sleeping player.
- Milk cures it.
- The thirst bar turns green while it lasts.

How long it lasts:

| | Peaceful | Easy | Normal | Hard |
|---|---|---|---|---|
| From Dirty water | 20 seconds | 30 seconds | 45 seconds, level II | 60 seconds, level II |
| From Murky water | 15 seconds | 20 seconds | 30 seconds | 45 seconds, level II |

![The thirst bar in green while the player has Upset Stomach](/screenshots/hud/upset-stomach-hud.png)

### Poison

A bad batch. Milk and honey cure it.

| | Peaceful | Easy | Normal | Hard |
|---|---|---|---|---|
| From Dirty water | 8 seconds | 10 seconds | 20 seconds | 30 seconds |
| From Murky water | 5 seconds | 8 seconds | 15 seconds | 20 seconds |

Poison stops at half a heart, so it never kills.

## Cleaning fresh water

Heat makes Dirty and Murky water Clean, and goes no further. Only a
[Copper Distiller](#copper-distiller) makes Pure water, from any water, sea water included. The Create
[Sand Filter](/docs/integrations/create#sand-filter) also cleans water a grade at a time, up to Pure.

Ways to boil water:

- A [Copper Canteen or a flask](#boiling-in-a-canteen-or-flask) held over a campfire.
- A [Hanging Pot](#copper-hanging-pot) on a campfire, which needs no attention.
- A furnace or a smoker, below.
- The Farmer's Delight [Cooking Pot](/docs/integrations/farmers-delight/#boiling-water-in-the-cooking-pot).

Clean and Pure water have nothing left to boil, so heat never takes them, and nothing boils salt out.

### Furnace and smoker

![A dirty water bottle comes out of the furnace clean](/screenshots/water/furnace-clean-water.png)

A water bottle, terracotta water bowl, water bucket, Copper Canteen or flask of Dirty or Murky
water comes out Clean. No experience is given.

| | Furnace | Smoker |
|---|---|---|
| Bottle or bowl | 8 seconds | 4 seconds |
| Bucket | 24 seconds | 12 seconds |
| Copper Canteen | 2 seconds a drink | No |
| Iron Flask | 3 seconds a drink | No |
| Diamond or Netherite Flask | 2 seconds a drink | No |

A canteen or flask takes as long as the drinks inside, so a half-full one is done in half the time.
A stack of bowls is boiled one at a time. Servers can turn furnace boiling off with
[enableFurnaceBoiling](/docs/configuration#enablefurnaceboiling).

## Boiling in a canteen or flask

Hold use on a lit campfire or soul campfire with a [Copper Canteen or a flask](/docs/features/drinking#copper-canteen-and-flasks)
that holds water. The progress shows above the hotbar, and when it is done all the water inside is
Clean.

| | Per drink | Full |
|---|---|---|
| Copper Canteen | 2 seconds | 8 seconds |
| Iron Flask | 3 seconds | 18 seconds |
| Diamond Flask | 2 seconds | 20 seconds |
| Netherite Flask | 2 seconds | 30 seconds |

- Letting go keeps the progress. Adding water starts it over.
- Salt water never boils clean.

## Copper Hanging Pot

![A Copper Hanging Pot of water boiling over a campfire](/screenshots/water/copper-hanging-pot.png)

Placed on a lit campfire or soul campfire, it boils water Clean on its own. Placed on the ground, it
stands a block up on its legs, so a campfire can go under it later. Putting any other solid block
under it knocks the pot off.

![A Copper Hanging Pot standing on the grass on its own legs, with room for a campfire under it](/screenshots/water/copper-hanging-pot-ground.png)

![Two sticks and a chain across the top, five copper ingots in a U below, make a Copper Hanging Pot](/screenshots/recipes/copper-hanging-pot-recipe.png)

On Minecraft 1.21 and 1.21.1 the recipe uses a chain instead of an iron chain.

- Holds three servings, a bucket. A bucket fills or empties it. A bottle or bowl adds or takes one.
- A waterskin fills up from it in one go, as far as the pot has water. Sneak to pour all of it in.
- Each serving takes 3 seconds.
- Adding water only adds that water's time. Putting the fire out pauses the boil.
- The water changes colour with its grade.
- It mixes like a cauldron, and salt water never boils clean.
- Rain fills it slowly, with Clean water.
- Water cannot be poured into it in the Nether.
- Breaking it drops the pot. The water is lost.

### Iron Hanging Pot

![An Iron Hanging Pot of water boiling over a campfire](/screenshots/water/iron-hanging-pot.png)

Works like the Copper Hanging Pot but holds six servings, two buckets, and boils slower: 4 seconds a
serving.

![Two sticks and a chain across the top, five iron ingots in a U below, make an Iron Hanging Pot](/screenshots/recipes/iron-hanging-pot-recipe.png)

## Copper Distiller

A two-block still that turns any water into Pure water, sea water included. It burns furnace fuel
and runs on its own, so hoppers can keep it going.

![A Copper Distiller up close, its fire burning: the boiler on its brick firebox, the swan neck pipe running over to the cooling tub, and the tap over the basin](/screenshots/water/copper-distiller-front.png)

It is made of four parts: a Brick Firebox, a Distiller Boiler, a Cooling Tub and a Copper Pipe.

![Three copper ingots in a row make four Copper Pipes](/screenshots/recipes/copper-pipe-recipe.png)

![Seven copper ingots in a ring round an iron ingot, a Copper Pipe in the middle of the top row, make a Distiller Boiler](/screenshots/recipes/distiller-boiler-recipe.png)

![Five Copper Pipes around a barrel, a cauldron below, make a Cooling Tub](/screenshots/recipes/cooling-tub-recipe.png)

![Three smooth stone slabs on top, a campfire in a ring of bricks below, make a Brick Firebox](/screenshots/recipes/brick-firebox-recipe.png)

Craft the four together into a whole distiller, or build it in the world:

![The Distiller Boiler beside a Copper Pipe, the Brick Firebox under the boiler and the Cooling Tub under the pipe, make a Copper Distiller](/screenshots/recipes/copper-distiller-recipe.png)

1. Place the Distiller Boiler on top of the Brick Firebox.
2. Place the Cooling Tub to its right, as seen from the front, facing the same way.
3. Use a Copper Pipe on either of them to join the two.

Before it works, fill the cooling tub once with any water container. Sea water will do. Without it
nothing condenses, and the arrow in the distiller's screen turns red.

![The Copper Distiller's screen: an emptied bucket in the water slot, coal burning, sea water in the boiler tank, Pure water in the basin tank, and a bottle filled from it](/screenshots/water/copper-distiller-gui.png)

- Right-click with an empty hand to open its screen: water in, fuel, an empty container to fill, and
  the two tanks.
- Right-click with a water container to pour it into the boiler, and with an empty one to draw
  Pure water from the basin under the tap. A waterskin, canteen or flask draws on a click and pours
  when sneaking.
- Each serving takes 8 seconds while the fire burns. Coal runs ten servings. The fire only burns
  while there is water to boil and room in the basin, so no fuel is wasted.
- The boiler and the basin each hold 9 servings, three buckets.
- Hoppers feed water in from above, one container at a time, and fuel or empty containers from the
  sides, and take filled containers and salt out from below.
- Other mods' pipes and pumps, such as Pipez, fill the boiler with water of any grade and draw Pure
  water out of the basin, through either half. On Forge 1.20.1 only the boiler half takes a pipe.
- Breaking it drops the distiller and whatever its slots held. The water in its tanks is lost.

With Jade installed, looking at it shows what each tank holds and its grade.

![Jade on a Copper Distiller: two drinks of Salty water in the boiler and four of Pure water in the basin](/screenshots/water/copper-distiller.png)

### Salt

When another mod in the pack adds salt, such as Croptopia, Spelunkery, Expanded Delight or Hearth
and Harvest, every bucket of sea water distilled leaves one of that salt behind. The distiller then
shows a salt slot. Without such a mod there is no salt and no slot. When the salt slot is full, the
sea water waits until it is emptied.
