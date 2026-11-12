package com.thirstwastaken2.item;

import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.platform.DrinkItem;
import com.thirstwastaken2.platform.ItemWaterData;
import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.IntSupplier;

/**
 * A carried, reusable water container holding several drinks of one mixed purity: the leather
 * waterskin, the copper canteen and the iron flask. They differ only in capacity, sprite and whether
 * they boil, so code that asks whether a stack is one checks {@link #is(ItemStack)}, never one item.
 *
 * <p>The canteen and the flask boil their water Clean over a lit campfire. Holding use on the campfire
 * repeats {@link #useOn} every {@link #BOIL_STEP_TICKS} ticks, the rate vanilla repeats a held right
 * click at, and each repeat is one step of boiling. The progress is kept on the server per player
 * rather than on the stack: a component written every step would re-sync the held stack, and the
 * client plays the re-equip animation for every changed stack in the hand.
 */
public final class WaterskinItem extends DrinkItem {
    /** The most any of them may be set to hold, which bounds the servings component, saved and sent. */
    public static final int MAX_CAPACITY = 64;
    /** The filled sprites a waterskin has, a third full to full. */
    private static final int SPRITES = 3;
    /** A bucket is three servings, the rate a cauldron uses, whatever it is poured into. */
    public static final int BUCKET_SERVINGS = 3;
    /** How often vanilla repeats a held right click, and so how much boiling each repeat adds. */
    public static final int BOIL_STEP_TICKS = 4;
    /** Steam every few steps rather than every one, which would bury the campfire. */
    private static final int STEAM_EVERY_STEPS = 3;

    /** Boiling in progress, per player. Only touched on the server thread. */
    private static final Map<Player, Boil> BOILING = new WeakHashMap<>();

    /**
     * How far one stack has boiled. {@code servings} and {@code quality} are what it held at the last
     * step: more water, or other water, since then means the progress no longer applies.
     */
    private record Boil(ItemStack stack, int servings, WaterQuality quality, int ticks) { }

    private final IntSupplier capacity;
    private final IntSupplier boilTicksPerServing;
    private final boolean spriteShowsServings;

    /** The leather waterskin: its capacity from the config, four by default, no boiling, a sprite per third. */
    public WaterskinItem(Properties properties) {
        this(properties, () -> ThirstConfig.get().waterskinCapacity, () -> 0, true);
    }

    /**
     * Capacity and boil time are asked for on each use rather than fixed, so the canteen and the flask
     * follow the config. A stack holding more than a lowered capacity keeps its water and takes no more.
     *
     * @param capacity            servings it holds when full, at most {@link #MAX_CAPACITY}
     * @param boilTicksPerServing ticks each serving takes to boil over a campfire, or 0 when it cannot
     * @param spriteShowsServings whether the item model dispatches on the servings left, as the
     *                            waterskin's does; a rigid vessel keeps one sprite and relies on its bar
     */
    public WaterskinItem(Properties properties, IntSupplier capacity, IntSupplier boilTicksPerServing,
                         boolean spriteShowsServings) {
        super(properties, null);
        this.capacity = capacity;
        this.boilTicksPerServing = boilTicksPerServing;
        this.spriteShowsServings = spriteShowsServings;
    }

    /** Whether {@code stack} is one of the carried containers: a waterskin, canteen or flask. */
    public static boolean is(ItemStack stack) {
        return stack.getItem() instanceof WaterskinItem;
    }

    /** How many servings {@code stack} holds when full, or 0 when it is not a carried container. */
    public static int capacity(ItemStack stack) {
        return stack.getItem() instanceof WaterskinItem vessel ? Math.max(1, vessel.capacity.getAsInt()) : 0;
    }

    /**
     * How long {@code stack} takes in a furnace: its per-serving boil time for every serving it holds,
     * or 0 when it is not a vessel that boils, or holds nothing a furnace would boil. The in-hand switch
     * does not turn this off; the furnace's own switch is its recipes'.
     */
    public static int furnaceTicks(ItemStack stack) {
        if (!(stack.getItem() instanceof WaterskinItem vessel) || !WaterPurity.boils(WaterPurity.quality(stack))) return 0;
        return servings(stack) * vessel.boilTicksPerServing.getAsInt();
    }

    /**
     * Gives a vessel coming out of a recipe the servings of the one that went in. A furnace recipe for
     * a vessel matches any fill, so its result records none; see {@code CookingRecipeMixin}.
     */
    public static void keepServings(ItemStack input, ItemStack result) {
        if (is(input) && is(result) && servings(result) == 0 && servings(input) > 0) {
            setServings(result, servings(input));
        }
    }

