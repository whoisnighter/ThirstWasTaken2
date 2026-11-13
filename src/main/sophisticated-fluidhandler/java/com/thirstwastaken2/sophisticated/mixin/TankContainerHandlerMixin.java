package com.thirstwastaken2.sophisticated.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thirstwastaken2.neoforge.WaterQualityFluidHandler;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankContainerHandler;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;

/**
 * The Tank's container code from Core 1.21.1-1.5.35, where it moved out of {@code TankUpgradeWrapper} into
 * a class of its own that Sophisticated Storage's tank block shares (issue #61). Every container the tank
 * fills or drains, from its two slots or from the cursor, is still found through this one method, so
 * wrapping it keeps water's grade on both sides, as {@link TankUpgradeWrapperMixin} does on older Cores.
 * The plugin applies exactly one of the two.
 */
@Mixin(value = TankContainerHandler.class, remap = false)
abstract class TankContainerHandlerMixin {
    @WrapMethod(method = "getFluidHandler", require = 0)
    private Optional<IFluidHandlerItem> thirst$keepWaterQuality(ItemStack stack,
                                                                Operation<Optional<IFluidHandlerItem>> original) {
        return WaterQualityFluidHandler.wrap(stack, original::call);
    }
}
