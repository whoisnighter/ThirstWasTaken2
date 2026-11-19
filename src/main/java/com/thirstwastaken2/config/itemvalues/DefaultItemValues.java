package com.thirstwastaken2.config.itemvalues;

import com.thirstwastaken2.config.ThirstConfig;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What every item the config names restores by default, for {@link ThirstConfig#drinks} and
 * {@link ThirstConfig#foods}: vanilla's and Farmer's Delight's here, every other mod's in a class of its
 * family ({@link FarmersDelightAddonItemValues}, {@link KaleidoscopeItemValues}, {@link LetsDoItemValues},
 * {@link OtherModItemValues}). A new mod's values go in its family's class and are called from both
 * {@link #drinks()} or {@link #foods()} and {@link #bringUpToDate}, at the end, so a fresh file and an
 * older one brought up to date list them alike.
 */
public final class DefaultItemValues {
    /** The mod's own water containers, which restore {@link ThirstConfig#plainWaterValue} rather than an entry of their own. */
    private static final String[] OWN_WATER_VESSELS = {"thirstwastaken2:terracotta_water_bowl",
            "thirstwastaken2:waterskin", "thirstwastaken2:copper_canteen", "thirstwastaken2:iron_flask",
            "thirstwastaken2:diamond_flask", "thirstwastaken2:netherite_flask"};

    private DefaultItemValues() { }

    /**
     * What a fresh config lists. Since the purification rework every value sits in one of a few bands,
     * so that nothing but prepared water and prepared drinks builds a reserve:
     *
     * <ul>
     *   <li>Prepared soups and non-alcoholic drinks, a juice, a tea, a coffee, a cocoa: 6 thirst and 4
     *       quenched, a serving of Clean water's worth, since they are made, not scooped. A cup is half.</li>
     *   <li>Plain milk: 4 and none. Honey and syrups: 2 and none.</li>
     *   <li>Alcohol keeps the thirst it had, the stronger the less, and half that as quenched; spirits
     *       nothing.</li>
     *   <li>Solid food is incidental water and builds no reserve: wet fruit and salads at most 2, a
     *       vegetable, berry, slice or dessert 1, dry food nothing.</li>
     * </ul>
     *
     * <p>Potions keep their own value. Plain water is not here at all: it is {@link ThirstConfig#plainWaterValue}.
     * Each family class says which of its mods' items each one is matched to.
     */
    public static Map<String, int[]> drinks() {
        Map<String, int[]> values = new LinkedHashMap<>();
        put(values, 6, 8, "minecraft:potion");
        // Milk never needs purifying, but it is not water: it fills without building a reserve.
        put(values, 4, 0, "minecraft:milk_bucket");
        put(values, 2, 0, "minecraft:honey_bottle");

        put(values, 6, 4, "farmersdelight:apple_cider", "farmersdelight:melon_juice", "farmersdelight:hot_cocoa");
        put(values, 4, 0, "farmersdelight:milk_bottle");
        KaleidoscopeItemValues.kaleidoscopeCookeryDrinks(values);
        FarmersDelightAddonItemValues.brewinAndChewinDrinks(values);
        OtherModItemValues.coldSweatDrinks(values);
        FarmersDelightAddonItemValues.culturalDelightsDrinks(values);
        FarmersDelightAddonItemValues.fruitsDelightDrinks(values);
        FarmersDelightAddonItemValues.expandedDelightDrinks(values);
        FarmersDelightAddonItemValues.rusticDelightDrinks(values);
        LetsDoItemValues.farmAndCharmDrinks(values);
        LetsDoItemValues.herbalBrewsDrinks(values);
        LetsDoItemValues.beachpartyDrinks(values);
        OtherModItemValues.hearthAndHarvestDrinks(values);
        OtherModItemValues.noMansLandDrinks(values);
        LetsDoItemValues.vineryDrinks(values);
        OtherModItemValues.croptopiaDrinks(values);
        FarmersDelightAddonItemValues.minersDelightDrinks(values);
        KaleidoscopeItemValues.kaleidoscopeFloraDrinks(values);
        FarmersDelightAddonItemValues.extraDelightDrinks(values);
        KaleidoscopeItemValues.kaleidoscopeChineseFoodDrinks(values);
        KaleidoscopeItemValues.kaleidoscopeTavernDrinks(values);
        OtherModItemValues.bountifulFaresDrinks(values);
        LetsDoItemValues.meadowDrinks(values);
        FarmersDelightAddonItemValues.youkaisFeastsDrinks(values);
        OtherModItemValues.hexaliaDrinks(values);
        return values;
    }

    public static Map<String, int[]> foods() {
        Map<String, int[]> values = new LinkedHashMap<>();
        put(values, 2, 0, "minecraft:apple", "minecraft:golden_apple", "minecraft:enchanted_golden_apple");
        // Upstream gives the two stews an apple's 2, 3. Every other stew here, Farmer's Delight's and the
        // keyword value for soups included, is 4, 5, so these match them.
        put(values, 6, 4, "minecraft:mushroom_stew", "minecraft:rabbit_stew");
        put(values, 2, 0, "minecraft:melon_slice");
        put(values, 1, 0, "minecraft:carrot", "minecraft:beetroot", "minecraft:sweet_berries", "minecraft:glow_berries", "minecraft:golden_carrot");
        put(values, 6, 4, "minecraft:beetroot_soup");
        put(values, 1, 0, "farmersdelight:pumpkin_slice");
        put(values, 2, 0, "farmersdelight:tomato");
        put(values, 1, 0, "farmersdelight:glow_berry_custard");
        put(values, 6, 4, "farmersdelight:bone_broth");
        put(values, 1, 0, "farmersdelight:cabbage_leaf");
        put(values, 2, 0, "farmersdelight:melon_popsicle");
        put(values, 2, 0, "farmersdelight:fruit_salad");
        put(values, 1, 0, "farmersdelight:tomato_sauce");
        put(values, 2, 0, "farmersdelight:mixed_salad");
        put(values, 6, 4, "farmersdelight:beef_stew", "farmersdelight:chicken_soup", "farmersdelight:vegetable_soup", "farmersdelight:fish_stew", "farmersdelight:pumpkin_soup", "farmersdelight:baked_cod_stew", "farmersdelight:noodle_soup", "farmersdelight:onion_soup");
        KaleidoscopeItemValues.kaleidoscopeCookeryFoods(values);
        FarmersDelightAddonItemValues.brewinAndChewinFoods(values);
        FarmersDelightAddonItemValues.culturalDelightsFoods(values);
        FarmersDelightAddonItemValues.fruitsDelightFoods(values);
        FarmersDelightAddonItemValues.oceansDelightFoods(values);
        FarmersDelightAddonItemValues.expandedDelightFoods(values);
        FarmersDelightAddonItemValues.rusticDelightFoods(values);
        LetsDoItemValues.farmAndCharmFoods(values);
        LetsDoItemValues.candlelightFoods(values);
        LetsDoItemValues.beachpartyFoods(values);
        OtherModItemValues.hearthAndHarvestFoods(values);
        OtherModItemValues.noMansLandFoods(values);
        LetsDoItemValues.vineryFoods(values);
        OtherModItemValues.croptopiaFoods(values);
        FarmersDelightAddonItemValues.minersDelightFoods(values);
        FarmersDelightAddonItemValues.extraDelightFoods(values);
        KaleidoscopeItemValues.kaleidoscopeChineseFoodFoods(values);
        KaleidoscopeItemValues.kaleidoscopeTavernFoods(values);
        OtherModItemValues.bountifulFaresFoods(values);
        FarmersDelightAddonItemValues.youkaisFeastsFoods(values);
        return values;
    }

    /**
     * Brings lists read from an older file up to date: what later releases added is filled in where the
     * file lacks it, and what no longer belongs there goes. A value the player set is never changed.
     */
    public static void bringUpToDate(Map<String, int[]> drinks, Map<String, int[]> foods) {
        // Every serving of plain water restores plainWaterValue, whatever holds it, so the mod's own
        // vessels and bowl no longer have a value of their own. Files from before the purification
        // rework list them; the entries would show in the item list and do nothing, so they go.
        for (String vessel : OWN_WATER_VESSELS) drinks.remove(vessel);
        // Milk and honey came after the first lists. A player who does not want them can set both
        // values to zero or list the item in itemBlacklist; only a missing key is filled in.
        drinks.putIfAbsent("minecraft:milk_bucket", new int[]{4, 0});
        drinks.putIfAbsent("minecraft:honey_bottle", new int[]{2, 0});
        // And for the Farmer's Delight drinks and meals the first lists missed.
        drinks.putIfAbsent("farmersdelight:milk_bottle", new int[]{4, 0});
        drinks.putIfAbsent("farmersdelight:hot_cocoa", new int[]{6, 4});
        foods.putIfAbsent("farmersdelight:bone_broth", new int[]{6, 4});
        foods.putIfAbsent("farmersdelight:onion_soup", new int[]{6, 4});
        foods.putIfAbsent("farmersdelight:glow_berry_custard", new int[]{1, 0});
        foods.putIfAbsent("farmersdelight:tomato", new int[]{2, 0});
        // And for Kaleidoscope Cookery, added after both.
        KaleidoscopeItemValues.kaleidoscopeCookeryDrinks(drinks);
        KaleidoscopeItemValues.kaleidoscopeCookeryFoods(foods);
        // And for Brewin' and Chewin', added after that.
        FarmersDelightAddonItemValues.brewinAndChewinDrinks(drinks);
        FarmersDelightAddonItemValues.brewinAndChewinFoods(foods);
        // And for Cold Sweat's waterskin, added after that.
        OtherModItemValues.coldSweatDrinks(drinks);
        // And for Cultural Delights, added after that.
        FarmersDelightAddonItemValues.culturalDelightsDrinks(drinks);
        FarmersDelightAddonItemValues.culturalDelightsFoods(foods);
        // And for Fruits Delight, added after that.
        FarmersDelightAddonItemValues.fruitsDelightDrinks(drinks);
        FarmersDelightAddonItemValues.fruitsDelightFoods(foods);
        // And for Ocean's Delight, added after that.
        FarmersDelightAddonItemValues.oceansDelightFoods(foods);
        // And for Expanded Delight and Rustic Delight, added after that.
        FarmersDelightAddonItemValues.expandedDelightDrinks(drinks);
        FarmersDelightAddonItemValues.expandedDelightFoods(foods);
        FarmersDelightAddonItemValues.rusticDelightDrinks(drinks);
        FarmersDelightAddonItemValues.rusticDelightFoods(foods);
        // And for Farm & Charm and Candlelight, added after that.
        LetsDoItemValues.farmAndCharmDrinks(drinks);
        LetsDoItemValues.farmAndCharmFoods(foods);
        LetsDoItemValues.candlelightFoods(foods);
        // And for HerbalBrews and Beachparty, added after that.
        LetsDoItemValues.herbalBrewsDrinks(drinks);
        LetsDoItemValues.beachpartyDrinks(drinks);
        LetsDoItemValues.beachpartyFoods(foods);
        // And for Hearth and Harvest, added after that.
        OtherModItemValues.hearthAndHarvestDrinks(drinks);
        OtherModItemValues.hearthAndHarvestFoods(foods);
        // And for No Man's Land, added after that.
        OtherModItemValues.noMansLandDrinks(drinks);
        OtherModItemValues.noMansLandFoods(foods);
        // And for Vinery, added after that.
        LetsDoItemValues.vineryDrinks(drinks);
        LetsDoItemValues.vineryFoods(foods);
        // And for Croptopia, added after that.
        OtherModItemValues.croptopiaDrinks(drinks);
        OtherModItemValues.croptopiaFoods(foods);
        // And for Miner's Delight, added after that.
        FarmersDelightAddonItemValues.minersDelightDrinks(drinks);
        FarmersDelightAddonItemValues.minersDelightFoods(foods);
        // And for Kaleidoscope Flora, added after that.
        KaleidoscopeItemValues.kaleidoscopeFloraDrinks(drinks);
        // And for Extra Delight, added after that.
        FarmersDelightAddonItemValues.extraDelightDrinks(drinks);
        FarmersDelightAddonItemValues.extraDelightFoods(foods);
        // And for Kaleidoscope Chinese Food, added after that.
        KaleidoscopeItemValues.kaleidoscopeChineseFoodDrinks(drinks);
        KaleidoscopeItemValues.kaleidoscopeChineseFoodFoods(foods);
        // And for Kaleidoscope Tavern, added after that.
        KaleidoscopeItemValues.kaleidoscopeTavernDrinks(drinks);
        KaleidoscopeItemValues.kaleidoscopeTavernFoods(foods);
        // And for Bountiful Fares, added after that.
        OtherModItemValues.bountifulFaresDrinks(drinks);
        OtherModItemValues.bountifulFaresFoods(foods);
        // And for Meadow, added after that.
        LetsDoItemValues.meadowDrinks(drinks);
        // And for Youkais' Feasts, added after that.
        FarmersDelightAddonItemValues.youkaisFeastsDrinks(drinks);
        FarmersDelightAddonItemValues.youkaisFeastsFoods(foods);
        // And for Hexalia, added after that.
        OtherModItemValues.hexaliaDrinks(drinks);
    }

    static void put(Map<String, int[]> values, int thirst, int quenched, String... ids) {
        for (String id : ids) values.put(id, new int[]{thirst, quenched});
    }

    static void putMissing(Map<String, int[]> values, int thirst, int quenched, String... ids) {
        for (String id : ids) values.putIfAbsent(id, new int[]{thirst, quenched});
    }
}
