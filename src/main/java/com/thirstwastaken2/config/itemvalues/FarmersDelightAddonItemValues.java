package com.thirstwastaken2.config.itemvalues;

import java.util.Map;

import static com.thirstwastaken2.config.itemvalues.DefaultItemValues.putMissing;

/**
 * What the drinks and foods of Farmer's Delight addons: Brewin' and Chewin', Cultural Delights, Fruits Delight, Youkais' Feasts, Ocean's
 * Delight, Expanded Delight, Rustic Delight, Miner's Delight and Extra Delight. restore by default. Every entry goes in with
 * {@code putIfAbsent}, so one call fills a fresh config and brings an older file up to date without
 * overwriting a player's edit. {@link DefaultItemValues} calls them, in the order that keeps a new
 * file's entries where they always were.
 */
final class FarmersDelightAddonItemValues {
    private FarmersDelightAddonItemValues() { }

    /**
     * Brewin' and Chewin's drinks, by id alone, as Kaleidoscope Cookery's are: they reach every node, one
     * with no integration included, and match nothing where the mod is absent. None of them is tagged
     * {@code c:drinks}, so without these they restore nothing.
     *
     * <p>A tankard or a bottle is one drink, a bottle of water's size. The stronger it is, the less it
     * restores: light brews a little under water, strong ones a third of it, and spirits nothing, so they
     * are left out, as are Salty Folly and Withering Dross, which no one drinks for their water. Brewed
     * drinks are safe whatever water went into the keg, as tea is.
     */
    static void brewinAndChewinDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 4, "brewinandchewin:kombucha");
        putMissing(drinks, 5, 2, "brewinandchewin:beer", "brewinandchewin:mead", "brewinandchewin:egg_grog", "brewinandchewin:glittering_grenadine");
        putMissing(drinks, 4, 2, "brewinandchewin:bloody_mary");
        putMissing(drinks, 3, 1, "brewinandchewin:red_wine", "brewinandchewin:white_wine", "brewinandchewin:currant_wine", "brewinandchewin:verruca_wine", "brewinandchewin:twisted_wine", "brewinandchewin:rice_wine", "brewinandchewin:old_wine");
        putMissing(drinks, 3, 1, "brewinandchewin:pale_jane", "brewinandchewin:strongroot_ale", "brewinandchewin:dread_nog");
        putMissing(drinks, 2, 1, "brewinandchewin:saccharine_rum", "brewinandchewin:steel_toe_stout", "brewinandchewin:red_rum");
    }

    /** Brewin' and Chewin's soups and porridges eaten out of a bowl in hand; see {@link #brewinAndChewinDrinks}. */
    static void brewinAndChewinFoods(Map<String, int[]> foods) {
        putMissing(foods, 6, 4, "brewinandchewin:creamy_onion_soup");
        putMissing(foods, 0, 0, "brewinandchewin:fiery_fondue", "brewinandchewin:chopped_liver");
        putMissing(foods, 2, 0, "brewinandchewin:grits");
    }

    /**
     * Cultural Delights' drinks, by id alone like the other mods': brewed in its vat since 0.18, a glass
     * bottle each, one drink per bottle. On Brewin' and Chewin's scale: the soft ones about a bottle of
     * water, light brews a little under, wine a third, a liqueur less. The spirits and the two that are
     * not drinks at all are listed as zero rather than left out: the mod tags its alcohol
     * {@code c:drinks/alcohol}, and a zero keeps the {@code c:drinks} tag value off them should a pack
     * fold that tag in. Brewed drinks are safe whatever water went into the vat, as tea is; sea water
     * brews nothing (see {@code src/main/culturaldelights}).
     */
    static void culturalDelightsDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 4, "culturaldelights:cola");
        putMissing(drinks, 6, 4, "culturaldelights:ginger_beer", "culturaldelights:butterbeer");
        putMissing(drinks, 5, 2, "culturaldelights:beer", "culturaldelights:mead", "culturaldelights:apple_cider");
        putMissing(drinks, 4, 2, "culturaldelights:bloody_mary", "culturaldelights:mojito", "culturaldelights:margarita");
        putMissing(drinks, 3, 1, "culturaldelights:wine", "culturaldelights:glow_wine");
        putMissing(drinks, 2, 1, "culturaldelights:lemon_liqueur");
        putMissing(drinks, 0, 0, "culturaldelights:tequila", "culturaldelights:gin", "culturaldelights:brandy", "culturaldelights:vodka", "culturaldelights:whiskey", "culturaldelights:rum", "culturaldelights:acid", "culturaldelights:vinegar");
    }

    /**
     * Cultural Delights' watery foods; see {@link #culturalDelightsDrinks}. A cucumber is most of a melon
     * slice, the salad and the soft corn and eggplant dishes are Farmer's Delight's. Pickles are salty and
     * the rest is dry, so neither is here.
     */
    static void culturalDelightsFoods(Map<String, int[]> foods) {
        putMissing(foods, 2, 0, "culturaldelights:cucumber");
        putMissing(foods, 1, 0, "culturaldelights:cut_cucumber");
        putMissing(foods, 2, 0, "culturaldelights:hearty_salad");
        putMissing(foods, 1, 0, "culturaldelights:creamed_corn", "culturaldelights:poached_eggplants");
        // 0.19.6's ice cream, in a wafer: Extra Delight's and Croptopia's ice cream.
        putMissing(foods, 1, 0, "culturaldelights:ice_cream");
    }

    /**
     * Fruits Delight's juices and teas, by id alone like the other mods'. The mod ships thirst values
     * of its own, but only for the original Thirst Was Taken, whose mod id is not ours, so without these
     * its drinks restore nothing. The drinks keep its values, which are already Farmer's Delight's juice
     * value here; a juice is safe whatever water went into it, as tea is.
     */
    static void fruitsDelightDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 4, "fruitsdelight:hamimelon_juice", "fruitsdelight:kiwi_juice", "fruitsdelight:orange_juice", "fruitsdelight:lemon_juice", "fruitsdelight:pear_juice", "fruitsdelight:hawberry_tea", "fruitsdelight:mango_tea", "fruitsdelight:peach_tea", "fruitsdelight:lychee_cherry_tea", "fruitsdelight:mangosteen_tea", "fruitsdelight:bayberry_soup");
        putMissing(drinks, 6, 4, "fruitsdelight:mango_milkshake");
        putMissing(drinks, 5, 2, "fruitsdelight:bellini_cocktail");
    }

    /**
     * Fruits Delight's watery foods; see {@link #fruitsDelightDrinks}. Its own values for food are two
     * to three times ours, so these follow the Farmer's Delight food each is closest to: a hamimelon
     * slice is a melon slice, a fruit an apple, a popsicle the melon popsicle, a stew a stew. Jam,
     * cookies, pies and the dry foods are left out.
     */
    static void fruitsDelightFoods(Map<String, int[]> foods) {
        putMissing(foods, 2, 0, "fruitsdelight:hamimelon_shaved_ice");
        putMissing(foods, 2, 0, "fruitsdelight:hamimelon_popsicle", "fruitsdelight:kiwi_popsicle");
        putMissing(foods, 2, 0, "fruitsdelight:apple_jello", "fruitsdelight:bayberry_jello", "fruitsdelight:blueberry_jello", "fruitsdelight:chorus_jello", "fruitsdelight:cranberry_jello", "fruitsdelight:durian_jello", "fruitsdelight:fig_jello", "fruitsdelight:glowberry_jello", "fruitsdelight:hamimelon_jello", "fruitsdelight:hawberry_jello", "fruitsdelight:kiwi_jello", "fruitsdelight:lemon_jello", "fruitsdelight:lychee_jello", "fruitsdelight:mango_jello", "fruitsdelight:mangosteen_jello", "fruitsdelight:melon_jello", "fruitsdelight:orange_jello", "fruitsdelight:peach_jello", "fruitsdelight:pear_jello", "fruitsdelight:persimmon_jello", "fruitsdelight:pineapple_jello", "fruitsdelight:sweetberry_jello");
        putMissing(foods, 2, 0, "fruitsdelight:hamimelon_slice");
        putMissing(foods, 2, 0, "fruitsdelight:orange", "fruitsdelight:lychee", "fruitsdelight:pineapple_slice", "fruitsdelight:kiwi", "fruitsdelight:peach", "fruitsdelight:mango", "fruitsdelight:pear");
        putMissing(foods, 1, 0, "fruitsdelight:orange_slice", "fruitsdelight:lemon_slice", "fruitsdelight:baked_pear");
        putMissing(foods, 6, 4, "fruitsdelight:pear_with_rock_sugar");
        putMissing(foods, 6, 4, "fruitsdelight:fig_chicken_stew");
        putMissing(foods, 2, 0, "fruitsdelight:mango_salad");
        putMissing(foods, 1, 0, "fruitsdelight:blueberry_custard");
    }

    /**
     * Youkais' Feasts' teas, juices, sakes and wines, by id alone like Fruits Delight's, whose authors
     * made it. It ships thirst values too, only for the original Thirst Was Taken, whose mod id is not
     * ours. Here a tea or juice is Farmer's Delight's juice, mead and the bottled Mio a light brew, and
     * the sakes and wines Brewin' and Chewin's wine. Every drink is safe whatever fresh water went into
     * the kettle or the tank, as tea is; sea water makes none (see {@code src/main/youkaisfeasts}).
     */
    static void youkaisFeastsDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 4, "youkaisfeasts:green_tea", "youkaisfeasts:white_tea", "youkaisfeasts:black_tea", "youkaisfeasts:oolong_tea", "youkaisfeasts:dark_tea", "youkaisfeasts:yellow_tea", "youkaisfeasts:cornflower_tea", "youkaisfeasts:tea_mocha", "youkaisfeasts:saidi_tea", "youkaisfeasts:sakura_honey_tea", "youkaisfeasts:genmai_tea", "youkaisfeasts:green_water", "youkaisfeasts:lemon_black_tea", "youkaisfeasts:moon_rocket");
        putMissing(drinks, 6, 4, "youkaisfeasts:black_grape_juice", "youkaisfeasts:red_grape_juice", "youkaisfeasts:white_grape_juice");
        putMissing(drinks, 5, 2, "youkaisfeasts:mead", "youkaisfeasts:mio");
        putMissing(drinks, 3, 1, "youkaisfeasts:kiku", "youkaisfeasts:hakutsuru", "youkaisfeasts:kappa_village", "youkaisfeasts:suigei", "youkaisfeasts:dassai", "youkaisfeasts:tengu_tango", "youkaisfeasts:full_moons_eve");
        putMissing(drinks, 3, 1, "youkaisfeasts:red_wine", "youkaisfeasts:white_wine", "youkaisfeasts:burgundy", "youkaisfeasts:champagne", "youkaisfeasts:van_allen");
    }

    /**
     * Youkais' Feasts' soups and watery foods; see {@link #youkaisFeastsDrinks}. A soup or stew is Farmer's
     * Delight's, a popsicle or shaved ice its melon popsicle, the congee Brewin' and Chewin's grits, a
     * cucumber Cultural Delights'. Soy sauce and mayonnaise, the ice cube, sushi, dango, mochi and the dry
     * dishes are left out.
     */
    static void youkaisFeastsFoods(Map<String, int[]> foods) {
        putMissing(foods, 6, 4, "youkaisfeasts:miso_soup", "youkaisfeasts:seafood_miso_soup", "youkaisfeasts:poor_god_soup", "youkaisfeasts:higan_soup", "youkaisfeasts:mushroom_soup", "youkaisfeasts:power_soup", "youkaisfeasts:potato_soup", "youkaisfeasts:borscht", "youkaisfeasts:avgolemono");
        putMissing(foods, 6, 4, "youkaisfeasts:tofu_crab_stew", "youkaisfeasts:signature_mushroom_stew", "youkaisfeasts:hokkaido_salmon_hotpot");
        putMissing(foods, 2, 0, "youkaisfeasts:milk_popsicle", "youkaisfeasts:big_popsicle", "youkaisfeasts:shaved_ice_over_rice", "youkaisfeasts:shirayuki", "youkaisfeasts:peach_tapioca");
        putMissing(foods, 2, 0, "youkaisfeasts:tutu_congee");
        putMissing(foods, 2, 0, "youkaisfeasts:cucumber");
        putMissing(foods, 1, 0, "youkaisfeasts:cucumber_slice", "youkaisfeasts:black_grape", "youkaisfeasts:red_grape", "youkaisfeasts:white_grape");
    }

    /**
     * Ocean's Delight's soup and bowls, by id alone: the mod has no drinks and uses no water, and its
     * Fabric and NeoForge builds share one mod id, so an id reaches every node and matches nothing where
     * the mod is absent. The Guardian Soup is a broth, the braised sea pickle a stew and the seagrass
     * salad a light bowl. The fried, baked and rolled seafood is left out.
     */
    static void oceansDelightFoods(Map<String, int[]> foods) {
        putMissing(foods, 6, 4, "oceansdelight:bowl_of_guardian_soup");
        putMissing(foods, 6, 4, "oceansdelight:braised_sea_pickle");
        putMissing(foods, 2, 0, "oceansdelight:seagrass_salad");
    }

    /**
     * Expanded Delight's juices and goat milk, by id alone like the other mods'. None is tagged
     * {@code c:drinks}. The juices are Farmer's Delight's juice value, the goat milk its milk's. NeoForge
     * 1.21.1 is the mod's only build for a version this mod supports, so these match nothing elsewhere.
     */
    static void expandedDelightDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 4, "expandeddelight:apple_juice", "expandeddelight:sweet_berry_juice", "expandeddelight:glow_berry_juice", "expandeddelight:cranberry_juice");
        putMissing(drinks, 4, 0, "expandeddelight:goat_milk_bottle", "expandeddelight:goat_milk_bucket");
    }

    /**
     * Expanded Delight's soups and salads; see {@link #expandedDelightDrinks}. Each is Farmer's Delight's
     * stew or mixed salad. Two of them cook from a water bucket, which takes no sea water (see
     * {@code src/main/expandeddelight}). The jellies are jam in a jar, and mac and cheese and the rest are
     * dry, so they are left out.
     */
    static void expandedDelightFoods(Map<String, int[]> foods) {
        putMissing(foods, 6, 4, "expandeddelight:asparagus_soup", "expandeddelight:asparagus_soup_creamy", "expandeddelight:peanut_honey_soup");
        putMissing(foods, 2, 0, "expandeddelight:cinnamon_apples", "expandeddelight:peanut_salad", "expandeddelight:sweet_potato_salad", "expandeddelight:goat_cheese_beetroot_salad");
        putMissing(foods, 1, 0, "expandeddelight:cranberries");
    }

    /**
     * Rustic Delight's coffees and syrup, by id alone like the other mods'; its builds share one mod id
     * on every version, both loaders. The mod ships thirst values of its own, but only for the original
     * Thirst Was Taken, whose mod id is not ours. The drinks keep them: plain coffee a little under a
     * bottle of water, dark coffee less, the sweetened ones more. Its cooking oil is left out.
     */
    static void rusticDelightDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 4, "rusticdelight:coffee");
        putMissing(drinks, 6, 4, "rusticdelight:dark_coffee");
        putMissing(drinks, 6, 4, "rusticdelight:milk_coffee");
        putMissing(drinks, 6, 4, "rusticdelight:syrup_coffee", "rusticdelight:chocolate_coffee", "rusticdelight:honey_coffee", "rusticdelight:pumpkin_coffee", "rusticdelight:cherry_blossom_coffee");
        putMissing(drinks, 2, 0, "rusticdelight:syrup");
    }

    /**
     * Rustic Delight's soups, sweet salad and raw bell peppers; see {@link #rusticDelightDrinks}. Its own
     * food values are close to ours, so these follow Farmer's Delight's: a soup is a stew, a pepper a
     * tomato, a slice a cabbage leaf. Roasted, rolled and stuffed peppers, the potato salad and the
     * batter are left out.
     */
    static void rusticDelightFoods(Map<String, int[]> foods) {
        putMissing(foods, 6, 4, "rusticdelight:bell_pepper_soup", "rusticdelight:calamari_soup");
        putMissing(foods, 2, 0, "rusticdelight:sweet_salad");
        putMissing(foods, 1, 0, "rusticdelight:bell_pepper_green", "rusticdelight:bell_pepper_yellow", "rusticdelight:bell_pepper_red", "rusticdelight:bell_pepper_orange", "rusticdelight:bell_pepper_white", "rusticdelight:bell_pepper_pink", "rusticdelight:bell_pepper_blue", "rusticdelight:bell_pepper_purple", "rusticdelight:bell_pepper_black");
        putMissing(foods, 1, 0, "rusticdelight:bell_pepper_slice_green", "rusticdelight:bell_pepper_slice_yellow", "rusticdelight:bell_pepper_slice_red", "rusticdelight:bell_pepper_slice_orange", "rusticdelight:bell_pepper_slice_white", "rusticdelight:bell_pepper_slice_pink", "rusticdelight:bell_pepper_slice_blue", "rusticdelight:bell_pepper_slice_purple", "rusticdelight:bell_pepper_slice_black");
    }

    /**
     * Miner's Delight's milk cup, by id alone like the other mods'. The mod is {@code minersdelight} on
     * NeoForge 1.21.1 and {@code miners_delight} on Forge 1.20.1, its only builds for a version this mod
     * supports, so every id is listed under both. The milk cup is Farmer's Delight's milk bottle. The
     * water cup is a water container with no value of its own, like the water bucket; see
     * {@code WaterPurity}.
     */
    static void minersDelightDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 4, 0, "minersdelight:milk_cup", "miners_delight:milk_cup");
    }

    /**
     * Miner's Delight's soups; see {@link #minersDelightDrinks}. Its three bowls are Farmer's Delight's
     * stews. A cup is half a bowl, eaten fast, so each cup gets half its bowl's value, rounded up. The
     * cave foods, plates and sandwiches are dry and left out.
     */
    static void minersDelightFoods(Map<String, int[]> foods) {
        for (String mod : new String[]{"minersdelight:", "miners_delight:"}) {
            putMissing(foods, 6, 4, mod + "cave_soup", mod + "bat_soup", mod + "insect_stew");
            putMissing(foods, 3, 2, mod + "beef_stew_cup", mod + "chicken_soup_cup", mod + "fish_stew_cup",
                    mod + "baked_cod_stew_cup", mod + "noodle_soup_cup", mod + "pumpkin_soup_cup",
                    mod + "vegetable_soup_cup", mod + "onion_soup_cup", mod + "mushroom_stew_cup",
                    mod + "rabbit_stew_cup", mod + "cave_soup_cup", mod + "bat_soup_cup", mod + "insect_stew_cup");
            putMissing(foods, 3, 2, mod + "bone_broth_cup", mod + "beetroot_soup_cup");
        }
    }

    /**
     * Extra Delight's drinks, by id alone like the other mods'. Its only official build is NeoForge 1.21.1;
     * an id another build lacks is never matched. Ades, punch and the juices are Farmer's Delight's juice,
     * lemon and lime juice drunk neat half of one, milkshakes Fruits Delight's, gourmet hot chocolate
     * Farmer's Delight's hot cocoa, the milky drinks and ginger beer a bottle of milk, tea the other mods'
     * teas, coffee Croptopia's. Its Tough As Nails tags rank them the same way.
     */
    static void extraDelightDrinks(Map<String, int[]> drinks) {
        for (String juice : new String[]{"lemonade", "limeade", "orangeade", "punch", "glow_berry_juice",
                "sweet_berry_juice", "tomato_juice", "cactus_juice", "orange_juice", "grapefruit_juice",
                "strawberry_juice", "pink_lemonade"}) {
            putMissing(drinks, 6, 4, "extradelight:" + juice);
        }
        putMissing(drinks, 4, 2, "extradelight:lemon_juice", "extradelight:lime_juice");
        for (String shake : new String[]{"milkshake", "chocolate_milkshake", "glow_berry_milkshake",
                "sweet_berry_milkshake", "pumpkin_milkshake", "honey_milkshake", "apple_milkshake",
                "cookie_dough_milkshake", "mint_chip_milkshake", "nut_butter_milkshake", "strawberry_milkshake"}) {
            putMissing(drinks, 6, 4, "extradelight:" + shake);
        }
        putMissing(drinks, 6, 4, "extradelight:gourmet_hot_chocolate");
        putMissing(drinks, 6, 4, "extradelight:chocolate_milk", "extradelight:eggnog", "extradelight:horchata", "extradelight:xocolati", "extradelight:ginger_beer", "extradelight:strawberry_milk");
        putMissing(drinks, 4, 0, "extradelight:soy_milk");
        putMissing(drinks, 6, 4, "extradelight:tea");
        putMissing(drinks, 6, 4, "extradelight:coffee", "extradelight:dalgona_coffee");
    }

    /**
     * Extra Delight's soups, popsicles and cold desserts; see {@link #extraDelightDrinks}. A soup or stew
     * is every other mod's, curry and chili thicker, like noodles. Fruit and honey popsicles are Farmer's
     * Delight's melon popsicle, the creamier ones less; ice cream and custard Croptopia's ice cream. The
     * rice dishes, pies, puddings and jellies are solid food, and each feast serves one of these bowls.
     */
    static void extraDelightFoods(Map<String, int[]> foods) {
        for (String soup : new String[]{"borscht", "cactus_soup", "carrot_soup", "corn_chowder", "fish_soup",
                "hazelnut_soup", "miso_soup", "mulligatawny_soup", "onion_soup", "oxtail_soup", "potato_soup",
                "sauerkraut_soup", "tomato_soup", "gazpacho", "congee", "chicken_stew", "lamb_stew", "pork_stew"}) {
            putMissing(foods, 6, 4, "extradelight:" + soup);
        }
        putMissing(foods, 6, 4, "extradelight:melon_gazpacho");
        putMissing(foods, 2, 0, "extradelight:curry", "extradelight:chili_con_carne", "extradelight:white_chili");
        putMissing(foods, 2, 0, "extradelight:apple_popsicle", "extradelight:glow_berry_popsicle", "extradelight:sweet_berry_popsicle", "extradelight:honey_popsicle", "extradelight:strawberry_popsicle");
        putMissing(foods, 2, 0, "extradelight:caramel_popsicle", "extradelight:cinnamon_popsicle", "extradelight:fudge_popsicle");
        for (String cold : new String[]{"ice_cream", "apple_ice_cream", "chocolate_ice_cream",
                "cookie_dough_ice_cream", "glow_berry_ice_cream", "honey_ice_cream", "mint_chip_ice_cream",
                "nut_butter_ice_cream", "pumpkin_ice_cream", "sweet_berry_ice_cream", "stuffed_apple_ice_cream",
                "ice_cream_sundae", "affogato", "apple_custard", "caramel_custard", "chocolate_custard",
                "honey_custard", "nut_butter_custard", "pumpkin_custard", "sweet_berry_custard",
                "strawberry_ice_cream", "strawberry_custard"}) {
            putMissing(foods, 1, 0, "extradelight:" + cold);
        }
    }
}
