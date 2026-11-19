package com.thirstwastaken2.cookingforblockheads.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.thirstwastaken2.cookingforblockheads.SinkWater;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Balm's Fabric wrapper around a tank, what a pipe fills a Sink through on Fabric. Balm hands the tank only
 * the fluid, so the variant, which carries the grade, is noted for the length of the call; the Sink's tank
 * reads it if the water lands there ({@link SinkWater#piping}). Every other Balm tank ignores it.
 *
 * <p>The variant and the transaction are taken as {@code Object} ({@code @Coerce}), so this class, which
 * every loader compiles, names no loader. Its package moved in 1.21.2's Balm, and 26.2's split it into a
 * slotted storage over single ones, which may call each other; the plugin applies this only to the
 * targets that exist. Balm's and Fabric API's names, so not remapped.
 */
@Mixin(targets = {
        "net.blay09.mods.balm.fabric.fluid.BalmFluidStorage",
        "net.blay09.mods.balm.fabric.platform.fluid.internal.BalmFluidStorage",
        "net.blay09.mods.balm.fabric.platform.fluid.internal.BalmSingleFluidStorage"}, remap = false)
abstract class BalmFluidStorageMixin {
    @Inject(method = "insert", require = 0, at = @At("HEAD"))
    private void thirst$piping(@Coerce Object variant, long amount, @Coerce Object transaction, CallbackInfoReturnable<Long> cir,
                               @Share("previous") LocalRef<Object> previous) {
        previous.set(SinkWater.piping(variant));
    }

    @Inject(method = "insert", require = 0, at = @At("RETURN"))
    private void thirst$piped(@Coerce Object variant, long amount, @Coerce Object transaction, CallbackInfoReturnable<Long> cir,
                              @Share("previous") LocalRef<Object> previous) {
        SinkWater.piped(previous.get());
    }
}
