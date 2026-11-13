package com.thirstwastaken2.platform;

import com.thirstwastaken2.ThirstWasTaken2;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Every vanilla call whose shape differs between the supported Minecraft versions.
 *
 * <p>Callers outside this package use the same signature on every version; the branches live here and
 * nowhere else. Keep each method a one-liner over vanilla — game logic belongs in its own package.
 */
public final class Vanilla {
    //? if >=1.21.9 {
    private static final net.minecraft.network.chat.FontDescription DROPLET_FONT =
            new net.minecraft.network.chat.FontDescription.Resource(ThirstWasTaken2.id("droplets"));
    //?} else
    //private static final Identifier DROPLET_FONT = ThirstWasTaken2.id("droplets");

    /** Vanilla's description id for the water cauldron, see {@link #isWaterCauldron}. */
    private static final String WATER_CAULDRON = "block.minecraft.water_cauldron";
    /** The tag 1.20.1 keeps custom model data in, see {@link #modelSelector}. */
    private static final String CUSTOM_MODEL_DATA = "CustomModelData";
    /** Set while {@code BlocksMixin} constructs the water cauldron, on 1.21.1 and 1.20.1 only. */
    private static boolean buildingWaterCauldron;

    private Vanilla() { }

    /**
     * Whether a block that is still inside its own constructor is vanilla's water cauldron. From 1.21.2
     * a block is handed its id before it is built, so its description id answers. On 1.21.1 the id only
     * exists once the block is registered, and asking earlier caches the wrong name for good, so
     * {@code BlocksMixin} marks the construction instead.
     */
    public static boolean isWaterCauldron(Block block) {
        //? if >1.21.1 {
        return WATER_CAULDRON.equals(block.getDescriptionId());
        //?} else
        //return buildingWaterCauldron;
    }

    /** Runs a block construction marked as the water cauldron's, or not. Called by {@code BlocksMixin} on 1.21.1. */
    public static <T> T buildingWaterCauldron(boolean waterCauldron, java.util.function.Supplier<T> construction) {
        buildingWaterCauldron = waterCauldron;
        try {
            return construction.get();
        } finally {
            buildingWaterCauldron = false;
        }
    }

    /** The registry id of a built-in or modded item. */
    public static Identifier itemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    /** A mob effect by id, or {@code null} when nothing is registered under it, such as another mod's. */
    public static Holder<MobEffect> mobEffect(Identifier id) {
        //? if >=1.21.2 {
        return BuiltInRegistries.MOB_EFFECT.get(id).<Holder<MobEffect>>map(holder -> holder).orElse(null);
        //?} elif >=1.20.5 {
        /*return BuiltInRegistries.MOB_EFFECT.getHolder(id).<Holder<MobEffect>>map(holder -> holder).orElse(null);
        *///?} else {
        /*return BuiltInRegistries.MOB_EFFECT.getHolder(ResourceKey.create(Registries.MOB_EFFECT, id))
                .<Holder<MobEffect>>map(holder -> holder).orElse(null);
        *///?}
    }

    /**
     * The instance of {@code effect} an entity has, or {@code null}. Before 1.20.5 an entity's effects
     * are keyed by the effect itself rather than by its holder, and so are the next three.
     */
    public static net.minecraft.world.effect.MobEffectInstance getEffect(
            net.minecraft.world.entity.LivingEntity entity, Holder<MobEffect> effect) {
        //? if >=1.20.5 {
        return entity.getEffect(effect);
        //?} else {
        /*return entity.getEffect(effect.value());
        *///?}
    }

    /** Whether an entity has {@code effect}. See {@link #getEffect}. */
    public static boolean hasEffect(net.minecraft.world.entity.LivingEntity entity, Holder<MobEffect> effect) {
        //? if >=1.20.5 {
        return entity.hasEffect(effect);
        //?} else {
        /*return entity.hasEffect(effect.value());
        *///?}
    }

