package com.thirstwastaken2.sophisticated.mixin;

import com.thirstwastaken2.item.WaterskinItem;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * A canteen or flask boiled by a Smelting, Smoking or Blasting upgrade, or their Auto versions, keeps
 * its servings. Core's {@code CookingLogic} long took its result straight from the recipe
 * ({@code getResultItem} on 1.21.1 up to Core 1.5.18, the recipe's {@code result} on 26.x up to at least
 * 1.5.19) rather than assembling one, so the mod's {@code CookingRecipeMixin} never ran and the vessel
 * came out holding nothing (issue #53). Newer Cores assemble, and there this does nothing, since the
 * result already has its servings. Hooked where the result goes into the output slot, which every Core
 * does through {@code setCookOutput} with a copy of its own, so changing it in place is safe.
 *
 * <p>Like every hook in this config it is {@code require = 0}, and the plugin skips the mixin when
 * {@code smelt} or the shadowed {@code getCookInput} is gone, so a change in Core turns this off in the
 * log rather than stopping the game.
 */
@Mixin(value = CookingLogic.class, remap = false)
abstract class CookingLogicMixin {
    @Shadow
    public abstract ItemStack getCookInput();

    @ModifyArg(method = "smelt", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/p3pp3rf1y/sophisticatedcore/upgrades/cooking/CookingLogic;setCookOutput(Lnet/minecraft/world/item/ItemStack;)V"))
    private ItemStack thirst$keepServings(ItemStack output) {
        WaterskinItem.keepServings(getCookInput(), output);
        return output;
    }
}
