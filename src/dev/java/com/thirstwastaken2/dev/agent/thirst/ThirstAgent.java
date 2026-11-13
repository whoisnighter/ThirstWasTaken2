package com.thirstwastaken2.dev.agent.thirst;

import com.google.gson.JsonObject;
import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.dev.agent.core.AgentDispatcher;
import com.thirstwastaken2.dev.agent.core.AgentQueue;
import com.thirstwastaken2.dev.agent.core.AgentWatchdog;
import com.thirstwastaken2.dev.harness.Autorun;
import com.thirstwastaken2.dev.harness.DevEnvironment;
import com.thirstwastaken2.dev.harness.ServerAwake;
import com.thirstwastaken2.dev.platform.DevLoader;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The agent, wired up: one queue per game process, polled on that process's own tick.
 *
 * <p>A dedicated server and a client of the same node share {@code run/&lt;node&gt;}, so the queue
 * directory carries a name — {@code server} or {@code client} by default,
 * {@code -Dthirstwastaken2.agent=&lt;name&gt;} otherwise — and the two never write to one file. The
 * result is {@code run/&lt;node&gt;/agent/&lt;name&gt;/}, holding {@code ready.json}, {@code in.jsonl}
 * and {@code out.jsonl}.
 *
 * <p>One queue per process rather than one per side: a client with an integrated server is still one
 * process, and splitting it in two would make an agent guess which of two directories a command
 * belongs in. {@code server.*} commands work wherever a server is running, integrated or dedicated,
 * and say so plainly when there is none.
 */
public final class ThirstAgent {
    /** {@code -Dthirstwastaken2.agent=<name>} names this process's queue directory. */
    public static final String NAME_PROPERTY = "thirstwastaken2.agent";
    /**
     * {@code -Dthirstwastaken2.agent.script=<file>} runs a file of requests once, at startup (several
     * files, comma separated, one after another, each answering into its own file; see runSuite), and
     * {@code -Dthirstwastaken2.agent.script.exit=true} stops the game once it has been answered. The
     * pair is read by {@link Autorun}, which the benchmark's own autorun property goes through too.
     */
    public static final String SCRIPT_PROPERTY = "thirstwastaken2.agent.script";
    /**
     * Seconds an unattended run may go without answering a request before it gives up and stops, 300
     * by default: {@code -Dthirstwastaken2.agent.stallSeconds=<n>}.
     */
    public static final String STALL_PROPERTY = "thirstwastaken2.agent.stallSeconds";
    /**
     * Seconds an unattended run's game thread may go without a tick before the process is halted, 300
     * by default: {@code -Dthirstwastaken2.agent.frozenSeconds=<n>}.
     */
    public static final String FROZEN_PROPERTY = "thirstwastaken2.agent.frozenSeconds";

    /** The queue this process owns, or null before {@link #install} or outside a development run. */
    private static AgentDispatcher dispatcher;
    private static String side;
    /** Stops this process: halting a dedicated server, or closing a client's window. */
    private static Runnable exit;
    /** The running server, integrated or dedicated; null on a client that has not loaded a world. */
    private static MinecraftServer server;

    private ThirstAgent() { }

    /**
     * Builds this process's queue and registers what it can answer. Called once from the dev tools'
     * entrypoint: {@code client} is true in a game with a window, which is the only process that can
     * answer a {@code client.*} command, and {@code exit} is how that process stops itself when a
     * script asks it to.
     */
    public static void install(boolean client, Runnable exit) {
        if (dispatcher != null) return;
        side = client ? "client" : "server";
        ThirstAgent.exit = exit;
        Path directory = DevLoader.gameDir().resolve("agent").resolve(queueName());

        JsonObject about = DevEnvironment.describe();
        about.addProperty("side", side);
        about.addProperty("name", queueName());

        dispatcher = new AgentDispatcher(new AgentQueue(directory, ThirstWasTaken2.LOGGER),
                ThirstWasTaken2.LOGGER, about);
        CommonProbes.register(dispatcher, side);
        ServerProbes.register(dispatcher);
        WaitUntil.register(dispatcher);
        if (client) {
            ClientProbes.register(dispatcher);
            ClientWorld.register(dispatcher);
            ClientConditions.register();
        }

        DevLoader.onServerStarted(started -> server = started);
        DevLoader.onServerStopping(stopping -> server = null);
    }

