package com.thirstwastaken2.config.itemvalues;

import java.util.Map;

import static com.thirstwastaken2.config.itemvalues.DefaultItemValues.putMissing;

/**
 * What the drinks and foods of the mods of no family here: Cold Sweat, Hearth and Harvest, No Man's Land, Croptopia,
 * Bountiful Fares and Hexalia restore by default. Every entry goes in with
 * {@code putIfAbsent}, so one call fills a fresh config and brings an older file up to date without
 * overwriting a player's edit. {@link DefaultItemValues} calls them, in the order that keeps a new
 * file's entries where they always were.
 */
final class OtherModItemValues {
    private OtherModItemValues() { }

    /**
     * Cold Sweat's filled waterskin, by id alone like the other mods' drinks. It holds 250 mB, a bottle,
     * and by default one sip empties it, so a sip is a bottle of water's worth. A server that gives it
     * more sips in Cold Sweat's config gets a bottle's worth from each.
     */
    static void coldSweatDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 4, "cold_sweat:filled_waterskin");
    }

    /**
     * Hearth and Harvest's drinks, by id alone; its 1.20.1 build has a subset of the same ids. Its own
     * Thirst compat names the upstream mod and never runs with this one, and its values are two to four
     * times ours for alcohol, so these follow Farmer's Delight's juices and milk and Brewin' and Chewin's
     * beer, wine and rum instead. Aged drinks are safe whatever fresh water went into the cask; sea water
     * ages nothing (see {@code src/main/hearthandharvest}). The goat milk bucket and the beer are 1.4.0's;
     * its brine is a pickling ingredient, not a drink.
     */
    static void hearthAndHarvestDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 4, "hearthandharvest:blueberry_juice", "hearthandharvest:cherry_juice", "hearthandharvest:raspberry_juice", "hearthandharvest:red_grape_juice", "hearthandharvest:green_grape_juice", "hearthandharvest:sweet_berry_juice", "hearthandharvest:glow_berry_juice");
        putMissing(drinks, 4, 0, "hearthandharvest:goat_milk_bottle", "hearthandharvest:goat_milk_bucket");
        putMissing(drinks, 6, 4, "hearthandharvest:chocolate_milk_bottle");
        putMissing(drinks, 5, 2, "hearthandharvest:mead", "hearthandharvest:hard_cider", "hearthandharvest:beer");
        putMissing(drinks, 6, 4, "hearthandharvest:root_beer");
        putMissing(drinks, 3, 1, "hearthandharvest:blueberry_wine", "hearthandharvest:cherry_wine", "hearthandharvest:raspberry_wine", "hearthandharvest:red_grape_wine", "hearthandharvest:green_grape_wine", "hearthandharvest:sweet_berry_wine", "hearthandharvest:glow_berry_wine", "hearthandharvest:melon_wine");
        putMissing(drinks, 2, 1, "hearthandharvest:moonshine");
        putMissing(drinks, 2, 0, "hearthandharvest:syrup_bottle");
    }

    /**
     * Hearth and Harvest's stews and fruit: a stew as Farmer's Delight's, berries and grapes as sweet
     * berries, a baked or caramel apple as an apple. {@code onion_soup} is its 1.20.1 build's own. Jams,
     * pickles, cheese and the dry foods are left out.
     */
    static void hearthAndHarvestFoods(Map<String, int[]> foods) {
        putMissing(foods, 6, 4, "hearthandharvest:corn_stew", "hearthandharvest:onion_soup");
        putMissing(foods, 1, 0, "hearthandharvest:blueberries", "hearthandharvest:raspberry", "hearthandharvest:cherry", "hearthandharvest:red_grapes", "hearthandharvest:green_grapes");
        putMissing(foods, 1, 0, "hearthandharvest:baked_apple", "hearthandharvest:caramel_apple");
    }

    /**
     * No Man's Land's drinks, by id alone. It tags all three {@code c:drinks}, which would give each
     * {@link com.thirstwastaken2.config.ThirstConfig#drinkTagValue}; these entries win over the tag. The pear juice is Farmer's Delight's juices,
     * the maple syrup the honey bottle, and the pesto, a sauce of oil and basil drunk from its bottle, less
     * than Farmer's Delight's tomato sauce. The juice and the pesto exist only with Farmer's Delight.
     * Sipping from its milk cauldron is a quarter of a milk bucket (see {@code src/main/nomansland}).
     */
    static void noMansLandDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 4, "nomansland:pear_juice");
        putMissing(drinks, 2, 0, "nomansland:maple_syrup_bottle");
        putMissing(drinks, 1, 0, "nomansland:pesto_bottle");
    }

    /**
     * No Man's Land's stew, Farmer's Delight's stews, and its pears, the apple's. The witch stew exists only
     * with Farmer's Delight. Meats, fish, nuts, breads, pastas, tarts and cakes are solid food.
     */
    static void noMansLandFoods(Map<String, int[]> foods) {
        putMissing(foods, 6, 4, "nomansland:witch_stew");
        putMissing(foods, 2, 0, "nomansland:pear", "nomansland:syruped_pear", "nomansland:honeyed_apple");
    }

    /**
     * Croptopia's drinks, by id alone: its builds share one mod id on every version and loader, so these
     * reach every node, one with no integration included, and match nothing where the mod is absent. None
     * is tagged {@code c:drinks}. Juices and lemonade are Farmer's Delight's juice, smoothies Beachparty's
     * cocktails, the milkshake Fruits Delight's, coffee, the latte and tea HerbalBrews', soy milk and
     * horchata a bottle of milk, beer, mead and wine Brewin' and Chewin's, rum its saccharine rum. Tea is
     * crafted cold from a water bottle, but it is its own item and safe, as every other mod's is; sea water
     * crafts nothing (see {@code src/main/croptopia}). Croptopia's water bottle and milk bottle are
     * ingredients that cannot be drunk, so they are left out.
     */
    static void croptopiaDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 4, "croptopia:apple_juice", "croptopia:cranberry_juice", "croptopia:grape_juice", "croptopia:melon_juice", "croptopia:orange_juice", "croptopia:pineapple_juice", "croptopia:saguaro_juice", "croptopia:tomato_juice", "croptopia:lemonade", "croptopia:limeade");
        putMissing(drinks, 6, 4, "croptopia:banana_smoothie", "croptopia:strawberry_smoothie", "croptopia:fruit_smoothie", "croptopia:kale_smoothie");
        putMissing(drinks, 6, 4, "croptopia:chocolate_milkshake");
        putMissing(drinks, 6, 4, "croptopia:tea");
        putMissing(drinks, 6, 4, "croptopia:coffee");
        putMissing(drinks, 6, 4, "croptopia:pumpkin_spice_latte");
        putMissing(drinks, 4, 0, "croptopia:soy_milk");
        putMissing(drinks, 6, 4, "croptopia:horchata");
        putMissing(drinks, 5, 2, "croptopia:beer", "croptopia:mead");
        putMissing(drinks, 3, 1, "croptopia:wine");
        putMissing(drinks, 2, 1, "croptopia:rum");
    }

    /**
     * Croptopia's soups, salads and wetter fruit; see {@link #croptopiaDrinks}. Soups and stews are
     * Farmer's Delight's, the fruit salad its fruit salad, the others its mixed salad, a sorbet its melon
     * popsicle, ice cream and yoghurt its custard. Cantaloupe and honeydew are a melon slice, the cucumber
     * Cultural Delights', the tomato Farmer's Delight's, the tree fruit an apple, berries, cherries,
     * kumquats, lemons and limes a sweet berry, lettuce and celery a cabbage leaf.
     * {@code bibim_naengmyeon}, a cold noodle soup, is in the 26.x builds
     * only. Pickles are salty, and the roasts, breads, pies and jams are solid food; they are left out.
     */
    static void croptopiaFoods(Map<String, int[]> foods) {
        putMissing(foods, 6, 4, "croptopia:leek_soup", "croptopia:pumpkin_soup", "croptopia:potato_soup", "croptopia:beef_stew", "croptopia:nether_wart_stew", "croptopia:borscht", "croptopia:goulash", "croptopia:chicken_and_dumplings", "croptopia:tofu_and_dumplings", "croptopia:chicken_and_noodles", "croptopia:bibim_naengmyeon");
        putMissing(foods, 2, 0, "croptopia:cucumber_salad", "croptopia:caesar_salad", "croptopia:leafy_salad", "croptopia:veggie_salad", "croptopia:beetroot_salad");
        putMissing(foods, 2, 0, "croptopia:fruit_salad");
        putMissing(foods, 2, 0, "croptopia:kiwi_sorbet");
        putMissing(foods, 1, 0, "croptopia:vanilla_ice_cream", "croptopia:strawberry_ice_cream", "croptopia:mango_ice_cream", "croptopia:pecan_ice_cream", "croptopia:chocolate_ice_cream", "croptopia:rum_raisin_ice_cream", "croptopia:yoghurt");
        putMissing(foods, 2, 0, "croptopia:cantaloupe", "croptopia:honeydew");
        putMissing(foods, 2, 0, "croptopia:cucumber");
        putMissing(foods, 2, 0, "croptopia:tomato", "croptopia:orange", "croptopia:grapefruit", "croptopia:peach", "croptopia:pear", "croptopia:plum", "croptopia:nectarine", "croptopia:apricot", "croptopia:mango", "croptopia:pineapple", "croptopia:kiwi", "croptopia:starfruit", "croptopia:dragonfruit", "croptopia:persimmon");
        putMissing(foods, 1, 0, "croptopia:grape", "croptopia:strawberry", "croptopia:blueberry", "croptopia:blackberry", "croptopia:raspberry", "croptopia:cranberry", "croptopia:currant", "croptopia:elderberry", "croptopia:cherry", "croptopia:kumquat", "croptopia:lemon", "croptopia:lime", "croptopia:lettuce", "croptopia:celery");
    }

    /**
     * Bountiful Fares' drinks, by id alone like the other mods'. One mod id on both loaders, and only its
     * 1.21.1 builds are still updated; an id another build lacks is never matched. Its cups and bottles
     * are each one drink, a bottle of water's size: the teas every other mod's tea, the ciders Farmer's
     * Delight's apple cider, mead, wine and coconut milk Brewin' and Chewin's mead and wine and a bottle of
     * milk. Fermented drinks are safe whatever water went into the vessel, as tea is; sea water ferments
     * nothing (see {@code src/main/bountifulfares}). The maple mead is registered under No Man's Land's
     * namespace, and only with it. The water cup is plain water and restores {@link com.thirstwastaken2.config.ThirstConfig#plainWaterValue}; its
     * entry only lets the item be found, as {@code minecraft:potion}'s does.
     */
    static void bountifulFaresDrinks(Map<String, int[]> drinks) {
        String mod = "bountifulfares:";
        putMissing(drinks, 6, 4, mod + "water_cup");
        for (String tea : new String[]{"green", "black", "chamomile", "honeysuckle", "bellflower", "torchflower"}) {
            putMissing(drinks, 6, 4, mod + tea + "_tea_bottle");
        }
        putMissing(drinks, 6, 4, mod + "apple_cider_jar", mod + "hoary_cider_jar", mod + "plum_cider_jar");
        putMissing(drinks, 5, 2, mod + "mead_bottle", "nomansland:maple_mead_bottle");
        putMissing(drinks, 3, 1, mod + "elderberry_wine_bottle", mod + "lapisberry_wine_bottle");
        putMissing(drinks, 4, 0, mod + "coconut_milk_bottle");
    }

    /**
     * Hexalia's brews, by id alone: one mod id on 1.20.1 and 1.21.1, both loaders; Gravebloom is 1.21.1's
     * only. Herbs steeped and drunk from a rustic bottle, so a tea's worth, whatever the brew does.
     * Nothing else of the mod is touched: its salt is in {@code c:salt}, which the distiller already
     * reads, and its Bloodlust stop on natural healing wraps the same {@code heal} call as
     * {@code FoodDataMixin} (issue #66).
     */
    static void hexaliaDrinks(Map<String, int[]> drinks) {
        String mod = "hexalia:";
        for (String brew : new String[]{"homestead", "bloodlust", "slimewalker", "spikeskin", "siphon", "daybloom", "gravebloom", "arachnid_grace", "hollow_silence"}) {
            putMissing(drinks, 6, 4, mod + "brew_of_" + brew);
        }
    }

    /**
     * Bountiful Fares' stews, salads, compotes and fruit; see {@link #bountifulFaresDrinks}. A stew is every
     * other mod's, a medley Farmer's Delight's mixed salad, a compote half a bowl, as Miner's Delight's
     * cups are. Its tree fruit is an apple, the spongekin slice a melon slice, the berries a sweet berry.
     * Candies, breads, cookies, stuffed potatoes, the chows and pickles are solid food and left out.
     */
    static void bountifulFaresFoods(Map<String, int[]> foods) {
        String mod = "bountifulfares:";
        for (String stew : new String[]{"apple", "bountiful", "coconut", "fish", "leek", "stone", "dirt"}) {
            putMissing(foods, 6, 4, mod + stew + "_stew");
        }
        for (String medley : new String[]{"arid", "coastal", "forest", "meadow", "mire", "tropical"}) {
            putMissing(foods, 2, 0, mod + medley + "_medley");
        }
        putMissing(foods, 2, 0, mod + "sea_salad");
        for (String fruit : new String[]{"apple", "hoary", "lemon", "orange", "plum"}) {
            putMissing(foods, 3, 2, mod + fruit + "_compote_jar");
        }
        putMissing(foods, 2, 0, mod + "orange", mod + "lemon", mod + "plum", mod + "hoary_apple", mod + "passion_fruit", mod + "spongekin_slice");
        putMissing(foods, 1, 0, mod + "elderberries", mod + "lapisberries");
    }
}
