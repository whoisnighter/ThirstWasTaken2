package com.thirstwastaken2.cookingforblockheads.mixin;

import com.thirstwastaken2.cookingforblockheads.SinkGrade;
import com.thirstwastaken2.cookingforblockheads.SinkWater;
//? if >=1.20.5 {
import net.blay09.mods.cookingforblockheads.block.entity.SinkBlockEntity;
//?} else {
/*import net.blay09.mods.cookingforblockheads.tile.SinkBlockEntity;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the grade of the water in a Sink's tank on its block entity, and saves it with the tank, under a
 * key of this mod's own: Balm's tank keeps only a fluid and an amount. See {@link SinkWater}.
 *
 * <p>No setChanged here: the tank's own fill marks the block entity changed before it fills, and the grade
 * changes only on a fill. Nothing is sent to the client, which never needs the grade. Only the save and load
 * signatures fork, and the package the block entity is in before 1.20.5.
 */
@Mixin(SinkBlockEntity.class)
abstract class SinkBlockEntityMixin implements SinkGrade {
    @Unique
    private static final String thirst$KEY = "thirstwastaken2_grade";

    @Unique
    private int thirst$grade = SinkWater.UNKNOWN;

    @Override
    public int thirst$grade() {
        return thirst$grade;
    }

    @Override
    public void thirst$setGrade(int grade) {
        thirst$grade = grade;
    }

    //? if >=1.21.6 {
    @Inject(method = "saveAdditional", require = 0, at = @At("TAIL"))
    private void thirst$saveGrade(net.minecraft.world.level.storage.ValueOutput output, CallbackInfo ci) {
        if (thirst$grade != SinkWater.UNKNOWN) output.putInt(thirst$KEY, thirst$grade);
    }

    @Inject(method = "loadAdditional", require = 0, at = @At("TAIL"))
    private void thirst$loadGrade(net.minecraft.world.level.storage.ValueInput input, CallbackInfo ci) {
        thirst$grade = input.getIntOr(thirst$KEY, SinkWater.UNKNOWN);
    }
    //?} elif >=1.20.5 {
    /*@Inject(method = "saveAdditional", require = 0, at = @At("TAIL"))
    private void thirst$saveGrade(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, CallbackInfo ci) {
        if (thirst$grade != SinkWater.UNKNOWN) tag.putInt(thirst$KEY, thirst$grade);
    }

    @Inject(method = "loadAdditional", require = 0, at = @At("TAIL"))
    private void thirst$loadGrade(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, CallbackInfo ci) {
        thirst$grade = tag.contains(thirst$KEY) ? tag.getInt(thirst$KEY) : SinkWater.UNKNOWN;
    }
    *///?} else {
    /*@Inject(method = "saveAdditional", require = 0, at = @At("TAIL"))
    private void thirst$saveGrade(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        if (thirst$grade != SinkWater.UNKNOWN) tag.putInt(thirst$KEY, thirst$grade);
    }

    @Inject(method = "load", require = 0, at = @At("TAIL"))
    private void thirst$loadGrade(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        thirst$grade = tag.contains(thirst$KEY) ? tag.getInt(thirst$KEY) : SinkWater.UNKNOWN;
    }
    *///?}
}
