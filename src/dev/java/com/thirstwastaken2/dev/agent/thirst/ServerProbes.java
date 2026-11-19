package com.thirstwastaken2.dev.agent.thirst;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.thirstwastaken2.api.ThirstApi;
import com.thirstwastaken2.data.ThirstData;
import com.thirstwastaken2.data.ThirstManager;
import com.thirstwastaken2.dev.agent.core.AgentDispatcher;
import com.thirstwastaken2.dev.agent.core.AgentException;
import com.thirstwastaken2.dev.agent.core.AgentRequest;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * What the server knows, as numbers. These answer wherever a server is running — a dedicated one, or
 * a client's integrated one — and say so plainly when there is none.
 *
 * <p>Nothing here parses chat. {@code server.command} does run a command, but it collects the feedback
 * the command produced into a list of its own rather than letting it go to the log for an agent to
 * scrape back out.
 */
final class ServerProbes {
    private ServerProbes() { }

    static void register(AgentDispatcher dispatcher) {
        registerFluid(dispatcher);
        dispatcher.register("server.info", (request, reply) -> {
            MinecraftServer server = server();
            JsonObject result = new JsonObject();
            result.addProperty("dedicated", server.isDedicatedServer());
            result.addProperty("tickCount", server.getTickCount());
            result.addProperty("players", server.getPlayerCount());
            result.addProperty("difficulty", server.getWorldData().getDifficulty().name());
            result.addProperty("levels", server.levelKeys().size());
            reply.ok(result);
        });

        /* Every online player's thirst, in one answer: what "each player sees only their own bar" is
         * checked against, from the side that decides it. */
        dispatcher.register("server.players", (request, reply) -> {
            JsonArray players = new JsonArray();
            for (ServerPlayer player : server().getPlayerList().getPlayers()) players.add(describe(player));
            JsonObject result = new JsonObject();
            result.add("players", players);
            result.addProperty("count", players.size());
            reply.ok(result);
        });

        dispatcher.register("server.thirst.get", (request, reply) ->
                reply.ok(describe(player(request, request.string("player")))));

        /*
         * Sets the state directly rather than through /thirst set, so a check of the command itself is
         * not also the way the fixture is built. Absent fields keep what the player has.
         */
        dispatcher.register("server.thirst.set", (request, reply) -> {
            ServerPlayer player = player(request, request.string("player"));
            ThirstData before = ThirstManager.get(player);
            int thirst = request.integer("thirst", 0, ThirstData.MAX);
            int quenched = request.has("quenched")
                    ? request.integer("quenched", 0, ThirstData.MAX) : before.quenched();
            ThirstData after = before.withLevels(thirst, quenched)
                    .withEnabled(request.flag("enabled", before.enabled()));
            float exhaustion = request.decimal("exhaustion", after.exhaustion());
            after = new ThirstData(after.thirst(), after.quenched(), exhaustion, after.enabled());
            ThirstManager.set(player, after);
            JsonObject result = new JsonObject();
            result.add("before", state(before));
            result.add("after", describe(player));
            reply.ok(result);
        });

        /*
         * The exhaustion modifier the drain applies to this player right now: climate, Fire Resistance
         * and Fire Protection together, 1 being a plains biome at the default depletion. Read by
         * charging one point of exhaustion through the API from zero and putting the state back, on the
         * server's thread. The drain reuses a modifier for a second, so wait 25 ticks after a move.
         * Without `player` it reads the only player online, whose name a 26.x dev client makes up.
         */
        dispatcher.register("server.thirst.modifier", (request, reply) -> {
            ServerPlayer player = request.has("player") ? player(request, request.string("player")) : onlyPlayer(request);
            float[] modifier = new float[1];
            onServer(server(), "server.thirst.modifier", () -> {
                ThirstData before = ThirstManager.get(player);
                ThirstManager.set(player, new ThirstData(before.thirst(), before.quenched(), 0.0F, before.enabled()));
                ThirstApi.addExhaustion(player, 1.0F);
                modifier[0] = ThirstManager.get(player).exhaustion();
                ThirstManager.set(player, before);
            });
            JsonObject result = describe(player);
            result.addProperty("modifier", round(modifier[0]));
            reply.ok(result);
        });

        /*
         * Runs a command and hands back what it returned and what it said. With `as`, it runs from the
         * console's own source moved to that player: `@s` resolves to them, and the permission level is
         * still the console's, so a check never fails because a development player is not an operator.
         * It runs on the server's own thread; see perform.
         */
        dispatcher.register("server.command", (request, reply) -> {
            String command = request.string("command").strip();
            if (command.startsWith("/")) command = command.substring(1);
            ServerPlayer as = request.has("as") ? player(request, request.string("as")) : null;
            JsonObject result = new JsonObject();
            result.addProperty("command", command);
            result.add("messages", perform(server(), command, as));
            reply.ok(result);
        });

        /*
         * Runs `ticks` server ticks as fast as the server can, through /tick sprint, and answers once
         * every one of them has run. For what only time does in the game, a keg fermenting or a crop
         * growing: the same ticks run the same code as at 20 a second, only without the sleep between
         * them, so the result is the one a player would get by waiting.
         *
         * Answering when the sprint is over, rather than after a guess at how long it takes, is the
         * point. A script that sprinted and then waited a fixed number of client ticks waited the whole
         * wall-clock time anyway, since the client ticks at 20 a second whatever the server does, and on
         * a server slower than the guess it went on while the sprint was still running. The server's
         * own tick count says when it is done; each look that finds it moving tells the dispatcher the
         * run is not stalled, since a long sprint answers nothing for minutes.
         */
        dispatcher.register("server.sprint", (request, reply) -> {
            MinecraftServer server = server();
            int ticks = request.integer("ticks", 1, 1_000_000);
            int start = server.getTickCount();
            long began = System.nanoTime();
            JsonArray messages = perform(server, "tick sprint " + ticks, null);
            dispatcher.defer(SPRINT_POLL_TICKS, reply, new Runnable() {
                private int seen = start;

                @Override
                public void run() {
                    int now = server.getTickCount();
                    if (now - start < ticks) {
                        if (now != seen) dispatcher.progress();
                        seen = now;
                        dispatcher.defer(SPRINT_POLL_TICKS, reply, this);
                        return;
                    }
                    JsonObject result = new JsonObject();
                    result.addProperty("ticks", ticks);
                    result.addProperty("ran", now - start);
                    result.addProperty("seconds", round((System.nanoTime() - began) / 1e9));
                    result.add("messages", messages);
                    reply.ok(result);
                }
            });
        });
    }

