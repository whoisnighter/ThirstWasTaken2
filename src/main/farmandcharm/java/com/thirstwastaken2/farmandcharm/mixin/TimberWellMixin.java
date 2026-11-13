package com.thirstwastaken2.farmandcharm.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.thirstwastaken2.farmandcharm.StandingWater;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.satisfy.farm_and_charm.core.block.TimberWellBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A bucket drawn from a Timber Well carries the grade of the groundwater the well pumps, or of the rain
 * that filled it. Unstamped, it read as Clean wherever the well stood, sea water under a beach included,
 * and one source block under a well is endless water.
 *
 * <p>Wraps the one call that makes the filled bucket, which runs only on the server.
 *
 * <p>Every injector is {@code require = 0}, and the plugin checks {@code useItemOn} is there and warns for
 * a hook that found nothing, so a change upstream turns this off in the log rather than stopping the game
 * (see {@code FarmAndCharmMixinPlugin}).
 */
@Mixin(TimberWellBlock.class)
abstract class TimberWellMixin {
    @WrapOperation(method = "useItemOn", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemUtils;createFilledResult(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack thirst$gradeWellWater(ItemStack empty, Player player, ItemStack filled, Operation<ItemStack> original,
                                            @Local(argsOnly = true) BlockState state, @Local(argsOnly = true) Level level,
                                            @Local(argsOnly = true) BlockPos pos) {
        WaterQuality quality = StandingWater.wellWater(level, pos, state);
        if (quality != null) WaterPurity.setQuality(filled, quality);
        return original.call(empty, player, filled);
    }
}
