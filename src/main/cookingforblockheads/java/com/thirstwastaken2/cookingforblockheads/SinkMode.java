package com.thirstwastaken2.cookingforblockheads;

import net.blay09.mods.cookingforblockheads.CookingForBlockheadsConfig;

/**
 * Which Sink the server runs: Cooking for Blockheads' {@code sinkRequiresWater}, read each time since its
 * config can be reloaded. {@code getActive} returns {@code CookingForBlockheadsConfigData} up to 1.21.1 and
 * the config class itself after, both with the same public field, so this line compiles on every version.
 */
final class SinkMode {
    private SinkMode() { }

    /** True when the Sink is a tank that holds what is poured in, false for the endless tap. */
    static boolean isTank() {
        return CookingForBlockheadsConfig.getActive().sinkRequiresWater;
    }
}
