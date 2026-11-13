package com.thirstwastaken2.sophisticated.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thirstwastaken2.data.ThirstManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The Feeding upgrade eats through {@code Item.finishUsingItem} directly, past
 * {@code ItemStack.finishUsingItem}, where the mod hands out thirst for everything eaten or drunk. So
 * food fed from a backpack filled the hunger bar and never the thirst bar. This does what the mod's own
 * hook does, at the same point: before the item is used up, so it is read as it was eaten.
 */
@Mixin(value = FeedingUpgradeWrapper.class, remap = false)
abstract class FeedingUpgradeWrapperMixin {
    @WrapOperation(method = "tryFeedingStack", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/Item;finishUsingItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack thirst$quenchFedItem(Item item, ItemStack stack, Level level, LivingEntity entity,
                                           Operation<ItemStack> original) {
        if (!level.isClientSide() && entity instanceof Player player) ThirstManager.drinkItem(player, stack);
        return original.call(item, stack, level, entity);
    }
}
