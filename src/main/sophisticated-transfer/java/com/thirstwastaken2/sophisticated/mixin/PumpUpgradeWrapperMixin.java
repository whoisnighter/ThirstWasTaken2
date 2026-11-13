package com.thirstwastaken2.sophisticated.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.thirstwastaken2.neoforge.SampledWater;
import com.thirstwastaken2.purity.WaterQuality;
import com.thirstwastaken2.sophisticated.CollectedWaterStorage;
import com.thirstwastaken2.sophisticated.WaterQualityResourceHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Function;

/**
 * Water quality through the Pump upgrade, which moves fluid between the backpack's tanks and the world,
 * the containers in nearby players' hands, and the fluid handlers of neighbouring blocks.
 *
 * <p>Pumping out needs nothing here, unlike on 1.21.1: this generation asks the tanks for the resource
 * they hold, components and all, so stamped water leaves them as it is.
 */
@Mixin(value = PumpUpgradeWrapper.class, remap = false)
abstract class PumpUpgradeWrapperMixin {
    @Unique
    private final SampledWater thirst$source = new SampledWater();

    /**
     * Water collected from the world, graded where it lies. The pump searches every block in range and
     * tries each until one transfer works, so a sample is only taken at a source block, and only once
     * the tanks have room for water at all: a full backpack by a lake would otherwise sample the whole
     * lake every few seconds.
     */
    @ModifyVariable(method = "fillFromBlock", require = 0, at = @At("HEAD"), argsOnly = true)
    private ResourceHandler<FluidResource> thirst$stampCollected(ResourceHandler<FluidResource> storage,
                                                               @Local(argsOnly = true) Level level,
                                                               @Local(argsOnly = true) BlockPos pos) {
        if (!level.getFluidState(pos).isSource() || !CollectedWaterStorage.hasRoomForWater(storage)) return storage;
        WaterQuality quality = thirst$source.at(level, pos);
        return quality == null ? storage : new CollectedWaterStorage(storage, quality);
    }

    /**
     * A bucket in hand, poured into the backpack or filled from it. The pump looks its handler up on the
     * container itself, not through the Tank upgrade's lookup, so the lookup is handed the unstamped view
     * and the handler it finds comes back wrapped.
     */
    @WrapOperation(method = "handleFluidContainerInHand", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/p3pp3rf1y/sophisticatedcore/util/CapabilityHelper;getFromFluidHandler(Lnet/neoforged/neoforge/transfer/access/ItemAccess;Ljava/util/function/Function;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object thirst$keepQualityInHand(ItemAccess access, Function<ResourceHandler<FluidResource>, Object> get,
                                            Object defaultValue, Operation<Object> original) {
        ItemAccess view = WaterQualityResourceHandler.viewFor(access);
        Function<ResourceHandler<FluidResource>, Object> wrapped =
                handler -> get.apply(WaterQualityResourceHandler.of(handler, view));
        return original.call(view, wrapped, defaultValue);
    }
}
