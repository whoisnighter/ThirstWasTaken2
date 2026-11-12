package com.thirstwastaken2.littlelogisticsforge.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.thirstwastaken2.forge.WaterQualityFluidHandler;
import com.thirstwastaken2.littlelogisticsforge.LittleLogisticsForgeWater;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Inside a click on a Little Logistics tank (the car, the barge, the Fluid Hopper), and only there, a
 * container's handler carries the grade across Forge's bucket wrapper. Every {@code FluidUtil} helper
 * finds the container's handler through this one method.
 */
@Mixin(value = FluidUtil.class, remap = false)
abstract class ForgeFluidUtilMixin {
    @ModifyReturnValue(method = "getFluidHandler(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraftforge/common/util/LazyOptional;", at = @At("RETURN"))
    private static LazyOptional<IFluidHandlerItem> thirst$graded(LazyOptional<IFluidHandlerItem> handler, ItemStack container) {
        if (!LittleLogisticsForgeWater.inTransfer()) return handler;
        return handler.lazyMap(WaterQualityFluidHandler::of);
    }
}
