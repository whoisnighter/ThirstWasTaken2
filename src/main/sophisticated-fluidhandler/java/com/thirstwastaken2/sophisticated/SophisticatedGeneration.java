package com.thirstwastaken2.sophisticated;

import com.thirstwastaken2.sophisticated.SophisticatedMixinPlugin.Need;

import java.util.List;
import java.util.Map;

/**
 * Which generation of Sophisticated Core this node's fluid code was written for: {@code IFluidHandler},
 * Core for 1.21.1.
 */
final class SophisticatedGeneration {
    /** The storage inventory the upgrades work through, which the transfer API generation replaced. */
    static final String MARKER = "net/p3pp3rf1y/sophisticatedcore/inventory/ITrackedContentsItemHandler.class";

    private static final String TANK = "water in a Tank upgrade loses its grade";

    /** What each of this generation's mixins needs of its target; see {@code SophisticatedMixinPlugin}. */
    static final Map<String, Need> NEEDS = Map.of(
            "TankUpgradeWrapperMixin", new Need(TANK, List.of("getFluidHandler"), List.of()),
            "TankContainerHandlerMixin", new Need(TANK, List.of("getFluidHandler"), List.of()),
            "PumpUpgradeWrapperMixin", new Need("water moved by a Pump upgrade may lose its grade",
                    List.of("fillFromBlock", "fillFromHand", "fillContainerInHand", "fillFluidHandler"),
                    List.of("net/p3pp3rf1y/sophisticatedcore/upgrades/pump/PumpUpgradeWrapper.fillFromFluidHandler",
                            "net/neoforged/neoforge/fluids/FluidUtil.tryFluidTransfer")),
            "FluidFilterLogicMixin", new Need("a pump filter set to water may refuse graded water",
                    List.of("matchesFluidFilter"),
                    List.of("net/neoforged/neoforge/fluids/FluidStack.isSameFluidSameComponents")),
            "AlchemyUpgradeWrapperMixin", new Need("the Alchemy upgrade may give no thirst, or drink plain water",
                    List.of("applyTo", "tick"),
                    List.of("net/p3pp3rf1y/sophisticatedcore/upgrades/alchemy/AlchemyCondition.test",
                            "net/p3pp3rf1y/sophisticatedcore/upgrades/alchemy/AlchemyUpgradeWrapper$FinishUsing.apply")));

    private SophisticatedGeneration() { }
}