    /**
     * {@code result} with the servings of {@code input}, for a caller handed the recipe's own result
     * stack rather than an assembled copy: a copy when servings had to be added, else {@code result}
     * itself, so the recipe's stack is never changed. Before 1.21.2 the Fabric furnace takes its result
     * this way; see {@code com.thirstwastaken2.fabric.mixin.FurnaceMixin}.
     */
    public static ItemStack withServingsOf(ItemStack input, ItemStack result) {
        if (!is(input) || !is(result) || servings(result) != 0 || servings(input) <= 0) return result;
        ItemStack copy = result.copy();
        setServings(copy, servings(input));
        return copy;
    }

    /**
     * Ticks each serving in {@code stack} takes to boil over a campfire, or 0 when it cannot boil, which
     * is every vessel while the config switches boiling in hand off.
     */
    public static int boilTicksPerServing(ItemStack stack) {
        return stack.getItem() instanceof WaterskinItem vessel ? vessel.boilTicks() : 0;
    }

    private int boilTicks() {
        return ThirstConfig.get().enableBoilingInHand ? boilTicksPerServing.getAsInt() : 0;
    }

    public static int servings(ItemStack stack) {
        return ItemWaterData.servings(stack);
    }

    /** Whether {@code stack} is a carried container with room for more water. */
    public static boolean hasRoom(ItemStack stack) {
        return is(stack) && servings(stack) < capacity(stack);
    }

    public static boolean addWater(ItemStack stack, int purity, int amount) {
        return addWater(stack, WaterQuality.fresh(purity), amount);
    }

    public static boolean addWater(ItemStack stack, WaterQuality added, int amount) {
        int current = servings(stack);
        int capacity = capacity(stack);
        if (current >= capacity || amount <= 0) return false;

        int poured = Math.min(amount, capacity - current);
        WaterQuality mixed = current == 0 ? added : mix(WaterPurity.quality(stack), current, added, poured);
        setServings(stack, current + poured);
        WaterPurity.setQuality(stack, mixed);
        return true;
    }

    /**
     * Serving-weighted mixing, rounded down, so that one clean mouthful cannot talk a whole batch up
     * a grade. Salt is not averaged at all: a single salty serving turns the skin into sea water,
     * which is what keeps the sea worth avoiding.
     */
    private static WaterQuality mix(WaterQuality existing, int held, WaterQuality added, int poured) {
        if (existing instanceof WaterQuality.Fresh inside && added instanceof WaterQuality.Fresh pouring) {
            return WaterQuality.fresh((inside.purity() * held + pouring.purity() * poured) / (held + poured));
        }
        return WaterQuality.SALT;
    }

    /** Removes stored drinks, clearing their quality once the container becomes empty. */
    public static boolean removeWater(ItemStack stack, int amount) {
        int current = servings(stack);
        if (!is(stack) || current <= 0 || amount <= 0) return false;

        int remaining = Math.max(0, current - amount);
        setServings(stack, remaining);
        if (remaining == 0) clearWaterQuality(stack);
        return true;
    }

    /**
     * Using a boiling vessel on a lit campfire boils it one step. Anything it holds but salt water is
     * taken, even water that is already Clean or Pure, which heat leaves as it is: a player still holding
     * use when the boil finishes would otherwise start drinking. Anywhere else this passes, and the
     * vessel is drunk as usual.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        int boilTicks = boilTicks();
        if (boilTicks == 0 || servings(stack) == 0
                || !CampfireBlock.isLitCampfire(level.getBlockState(pos))) {
            return InteractionResult.PASS;
        }
        // No swing: the step repeats every few ticks, and a hand flailing at the fire reads as punching it.
        if (level.isClientSide() || context.getPlayer() == null) return InteractionResult.CONSUME;

        Player player = context.getPlayer();
        if (!(WaterPurity.quality(stack) instanceof WaterQuality.Fresh fresh)) {
            // Boiling leaves the salt behind; taking it out is distillation, which is on the roadmap.
            Vanilla.sendOverlayMessage(player, Component.translatable("thirstwastaken2.message.cannot_boil_salt"));
            return InteractionResult.CONSUME;
        }
        if (WaterPurity.boils(fresh)) boilStep(player, stack, fresh, boilTicks, (ServerLevel) level, pos);
        return InteractionResult.CONSUME;
    }

    /** One step of boiling, finishing the whole vessel Clean once every serving has had its time. */
    private static void boilStep(Player player, ItemStack stack, WaterQuality quality, int boilTicksPerServing,
                                 ServerLevel level, BlockPos pos) {
        int servings = servings(stack);
        Boil previous = BOILING.get(player);
        boolean resumes = previous != null && previous.stack() == stack
                && servings <= previous.servings() && quality.equals(previous.quality());
        int total = servings * boilTicksPerServing;
        int ticks = Math.min(total, (resumes ? previous.ticks() : 0) + BOIL_STEP_TICKS);

        if (ticks >= total) {
            BOILING.remove(player);
            WaterPurity.setQuality(stack, WaterPurity.boil(quality));
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0F, 1.0F);
            Vanilla.sendOverlayMessage(player, Component.translatable("thirstwastaken2.message.boiled"));
            return;
        }