    /** {@code effect} for {@code ticks} at {@code amplifier}, with particles and an icon. See {@link #getEffect}. */
    public static net.minecraft.world.effect.MobEffectInstance effectInstance(Holder<MobEffect> effect, int ticks,
                                                                             int amplifier) {
        return effectInstance(effect, ticks, amplifier, false, true, true);
    }

    /** {@code effect} with every flag vanilla's constructor takes. See {@link #getEffect}. */
    public static net.minecraft.world.effect.MobEffectInstance effectInstance(Holder<MobEffect> effect, int ticks,
            int amplifier, boolean ambient, boolean visible, boolean showIcon) {
        //? if >=1.20.5 {
        return new net.minecraft.world.effect.MobEffectInstance(effect, ticks, amplifier, ambient, visible, showIcon);
        //?} else {
        /*return new net.minecraft.world.effect.MobEffectInstance(effect.value(), ticks, amplifier, ambient, visible, showIcon);
        *///?}
    }

    /** Vanilla's Poison as a holder, which it already is from 1.20.5. See {@link #getEffect}. */
    public static Holder<MobEffect> poison() {
        //? if >=1.20.5 {
        return net.minecraft.world.effect.MobEffects.POISON;
        //?} else {
        /*return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(net.minecraft.world.effect.MobEffects.POISON);
        *///?}
    }

    /**
     * A built-in loot table's id. From 1.20.5 loot tables are a registry and the constants are keys;
     * before it they are the ids themselves.
     */
    //? if >=1.20.5 {
    public static Identifier lootTableId(ResourceKey<net.minecraft.world.level.storage.loot.LootTable> table) {
        return table.identifier();
    }
    //?} else {
    /*public static Identifier lootTableId(Identifier table) {
        return table;
    }
    *///?}

    /**
     * Awards one criterion of an advancement, or does nothing if a datapack has removed it. 1.20.2 put
     * an advancement behind a holder that carries its id.
     */
    public static void awardAdvancement(ServerPlayer player, Identifier id, String criterion) {
        net.minecraft.server.MinecraftServer server = player.level().getServer();
        if (server == null) return;
        //? if >=1.20.5 {
        net.minecraft.advancements.AdvancementHolder advancement = server.getAdvancements().get(id);
        //?} else {
        /*net.minecraft.advancements.Advancement advancement = server.getAdvancements().getAdvancement(id);
        *///?}
        if (advancement != null) player.getAdvancements().award(advancement, criterion);
    }

    /**
     * Twice the total level of the enchantments on a player's armour that protect against
     * {@code source}. 1.21 made enchantments data, which read the level to decide.
     */
    public static float damageProtection(ServerPlayer player, DamageSource source) {
        //? if >=1.21 {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getDamageProtection(level(player), player, source);
        //?} else {
        /*return net.minecraft.world.item.enchantment.EnchantmentHelper.getDamageProtection(player.getArmorSlots(), source);
        *///?}
    }

    /**
     * How far a player reaches to use a block. 1.20.5 made it an attribute; before it survival reached
     * 4.5 blocks and creative 5.
     */
    public static double blockReach(net.minecraft.world.entity.player.Player player) {
        //? if >=1.20.5 {
        return player.blockInteractionRange();
        //?} else {
        /*return player.getAbilities().instabuild ? 5.0 : 4.5;
        *///?}
    }

    /** Whether a player uses no items up, as in creative mode. A method of its own from 1.20.5. */
    public static boolean hasInfiniteMaterials(net.minecraft.world.entity.player.Player player) {
        //? if >=1.20.5 {
        return player.hasInfiniteMaterials();
        //?} else {
        /*return player.getAbilities().instabuild;
        *///?}
    }

    /**
     * The wisp of steam over boiling water. White smoke arrived in 1.20.3; before it a cloud puff is the
     * nearest white particle.
     */
    public static net.minecraft.core.particles.SimpleParticleType steamParticle() {
        //? if >=1.20.5 {
        return net.minecraft.core.particles.ParticleTypes.WHITE_SMOKE;
        //?} else {
        /*return net.minecraft.core.particles.ParticleTypes.CLOUD;
        *///?}
    }

