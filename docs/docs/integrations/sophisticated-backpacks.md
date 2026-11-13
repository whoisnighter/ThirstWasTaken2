# ![](https://cdn.modrinth.com/data/TyCTlI4b/e31c7e2f8769d317339e25b2a8d1b40fbf312729_96.webp){.mod-icon} Sophisticated Backpacks

With ![](https://cdn.modrinth.com/data/TyCTlI4b/e31c7e2f8769d317339e25b2a8d1b40fbf312729_96.webp){.mod-icon} [Sophisticated Backpacks](https://modrinth.com/mod/sophisticated-backpacks) installed, the mod
adds the Drinking Upgrade, and water keeps its grade in the backpack's upgrades. The Drinking Upgrade
also fits ![](https://media.forgecdn.net/avatars/thumbnails/543/206/64/64/637872959580005837.png){.mod-icon} [Sophisticated Storage](https://modrinth.com/mod/sophisticated-storage) chests, barrels and
shulker boxes.

::: warning Supported versions
NeoForge only. The Fabric version ignores Sophisticated mods.
:::

## Drinking Upgrade

![A backpack of water bottles, a waterskin, water bowls and honey bottles, with the Advanced Drinking Upgrade's settings open](/screenshots/integrations/sophisticated/sophisticated-drinking-upgrade.png)

Drinks from the backpack when the thirst bar is low, the way the Feeding Upgrade eats.

- Drinks water bottles, waterskins, water bowls, other drinks that restore thirst, and water from a
  Tank Upgrade in the same backpack.
- The cleanest water goes first. Salt water is never drunk.
- Other drinks come after water. Potions other than water, milk and ominous bottles are left alone.
- Empty bottles and bowls go back into the backpack.
- The filter slots limit which items it drinks.
- In a placed Sophisticated Storage block, it serves players within three blocks.

![A waterskin, two glass bottles, an Upgrade Base and an Ender Pearl make a Drinking Upgrade](/screenshots/integrations/sophisticated/drinking-upgrade-recipe.png)

The Upgrade Base of Sophisticated Storage works too. The recipe unlocks once an Upgrade Base is picked
up.

### Advanced Drinking Upgrade

Made from a Drinking Upgrade, a diamond, two gold ingots and three redstone. It has more filter slots
and two buttons:

| Button | Choices |
|---|---|
| When to drink | As soon as a little thirsty, once half the drink fits, or only when the whole drink fits |
| Lowest grade | Dirty, Murky, Clean or Pure. Left click to go up, right click to go down |

The basic upgrade drinks once half the drink fits, and only Clean water or better.

![A Drinking Upgrade, a diamond, two gold ingots and three redstone make an Advanced Drinking Upgrade](/screenshots/integrations/sophisticated/advanced-drinking-upgrade-recipe.png)

## Water grades in upgrades

- The Tank Upgrade keeps the grade of the water poured in, and fills bottles, buckets, waterskins and
  bowls with that grade.
- The Pump Upgrade grades water from the world like a bottle would. Sea water stays salt water.
- Water of different grades does not mix in one tank.
- The Feeding and Alchemy Upgrades restore thirst for what they feed. The Alchemy Upgrade never drinks
  plain water.
- The Smelting and Smoking Upgrades clean water like a furnace or a smoker.
