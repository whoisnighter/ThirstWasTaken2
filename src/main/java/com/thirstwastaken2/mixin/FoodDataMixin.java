package com.thirstwastaken2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thirstwastaken2.data.HealthRegen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Natural healing from food, through {@code HealthRegen}: dehydration and Upset Stomach stop it, the food
 * cost vanilla would have charged for a skipped heal is refunded so hunger is not silently drained, and
 * a heal that goes through gets the water reserve's bonus. As the original MixinFoodData, without its
 * slow heal for a nearly hydrated player.
 *
 * <p>Wrapped, not redirected: a second {@code @Redirect} of the same call fails to apply and crashes the
 * game, as Hexalia's before 1.3.8.2 did (issue #66). A wrap chains with other mods' wraps and redirects.
 */
@Mixin(FoodData.class)
abstract class FoodDataMixin {
    // FoodData#tick took any Player until 1.21.2 narrowed it to ServerPlayer, and heals through it.
    //? if >=1.21.2 {
    private static final String HEAL = "Lnet/minecraft/server/level/ServerPlayer;heal(F)V";
    //?} else
    //private static final String HEAL = "Lnet/minecraft/world/entity/player/Player;heal(F)V";

    @Shadow public abstract void addExhaustion(float amount);

    @Shadow public abstract float getSaturationLevel();

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", ordinal = 0, target = HEAL))
    //? if >=1.21.2 {
    private void thirst$healWithSaturation(ServerPlayer player, float amount, Operation<Void> heal) {
    //?} else
    //private void thirst$healWithSaturation(Player player, float amount, Operation<Void> heal) {
        if (HealthRegen.blocksFoodHeal(player)) addExhaustion(-Math.min(getSaturationLevel(), HealthRegen.MAX_REFUND));
        else heal.call(player, amount);
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", ordinal = 1, target = HEAL))
    //? if >=1.21.2 {
    private void thirst$healWithHunger(ServerPlayer player, float amount, Operation<Void> heal) {
    //?} else
    //private void thirst$healWithHunger(Player player, float amount, Operation<Void> heal) {
        if (HealthRegen.blocksFoodHeal(player)) addExhaustion(-HealthRegen.MAX_REFUND);
        else heal.call(player, amount);
    }
}
