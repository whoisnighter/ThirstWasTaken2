# Where sea water is refused

Sea water is `WaterQuality.Salt`. It is a different kind of water, not a low grade of fresh water: it
carries `water_salty: true` and no `water_purity`. This page lists every place that turns it away,
every place that takes it but leaves it salty, and the hanging pot, which takes it and mixes it in.
Checked against the code on 2026-10-01.

A check that keeps sea water out reads `WaterPurity.isSalty(stack)` on a stack,
`WaterPurity.storedQuality(state) == WaterQuality.SALT` on a block, or
`instanceof WaterQuality.Fresh` on a quality it already has.

## Refused: nothing happens

### Core

| What | Where | How |
|---|---|---|
| Boiling a canteen or flask on a campfire | [WaterskinItem.java](../../../src/main/java/com/thirstwastaken2/item/WaterskinItem.java) `useOn` | The click is spent and the player sees `thirstwastaken2.message.cannot_boil_salt`. The water stays salty |
| Boiling in a hanging pot | [HangingPotBlock.java](../../../src/main/java/com/thirstwastaken2/block/HangingPotBlock.java) `needsBoiling` | Only fresh water below Pure boils, so sea water sits in the pot unchanged. Gametest `anUnlitCampfireOrSaltWaterDoesNotBoil` |
| Purification recipes: furnace, smoker, campfire, Cooking Pot | [ThirstRecipeProvider.java](../../../src/datagen/java/com/thirstwastaken2/datagen/ThirstRecipeProvider.java) | Every ingredient asks for a `water_purity` and for `water_salty: false`, so sea water matches no recipe |
| Grade in the API | [ThirstApi.java](../../../src/main/java/com/thirstwastaken2/api/ThirstApi.java) `purity` | Sea water reports `NOT_WATER` to other mods; `isSalt` tells them why |

### Integrations

