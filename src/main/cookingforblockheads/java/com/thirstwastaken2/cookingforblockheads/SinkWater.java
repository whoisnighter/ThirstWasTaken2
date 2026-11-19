package com.thirstwastaken2.cookingforblockheads;

import com.thirstwastaken2.item.WaterContainers;
import com.thirstwastaken2.platform.Loader;
import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The water Cooking for Blockheads' Sink hands out. By default the Sink is an endless tap, and with
 * {@code sinkRequiresWater} it is a tank of 16 buckets. Either way it gave out plain water bottles and
 * buckets, which read as {@code defaultQuality}, Clean: an endless tap of Clean water in any kitchen made
 * purifying pointless, and a bucket of sea water poured into a tank came back fresh.
 *
 * <p>Deliberate: the endless tap gives {@link #TAP}, Murky, the grade Candlelight's and Hearth and
 * Harvest's sinks give, so the mods agree. It stays an endless tap, as its author meant, but its water
 * wants boiling; the Oven next to it does that, through the mod's smoking recipes.
 *
 * <p>A tank keeps the grade of what went in, so a Sink fed Pure water from a distiller is a refill
 * station for it. The tank itself only counts an amount of one fluid, so the grade is kept beside it, on
 * the block entity ({@link SinkGrade}), and mixed to the worse as a cauldron mixes. It learns the grade
 * of what goes in from whoever pours: the player's click ({@link #before}), or the loader's fluid
 * wrapper Balm puts in front of the tank for pipes ({@link #piping}). Water that arrives by a route
 * neither sees counts as {@link #TAP}. Sea water is refused on the way in, by hand and by pipe, since
 * it would come out fresh.
 *
 * <p>The water leaves the Sink by several routes, which differ by version: a bottle it builds itself, a
 * bucket through Balm's fluid hooks or its own helper, the mod's waterskins and bowls through the
 * loader's fluid API. So rather than hooking each, {@link #before} copies the inventory as the click
 * starts and {@link #after} grades what is new when it ends. Copying 41 stacks once per click on a Sink,
 * on the server, is cheap. Loaded only from the mixins, which only apply with the mod installed.
 *
 * <p>The fields below are the server thread's: they are only set and read there, and each is put back
 * as the call that set it ends, so nothing is left over for the next fill.
 */
public final class SinkWater {
    /** Murky: water from a tap nobody has boiled. */
    public static final WaterQuality TAP = WaterQuality.fresh(1);
    /** No grade recorded: a Sink filled before this was added, or one never filled. Read as {@link #TAP}. */
    public static final int UNKNOWN = -1;

    /** The Sink a click is pouring into, and the grade of what the player holds. */
    private static Object clickSink;
    private static WaterQuality clickWater;
    /** The fluid a pipe is handing Balm's wrapper, as the loader's own object, read only if it lands. */
    private static Object pipeFluid;

    private SinkWater() { }

    /** What one click on the Sink saw as it started: the inventory, and the grade any water drawn has. */
    public record Click(ItemStack[] inventory, WaterQuality drawn) { }

    /** Whether {@code stack} is sea water, which the Sink refuses: it would come out fresh. */
    public static boolean refuses(ItemStack stack) {
        return WaterPurity.isWaterContainer(stack) && WaterPurity.isSalty(stack);
    }

    /**
     * Starts a click on the Sink: copies the inventory, notes the grade water drawn now has, and the
     * grade of the water in hand, should it be poured in. Null on the client, which grades nothing.
     */
    public static Click before(Level level, BlockEntity sink, Player player, ItemStack held) {
        if (level.isClientSide()) return null;
        Inventory inventory = player.getInventory();
        ItemStack[] copy = new ItemStack[inventory.getContainerSize()];
        for (int slot = 0; slot < copy.length; slot++) copy[slot] = inventory.getItem(slot).copy();
        if (sink instanceof SinkGrade && WaterPurity.isWaterContainer(held)) {
            clickSink = sink;
            clickWater = WaterPurity.quality(held);
        }
        return new Click(copy, drawn(sink));
    }

    /**
     * The grade water drawn from {@code sink} has: {@link #TAP} from the endless tap, the tank's own
     * otherwise. The tank's, while it holds water, is the one recorded.
     */
    private static WaterQuality drawn(BlockEntity sink) {
        if (!SinkMode.isTank() || !(sink instanceof SinkGrade grade)) return TAP;
        return decode(grade.thirst$grade());
    }

    /**
     * Ends a click: grades the water that reached the inventory during it, slot by slot, against
     * {@code click}: a water container new in a slot gets the Sink's grade; more of the same stack, the
     * new ones do; one of the mod's vessels that gained servings mixes them in, serving-weighted, as
     * pouring by hand does. A slot that lost water poured it into the Sink, and keeps its grade.
     */
    public static void after(Click click, Player player) {
        clickSink = null;
        clickWater = null;
        if (click == null) return;
        ItemStack[] before = click.inventory();
        WaterQuality drawn = click.drawn();
        Inventory inventory = player.getInventory();
        int slots = Math.min(before.length, inventory.getContainerSize());
        for (int slot = 0; slot < slots; slot++) {
            ItemStack was = before[slot];
            ItemStack now = inventory.getItem(slot);
            if (!WaterPurity.isWaterContainer(now) || WaterPurity.isSalty(now) || ItemStack.matches(now, was)) continue;
            if (ItemStack.matches(now, was.copyWithCount(now.getCount()))) {
                // The same water, more of it: only the new ones came from the Sink.
                int extra = now.getCount() - was.getCount();
                if (extra <= 0) continue;
                now.shrink(extra);
                Vanilla.placeItemBackInInventory(player, WaterPurity.setQuality(now.copyWithCount(extra), drawn));
            } else if (WaterContainers.handles(now) && now.getItem() == was.getItem()) {
                int servings = WaterContainers.servings(now);
                if (servings <= WaterContainers.servings(was)) continue;
                ItemStack mixed = WaterContainers.holding(was, drawn, servings);
                if (mixed == null) continue;
                mixed.setCount(now.getCount());
                inventory.setItem(slot, mixed);
            } else {
                WaterPurity.setQuality(now, drawn);
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
                dropped.setItem(WaterPurity.setQuality(item.copy(), drawn));
            }
        }
    }

    /**
     * A Balm fluid wrapper starts handing {@code fluid} to whatever tank it wraps, which may be a Sink's.
     * Returns what was there before, for {@link #piped} to put back: wrappers nest on some versions.
     * Only a reference is stored here; the grade is read once the water lands in a Sink.
     */
    public static Object piping(Object fluid) {
        Object previous = pipeFluid;
        pipeFluid = fluid;
        return previous;
    }

    /** The wrapper call {@link #piping} started has ended. */
    public static void piped(Object previous) {
        pipeFluid = previous;
    }

    /**
     * The grade of the water being poured into {@code sink} right now: the player's, when the click is on
     * this Sink, then the pipe's, then {@link #TAP} for a route neither sees. Null when a pipe is handing
     * over something that is not water, which a Sink's tank refuses on its own.
     */
    private static WaterQuality incoming(Object sink) {
        if (clickSink == sink && clickWater != null) return clickWater;
        Object fluid = pipeFluid;
        return fluid == null ? TAP : Loader.waterIn(fluid);
    }

    /** Whether the water arriving at {@code sink}'s tank is sea water, which is refused. */
    public static boolean refusesIncoming(BlockEntity sink) {
        if (sink == null || sink.getLevel() == null || sink.getLevel().isClientSide()) return false;
        return incoming(sink) instanceof WaterQuality.Salt;
    }

    /**
     * {@code added} millibuckets went into {@code sink}'s tank, which held {@code held} before: into an
     * empty tank the water's own grade, otherwise the worse of the two. Only a tank that really grew is
     * asked; the endless tap takes water without keeping it.
     */
    public static void filled(BlockEntity sink, int held, int added) {
        if (added <= 0 || !(sink instanceof SinkGrade grade) || sink.getLevel() == null || sink.getLevel().isClientSide()) return;
        WaterQuality water = incoming(sink);
        if (water == null) return;
        WaterQuality mixed = held <= 0 ? water : WaterQuality.worse(decode(grade.thirst$grade()), water);
        int encoded = encode(mixed);
        if (encoded != grade.thirst$grade()) grade.thirst$setGrade(encoded);
    }

    /** A fresh grade as its purity, 0 to 3. Sea water never gets in, so it has no code. */
    public static int encode(WaterQuality quality) {
        return quality instanceof WaterQuality.Fresh fresh ? fresh.purity() : UNKNOWN;
    }

    /** What {@link #encode} wrote, or {@link #TAP} for {@link #UNKNOWN} or anything out of range. */
    public static WaterQuality decode(int grade) {
        return grade >= WaterPurity.MIN && grade <= WaterPurity.MAX ? WaterQuality.fresh(grade) : TAP;
    }
}
