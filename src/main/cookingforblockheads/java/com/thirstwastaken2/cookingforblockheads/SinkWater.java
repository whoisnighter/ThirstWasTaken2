package com.thirstwastaken2.cookingforblockheads;

import com.thirstwastaken2.item.WaterContainers;
import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The water Cooking for Blockheads' Sink hands out. By default the Sink is an endless tap, and with
 * {@code sinkRequiresWater} it is a tank that keeps only an amount of plain water. Either way it gave out
 * plain water bottles and buckets, which read as {@code defaultQuality}, Clean: an endless tap of Clean
 * water in any kitchen made purifying pointless, and a bucket of sea water poured into a tank came back
 * fresh.
 *
 * <p>Deliberate: everything drawn from the Sink is {@link #TAP}, Murky, whatever filled it, the grade
 * Candlelight's and Hearth and Harvest's sinks give, so the mods agree. The Sink stays an endless tap,
 * as its author meant, but its water wants boiling; the Oven next to it does that, through the mod's
 * smoking recipes. Sea water is refused on the way in, since it would come out fresh.
 *
 * <p>The water leaves the Sink by several routes, which differ by version: a bottle it builds itself, a
 * bucket through Balm's fluid hooks or its own helper, the mod's waterskins and bowls through the
 * loader's fluid API. So rather than hooking each, {@link #before} copies the inventory as the click
 * starts and {@link #after} grades what is new when it ends. Copying 41 stacks once per click on a Sink,
 * on the server, is cheap. Loaded only from the mixin, which only applies with the mod installed.
 */
public final class SinkWater {
    /** Murky: water from a tap nobody has boiled. */
    public static final WaterQuality TAP = WaterQuality.fresh(1);

    private SinkWater() { }

    /** Whether {@code stack} is sea water, which the Sink refuses: it would come out fresh. */
    public static boolean refuses(ItemStack stack) {
        return WaterPurity.isWaterContainer(stack) && WaterPurity.isSalty(stack);
    }

    /** The inventory as a click on the Sink starts, or null on the client, which grades nothing. */
    public static ItemStack[] before(Level level, Player player) {
        if (level.isClientSide()) return null;
        Inventory inventory = player.getInventory();
        ItemStack[] copy = new ItemStack[inventory.getContainerSize()];
        for (int slot = 0; slot < copy.length; slot++) copy[slot] = inventory.getItem(slot).copy();
        return copy;
    }

    /**
     * Grades the water that reached the inventory during the click, slot by slot, against {@code before}:
     * a water container new in a slot is {@link #TAP}; more of the same stack, the new ones are; one of the
     * mod's vessels that gained servings mixes them in, serving-weighted, as pouring by hand does. A slot
     * that lost water poured it into the Sink, and keeps its grade.
     */
    public static void after(ItemStack[] before, Player player) {
        if (before == null) return;
        Inventory inventory = player.getInventory();
        int slots = Math.min(before.length, inventory.getContainerSize());
        for (int slot = 0; slot < slots; slot++) {
            ItemStack was = before[slot];
            ItemStack now = inventory.getItem(slot);
            if (!WaterPurity.isWaterContainer(now) || WaterPurity.isSalty(now) || ItemStack.matches(now, was)) continue;
            if (ItemStack.matches(now, was.copyWithCount(now.getCount()))) {
                // The same water, more of it: only the new ones came from the Sink.
                int drawn = now.getCount() - was.getCount();
                if (drawn <= 0) continue;
                now.shrink(drawn);
                Vanilla.placeItemBackInInventory(player, WaterPurity.setQuality(now.copyWithCount(drawn), TAP));
            } else if (WaterContainers.handles(now) && now.getItem() == was.getItem()) {
                int servings = WaterContainers.servings(now);
                if (servings <= WaterContainers.servings(was)) continue;
                ItemStack mixed = WaterContainers.holding(was, TAP, servings);
                if (mixed == null) continue;
                mixed.setCount(now.getCount());
                inventory.setItem(slot, mixed);
            } else {
                WaterPurity.setQuality(now, TAP);
            }
        }
        // A full inventory: the filled container is dropped at the player's feet instead (vanilla's
        // createFilledResult, which Balm and the Sink's own helpers go through), where the slots above
        // never see it. Left plain, it read as Clean, so a full inventory made the Sink a Clean tap again.
        // Dropped this tick means not yet ticked; only plain water is touched, so anything graded is left.
        for (ItemEntity dropped : player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(2),
                entity -> entity.tickCount == 0)) {
            ItemStack item = dropped.getItem();
            if (WaterPurity.isWaterContainer(item) && !WaterPurity.isStamped(item)) {
                dropped.setItem(WaterPurity.setQuality(item.copy(), TAP));
            }
        }
    }
}
