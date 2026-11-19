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
 * Balm's transfer API wrapper around a tank, what a pipe fills a Sink through on NeoForge 26.1 and later.
 * As {@link BalmFluidStorageMixin} on Fabric: the fluid resource, which carries the grade, is noted for
 * the length of the call, for the Sink's tank to read ({@link SinkWater#piping}).
 *
 * <p>The resource and the transaction are taken as {@code Object} ({@code @Coerce}), so this class, which
 * every loader compiles, names no loader. Balm's and NeoForge's names, so not remapped.
 */
@Mixin(targets = "net.blay09.mods.balm.neoforge.platform.compatibility.fluid.internal.NeoForgeFluidTank", remap = false)
abstract class BalmResourceHandlerMixin {
    @Inject(method = "insert", require = 0, at = @At("HEAD"))
    private void thirst$piping(int slot, @Coerce Object resource, int amount, @Coerce Object transaction,
                               CallbackInfoReturnable<Integer> cir, @Share("previous") LocalRef<Object> previous) {
        previous.set(SinkWater.piping(resource));
    }

    @Inject(method = "insert", require = 0, at = @At("RETURN"))
    private void thirst$piped(int slot, @Coerce Object resource, int amount, @Coerce Object transaction,
                              CallbackInfoReturnable<Integer> cir, @Share("previous") LocalRef<Object> previous) {
        SinkWater.piped(previous.get());
    }
}
