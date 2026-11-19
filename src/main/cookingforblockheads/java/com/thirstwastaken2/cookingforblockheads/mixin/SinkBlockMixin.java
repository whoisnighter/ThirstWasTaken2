package com.thirstwastaken2.cookingforblockheads.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.thirstwastaken2.cookingforblockheads.SinkWater;
import net.blay09.mods.cookingforblockheads.block.SinkBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cooking for Blockheads' Sink: sea water is refused, and what is drawn is graded, Murky from the endless
 * tap and the tank's own grade from a tank. The click also tells the tank the grade of water poured in.
 * See {@link SinkWater} for why, and for how what was drawn is found.
 *
 * <p>Refusing spends the click, so a salty bucket is neither poured in nor passed on to anything else.
 * Both injectors are {@code require = 0}, and the plugin checks the method is there, so a change upstream
 * turns this off in the log rather than stopping the game. Only the signature forks: the Sink's click is
 * {@code use} on 1.20.1, and returns an {@code ItemInteractionResult} on 1.21.1. From 1.21.2 the refusal is
 * the plain {@code CONSUME} (no arm swing). The 1.20.1 branch is written with {@code SUCCESS_SERVER},
 * which the replacement in stonecutter.gradle.kts turns into that version's {@code CONSUME}; the newer
 * branch must not use that spelling, or the rule would rewrite it too.
 */
@Mixin(SinkBlock.class)
abstract class SinkBlockMixin {
    //? if >=1.21.2 {
    @Inject(method = "useItemOn", require = 0, at = @At("HEAD"), cancellable = true)
    private void thirst$refuseSeaWater(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                       BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir,
                                       @Share("before") LocalRef<SinkWater.Click> before) {
        if (SinkWater.refuses(stack)) cir.setReturnValue(InteractionResult.CONSUME);
        else before.set(SinkWater.before(level, level.getBlockEntity(pos), player, stack));
    }

    @Inject(method = "useItemOn", require = 0, at = @At("RETURN"))
    private void thirst$gradeDrawn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                   BlockHitResult hit, CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir,
                                   @Share("before") LocalRef<SinkWater.Click> before) {
        SinkWater.after(before.get(), player);
    }
    //?} elif >=1.20.5 {
    /*@Inject(method = "useItemOn", require = 0, at = @At("HEAD"), cancellable = true)
    private void thirst$refuseSeaWater(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                       BlockHitResult hit, CallbackInfoReturnable<net.minecraft.world.ItemInteractionResult> cir,
                                       @Share("before") LocalRef<SinkWater.Click> before) {
        if (SinkWater.refuses(stack)) cir.setReturnValue(net.minecraft.world.ItemInteractionResult.CONSUME);
        else before.set(SinkWater.before(level, level.getBlockEntity(pos), player, stack));
    }

    @Inject(method = "useItemOn", require = 0, at = @At("RETURN"))
    private void thirst$gradeDrawn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                   BlockHitResult hit, CallbackInfoReturnable<net.minecraft.world.ItemInteractionResult> cir,
                                   @Share("before") LocalRef<SinkWater.Click> before) {
        SinkWater.after(before.get(), player);
    }
    *///?} else {
    /*@Inject(method = "use", require = 0, at = @At("HEAD"), cancellable = true)
    private void thirst$refuseSeaWater(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                       BlockHitResult hit, CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir,
                                       @Share("before") LocalRef<SinkWater.Click> before) {
        if (SinkWater.refuses(player.getItemInHand(hand))) cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
        else before.set(SinkWater.before(level, level.getBlockEntity(pos), player, player.getItemInHand(hand)));
    }

    @Inject(method = "use", require = 0, at = @At("RETURN"))
    private void thirst$gradeDrawn(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                   BlockHitResult hit, CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir,
                                   @Share("before") LocalRef<SinkWater.Click> before) {
        SinkWater.after(before.get(), player);
    }
    *///?}
}
