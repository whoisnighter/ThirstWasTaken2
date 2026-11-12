package com.thirstwastaken2.fabric.mixin;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * A canteen or flask boiled in a furnace keeps its servings, before 1.21.2 on Fabric. There vanilla's
 * furnace fills its output from the recipe's own result ({@code getResultItem}) rather than assembling
 * one from the input, so {@code CookingRecipeMixin} in core never runs and the vessel came out holding
 * nothing (issue #55). Forge and NeoForge patch the furnace to assemble, and from 1.21.2 vanilla does
 * too, so this is Fabric code and empty from 1.21.2. Hooked in {@code burn} alone: {@code canBurn} only
 * compares the result with the output slot, which a vessel, stacking to one, never shares. 1.20.5 gave
 * {@code getResultItem} a registry lookup in place of the registry access.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
abstract class FurnaceMixin {
    //? if <1.20.5 {
    /*@com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "burn", at = @org.spongepowered.asm.mixin.injection.At(
            value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/Recipe;getResultItem(Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;"))
    private static net.minecraft.world.item.ItemStack thirst$keepServings(net.minecraft.world.item.ItemStack result,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> items) {
        return com.thirstwastaken2.item.WaterskinItem.withServingsOf(items.get(0), result);
    }
    *///?} elif <1.21.2 {
    /*@com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "burn", at = @org.spongepowered.asm.mixin.injection.At(
            value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/Recipe;getResultItem(Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;"))
    private static net.minecraft.world.item.ItemStack thirst$keepServings(net.minecraft.world.item.ItemStack result,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> items) {
        return com.thirstwastaken2.item.WaterskinItem.withServingsOf(items.get(0), result);
    }
    *///?}
}
