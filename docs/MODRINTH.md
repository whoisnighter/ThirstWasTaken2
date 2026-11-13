<div align="center">

<br>

[![modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_64h.png)](https://modrinth.com/mod/thirst-was-taken-2)
[![curseforge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/curseforge_64h.png)](https://www.curseforge.com/minecraft/mc-mods/thirst-was-taken-2)
[![ghpages](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/documentation/ghpages_64h.png)](https://whoisnighter.github.io/ThirstWasTaken2/)

</div>

## Thirst and Quenched
* **Thirst Bar:** Depletes as you run, jump, mine, and fight.
* **Quenched Buffer:** Works like saturation. It drains first, and [heals](https://whoisnighter.github.io/ThirstWasTaken2/docs/features/thirst-and-quenched#healing) you once saturation runs out.
* **Dehydration:** Food stops healing below half thirst. Low thirst prevents sprinting, and empty thirst causes steady damage.
* **Environment:** Thirst drains a little even at rest. Hot biomes like deserts and the Nether deplete it faster. Fire Resistance and Fire Protection reduce heat drain.

<div align="center">
<table>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/screenshots/hud/thirst-food-bars.png" alt="The thirst bar above the hunger bar" width="372"></td>
    <td align="center"><img src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/screenshots/water/water-tooltips.gif" alt="A water bottle tooltip showing each grade" width="272"></td>
  </tr>
</table>
</div>

## Water Quality
| Grade | Common Sources | Effects |
| :--- | :--- | :--- |
| **Dirty** | Swamps, stagnant pools | Often causes [Upset Stomach](https://whoisnighter.github.io/ThirstWasTaken2/docs/features/water-purity#upset-stomach) or [Poisoning](https://whoisnighter.github.io/ThirstWasTaken2/docs/features/water-purity#poison), worse on harder difficulties |
| **Murky** | Standard rivers, lakes, caves | Can cause [Upset Stomach](https://whoisnighter.github.io/ThirstWasTaken2/docs/features/water-purity#upset-stomach) or [Poisoning](https://whoisnighter.github.io/ThirstWasTaken2/docs/features/water-purity#poison) |
| **Clean** | Mountain rivers, rain, boiled water | Safe |
| **Pure** | Cold peaks, dripstone, distilled water | Safe, with the biggest reserve |
| **Salty** | Oceans and beaches | Cannot quench thirst; makes you [Parched](https://whoisnighter.github.io/ThirstWasTaken2/docs/features/water-purity#parched) |

<div align="center">
<table>
  <tr>
    <th align="center"><img src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/icons/upset-stomach.png" alt="Upset Stomach effect icon" width="36" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/features/water-purity#upset-stomach">Upset Stomach</a></th>
    <th align="center"><img src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/icons/parched.png" alt="Parched effect icon" width="36" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/features/water-purity#parched">Parched</a></th>
  </tr>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/screenshots/hud/upset-stomach-hud.png" alt="The thirst bar in green while Upset Stomach" width="372"></td>
    <td align="center"><img src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/screenshots/hud/parched-hud.png" alt="The thirst bar in dry sand colours while Parched" width="372"></td>
  </tr>
</table>
</div>

## Purification
* **Boiling:** Boil water bottles, bowls, buckets, canteens and flasks in a furnace to make them Clean, which is safe to drink.
* **Hanging Pots:** Hang a Copper or Iron Hanging Pot over a lit campfire to boil water Clean on its own.
* **Copper Distiller:** A two-block still and the way to Pure water, from any water, sea water included. It burns furnace fuel and hoppers can keep it running.
* **Rain and Dripstone:** Cauldrons placed under open rain or pointed dripstone automatically fill with clean water.

<div align="center">
<table>
  <tr>
    <th align="center">Iron Hanging Pot</th>
    <th align="center">Furnace</th>
  </tr>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/screenshots/water/iron-hanging-pot.png" alt="Iron Hanging Pot boiling water over a campfire" width="300"></td>
    <td align="center"><img src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/screenshots/water/furnace-clean-water.png" alt="Purifying Water" width="380"></td>
  </tr>
</table>
</div>

<div align="center">
<table>
  <tr>
    <th align="center">Copper Distiller</th>
    <th align="center">Screen and recipe</th>
  </tr>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/screenshots/water/copper-distiller-front.png" alt="A Copper Distiller up close, its fire burning: the boiler on its brick firebox, the swan neck pipe running over to the cooling tub, and the tap over the basin" width="400"></td>
    <td align="center"><img src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/screenshots/water/copper-distiller-gui.png" alt="The Copper Distiller's screen: a bucket of sea water poured in, coal burning, sea water in the boiler tank and Pure water in the basin tank, bottles filling" width="352"><br><img src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/screenshots/recipes/copper-distiller-recipe.png" alt="The Distiller Boiler beside a Copper Pipe, the Brick Firebox under the boiler and the Cooling Tub under the pipe, make a Copper Distiller" width="352"><br><a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/features/water-purity#copper-distiller">How to craft its parts</a></td>
  </tr>
</table>
</div>

## Early Game Gear and Drinking
* **Terracotta Bowls:** Mold clay into bowls and fire them in a furnace to scoop water early on.
* **Waterskin:** Holds 4 servings of water in a single slot. Filled water bowls stack to 3. Intelligently mixes water grades.
* **Drink by Hand:** Sneak and right-click any fresh water block to drink directly without a container.

<div align="center">
<table>
  <tr>
    <th align="center">Clay Bowl</th>
    <th align="center">Waterskin</th>
  </tr>
  <tr>
    <td align="center"><img alt="Three clay balls in a bowl shape make three Clay Bowls" src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/screenshots/recipes/clay-bowl-recipe.png" width="280"><br><img src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/screenshots/recipes/furnace-terracotta-bowl.png" alt="Firing a Clay Bowl into a Terracotta Bowl" width="280"></td>
    <td align="center"><img alt="Four leather in a ring make a Waterskin" src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/screenshots/recipes/waterskin-recipe.png" width="280"></td>
  </tr>
</table>
</div>

## Mid Game Gear
* **Copper Canteen:** Holds 4 servings. Hold it over a lit campfire to boil the water inside Clean.
* **Iron Flask:** Holds 6 servings and boils slower. Both also work in a furnace.

<div align="center">
<table>
  <tr>
    <th align="center">Copper Canteen</th>
    <th align="center">Iron Flask</th>
  </tr>
  <tr>
    <td align="center"><img alt="Three copper ingots and a string make a Copper Canteen" src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/screenshots/recipes/copper-canteen-recipe.png" width="300"></td>
    <td align="center"><img alt="Five iron ingots and an iron nugget make an Iron Flask" src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/screenshots/recipes/iron-flask-recipe.png" width="264"></td>
  </tr>
</table>
</div>

## Late Game Gear
* **Diamond Flask:** Holds 10 servings and boils faster than the Iron Flask.
* **Netherite Flask:** Holds 15 servings and doesn't burn. Upgrade a Diamond Flask at a smithing table; the water inside stays.

<div align="center">
<table>
  <tr>
    <th align="center">Diamond Flask</th>
    <th align="center">Netherite Flask</th>
  </tr>
  <tr>
    <td align="center"><img alt="Six diamonds make a Diamond Flask" src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/screenshots/recipes/diamond-flask-recipe.png" width="288"></td>
    <td align="center"><img alt="A Netherite Upgrade, a Diamond Flask and a netherite ingot make a Netherite Flask at a smithing table" src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/screenshots/recipes/netherite-flask-recipe.png" width="264"></td>
  </tr>
</table>
</div>

## Settings Screen
Change every setting in game, with a live preview of the thirst bar and the AppleSkin outline. Item Values sets how much thirst any food or drink restores, modded ones included. Open it through <img alt="Mod Menu icon" src="https://cdn.modrinth.com/data/mOgUt4GM/5a20ed1450a0e1e79a1fe04e61bb4e5878bf1d20.png" width="20" height="20" align="absmiddle"> <a href="https://modrinth.com/mod/modmenu">Mod Menu</a> on Fabric, or the Config button in the Mods list on NeoForge and Forge.

<p align="center">
  <img alt="The ThirstWasTaken2 settings screen: the Thirst and Water pages, the Sickness tables for each difficulty, the AppleSkin preview changing outline and its tooltip droplets switched off and on, and the Item Values list scrolling to a modded group" src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/screenshots/config/config-showcase.gif" width="100%">
</p>

## Mod Compatibility

<details>
<summary><img alt="Farmer's Delight" src="https://i.imgur.com/wqSocVR.png" width="204" align="absmiddle"> <b>and its addons</b>, click to expand</summary>
<br>

<table>
  <tr>
    <td width="55%">
      <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/"><img alt="Farmer's Delight" src="https://i.imgur.com/wqSocVR.png" width="272"></a><br>
      <i>1.21.1 NeoForge and 1.20.1 Forge<br>
      Fabric: <img alt="Farmer's Delight Refabricated icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2F7vxePowz%2F26e8448993e9bda4dba92b6e7a1a13d9c4333138.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> <a href="https://modrinth.com/mod/farmers-delight-refabricated">Farmer's Delight Refabricated</a></i><br><br>
      Soups, stews, and drinks restore thirst. The Cooking Pot boils water Clean. The Nourishment effect pauses thirst depletion.
    </td>
    <td width="45%">
      <img alt="A Farmer's Delight Cooking Pot boiling water bottles Clean" src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/screenshots/integrations/farmers-delight/farmers-delight-cooking-pot.png" width="100%">
    </td>
  </tr>
  <tr>
    <td width="55%">
      <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/brewin-and-chewin"><img alt="Brewin' and Chewin'" src="https://i.imgur.com/EFkjwBq.png" width="298"></a><br>
      <i>1.21.1 Fabric and NeoForge</i><br><br>
      Brews and soups restore thirst, less the stronger the drink. Water keeps its purity grade in the Keg, and nothing ferments from sea water.
    </td>
    <td width="45%">
      <img alt="A Brewin' and Chewin' Keg beside barrels on a lakeshore, with Jade naming its water Dirty" src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/screenshots/integrations/brewin-and-chewin/brewin-keg.png" width="100%">
    </td>
  </tr>
</table>

<table>
  <tr>
    <td width="45%"><a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/cultural-delights"><img alt="Cultural Delights" src="https://cdn.modrinth.com/data/cached_images/d6a323ce1e69b76f2143b3c0e03bd9ce36fc69e1.png" width="272"></a><br><i>1.21.1 NeoForge</i></td>
    <td width="55%">Its drinks, cucumbers and salads restore thirst, and the Vat brews nothing from sea water.</td>
  </tr>
  <tr>
    <td width="45%"><a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/fruits-delight"><img alt="Fruits Delight" src="https://media.forgecdn.net/attachments/765/821/logo2.png" width="272"></a><br><i>1.21.1 NeoForge, 1.20.1 Forge</i></td>
    <td width="55%">Its juices, teas, jellos, popsicles and juicy fruits restore thirst, and sea water makes no juice.</td>
  </tr>
  <tr>
    <td width="45%"><img alt="Youkais' Feasts icon" src="https://cdn.modrinth.com/data/Ohy0TgO2/4e25b7b5de37ad03845970ec8ef72f5c0b8e4617_96.webp" width="24" height="24" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/youkais-feasts">Gensokyo Delight ~ Youkais' Feasts</a><br><i>1.21.1 NeoForge, 1.20.1 Forge</i></td>
    <td width="55%">Its teas, sakes, wines, soups and popsicles restore thirst, and sea water goes into no Kettle, Fermentation Tank or iron pot.</td>
  </tr>
  <tr>
    <td width="45%"><img alt="Hearth and Harvest icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2F8EEEXOzj%2Fe5d9aa8bd6bf5dcbd674f08b92957d4b001229e3.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/hearth-and-harvest">Hearth and Harvest</a><br><i>1.21.1 NeoForge, 1.20.1 Forge</i></td>
    <td width="55%">Its juices, milks, wines and stews restore thirst. Its sinks, jugs, troughs and kegs keep the grade of their water, and only sea water boils down to salt.</td>
  </tr>
  <tr>
    <td width="45%"><img alt="Miner's Delight icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FqMxbM4BQ%2F0d6f967d3ad184dd296c62a9891e2b2b7d45f61d.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/miners-delight">Miner's Delight</a><br><i>1.21.1 NeoForge, 1.20.1 Forge</i></td>
    <td width="55%">Its soups and milk cup restore thirst, and its copper cups keep the grade of their water, so a cup of sea water stays salty.</td>
  </tr>
  <tr>
    <td width="45%"><a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/oceans-delight"><img alt="Ocean's Delight" src="https://i.imgur.com/OqgniyH.png" width="272"></a><br><i>1.21.1 Fabric and NeoForge</i></td>
    <td width="55%">Its Guardian Soup, braised sea pickle and seagrass salad restore thirst.</td>
  </tr>
  <tr>
    <td width="45%"><a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/expanded-delight"><img alt="Expanded Delight" src="https://wsrv.nl/?url=https%3A%2F%2Fmedia.forgecdn.net%2Fattachments%2F1067%2F753%2Fsome-logo.png&amp;n=-1" width="272"></a><br><i>1.21.1 NeoForge</i></td>
    <td width="55%">Its juices, goat milk, soups and salads restore thirst, and the Cooking Pot cooks nothing from sea water.</td>
  </tr>
  <tr>
    <td width="45%"><a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/rustic-delight"><img alt="Rustic Delight" src="https://cdn.modrinth.com/data/cached_images/6ecede4d7053895d6f424894b35007d35a5b883b.png" width="242"></a><br><i>Fabric and 1.21.1 NeoForge</i></td>
    <td width="55%">Its coffees, soups and bell peppers restore thirst.</td>
  </tr>
  <tr>
    <td width="45%"><img alt="Extra Delight icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FyRrY3XII%2Fcec2396cf9f6f74a5b0cff196301a4b8b8124e1e.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/extra-delight">Extra Delight</a><br><i>1.21.1 NeoForge</i></td>
    <td width="55%">Its juices, milkshakes, coffee, tea, soups and popsicles restore thirst. Its jars, kegs, vat and mixing bowl keep the grade of their water, its tap gives Murky water, and only sea water dries into salt.</td>
  </tr>
</table>

</details>

<details>
<summary><img alt="Let's Do: Farm &amp; Charm icon" src="https://cdn.modrinth.com/data/HJetCzWo/7c6c372629b3efa41409621631d60df12963f005_96.webp" width="20" height="20" align="absmiddle"> <b>Let's Do mods</b>, click to expand</summary>
<br>

<table>
  <tr>
    <td width="45%"><img alt="Let's Do: Farm &amp; Charm icon" src="https://cdn.modrinth.com/data/HJetCzWo/7c6c372629b3efa41409621631d60df12963f005_96.webp" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farm-and-charm/">Let's Do: Farm &amp; Charm</a><br><i>1.21.1 Fabric and NeoForge</i></td>
    <td width="55%">Teas and soups restore thirst. A bucket from the Timber Well gets the grade of the groundwater below it, and water from a Water Trough is Murky, so neither is a free source of clean water.</td>
  </tr>
  <tr>
    <td width="45%"><img alt="Let's Do: Candlelight icon" src="https://cdn.modrinth.com/data/qwbArkQk/5e0770c8da0fab82a70bc9c3913c8d3996c53345_96.webp" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farm-and-charm/candlelight">Let's Do: Candlelight</a><br><i>1.21.1 Fabric and NeoForge, with Farm &amp; Charm</i></td>
    <td width="55%">Soups, salads and chocolate mousse restore thirst. Kitchen sinks still fill on their own, but their water is Murky and needs boiling, and they refuse sea water.</td>
  </tr>
  <tr>
    <td width="45%"><img alt="Let's Do: HerbalBrews icon" src="https://cdn.modrinth.com/data/Eh11TaTm/cea48ad39e9323e9e0f5354ee1d4c160f46b50be_96.webp" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/herbalbrews">Let's Do: HerbalBrews</a><br><i>1.21.1 Fabric and NeoForge</i></td>
    <td width="55%">Teas and coffees restore thirst, also when drunk from a placed Jug. The Tea Kettle brews nothing from sea water.</td>
  </tr>
  <tr>
    <td width="45%"><img alt="Let's Do: Beachparty icon" src="https://cdn.modrinth.com/data/GyKzAh3l/41b9b45c365ecd55aced04bcd22af93878f766a0_96.webp" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/beachparty">Let's Do: Beachparty</a><br><i>1.21.1 Fabric and NeoForge</i></td>
    <td width="55%">Cocktails and open coconuts restore thirst. A placed cocktail gives three sips, each a third of the glass.</td>
  </tr>
  <tr>
    <td width="45%"><img alt="Let's Do: Vinery icon" src="https://cdn.modrinth.com/data/1DWmBJVA/029aec55be4d860ba0aede4939dd93332b6dafad_96.webp" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/vinery">Let's Do: Vinery</a><br><i>1.21.1 Fabric and NeoForge</i></td>
    <td width="55%">Its juices, wines, grapes and cherries restore thirst, wine less than the juice it is made from.</td>
  </tr>
  <tr>
    <td width="45%"><img alt="Let's Do: Meadow icon" src="https://cdn.modrinth.com/data/ra7o6Sl7/d7e6f6bcaa82b9af4a670e6dd6cb980c970aa0f2_96.webp" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/meadow">Let's Do: Meadow</a><br><i>1.21.1 Fabric and NeoForge</i></td>
    <td width="55%">The wooden bucket keeps the water's grade. The wooden cauldron gives Murky water, and sea water won't go into it or the Cooking Pot. Wooden milk buckets restore thirst.</td>
  </tr>
</table>

</details>

<details>
<summary><img alt="Kaleidoscope Cookery icon" src="https://media.forgecdn.net/avatars/thumbnails/1361/462/64/64/638884307253099520.png" width="20" height="20" align="absmiddle"> <b>Kaleidoscope mods</b>, click to expand</summary>
<br>

<table>
  <tr>
    <td width="55%">
      <img alt="Kaleidoscope Cookery icon" src="https://media.forgecdn.net/avatars/thumbnails/1361/462/64/64/638884307253099520.png" width="20" height="20" align="absmiddle"> <b><a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/kaleidoscope-cookery/">Kaleidoscope Cookery</a></b><br>
      <i>1.21.1 NeoForge and 1.20.1 Forge<br>
      Fabric: <img alt="Kaleidoscope Cookery Refabricated icon" src="https://cdn.modrinth.com/data/Ct11Kuii/819ba69579e76715103825ce28b345781b415393.png" width="20" height="20" align="absmiddle"> <a href="https://modrinth.com/mod/kaleidoscope-cookery-refabricated">Kaleidoscope Cookery Refabricated</a></i><br><br>
      Teas, milk tea and soups restore thirst. Water keeps its purity grade in the Stockpot and the Teapot, and the Teapot brews nothing from sea water.
    </td>
    <td width="45%">
      <img alt="A Teapot on a lit Stove among teacups in a cherry grove, with Jade naming its water Clean" src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/screenshots/integrations/kaleidoscope-cookery/kaleidoscope-teapot.png" width="100%">
    </td>
  </tr>
</table>

<table>
  <tr>
    <td width="45%"><img alt="Kaleidoscope Flora icon" src="https://media.forgecdn.net/avatars/thumbnails/2040/206/64/64/639247202160912813.png" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/kaleidoscope-cookery/kaleidoscope-flora">Kaleidoscope Flora</a><br><i>1.21.1 NeoForge, 1.20.1 Forge</i></td>
    <td width="55%">Its flower teas restore thirst, and the Teapot brews none of them from sea water.</td>
  </tr>
  <tr>
    <td width="45%"><img alt="Kaleidoscope Chinese Food icon" src="https://cdn.modrinth.com/data/cuIIkdlx/748a556a3658f0a5f67068f4d8d5cf0041b794a7.png" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/kaleidoscope-cookery/kaleidoscope-chinese-food">Kaleidoscope Chinese Food</a><br><i>1.21.1 NeoForge, 1.20.1 Forge</i></td>
    <td width="55%">Its teas, soups and noodles restore thirst.</td>
  </tr>
  <tr>
    <td width="45%"><img alt="Kaleidoscope Tavern icon" src="https://cdn.modrinth.com/data/r9RZvhiJ/b96860512d16e72dcaa30b15cb0b9b25c18a38e8.png" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/kaleidoscope-tavern">Kaleidoscope Tavern</a><br><i>1.21.1 NeoForge, 1.20.1 Forge; Fabric: <img alt="Kaleidoscope Tavern Refabricated icon" src="https://cdn.modrinth.com/data/UMblNdlF/bfeeabb4b3f926a0afbc953490c32892a0999122.png" width="20" height="20" align="absmiddle"> <a href="https://modrinth.com/mod/kaleidoscope-tavern-refabricated">Refabricated</a></i></td>
    <td width="55%">Its wines, cocktails and juices restore thirst, and water keeps its grade in the Barrel, through the Tap and in a placed Water Bottle. The Barrel and the Shaker won't take sea water.</td>
  </tr>
</table>

</details>

<table>
  <tr>
    <td width="55%">
      <img alt="AppleSkin icon" src="https://cdn.modrinth.com/data/EsAfCjCV/icon.png" width="20" height="20" align="absmiddle"> <b><a href="https://modrinth.com/mod/appleskin">AppleSkin</a></b><br>
      <i>Fabric, NeoForge and Forge</i><br><br>
      Displays your Quenched reserve directly on the HUD, with multiple styles available to choose from. Tooltips show exact thirst values for food and drinks.
    </td>
    <td width="45%">
      <img alt="The thirst bar with AppleSkin, cycling through the Diamond, Ice, Gold, AppleSkin and Legacy quenched outlines" src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/screenshots/hud/hud-appleskin.gif" width="100%">
    </td>
  </tr>
  <tr>
    <td width="55%">
      <img alt="Jade icon" src="https://cdn.modrinth.com/data/nvQzSEkH/b04217bc2b7dc524c4d12f81ff42cc1cefb9b0fc_96.webp" width="20" height="20" align="absmiddle"> <b><a href="https://modrinth.com/mod/jade">Jade 🔍</a></b><br>
      <i>Fabric, NeoForge and Forge</i><br><br>
      Shows the purity grade of water sources, waterlogged blocks, and cauldrons directly under your crosshair.
    </td>
    <td width="45%">
      <img alt="Jade showing the water grade under the crosshair" src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/screenshots/water/jade-water.png" width="100%">
    </td>
  </tr>
  <tr>
    <td width="55%">
      <img alt="Create icon" src="https://cdn.modrinth.com/data/LNytGWDc/61d716699bcf1ec42ed4926a9e1c7311be6087e2_96.webp" width="20" height="20" align="absmiddle"> <b><a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/create">Create</a></b><br>
      <i>1.21.1 NeoForge and 1.20.1 Forge<br>
      26.1.2 and 26.2 Fabric: <img alt="Create Fly icon" src="https://cdn.modrinth.com/data/dKvj0eNn/a1e1ad6f018c3a47cb300edbf0ebebce894bfd45_96.webp" width="20" height="20" align="absmiddle"> <a href="https://modrinth.com/mod/create-fly">Create Fly</a></i><br><br>
      Adds a Sand Filter to purify dirty water by one grade. Water also keeps its purity grade through pipes, pumps, tanks, drains, and spouts.<br><br>
      <i>Also works with <img alt="Create Pipes n Physics icon" src="https://cdn.modrinth.com/data/CuAT8bVS/6c7f64edd8244c250a35a1b2cc241fd2d01f0a7d_96.webp" width="20" height="20" align="absmiddle"> <a href="https://modrinth.com/mod/create-pipes-n-physics">Create Pipes n Physics</a> on 1.21.1 NeoForge.</i>
    </td>
    <td width="45%">
      <img alt="Engineer's Goggles showing Murky water entering the Sand Filter and Clean water leaving it" src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/screenshots/integrations/create/create-sand-filter-goggles.png" width="100%">
    </td>
  </tr>
  <tr>
    <td width="55%">
      <img alt="Sophisticated Backpacks icon" src="https://cdn.modrinth.com/data/TyCTlI4b/e31c7e2f8769d317339e25b2a8d1b40fbf312729_96.webp" width="20" height="20" align="absmiddle"> <b><a href="https://modrinth.com/mod/sophisticated-backpacks">Sophisticated Backpacks</a></b><br>
      <i>NeoForge, every version</i><br><br>
      Adds the Drinking Upgrade, which drinks from your backpack when you get thirsty, the cleanest water first. Water keeps its purity grade in the Tank and Pump Upgrades. Also works with <img alt="Sophisticated Storage icon" src="https://media.forgecdn.net/avatars/thumbnails/543/206/64/64/637872959580005837.png" width="20" height="20" align="absmiddle"> <a href="https://modrinth.com/mod/sophisticated-storage">Sophisticated Storage</a>.
    </td>
    <td width="45%">
      <img alt="A backpack of water with the Advanced Drinking Upgrade's settings open" src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/screenshots/integrations/sophisticated/sophisticated-drinking-upgrade.png" width="100%">
    </td>
  </tr>
  <tr>
    <td width="55%">
      <img alt="Supplementaries icon" src="https://cdn.modrinth.com/data/fFEIiSDQ/e9f5f66fa3b67e54acb91258a1428d68311c58bc_96.webp" width="20" height="20" align="absmiddle"> <b><a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/supplementaries">Supplementaries</a></b><br>
      <i>1.21.1 Fabric and NeoForge</i><br><br>
      Water keeps its purity grade in Jars, Goblets and Faucets, and sea water stays sea water. A Jar or a Goblet of water can be drunk straight from the block. Faucets fill and empty hanging pots and grade the water they draw from a lake.
    </td>
    <td width="45%">
      <img alt="Five jars of water on a lakeshore, brown, grey, blue, cyan and turquoise, with Jade naming the middle one Clean" src="https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/screenshots/integrations/supplementaries/supplementaries-jars.png" width="100%">
    </td>
  </tr>
</table>

Also works with:

<table>
  <tr>
    <td width="45%"><a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/cold-sweat"><img alt="Cold Sweat" src="https://i.imgur.com/N6amWeJ.png" width="180"></a><br><i>1.21.1 NeoForge, 1.20.1 Forge</i></td>
    <td width="55%">Thirst follows the temperature Cold Sweat shows around you, hearths and shade included. Its Waterskin carries a purity grade and quenches thirst, and the Boiler purifies water.</td>
  </tr>
  <tr>
    <td width="45%"><a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/serene-seasons"><img alt="Serene Seasons" src="https://cdn.modrinth.com/data/cached_images/ba72bd7e14454054eda390d3a1e42c0be51a810e.png" width="200"></a><br><i>Fabric, NeoForge and Forge</i></td>
    <td width="55%">Thirst drains faster in summer and slower in winter, and tropical biomes dry out in their dry season.</td>
  </tr>
  <tr>
    <td width="45%"><a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/spelunkery"><img alt="Spelunkery" src="https://cdn-raw.modrinth.com/data/krskFMfA/images/3334d4f848d7b094895130da48ed04b15862fb52.png" width="200"></a><br><i>1.21.1 Fabric and NeoForge</i></td>
    <td width="55%">Its Spring Water is always Pure, and only sea water boils down to salt in a furnace.</td>
  </tr>
</table>

...and more. [View full list](https://whoisnighter.github.io/ThirstWasTaken2/docs/installation#compatible-mods)

## Version Support

| Minecraft version | Mod version | Support status |
|---|---|---|
| 26.3 | Latest | Active |
| 26.2 | Latest | Active |
| 26.1.x | Latest | Active |
| 1.21.11 (Fabric only) | Latest | Active |
| 1.21.11 (NeoForge) | 1.8.0 | No longer supported |
| 1.21.1 | Latest | Active |
| 1.21 (Fabric only) | Latest | Active |
| 1.20.1 (Fabric and Forge) | Latest | Active |

### All Compatibility by Loader

<table>
  <tr>
    <th>Mod</th>
    <th>Fabric</th>
    <th>NeoForge</th>
    <th>Forge (1.20.1)</th>
  </tr>
  <tr>
    <td><img alt="AppleSkin icon" src="https://cdn.modrinth.com/data/EsAfCjCV/icon.png" width="20" height="20" align="absmiddle"> <a href="https://modrinth.com/mod/appleskin">AppleSkin</a><br><img alt="Jade icon" src="https://cdn.modrinth.com/data/nvQzSEkH/b04217bc2b7dc524c4d12f81ff42cc1cefb9b0fc_96.webp" width="20" height="20" align="absmiddle"> <a href="https://modrinth.com/mod/jade">Jade 🔍</a><br><img alt="Serene Seasons icon" src="https://cdn.modrinth.com/data/e0bNACJD/f8b292ea53e0a0ea908570defddc48673d16d7d6.png" width="20" height="20" align="absmiddle"> <a href="https://modrinth.com/mod/serene-seasons">Serene Seasons</a><br><img alt="Terralith icon" src="https://cdn.modrinth.com/data/8oi3bsk5/1959d924a1088944bbf07a06ba523726112d7e7a_96.webp" width="20" height="20" align="absmiddle"> <a href="https://modrinth.com/mod/terralith">Terralith</a><br><img alt="Cooking for Blockheads icon" src="https://cdn.modrinth.com/data/vJnhuDde/811d0c55230d84d9e182df1b0bd631ddebecc0ad_96.webp" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/cooking-for-blockheads">Cooking for Blockheads</a></td>
    <td>every version</td>
    <td>every version</td>
    <td>yes</td>
  </tr>
  <tr>
    <td><img alt="Farmer's Delight icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FR2OftAxM%2F8e7aa38ab94d94bb0a2894a218b69beb49002b34.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/">Farmer's Delight</a></td>
    <td>every version (Refabricated)</td>
    <td>1.21.1</td>
    <td>yes</td>
  </tr>
  <tr>
    <td><img alt="Create icon" src="https://cdn.modrinth.com/data/LNytGWDc/61d716699bcf1ec42ed4926a9e1c7311be6087e2_96.webp" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/create">Create</a></td>
    <td>26.1.x, 26.2 (Create Fly)</td>
    <td>1.21.1</td>
    <td>yes</td>
  </tr>
  <tr>
    <td><img alt="Create Pipes n Physics icon" src="https://cdn.modrinth.com/data/CuAT8bVS/6c7f64edd8244c250a35a1b2cc241fd2d01f0a7d_96.webp" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/create#create-pipes-n-physics">Create Pipes n Physics</a></td>
    <td>–</td>
    <td>1.21.1</td>
    <td>–</td>
  </tr>
  <tr>
    <td><img alt="Sophisticated Backpacks icon" src="https://cdn.modrinth.com/data/TyCTlI4b/e31c7e2f8769d317339e25b2a8d1b40fbf312729_96.webp" width="20" height="20" align="absmiddle"> <a href="https://modrinth.com/mod/sophisticated-backpacks">Sophisticated Backpacks</a><br><img alt="Sophisticated Storage icon" src="https://media.forgecdn.net/avatars/thumbnails/543/206/64/64/637872959580005837.png" width="20" height="20" align="absmiddle"> <a href="https://modrinth.com/mod/sophisticated-storage">Sophisticated Storage</a></td>
    <td>–</td>
    <td>every version</td>
    <td>–</td>
  </tr>
  <tr>
    <td><img alt="Kaleidoscope Cookery icon" src="https://media.forgecdn.net/avatars/thumbnails/1361/462/64/64/638884307253099520.png" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/kaleidoscope-cookery/">Kaleidoscope Cookery</a></td>
    <td>every version (Refabricated)</td>
    <td>1.21.1</td>
    <td>yes</td>
  </tr>
  <tr>
    <td><img alt="Kaleidoscope Tavern icon" src="https://cdn.modrinth.com/data/r9RZvhiJ/b96860512d16e72dcaa30b15cb0b9b25c18a38e8.png" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/kaleidoscope-tavern">Kaleidoscope Tavern</a></td>
    <td>every version (Refabricated)</td>
    <td>1.21.1</td>
    <td>yes</td>
  </tr>
  <tr>
    <td><img alt="Croptopia icon" src="https://media.forgecdn.net/avatars/thumbnails/308/636/64/64/637392485303151332.png" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/croptopia">Croptopia</a></td>
    <td>1.20.1, 1.21.1, 26.1.x, 26.2</td>
    <td>1.21.1, 26.1.x, 26.2</td>
    <td>yes</td>
  </tr>
  <tr>
    <td><img alt="Rustic Delight icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2Ffoa4fGIH%2Feecc99e281522f2291081c48176f0faa84c107bc.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/rustic-delight">Rustic Delight</a></td>
    <td>1.21.1 and newer</td>
    <td>1.21.1</td>
    <td>–</td>
  </tr>
  <tr>
    <td><img alt="Supplementaries icon" src="https://cdn.modrinth.com/data/fFEIiSDQ/e9f5f66fa3b67e54acb91258a1428d68311c58bc_96.webp" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/supplementaries">Supplementaries</a><br><img alt="Brewin' and Chewin' icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FhIu9KJTT%2Ff7c591a80046859d3d45c04ecbbc54d264483d5e.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/brewin-and-chewin">Brewin' and Chewin'</a><br><img alt="Ocean's Delight icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FDGiq4ZSW%2F949ba66d6fffb5a984fbb70e3ef4a51f15be3191.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/oceans-delight">Ocean's Delight</a><br><img alt="Let's Do: Farm &amp; Charm icon" src="https://cdn.modrinth.com/data/HJetCzWo/7c6c372629b3efa41409621631d60df12963f005_96.webp" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farm-and-charm/">Let's Do: Farm &amp; Charm</a><br><img alt="Let's Do: Candlelight icon" src="https://cdn.modrinth.com/data/qwbArkQk/5e0770c8da0fab82a70bc9c3913c8d3996c53345_96.webp" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farm-and-charm/candlelight">Let's Do: Candlelight</a><br><img alt="Let's Do: HerbalBrews icon" src="https://cdn.modrinth.com/data/Eh11TaTm/cea48ad39e9323e9e0f5354ee1d4c160f46b50be_96.webp" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/herbalbrews">Let's Do: HerbalBrews</a><br><img alt="Let's Do: Beachparty icon" src="https://cdn.modrinth.com/data/GyKzAh3l/41b9b45c365ecd55aced04bcd22af93878f766a0_96.webp" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/beachparty">Let's Do: Beachparty</a><br><img alt="Let's Do: Vinery icon" src="https://cdn.modrinth.com/data/1DWmBJVA/029aec55be4d860ba0aede4939dd93332b6dafad_96.webp" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/vinery">Let's Do: Vinery</a><br><img alt="Let's Do: Meadow icon" src="https://cdn.modrinth.com/data/ra7o6Sl7/d7e6f6bcaa82b9af4a670e6dd6cb980c970aa0f2_96.webp" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/meadow">Let's Do: Meadow</a><br><img alt="Spelunkery icon" src="https://cdn.modrinth.com/data/krskFMfA/465cfcd453c22ee5a09884ede98a0442e97658c5.png" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/spelunkery">Spelunkery</a><br><img alt="Bountiful Fares icon" src="https://cdn.modrinth.com/data/YgrSDjlb/f4e10611dcc1791138225224fa444f5a8ef4348f_96.webp" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/bountiful-fares">Bountiful Fares</a></td>
    <td>1.21.1</td>
    <td>1.21.1</td>
    <td>–</td>
  </tr>
  <tr>
    <td><img alt="Fruits Delight icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2Fg6sbyCTu%2F4ecc5d554f260b876d21c427aa6c2bdf4457fd5c.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/fruits-delight">Fruits Delight</a><br><img alt="Youkais' Feasts icon" src="https://cdn.modrinth.com/data/Ohy0TgO2/4e25b7b5de37ad03845970ec8ef72f5c0b8e4617_96.webp" width="24" height="24" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/youkais-feasts">Youkais' Feasts</a><br><img alt="Hearth and Harvest icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2F8EEEXOzj%2Fe5d9aa8bd6bf5dcbd674f08b92957d4b001229e3.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/hearth-and-harvest">Hearth and Harvest</a><br><img alt="Miner's Delight icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FqMxbM4BQ%2F0d6f967d3ad184dd296c62a9891e2b2b7d45f61d.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/miners-delight">Miner's Delight</a><br><img alt="Cold Sweat icon" src="https://cdn.modrinth.com/data/uXhSmPjd/bf55420556c30d44d2f5cf7b8915705b9214b4ef.png" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/cold-sweat">Cold Sweat</a><br><img alt="Kaleidoscope Flora icon" src="https://media.forgecdn.net/avatars/thumbnails/2040/206/64/64/639247202160912813.png" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/kaleidoscope-cookery/kaleidoscope-flora">Kaleidoscope Flora</a><br><img alt="Kaleidoscope Chinese Food icon" src="https://cdn.modrinth.com/data/cuIIkdlx/748a556a3658f0a5f67068f4d8d5cf0041b794a7.png" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/kaleidoscope-cookery/kaleidoscope-chinese-food">Kaleidoscope Chinese Food</a><br><img alt="Mekanism icon" src="https://cdn.modrinth.com/data/Ce6I4WUE/ea185eb1300a64867f89101d4798e71a54ef6bed_96.webp" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/mekanism">Mekanism</a><br><img alt="Immersive Engineering icon" src="https://cdn.modrinth.com/data/tIm2nV03/icon.png" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/immersive-engineering">Immersive Engineering</a><br><img alt="Little Logistics icon" src="https://cdn.modrinth.com/data/1zZJrh9c/fdee529567ce17536fcc53177731e28304ece5af_96.webp" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/little-logistics">Little Logistics</a></td>
    <td>–</td>
    <td>1.21.1</td>
    <td>yes</td>
  </tr>
  <tr>
    <td><img alt="Cultural Delights icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FYttyNOFA%2Fd857243f0e7dedd3d7f552c4371326773629e42e.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/cultural-delights">Cultural Delights</a><br><img alt="Expanded Delight icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2Fe9V6wFcR%2F4cbbace573b20628290929948a77c74d95ed7a70.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/expanded-delight">Expanded Delight</a><br><img alt="Extra Delight icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FyRrY3XII%2Fcec2396cf9f6f74a5b0cff196301a4b8b8124e1e.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/extra-delight">Extra Delight</a><br><img alt="No Man's Land icon" src="https://cdn.modrinth.com/data/kjZCvAn6/958489a1729e9e17a6a5a0728ef249236c07f7b3_96.webp" width="20" height="20" align="absmiddle"> <a href="https://whoisnighter.github.io/ThirstWasTaken2/docs/integrations/no-mans-land">No Man's Land</a></td>
    <td>–</td>
    <td>1.21.1</td>
    <td>–</td>
  </tr>
</table>

## For Developers

<details>
<summary><b>Data packs and Java API</b>, click to expand</summary>

**Data Packs:** Give any item a thirst value with one JSON file in `data/<namespace>/thirstwastaken2/drinks/`, no dependency needed:

```json
{
  "values": {
    "mymod:lemonade": { "thirst": 6, "quenched": 8 },
    "mymod:iced_tea": { "thirst": 8, "quenched": 12 }
  }
}
```

**Java API:** Read and change thirst, react to drinking and check water purity, on every loader and version:

```java
// Salty snacks restore no thirst.
ThirstEvents.DRINK.register((player, stack, drink) -> {
    if (stack.is(MyItems.SALTY_SNACK)) drink.cancel();
});

// Give a player a bottle of Pure water.
player.addItem(ThirstApi.waterBottle(ThirstApi.maxPurity()));
```

See the [data pack](https://whoisnighter.github.io/ThirstWasTaken2/docs/developers/data-packs) and [Java API](https://whoisnighter.github.io/ThirstWasTaken2/docs/developers/java-api) guides.

</details>

## Quick FAQ

**Does it work in Peaceful mode?**  
Yes. Thirst refills on its own, unless [`thirstDepletionInPeaceful`](https://whoisnighter.github.io/ThirstWasTaken2/docs/configuration#thirstdepletioninpeaceful) is on. Pairs well with <img alt="Peaceful Hunger icon" src="https://cdn.modrinth.com/data/NGEcCZ3C/4f2503680e564e8d2ad5e252993a44af2c61acc3_96.webp" width="20" height="20" align="absmiddle"> [Peaceful Hunger](https://modrinth.com/mod/peaceful-hunger).

**Can I use this in a modpack?**  
Yes. You are free to include Thirst Was Taken 2 in any public or private modpack.

**Supported languages:**  
English, Vietnamese, Simplified Chinese, Traditional Chinese, French, Japanese, Korean, Polish, and Russian.

![Item tooltips in Simplified Chinese](https://raw.githubusercontent.com/whoisnighter/ThirstWasTaken2/main/docs/public/screenshots/water/chinese-tooltips.png)

Want to improve or add a translation? [Open a pull request](https://github.com/whoisnighter/ThirstWasTaken2).

## Credits

* Based on [Thirst Was Taken](https://modrinth.com/mod/thirst-was-taken) by [ghen](https://github.com/ghen-git), under the MIT License.
* Hanging Pot models adapted from [Dehydration](https://github.com/Globox1997/Dehydration) by [Globox1997](https://github.com/Globox1997), under the GPL-3.0.
* Parched effect icon inspired by [Yet Another Thirst](https://modrinth.com/mod/yet-another-thirst) by [minhnh303](https://github.com/minhnh303), redrawn from scratch.
* Licensed under the [GPL-3.0](https://github.com/whoisnighter/ThirstWasTaken2/blob/main/LICENSE) from 1.0.7. See [CREDITS.md](https://github.com/whoisnighter/ThirstWasTaken2/blob/main/CREDITS.md).