| Mod | Where | What is refused | Nodes |
|---|---|---|---|
| Farmer's Delight Refabricated | [CookingPotMatching.java](../../../src/main/farmersdelight/java/com/thirstwastaken2/farmersdelight/CookingPotMatching.java) | The Cooking Pot boiling a sea water bottle into Pure. Refabricated never tested component ingredients; this tests them. NeoForge and Forge Farmer's Delight test them already | Fabric |
| Expanded Delight | [CookingPotSeaWaterMixin.java](../../../src/main/expandeddelight/java/com/thirstwastaken2/expandeddelight/mixin/CookingPotSeaWaterMixin.java) | Every Cooking Pot recipe with sea water in one of its six ingredient slots, not only Expanded Delight's, unless the recipe asks for sea water itself (Hearth and Harvest's salt, below) | `1.21.1-neoforge` |
| Brewin' and Chewin' | [KegFermentingMixin.java](../../../src/main/brewinandchewin/java/com/thirstwastaken2/brewinandchewin/mixin/KegFermentingMixin.java) | Fermenting in a keg holding sea water. The keg still stores it and hands it back salty, and a keg already brewing stops | both 1.21.1 nodes |
| Cultural Delights | [VatSeaWaterMixin.java](../../../src/main/culturaldelights/java/com/thirstwastaken2/culturaldelights/mixin/VatSeaWaterMixin.java), [VatWater.java](../../../src/main/culturaldelights/java/com/thirstwastaken2/culturaldelights/VatWater.java) | Brewing in a vat with a sea water bucket in any slot. The bucket may sit in its slot; a brew in progress stops | `1.21.1-neoforge` |
| Fruits Delight | [WaterBottleIngredientMixin.java](../../../src/main/fruitsdelight/java/com/thirstwastaken2/fruitsdelight/mixin/WaterBottleIngredientMixin.java) | A sea water bottle counting as the water bottle a recipe asks for | `1.21.1-neoforge`, `1.20.1-forge` |
| Fruits Delight | [FruitCauldronMixin.java](../../../src/main/fruitsdelight/java/com/thirstwastaken2/fruitsdelight/mixin/FruitCauldronMixin.java) | Lemon slice or jam into a full cauldron of sea water, by hand or by dispenser | `1.21.1-neoforge`, `1.20.1-forge` |
| Kaleidoscope Cookery | [TeapotBlockEntityMixin.java](../../../src/main/kaleidoscope/java/com/thirstwastaken2/kaleidoscope/mixin/TeapotBlockEntityMixin.java) | Brewing in a teapot holding sea water. The tick is skipped and the ingredients wait | every Kaleidoscope node |
| Farm & Charm | [SeaWaterIngredientMixin.java](../../../src/main/farmandcharm/java/com/thirstwastaken2/farmandcharm/mixin/SeaWaterIngredientMixin.java) | A sea water bucket in a recipe of the Cooking Pot, the Stove, the Crafting Bowl, or Candlelight's Large Cooking Pot. The bucket sits in its slot and nothing cooks | both 1.21.1 nodes |
| Farm & Charm | [WaterTroughMixin.java](../../../src/main/farmandcharm/java/com/thirstwastaken2/farmandcharm/mixin/WaterTroughMixin.java) | A sea water bucket poured into a Water Trough, which would hand it back fresh | both 1.21.1 nodes |
| Candlelight | [SinkMixin.java](../../../src/main/farmandcharm/java/com/thirstwastaken2/farmandcharm/mixin/SinkMixin.java) | A sea water bucket poured into a kitchen sink, Farm & Charm's sink block, which would hand it back fresh | both 1.21.1 nodes |
| HerbalBrews | [TeaKettleMixin.java](../../../src/main/herbalbrews/java/com/thirstwastaken2/herbalbrews/mixin/TeaKettleMixin.java) | A sea water bucket or bottle in the Tea Kettle's water slot. It sits in the slot and fills nothing | both 1.21.1 nodes |
| Hearth and Harvest | [SinkBlockMixin.java](../../../src/main/hearthandharvest/java/com/thirstwastaken2/hearthandharvest/mixin/SinkBlockMixin.java) | Sea water poured into the Sink, which would hand it back as its Murky water. The click is spent | `1.21.1-neoforge` |
| Hearth and Harvest | [CaskBlockEntityMixin.java](../../../src/main/hearthandharvest/java/com/thirstwastaken2/hearthandharvest/mixin/CaskBlockEntityMixin.java) | Ageing mead, moonshine or root beer in a Cask with a sea water bottle in it. The bottle waits in its slot | `1.21.1-neoforge` |
| Croptopia | [ShapedRecipeMixin.java](../../../src/main/croptopia/java/com/thirstwastaken2/croptopia/mixin/ShapedRecipeMixin.java), [ShapelessRecipeMixin.java](../../../src/main/croptopia/java/com/thirstwastaken2/croptopia/mixin/ShapelessRecipeMixin.java) | A sea water bucket in any crafting recipe that makes an item of Croptopia's: its sixteen water bottles, tea, steamed rice, soups, dough. The grid keeps its items and shows no result | 26.2, 26.1.x and 1.21.1 on both loaders, 1.20.1 Fabric and Forge |
| Meadow | [AbstractCauldronBlockMixin.java](../../../src/main/meadow/java/com/thirstwastaken2/meadow/mixin/AbstractCauldronBlockMixin.java) | Sea water poured into any of the three wooden cauldrons, which hold no grade and give Murky water. The click is spent | both 1.21.1 nodes |
| Meadow | [CookingCauldronBlockEntityMixin.java](../../../src/main/meadow/java/com/thirstwastaken2/meadow/mixin/CookingCauldronBlockEntityMixin.java) | Sea water poured into the Cooking Pot, which keeps only an amount of water. The click is spent | both 1.21.1 nodes |
| Bountiful Fares | [FermentationVesselBlockMixin.java](../../../src/main/bountifulfares/java/com/thirstwastaken2/bountifulfares/mixin/FermentationVesselBlockMixin.java) | Sea water poured into an empty Fermentation Vessel, which would ferment it into cider or wine. The click is spent and the water stays in the hand | both 1.21.1 nodes |
| Bountiful Fares | [ShapedRecipeMixin.java](../../../src/main/bountifulfares/java/com/thirstwastaken2/bountifulfares/mixin/ShapedRecipeMixin.java), [ShapelessRecipeMixin.java](../../../src/main/bountifulfares/java/com/thirstwastaken2/bountifulfares/mixin/ShapelessRecipeMixin.java) | A sea water cup or bucket in any crafting recipe that makes an item of Bountiful Fares': its six teas and the Sponge Cake. The grid keeps its items and shows no result | both 1.21.1 nodes |
| Cooking for Blockheads | [SinkBlockMixin.java](../../../src/main/cookingforblockheads/java/com/thirstwastaken2/cookingforblockheads/mixin/SinkBlockMixin.java) | Sea water poured into the Sink, which would hand it back as its Murky water. The click is spent | every node |
| Sophisticated Backpacks | [DrinkingUpgradeWrapper.java](../../../src/main/sophisticated/java/com/thirstwastaken2/sophisticated/drinking/DrinkingUpgradeWrapper.java) | The Drinking Upgrade drinking sea water on its own | NeoForge nodes |

## Refused by mixing rules, not by salt

Tanks that compare fluid components will not put two kinds of water together. A tank of fresh water
refuses sea water, and a tank of sea water refuses fresh, as two grades refuse each other:
Sophisticated's tanks, Supplementaries' jars and faucet, Brewin' and Chewin's keg, Create's tanks,
Hearth and Harvest's Jug (on Forge 1.20.1 too), Trough, Sprinkler and Stomping Basin. See
[sophisticated/AGENTS.md](../../../src/main/sophisticated/AGENTS.md) for the measured cases.

## Taken, and kept salty

These take sea water and give it back salty. Nothing here makes it fresh.

