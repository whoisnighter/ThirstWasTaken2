package com.thirstwastaken2.farmandcharm.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.satisfy.farm_and_charm.core.block.entity.CookingPotBlockEntity;
import net.satisfy.farm_and_charm.core.block.entity.CraftingBowlBlockEntity;
import net.satisfy.farm_and_charm.core.block.entity.StoveBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Sea water is not the water a Farm & Charm recipe asks for, as Brewin' and Chewin's keg, Cultural
 * Delights' vat and Fruits Delight make nothing from it. The recipes take {@code #c:water_bottles},
 * which holds the water bucket, matched by item alone, so a bucket of sea water made nettle tea.
 *
 * <p>Each block finds its recipe with a matcher of its own, testing each ingredient against each slot:
 * the Cooking Pot, the Stove (and Candlelight's, which extend it), the Crafting Bowl, and Candlelight's
 * Large Cooking Pot up to 2.1.13, named by string since Candlelight is not compiled against. From 2.1.14
 * that pot extends the Cooking Pot, whose target covers it, and the plugin skips it, as it does when
 * Candlelight is absent. Only water containers carry salt, so a salty stack matching nothing is the same
 * as sea water not matching water. Fresh water of any grade still matches: the pot and stove are heated,
 * and what they make is its own item, as tea is.
 */
@Mixin(value = {CookingPotBlockEntity.class, StoveBlockEntity.class, CraftingBowlBlockEntity.class},
        targets = "net.satisfy.candlelight.core.block.entity.LargeCookingPotBlockEntity", remap = false)
abstract class SeaWaterIngredientMixin {
    // Minecraft's own method, so remapped on Fabric, whose Farm & Charm jar is in intermediary. Each
    // target has at least one of these methods; the plugin checks that before applying. require = 0 on
    // top: a matcher that changes shape upstream turns this off for that block instead of stopping the
    // game, as a Candlelight pot that had lost its matcher once did (issue #56).
    @WrapOperation(method = {"matchesInventory", "findBestMatchingSlot", "countMatchingSlots", "matchExact"}, require = 0,
            at = @At(value = "INVOKE", remap = true,
                    target = "Lnet/minecraft/world/item/crafting/Ingredient;test(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean thirst$seaWaterIsNotWater(Ingredient ingredient, ItemStack stack, Operation<Boolean> original) {
        return !WaterPurity.isSalty(stack) && original.call(ingredient, stack);
    }
}
