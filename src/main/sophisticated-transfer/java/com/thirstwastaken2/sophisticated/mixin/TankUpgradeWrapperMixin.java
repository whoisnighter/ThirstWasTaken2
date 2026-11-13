package com.thirstwastaken2.sophisticated.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thirstwastaken2.sophisticated.WaterQualityResourceHandler;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankUpgradeWrapper;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;

/**
 * Every container the Tank upgrade fills or drains, from its two slots or from the cursor, is found
 * through this one method, so wrapping it is enough to keep water's grade on both sides.
 *
 * <p>Core up to 26.1.2-1.5.34 and 26.2-1.5.32 only. Later the method is in {@code TankContainerHandler}, which
 * {@link TankContainerHandlerMixin} wraps, and the plugin leaves this one out.
 */
@Mixin(value = TankUpgradeWrapper.class, remap = false)
abstract class TankUpgradeWrapperMixin {
    @WrapMethod(method = "getFluidHandler", require = 0)
    private Optional<ResourceHandler<FluidResource>> thirst$keepWaterQuality(ItemStack stack, ItemAccess access,
            Operation<Optional<ResourceHandler<FluidResource>>> original) {
        return WaterQualityResourceHandler.wrap(stack, access, original::call);
    }
}
