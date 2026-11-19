package com.thirstwastaken2.purity;

import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.data.ThirstManager;
import com.thirstwastaken2.effect.ThirstEffects;
import com.thirstwastaken2.effect.WaterSickness;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.item.WaterskinItem;
import com.thirstwastaken2.platform.ItemWaterData;
import com.thirstwastaken2.platform.Vanilla;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WaterPurity {
    public static final int MIN = 0;
    public static final int MAX = 3;
    /**
     * Clean, the grade heat leaves water at and never goes past. Boiling kills what makes water unsafe
     * but leaves what is dissolved in it, so taking water to Pure is the distiller's work, or a sand
     * filter's. {@link #boil} and {@link #boilStep} are the only two places that know this cap; every
     * heat source goes through one of them.
     */
    public static final int BOILED = 2;
    /**
     * What a cauldron holds, as one value rather than a grade plus a flag: {@link #BLOCK_UNSET} for
     * a cauldron nothing has been poured into, 1-4 for the four grades, and {@link #BLOCK_SALT} for
     * sea water. A separate boolean could not work, because vanilla hands a freshly placed block the
     * first value of every property it has, and for a boolean that value is {@code true}.
     */
    public static final IntegerProperty BLOCK_PURITY = IntegerProperty.create("purity", 0, 5);
    public static final int BLOCK_UNSET = 0;
    public static final int BLOCK_SALT = 5;

    private static final TagKey<Biome> STAGNANT_WATER = TagKey.create(
            Registries.BIOME, ThirstWasTaken2.id("stagnant_water"));
    /**
     * Biomes whose water is the sea's besides {@code is_ocean} and {@code is_beach}, such as No Man's
     * Land's Mud Beach, which replaces part of a beach without that tag. Ours, so a coastal biome joins
     * without changing a vanilla tag other mods place structures by.
     */
    private static final TagKey<Biome> SEA_WATER = TagKey.create(
            Registries.BIOME, ThirstWasTaken2.id("sea_water"));
    /** Water that is Pure wherever it lies. Empty as shipped; a tag, so a data pack can add a fluid. */
    private static final TagKey<Fluid> PURE_WATER = TagKey.create(
            Registries.FLUID, ThirstWasTaken2.id("pure_water"));
    /**
     * Water that is salty wherever it lies, such as Spelunkery's Spring Water: a sluice sifts rock salt
     * and sulfur out of it, so it is mineral water, not a mountain stream. With {@code enableSeaWater}
     * off it is graded by its biome like any other water.
     */
    private static final TagKey<Fluid> SALT_WATER = TagKey.create(
            Registries.FLUID, ThirstWasTaken2.id("salt_water"));
    private static final int SURFACE_MOUNTAIN_Y = 100;
    /** The best grade water out of the sky's reach can have: Murky. */
    private static final int COVERED_MAX = 1;
    /** How far up a water column is followed to find its surface. */
    private static final int COVERED_COLUMN = 16;
    private static final int SALTY_EXHAUSTION = 8;
    /**
     * How hard a drink of sea water leaves the player Parched: II, like vanilla's pufferfish gives
     * Hunger III. How long is {@code seaWaterParchedSeconds}, 30 by default. It is given without
     * particles: a dry mouth is felt, not seen, and the icon and the sandy thirst bar already show it.
     * Nausea lasts {@code seaWaterNauseaSeconds}, 8 by default: the original's 5 barely warped the
     * screen, because vanilla fades Nausea in over 150 ticks and starts fading it out 60 before it ends.
     */
    private static final int SALT_PARCHED_LEVEL = 1;

    /** Bounds of the contamination score a sample is graded from. It is never stored on an item. */
    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 100;

    /**
     * The bowl's model variant for salt water, one past the four grades. Only the mod's own bowl has a
     * salty sprite: sea water in a vanilla bottle or bucket looks like any water on every version, and
     * its tooltip says Salty.
     */
    private static final int SALT_BOWL_MODEL = 4;

    /** Purity that has to be looked up from the config instead of being baked into the item. */
    private static final int PURITY_FROM_CONFIG = -1;

    /**
     * {@code drawsWater}: an empty container a full water cauldron fills, as a bucket. {@code vessel}: for
     * another mod's container of plain water, what it swaps with and how much; see {@link #vessel}.
     */
    private record ItemInfo(boolean container, boolean plainWater, int staticPurity, boolean drawsWater,
                            Vessel vessel) {
        ItemInfo(boolean container, boolean plainWater, int staticPurity, boolean drawsWater) {
            this(container, plainWater, staticPurity, drawsWater, null);
        }
    }

    private static final ItemInfo NOT_A_CONTAINER = new ItemInfo(false, false, PURITY_FROM_CONFIG, false);
    private static final ItemInfo DRAWS_WATER = new ItemInfo(false, false, PURITY_FROM_CONFIG, true);
    private static final Map<Item, ItemInfo> INFO = new ConcurrentHashMap<>();

    private WaterPurity() { }

    public static boolean isWaterContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (WaterskinItem.is(stack)) return WaterskinItem.servings(stack) > 0;
        if (info(stack.getItem()).container()) return true;
        // Water bottles are plain potions distinguished only by their contents.
        return Vanilla.holdsWaterPotion(stack);
    }

    /**
     * Whether a full water cauldron fills {@code stack} with water, so that the container it becomes is
     * stamped with the cauldron's grade: a glass bottle, a bucket, Miner's Delight's copper cup, Bountiful
     * Fares' cup.
     */
    public static boolean drawsFromCauldron(ItemStack stack) {
        return !stack.isEmpty() && info(stack.getItem()).drawsWater();
    }

    /**
     * Another mod's container of plain water that is emptied or filled whole, {@code servings} at a time,
     * and becomes {@code swap} when it is: the full one the empty one, and the other way round.
     */
    public record Vessel(Item swap, int servings) { }

    /**
     * {@code item} as another mod's container of plain water, or null: Miner's Delight's water cup and
     * copper cup, a bucket each, Meadow's wooden water bucket and wooden bucket, a bucket each, Bountiful
     * Fares' water cup and cup, a bottle each, and Cold Sweat's filled
     * and empty waterskin, a bottle each, at Cold Sweat's default of one use. Whether it is the full one is
     * {@link #isWaterContainer}. Not drinks made with water, such as tea, nor Croptopia's water bottle, an
     * ingredient with no grade that a bucket makes sixteen of. Vanilla's containers are not here; callers
     * handle them themselves.
     */
    public static Vessel vessel(Item item) {
        return info(item).vessel();
    }

    /** Water-only drinks are blocked at a full thirst bar, unlike drinks with other gameplay uses. */
    public static boolean isPlainWaterDrink(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (WaterskinItem.is(stack)) return WaterskinItem.servings(stack) > 0;
        if (stack.is(ThirstItems.TERRACOTTA_WATER_BOWL)) return true;
        if (stack.is(Items.POTION) && Vanilla.holdsWaterPotion(stack)) return true;
        return info(stack.getItem()).plainWater();
    }

    /**
     * Whether {@code item} is one of the mod's own water containers, the filled bowl or a carried vessel,
     * which hold plain water and nothing else.
     */
    public static boolean isOwnWaterVessel(Item item) {
        return item == ThirstItems.TERRACOTTA_WATER_BOWL || item instanceof WaterskinItem;
    }

    /**
     * The grade of the fresh water in {@code stack}. Salt water has no grade, so unless salinity has
     * already been ruled out, ask {@link #quality(ItemStack)} instead of this.
     */
    public static int get(ItemStack stack) {
        Integer purity = ItemWaterData.grade(stack);
        if (purity != null) return purity;
        int staticPurity = info(stack.getItem()).staticPurity();
        return staticPurity == PURITY_FROM_CONFIG ? ThirstConfig.get().defaultQuality : staticPurity;
    }

    public static WaterQuality quality(ItemStack stack) {
        return isSalty(stack) ? WaterQuality.SALT : WaterQuality.fresh(get(stack));
    }

    public static boolean isSalty(ItemStack stack) {
        return ItemWaterData.salty(stack);
    }

    /** Whether a container already carries a sampled quality of its own. */
    public static boolean isStamped(ItemStack stack) {
        return ItemWaterData.hasGrade(stack) || isSalty(stack);
    }

    public static ItemStack set(ItemStack stack, int purity) {
        return setQuality(stack, WaterQuality.fresh(purity));
    }

    public static ItemStack setQuality(ItemStack stack, WaterQuality quality) {
        if (quality instanceof WaterQuality.Fresh fresh) {
            ItemWaterData.setFresh(stack, fresh.purity());
        } else {
            ItemWaterData.setSalty(stack);
        }
        syncModel(stack, quality);
        return stack;
    }

    /**
     * A copy of {@code stack} without its quality, the plain container other mods compare against: both
     * components come off. Salt water loses its salt too, so only hand this to code that gets the
     * quality back from the caller.
     */
    public static ItemStack unstamped(ItemStack stack) {
        ItemStack plain = stack.copy();
        ItemWaterData.clearQuality(plain);
        return plain;
    }

    /**
     * What heat makes of {@code quality} in one go, as a pot or a vessel over a fire does: fresh water
     * below {@link #BOILED} comes out Clean, and everything else as it went in. Heat never lowers a grade,
     * never makes Pure water of Clean, and never takes the salt out.
     */
    public static WaterQuality boil(WaterQuality quality) {
        if (quality instanceof WaterQuality.Fresh fresh && fresh.purity() < BOILED) return WaterQuality.fresh(BOILED);
        return quality;
    }

    /**
     * One pass of a heat source that raises water a grade at a time, as Cold Sweat's Boiler does: one
     * grade up, but not past {@link #BOILED}, and never down.
     */
    public static WaterQuality boilStep(WaterQuality quality) {
        if (!(quality instanceof WaterQuality.Fresh fresh)) return quality;
        int grade = fresh.purity();
        return WaterQuality.fresh(Math.max(grade, Math.min(grade + 1, BOILED)));
    }

    /** Whether heat would still change {@code quality}: fresh water below {@link #BOILED}. */
    public static boolean boils(WaterQuality quality) {
        return quality instanceof WaterQuality.Fresh fresh && fresh.purity() < BOILED;
    }

    /** Raises the grade of fresh water. Salt water has no grade to raise and comes back unchanged. */
    public static ItemStack purify(ItemStack stack, int levels) {
        if (isWaterContainer(stack) && quality(stack) instanceof WaterQuality.Fresh fresh) {
            setQuality(stack, WaterQuality.fresh(fresh.purity() + levels));
        }
        return stack;
    }

    /**
     * Samples only when water is collected or drunk. The fixed 5x3x5 inspection has no entity
     * lookup, allocation per block or tick-time cost, while biome tags keep modded worlds extensible.
     */
    public static WaterQuality sampleAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        WaterQuality stored = storedQuality(state);
        if (stored != null) return stored;

        ThirstConfig config = ThirstConfig.get();
        FluidState fluid = state.getFluidState();
        if (!fluid.is(FluidTags.WATER)) return WaterQuality.fresh(config.defaultQuality);
        // Before the sea check: a Pure fluid is Pure even by the coast.
        if (fluid.is(PURE_WATER)) return WaterQuality.fresh(MAX);
        if (config.enableSeaWater && fluid.is(SALT_WATER)) return WaterQuality.SALT;

        var biome = level.getBiome(pos);
        // The sea is not a grade of fresh water, so it never reaches the scoring below. This also
        // spares the neighbourhood scan on every coastline.
        if (config.enableSeaWater && (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH)
                || biome.is(SEA_WATER))) {
            return WaterQuality.SALT;
        }

        int score;
        if (biome.is(STAGNANT_WATER)) score = 85;
        else if (biome.is(BiomeTags.IS_RIVER)) score = 42;
        else if (biome.is(BiomeTags.IS_MOUNTAIN)) score = 28;
        else if (biome.is(BiomeTags.IS_JUNGLE) || biome.is(BiomeTags.IS_SAVANNA)
                || biome.is(BiomeTags.IS_BADLANDS)) score = 70;
        else score = 55;

        float temperature = biome.value().getBaseTemperature();
        if (temperature >= 1.5F) score += 10;
        else if (temperature <= 0.15F) score -= 10;
        // Diverges from 1.x, which also cleaned water below y 32 as a deep aquifer: underground water
        // is no longer better for being deep.
        if (pos.getY() > SURFACE_MOUNTAIN_Y) score -= 5;
        if (!fluid.isSource()) score -= 5;
        score += nearbyPollution(level, pos);
        int grade = grade(score);
        // Not in the original. Water the sky does not reach, a cave pool or a source under a roof, is
        // at best Murky: rain does not renew it, so it needs treating like any other risky water.
        // Checked last, as the costliest test, and only when the score left something to cap.
        if (grade > COVERED_MAX && !openToTheSky(level, pos)) grade = COVERED_MAX;
        return WaterQuality.fresh(grade);
    }

    /**
     * Whether the water at {@code pos} lies open to the sky: the top of its column, at most
     * {@link #COVERED_COLUMN} blocks up, with nothing above it that the {@code MOTION_BLOCKING} heightmap
     * sees, leaves included. The client receives that heightmap with every chunk, so Jade grades the
     * same water the same way the server does.
     */
    private static boolean openToTheSky(Level level, BlockPos pos) {
        BlockPos.MutableBlockPos top = pos.mutable();
        for (int up = 0; up < COVERED_COLUMN; up++) {
            if (!level.getFluidState(top.move(0, 1, 0)).is(FluidTags.WATER)) {
                top.move(0, -1, 0);
                break;
            }
        }
        return Vanilla.skyAbove(level, top);
    }

    /** Makes the player ill, or not, from the water in {@code stack}, and returns whether it quenches. */
    public static boolean applyEffects(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer) || !isWaterContainer(stack)) return true;
        if (quality(stack) instanceof WaterQuality.Fresh fresh) {
            // Diverges from the original's single Nausea and Poison roll, which also applied Hunger:
            // bad water now makes you ill by difficulty, and every fresh drink still quenches.
            WaterSickness.drink(player, fresh);
            return true;
        }
        ThirstManager.addExhaustion(player, SALTY_EXHAUSTION);
        ThirstConfig config = ThirstConfig.get();
        if (config.seaWaterNauseaSeconds > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, config.seaWaterNauseaSeconds * 20));
        }
        // Not in the original. Salt makes you thirstier at once: the body spends more water
        // getting rid of it than the drink brought in. Bad fresh water dries you out only
        // later, once it makes you ill, so it does not make you Parched.
        if (config.seaWaterParchedSeconds > 0) {
            player.addEffect(Vanilla.effectInstance(ThirstEffects.PARCHED, config.seaWaterParchedSeconds * 20,
                    SALT_PARCHED_LEVEL, false, false, true));
        }
        return false;
    }

    /**
     * The quenched a drink of {@code quality} gives out of {@code base}, by the config's
     * {@code quenchedPercent} for its grade, rounded down: by default Dirty none and Murky half, so bad
     * water fills the bar but not for long, the way rotten flesh gives almost no saturation. Salt water
     * quenches nothing anyway and is left alone.
     */
    public static int quenched(WaterQuality quality, int base) {
        if (!(quality instanceof WaterQuality.Fresh fresh)) return base;
        return base * ThirstConfig.get().quenchedPercent[fresh.purity()] / 100;
    }

    /**
     * The grade rain leaves in a cauldron or a hanging pot, Clean by default. Chosen rather than left to
     * {@code defaultQuality}, so collecting rain is a decision with a known outcome.
     */
    public static int rainwaterQuality() {
        return ThirstConfig.get().rainwaterQuality;
    }

    /** The grade a pointed dripstone leaves in a cauldron, having filtered the water: Pure by default. */
    public static int dripstoneQuality() {
        return ThirstConfig.get().dripstoneQuality;
    }

    /** @return a fresh copy of the grade line, see {@link TooltipLines}. */
    public static Component tooltip(int purity) {
        return TooltipLines.PURITY[Mth.clamp(purity, MIN, MAX)].copy();
    }

    /** The one line salt water gets. It replaces the grade line rather than joining it. */
    public static Component saltTooltip() {
        return TooltipLines.SALT.copy();
    }

    /**
     * Adds the stored quality properties to the water cauldron and nothing else. Powder snow cauldrons
     * are {@code LayeredCauldronBlock}s too, but they never hold water, and the two properties would
     * multiply their blockstates tenfold for nothing.
     *
     * <p>Runs inside the block's constructor, before {@code Blocks.WATER_CAULDRON} is assigned or the
     * block is registered, which is why identifying it is left to {@link Vanilla#isWaterCauldron}.
     */
    public static void addCauldronProperties(Block block, StateDefinition.Builder<Block, BlockState> builder) {
        if (Vanilla.isWaterCauldron(block)) builder.add(BLOCK_PURITY);
    }

    /** What a cauldron holds, or {@code null} when nothing has been poured into it yet. */
    public static WaterQuality storedQuality(BlockState state) {
        if (!state.hasProperty(BLOCK_PURITY)) return null;
        int stored = state.getValue(BLOCK_PURITY);
        if (stored == BLOCK_UNSET) return null;
        return stored == BLOCK_SALT ? WaterQuality.SALT : WaterQuality.fresh(stored - 1);
    }

    /** The blockstate value that stores {@code quality} in a cauldron. */
    public static int storedValue(WaterQuality quality) {
        // Grades are offset by one so that zero can act as "unset".
        return quality instanceof WaterQuality.Fresh fresh ? fresh.purity() + 1 : BLOCK_SALT;
    }

    /** The grade a sampled contamination score falls into. */
    private static int grade(int score) {
        int clamped = Mth.clamp(score, MIN_SCORE, MAX_SCORE);
        if (clamped <= 15) return 3;
        if (clamped <= 35) return 2;
        if (clamped <= 65) return 1;
        return 0;
    }

    /**
     * The name of grade {@code purity} in its tooltip colour, so anywhere else that names a grade, such
     * as the config screen, looks the way the tooltip does. A fresh copy each call.
     */
    public static Component purityName(int purity) {
        return TooltipLines.PURITY[Mth.clamp(purity, MIN, MAX)].copy();
    }

    private static String purityKey(int purity) {
        return switch (purity) {
            case 0 -> "thirst.water.dirty";
            case 1 -> "thirst.water.murky";
            case 2 -> "thirst.water.clean";
            default -> "thirst.water.pure";
        };
    }

    /**
     * The grade ramp runs warm to cool so that all four stay apart on a dark tooltip. Salt sits off
     * that ramp on purpose: on this tooltip, blue means drinkable.
     */
    private static int purityColor(int purity) {
        return switch (purity) {
            case 0 -> 0xB0632E;
            case 1 -> 0xC2A878;
            case 2 -> 0x74B8E0;
            default -> 0x4FD6FF;
        };
    }

    /**
     * Tooltip lines are rebuilt every frame a stack is hovered, so each one is built once and copied
     * out. A copy shares the translatable contents, and with them the parsed translation, while the
     * caller stays free to restyle its own line in place. Nested so that nothing is built during block
     * bootstrap, which is when {@code WaterPurity} itself loads.
     */
    private static final class TooltipLines {
        static final Component[] PURITY = new Component[MAX + 1];
        static final Component SALT = Component.translatable("thirst.water.salty").withStyle(Style.EMPTY.withColor(0xE6DFC8));

        static {
            for (int purity = MIN; purity <= MAX; purity++) {
                PURITY[purity] = Component.translatable(purityKey(purity)).withStyle(Style.EMPTY.withColor(purityColor(purity)));
            }
        }
    }

    /**
     * What the blocks around {@code origin} add to its contamination score: mud or mangrove roots, and
     * farmland, each counted once. Public so a gametest can check it apart from the biome, which the test
     * world picks at random and which decides whether the added points cross a grade boundary.
     */
    public static int nearbyPollution(Level level, BlockPos origin) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        boolean muddy = false;
        boolean agricultural = false;
        for (int dy = -1; dy <= 1 && !(muddy && agricultural); dy++) {
            for (int dx = -2; dx <= 2 && !(muddy && agricultural); dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockState nearby = level.getBlockState(cursor.setWithOffset(origin, dx, dy, dz));
                    if (nearby.is(Blocks.MUD) || nearby.is(Blocks.MANGROVE_ROOTS)
                            || nearby.is(Blocks.MUDDY_MANGROVE_ROOTS)) {
                        muddy = true;
                    } else if (nearby.is(Blocks.COMPOSTER) || nearby.is(Blocks.FARMLAND)) {
                        agricultural = true;
                    }
                    if (muddy && agricultural) break;
                }
            }
        }
        return (muddy ? 15 : 0) + (agricultural ? 10 : 0);
    }

    /** Keeps the sprite in step with the contents, so that salt water never looks drinkable. */
    private static void syncModel(ItemStack stack, WaterQuality quality) {
        if (stack.is(ThirstItems.TERRACOTTA_WATER_BOWL)) {
            int variant = quality instanceof WaterQuality.Fresh fresh ? fresh.purity() : SALT_BOWL_MODEL;
            Vanilla.setModelSelector(stack, ThirstItems.BOWL_MODEL_INDEX, variant);
        }
    }

    private static ItemInfo info(Item item) {
        ItemInfo cached = INFO.get(item);
        return cached != null ? cached : INFO.computeIfAbsent(item, WaterPurity::resolve);
    }

    private static ItemInfo resolve(Item item) {
        if (item == Items.WATER_BUCKET || item == ThirstItems.TERRACOTTA_WATER_BOWL) {
            return new ItemInfo(true, item == ThirstItems.TERRACOTTA_WATER_BOWL, PURITY_FROM_CONFIG, false);
        }
        if (item == Items.GLASS_BOTTLE || item == Items.BUCKET) return DRAWS_WATER;
        if (item == Items.POTION) {
            // Only water bottles count, which isWaterContainer decides per stack.
            return NOT_A_CONTAINER;
        }

        Identifier id = Vanilla.itemId(item);
        String namespace = id.getNamespace();
        String path = id.getPath();

        if (namespace.equals("farmersdelight")) {
            // Only the two bottled drinks were registered as containers by the original mod.
            boolean container = path.equals("melon_juice") || path.equals("apple_cider");
            return new ItemInfo(container, false, 3, false);
        }
        if (namespace.equals("cold_sweat")) {
            // Cold Sweat's filled waterskin, graded when it is filled. Not plain water: its default use
            // pours it over the player and a sip also warms or cools, so a full thirst bar stops neither.
            if (path.equals("filled_waterskin")) {
                return new ItemInfo(true, false, PURITY_FROM_CONFIG, false, vessel(namespace, "waterskin", 1));
            }
            return path.equals("waterskin")
                    ? new ItemInfo(false, false, PURITY_FROM_CONFIG, false, vessel(namespace, "filled_waterskin", 1))
                    : NOT_A_CONTAINER;
        }
        // Miner's Delight names itself minersdelight on 1.21.1 and miners_delight on 1.20.1. Its copper cup
        // is a small bucket: the water cup holds a bucket of water, not drinkable, and the empty cup draws
        // one from a full cauldron. Graded like the bucket, so the cup cannot turn sea water fresh.
        if (namespace.equals("minersdelight") || namespace.equals("miners_delight")) {
            if (path.equals("water_cup")) {
                return new ItemInfo(true, false, PURITY_FROM_CONFIG, false, vessel(namespace, "copper_cup", 3));
            }
            return path.equals("copper_cup")
                    ? new ItemInfo(false, false, PURITY_FROM_CONFIG, true, vessel(namespace, "water_cup", 3))
                    : NOT_A_CONTAINER;
        }
        // Bountiful Fares' ceramic cup is a glass bottle: the water cup is a serving of plain water, drunk
        // like a water bottle, and the empty cup, registered as jar, draws one from a full cauldron.
        if (namespace.equals("bountifulfares")) {
            if (path.equals("water_cup")) {
                return new ItemInfo(true, true, PURITY_FROM_CONFIG, false, vessel(namespace, "jar", 1));
            }
            return path.equals("jar")
                    ? new ItemInfo(false, false, PURITY_FROM_CONFIG, true, vessel(namespace, "water_cup", 1))
                    : NOT_A_CONTAINER;
        }
        // Meadow's wooden bucket is a bucket: the wooden water bucket holds a bucket of water, not drinkable,
        // and the empty one draws one from a full cauldron. Graded like the bucket, so it cannot turn sea
        // water fresh. Its scoop is stamped in src/main/meadow.
        if (namespace.equals("meadow")) {
            if (path.equals("wooden_water_bucket")) {
                return new ItemInfo(true, false, PURITY_FROM_CONFIG, false, vessel(namespace, "wooden_bucket", 3));
            }
            return path.equals("wooden_bucket")
                    ? new ItemInfo(false, false, PURITY_FROM_CONFIG, true, vessel(namespace, "wooden_water_bucket", 3))
                    : NOT_A_CONTAINER;
        }
        return NOT_A_CONTAINER;
    }

    /**
     * A vessel swapping with the registered item {@code namespace:path}, or null when there is none. Only
     * from {@link #resolve}, so once per item.
     */
    private static Vessel vessel(String namespace, String path, int servings) {
        Item swap = BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath(namespace, path)).orElse(Items.AIR);
        return swap == Items.AIR ? null : new Vessel(swap, servings);
    }
}
