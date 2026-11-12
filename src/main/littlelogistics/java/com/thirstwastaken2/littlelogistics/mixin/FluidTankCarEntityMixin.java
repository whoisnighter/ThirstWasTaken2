package com.thirstwastaken2.littlelogistics.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thirstwastaken2.littlelogistics.LittleLogisticsWater;
import dev.murad.shipping.entity.custom.train.wagon.FluidTankCarEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A bucket or other container clicked on a Fluid Tank Car keeps the grade of the water it draws or pours.
 * The car hands the click to NeoForge's {@code FluidUtil}, whose bucket wrapper gives and takes plain
 * water, so a bucket drawn from a car of Murky water came out Clean (issue #52). Inside the click, and only
 * there, {@link NeoForgeFluidUtilMixin} carries the grade across.
 */
@Mixin(FluidTankCarEntity.class)
abstract class FluidTankCarEntityMixin {
    @WrapOperation(method = "interact", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/fluids/FluidUtil;interactWithFluidHandler(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;)Z"))
    private boolean thirst$graded(Player player, InteractionHand hand, IFluidHandler tank, Operation<Boolean> original) {
        return LittleLogisticsWater.graded(() -> original.call(player, hand, tank));
    }
}
