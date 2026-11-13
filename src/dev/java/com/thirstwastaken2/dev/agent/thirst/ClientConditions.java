package com.thirstwastaken2.dev.agent.thirst;

import com.google.gson.JsonObject;
import com.thirstwastaken2.dev.agent.core.AgentException;
import com.thirstwastaken2.dev.agent.core.AgentRequest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

/**
 * The {@code wait_until} conditions only a client can judge: whether its world is in, which screen is
 * open, what a slot of the open menu holds, and a block as the player's own world has it.
 */
final class ClientConditions {
    /** Looks in a row {@code world_ready} has to hold for unless a request says otherwise: two seconds. */
    static final int WORLD_READY_HOLD = 40;

    private ClientConditions() { }

    static void register() {
        WaitUntil.condition("world_ready", WORLD_READY_HOLD, request -> worldReady(
                request.has("radius") ? request.integer("radius", 0, 8) : 2, request.flag("allowScreen", false)));

        WaitUntil.condition("screen_open", 1, request -> {
            String wanted = request.string("screen");
            return seen -> {
                Screen screen = AgentClientVanilla.screen(Minecraft.getInstance());
                seen.addProperty("screen", screen == null ? null : screen.getClass().getName());
                if (wanted.equals("none")) return screen == null;
                if (screen == null) return false;
                return wanted.equals("any") || wanted.equals(screen.getClass().getName())
                        || wanted.equals(screen.getClass().getSimpleName());
            };
        });

        WaitUntil.condition("slot_not_empty", 1, request -> {
            SlotRef slot = SlotRef.of(request);
            return seen -> !slot.read(seen).isEmpty();
        });

        WaitUntil.condition("slot_matches", 1, request -> {
            SlotRef slot = SlotRef.of(request);
            String item = request.string("item");
            Integer count = request.has("count") ? request.integer("count") : null;
            return seen -> {
                ItemStack stack = slot.read(seen);
                if (stack.isEmpty()) return false;
                if (!item.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) return false;
                return count == null || stack.getCount() == count;
            };
        });

        // Over the dedicated server's: on a client, the world the player sees, which the server keeps
        // in step with its own.
        WaitUntil.condition("block_state", 1, request -> {
            BlockPos pos = WaitUntil.position(request);
            return seen -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.level == null) {
                    seen.addProperty("error", "no world is loaded");
                    return false;
                }
                return WaitUntil.blockMatches(minecraft.level, pos, request, seen);
            };
        });
    }

    /**
     * The world is in and can be played: a player in a level with no screen in front of it (a loading
     * screen, a prompt), the chunks within {@code radius} of the player loaded on the client, and on a
     * singleplayer client the integrated server running with the player in it, so the next
     * {@code server.command} reaches someone.
     */
    static WaitUntil.Condition worldReady(int radius, boolean allowScreen) {
        return seen -> {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            Screen screen = AgentClientVanilla.screen(minecraft);
            seen.addProperty("level", minecraft.level != null);
            seen.addProperty("player", player != null);
            seen.addProperty("screen", screen == null ? null : screen.getClass().getName());
            if (minecraft.level == null || player == null) return false;
            if (screen != null && !allowScreen) return false;
            int chunkX = SectionPos.blockToSectionCoord(player.getBlockX());
            int chunkZ = SectionPos.blockToSectionCoord(player.getBlockZ());
            int loaded = 0;
            int needed = 0;
            for (int x = chunkX - radius; x <= chunkX + radius; x++) {
                for (int z = chunkZ - radius; z <= chunkZ + radius; z++) {
                    needed++;
                    if (minecraft.level.getChunkSource().hasChunk(x, z)) loaded++;
                }
            }
            seen.addProperty("chunks", loaded + "/" + needed);
            if (loaded < needed) return false;
            if (minecraft.hasSingleplayerServer()) {
                MinecraftServer server = ThirstAgent.server();
                int players = server == null ? 0 : server.getPlayerCount();
                seen.addProperty("serverPlayers", players);
                if (players == 0) return false;
            }
            return true;
        };
    }

    /** A slot of the open menu, by its index there ({@code slot}) or the player's ({@code inventory}). */
    private static final class SlotRef {
        private final Integer menuSlot;
        private final Integer inventorySlot;

        private SlotRef(Integer menuSlot, Integer inventorySlot) {
            this.menuSlot = menuSlot;
            this.inventorySlot = inventorySlot;
        }

        static SlotRef of(AgentRequest request) {
            if (request.has("inventory")) return new SlotRef(null, request.integer("inventory"));
            if (request.has("slot")) return new SlotRef(request.integer("slot"), null);
            throw new AgentException(request.command() + ": '" + request.string("condition")
                    + "' needs 'slot' (the open menu's index) or 'inventory' (the player's)");
        }

        /** What the slot holds now, empty while no menu has it; the menu and the stack go into {@code seen}. */
        ItemStack read(JsonObject seen) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                seen.addProperty("error", "no player");
                return ItemStack.EMPTY;
            }
            seen.addProperty("menu", player.containerMenu.getClass().getName());
            int index = -1;
            if (menuSlot != null) {
                index = menuSlot;
            } else {
                for (int i = 0; i < player.containerMenu.slots.size(); i++) {
                    net.minecraft.world.inventory.Slot slot = player.containerMenu.slots.get(i);
                    if (slot.container == player.getInventory() && slot.getContainerSlot() == inventorySlot) {
                        index = i;
                        break;
                    }
                }
            }
            if (index < 0 || index >= player.containerMenu.slots.size()) {
                seen.addProperty("error", "the open menu has no such slot");
                return ItemStack.EMPTY;
            }
            ItemStack stack = player.containerMenu.slots.get(index).getItem();
            seen.addProperty("slot", index);
            seen.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            seen.addProperty("count", stack.getCount());
            return stack;
        }
    }
}
