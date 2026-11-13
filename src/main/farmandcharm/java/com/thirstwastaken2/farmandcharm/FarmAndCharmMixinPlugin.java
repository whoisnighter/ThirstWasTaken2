package com.thirstwastaken2.farmandcharm;

import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies {@code thirstwastaken2.farmandcharm.mixins.json} only where Farm & Charm is installed, and each
 * mixin only on the targets that are there: Candlelight's Large Cooking Pot is absent without Candlelight,
 * and the other targets stay.
 *
 * <p>Every injector in the config is {@code require = 0}, so nothing upstream can stop the game. A target
 * method that is gone skips the mixin; a call a hook sits on that is gone leaves that hook doing nothing.
 * Both are read off the target's class file before it is transformed and logged as "not a version
 * ThirstWasTaken2 supports", which is how an upstream change is noticed.
 */
public final class FarmAndCharmMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("thirstwastaken2");

    /**
     * What a mixin injects into on one target: the methods, at least one of which must be there, and the
     * calls in them its hooks sit on, each as {@code owner.name} under its Mojang name (NeoForge, a Fabric
     * dev run) and its intermediary one (a Fabric player's game). And what the player loses without it.
     */
    private record Need(String consequence, List<String> methods, List<List<String>> calls) { }

    /** {@code useItemOn}, under its Mojang name and its intermediary one. */
    private static final List<String> USE_ITEM_ON = List.of("useItemOn", "method_55765");

    private static final List<String> INGREDIENT_TEST = List.of(
            "net/minecraft/world/item/crafting/Ingredient.test", "net/minecraft/class_1856.method_8093");

    /**
     * The methods a mixin injects into, by target class. A target is skipped unless it declares at least
     * one, so a method renamed upstream skips that target, with a warning, rather than failing the game.
     */
    private static final Map<String, Need> NEEDS = Map.of(
            "net.satisfy.farm_and_charm.core.block.entity.CookingPotBlockEntity",
            new Need("sea water may cook there", List.of("matchesInventory"), List.of(INGREDIENT_TEST)),
            "net.satisfy.farm_and_charm.core.block.entity.StoveBlockEntity",
            new Need("sea water may cook there", List.of("findBestMatchingSlot", "countMatchingSlots"), List.of(INGREDIENT_TEST)),
            "net.satisfy.farm_and_charm.core.block.entity.CraftingBowlBlockEntity",
            new Need("sea water may cook there", List.of("matchExact"), List.of(INGREDIENT_TEST)),
            "net.satisfy.farm_and_charm.core.block.TimberWellBlock",
            new Need("Timber Well buckets may come out ungraded", USE_ITEM_ON, List.of(
                    List.of("net/minecraft/world/item/ItemUtils.createFilledResult", "net/minecraft/class_5328.method_30012"))),
            "net.satisfy.farm_and_charm.core.block.WaterTroughBlock",
            new Need("the Water Trough may take sea water and give ungraded water", USE_ITEM_ON, List.of(
                    List.of("net/minecraft/world/entity/player/Player.setItemInHand", "net/minecraft/class_1657.method_6122"))),
            "net.satisfy.foundation.block.SinkBlock",
            new Need("kitchen sinks may take sea water and give ungraded water", USE_ITEM_ON, List.of(
                    List.of("net/minecraft/world/entity/player/Inventory.add", "net/minecraft/class_1661.method_7394"),
                    List.of("net/minecraft/world/entity/player/Player.drop", "net/minecraft/class_1657.method_7328"))));

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
        String target = dotted(targetClassName);
        if (target.equals(LARGE_COOKING_POT)) return FarmAndCharmPresence.declaresMethod(target, "matchesInventory");
        Need need = NEEDS.get(target);
        if (need == null) return FarmAndCharmPresence.hasTarget(target);
        if (!FarmAndCharmPresence.hasMethod(target, need.consequence(), need.methods().toArray(new String[0]))) return false;
        // The method is there; a call a hook sits on may not be. require = 0 lets the mixin apply without
        // it, that hook doing nothing, and the rest still working. Read before Mixin transforms the class,
        // which is why this is not left to postApply, where MixinExtras' wrappers are not in yet.
        Set<String> calls = FarmAndCharmPresence.callsIn(target, need.methods());
        for (List<String> call : need.calls()) {
            if (call.stream().noneMatch(calls::contains)) {
                LOGGER.warn("{} no longer calls {} in {}, so it is not a version ThirstWasTaken2 supports; {}",
                        target, call.get(0), need.methods().get(0), need.consequence());
            }
        }
        return true;
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

    private static String dotted(String targetClassName) {
        return targetClassName.replace('/', '.');
    }
}
