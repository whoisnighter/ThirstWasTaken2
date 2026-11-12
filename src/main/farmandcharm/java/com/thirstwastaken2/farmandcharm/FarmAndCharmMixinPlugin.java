package com.thirstwastaken2.farmandcharm;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies {@code thirstwastaken2.farmandcharm.mixins.json} only where Farm & Charm is installed, and each
 * mixin only on the targets that are there: Candlelight's Large Cooking Pot is absent without Candlelight,
 * and the other targets stay.
 */
public final class FarmAndCharmMixinPlugin implements IMixinConfigPlugin {
    /**
     * The mods' own methods a mixin injects into, by target class. A target is skipped unless it declares
     * at least one, so a method renamed upstream skips that target rather than failing it.
     */
    private static final Map<String, List<String>> NEEDS_ONE_OF = Map.of(
            "net.satisfy.farm_and_charm.core.block.entity.CookingPotBlockEntity", List.of("matchesInventory"),
            "net.satisfy.farm_and_charm.core.block.entity.StoveBlockEntity", List.of("findBestMatchingSlot", "countMatchingSlots"),
            "net.satisfy.farm_and_charm.core.block.entity.CraftingBowlBlockEntity", List.of("matchExact"));

    /**
     * Candlelight's Large Cooking Pot had a copy of the Cooking Pot's matcher up to 2.1.13. From 2.1.14 it
     * extends Farm & Charm's Cooking Pot and declares no matcher, so the Cooking Pot target covers it and
     * its own is skipped, without the warning a missing method otherwise logs.
     */
    private static final String LARGE_COOKING_POT = "net.satisfy.candlelight.core.block.entity.LargeCookingPotBlockEntity";

    @Override
    public void onLoad(String mixinPackage) { }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // A target named by string reaches here as Loom wrote it into the Fabric jar, with slashes
        // (net/satisfy/...), while one named by class comes dotted. Compared as it came, the slashed Large
        // Cooking Pot missed its check, was applied unchecked, and crashed every Fabric client with
        // Candlelight 2.1.14 or later (issue #56).
        String target = targetClassName.replace('/', '.');
        if (target.equals(LARGE_COOKING_POT)) return FarmAndCharmPresence.declaresMethod(target, "matchesInventory");
        List<String> methods = NEEDS_ONE_OF.get(target);
        if (methods == null) return FarmAndCharmPresence.hasTarget(target);
        for (String method : methods) {
            if (FarmAndCharmPresence.hasMethod(target, method)) return true;
        }
        return false;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
