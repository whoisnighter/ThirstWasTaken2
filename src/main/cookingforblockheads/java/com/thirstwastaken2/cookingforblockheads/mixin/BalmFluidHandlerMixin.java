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
 * Balm's {@code IFluidHandler} wrapper around a tank, what a pipe fills a Sink through on NeoForge 1.21.1
 * and Forge 1.20.1. As {@link BalmFluidStorageMixin} on Fabric: the fluid stack, which carries the grade,
 * is noted for the length of the call, for the Sink's tank to read ({@link SinkWater#piping}).
 *
 * <p>The stack and the action are taken as {@code Object} ({@code @Coerce}), so this class, which every
 * loader compiles, names no loader. The plugin applies it only to the target that exists. Balm's and the
 * loader's names, so not remapped.
 */
@Mixin(targets = {
        "net.blay09.mods.balm.neoforge.fluid.NeoForgeFluidTank",
        "net.blay09.mods.balm.forge.fluid.ForgeFluidTank"}, remap = false)
abstract class BalmFluidHandlerMixin {
    @Inject(method = "fill", require = 0, at = @At("HEAD"))
    private void thirst$piping(@Coerce Object stack, @Coerce Object action, CallbackInfoReturnable<Integer> cir,
                               @Share("previous") LocalRef<Object> previous) {
        previous.set(SinkWater.piping(stack));
    }

    @Inject(method = "fill", require = 0, at = @At("RETURN"))
    private void thirst$piped(@Coerce Object stack, @Coerce Object action, CallbackInfoReturnable<Integer> cir,
                              @Share("previous") LocalRef<Object> previous) {
        SinkWater.piped(previous.get());
    }
}
