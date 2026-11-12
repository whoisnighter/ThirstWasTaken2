package com.thirstwastaken2.littlelogistics.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thirstwastaken2.littlelogistics.LittleLogisticsWater;
import dev.murad.shipping.entity.custom.vessel.barge.FluidTankBargeEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The Fluid Barge's click, the same as the Fluid Tank Car's; see {@link FluidTankCarEntityMixin}. */
@Mixin(FluidTankBargeEntity.class)
abstract class FluidTankBargeEntityMixin {
    @WrapOperation(method = "doInteract", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/fluids/FluidUtil;interactWithFluidHandler(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;)Z"))
    private boolean thirst$graded(Player player, InteractionHand hand, IFluidHandler tank, Operation<Boolean> original) {
        return LittleLogisticsWater.graded(() -> original.call(player, hand, tank));
    }
}
