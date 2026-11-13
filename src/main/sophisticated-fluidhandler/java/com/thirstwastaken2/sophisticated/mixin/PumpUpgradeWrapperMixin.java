package com.thirstwastaken2.sophisticated.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.thirstwastaken2.neoforge.SampledWater;
import com.thirstwastaken2.neoforge.WaterFluids;
import com.thirstwastaken2.purity.WaterQuality;
import com.thirstwastaken2.sophisticated.StampedWaterSource;
import com.thirstwastaken2.neoforge.WaterQualityFluidHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Water quality through the Pump upgrade, which moves fluid between the backpack's tanks and the world,
 * the containers in nearby players' hands, and the fluid handlers of neighbouring blocks.
 */
@Mixin(value = PumpUpgradeWrapper.class, remap = false)
abstract class PumpUpgradeWrapperMixin {
    @Unique
    private final SampledWater thirst$source = new SampledWater();

    /**
     * Water collected from the world, graded where it lies. The pump searches every source in range
     * and tries each until one transfer works, so a sample is only taken once the tanks have room for
     * water at all: a full backpack by a lake would otherwise sample the whole lake every few seconds.
     */
    @WrapOperation(method = "fillFromBlock", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/p3pp3rf1y/sophisticatedcore/upgrades/pump/PumpUpgradeWrapper;fillFromFluidHandler(Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;)Z"))
    private boolean thirst$stampCollected(PumpUpgradeWrapper pump, IFluidHandler source, IFluidHandler storage,
                                         Operation<Boolean> original,
                                         @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos pos) {
        WaterQuality quality = thirst$hasRoomForWater(storage) ? thirst$source.at(level, pos) : null;
        return original.call(pump, quality == null ? source : new StampedWaterSource(source, quality), storage);
    }

    /** A bucket in hand poured into the backpack. */
    @WrapMethod(method = "fillFromHand", require = 0)
    private boolean thirst$keepQualityFromHand(Player player, InteractionHand hand, IFluidHandlerItem container,
                                              IFluidHandler storage, Operation<Boolean> original) {
        return original.call(player, hand, WaterQualityFluidHandler.of(container), storage);
    }

    /** A bucket in hand filled from the backpack. */
    @WrapMethod(method = "fillContainerInHand", require = 0)
    private boolean thirst$keepQualityIntoHand(Player player, InteractionHand hand, IFluidHandlerItem container,
                                              IFluidHandler storage, Operation<Boolean> original) {
        return original.call(player, hand, WaterQualityFluidHandler.of(container), storage);
    }

    /**
     * Pumping out asks the backpack for a fresh stack of the tank's fluid, without its components, and
     * the backpack only hands out fluid whose components match. Stamped water would never leave, so the
     * request is made of the water actually in the tank.
     */
    @WrapOperation(method = "fillFluidHandler(Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;I)Z", require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/fluids/FluidUtil;tryFluidTransfer(Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;Lnet/neoforged/neoforge/fluids/FluidStack;Z)Lnet/neoforged/neoforge/fluids/FluidStack;"))
    private FluidStack thirst$requestStoredWater(IFluidHandler destination, IFluidHandler storage, FluidStack request,
                                                 boolean transfer, Operation<FluidStack> original,
                                                 @Local FluidStack tankFluid) {
        FluidStack asStored = WaterFluids.isWater(tankFluid) ? tankFluid.copyWithAmount(request.getAmount()) : request;
        return original.call(destination, storage, asStored, transfer);
    }

    @Unique
    private static boolean thirst$hasRoomForWater(IFluidHandler storage) {
        for (int tank = 0; tank < storage.getTanks(); tank++) {
            FluidStack held = storage.getFluidInTank(tank);
            if (held.isEmpty() || WaterFluids.isWater(held) && held.getAmount() < storage.getTankCapacity(tank)) {
                return true;
            }
        }
        return false;
    }
}
