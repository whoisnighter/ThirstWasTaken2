package com.thirstwastaken2.littlelogisticsforge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thirstwastaken2.littlelogisticsforge.LittleLogisticsForgeWater;
import dev.murad.shipping.entity.custom.train.wagon.FluidTankCarEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A bucket or other container clicked on a Fluid Tank Car keeps the grade of the water it draws or pours.
 * The car hands the click to Forge's {@code FluidUtil}, whose bucket wrapper gives and takes plain water,
 * so a bucket drawn from a car of Murky water came out Clean (issue #52). Inside the click, and only
 * there, {@link ForgeFluidUtilMixin} carries the grade across. {@code interact} is Minecraft's, remapped
 * through the refmap; the call is Forge's, which is not.
 */
@Mixin(FluidTankCarEntity.class)
abstract class FluidTankCarEntityMixin {
    @WrapOperation(method = "interact", at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraftforge/fluids/FluidUtil;interactWithFluidHandler(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraftforge/fluids/capability/IFluidHandler;)Z"))
    private boolean thirst$graded(Player player, InteractionHand hand, IFluidHandler tank, Operation<Boolean> original) {
        return LittleLogisticsForgeWater.graded(() -> original.call(player, hand, tank));
    }
}
