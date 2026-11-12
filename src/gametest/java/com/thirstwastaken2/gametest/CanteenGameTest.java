package com.thirstwastaken2.gametest;

import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.item.WaterContainers;
import com.thirstwastaken2.item.WaterskinItem;
import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The copper canteen and the iron flask: their capacity, boiling on a campfire through the real use
 * path, and which of them a furnace takes. Boiling is driven by calling the use path once per step, the
 * way a held right click repeats it.
 */
public final class CanteenGameTest {
    private static final BlockPos CAMPFIRE = new BlockPos(2, 1, 2);
    private static final WaterQuality DIRTY = WaterQuality.fresh(WaterPurity.MIN);
    private static final WaterQuality CLEAN = WaterQuality.fresh(WaterPurity.BOILED);

    @GameTest
    public void eachVesselHoldsItsOwnCapacity(GameTestHelper helper) {
        checkCapacity(helper, ThirstItems.WATERSKIN, 4);
        checkCapacity(helper, ThirstItems.COPPER_CANTEEN, 4);
        checkCapacity(helper, ThirstItems.IRON_FLASK, 6);
        checkCapacity(helper, ThirstItems.DIAMOND_FLASK, 10);
        checkCapacity(helper, ThirstItems.NETHERITE_FLASK, 15);
        helper.succeed();
    }

    @GameTest
    public void eachVesselCapacityIsReadFromTheConfig(GameTestHelper helper) {
        TestFixtures.withConfig(config -> {
            config.waterskinCapacity = 1;
            config.copperCanteenCapacity = com.thirstwastaken2.config.ThirstConfig.MAX_CONTAINER;
            config.ironFlaskCapacity = 99;
        }, () -> {
            checkCapacity(helper, ThirstItems.WATERSKIN, 1);
            ItemStack canteen = new ItemStack(ThirstItems.COPPER_CANTEEN);
            WaterskinItem.addWater(canteen, DIRTY, 99);
            TestFixtures.check(helper, WaterskinItem.servings(canteen) == 64,
                    "a canteen set to 64 should hold 64, got " + WaterskinItem.servings(canteen));
            ItemStack flask = new ItemStack(ThirstItems.IRON_FLASK);
            WaterskinItem.addWater(flask, DIRTY, 99);
            TestFixtures.check(helper, WaterskinItem.servings(flask) == 64,
                    "a capacity over 64 should clamp to 64, got " + WaterskinItem.servings(flask));
        });
        helper.succeed();
    }

    /** A vessel saved holding more than a lowered capacity keeps it: it can be drunk from, but takes nothing. */
    @GameTest
    public void aVesselOverALoweredCapacityKeepsItsWater(GameTestHelper helper) {
        ItemStack flask = filled(ThirstItems.IRON_FLASK, CLEAN, 6);
        TestFixtures.withConfig(config -> config.ironFlaskCapacity = 2, () -> {
            TestFixtures.check(helper, WaterskinItem.servings(flask) == 6 && !WaterskinItem.hasRoom(flask),
                    "lowering the capacity should take no water away and leave no room, got "
                            + WaterskinItem.servings(flask));
            TestFixtures.check(helper, !WaterskinItem.addWater(flask, CLEAN, 1),
                    "a flask over its capacity should take no more water");
            TestFixtures.check(helper, WaterskinItem.removeWater(flask, 1) && WaterskinItem.servings(flask) == 5,
                    "a flask over its capacity should still be drunk from, got " + WaterskinItem.servings(flask));
            TestFixtures.check(helper, WaterskinItem.sprite(9, 4) == 3,
                    "a skin over its capacity should look full");
        });
        helper.succeed();
    }

    @GameTest
    public void aRigidVesselKeepsOneSprite(GameTestHelper helper) {
        ItemStack canteen = filled(ThirstItems.COPPER_CANTEEN, DIRTY, 2);
        TestFixtures.check(helper, Vanilla.modelSelectorOf(canteen) == null,
                "a canteen has one sprite, so it should carry no custom model data, got "
                        + Vanilla.modelSelectorOf(canteen));
        TestFixtures.check(helper, Vanilla.modelSelectorOf(filled(ThirstItems.WATERSKIN, DIRTY, 2)) != null,
                "the waterskin should still pick its sprite by fill level");
        helper.succeed();
    }

