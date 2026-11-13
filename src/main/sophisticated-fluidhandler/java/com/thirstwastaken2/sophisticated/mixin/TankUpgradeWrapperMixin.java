package com.thirstwastaken2.sophisticated.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thirstwastaken2.neoforge.WaterQualityFluidHandler;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankUpgradeWrapper;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;

/**
 * Every container the Tank upgrade fills or drains, from its two slots or from the cursor, is found
 * through this one method, so wrapping it is enough to keep water's grade on both sides.
 *
 * <p>Core up to 1.21.1-1.5.33 only. From 1.5.35 the method is in {@code TankContainerHandler}, which
 * {@link TankContainerHandlerMixin} wraps, and the plugin leaves this one out.
 */
@Mixin(value = TankUpgradeWrapper.class, remap = false)
abstract class TankUpgradeWrapperMixin {
    @WrapMethod(method = "getFluidHandler", require = 0)
    private Optional<IFluidHandlerItem> thirst$keepWaterQuality(ItemStack stack,
                                                                Operation<Optional<IFluidHandlerItem>> original) {
        return WaterQualityFluidHandler.wrap(stack, original::call);
    }
}