    /**
     * Pours water into a block the way a pipe does, through the loader's block fluid capability on the
     * server's thread: {@code x}, {@code y}, {@code z} in the overworld, {@code purity} 0 to 3 or
     * {@code salty} true, and {@code amount} in mB (a bucket by default). Answers how much went in, -1
     * when the block takes no fluid. For tanks of other mods that only a pipe mod would otherwise reach.
     */
    static void registerFluid(AgentDispatcher dispatcher) {
        dispatcher.register("server.fluid.fill", (request, reply) -> {
            MinecraftServer server = server();
            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(request.integer("x", -30_000_000, 30_000_000),
                    request.integer("y", -2048, 2048), request.integer("z", -30_000_000, 30_000_000));
            com.thirstwastaken2.purity.WaterQuality quality = request.flag("salty", false)
                    ? com.thirstwastaken2.purity.WaterQuality.SALT
                    : com.thirstwastaken2.purity.WaterQuality.fresh(request.integer("purity", 0, 3));
            int amount = request.has("amount") ? request.integer("amount", 1, 1_000_000) : 1000;
            int[] filled = new int[1];
            onServer(server, "server.fluid.fill", () ->
                    filled[0] = com.thirstwastaken2.dev.platform.DevLoader.fillFluid(server.overworld(), pos, quality, amount));
            JsonObject result = new JsonObject();
            result.addProperty("filled", filled[0]);
            reply.ok(result);
        });
    }

    /** How often, in client or server ticks, {@code server.sprint} looks at the server's tick count. */
    private static final int SPRINT_POLL_TICKS = 5;