    @GameTest
    public void aCanteenBoilsCleanOverALitCampfire(GameTestHelper helper) {
        ServerPlayer player = playerAtCampfire(helper, Blocks.CAMPFIRE.defaultBlockState(),
                filled(ThirstItems.COPPER_CANTEEN, DIRTY, 2));
        int steps = 2 * canteenBoilTicks() / WaterskinItem.BOIL_STEP_TICKS;

        use(helper, player, steps - 1);
        TestFixtures.check(helper, WaterPurity.quality(held(player)).equals(DIRTY),
                "one step short of the boil the water should still be dirty, got " + WaterPurity.quality(held(player)));
        use(helper, player, 1);
        TestFixtures.check(helper, WaterPurity.quality(held(player)).equals(CLEAN),
                "a full boil should leave the canteen Clean, got " + WaterPurity.quality(held(player)));
        TestFixtures.check(helper, WaterskinItem.servings(held(player)) == 2,
                "boiling must not change how much water there is, got " + WaterskinItem.servings(held(player)));
        helper.succeed();
    }

    @GameTest
    public void copperBoilsFasterThanIron(GameTestHelper helper) {
        TestFixtures.check(helper, canteenBoilTicks() < flaskBoilTicks(),
                "copper carries heat better, so a canteen serving should boil sooner than a flask one");
        ServerPlayer player = playerAtCampfire(helper, Blocks.SOUL_CAMPFIRE.defaultBlockState(),
                filled(ThirstItems.IRON_FLASK, DIRTY, 1));
        use(helper, player, flaskBoilTicks() / WaterskinItem.BOIL_STEP_TICKS);
        TestFixtures.check(helper, WaterPurity.quality(held(player)).equals(CLEAN),
                "a flask should boil over a soul campfire too, got " + WaterPurity.quality(held(player)));
        helper.succeed();
    }

    @GameTest
    public void stoppingKeepsTheProgressAndMoreWaterResetsIt(GameTestHelper helper) {
        ServerPlayer player = playerAtCampfire(helper, Blocks.CAMPFIRE.defaultBlockState(),
                filled(ThirstItems.IRON_FLASK, DIRTY, 3));
        use(helper, player, 5);
        int kept = WaterskinItem.boilProgress(player, held(player));
        TestFixtures.check(helper, kept == 5 * WaterskinItem.BOIL_STEP_TICKS,
                "five steps should be kept between uses, got " + kept + " ticks");

        WaterskinItem.addWater(held(player), DIRTY, 1);
        use(helper, player, 1);
        int restarted = WaterskinItem.boilProgress(player, held(player));
        TestFixtures.check(helper, restarted == WaterskinItem.BOIL_STEP_TICKS,
                "water poured in has not boiled, so the count should restart, got " + restarted + " ticks");
        helper.succeed();
    }

    @GameTest
    public void saltEmptyAndUnlitDoNotBoil(GameTestHelper helper) {
        ServerPlayer salty = playerAtCampfire(helper, Blocks.CAMPFIRE.defaultBlockState(),
                filled(ThirstItems.COPPER_CANTEEN, WaterQuality.SALT, 4));
        use(helper, salty, 4 * canteenBoilTicks() / WaterskinItem.BOIL_STEP_TICKS);
        TestFixtures.check(helper, WaterPurity.isSalty(held(salty)), "boiling must not take the salt out");

        ServerPlayer unlit = playerAtCampfire(helper, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false),
                filled(ThirstItems.COPPER_CANTEEN, DIRTY, 1));
        use(helper, unlit, canteenBoilTicks() / WaterskinItem.BOIL_STEP_TICKS);
        TestFixtures.check(helper, WaterPurity.quality(held(unlit)).equals(DIRTY),
                "an unlit campfire should not boil anything, got " + WaterPurity.quality(held(unlit)));
        TestFixtures.check(helper, WaterskinItem.boilProgress(unlit, held(unlit)) == 0,
                "an unlit campfire should not count toward a boil");

