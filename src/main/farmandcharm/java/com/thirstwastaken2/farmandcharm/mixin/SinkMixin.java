package com.thirstwastaken2.farmandcharm.mixin;

import com.thirstwastaken2.farmandcharm.StandingWater;
import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.satisfy.foundation.block.SinkBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The kitchen sink, which Farm & Charm 1.1.28 moved into Foundation, the library nested in its jar, and
 * which Candlelight registers as its kitchen sinks. Its tap fills it from nothing (from Foundation 1.0.7
 * an empty hand opens the tap and the basin rises a level every three seconds, up to three; a bottle
 * takes one, a bucket all three), and it handed out plain water bottles and buckets, which read as
 * Clean: an endless tap that made purifying pointless. What it gives is now
 * {@link StandingWater#STANDING}, Murky, as a trough's is. It stays an endless tap, as its authors meant,
 * but its water still wants boiling. Sea water poured in, from a bucket or a bottle, is refused, since it
 * would come out fresh.
 *
 * <p>Every injector is {@code require = 0}, and the plugin checks {@code useItemOn} is there and warns for
 * a hook that found nothing, so a change upstream turns this off in the log rather than stopping the game
 * (see {@code FarmAndCharmMixinPlugin}).
 */
@Mixin(SinkBlock.class)
abstract class SinkMixin {
    @Inject(method = "useItemOn", require = 0, at = @At("HEAD"), cancellable = true)
    private void thirst$noSeaWater(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                   InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (WaterPurity.isWaterContainer(stack) && WaterPurity.isSalty(stack)) cir.setReturnValue(ItemInteractionResult.CONSUME);
    }

    // Every result goes to the inventory, or is dropped when it is full: an empty bucket or bottle after
    // pouring, or a washed item, left alone, and the water bucket or bottle drawn.
    @ModifyArg(method = "useItemOn", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private ItemStack thirst$standingWater(ItemStack drawn) {
        return StandingWater.standing(drawn);
    }

    @ModifyArg(method = "useItemOn", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemStack thirst$standingWaterDropped(ItemStack drawn) {
        return StandingWater.standing(drawn);
    }
}
