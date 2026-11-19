package com.thirstwastaken2.neoforge;

import com.thirstwastaken2.item.ThirstItems;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Gives the waterskin and the terracotta bowls NeoForge 1.21.1's item fluid capability, and the copper
 * distiller its block one, {@link DistillerFluidHandler}.
 */
final class WaterContainerCapabilities {
    private WaterContainerCapabilities() { }

    static void register(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new WaterContainerFluidHandler(stack),
                ThirstItems.WATERSKIN, ThirstItems.COPPER_CANTEEN, ThirstItems.IRON_FLASK,
                ThirstItems.DIAMOND_FLASK, ThirstItems.NETHERITE_FLASK,
                ThirstItems.TERRACOTTA_BOWL, ThirstItems.TERRACOTTA_WATER_BOWL);
        DistillerFluidHandler.register(event);
    }
}
