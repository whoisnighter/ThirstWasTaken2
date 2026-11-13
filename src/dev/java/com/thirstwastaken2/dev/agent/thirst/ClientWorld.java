package com.thirstwastaken2.dev.agent.thirst;

import com.google.gson.JsonObject;
import com.thirstwastaken2.dev.agent.core.AgentDispatcher;
import com.thirstwastaken2.dev.agent.core.AgentException;
import com.thirstwastaken2.dev.agent.core.AgentRequest;
import com.thirstwastaken2.dev.platform.DevLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@code client.world}: leaves the world this client is in and opens another of {@code saves/}, so
 * several scripts can run in one launch each in a world of its own, the way {@code -Pquickplay} opens
 * one at launch.
 *
 * <p>With {@code from}, the world opened is a fresh copy of that one, made over whatever was left under
 * the name before. {@code new_world.py} makes a world once, nothing ever opens it, and every script of a
 * suite starts from a copy of it as if it had just been made. A script that names the world
 * {@code -Pquickplay} already opened, without {@code from}, changes nothing and answers at once, so the
 * same line works in a script run on its own.
 */
final class ClientWorld {
    /** Ticks given to the title screen between leaving one world and opening the next. */
    private static final int LEAVE_TICKS = 5;
    /** How long a world may take to open, unless the request says: two minutes. */
    private static final int DEFAULT_MAX_TICKS = 2400;

    private ClientWorld() { }

    static void register(AgentDispatcher dispatcher) {
        /*
         * Args: `open` (a folder of saves/) with `from` (a folder to copy it from first) and `maxTicks`,
         * or `leave: true`. `open` answers once the world is ready, as wait_until world_ready says it,
         * with the ticks that took.
         */
        dispatcher.register("client.world", (request, reply) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (request.flag("leave", false)) {
                boolean wasIn = minecraft.level != null;
                if (wasIn) AgentClientVanilla.leaveWorld(minecraft);
                dispatcher.defer(LEAVE_TICKS, reply, () -> {
                    JsonObject result = new JsonObject();
                    result.addProperty("left", wasIn);
                    result.addProperty("inWorld", minecraft.level != null);
                    reply.ok(result);
                });
                return;
            }

            Path saves = DevLoader.gameDir().resolve("saves");
            String folder = folder(request, "open");
            String from = request.has("from") ? folder(request, "from") : null;
            int maxTicks = request.has("maxTicks") ? request.integer("maxTicks", 1, WaitUntil.MAX_TICKS)
                    : DEFAULT_MAX_TICKS;
            if (folder.equals(from)) throw new AgentException("client.world: 'from' has to name another world");
            Path source = saves.resolve(from == null ? folder : from);
            if (!Files.isDirectory(source)) {
                throw new AgentException("client.world: there is no world " + source.getFileName() + " in "
                        + saves.toAbsolutePath() + "; make it with tools/agent/new_world.py");
            }

            String loaded = loadedWorld(minecraft);
            if (from == null && folder.equals(loaded)) {
                JsonObject result = new JsonObject();
                result.addProperty("world", folder);
                result.addProperty("alreadyOpen", true);
                result.addProperty("ticks", 0);
                reply.ok(result);
                return;
            }
            // Leaving returns once the integrated server has saved and stopped, so the world it had
            // open is free to be copied over or opened again.
            if (minecraft.level != null) AgentClientVanilla.leaveWorld(minecraft);
            dispatcher.defer(LEAVE_TICKS, reply, () -> {
                if (from != null) copy(saves.resolve(from), saves.resolve(folder));
                AgentClientVanilla.openWorld(minecraft, folder);
                WaitUntil.poll(dispatcher, reply, "world_ready", ClientConditions.worldReady(2, false), maxTicks,
                        ClientConditions.WORLD_READY_HOLD, result -> {
                            result.addProperty("world", folder);
                            if (from != null) result.addProperty("copiedFrom", from);
                            result.addProperty("left", loaded);
                        });
            });
        });
    }

    /** The folder of {@code saves/} the integrated server has open, or null without one. */
    static String loadedWorld(Minecraft minecraft) {
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (server == null) return null;
        Path root = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        return root.getFileName() == null ? null : root.getFileName().toString();
    }

    /** A world's folder name from {@code key}, refusing anything that would reach outside {@code saves/}. */
    private static String folder(AgentRequest request, String key) {
        String name = request.string(key).trim();
        if (name.isEmpty() || name.contains("/") || name.contains("\\") || name.equals(".") || name.equals("..")) {
            throw new AgentException("client.world: '" + key + "' is a folder name in saves/, not '" + name + "'");
        }
        return name;
    }

    /** Replaces {@code target} with a copy of {@code source}, leaving out the session lock. */
    private static void copy(Path source, Path target) {
        try {
            if (Files.exists(target)) {
                List<Path> old;
                try (Stream<Path> walk = Files.walk(target)) {
                    old = walk.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
                }
                for (Path path : old) Files.delete(path);
            }
            List<Path> files;
            try (Stream<Path> walk = Files.walk(source)) {
                files = walk.collect(Collectors.toList());
            }
            for (Path path : files) {
                if (path.getFileName().toString().equals("session.lock")) continue;
                Path to = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) Files.createDirectories(to);
                else Files.copy(path, to);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("client.world: could not copy " + source + " to " + target, e);
        }
    }
}
