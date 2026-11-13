package com.thirstwastaken2.sophisticated;

import com.thirstwastaken2.sophisticated.SophisticatedMixinPlugin.Need;

import java.util.List;
import java.util.Map;

/**
 * Which generation of Sophisticated Core this node's fluid code was written for: the transfer API
 * ({@code ResourceHandler<FluidResource>}, {@code ItemAccess}, transactions), Core 1.21.11 and later.
 */
final class SophisticatedGeneration {
    /** The item access Core's tanks and pumps hand container handlers, which only this generation has. */
    static final String MARKER = "net/p3pp3rf1y/sophisticatedcore/util/MutableStackItemAccess.class";

    private static final String TANK = "water in a Tank upgrade loses its grade";

    /** What each of this generation's mixins needs of its target; see {@code SophisticatedMixinPlugin}. */
    static final Map<String, Need> NEEDS = Map.of(
            "TankUpgradeWrapperMixin", new Need(TANK, List.of("getFluidHandler"), List.of()),
            "TankContainerHandlerMixin", new Need(TANK, List.of("getFluidHandler"), List.of()),
            "PumpUpgradeWrapperMixin", new Need("water moved by a Pump upgrade may lose its grade",
                    List.of("fillFromBlock", "handleFluidContainerInHand"),
                    List.of("net/p3pp3rf1y/sophisticatedcore/util/CapabilityHelper.getFromFluidHandler")),
            "FluidFilterLogicMixin", new Need("a pump filter set to water may refuse graded water",
                    List.of("matchesFluidFilter"),
                    List.of("net/neoforged/neoforge/transfer/fluid/FluidResource.matches",
                            "net/neoforged/neoforge/fluids/FluidStack.isSameFluidSameComponents")),
            "AlchemyUpgradeWrapperMixin", new Need("the Alchemy upgrade may drink plain water",
                    List.of("applyTo"),
                    List.of("net/p3pp3rf1y/sophisticatedcore/upgrades/alchemy/AlchemyCondition.test")));

    private SophisticatedGeneration() { }
}
