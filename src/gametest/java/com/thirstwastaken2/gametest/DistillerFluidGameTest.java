package com.thirstwastaken2.gametest;

import com.thirstwastaken2.block.DistillerBlock;
import com.thirstwastaken2.block.DistillerBlockEntity;
import com.thirstwastaken2.block.ThirstBlocks;
import com.thirstwastaken2.gametest.platform.DistillerFluids;
import com.thirstwastaken2.gametest.platform.FluidMove;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The copper distiller through each loader's block fluid capability, the way a Pipez pipe reaches it
 * (issue #65): water of any grade into the boiler, in a pipe's 50 mB steps, Pure water out of the basin,
 * anything else refused, and a move that is not committed leaving the tanks as they were. See
 * {@code docs/dev/integration/storage/PIPEZ-INTEGRATION.md}.
 */
public final class DistillerFluidGameTest {
    private static final BlockPos BOILER = new BlockPos(3, 2, 3);
    private static final BlockPos TUB = BOILER.west();
    private static final WaterQuality MURKY = WaterQuality.fresh(1);
    private static final WaterQuality PURE = WaterQuality.fresh(WaterPurity.MAX);

    @GameTest
    public void aPipesFiftyMillibucketsFillTheBoilerAServingAtATime(GameTestHelper helper) {
        DistillerBlockEntity machine = machine(helper);
        Level level = helper.getLevel();
        BlockPos boiler = helper.absolutePos(BOILER);
        for (int step = 0; step < 4; step++) {
            TestFixtures.check(helper, DistillerFluids.fillWater(level, boiler, MURKY, 50, true) == 50,
                    "each 50 mB should go in");
        }
        TestFixtures.check(helper, machine.boilerMb() == 200 && machine.boilerServings() == 0,
                "four steps should leave 200 mB and no whole serving, got " + machine.boilerMb());
        DistillerFluids.fillWater(level, boiler, MURKY, 50, true);
        TestFixtures.check(helper, machine.boilerServings() == 1 && MURKY.equals(machine.boilerQuality()),
                "the fifth should make one Murky serving, got " + machine.boilerServings() + " of " + machine.boilerQuality());
        helper.succeed();
    }

    @GameTest
    public void theBoilerTakesNoMoreThanItsTank(GameTestHelper helper) {
        DistillerBlockEntity machine = machine(helper);
        int filled = DistillerFluids.fillWater(helper.getLevel(), helper.absolutePos(BOILER), MURKY, 100000, true);
        TestFixtures.check(helper, filled == DistillerBlockEntity.tankMb() && machine.boilerMb() == filled,
                "the boiler should fill to its " + DistillerBlockEntity.tankMb() + " mB, took " + filled);
        helper.succeed();
    }

    @GameTest
    public void gradesMixToTheWorseAsWhenPoured(GameTestHelper helper) {
        DistillerBlockEntity machine = machine(helper);
        Level level = helper.getLevel();
        BlockPos boiler = helper.absolutePos(BOILER);
        DistillerFluids.fillWater(level, boiler, PURE, 250, true);
        DistillerFluids.fillWater(level, boiler, MURKY, 250, true);
        TestFixtures.check(helper, MURKY.equals(machine.boilerQuality()),
                "Murky into Pure should leave Murky, got " + machine.boilerQuality());
        DistillerFluids.fillWater(level, boiler, WaterQuality.SALT, 250, true);
        TestFixtures.check(helper, WaterQuality.SALT.equals(machine.boilerQuality()),
                "sea water should make it all salty, got " + machine.boilerQuality());
        helper.succeed();
    }

    @GameTest
    public void waterWithNoGradeCountsAsTheDefault(GameTestHelper helper) {
        DistillerBlockEntity machine = machine(helper);
        DistillerFluids.fillWater(helper.getLevel(), helper.absolutePos(BOILER), null, 250, true);
        WaterQuality expected = WaterQuality.fresh(com.thirstwastaken2.config.ThirstConfig.get().defaultQuality);
        TestFixtures.check(helper, expected.equals(machine.boilerQuality()),
                "plain water should be " + expected + ", got " + machine.boilerQuality());
        helper.succeed();
    }

    @GameTest
    public void lavaIsRefused(GameTestHelper helper) {
        DistillerBlockEntity machine = machine(helper);
        int filled = DistillerFluids.fillLava(helper.getLevel(), helper.absolutePos(BOILER), 1000);
        TestFixtures.check(helper, filled == 0 && machine.boilerMb() == 0, "lava should be refused, took " + filled);
        helper.succeed();
    }

    @GameTest
    public void theBasinGivesPureWaterAndTheBoilerGivesNone(GameTestHelper helper) {
        DistillerBlockEntity machine = machine(helper);
        Level level = helper.getLevel();
        BlockPos boiler = helper.absolutePos(BOILER);
        DistillerFluids.fillWater(level, boiler, MURKY, 500, true);
        FluidMove none = DistillerFluids.drain(level, boiler, 50, true);
        TestFixtures.check(helper, none.amount() == 0 && machine.boilerMb() == 500,
                "the boiler's Murky water should not come out, drew " + none.amount());

        distil(machine, 1);
        FluidMove drawn = DistillerFluids.drain(level, boiler, 50, true);
        TestFixtures.check(helper, drawn.amount() == 50 && PURE.equals(drawn.quality()),
                "50 mB of Pure water should come out of the basin, got " + drawn.amount() + " of " + drawn.quality());
        TestFixtures.check(helper, machine.basinMb() == 200 && machine.basinServings() == 0,
                "the basin should keep the other 200 mB, got " + machine.basinMb());
        helper.succeed();
    }

    @GameTest
    public void aPartServingWaitsAndKeepsItsGrade(GameTestHelper helper) {
        DistillerBlockEntity machine = machine(helper);
        DistillerFluids.fillWater(helper.getLevel(), helper.absolutePos(BOILER), MURKY, 300, true);
        distil(machine, 2);
        TestFixtures.check(helper, machine.basinServings() == 1 && machine.boilerMb() == 50
                        && MURKY.equals(machine.boilerQuality()),
                "one serving should boil and 50 mB of Murky wait, got basin " + machine.basinMb() + ", boiler "
                        + machine.boilerMb() + " of " + machine.boilerQuality());
        helper.succeed();
    }

    @GameTest
    public void aMoveNotCommittedChangesNothing(GameTestHelper helper) {
        DistillerBlockEntity machine = machine(helper);
        Level level = helper.getLevel();
        BlockPos boiler = helper.absolutePos(BOILER);
        DistillerFluids.fillWater(level, boiler, MURKY, 500, true);
        distil(machine, 1);
        long before = machine.tanks();
        TestFixtures.check(helper, DistillerFluids.fillWater(level, boiler, PURE, 100, false) == 100,
                "a simulated fill should say 100 would go in");
        TestFixtures.check(helper, DistillerFluids.drain(level, boiler, 100, false).amount() == 100,
                "a simulated drain should say 100 would come out");
        TestFixtures.check(helper, machine.tanks() == before && MURKY.equals(machine.boilerQuality()),
                "neither should have changed the tanks");
        helper.succeed();
    }

    @GameTest
    public void eitherHalfIsPipedWhereTheLoaderAllows(GameTestHelper helper) {
        DistillerBlockEntity machine = machine(helper);
        Level level = helper.getLevel();
        BlockPos tub = helper.absolutePos(TUB);
        TestFixtures.check(helper, DistillerFluids.has(level, helper.absolutePos(BOILER)), "the boiler half should be piped");
        TestFixtures.check(helper, DistillerFluids.has(level, tub) == DistillerFluids.bothHalves(),
                "the tub half should be piped " + (DistillerFluids.bothHalves() ? "here" : "only where the loader allows"));
        if (DistillerFluids.bothHalves()) {
            DistillerFluids.fillWater(level, tub, MURKY, 250, true);
            TestFixtures.check(helper, machine.boilerMb() == 250, "water through the tub half should reach the boiler");
        }
        helper.succeed();
    }

    @GameTest
    public void anUnpipedBoilerIsNotAFluidBlock(GameTestHelper helper) {
        BlockState boiler = ((DistillerBlock) ThirstBlocks.COPPER_DISTILLER).unpiped(Direction.NORTH);
        helper.setBlock(BOILER, boiler);
        TestFixtures.check(helper, !DistillerFluids.has(helper.getLevel(), helper.absolutePos(BOILER)),
                "a boiler not yet joined to its tub has no tanks to pipe");
        helper.succeed();
    }

    private static DistillerBlockEntity machine(GameTestHelper helper) {
        BlockState boiler = ThirstBlocks.COPPER_DISTILLER.defaultBlockState().setValue(DistillerBlock.FACING, Direction.NORTH);
        helper.setBlock(BOILER, boiler);
        helper.setBlock(TUB, boiler.setValue(DistillerBlock.PART, DistillerBlock.Part.TUB).setValue(DistillerBlock.COOLED, true));
        return (DistillerBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(BOILER));
    }

    /** Ticks long enough for {@code servings} to boil, on coal of its own. */
    private static void distil(DistillerBlockEntity machine, int servings) {
        machine.setItem(DistillerBlockEntity.FUEL, new ItemStack(Items.COAL, 2));
        for (int tick = 0; tick < servings * DistillerBlockEntity.servingTicks(); tick++) machine.tick();
    }
}
