package com.thirstwastaken2.farmandcharm.mixin;

import com.thirstwastaken2.farmandcharm.StandingWater;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.satisfy.farm_and_charm.core.block.WaterTroughBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A Water Trough holds only a level, so whatever was poured in came back out Clean, and the middle of a
 * line of three or more is endless. Now what is drawn from it is {@link StandingWater#STANDING}, Murky,
 * whatever went in: nothing gets better by passing through a trough. Sea water is refused, since it
 * would come out fresh.
 *
 * <p>Every injector is {@code require = 0}, and the plugin checks {@code useItemOn} is there and warns for
 * a hook that found nothing, so a change upstream turns this off in the log rather than stopping the game
 * (see {@code FarmAndCharmMixinPlugin}).
 */
@Mixin(WaterTroughBlock.class)
abstract class WaterTroughMixin {
    @Inject(method = "useItemOn", require = 0, at = @At("HEAD"), cancellable = true)
    private void thirst$noSeaWater(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                   InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (StandingWater.isSaltyBucket(stack)) cir.setReturnValue(ItemInteractionResult.CONSUME);
    }

    // Both branches hand their result through setItemInHand: the empty bucket after pouring, which is no
    // water container and is left alone, and the water bucket drawn.
    @ModifyArg(method = "useItemOn", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;setItemInHand(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V"),
            index = 1)
    private ItemStack thirst$standingWater(ItemStack drawn) {
        return StandingWater.standing(drawn);
    }
}
