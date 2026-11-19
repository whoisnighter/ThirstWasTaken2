package com.thirstwastaken2.datagen;

import com.thirstwastaken2.ThirstWasTaken2;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

import java.util.concurrent.CompletableFuture;

/**
 * Fluids whose water {@code WaterPurity.sampleAt} grades the same wherever they lie, ahead of the biome:
 * {@code pure_water} always Pure, {@code salt_water} salty like the sea.
 *
 * <p>{@code pure_water} ships empty, for data packs. Spelunkery's Spring Water is in {@code salt_water} as
 * optional entries, so the tag loads without Spelunkery: a sluice sifts rock salt, saltpeter and sulfur
 * out of it and its pools are rimmed with basalt, so it reads as hot mineral water, not a mountain
 * stream. It was Pure until a player pointed that out, and one placed bucket was endless Pure bottles.
 * See docs/dev/integration/world/SPELUNKERY-INTEGRATION.md.
 */
public final class ThirstFluidTagProvider extends FabricTagsProvider<Fluid> {
    private static final TagKey<Fluid> PURE_WATER =
            TagKey.create(Registries.FLUID, ThirstWasTaken2.id("pure_water"));
    private static final TagKey<Fluid> SALT_WATER =
            TagKey.create(Registries.FLUID, ThirstWasTaken2.id("salt_water"));

    public ThirstFluidTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Registries.FLUID, registries);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        // Fabric API's tag builder was called getOrCreateTagBuilder on 1.21.1.
        //? if >1.21.1 {
        builder(PURE_WATER);
        builder(SALT_WATER)
        //?} else {
        /*getOrCreateTagBuilder(PURE_WATER);
        getOrCreateTagBuilder(SALT_WATER)
        *///?}
                .addOptional(spelunkery("spring_water"))
                .addOptional(spelunkery("flowing_spring_water"));
    }

    private static ResourceKey<Fluid> spelunkery(String path) {
        return ResourceKey.create(Registries.FLUID, Identifier.fromNamespaceAndPath("spelunkery", path));
    }

    @Override
    public String getName() {
        return "ThirstWasTaken2 Fluid Tags";
    }
}