    /** The dispatcher this process owns, or null when the agent is not installed. */
    public static AgentDispatcher dispatcher() {
        return dispatcher;
    }

    /** {@code server} or {@code client}: which side of the game this process's queue is polled on. */
    public static String side() {
        return side;
    }

    /** The running server, integrated or dedicated, or null when there is none. */
    public static MinecraftServer server() {
        return server;
    }

    /**
     * Opens the queue and runs the launch's script, if it named one. Called once the process can
     * answer: after the server has started on a dedicated server, and on the first client tick.
     */
    public static void start() {
        if (dispatcher == null) return;
        boolean open = dispatcher.start();
        Autorun script = Autorun.of(SCRIPT_PROPERTY);
        if (script == null || script.argument().isEmpty()) return;
        if (!open) {
            // Most likely another game of this node still running, one whose Gradle task was killed
            // but whose window was not. Running the script anyway would mix both runs' answers.
            ThirstWasTaken2.LOGGER.error("[ThirstAgent] DONE refused, the script was not run: the queue in {} is not "
                    + "this game's; the line above says why", dispatcher.queue().directory().toAbsolutePath());
            if (script.stopWhenDone()) stop();
            return;
        }
        List<Path> scripts = scripts(script.argument());
        if (scripts.isEmpty()) {
            ThirstWasTaken2.LOGGER.error("[ThirstAgent] DONE refused: no script to run in '{}'", script.argument());
            if (script.stopWhenDone()) stop();
            return;
        }
        if (scripts.size() > 1) {
            runSuite(scripts, 0, outputs(scripts), script.stopWhenDone(), 0);
            return;
        }
        if (script.stopWhenDone()) {
            // Nobody is watching an unattended run, so it must end on its own even when it cannot finish.
            dispatcher.watchStall(Integer.getInteger(STALL_PROPERTY, 300), () -> {
                ThirstWasTaken2.LOGGER.error("[ThirstAgent] DONE stalled: no request answered for {} s while {} "
                        + "waited; stopping", Integer.getInteger(STALL_PROPERTY, 300), dispatcher.status());
                flush("the run stalled, and was stopped before this request was started");
                if (exit != null) exit.run();
            });
            AgentWatchdog.start(dispatcher, Thread.currentThread(), Integer.getInteger(FROZEN_PROPERTY, 300),
                    ThirstWasTaken2.LOGGER);
        }
        dispatcher.runScript(scripts.get(0), () -> {
            ThirstWasTaken2.LOGGER.info("[ThirstAgent] DONE script answered, out={}",
                    dispatcher.queue().file(AgentQueue.OUT).toAbsolutePath());
            if (script.stopWhenDone()) stop();
        });
    }

