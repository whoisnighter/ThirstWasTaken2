package com.thirstwastaken2.littlelogistics.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.thirstwastaken2.littlelogistics.LittleLogisticsWater;
import com.thirstwastaken2.neoforge.WaterQualityFluidHandler;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

/**
 * Inside a click on a Little Logistics tank ({@link FluidTankCarEntityMixin}, {@link FluidTankBargeEntityMixin}),
 * and only there, a container's handler carries the grade across NeoForge's bucket wrapper. Every
 * {@code FluidUtil} helper finds the container's handler through this one method.
 */
@Mixin(FluidUtil.class)
abstract class NeoForgeFluidUtilMixin {
    @ModifyReturnValue(method = "getFluidHandler(Lnet/minecraft/world/item/ItemStack;)Ljava/util/Optional;", at = @At("RETURN"))
    private static Optional<IFluidHandlerItem> thirst$graded(Optional<IFluidHandlerItem> handler, ItemStack container) {
        if (!LittleLogisticsWater.inTransfer()) return handler;
        return handler.map(WaterQualityFluidHandler::of);
    }
}