        BOILING.put(player, new Boil(stack, servings, quality, ticks));
        Vanilla.sendOverlayMessage(player,
                Component.translatable("thirstwastaken2.message.boiling", ticks * 100 / total));
        if (ticks / BOIL_STEP_TICKS % STEAM_EVERY_STEPS == 0) {
            level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    1, 0.1, 0.0, 0.1, 0.02);
        }
    }

    /** Ticks {@code player} has boiled {@code stack} so far, or 0 when they are not boiling it. */
    public static int boilProgress(Player player, ItemStack stack) {
        Boil boil = BOILING.get(player);
        return boil != null && boil.stack() == stack ? boil.ticks() : 0;
    }

    @Override
    protected boolean canDrink(ItemStack stack) {
        return servings(stack) > 0;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        int current = servings(stack);
        // Vanilla's side effects only: a drink leaves the skin itself, one serving lighter.
        if (current > 0) drinkEffects(stack, level, entity);
        boolean creativePlayer = entity instanceof Player player && player.getAbilities().instabuild;
        if (!level.isClientSide() && current > 0 && !creativePlayer) {
            removeWater(stack, 1);
        }
        return stack;
    }

    /**
     * Right-click a slotted container with a water bottle or bucket on the cursor. A bottle adds one
     * serving and a bucket three, as far as there is room. Dynamic purity is retained and mixed.
     */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack waterskin, ItemStack carried, Slot slot,
                                            ClickAction action, Player player, SlotAccess carriedAccess) {
        if (action != ClickAction.SECONDARY || !hasRoom(waterskin)) return false;

        ItemStack remainder;
        int amount;
        if (isWaterBottle(carried)) {
            remainder = new ItemStack(Items.GLASS_BOTTLE);
            amount = 1;
        } else if (carried.is(Items.WATER_BUCKET)) {
            remainder = new ItemStack(Items.BUCKET);
            amount = BUCKET_SERVINGS;
        } else {
            return false;
        }

        if (!addWater(waterskin, WaterPurity.quality(carried), amount)) return false;
        if (!player.getAbilities().instabuild) consumeContainer(carried, remainder, player, carriedAccess);
        slot.setChanged();
        return true;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return servings(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.min(13, Math.round(13.0F * servings(stack) / capacity(stack)));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        if (!(WaterPurity.quality(stack) instanceof WaterQuality.Fresh fresh)) return 0xD8D2BE;
        return switch (fresh.purity()) {
            case 0 -> 0x8A5A2B;
            case 1 -> 0xB09A63;
            case 2 -> 0x3F76E4;
            default -> 0x42C8F5;
        };
    }

    private static boolean isWaterBottle(ItemStack stack) {
        if (!stack.is(Items.POTION)) return false;
        return Vanilla.holdsWaterPotion(stack);
    }

    private static void setServings(ItemStack stack, int servings) {
        ItemWaterData.setServings(stack, servings);
        if (!(stack.getItem() instanceof WaterskinItem vessel) || !vessel.spriteShowsServings) return;
        if (servings == 0) {
            Vanilla.clearModelSelector(stack);
        } else {
            Vanilla.setModelSelector(stack, ThirstItems.WATERSKIN_MODEL_INDEX, sprite(servings, capacity(stack)));
        }
    }

    /**
     * Which of the three filled sprites {@code servings} out of {@code capacity} show: a third full, two
     * thirds or full, rounded up, so any water at all shows and a skin over a lowered capacity looks full.
     */
    public static int sprite(int servings, int capacity) {
        return (SPRITES * Math.min(servings, capacity) + capacity - 1) / capacity;
    }

    private static void clearWaterQuality(ItemStack stack) {
        ItemWaterData.clearQuality(stack);
    }

    private static void consumeContainer(ItemStack carried, ItemStack remainder, Player player,
                                         SlotAccess carriedAccess) {
        if (carried.getCount() == 1) {
            carriedAccess.set(remainder);
            return;
        }
        carried.shrink(1);
        Vanilla.placeItemBackInInventory(player, remainder);
    }
}
