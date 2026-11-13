package com.thirstwastaken2.sophisticated.drinking;

import com.thirstwastaken2.api.ThirstApi;
import com.thirstwastaken2.data.ThirstData;
import com.thirstwastaken2.data.ThirstManager;
import com.thirstwastaken2.neoforge.WaterContainerFluids;
import com.thirstwastaken2.neoforge.WaterFluids;
import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IFilteredUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Drinks from the backpack when the thirst bar is low, the way the Feeding upgrade eats: bottles,
 * waterskins, bowls and other drinks with a thirst value, and water straight from a Tank upgrade in the
 * same backpack, one serving at a time.
 *
 * <p>Everything drunk goes through the same code as drinking by hand, so sickness, advancements and
 * thirst values match: an item through {@code ItemStack.finishUsingItem}, where the mod's own hook runs,
 * and tank water through {@link ThirstManager#drinkItem} as a water bottle of its grade.
 *
 * <p>The cleanest water goes first, then drinks that are not water at all. Water below the lowest grade
 * the upgrade is set to is never drunk, and salt water never is.
 */
public final class DrinkingUpgradeWrapper extends UpgradeWrapperBase<DrinkingUpgradeWrapper, DrinkingUpgradeItem>
        implements ITickableUpgrade, IFilteredUpgrade {
    /** The Feeding upgrade's timings: a short wait while the player is still thirsty, a long one after. */
    private static final int COOLDOWN = 100;
    private static final int STILL_THIRSTY_COOLDOWN = 10;
    /** How far a placed storage reaches for players, again the Feeding upgrade's. */
    private static final int RANGE = 3;

    public static final DrinkAt DEFAULT_DRINK_AT = DrinkAt.HALF;
    /** Clean, so water the player has not treated is never picked on its own. */
    public static final int DEFAULT_MIN_PURITY = 2;

    /** Milk takes every effect away, which is not something to do to a player unasked. */
    private static final TagKey<Item> MILK = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "drinks/milk"));

    /** Nothing to drink. Water ranks above it by grade, and any other drink at {@link #OTHER_DRINK}. */
    private static final int NONE = -1;
    private static final int OTHER_DRINK = 0;

    private final FilterLogic filterLogic;

    public DrinkingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
        super(storageWrapper, upgrade, upgradeSaveHandler);
        filterLogic = new FilterLogic(upgrade, upgradeSaveHandler, upgradeItem.getFilterSlotCount(),
                DrinkingUpgradeWrapper::canFilter, ModCoreDataComponents.FILTER_ATTRIBUTES);
    }

    @Override
    public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
        if (level.isClientSide() || isInCooldown(level)) return;
        boolean stillThirsty = false;
        if (entity instanceof Player player) {
            stillThirsty = drinkAndStillThirsty(player, level);
        } else {
            // A placed storage, or a backpack on something other than a player, serves whoever is close.
            for (Player player : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(RANGE))) {
                stillThirsty |= drinkAndStillThirsty(player, level);
            }
        }
        setCooldown(level, stillThirsty ? STILL_THIRSTY_COOLDOWN : COOLDOWN);
    }

    private boolean drinkAndStillThirsty(Player player, Level level) {
        int missing = missingThirst(player);
        return missing > 0 && drink(player, level, missing) && missingThirst(player) > 0;
    }

    /**
     * Zero while the bar is full, and while thirst does not apply to the player at all. The upgrade
     * drinks for thirst only: topping quenched up at a full bar is a choice it leaves to the player.
     */
    private static int missingThirst(Player player) {
        ThirstData data = ThirstManager.get(player);
        if (!data.enabled() || player.getAbilities().invulnerable) return 0;
        return ThirstData.MAX - data.thirst();
    }

    private boolean drink(Player player, Level level, int missing) {
        DrinkAt drinkAt = getDrinkAt();
        int minPurity = getMinPurity();

        DrinkingStorage storage = new DrinkingStorage(storageWrapper);
        int bestSlot = -1;
        int bestRank = NONE;
        for (int slot = 0; slot < storage.slots(); slot++) {
            int rank = rank(storage.get(slot), missing, drinkAt, minPurity);
            if (rank > bestRank) {
                bestRank = rank;
                bestSlot = slot;
            }
        }

        FluidStack serving = tankServing(storage, missing, drinkAt, minPurity);
        // At the same grade the tank goes first: it leaves no empty bottle behind to find room for.
        if (!serving.isEmpty() && rank(WaterFluids.quality(serving)) >= bestRank && drinkFromTank(player, level, storage, serving)) {
            return true;
        }
        if (bestSlot < 0) return false;
        drinkFromSlot(player, level, storage, bestSlot);
        return true;
    }

    private int rank(ItemStack stack, int missing, DrinkAt drinkAt, int minPurity) {
        if (!isDrink(stack) || !filterLogic.matchesFilter(stack)) return NONE;
        int[] value = ThirstApi.thirstValues(stack);
        if (value == null || !drinkAt.allows(missing, value[0])) return NONE;
        if (!WaterPurity.isWaterContainer(stack)) return OTHER_DRINK;
        return acceptedRank(WaterPurity.quality(stack), minPurity);
    }

    private static int acceptedRank(WaterQuality quality, int minPurity) {
        return quality instanceof WaterQuality.Fresh fresh && fresh.purity() >= minPurity ? rank(quality) : NONE;
    }

    private static int rank(WaterQuality quality) {
        return quality instanceof WaterQuality.Fresh fresh ? OTHER_DRINK + 1 + fresh.purity() : NONE;
    }

    /** A serving of the cleanest water the settings accept from any one tank, or empty. */
    private static FluidStack tankServing(DrinkingStorage storage, int missing, DrinkAt drinkAt, int minPurity) {
        FluidStack best = FluidStack.EMPTY;
        int bestRank = NONE;
        for (int tank = 0; tank < storage.tanks(); tank++) {
            FluidStack fluid = storage.tank(tank);
            if (!WaterFluids.isWater(fluid) || fluid.getAmount() < WaterContainerFluids.SERVING) continue;
            int rank = acceptedRank(WaterFluids.quality(fluid), minPurity);
            if (rank > bestRank) {
                bestRank = rank;
                best = fluid.copyWithAmount(WaterContainerFluids.SERVING);
            }
        }
        if (best.isEmpty()) return best;
        int[] value = ThirstApi.thirstValues(waterBottle(WaterFluids.quality(best)));
        return value != null && drinkAt.allows(missing, value[0]) ? best : FluidStack.EMPTY;
    }

    private static boolean drinkFromTank(Player player, Level level, DrinkingStorage storage, FluidStack serving) {
        if (!storage.drain(serving)) return false;
        ThirstManager.drinkItem(player, waterBottle(WaterFluids.quality(serving)));
        playDrinkSound(player, level);
        return true;
    }

    /** The bottle a serving of tank water is drunk as, so it counts exactly as much as one. */
    private static ItemStack waterBottle(WaterQuality quality) {
        return WaterPurity.setQuality(PotionContents.createItemStack(Items.POTION, Potions.WATER), quality);
    }

    private static void drinkFromSlot(Player player, Level level, DrinkingStorage storage, int slot) {
        ItemStack stack = storage.get(slot);
        ItemStack drunk = stack.copyWithCount(1);
        storage.set(slot, stack.copyWithCount(stack.getCount() - 1));
        // ItemStack's finishUsingItem, not the item's: the mod hands out thirst, sickness and advancements
        // at the head of that one, as for a drink finished by hand.
        ItemStack result = EventHooks.onItemUseFinish(player, drunk.copy(), 0, drunk.finishUsingItem(level, player));
        playDrinkSound(player, level);
        if (result.isEmpty()) return;
        // The empty bottle, bowl or lighter waterskin goes back where it came from, or to the player.
        ItemStack left = storage.insert(result);
        if (!left.isEmpty()) Vanilla.placeItemBackInInventory(player, left);
    }

    private static void playDrinkSound(Player player, Level level) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS,
                0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
    }

    /**
     * Something the upgrade may drink: a drink with a thirst value that does nothing else a player would
     * mind losing. Potions are the Alchemy upgrade's, water bottles apart, and milk and ominous bottles
     * stay the player's own choice.
     */
    static boolean isDrink(ItemStack stack) {
        return canFilter(stack) && ThirstApi.restoresThirst(stack);
    }

    /**
     * What a filter slot takes: {@link #isDrink} by item, so an empty waterskin can still be set as a
     * filter.
     */
    static boolean canFilter(ItemStack stack) {
        if (stack.isEmpty() || stack.is(Items.OMINOUS_BOTTLE) || stack.is(MILK)) return false;
        PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
        if (potion != null && !potion.is(Potions.WATER)) return false;
        if (ThirstApi.thirstValues(stack.getItem()) == null) return false;
        return Vanilla.isDrinkAnimation(stack) || WaterPurity.isPlainWaterDrink(stack);
    }

    @Override
    public FilterLogic getFilterLogic() {
        return filterLogic;
    }

    /** The basic upgrade always drinks at its defaults; only the advanced one shows these settings. */
    public DrinkAt getDrinkAt() {
        return upgradeItem.isAdvanced() ? upgrade.getOrDefault(DrinkingUpgrade.DRINK_AT, DEFAULT_DRINK_AT) : DEFAULT_DRINK_AT;
    }

    public void setDrinkAt(DrinkAt drinkAt) {
        upgrade.set(DrinkingUpgrade.DRINK_AT, drinkAt);
        save();
    }

    public int getMinPurity() {
        return upgradeItem.isAdvanced() ? upgrade.getOrDefault(DrinkingUpgrade.MIN_PURITY, DEFAULT_MIN_PURITY) : DEFAULT_MIN_PURITY;
    }

    public void setMinPurity(int minPurity) {
        upgrade.set(DrinkingUpgrade.MIN_PURITY, Math.clamp(minPurity, WaterPurity.MIN, WaterPurity.MAX));
        save();
    }
}
