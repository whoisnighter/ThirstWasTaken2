package com.thirstwastaken2.kaleidoscope.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.api.blockentity.ITeapot;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.TeapotBlockEntity;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.thirstwastaken2.kaleidoscope.BrewedWater;
import com.thirstwastaken2.kaleidoscope.BrewedWaterQuality;
import com.thirstwastaken2.kaleidoscope.ReturnedWater;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The teapot keeps the grade of the water poured into it: back out in a bucket, and in the item when the
 * teapot is picked up and placed again. Sea water goes in and comes back out salty, but brews nothing:
 * tea brewed from the sea would come out as safe as any other, a free way to make it drinkable.
 *
 * <p>The tea itself is not affected. It is brewed from boiled water, so a cup restores its fixed value
 * whatever fresh water went into the pot.
 */
@Mixin(value = TeapotBlockEntity.class, remap = false)
abstract class TeapotBlockEntityMixin implements BrewedWater {
    @Shadow private int status;
    @Shadow private Identifier teaFluidId;

    /** What was poured in. Only meaningful while {@link #thirst$heldWater} says there is water. */
    @Unique private WaterQuality thirst$quality;

    @Override
    public WaterQuality thirst$heldWater() {
        return status == ITeapot.PUT_INGREDIENT && BrewedWaterQuality.isWater(teaFluidId) ? thirst$quality : null;
    }

    @Override
    public void thirst$holdWater(WaterQuality quality) {
        thirst$quality = quality;
    }

    /**
     * Every overload of {@code addTeaFluid}. Refabricated 1.6.0.7 added one that also takes the Transfer
     * API's {@code ContainerItemContext}: the old one now calls it, and the recipe item calls it straight,
     * so a selector by name alone, which takes only the first overload, missed the recipe item and left
     * the last grade in the teapot. The stack is read by type, so this names no Fabric class and matches
     * the single overload the other builds have. Nested calls read the same full bucket and store the
     * same grade.
     */
    @Inject(method = "addTeaFluid*", at = @At("HEAD"))
    private void thirst$readPouredGrade(CallbackInfoReturnable<Boolean> cir, @Local(argsOnly = true) ItemStack itemStack,
                                        @Share("poured") LocalRef<WaterQuality> poured) {
        // Read before the call: it empties the bucket.
        poured.set(BrewedWaterQuality.of(itemStack));
    }

    @Inject(method = "addTeaFluid*", at = @At("RETURN"))
    private void thirst$keepPouredGrade(CallbackInfoReturnable<Boolean> cir, @Share("poured") LocalRef<WaterQuality> poured) {
        if (cir.getReturnValueZ()) thirst$quality = poured.get();
    }

    /**
     * Sea water brews nothing. The teapot starts brewing from its tick, once it has water, heat and a
     * recipe for what went in, so the tick is skipped while it holds sea water: no recipe is looked up,
     * no timer runs, and the ingredients wait until the water is taken back out. {@code tick} is the
     * mod's own, matched by name; it has one overload on every build.
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void thirst$noTeaFromSeaWater(Level level, CallbackInfo ci) {
        if (BrewedWaterQuality.isSalt(thirst$heldWater())) ci.cancel();
    }

    @WrapMethod(method = "removeTeaFluid")
    private boolean thirst$returnGrade(Level level, LivingEntity user, ItemStack itemStack, Operation<Boolean> original) {
        boolean removed = ReturnedWater.during(thirst$heldWater(), () -> original.call(level, user, itemStack));
        if (removed) thirst$quality = null;
        return removed;
    }

    /**
     * The teapot picked up with water in it: {@code getDrops} builds the item's tag from nothing, holding
     * only the fluid id. A finished teapot writes a tag too, but holds no water, so nothing is added there.
     * The call is Minecraft's, so its target is remapped; from 1.21.11 it is handed a {@code TagValueOutput}.
     */
    //? if >1.21.1 {
    @ModifyArg(method = "getDrops", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/item/BlockItem;setBlockEntityData(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/entity/BlockEntityType;Lnet/minecraft/world/level/storage/TagValueOutput;)V"),
            index = 2)
    private net.minecraft.world.level.storage.TagValueOutput thirst$keepGradeInItem(net.minecraft.world.level.storage.TagValueOutput output) {
        return BrewedWaterQuality.saved(output, output::putInt, thirst$heldWater());
    }
    //?} else {
    /*@ModifyArg(method = "getDrops", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/item/BlockItem;setBlockEntityData(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/entity/BlockEntityType;Lnet/minecraft/nbt/CompoundTag;)V"),
            index = 2)
    private net.minecraft.nbt.CompoundTag thirst$keepGradeInItem(net.minecraft.nbt.CompoundTag tag) {
        return BrewedWaterQuality.saved(tag, tag::putInt, thirst$heldWater());
    }
    *///?}

    // Minecraft's own methods, so remapped: the Fabric 1.21.x jars name them in intermediary. From
    // 1.21.6 they take a ValueOutput / ValueInput rather than a tag; before 1.20.5 they take no
    // registries, and loading is `load`.
    //? if >=1.21.6 {
    @Inject(method = "saveAdditional", at = @At("TAIL"), remap = true)
    private void thirst$saveGrade(net.minecraft.world.level.storage.ValueOutput output, CallbackInfo ci) {
        BrewedWaterQuality.save(output::putInt, thirst$heldWater());
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"), remap = true)
    private void thirst$loadGrade(net.minecraft.world.level.storage.ValueInput input, CallbackInfo ci) {
        thirst$quality = BrewedWaterQuality.load(input::getIntOr);
    }
    //?} elif >=1.20.5 {
    /*@Inject(method = "saveAdditional", at = @At("TAIL"), remap = true)
    private void thirst$saveGrade(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, CallbackInfo ci) {
        BrewedWaterQuality.save(tag::putInt, thirst$heldWater());
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"), remap = true)
    private void thirst$loadGrade(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, CallbackInfo ci) {
        thirst$quality = BrewedWaterQuality.load((key, fallback) -> com.thirstwastaken2.platform.Vanilla.getInt(tag, key, fallback));
    }
    *///?} else {
    /*@Inject(method = "saveAdditional", at = @At("TAIL"), remap = true)
    private void thirst$saveGrade(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        BrewedWaterQuality.save(tag::putInt, thirst$heldWater());
    }

    @Inject(method = "load", at = @At("TAIL"), remap = true)
    private void thirst$loadGrade(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        thirst$quality = BrewedWaterQuality.load((key, fallback) -> com.thirstwastaken2.platform.Vanilla.getInt(tag, key, fallback));
    }
    *///?}
}
