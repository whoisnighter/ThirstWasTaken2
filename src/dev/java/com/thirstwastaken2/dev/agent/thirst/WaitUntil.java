package com.thirstwastaken2.dev.agent.thirst;

import com.google.gson.JsonObject;
import com.thirstwastaken2.dev.agent.core.AgentDispatcher;
import com.thirstwastaken2.dev.agent.core.AgentException;
import com.thirstwastaken2.dev.agent.core.AgentReply;
import com.thirstwastaken2.dev.agent.core.AgentRequest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * {@code wait_until}: answers as soon as a condition holds, rather than after a fixed number of ticks
 * chosen to be long enough on the slowest machine. A {@code wait} of 400 ticks at the top of a script,
 * so the world is surely in, costs twenty seconds every run even when the world was in after five.
 *
 * <p>The condition is looked at once a tick, on the tick of the side that owns the queue, and the
 * answer carries the ticks it took, so a script can still assert on timing when that is the point. One
 * that has not held after {@code maxTicks} is refused with what was last seen, which says why. The cap
 * on {@code maxTicks} keeps a single request well inside the stall guard of an unattended run.
 *
 * <p>This half names nothing of the client, since a dedicated server answers {@code wait_until} too;
 * the conditions only a client can judge are added by {@link ClientConditions}, on a client.
 */
final class WaitUntil {
    /**
     * The longest a {@code wait_until} may wait: four minutes at 20 ticks a second, under the five an
     * unattended run waits without an answer before it calls itself stalled.
     */
    static final int MAX_TICKS = 20 * 60 * 4;
    /** {@code maxTicks} when a request does not say. */
    private static final int DEFAULT_MAX_TICKS = 600;

    /** One look at the game: whether the condition holds, and what was seen, written into {@code seen}. */
    interface Condition {
        boolean met(JsonObject seen);
    }

    /** Builds a condition from a request's arguments, refusing bad ones before any tick is spent. */
    interface Factory {
        Condition create(AgentRequest request);
    }

    /** A condition, and how many looks in a row it has to hold for when the request does not say. */
    private static final class Entry {
        private final Factory factory;
        private final int hold;

        private Entry(Factory factory, int hold) {
            this.factory = factory;
            this.hold = hold;
        }
    }

    private static final Map<String, Entry> CONDITIONS = new LinkedHashMap<>();

    private WaitUntil() { }

    /**
     * Adds or replaces a condition. A client registers its own {@code block_state}, which reads the
     * world the player sees, over the one a dedicated server reads from its overworld.
     */
    static void condition(String name, int hold, Factory factory) {
        CONDITIONS.put(name, new Entry(factory, hold));
    }

    static void register(AgentDispatcher dispatcher) {
        condition("block_state", 1, request -> {
            BlockPos pos = position(request);
            return seen -> {
                MinecraftServer server = ThirstAgent.server();
                if (server == null) {
                    seen.addProperty("error", "no server is running");
                    return false;
                }
                return blockMatches(server.overworld(), pos, request, seen);
            };
        });

        /*
         * Args: `condition`, `maxTicks` (600 by default, at most 4800), `hold` (looks in a row it has to
         * hold for; world_ready 40 by default, the rest 1), and whatever the condition takes.
         */
        dispatcher.register("wait_until", (request, reply) -> {
            String name = request.string("condition");
            Entry entry = CONDITIONS.get(name);
            if (entry == null) {
                throw new AgentException("wait_until: no condition called '" + name + "' on this side; it knows "
                        + String.join(", ", CONDITIONS.keySet()));
            }
            int maxTicks = request.has("maxTicks") ? request.integer("maxTicks", 1, MAX_TICKS) : DEFAULT_MAX_TICKS;
            int hold = request.has("hold") ? request.integer("hold", 1, MAX_TICKS) : entry.hold;
            poll(dispatcher, reply, name, entry.factory.create(request), maxTicks, hold, result -> { });
        });
    }

    /**
     * Looks at {@code condition} now and once a tick after, and answers once it has held for
     * {@code hold} looks in a row, or refuses once {@code maxTicks} ticks have gone without that.
     * {@code finish} adds to the answer whatever the caller wants to say beside it.
     */
    static void poll(AgentDispatcher dispatcher, AgentReply reply, String name, Condition condition,
                     int maxTicks, int hold, Consumer<JsonObject> finish) {
        Runnable look = new Runnable() {
            private int ticks;
            private int held;

            @Override
            public void run() {
                JsonObject seen = new JsonObject();
                boolean met;
                try {
                    met = condition.met(seen);
                } catch (RuntimeException e) {
                    // A world half way in or out of existence throws from places nobody guards; that is
                    // only "not yet", and the next look will tell.
                    seen.addProperty("error", e.getClass().getSimpleName() + ": " + e.getMessage());
                    met = false;
                }
                held = met ? held + 1 : 0;
                if (held >= hold) {
                    JsonObject result = new JsonObject();
                    result.addProperty("condition", name);
                    result.addProperty("ticks", ticks);
                    result.addProperty("hold", hold);
                    result.add("seen", seen);
                    finish.accept(result);
                    reply.ok(result);
                    return;
                }
                if (ticks >= maxTicks) {
                    reply.fail("wait_until: '" + name + "' did not hold for " + hold + " look(s) in a row within "
                            + maxTicks + " ticks; last seen " + seen);
                    return;
                }
                ticks++;
                dispatcher.defer(1, reply, this);
            }
        };
        look.run();
    }

    /** The block a request names with {@code x}, {@code y} and {@code z}. */
    static BlockPos position(AgentRequest request) {
        return new BlockPos(request.integer("x"), request.integer("y"), request.integer("z"));
    }

    /**
     * Whether the block at {@code pos} is the one the request names, by {@code block} id when given,
     * with every one of {@code properties} at the value given. What is there goes into {@code seen}.
     */
    static boolean blockMatches(Level level, BlockPos pos, AgentRequest request, JsonObject seen) {
        BlockState state = level.getBlockState(pos);
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        seen.addProperty("block", id);
        JsonObject properties = new JsonObject();
        for (Property<?> property : state.getProperties()) {
            properties.addProperty(property.getName(), valueName(state, property));
        }
        seen.add("properties", properties);
        if (request.has("block") && !request.string("block").equals(id)) return false;
        JsonObject wanted = request.object("properties");
        for (String key : wanted.keySet()) {
            if (!properties.has(key) || !properties.get(key).getAsString().equals(wanted.get(key).getAsString())) {
                return false;
            }
        }
        return true;
    }

    private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }
}