    /**
     * Runs one command as the console, or as {@code as}, on the server's own thread, and answers what it
     * said.
     *
     * <p>A client polls its queue on the client thread, and the integrated server answers a block
     * entity lookup from any other thread with null, so {@code data get block} would report every block
     * entity missing. The command is therefore handed to the server's own thread and waited for.
     */
    private static JsonArray perform(MinecraftServer server, String command, ServerPlayer as) {
        Feedback feedback = new Feedback();
        CommandSourceStack console = server.createCommandSourceStack().withSource(feedback);
        Runnable run = () -> {
            CommandSourceStack source = console;
            if (as != null) {
                source = source.withEntity(as).withPosition(as.position());
                if (as.level() instanceof ServerLevel level) source = source.withLevel(level);
            }
            server.getCommands().performPrefixedCommand(source, command);
        };
        if (server.isSameThread()) {
            run.run();
        } else {
            try {
                server.submit(run).get(30, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                throw new AgentException("the server did not run '" + command
                        + "' within 30 seconds; is the world paused?");
            } catch (InterruptedException | ExecutionException e) {
                throw new AgentException("'" + command + "' failed: " + e);
            }
        }
        JsonArray messages = new JsonArray();
        feedback.messages.forEach(messages::add);
        return messages;
    }

    /** Runs {@code run} on the server's own thread and waits for it, as {@link #perform} does a command. */
    private static void onServer(MinecraftServer server, String what, Runnable run) {
        if (server.isSameThread()) {
            run.run();
            return;
        }
        try {
            server.submit(run).get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new AgentException(what + ": the server did not answer within 30 seconds; is the world paused?");
        } catch (InterruptedException | ExecutionException e) {
            throw new AgentException(what + " failed: " + e);
        }
    }

    /** The whole of a player's thirst plus the server-side state a check might read beside it. */
    static JsonObject describe(ServerPlayer player) {
        JsonObject result = state(ThirstManager.get(player));
        result.addProperty("name", player.getScoreboardName());
        result.addProperty("uuid", player.getUUID().toString());
        result.addProperty("dimension", player.level().dimension().identifier().toString());
        result.addProperty("x", round(player.getX()));
        result.addProperty("y", round(player.getY()));
        result.addProperty("z", round(player.getZ()));
        result.addProperty("health", round(player.getHealth()));
        result.addProperty("food", player.getFoodData().getFoodLevel());
        result.addProperty("sprinting", player.isSprinting());
        result.addProperty("creative", player.isCreative());
        result.addProperty("alive", player.isAlive());
        return result;
    }

    static JsonObject state(ThirstData data) {
        JsonObject result = new JsonObject();
        result.addProperty("thirst", data.thirst());
        result.addProperty("quenched", data.quenched());
        result.addProperty("exhaustion", round(data.exhaustion()));
        result.addProperty("enabled", data.enabled());
        return result;
    }

    static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private static MinecraftServer server() {
        MinecraftServer server = ThirstAgent.server();
        if (server == null) {
            throw new AgentException("no server is running in this process; "
                    + "on a client, load a world or use client.command to reach the one it joined");
        }
        return server;
    }

    /** The one online player with that name, however it is spelled. */
    private static ServerPlayer player(AgentRequest request, String name) {
        for (ServerPlayer player : server().getPlayerList().getPlayers()) {
            if (player.getScoreboardName().equalsIgnoreCase(name)) return player;
        }
        List<String> online = new ArrayList<>();
        for (ServerPlayer player : server().getPlayerList().getPlayers()) {
            online.add(player.getScoreboardName());
        }
        throw new AgentException(request.command() + ": no player called '" + name + "' is online; "
                + (online.isEmpty() ? "nobody is" : "online: " + String.join(", ", online)));
    }

    /** The one player online, for a probe asked without a name. */
    private static ServerPlayer onlyPlayer(AgentRequest request) {
        List<ServerPlayer> players = server().getPlayerList().getPlayers();
        if (players.size() != 1) {
            throw new AgentException(request.command() + ": no `player` given, and " + players.size()
                    + " players are online rather than one");
        }
        return players.get(0);
    }

    /** Collects what a command says instead of letting it go to the log. */
    private static final class Feedback implements CommandSource {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void sendSystemMessage(Component message) {
            messages.add(message.getString());
        }

        @Override
        public boolean acceptsSuccess() {
            return true;
        }

        @Override
        public boolean acceptsFailure() {
            return true;
        }

        @Override
        public boolean shouldInformAdmins() {
            return false;
        }
    }
}