| What | Where | What happens |
|---|---|---|
| Drinking | [ThirstManager.java](../../../src/main/java/com/thirstwastaken2/data/ThirstManager.java), [WaterPurity.java](../../../src/main/java/com/thirstwastaken2/purity/WaterPurity.java) | Can be drunk. Restores nothing, makes the player thirstier, and still counts as a drink |
| Waterskin, canteen, flask | [WaterskinItem.java](../../../src/main/java/com/thirstwastaken2/item/WaterskinItem.java) | One salty serving poured in turns the whole vessel salty |
| Create Sand Filter (Create, Create on Forge, Create Fly) | `SandFilterBlockEntity`, [createfly/WaterFluids.java](../../../src/main/createfly/java/com/thirstwastaken2/createfly/WaterFluids.java) | Flows through unfiltered: sand does not take salt out |
| Cold Sweat Boiler | [BoiledWater.java](../../../src/main/coldsweat/java/com/thirstwastaken2/coldsweat/BoiledWater.java) (and `coldsweatforge`) | Accepted into a water slot, never raised, and does not keep the Boiler burning |
| Cold Sweat waterskin on a campfire | [CampfireWaterskinMixin.java](../../../src/main/coldsweat/java/com/thirstwastaken2/coldsweat/mixin/CampfireWaterskinMixin.java) (and `coldsweatforge`) | Cooks and comes back warm, still salty |
| Kaleidoscope Stockpot | [StockpotBlockEntityMixin.java](../../../src/main/kaleidoscope/java/com/thirstwastaken2/kaleidoscope/mixin/StockpotBlockEntityMixin.java) | Cooks soup with it on purpose; the bucket comes back salty |
| Farm & Charm Timber Well | [TimberWellMixin.java](../../../src/main/farmandcharm/java/com/thirstwastaken2/farmandcharm/mixin/TimberWellMixin.java) | A well over sea water, on a beach or by the ocean, gives sea water |
| Kaleidoscope teapot as an item | [TeapotItemMixin.java](../../../src/main/kaleidoscope/java/com/thirstwastaken2/kaleidoscope/mixin/TeapotItemMixin.java) | Holds sea water and empties it back salty |
| Hearth and Harvest's Jug, Trough, Sprinkler and Stomping Basin | [FluidHandlingMixin.java](../../../src/main/hearthandharvest/java/com/thirstwastaken2/hearthandharvest/mixin/FluidHandlingMixin.java), [JugBlockItemMixin.java](../../../src/main/hearthandharvest/java/com/thirstwastaken2/hearthandharvest/mixin/JugBlockItemMixin.java) | Hold sea water poured in, or a Jug scooped from the sea, and hand it back salty |

## Taken only if salty

| Mod | Where | What happens | Nodes |
|---|---|---|---|
| Spelunkery | [AbstractCookingRecipeMixin.java](../../../src/main/spelunkery/java/com/thirstwastaken2/spelunkery/mixin/AbstractCookingRecipeMixin.java) | Its furnace recipe boils a sea water bucket into a salt bucket, and refuses every fresh bucket, which our purification takes instead. Nothing drinkable comes out | both 1.21.1 nodes |
| Hearth and Harvest | `minecraft:salt_from_bottle`, replaced from [FarmersDelightRecipeProvider.java](../../../src/datagen/java/com/thirstwastaken2/datagen/FarmersDelightRecipeProvider.java) on 1.21.1; [SaltFromSeaWaterMixin.java](../../../src/main/hearthandharvestforge/java/com/thirstwastaken2/hearthandharvestforge/mixin/SaltFromSeaWaterMixin.java) on Forge | The Cooking Pot boils a sea water bottle into two salt, and no fresh bottle, which our purification takes instead | `1.21.1-neoforge`, `1.20.1-forge` |

## The hanging pot takes sea water

[HangingPotInteractions.java](../../../src/main/java/com/thirstwastaken2/block/HangingPotInteractions.java)
never asks whether the water is salty. A water bucket, a water bottle (`WaterPurity.isWaterContainer`),
a terracotta water bowl or a sneaking waterskin all pour in whatever quality they hold. The only pour
it refuses is where water evaporates, as in the Nether.

What happens next, in [HangingPotBlock.java](../../../src/main/java/com/thirstwastaken2/block/HangingPotBlock.java):

- **Mixing.** `withPoured` mixes with `WaterQuality.worse`, which returns `SALT` whenever either side is
  salty. One bottle of sea water turns a whole pot salty, Pure water included. That is the same rule as
  a cauldron and a waterskin.
- **Boiling.** `needsBoiling` is false for salt, so the pot never boils it and the boil counter is not
  set.
- **Drawing.** A bottle, bucket, bowl or waterskin drawn from it comes out salty.

So the pot works as storage for sea water, but never purifies it. A player who adds sea water to a
pot of Pure water loses the Pure water with no warning. If that should change, the place is
`HangingPotInteractions.use`: refuse the pour when `poured.salty()` and the pot holds fresh water, the
way a tank refuses a second grade.

Taking the salt out is distillation, which the Copper Distiller does (`block/DistillerBlockEntity`).
