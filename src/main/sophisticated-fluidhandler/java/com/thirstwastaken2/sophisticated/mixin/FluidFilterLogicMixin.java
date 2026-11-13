package com.thirstwastaken2.sophisticated.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thirstwastaken2.neoforge.WaterFluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.FluidFilterLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A pump filter set to water means any water. The filter compares components, and a filter is set
 * from whatever container the player clicked, so without this a filter made from a plain bucket would
 * refuse every graded stack the integration now puts in the tanks, and one made from a graded bucket
 * would take only that grade.
 */
@Mixin(value = FluidFilterLogic.class, remap = false)
abstract class FluidFilterLogicMixin {
    @WrapOperation(method = "matchesFluidFilter", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/fluids/FluidStack;isSameFluidSameComponents(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/FluidStack;)Z"))
    private boolean thirst$anyGradeOfWater(FluidStack filter, FluidStack fluid, Operation<Boolean> original) {
        if (WaterFluids.isWater(filter) && WaterFluids.isWater(fluid)) {
            return original.call(WaterFluids.unstamped(filter), WaterFluids.unstamped(fluid));
        }
        return original.call(filter, fluid);
    }
}