    /**
     * Registers one of the mod's items. From 1.21.2 an item has to know its own id before it is
     * built, so the properties are stamped with it first.
     */
    public static Item registerItem(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ThirstWasTaken2.id(name));
        //? if >=1.21.2 {
        Item item = factory.apply(properties.setId(key));
        //?} else
        //Item item = factory.apply(properties);
        DefaultData.itemBuilt(properties, item);
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    /**
     * Registers one of the mod's blocks. From 1.21.2 a block has to know its own id before it is
     * built, the same as an item.
     */
    public static <T extends Block> T registerBlock(String name, Function<BlockBehaviour.Properties, T> factory,
                                                    BlockBehaviour.Properties properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, ThirstWasTaken2.id(name));
        //? if >=1.21.2 {
        T block = factory.apply(properties.setId(key));
        //?} else
        //T block = factory.apply(properties);
        return Registry.register(BuiltInRegistries.BLOCK, key, block);
    }

    /**
     * Registers the item that places {@code block}, under the block's own id. From 1.21.2 an item names
     * itself after its own id unless told to use the block's name, which a block item always wants.
     */
    public static Item registerBlockItem(Block block, Item.Properties properties) {
        return registerBlockItem(block, props -> new BlockItem(block, props), properties);
    }

    /** {@link #registerBlockItem(Block, Item.Properties)} for a block item class of the mod's own. */
    public static Item registerBlockItem(Block block, Function<Item.Properties, ? extends BlockItem> factory,
                                         Item.Properties properties) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        //? if >=1.21.2 {
        return registerItem(name, factory::apply, properties.useBlockDescriptionPrefix());
        //?} else
        //return registerItem(name, factory::apply, properties);
    }

    /**
     * Registers a block entity type for {@code blocks} under the mod's id {@code name}. Run it from the
     * block entity type registry's {@code Loader.onRegister}, after the blocks exist.
     */
    public static <T extends BlockEntity> BlockEntityType<T> registerBlockEntity(
            String name, BiFunction<BlockPos, BlockState, T> factory, Block... blocks) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ThirstWasTaken2.id(name),
                blockEntityType(factory, blocks));
    }

    /**
     * A block entity type for {@code blocks}, not registered. 1.21.2 replaced the builder with a public
     * constructor. Both take vanilla's supplier interface, which the Fabric nodes' access widener opens
     * and NeoForge and Forge make public, as they do the constructor.
     */
    public static <T extends BlockEntity> BlockEntityType<T> blockEntityType(
            BiFunction<BlockPos, BlockState, T> factory, Block... blocks) {
        //? if >=1.21.2 {
        return new BlockEntityType<>(factory::apply, java.util.Set.of(blocks));
        //?} else
        //return BlockEntityType.Builder.of(factory::apply, blocks).build(null);
    }

    /**
     * Registers a menu type under the mod's id {@code name}, made from the window id and the player's
     * inventory alone: whatever else the client shows comes through the menu's own data slots. Run it
     * from the menu registry's {@code Loader.onRegister}.
     */
    public static <T extends AbstractContainerMenu> MenuType<T> registerMenu(
            String name, BiFunction<Integer, Inventory, T> factory) {
        return Registry.register(BuiltInRegistries.MENU, ThirstWasTaken2.id(name), menuType(factory));
    }

    /**
     * A menu type, not registered. Its constructor is private in vanilla on every version; the Fabric
     * nodes' access widener opens it, and NeoForge's and Forge's access transformers make it public.
     */
    public static <T extends AbstractContainerMenu> MenuType<T> menuType(BiFunction<Integer, Inventory, T> factory) {
        return new MenuType<>(factory::apply, net.minecraft.world.flag.FeatureFlags.VANILLA_SET);
    }

    /**
     * How long {@code fuel} burns in a furnace, in ticks, judged in {@code entity}'s level: 0 for
     * anything that is not fuel, or with no level to ask. Before 1.21.2 a static table; then the
     * level's fuel values; from 26.3 the item's cooking fuel component, resolved in the loot context a
     * furnace gives it, which names the block entity as the container doing the work. On NeoForge
     * 1.21.1 and Forge the loader has fuel of its own, so callers go through {@code Loader.burnTime}.
     */
    public static <T extends BlockEntity & Container> int burnTime(T entity, ItemStack fuel) {
        Level level = entity.getLevel();
        if (fuel.isEmpty() || level == null) return 0;
        //? if >=26.3 {
        if (!(level instanceof ServerLevel server)) return 0;
        net.minecraft.world.level.storage.loot.LootContext context =
                new net.minecraft.world.level.storage.loot.LootContext.Builder(
                        new net.minecraft.world.level.storage.loot.LootParams.Builder(server)
                                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE,
                                        entity.getBlockState())
                                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY,
                                        entity)
                                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
                                        net.minecraft.world.phys.Vec3.atCenterOf(entity.getBlockPos()))
                                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.CONTAINER,
                                        entity)
                                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CONTAINER_PROCESS))
                        .create(java.util.Optional.empty());
        return net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt.getFromItem(fuel,
                net.minecraft.core.component.DataComponents.COOKING_FUEL,
                net.minecraft.world.item.component.CookingFuel::burnTime, context, 0);
        //?} elif >=1.21.2 {
        /*return level.fuelValues().burnDuration(fuel);
        *///?} else {
        /*return net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.getFuel()
                .getOrDefault(fuel.getItem(), 0);
        *///?}
    }

    /**
     * Whether {@code fuel} burns in a furnace, asked without a furnace, as a slot asks on the client:
     * the static table before 1.21.2, the level's fuel values until 26.3, the cooking fuel component
     * from then. Callers go through {@code Loader.isFuel}, for the same reason as {@link #burnTime}.
     */
    public static boolean isFuel(Level level, ItemStack fuel) {
        //? if >=26.3 {
        return fuel.has(net.minecraft.core.component.DataComponents.COOKING_FUEL);
        //?} elif >=1.21.2 {
        /*return level.fuelValues().isFuel(fuel);
        *///?} else {
        /*return net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.isFuel(fuel);
        *///?}
    }

    /**
     * What is left of {@code stack}'s item once it is used up, as a lava bucket leaves a bucket; empty for
     * most items. Before 1.21.2 an item, then a stack, and from 26.1 a template, which may be missing.
     */
    public static ItemStack craftingRemainder(ItemStack stack) {
        //? if >=26.1 {
        net.minecraft.world.item.ItemStackTemplate remainder = stack.getItem().getCraftingRemainder();
        return remainder == null ? ItemStack.EMPTY : remainder.create();
        //?} elif >=1.21.2 {
        /*return stack.getItem().getCraftingRemainder();
        *///?} else {
        /*Item remainder = stack.getItem().getCraftingRemainingItem();
        return remainder == null ? ItemStack.EMPTY : new ItemStack(remainder);
        *///?}
    }

    /**
     * Whether the position is Nether-like, i.e. water placed there boils away. Replaced
     * {@code DimensionType#ultraWarm} and moved to environment attributes in 1.21.9.
     */
    public static boolean waterEvaporates(Level level, BlockPos pos) {
        //? if >=1.21.9 {
        // getValue is generic and hands back a boxed Boolean; unboxing it directly would throw on a
        // dimension that does not define the attribute, on the exhaustion path of every tick.
        return Boolean.TRUE.equals(level.environmentAttributes().getValue(
                net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES, pos));
        //?} else
        //return level.dimensionType().ultraWarm();
    }

    /**
     * Switches a style to the mod's droplet bitmap font, without a shadow: bitmap glyphs keep their own
     * palette, and a shadow would smear their 1px outlines. Styles could only turn the shadow off from
     * 1.21.4, so on 1.21.1 the droplets are drawn with one.
     */
    public static Style dropletFont(Style style) {
        //? if >=1.21.4 {
        return style.withFont(DROPLET_FONT).withoutShadow();
        //?} else
        //return style.withFont(DROPLET_FONT);
    }

    /** The sound of drinking a potion. A later release turned the constant into a registry holder. */
    public static SoundEvent drinkSound() {
        //? if >1.21.1 {
        return SoundEvents.GENERIC_DRINK.value();
        //?} else
        //return SoundEvents.GENERIC_DRINK;
    }

    /** Whether a command source may run operator commands: permission level 2, vanilla's game masters. */
    public static boolean isGameMaster(CommandSourceStack source) {
        //? if >=1.21.11 {
        return net.minecraft.commands.Commands.LEVEL_GAMEMASTERS.check(source.permissions());
        //?} else
        //return source.hasPermission(2);
    }

    /** Whether a command source may run owner-only commands: permission level 4. */
    public static boolean isOwner(CommandSourceStack source) {
        //? if >=1.21.11 {
        return net.minecraft.commands.Commands.LEVEL_OWNERS.check(source.permissions());
        //?} else
        //return source.hasPermission(4);
    }

    /** The level a server player is in. A release after 1.21.1 narrowed {@code level()} to return it. */
    public static ServerLevel level(ServerPlayer player) {
        //? if >1.21.1 {
        return player.level();
        //?} else
        //return player.serverLevel();
    }

    /** Whether the naturalRegeneration game rule is on in the player's level. 1.21.11 made game rules typed values. */
    public static boolean naturalRegeneration(ServerPlayer player) {
        //? if >=1.21.11 {
        return level(player).getGameRules().get(net.minecraft.world.level.gamerules.GameRules.NATURAL_HEALTH_REGENERATION);
        //?} else
        //return level(player).getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_NATURAL_REGENERATION);
    }

    /** Whether the waterSourceConversion game rule is on, so water refills itself. 1.21.11 made game rules typed values. */
    public static boolean waterSourceConversion(ServerLevel level) {
        //? if >=1.21.11 {
        return level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.WATER_SOURCE_CONVERSION);
        //?} else
        //return level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_WATER_SOURCE_CONVERSION);
    }

    /**
     * Takes the fluid at {@code pos} out of the world the way a bucket does, emptying a water block or
     * draining a waterlogged one, and whether anything was taken. {@code pickupBlock} took no entity on
     * 1.20.1.
     */
    public static boolean pickupFluid(net.minecraft.world.entity.player.Player player, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof net.minecraft.world.level.block.BucketPickup pickup)) return false;
        //? if >=1.20.2 {
        return !pickup.pickupBlock(player, level, pos, state).isEmpty();
        //?} else
        //return !pickup.pickupBlock(level, pos, state).isEmpty();
    }

    /** Damages a player from the server. 1.21.2 split a server-only {@code hurtServer} out of {@code hurt}. */
    public static void hurt(ServerPlayer player, DamageSource source, float amount) {
        //? if >=1.21.2 {
        player.hurtServer(level(player), source, amount);
        //?} else
        //player.hurt(source, amount);
    }

    /**
     * Custom model data holding {@code value} at float index {@code index}, which is what the item
     * models dispatch on. 1.21.4 turned custom model data into lists; before it the component was a
     * single integer, which is enough because no item of this mod reads more than one index. On 1.20.1,
     * which has no components, it is that same integer as the stack's {@code CustomModelData} tag, and
     * this returns it boxed: only datagen and {@link #selectsModel} use the value itself.
     */
    //? if >=1.21.4 {
    public static net.minecraft.world.item.component.CustomModelData modelSelector(int index, int value) {
        Float[] floats = new Float[index + 1];
        java.util.Arrays.fill(floats, 0.0F);
        floats[index] = (float) value;
        return new net.minecraft.world.item.component.CustomModelData(
                java.util.List.of(floats), java.util.List.of(), java.util.List.of(), java.util.List.of());
    }
    //?} elif >=1.20.5 {
    /*public static net.minecraft.world.item.component.CustomModelData modelSelector(int index, int value) {
        return new net.minecraft.world.item.component.CustomModelData(value);
    }
    *///?} else {
    /*public static Integer modelSelector(int index, int value) {
        return value;
    }
    *///?}

    /** Sets a stack's model selector, see {@link #modelSelector(int, int)}. */
    public static void setModelSelector(ItemStack stack, int index, int value) {
        //? if >=1.20.5 {
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA, modelSelector(index, value));
        //?} else {
        /*stack.getOrCreateTag().putInt(CUSTOM_MODEL_DATA, value);
        *///?}
    }

    /** Takes a stack's model selector away, so its item model falls back to its plain sprite. */
    public static void clearModelSelector(ItemStack stack) {
        //? if >=1.20.5 {
        stack.remove(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA);
        //?} else {
        /*stack.removeTagKey(CUSTOM_MODEL_DATA);
        *///?}
    }

    /** Item properties whose stacks start out with a model selector, see {@link #modelSelector(int, int)}. */
    public static Item.Properties modelSelectorByDefault(Item.Properties properties, int index, int value) {
        //? if >=1.20.5 {
        return properties.component(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,
                modelSelector(index, value));
        //?} else {
        /*return DefaultData.add(properties, tag -> tag.putInt(CUSTOM_MODEL_DATA, value));
        *///?}
    }

    /**
     * The model selector a stack carries, or {@code null}. Only for comparing and printing: its type is
     * whatever the version stores.
     */
    public static Object modelSelectorOf(ItemStack stack) {
        //? if >=1.20.5 {
        return stack.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA);
        //?} else {
        /*net.minecraft.nbt.CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(CUSTOM_MODEL_DATA, net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)
                ? tag.getInt(CUSTOM_MODEL_DATA) : null;
        *///?}
    }

    /** Whether a stack carries exactly the model selector {@code modelSelector(index, value)}. */
    public static boolean selectsModel(ItemStack stack, int index, int value) {
        return modelSelector(index, value).equals(modelSelectorOf(stack));
    }

    /** A vanilla water bottle: a potion whose contents are plain water. */
    public static ItemStack waterBottle() {
        //? if >=1.20.5 {
        return net.minecraft.world.item.alchemy.PotionContents.createItemStack(
                net.minecraft.world.item.Items.POTION, net.minecraft.world.item.alchemy.Potions.WATER);
        //?} else {
        /*return net.minecraft.world.item.alchemy.PotionUtils.setPotion(
                new ItemStack(net.minecraft.world.item.Items.POTION), net.minecraft.world.item.alchemy.Potions.WATER);
        *///?}
    }

    /**
     * Whether nothing above {@code pos} blocks motion, so the sky is open over it. Water itself counts
     * in the {@code MOTION_BLOCKING} heightmap, so {@code pos} should be the top of a water column.
     */
    public static boolean skyAbove(net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ())
                <= pos.getY() + 1;
    }

    /**
     * An Awkward Potion: a potion that is not water, which has its own thirst value and must never be
     * mistaken for a water bottle by a lookup that caches per item.
     */
    public static ItemStack awkwardPotion() {
        //? if >=1.20.5 {
        return net.minecraft.world.item.alchemy.PotionContents.createItemStack(
                net.minecraft.world.item.Items.POTION, net.minecraft.world.item.alchemy.Potions.AWKWARD);
        //?} else {
        /*return net.minecraft.world.item.alchemy.PotionUtils.setPotion(
                new ItemStack(net.minecraft.world.item.Items.POTION), net.minecraft.world.item.alchemy.Potions.AWKWARD);
        *///?}
    }

    /**
     * A villager offer of {@code gives} for {@code emeralds} emeralds. 1.20.5 priced offers in
     * {@code ItemCost}; before it the price was a stack.
     */
    public static net.minecraft.world.item.trading.MerchantOffer emeraldOffer(int emeralds, ItemStack gives,
                                                                              int maxUses, int xp, float priceMultiplier) {
        //? if >=1.20.5 {
        return new net.minecraft.world.item.trading.MerchantOffer(
                new net.minecraft.world.item.trading.ItemCost(net.minecraft.world.item.Items.EMERALD, emeralds),
                gives, maxUses, xp, priceMultiplier);
        //?} else {
        /*return new net.minecraft.world.item.trading.MerchantOffer(
                new ItemStack(net.minecraft.world.item.Items.EMERALD, emeralds), gives, maxUses, xp, priceMultiplier);
        *///?}
    }

    /** Whether a stack's potion contents are plain water, whatever the item. */
    public static boolean holdsWaterPotion(ItemStack stack) {
        //? if >=1.20.5 {
        net.minecraft.world.item.alchemy.PotionContents potion =
                stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        return potion != null && potion.is(net.minecraft.world.item.alchemy.Potions.WATER);
        //?} else {
        /*// The potion's id as the tag spells it, rather than PotionUtils.getPotion, which parses it into a
        // new id on every call, and tooltips ask this every frame.
        net.minecraft.nbt.CompoundTag tag = stack.getTag();
        if (tag == null) return false;
        String potion = tag.getString("Potion");
        return potion.equals("minecraft:water") || potion.equals("water");
        *///?}
    }

    /**
     * Whether vanilla's cauldron interactions, which mods add to, have one for {@code stack} on an empty
     * cauldron or, with {@code water}, a water cauldron. The interaction may still refuse, such as a bottle
     * on a full cauldron; an item with none does nothing to the cauldron at all. 26.1 made the maps
     * dispatchers asked with the stack, which answer the do-nothing default when nothing matches.
     */
    public static boolean cauldronHasInteraction(boolean water, ItemStack stack) {
        //? if >=26.1 {
        net.minecraft.core.cauldron.CauldronInteraction interaction = (water
                ? net.minecraft.core.cauldron.CauldronInteractions.WATER
                : net.minecraft.core.cauldron.CauldronInteractions.EMPTY).get(stack);
        return interaction != null && interaction != net.minecraft.core.cauldron.CauldronInteraction.DEFAULT;
        //?} elif >=1.20.5 {
        /*return (water ? net.minecraft.core.cauldron.CauldronInteraction.WATER
                : net.minecraft.core.cauldron.CauldronInteraction.EMPTY).map().containsKey(stack.getItem());
        *///?} else {
        /*return (water ? net.minecraft.core.cauldron.CauldronInteraction.WATER
                : net.minecraft.core.cauldron.CauldronInteraction.EMPTY).containsKey(stack.getItem());
        *///?}
    }

    /**
     * The item model a stack is pointed at, or {@code null}. Always {@code null} before 1.21.2, where the
     * {@code minecraft:item_model} component does not exist.
     */
    public static Identifier itemModelOf(ItemStack stack) {
        //? if >=1.21.2 {
        return stack.get(net.minecraft.core.component.DataComponents.ITEM_MODEL);
        //?} else
        //return null;
    }

    /**
     * A tag's string, or {@code fallback} when it has none. From 1.21.5 a tag's getters answer with a
     * fallback of their own; before it they answered with an empty value.
     */
    public static String getString(net.minecraft.nbt.CompoundTag tag, String key, String fallback) {
        //? if >=1.21.5 {
        return tag.getStringOr(key, fallback);
        //?} else
        //return tag.contains(key, net.minecraft.nbt.Tag.TAG_STRING) ? tag.getString(key) : fallback;
    }

    /** A tag's number as an int, or {@code fallback} when it has none. See {@link #getString}. */
    public static int getInt(net.minecraft.nbt.CompoundTag tag, String key, int fallback) {
        //? if >=1.21.5 {
        return tag.getIntOr(key, fallback);
        //?} else
        //return tag.contains(key, net.minecraft.nbt.Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : fallback;
    }

    /**
     * Adds one int to the block entity data a block item carries, and does nothing when it carries
     * none. From 1.21.9 that data names its block entity type rather than keeping it under {@code id}.
     */
    public static void putBlockEntityInt(ItemStack stack, String key, int value) {
        //? if >=1.21.9 {
        net.minecraft.world.item.component.TypedEntityData<net.minecraft.world.level.block.entity.BlockEntityType<?>> data =
                stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) return;
        net.minecraft.nbt.CompoundTag tag = data.copyTagWithoutId();
        tag.putInt(key, value);
        stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,
                net.minecraft.world.item.component.TypedEntityData.of(data.type(), tag));
        //?} elif >=1.20.5 {
        /*if (!stack.has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA)) return;
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,
                stack, tag -> tag.putInt(key, value));
        *///?} else {
        /*net.minecraft.nbt.CompoundTag tag = stack.getTagElement("BlockEntityTag");
        if (tag != null) tag.putInt(key, value);
        *///?}
    }

    /** Shows {@code message} in a player's action bar. 26.1 gave it a method of its own. */
    public static void sendOverlayMessage(net.minecraft.world.entity.player.Player player, net.minecraft.network.chat.Component message) {
        //? if >=26.1 {
        player.sendOverlayMessage(message);
        //?} else
        //player.displayClientMessage(message, true);
    }

    /** Whether a stack is used with the drinking animation. 1.21.2 renamed {@code UseAnim} to {@code ItemUseAnimation}. */
    public static boolean isDrinkAnimation(ItemStack stack) {
        //? if >=1.21.2 {
        return stack.getUseAnimation() == net.minecraft.world.item.ItemUseAnimation.DRINK;
        //?} else
        //return stack.getUseAnimation() == net.minecraft.world.item.UseAnim.DRINK;
    }

    /**
     * Gives a player an item the inventory has no room for in hand, dropping what does not fit. 26.3
     * asks every such call whether the client already predicted it; the mod's callers (a waterskin's
     * remainder, the Sophisticated Drinking upgrade's empty container) run on the server alone, which is
     * what the older signature meant anyway.
     */
    public static void placeItemBackInInventory(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        //? if >=26.3 {
        player.getInventory().placeItemBackInInventory(stack, net.minecraft.util.Prediction.SERVER_ONLY);
        //?} else
        //player.getInventory().placeItemBackInInventory(stack);
    }

    /**
     * A loot pool rolled {@code rolls} times. 26.3 split the number providers into an int and a float
     * family and put every one behind a {@code Holder}, so the value a pool takes is a different type.
     */
    public static net.minecraft.world.level.storage.loot.LootPool.Builder lootPool(int rolls) {
        //? if >=26.3 {
        return net.minecraft.world.level.storage.loot.LootPool.lootPool().setRolls(
                net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.exactly(rolls));
        //?} else {
        /*return net.minecraft.world.level.storage.loot.LootPool.lootPool().setRolls(
                net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(rolls));
        *///?}
    }

    /** A loot function setting a stack's count anywhere between {@code min} and {@code max}. See {@link #lootPool}. */
    public static net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction.Builder<?> setCount(
            int min, int max) {
        //? if >=26.3 {
        return net.minecraft.world.level.storage.loot.functions.SetItemCountFunction.setCount(
                net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.between(min, max));
        //?} else {
        /*return net.minecraft.world.level.storage.loot.functions.SetItemCountFunction.setCount(
                net.minecraft.world.level.storage.loot.providers.number.UniformGenerator.between(min, max));
        *///?}
    }
}