    /**
     * Several scripts in one launch, {@code -Pagent=a.jsonl,b.jsonl}: each answered in turn into its own
     * {@code out-<script>.jsonl}, so a launch is paid once rather than once a file. A script that stalls
     * is given up on, its unanswered requests refused into its own file, and the next one starts; only
     * a frozen game, which the watchdog halts, ends the suite early. The game stops after the last file
     * when the launch asked for that.
     */
    private static void runSuite(List<Path> scripts, int index, List<String> outputs, boolean stopWhenDone,
                                 int stalled) {
        int stallSeconds = Integer.getInteger(STALL_PROPERTY, 300);
        if (index == 0 && stopWhenDone) {
            AgentWatchdog.start(dispatcher, Thread.currentThread(), Integer.getInteger(FROZEN_PROPERTY, 300),
                    ThirstWasTaken2.LOGGER);
        }
        if (index >= scripts.size()) {
            ThirstWasTaken2.LOGGER.info("[ThirstAgent] DONE suite answered: {} scripts, {} stalled or skipped, out={}",
                    scripts.size(), stalled, dispatcher.queue().directory().toAbsolutePath());
            if (stopWhenDone) stop();
            return;
        }
        Path script = scripts.get(index);
        String out = outputs.get(index);
        String position = (index + 1) + "/" + scripts.size();
        if (!java.nio.file.Files.isReadable(script)) {
            ThirstWasTaken2.LOGGER.error("[ThirstAgent] DONE skipped ({}): cannot read the script {}", position,
                    script.toAbsolutePath());
            runSuite(scripts, index + 1, outputs, stopWhenDone, stalled + 1);
            return;
        }
        if (stopWhenDone) {
            dispatcher.watchStall(stallSeconds, () -> {
                ThirstWasTaken2.LOGGER.error("[ThirstAgent] DONE stalled ({}): {} answered nothing for {} s while {} "
                        + "waited; going on with the next script", position, script.getFileName(), stallSeconds,
                        dispatcher.status());
                dispatcher.abandon("the script stalled, and was given up on before this request was answered");
                runSuite(scripts, index + 1, outputs, stopWhenDone, stalled + 1);
            });
        }
        dispatcher.runScript(script, out, () -> {
            ThirstWasTaken2.LOGGER.info("[ThirstAgent] DONE script answered ({}), script={}, out={}", position,
                    script.getFileName(), dispatcher.queue().file(out).toAbsolutePath());
            runSuite(scripts, index + 1, outputs, stopWhenDone, stalled);
        });
    }

    /** The files a launch named, split on commas: one, or several for a suite. */
    private static List<Path> scripts(String argument) {
        List<Path> scripts = new ArrayList<>();
        for (String part : argument.split(",")) {
            if (part.isBlank()) continue;
            try {
                scripts.add(Path.of(part.trim()));
            } catch (java.nio.file.InvalidPathException e) {
                // Left out with a line in the log rather than thrown out of the client tick, which would
                // crash the game before any script ran.
                ThirstWasTaken2.LOGGER.error("[ThirstAgent] DONE skipped: '{}' is not a path: {}", part.trim(),
                        e.getMessage());
            }
        }
        return scripts;
    }

    /**
     * {@code out-<script>.jsonl} for each script of a suite, named after the file without its extension,
     * with {@code -2}, {@code -3} on a name that comes up again.
     */
    private static List<String> outputs(List<Path> scripts) {
        List<String> outputs = new ArrayList<>();
        Set<String> taken = new HashSet<>();
        for (Path script : scripts) {
            String stem = script.getFileName().toString();
            if (stem.endsWith(".jsonl")) stem = stem.substring(0, stem.length() - ".jsonl".length());
            String name = "out-" + stem + ".jsonl";
            for (int copy = 2; !taken.add(name); copy++) name = "out-" + stem + "-" + copy + ".jsonl";
            outputs.add(name);
        }
        return outputs;
    }

    /** Answers whatever is still deferred and closes the queue, without stopping the game. */
    public static void flush() {
        if (dispatcher != null) dispatcher.stop();
    }

    private static void flush(String reason) {
        if (dispatcher != null) dispatcher.stop(reason);
    }

    /** What the queue is doing, in one line for a person, or null when the agent is not installed. */
    public static String status() {
        return dispatcher == null ? null : dispatcher.status();
    }

    /** Closes the queue and then stops this process: halting a dedicated server, or closing a window. */
    public static void stop() {
        flush();
        if (exit != null) exit.run();
    }

    /** Called once per tick on this process's own side. */
    public static void tick() {
        if (dispatcher != null) dispatcher.tick();
    }

    /**
     * The same, on a dedicated server's tick, where the queue also has to keep the server from pausing:
     * an empty one stops ticking after {@code pause-when-empty-seconds} and stops answering with it.
     */
    public static void serverTick(MinecraftServer running) {
        if (dispatcher == null) return;
        ServerAwake.keep(running);
        dispatcher.tick();
    }

    private static String queueName() {
        String name = System.getProperty(NAME_PROPERTY);
        return name == null || name.isBlank() ? side : name.trim();
    }
}
