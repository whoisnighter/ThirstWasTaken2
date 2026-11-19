package com.thirstwastaken2.neoforge;

import com.thirstwastaken2.item.ThirstItems;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Gives the waterskin and the terracotta bowls the transfer API's item fluid capability, and the copper
 * distiller its block one, {@link DistillerResourceHandler}.
 */
final class WaterContainerCapabilities {
    private WaterContainerCapabilities() { }

    static void register(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.Fluid.ITEM, (stack, access) -> new WaterContainerResourceHandler(access),
                ThirstItems.WATERSKIN, ThirstItems.COPPER_CANTEEN, ThirstItems.IRON_FLASK,
                ThirstItems.DIAMOND_FLASK, ThirstItems.NETHERITE_FLASK,
                ThirstItems.TERRACOTTA_BOWL, ThirstItems.TERRACOTTA_WATER_BOWL);
        DistillerResourceHandler.register(event);
    }
}
