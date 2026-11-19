package com.thirstwastaken2.cookingforblockheads.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.thirstwastaken2.cookingforblockheads.SinkWater;
//? if >=1.20.5 {
import net.blay09.mods.cookingforblockheads.block.entity.SinkBlockEntity;
//?} else {
/*import net.blay09.mods.cookingforblockheads.tile.SinkBlockEntity;
*///?}
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Sink's tank, an anonymous subclass of Balm's inside the Sink's block entity. Every fill reaches it,
 * from a pipe through Balm's loader wrapper or from a click, so this is where sea water is refused and
 * the grade of what went in is mixed into the Sink's. See {@link SinkWater}.
 *
 * <p>The amount is compared before and after rather than trusting the return value: the endless tap
 * answers that it took everything and keeps nothing, and only water really kept changes the grade. Only
 * the signature forks: the tank gained a slot index in 26.2, and its block entity was in another package
 * before 1.20.5. The mod's own methods, so not remapped; Fluid is a class reference, which is.
 *
 * <p>The block entity is taken from the constructor, which every version declares as (block entity,
 * capacity), rather than shadowing the synthetic {@code this$0}: Forge's annotation processor crashes
 * validating a shadow of that field.
 */
//? if >=1.20.5 {
@Mixin(targets = "net.blay09.mods.cookingforblockheads.block.entity.SinkBlockEntity$1", remap = false)
//?} else {
/*@Mixin(targets = "net.blay09.mods.cookingforblockheads.tile.SinkBlockEntity$1", remap = false)
*///?}
abstract class SinkTankMixin {
    @Unique
    private BlockEntity thirst$sink;

    @Inject(method = "<init>", require = 0, at = @At("RETURN"))
    private void thirst$keepSink(SinkBlockEntity sink, int capacity, CallbackInfo ci) {
        thirst$sink = sink;
    }

    //? if >=26.2 {
    @Shadow
    public abstract int getAmount(int slot);

    @Inject(method = "fill", require = 0, at = @At("HEAD"), cancellable = true)
    private void thirst$refuseSeaWater(int slot, Fluid fluid, int amount, boolean simulate, CallbackInfoReturnable<Integer> cir,
                                       @Share("held") LocalIntRef held) {
        if (SinkWater.refusesIncoming(thirst$sink)) cir.setReturnValue(0); else held.set(getAmount(slot));
    }

    @Inject(method = "fill", require = 0, at = @At("RETURN"))
    private void thirst$gradeFilled(int slot, Fluid fluid, int amount, boolean simulate, CallbackInfoReturnable<Integer> cir,
                                    @Share("held") LocalIntRef held) {
        if (!simulate) SinkWater.filled(thirst$sink, held.get(), getAmount(slot) - held.get());
    }
    //?} else {
    /*@Shadow
    public abstract int getAmount();

    @Inject(method = "fill", require = 0, at = @At("HEAD"), cancellable = true)
    private void thirst$refuseSeaWater(Fluid fluid, int amount, boolean simulate, CallbackInfoReturnable<Integer> cir,
                                       @Share("held") LocalIntRef held) {
        if (SinkWater.refusesIncoming(thirst$sink)) cir.setReturnValue(0); else held.set(getAmount());
    }

    @Inject(method = "fill", require = 0, at = @At("RETURN"))
    private void thirst$gradeFilled(Fluid fluid, int amount, boolean simulate, CallbackInfoReturnable<Integer> cir,
                                    @Share("held") LocalIntRef held) {
        if (!simulate) SinkWater.filled(thirst$sink, held.get(), getAmount() - held.get());
    }
    *///?}
}
