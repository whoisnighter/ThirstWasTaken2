package com.thirstwastaken2.sophisticated;

import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies {@code thirstwastaken2.sophisticated.mixins.json} only when Sophisticated Core is installed,
 * and each mixin only where Core still has what it hooks.
 *
 * <p>Every injector in the config is {@code require = 0}, so nothing upstream can stop the game. A method
 * a mixin names that is gone skips the mixin; a call a hook sits on that is gone leaves that hook doing
 * nothing. Both are read off the target's class file before it is transformed and logged as "not a
 * version ThirstWasTaken2 supports", which is how an upstream change is noticed. Core 1.5.35 (1.21.1)
 * moving the Tank's container code out of {@code TankUpgradeWrapper} crashed every game with a
 * {@code require = 1} hook on the old method (issue #61).
 */
public final class SophisticatedMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("thirstwastaken2");

    /**
     * What a mixin needs of its one target: the methods it names, every one of which must be there, and
     * the calls in them its hooks sit on, as {@code owner.name} with the owner slashed. And what the
     * player loses without it.
     */
    record Need(String consequence, List<String> methods, List<String> calls) { }

    /**
     * The Tank's container code, which Core moved out of {@code TankUpgradeWrapper} into this class of its
     * own in 1.21.1-1.5.35, 26.1.2-1.5.36 and 26.2-1.5.34, so Sophisticated Storage's tank block shares it.
     */
    static final String TANK_CONTAINER_HANDLER = "net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankContainerHandler";

    /** The mixins that are the same on both generations of Core, by simple name. */
    private static final Map<String, Need> COMMON = Map.of(
            "FeedingUpgradeWrapperMixin", new Need("food fed by the Feeding upgrade restores no thirst",
                    List.of("tryFeedingStack"), List.of("net/minecraft/world/item/Item.finishUsingItem")),
            "CookingLogicMixin", new Need("a canteen or flask cooked by a Smelting upgrade may come out empty",
                    List.of("smelt", "getCookInput"),
                    List.of("net/p3pp3rf1y/sophisticatedcore/upgrades/cooking/CookingLogic.setCookOutput")));

    private static final Map<String, Need> NEEDS = new HashMap<>();

    static {
        NEEDS.putAll(COMMON);
        NEEDS.putAll(SophisticatedGeneration.NEEDS);
    }

    @Override
    public void onLoad(String mixinPackage) { }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!SophisticatedPresence.isPresent()) return false;
        String target = targetClassName.replace('/', '.');
        String mixin = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        // One of the two Tank mixins applies, whichever shape this Core has: the old one, up to Core
        // 1.21.1-1.5.33, 26.1.2-1.5.34 and 26.2-1.5.32, only where the container handler is not yet a class
        // of its own; the new one only where it is.
        if (mixin.equals("TankUpgradeWrapperMixin") && SophisticatedPresence.hasClass(TANK_CONTAINER_HANDLER)) return false;
        if (mixin.equals("TankContainerHandlerMixin") && !SophisticatedPresence.hasClass(TANK_CONTAINER_HANDLER)) return false;
        Need need = NEEDS.get(mixin);
        if (need == null) return SophisticatedPresence.hasClass(target);
        for (String method : need.methods()) {
            if (!SophisticatedPresence.declaresMethod(target, method)) {
                LOGGER.warn("{} has no {}, so it is not a version of Sophisticated Core ThirstWasTaken2 supports; {}",
                        target, method, need.consequence());
                return false;
            }
        }
        // The methods are there; a call a hook sits on may not be. require = 0 lets the mixin apply without
        // it, that hook doing nothing and the rest still working. Read before Mixin transforms the class.
        Set<String> calls = SophisticatedPresence.callsIn(target, need.methods());
        for (String call : need.calls()) {
            if (!calls.contains(call)) {
                LOGGER.warn("{} no longer calls {}, so it is not a version of Sophisticated Core ThirstWasTaken2 supports; {}",
                        target, call, need.consequence());
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
}
