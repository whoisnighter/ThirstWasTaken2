package com.thirstwastaken2.block;

import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.item.WaterskinItem;
import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Filling a cooling tub with its coolant, alone or as the distiller's tub half, and the right-click
 * shortcuts on a whole distiller that pour water into its boiler and draw it from its basin.
 *
 * <p>For the coolant any water does, sea water too, and whatever grade: it only carries heat off the
 * coil and never reaches the distillate, so it costs no drinking water. It is poured once and stays. A bucket, a bottle or a terracotta bowl
 * empties into it; a waterskin, canteen or flask pours what it holds, up to a bucket. Where water
 * evaporates, as in the Nether, the tub refuses it the way a hanging pot does.
 */
public final class DistillerInteractions {
    private DistillerInteractions() { }

    public static InteractionResult fillTub(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        boolean tub = state.is(ThirstBlocks.COOLING_TUB)
                || state.is(ThirstBlocks.COPPER_DISTILLER) && state.getValue(DistillerBlock.PART) == DistillerBlock.Part.TUB;
        if (!tub || state.getValue(DistillerBlock.COOLED) || player.isSpectator()) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        Pour pour = pour(held);
        if (pour == null) return InteractionResult.PASS;
        if (HangingPotInteractions.evaporates(level, pos)) return HangingPotInteractions.hissed(level);
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (pour.empty() == null) {
            WaterskinItem.removeWater(held, Math.min(WaterskinItem.servings(held), WaterskinItem.BUCKET_SERVINGS));
        } else {
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, pour.empty()));
        }
        level.setBlock(pos, state.setValue(DistillerBlock.COOLED, true), Block.UPDATE_ALL);
        level.playSound(null, pos, pour.sound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
        return InteractionResult.SUCCESS_SERVER;
    }

    /**
     * The shortcuts on either half of a whole distiller: a container of water pours into the boiler, an
     * empty one draws Pure water from the basin, as at a hanging pot. A bucket moves its three servings
     * or nothing. A waterskin, canteen or flask draws, and pours when its holder is sneaking, the way its
     * sneak-use pours it out anywhere else. A tub still without its coolant takes the water as that
     * first, in {@link #fillTub}, which runs before this. Any other click that is not sneaking opens the
     * distiller's GUI.
     */
    public static InteractionResult useMachine(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(ThirstBlocks.COPPER_DISTILLER) || !DistillerBlock.isWhole(state) || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        DistillerBlockEntity machine = DistillerBlock.machine(level, pos, state);
        if (machine == null) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        boolean carried = WaterskinItem.is(held);
        boolean sneaking = player.isSecondaryUseActive();
        if (carried ? sneaking : !sneaking) {
            int water = DistillerWater.held(held);
            int room = machine.boilerRoom();
            if (water > 0 && room > 0 && (carried || water <= room)) {
                if (level.isClientSide()) return InteractionResult.SUCCESS;
                int poured = machine.pour(Math.min(water, room), WaterPurity.quality(held));
                ItemStack emptied = DistillerWater.emptied(held, poured);
                level.playSound(null, pos, DistillerWater.pourSound(held), SoundSource.BLOCKS, 1.0F, 1.0F);
                player.setItemInHand(hand, carried ? emptied : ItemUtils.createFilledResult(held, player, emptied));
                level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
                return InteractionResult.SUCCESS_SERVER;
            }
        }
        if (!sneaking) {
            int room = DistillerWater.room(held);
            int needed = carried ? 1 : room;
            if (room > 0 && machine.basinServings() >= needed) {
                if (level.isClientSide()) return InteractionResult.SUCCESS;
                int drawn = machine.draw(Math.min(room, machine.basinServings()));
                ItemStack filled = DistillerWater.filled(held, drawn);
                level.playSound(null, pos, DistillerWater.fillSound(held), SoundSource.BLOCKS, 1.0F, 1.0F);
                player.setItemInHand(hand, carried ? filled : ItemUtils.createFilledResult(held, player, filled));
                level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
                return InteractionResult.SUCCESS_SERVER;
            }
            // Anything else, an empty hand included, opens the GUI, as a furnace does; sneaking leaves
            // the click to the item, so a block can still be placed against it.
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            player.openMenu(machine);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }

    /** What {@code held} pours into a tub, or {@code null} when it holds no water. */
    private static Pour pour(ItemStack held) {
        if (held.is(Items.WATER_BUCKET)) return new Pour(new ItemStack(Items.BUCKET), SoundEvents.BUCKET_EMPTY);
        if (held.is(Items.POTION) && WaterPurity.isWaterContainer(held)) {
            return new Pour(new ItemStack(Items.GLASS_BOTTLE), SoundEvents.BOTTLE_EMPTY);
        }
        if (held.is(ThirstItems.TERRACOTTA_WATER_BOWL)) {
            return new Pour(new ItemStack(ThirstItems.TERRACOTTA_BOWL), SoundEvents.BUCKET_EMPTY);
        }
        if (WaterskinItem.is(held) && WaterskinItem.servings(held) > 0) return new Pour(null, SoundEvents.BOTTLE_EMPTY);
        return null;
    }

    /** The container left once poured, {@code null} for a vessel that keeps what it does not pour. */
    private record Pour(ItemStack empty, SoundEvent sound) { }
}
