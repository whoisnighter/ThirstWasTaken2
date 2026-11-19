package com.thirstwastaken2.block;

import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.platform.Loader;
import com.thirstwastaken2.platform.SavedBlockEntity;
import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The copper distiller's machine, on its boiler half: its two tanks, its fire, how far the serving on the
 * boil has got, the salt it has left behind, and its slots. Its numbers are in
 * {@code docs/dev/mechanics/WATER-REFERENCE.md}.
 *
 * <p>Each tick on the server it pours the container in {@link #WATER_IN} into the boiler, fills the one
 * in {@link #EMPTY_IN} from the basin, and boils: while the boiler holds water, the basin has room and
 * the tub holds its coolant, the fire burns and every {@link #servingTicks} one serving moves from the
 * boiler to the basin, Pure whatever it was. With nothing to do the fire waits rather than burning down,
 * so no fuel is wasted. Its block is {@code lit} while fuel is alight.
 *
 * <p>Every {@link #SALT_SERVINGS} salty servings leave one salt in {@link #SALT_OUT}, when another mod
 * has salt ({@link DistillerSalt}); with none, nothing. When that slot is full the salty water waits, as
 * a furnace waits on a full output; fresh water, which leaves nothing, still runs.
 *
 * <p>Hoppers reach it through either half: from above into {@link #WATER_IN}, from the sides into
 * {@link #FUEL} or {@link #EMPTY_IN}, whichever takes the item, and from below out of
 * {@link #FILLED_OUT}, {@link #SALT_OUT}, and whatever is left in the other two once it is spent, an
 * empty bucket or bottle, as a furnace gives back a lava bucket's bucket.
 *
 * <p>It is also the menu a player opens on either half, {@link DistillerMenu}, whose gauges read
 * {@link #data}.
 *
 * <p>Pipes reach its tanks through each loader's fluid capability, in millibuckets: water in to the
 * boiler through {@link #fillBoiler}, Pure water out of the basin through {@link #drainBasin}. So the
 * tanks count millibuckets underneath, {@link #SERVING_MB} to a serving, and a pipe's 50 mB a tick
 * fills a part serving, which boils once it is whole. The containers still move whole servings.
 * See {@code docs/dev/integration/storage/PIPEZ-INTEGRATION.md}.
 *
 * <p>The client is told what it saves, for Jade's overlay, but only when the tanks or the salt count
 * change, a few times a serving at most: the fire and the progress tick every tick, and only the menu
 * shows those.
 */
public final class DistillerBlockEntity extends SavedBlockEntity implements WorldlyContainer, MenuProvider {
    public static final int WATER_IN = 0;
    public static final int FUEL = 1;
    public static final int EMPTY_IN = 2;
    public static final int FILLED_OUT = 3;
    public static final int SALT_OUT = 4;
    public static final int SLOTS = 5;
    /** Salty servings that leave one salt behind: a bucket of sea water. */
    public static final int SALT_SERVINGS = 3;
    /** Millibuckets in a serving, as every fluid integration counts one. */
    public static final int SERVING_MB = 250;

    private static final int[] TOP = { WATER_IN };
    private static final int[] SIDES = { FUEL, EMPTY_IN };
    private static final int[] BOTTOM = { FILLED_OUT, SALT_OUT, WATER_IN, FUEL };

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    /** Millibuckets waiting in the boiler, and their one quality as {@code WaterPurity.storedValue} keeps it, 0 for none. */
    private int boilerMb;
    private int boilerQuality;
    /** Millibuckets of Pure water in the basin. */
    private int basinMb;
    /** Ticks of fire left, and how long the fuel burning now lasts in all. */
    private int burnLeft;
    private int burnTotal;
    /** Ticks the serving on the boil has had. */
    private int progress;
    /** Salty servings distilled toward the next salt, up to {@link #SALT_SERVINGS}. */
    private int saltServings;
    /** What the client was last sent, {@link #synced()}; -1 before anything was. */
    private long lastSynced = -1;

    /** What the menu shows, read live on the server and sent to the client by the menu, by {@code DistillerMenu}'s indices. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            switch (index) {
                case DistillerMenu.BURN_LEFT:
                    return burnLeft;
                case DistillerMenu.BURN_TOTAL:
                    return burnTotal;
                case DistillerMenu.PROGRESS:
                    return progress;
                case DistillerMenu.SERVING:
                    return servingTicks();
                case DistillerMenu.TANK:
                    return tank();
                case DistillerMenu.BOILER:
                    return boilerServings();
                case DistillerMenu.BOILER_QUALITY:
                    return boilerMb == 0 ? 0 : boilerQuality;
                case DistillerMenu.BASIN:
                    return basinServings();
                case DistillerMenu.COOLED:
                    return tubCooled() ? 1 : 0;
                case DistillerMenu.SALT:
                    return DistillerSalt.item() != null ? 1 : 0;
                default:
                    return 0;
            }
        }

        @Override
        public void set(int index, int value) { }

        @Override
        public int getCount() {
            return DistillerMenu.DATA;
        }
    };

    public DistillerBlockEntity(BlockPos pos, BlockState state) {
        super(ThirstBlockEntities.COPPER_DISTILLER, pos, state);
    }

    /**
     * Servings each tank holds, {@code distillerTankServings}: by default 9, three buckets. A tank that
     * holds more after the config shrinks it keeps its water and takes no more until it drops below.
     */
    public static int tank() {
        return ThirstConfig.get().distillerTankServings;
    }

    /** Ticks a serving takes to distil, {@code distillerServingSeconds}: by default 8 seconds, so coal's 80 runs ten. */
    public static int servingTicks() {
        return ThirstConfig.get().distillerServingSeconds * 20;
    }

    /** Each tank's size in millibuckets. */
    public static int tankMb() {
        return tank() * SERVING_MB;
    }

    /** Whole servings waiting in the boiler. */
    public int boilerServings() {
        return boilerMb / SERVING_MB;
    }

    /** Whole servings the boiler has room for. */
    public int boilerRoom() {
        return Math.max(0, tankMb() - boilerMb) / SERVING_MB;
    }

    /** Millibuckets waiting in the boiler, a part serving included. */
    public int boilerMb() {
        return boilerMb;
    }

    /** What the boiler holds, or {@code null} when it is empty. */
    public WaterQuality boilerQuality() {
        if (boilerMb == 0 || boilerQuality == 0) return null;
        return boilerQuality == WaterPurity.BLOCK_SALT ? WaterQuality.SALT : WaterQuality.fresh(boilerQuality - 1);
    }

    /** Whole servings of Pure water in the basin. */
    public int basinServings() {
        return basinMb / SERVING_MB;
    }

    /** Millibuckets of Pure water in the basin, a part serving included. */
    public int basinMb() {
        return basinMb;
    }

    /** Ticks of fire left. */
    public int burnLeft() {
        return burnLeft;
    }

    /** Ticks the serving on the boil has had. */
    public int progress() {
        return progress;
    }

    /** Salty servings distilled toward the next salt. */
    public int saltServings() {
        return saltServings;
    }

    /** Whether the tub beside it holds its coolant, without which nothing condenses. */
    public boolean cooled() {
        return tubCooled();
    }

    @Override
    protected boolean syncsToClient() {
        return true;
    }

    /** Saves always, and tells the client only when what Jade shows has changed. */
    @Override
    protected void changed() {
        long synced = synced();
        if (synced == lastSynced) {
            setChanged();
            return;
        }
        lastSynced = synced;
        super.changed();
    }

    /**
     * The tanks, the boiler's quality and the salt count, packed into one number to compare. A tank holds
     * at most 64 servings, 16000 mB, so each fits its 16 bits.
     */
    private long synced() {
        return tanks() | (long) saltServings << 40;
    }

    /**
     * Pours up to {@code servings} of {@code quality} into the boiler, mixed the way a cauldron mixes:
     * salt stays salt, fresh water keeps the worse grade. Returns how many went in.
     */
    public int pour(int servings, WaterQuality quality) {
        int poured = Math.min(servings, boilerRoom());
        if (poured <= 0) return 0;
        fillBoiler(poured * SERVING_MB, quality);
        changed();
        return poured;
    }

    /** Takes up to {@code servings} of Pure water out of the basin. Returns how many came out. */
    public int draw(int servings) {
        int drawn = Math.min(servings, basinServings());
        if (drawn <= 0) return 0;
        basinMb -= drawn * SERVING_MB;
        changed();
        return drawn;
    }

    /**
     * Pours up to {@code mb} millibuckets of {@code quality} into the boiler, mixed as {@link #pour} mixes,
     * and returns how many went in. It neither saves nor tells the client: a pipe's move may still be
     * rolled back, so the loader's handler calls {@link #fluidsChanged} once it is final.
     */
    public int fillBoiler(int mb, WaterQuality quality) {
        int filled = Math.min(mb, tankMb() - boilerMb);
        if (filled <= 0) return 0;
        WaterQuality held = boilerQuality();
        boilerQuality = WaterPurity.storedValue(held == null ? quality : WaterQuality.worse(held, quality));
        boilerMb += filled;
        return filled;
    }

    /** Takes up to {@code mb} millibuckets of Pure water out of the basin, as {@link #fillBoiler} does. */
    public int drainBasin(int mb) {
        int drained = Math.min(mb, basinMb);
        if (drained <= 0) return 0;
        basinMb -= drained;
        return drained;
    }

    /** Both tanks and the boiler's quality in one number, for a transaction to put back. */
    public long tanks() {
        return (long) boilerMb | (long) boilerQuality << 16 | (long) basinMb << 24;
    }

    /** Puts back what {@link #tanks} gave. */
    public void restoreTanks(long tanks) {
        boilerMb = (int) (tanks & 0xFFFF);
        boilerQuality = (int) (tanks >>> 16 & 0xFF);
        basinMb = (int) (tanks >>> 24 & 0xFFFF);
    }

    /** Saves the tanks and tells the client, once a pipe's move is final. */
    public void fluidsChanged() {
        changed();
    }

    /** One tick of the machine, on the server. */
    public void tick() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;
        boolean changed = takeWater();
        changed |= fillContainer();
        changed |= makeSalt();
        if (canDistil()) {
            if (burnLeft == 0) changed |= lightFuel();
            if (burnLeft > 0) {
                burnLeft--;
                if (++progress >= servingTicks()) distilOne();
                changed = true;
            }
        } else if (boilerMb < SERVING_MB && progress > 0) {
            progress = 0;
            changed = true;
        }
        if (changed) changed();
        showFire(level);
    }

    /**
     * Whether there is water to boil, room for it in the basin, coolant in the tub to condense it, and,
     * for salt water, room for the salt it leaves.
     */
    private boolean canDistil() {
        return boilerMb >= SERVING_MB && basinMb + SERVING_MB <= tankMb() && tubCooled()
                && !(saltServings >= SALT_SERVINGS && WaterQuality.SALT.equals(boilerQuality()) && DistillerSalt.item() != null);
    }

    /** Draws the fire in the firebox, with its light and smoke, while fuel is alight, and puts it out after. */
    private void showFire(Level level) {
        BlockState state = getBlockState();
        boolean lit = burnLeft > 0;
        if (!state.is(ThirstBlocks.COPPER_DISTILLER) || state.getValue(DistillerBlock.LIT) == lit) return;
        level.setBlock(getBlockPos(), state.setValue(DistillerBlock.LIT, lit), Block.UPDATE_ALL);
    }

    /**
     * Puts one salt in {@link #SALT_OUT} once {@link #SALT_SERVINGS} salty servings are counted, when
     * there is a salt and room for it. Without a salt the count waits at its top, so a salt added later
     * by a data pack comes out at once.
     */
    private boolean makeSalt() {
        if (saltServings < SALT_SERVINGS) return false;
        Item salt = DistillerSalt.item();
        if (salt == null) return false;
        ItemStack out = items.get(SALT_OUT);
        if (out.isEmpty()) {
            items.set(SALT_OUT, new ItemStack(salt));
        } else if (out.is(salt) && out.getCount() < out.getMaxStackSize()) {
            out.grow(1);
        } else {
            return false;
        }
        saltServings -= SALT_SERVINGS;
        return true;
    }

    private boolean tubCooled() {
        BlockState state = getBlockState();
        if (!state.is(ThirstBlocks.COPPER_DISTILLER) || getLevel() == null) return false;
        BlockState tub = getLevel().getBlockState(getBlockPos().relative(DistillerBlock.towardOtherHalf(state)));
        return tub.is(ThirstBlocks.COPPER_DISTILLER) && tub.getValue(DistillerBlock.PART) == DistillerBlock.Part.TUB
                && tub.getValue(DistillerBlock.COOLED);
    }

    /** Pours the container in {@link #WATER_IN} into the boiler, a bucket only when all three fit. */
    private boolean takeWater() {
        ItemStack stack = items.get(WATER_IN);
        int held = DistillerWater.held(stack);
        int room = boilerRoom();
        if (held == 0 || room == 0 || stack.getCount() != 1 || DistillerWater.whole(stack) && held > room) {
            return false;
        }
        WaterQuality quality = WaterPurity.quality(stack);
        int poured = Math.min(held, room);
        items.set(WATER_IN, DistillerWater.emptied(stack, poured));
        pour(poured, quality);
        return true;
    }

    /**
     * Fills the container in {@link #EMPTY_IN} from the basin. One that is filled whole, a bottle or a
     * bucket, goes to {@link #FILLED_OUT} at once when that is free; a carried one fills as far as the
     * basin goes and moves out once it is full.
     */
    private boolean fillContainer() {
        ItemStack stack = items.get(EMPTY_IN);
        int room = DistillerWater.room(stack);
        boolean carried = !stack.isEmpty() && !DistillerWater.whole(stack);
        if (carried && room == 0) {
            // Filled earlier while the output was taken.
            if (!items.get(FILLED_OUT).isEmpty()) return false;
            items.set(FILLED_OUT, stack);
            items.set(EMPTY_IN, ItemStack.EMPTY);
            return true;
        }
        int basin = basinServings();
        if (room == 0 || basin == 0) return false;
        if (DistillerWater.whole(stack)) {
            if (basin < room || !items.get(FILLED_OUT).isEmpty()) return false;
            items.set(FILLED_OUT, DistillerWater.filled(stack, room));
            stack.shrink(1);
            basinMb -= room * SERVING_MB;
            return true;
        }
        int drawn = Math.min(room, basin);
        ItemStack skin = DistillerWater.filled(stack, drawn);
        basinMb -= drawn * SERVING_MB;
        boolean full = DistillerWater.room(skin) == 0;
        if (full && items.get(FILLED_OUT).isEmpty()) {
            items.set(FILLED_OUT, skin);
            items.set(EMPTY_IN, ItemStack.EMPTY);
        } else {
            items.set(EMPTY_IN, skin);
        }
        return true;
    }

    /** Burns one fuel item, leaving what is left of it, an empty bucket, in its slot. */
    private boolean lightFuel() {
        ItemStack fuel = items.get(FUEL);
        int burn = Loader.burnTime(this, fuel);
        if (burn <= 0) return false;
        ItemStack remainder = Vanilla.craftingRemainder(fuel);
        fuel.shrink(1);
        if (fuel.isEmpty() && !remainder.isEmpty()) items.set(FUEL, remainder);
        burnLeft = burn;
        burnTotal = burn;
        return true;
    }

    /** One serving from the boiler to the basin. */
    private void distilOne() {
        progress = 0;
        if (WaterQuality.SALT.equals(boilerQuality())) saltServings = Math.min(saltServings + 1, SALT_SERVINGS);
        boilerMb -= SERVING_MB;
        if (boilerMb == 0) boilerQuality = 0;
        basinMb += SERVING_MB;
    }

    @Override
    protected void save(Output output) {
        // Whole servings under the keys saves have always had, and beside them the part serving a pipe left.
        output.putInt("boiler", boilerMb / SERVING_MB);
        output.putInt("boiler_partial", boilerMb % SERVING_MB);
        output.putInt("boiler_quality", boilerQuality);
        output.putInt("basin", basinMb / SERVING_MB);
        output.putInt("basin_partial", basinMb % SERVING_MB);
        output.putInt("burn_left", burnLeft);
        output.putInt("burn_total", burnTotal);
        output.putInt("progress", progress);
        output.putInt("salt_servings", saltServings);
        output.putItems(items);
    }

    @Override
    protected void load(Input input) {
        boilerMb = input.getInt("boiler", 0) * SERVING_MB + input.getInt("boiler_partial", 0);
        boilerQuality = input.getInt("boiler_quality", 0);
        basinMb = input.getInt("basin", 0) * SERVING_MB + input.getInt("basin_partial", 0);
        burnLeft = input.getInt("burn_left", 0);
        burnTotal = input.getInt("burn_total", 0);
        progress = input.getInt("progress", 0);
        saltServings = input.getInt("salt_servings", 0);
        input.getItems(items);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP) return TOP;
        return side == Direction.DOWN ? BOTTOM : SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canPlaceItem(slot, stack);
    }

    /** From below: what the machine made, and what it has spent in the slots it took water and fuel from. */
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (side != Direction.DOWN) return false;
        if (slot == WATER_IN) return DistillerWater.held(stack) == 0;
        if (slot == FUEL) return Loader.burnTime(this, stack) == 0;
        return slot == FILLED_OUT || slot == SALT_OUT;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        // One container at a time in the water slot, as in the menu: a hopper moves one item per transfer
        // but would merge it into a stack of water bowls, which the slot cannot pour.
        if (slot == WATER_IN && !items.get(WATER_IN).isEmpty()) return false;
        return getLevel() != null && accepts(getLevel(), slot, stack);
    }

    /**
     * What each slot takes: water to pour, fuel to burn, a container to fill; nothing into the outputs.
     * The menu asks the same on the client, where there is no machine, only the level.
     */
    static boolean accepts(Level level, int slot, ItemStack stack) {
        switch (slot) {
            case WATER_IN:
                return DistillerWater.held(stack) > 0;
            case FUEL:
                return Loader.isFuel(level, stack);
            case EMPTY_IN:
                return DistillerWater.room(stack) > 0;
            default:
                return false;
        }
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new DistillerMenu(id, inventory, this, data);
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }
}