        ServerPlayer skin = playerAtCampfire(helper, Blocks.CAMPFIRE.defaultBlockState(),
                filled(ThirstItems.WATERSKIN, DIRTY, 3));
        use(helper, skin, 3 * flaskBoilTicks() / WaterskinItem.BOIL_STEP_TICKS);
        TestFixtures.check(helper, WaterPurity.quality(held(skin)).equals(DIRTY),
                "a leather waterskin cannot go on the fire, got " + WaterPurity.quality(held(skin)));
        helper.succeed();
    }

    /**
     * One furnace recipe per grade takes a canteen or a flask at any fill, and keeps the fill: the
     * recipe names no servings, so this checks one, the default and 64. The time is each serving's
     * boil time for every serving it holds.
     */
    @GameTest
    public void bothVesselsBoilInAFurnaceAtAnyFill(GameTestHelper helper) {
        TestFixtures.withConfig(config -> {
            config.copperCanteenCapacity = com.thirstwastaken2.config.ThirstConfig.MAX_CONTAINER;
            config.ironFlaskCapacity = com.thirstwastaken2.config.ThirstConfig.MAX_CONTAINER;
            config.diamondFlaskCapacity = com.thirstwastaken2.config.ThirstConfig.MAX_CONTAINER;
            config.netheriteFlaskCapacity = com.thirstwastaken2.config.ThirstConfig.MAX_CONTAINER;
        }, () -> {
            for (Item vessel : List.of(ThirstItems.COPPER_CANTEEN, ThirstItems.IRON_FLASK, ThirstItems.DIAMOND_FLASK,
                    ThirstItems.NETHERITE_FLASK)) {
                for (int servings : new int[] {1, 4, 6, 64}) {
                    for (WaterQuality grade : List.of(DIRTY, WaterQuality.fresh(1))) {
                        ItemStack result = TestFixtures.cook(helper, RecipeType.SMELTING, filled(vessel, grade, servings));
                        TestFixtures.check(helper, result.is(vessel) && WaterskinItem.servings(result) == servings
                                        && CLEAN.equals(WaterPurity.quality(result)),
                                "a furnace should boil " + servings + " servings of " + grade + " in " + vessel
                                        + " Clean and keep them, got " + result + " holding "
                                        + WaterskinItem.servings(result) + " of " + WaterPurity.quality(result));
                    }
                }
                TestFixtures.check(helper, WaterskinItem.furnaceTicks(filled(vessel, DIRTY, 5))
                                == 5 * WaterskinItem.boilTicksPerServing(filled(vessel, DIRTY, 1)),
                        vessel + " should take its boil time once per serving in a furnace");
                TestFixtures.check(helper, TestFixtures.cook(helper, RecipeType.SMOKING, filled(vessel, DIRTY, 2)).isEmpty(),
                        vessel + " has no smoker recipe");
                TestFixtures.check(helper, TestFixtures.cook(helper, RecipeType.SMELTING, filled(vessel, CLEAN, 2)).isEmpty()
                                && WaterskinItem.furnaceTicks(filled(vessel, CLEAN, 2)) == 0,
                        "Clean water in " + vessel + " has nothing left to boil");
            }
        });
        TestFixtures.check(helper, WaterskinItem.furnaceTicks(filled(ThirstItems.COPPER_CANTEEN, DIRTY, 4)) == 160
                        && WaterskinItem.furnaceTicks(filled(ThirstItems.IRON_FLASK, DIRTY, 6)) == 360,
                "a full canteen should take 8 seconds in a furnace and a full flask 18");
        TestFixtures.check(helper, TestFixtures.cook(helper, RecipeType.SMELTING,
                        filled(ThirstItems.IRON_FLASK, WaterQuality.SALT, 2)).isEmpty(),
                "salt water in a flask should not smelt");
        TestFixtures.check(helper, TestFixtures.cook(helper, RecipeType.SMELTING,
                        filled(ThirstItems.WATERSKIN, DIRTY, 2)).isEmpty(),
                "a waterskin has no furnace recipe");
        TestFixtures.check(helper, TestFixtures.cook(helper, RecipeType.CAMPFIRE_COOKING,
                        filled(ThirstItems.IRON_FLASK, DIRTY, 2)).isEmpty(),
                "a campfire recipe would put the flask in the campfire's slots instead of boiling it in hand");
        helper.succeed();
    }

    /**
     * A real furnace, ticked until it is done, hands back the vessel still full. Asking the recipe, as
     * the test above does, was not enough: before 1.21.2 the Fabric furnace takes the recipe's own
     * result rather than assembling one, and a boiled canteen came out empty (issue #55).
     */
    @GameTest
    public void aFurnaceHandsBackTheVesselStillFull(GameTestHelper helper) {
        BlockPos at = helper.absolutePos(CAMPFIRE);
        int furnaces = 0;
        for (Item vessel : List.of(ThirstItems.COPPER_CANTEEN, ThirstItems.IRON_FLASK)) {
            for (WaterQuality grade : List.of(DIRTY, WaterQuality.fresh(1))) {
                BlockPos pos = at.offset(2 * (furnaces % 2), 0, 2 * (furnaces++ / 2));
                helper.getLevel().setBlockAndUpdate(pos, Blocks.FURNACE.defaultBlockState());
                net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace =
                        (net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity) helper.getLevel().getBlockEntity(pos);
                ItemStack input = filled(vessel, grade, WaterskinItem.capacity(new ItemStack(vessel)));
                int ticks = WaterskinItem.furnaceTicks(input);
                furnace.setItem(0, input.copy());
                furnace.setItem(1, new ItemStack(Items.COAL));
                for (int tick = 0; tick < ticks + 20 && furnace.getItem(2).isEmpty(); tick++) {
                    net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.serverTick(
                            helper.getLevel(), pos, helper.getLevel().getBlockState(pos), furnace);
                }
                ItemStack result = furnace.getItem(2);
                TestFixtures.check(helper, result.is(vessel) && furnace.getItem(0).isEmpty()
                                && WaterskinItem.servings(result) == WaterskinItem.servings(input)
                                && CLEAN.equals(WaterPurity.quality(result)),
                        "a furnace should boil " + vessel + " of " + grade + " into one still holding "
                                + WaterskinItem.servings(input) + " Clean, got " + result + " holding "
                                + WaterskinItem.servings(result) + " of " + WaterPurity.quality(result));
            }
        }
        helper.succeed();
    }

    @GameTest
    public void bothAreCraftedFromScratch(GameTestHelper helper) {
        ItemStack copper = new ItemStack(Items.COPPER_INGOT);
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack canteen = TestFixtures.craftGrid(helper, 3, 3, new ArrayList<>(List.of(
                ItemStack.EMPTY, new ItemStack(Items.STRING), ItemStack.EMPTY,
                copper.copy(), ItemStack.EMPTY, copper.copy(),
                ItemStack.EMPTY, copper.copy(), ItemStack.EMPTY)));
        ItemStack flask = TestFixtures.craftGrid(helper, 3, 3, grid(new ItemStack(Items.IRON_NUGGET), iron));
        TestFixtures.check(helper, canteen.is(ThirstItems.COPPER_CANTEEN), "string over three copper should make a canteen, got " + canteen);
        TestFixtures.check(helper, flask.is(ThirstItems.IRON_FLASK), "a nugget over three iron should make a flask, got " + flask);
        TestFixtures.check(helper, WaterContainers.handles(canteen) && WaterContainers.capacity(flask) == 6,
                "both should be fluid containers, the flask of six servings");
        helper.succeed();
    }

    /** Three clay balls make three bowls, a waterskin is four leather, the boiler's centre is iron. */
    @GameTest
    public void theOtherContainersCostWhatTheDesignSays(GameTestHelper helper) {
        ItemStack clay = new ItemStack(Items.CLAY_BALL);
        ItemStack bowls = TestFixtures.craftGrid(helper, 3, 3, new ArrayList<>(List.of(
                clay.copy(), ItemStack.EMPTY, clay.copy(),
                ItemStack.EMPTY, clay.copy(), ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY)));
        TestFixtures.check(helper, bowls.is(ThirstItems.CLAY_BOWL) && bowls.getCount() == 3,
                "three clay balls should make three clay bowls, got " + bowls);

        ItemStack leather = new ItemStack(Items.LEATHER);
        ItemStack skin = TestFixtures.craftGrid(helper, 3, 3, new ArrayList<>(List.of(
                ItemStack.EMPTY, leather.copy(), ItemStack.EMPTY,
                leather.copy(), ItemStack.EMPTY, leather.copy(),
                ItemStack.EMPTY, leather.copy(), ItemStack.EMPTY)));
        TestFixtures.check(helper, skin.is(ThirstItems.WATERSKIN), "four leather in a ring should make a waterskin, got " + skin);

        ItemStack copper = new ItemStack(Items.COPPER_INGOT);
        ItemStack boiler = TestFixtures.craftGrid(helper, 3, 3, new ArrayList<>(List.of(
                copper.copy(), new ItemStack(ThirstItems.COPPER_PIPE), copper.copy(),
                copper.copy(), new ItemStack(Items.IRON_INGOT), copper.copy(),
                copper.copy(), copper.copy(), copper.copy())));
        TestFixtures.check(helper, boiler.is(ThirstItems.DISTILLER_BOILER),
                "copper round an iron ingot under a pipe should make the distiller boiler, got " + boiler);
        helper.succeed();
    }

    private static void checkCapacity(GameTestHelper helper, Item item, int capacity) {
        ItemStack stack = new ItemStack(item);
        WaterskinItem.addWater(stack, DIRTY, 99);
        TestFixtures.check(helper, WaterskinItem.servings(stack) == capacity && WaterskinItem.capacity(stack) == capacity,
                item + " should hold exactly " + capacity + ", got " + WaterskinItem.servings(stack));
        TestFixtures.check(helper, !WaterskinItem.addWater(stack, DIRTY, 1), item + " should refuse water when full");
    }

    private static ItemStack filled(Item item, WaterQuality quality, int servings) {
        ItemStack stack = new ItemStack(item);
        WaterskinItem.addWater(stack, quality, servings);
        return stack;
    }

    /** Three {@code metal} under {@code top}, two at the sides and one below, the flask's shape. */
    /**
     * Six diamonds make the diamond flask, and the smithing table turns it into the netherite one with
     * its water still in it, as it carries enchantments over on netherite gear.
     */
    @GameTest
    public void diamondFlaskIsCraftedAndUpgradesToNetherite(GameTestHelper helper) {
        ItemStack diamond = new ItemStack(Items.DIAMOND);
        ItemStack flask = TestFixtures.craftGrid(helper, 3, 3, new ArrayList<>(List.of(
                ItemStack.EMPTY, diamond.copy(), ItemStack.EMPTY,
                diamond.copy(), ItemStack.EMPTY, diamond.copy(),
                diamond.copy(), diamond.copy(), diamond.copy())));
        TestFixtures.check(helper, flask.is(ThirstItems.DIAMOND_FLASK), "six diamonds should make a diamond flask, got " + flask);

        WaterskinItem.addWater(flask, DIRTY, 7);
        ItemStack netherite = TestFixtures.smith(helper, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                flask, new ItemStack(Items.NETHERITE_INGOT));
        TestFixtures.check(helper, netherite.is(ThirstItems.NETHERITE_FLASK) && WaterskinItem.servings(netherite) == 7
                        && DIRTY.equals(WaterPurity.quality(netherite)),
                "the smithing table should upgrade the flask and keep its 7 servings of " + DIRTY + ", got " + netherite
                        + " holding " + WaterskinItem.servings(netherite) + " of " + WaterPurity.quality(netherite));
        TestFixtures.check(helper, WaterContainers.capacity(new ItemStack(ThirstItems.NETHERITE_FLASK)) == 15,
                "the netherite flask should be a fluid container of fifteen servings");
        helper.succeed();
    }

    private static List<ItemStack> grid(ItemStack top, ItemStack metal) {
        return new ArrayList<>(List.of(
                ItemStack.EMPTY, top.copy(), ItemStack.EMPTY,
                metal.copy(), ItemStack.EMPTY, metal.copy(),
                ItemStack.EMPTY, metal.copy(), ItemStack.EMPTY));
    }

    private static ServerPlayer playerAtCampfire(GameTestHelper helper, BlockState campfire, ItemStack stack) {
        helper.setBlock(CAMPFIRE, campfire);
        ServerPlayer player = TestFixtures.mockPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    private static ItemStack held(ServerPlayer player) {
        return player.getItemInHand(InteractionHand.MAIN_HAND);
    }

    /** Uses the held item on the campfire {@code times}, as a held right click repeats it. */
    private static void use(GameTestHelper helper, ServerPlayer player, int times) {
        BlockPos pos = helper.absolutePos(CAMPFIRE);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        for (int i = 0; i < times; i++) {
            player.gameMode.useItemOn(player, helper.getLevel(), held(player), InteractionHand.MAIN_HAND, hit);
        }
    }

    /** Ticks a serving takes to boil in the copper canteen, from the config. */
    private static int canteenBoilTicks() {
        return WaterskinItem.boilTicksPerServing(new ItemStack(ThirstItems.COPPER_CANTEEN));
    }

    /** Ticks a serving takes to boil in the iron flask, from the config. */
    private static int flaskBoilTicks() {
        return WaterskinItem.boilTicksPerServing(new ItemStack(ThirstItems.IRON_FLASK));
    }
}
