package com.thirstwastaken2.dev.agent.thirst;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.thirstwastaken2.client.ThirstHud;
import com.thirstwastaken2.client.config.ThirstConfigScreen;
import com.thirstwastaken2.client.platform.ClientVanilla;
import com.thirstwastaken2.data.ThirstManager;
import com.thirstwastaken2.dev.agent.core.AgentDispatcher;
import com.thirstwastaken2.dev.agent.core.AgentException;
import com.thirstwastaken2.dev.agent.core.AgentReply;
import com.thirstwastaken2.dev.agent.core.AgentRequest;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the client knows, as numbers: the value it holds, whether it is sprinting, where the mod drew
 * the bar, and what colour a pixel of the framebuffer is.
 *
 * <p>Every handler here runs on the client thread, from the client tick. A probe that answers about a
 * frame — a capture or a sample — is deferred rather than answered on the spot, because the framebuffer
 * is read back from the GPU asynchronously from 1.21.11 on and the answer would otherwise describe the
 * previous frame or no frame at all.
 *
 * <p>Nothing in here is an assertion about a picture. A capture is written so a person can look at it
 * afterwards; the answer an agent acts on is always a number or a string.
 */
final class ClientProbes {
    /** Ticks between two looks at a capture that has not finished, and how many looks to take. */
    private static final int CAPTURE_POLL_TICKS = 2;
    private static final int CAPTURE_ATTEMPTS = 30;
    /** Ticks a respawn is given to reach the server and come back before the answer is written. */
    private static final int RESPAWN_TICKS = 10;
    /** Ticks a slot click is given to reach the server and its answer to come back. */
    private static final int SLOT_TICKS = 5;

    private ClientProbes() { }

    static void register(AgentDispatcher dispatcher) {
        dispatcher.register("client.info", (request, reply) -> {
            Minecraft minecraft = client();
            JsonObject result = new JsonObject();
            result.addProperty("frameWidth", minecraft.getWindow().getWidth());
            result.addProperty("frameHeight", minecraft.getWindow().getHeight());
            result.addProperty("guiWidth", minecraft.getWindow().getGuiScaledWidth());
            result.addProperty("guiHeight", minecraft.getWindow().getGuiScaledHeight());
            result.addProperty("guiScale", AgentClientVanilla.guiScale(minecraft));
            result.addProperty("fps", minecraft.getFps());
            result.addProperty("screen", screenName(minecraft));
            result.addProperty("inWorld", minecraft.level != null);
            result.addProperty("singleplayer", minecraft.hasSingleplayerServer());
            // The world -Pquickplay asked for, and whether the client is still outside it. A world
            // that fails to load (a broken data pack) drops back to a menu without stopping the game,
            // so without this a script asking only whether the client answers passes.
            String quickplay = System.getProperty("thirstwastaken2.agent.quickplay");
            result.addProperty("quickplay", quickplay);
            result.addProperty("quickplayFailed", quickplay != null && minecraft.level == null);
            ServerData current = minecraft.getCurrentServer();
            result.addProperty("server", current == null ? null : current.ip);
            LocalPlayer player = minecraft.player;
            result.addProperty("player", player == null ? null : player.getScoreboardName());
            JsonArray keys = new JsonArray();
            Keys.names(minecraft).forEach(keys::add);
            result.add("keys", keys);
            // Whether sneak and sprint are held or toggled is the player's own accessibility setting,
            // and it changes what holding one of those keys means. See Keys.
            result.addProperty("toggleCrouch", minecraft.options.toggleCrouch().get());
            result.addProperty("toggleSprint", minecraft.options.toggleSprint().get());
            // Whether this client was launched to be driven, and whether it is holding the mouse
            // pointer inside its window. A driven one never does; see ClientWindow.
            result.addProperty("driven", ClientWindow.driven());
            result.addProperty("mouseGrabbed", minecraft.mouseHandler.isMouseGrabbed());
            reply.ok(result);
        });

        /*
         * The client's own answer to "what thirst does this client hold": read straight out of the
         * synced state this client was given, never off a screenshot. Everything beside it is the state
         * that decides whether the bar is drawn at all.
         */
        dispatcher.register("client.state", (request, reply) -> {
            Minecraft minecraft = client();
            LocalPlayer player = player(minecraft);
            JsonObject result = ServerProbes.state(ThirstManager.get(player));
            result.addProperty("name", player.getScoreboardName());
            result.addProperty("uuid", player.getUUID().toString());
            result.addProperty("sprinting", player.isSprinting());
            result.addProperty("sneaking", player.isShiftKeyDown());
            result.addProperty("onGround", player.onGround());
            result.addProperty("health", ServerProbes.round(player.getHealth()));
            result.addProperty("food", player.getFoodData().getFoodLevel());
            result.addProperty("creative", player.isCreative());
            result.addProperty("alive", player.isAlive());
            result.addProperty("dimension", player.level().dimension().identifier().toString());
            result.addProperty("x", ServerProbes.round(player.getX()));
            result.addProperty("y", ServerProbes.round(player.getY()));
            result.addProperty("z", ServerProbes.round(player.getZ()));
            result.addProperty("ridingLiving", player.getVehicle() instanceof LivingEntity);
            result.addProperty("hudHidden", ClientVanilla.isHudHidden(minecraft));
            result.addProperty("barShouldRender", ThirstHud.shouldRender(player));
            result.addProperty("screen", screenName(minecraft));
            reply.ok(result);
        });

        /*
         * Where the bar was drawn, taken from the draw call by the recording mixin. `drawnMsAgo` is how
         * "the bar is hidden" is told from "the bar is empty": a hidden bar simply stops being drawn,
         * and the HUD redraws every frame, so anything over a few frames old means it is gone.
         */
        dispatcher.register("client.hud", (request, reply) -> {
            Minecraft minecraft = client();
            JsonObject result = new JsonObject();
            result.addProperty("recording", HudRecord.installed());
            result.addProperty("rowsDrawn", HudRecord.rows());
            result.addProperty("guiWidth", minecraft.getWindow().getGuiScaledWidth());
            result.addProperty("guiHeight", minecraft.getWindow().getGuiScaledHeight());
            result.addProperty("guiScale", AgentClientVanilla.guiScale(minecraft));
            result.add("bar", bar(HudRecord.hud()));
            result.add("preview", bar(HudRecord.preview()));
            result.add("food", row(HudRecord.food()));
            result.add("air", row(HudRecord.air()));
            JsonObject geometry = new JsonObject();
            geometry.addProperty("iconSize", HudRecord.ICON_SIZE);
            geometry.addProperty("iconStride", HudRecord.ICON_STRIDE);
            geometry.addProperty("icons", HudRecord.ICONS);
            geometry.addProperty("barWidth", HudRecord.BAR_WIDTH);
            result.add("geometry", geometry);
            if (!HudRecord.installed()) {
                result.addProperty("note", "the recording mixin has not run; "
                        + "either the bar has never been drawn, or ThirstHud has been renamed");
            }
            reply.ok(result);
        });

        /* Writes the framebuffer to a PNG beside the queue. Evidence for a person, never an assertion. */
        dispatcher.register("client.capture", (request, reply) -> {
            Minecraft minecraft = client();
            Frames.Capture capture = Frames.capture(minecraft, dispatcher.queue().directory(),
                    request.string("name", null));
            whenCaptured(dispatcher, reply, capture, () -> reply.ok(frame(minecraft, capture)));
        });

        /*
         * The colour of the framebuffer at named points. Points are in GUI pixels by default, which is
         * what client.hud answers in, so a check reads "the pixel at the middle of the third droplet"
         * rather than a physical coordinate that changes with the window.
         */
        dispatcher.register("client.pixels", (request, reply) -> {
            Minecraft minecraft = client();
            List<int[]> points = points(request);
            boolean gui = request.choice("space", "gui", "gui", "frame").equals("gui");
            boolean fresh = request.flag("capture", Frames.lastFile() == null);
            if (!fresh) {
                reply.ok(samples(minecraft, Frames.lastFile(), points, gui));
                return;
            }
            Frames.Capture capture = Frames.capture(minecraft, dispatcher.queue().directory(),
                    request.string("name", null));
            whenCaptured(dispatcher, reply, capture,
                    () -> reply.ok(samples(minecraft, capture.file(), points, gui)));
        });

        /* Commands go through the player's own connection, so the server sees them exactly as typed. */
        dispatcher.register("client.command", (request, reply) -> {
            String command = request.string("command").strip();
            if (command.startsWith("/")) command = command.substring(1);
            player(client()).connection.sendCommand(command);
            JsonObject result = new JsonObject();
            result.addProperty("command", command);
            reply.ok(result);
        });

        dispatcher.register("client.chat", (request, reply) -> {
            player(client()).connection.sendChat(request.string("message"));
            reply.ok();
        });

        /*
         * Holds keys down for a number of ticks and answers with the state afterwards. This is the sprint
         * gate: hold forward and sprint at 7 thirst and at 6, and read LocalPlayer.isSprinting() rather
         * than walking a measured track and comparing distances.
         */
        dispatcher.register("client.hold", (request, reply) -> {
            Minecraft minecraft = client();
            List<String> names = request.strings("keys");
            if (names.isEmpty()) throw new AgentException("client.hold: 'keys' needs at least one key");
            int ticks = request.integer("ticks", 1, 20 * 60);
            Map<String, KeyMapping> held = new LinkedHashMap<>();
            for (String name : names) held.put(name, Keys.of(minecraft, name));
            JsonObject before = movement(minecraft);
            held.values().forEach(key -> key.setDown(true));
            dispatcher.defer(ticks, reply, () -> {
                JsonObject after = movement(minecraft);
                held.values().forEach(key -> key.setDown(false));
                JsonObject result = new JsonObject();
                JsonArray keys = new JsonArray();
                held.keySet().forEach(keys::add);
                result.add("keys", keys);
                result.addProperty("ticks", ticks);
                result.add("before", before);
                result.add("after", after);
                reply.ok(result);
            });
        });

        /* Sets one key's state and leaves it there, for a hold that spans several requests. */
        dispatcher.register("client.key", (request, reply) -> {
            Minecraft minecraft = client();
            KeyMapping key = Keys.of(minecraft, request.string("key"));
            boolean down = request.flag("down", true);
            key.setDown(down);
            JsonObject result = new JsonObject();
            result.addProperty("key", request.string("key"));
            result.addProperty("down", key.isDown());
            reply.ok(result);
        });

        /* Screens are opened through the game, not by clicking at coordinates that move with the window.
         * The inventory is here rather than behind client.hold because vanilla opens it from
         * consumeClick, which a held key never reaches; it is where the effect list is drawn. */
        dispatcher.register("client.screen", (request, reply) -> {
            Minecraft minecraft = client();
            String open = request.choice("open", "none", "none", "config", "inventory");
            ClientVanilla.setScreen(minecraft, switch (open) {
                case "config" -> new ThirstConfigScreen(AgentClientVanilla.screen(minecraft));
                case "inventory" -> new net.minecraft.client.gui.screens.inventory.InventoryScreen(player(minecraft));
                default -> null;
            });
            JsonObject result = new JsonObject();
            result.addProperty("screen", screenName(minecraft));
            reply.ok(result);
        });

        /* Toggles F1's HUD state so scripts can prove both the flag and the absence of fresh draws. */
        dispatcher.register("client.hud.toggle", (request, reply) -> {
            Minecraft minecraft = client();
            AgentClientVanilla.toggleHud(minecraft);
            JsonObject result = new JsonObject();
            result.addProperty("hidden", ClientVanilla.isHudHidden(minecraft));
            reply.ok(result);
        });

        /*
         * The lines an item's tooltip produced, as text. Whether they read well is still a person's
         * judgement; whether the grade line is there, and in the right order, is not.
         */
        dispatcher.register("client.tooltip", (request, reply) -> {
            Minecraft minecraft = client();
            LocalPlayer player = player(minecraft);
            ItemStack stack = stack(request, player);
            TooltipFlag flag = request.flag("advanced", false) ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL;
            //? if >=1.20.5 {
            List<Component> lines = stack.getTooltipLines(Item.TooltipContext.of(player.level()), player, flag);
            //?} else {
            /*List<Component> lines = stack.getTooltipLines(player, flag);
            *///?}
            JsonArray text = new JsonArray();
            JsonArray colours = new JsonArray();
            for (Component line : lines) {
                text.add(line.getString());
                Integer colour = line.getStyle().getColor() == null ? null
                        : line.getStyle().getColor().getValue();
                colours.add(colour == null ? null : String.format("#%06X", colour));
            }
            JsonObject result = new JsonObject();
            result.addProperty("item", stack.getItem().toString());
            result.addProperty("count", stack.getCount());
            result.add("lines", text);
            result.add("colours", colours);
            reply.ok(result);
        });

        /*
         * A mouse click on the open screen, for a control that has no other way in, such as another
         * mod's settings button. Coordinates are GUI pixels from the centre of the screen by default:
         * container screens are centred, so a control on one sits at the same offset from the centre
         * whatever the window's size, which is not true of an offset from the corner. A screen built
         * from a header and a footer anchors its rows to one edge instead, so {@code top} and {@code
         * bottom} keep x from the centre and measure y from that edge, {@code bottom} upwards. The
         * answer says which of the screen's children was under the point, and whether it took the
         * press. {@code button} is 0 left, 1 right and 2 middle on every version; see
         * {@link AgentClientVanilla#click}, which is where 26.3's renumbering is undone.
         */
        dispatcher.register("client.click", (request, reply) -> {
            Minecraft minecraft = client();
            Screen screen = AgentClientVanilla.screen(minecraft);
            if (screen == null) throw new AgentException("client.click: no screen is open");
            double[] point = point(request, minecraft);
            double x = point[0];
            double y = point[1];
            int button = request.has("button") ? request.integer("button", 0, 2) : 0;
            String target = screen.getChildAt(x, y).map(child -> child.getClass().getName()).orElse(null);
            // With the virtual pointer shown, the click is made where the pointer is, and a recording sees it.
            if (Pointer.shown()) {
                Pointer.place(minecraft, x, y);
                Pointer.clicked();
            }
            boolean taken = AgentClientVanilla.click(screen, x, y, button);
            JsonObject result = new JsonObject();
            result.addProperty("screen", screen.getClass().getName());
            result.addProperty("width", screen.width);
            result.addProperty("height", screen.height);
            result.addProperty("x", x);
            result.addProperty("y", y);
            result.addProperty("target", target);
            result.addProperty("taken", taken);
            reply.ok(result);
        });

        /*
         * Turns the mouse wheel over the open screen at a point, placed as client.click places one, for a
         * list longer than the screen. `amount` is wheel notches, positive scrolling down the page.
         */
        dispatcher.register("client.scroll", (request, reply) -> {
            Minecraft minecraft = client();
            Screen screen = AgentClientVanilla.screen(minecraft);
            if (screen == null) throw new AgentException("client.scroll: no screen is open");
            double[] point = point(request, minecraft);
            double x = point[0];
            double y = point[1];
            if (Pointer.shown()) Pointer.place(minecraft, x, y);
            int amount = request.integer("amount", -100, 100);
            // One event a notch, as a real wheel sends them: a screen may move one row per event whatever
            // its size, as the config screen does. The game's wheel is positive upwards.
            boolean taken = false;
            for (int notch = 0; notch < Math.abs(amount); notch++) {
                taken |= ClientVanilla.mouseScrolled(screen, x, y, -Math.signum(amount));
            }
            JsonObject result = new JsonObject();
            result.addProperty("x", x);
            result.addProperty("y", y);
            result.addProperty("amount", amount);
            result.addProperty("taken", taken);
            reply.ok(result);
        });

        /*
         * Shows the virtual pointer and moves it, for a recording: hover states and tooltips are drawn
         * under it as under a real mouse, and client.click and client.scroll without a point of their
         * own land where it is. The point is placed as client.click places one. Over `ticks` it glides
         * there along a minimum-jerk curve, one step a tick; with `drag` the left button is pressed where it
         * starts, held while it moves and released where it stops, which is how a slider is dragged.
         * `hide` hands the position back to the real mouse.
         */
        dispatcher.register("client.mouse", (request, reply) -> {
            Minecraft minecraft = client();
            if (request.flag("hide", false)) {
                Pointer.hide();
                JsonObject result = new JsonObject();
                result.addProperty("shown", false);
                reply.ok(result);
                return;
            }
            double[] point = point(request, minecraft);
            int ticks = request.has("ticks") ? request.integer("ticks", 0, 20 * 60) : 0;
            boolean drag = request.flag("drag", false);
            Screen screen = AgentClientVanilla.screen(minecraft);
            if (drag && screen == null) throw new AgentException("client.mouse: a drag needs an open screen");
            double startX = Pointer.shown() ? Pointer.x() : point[0];
            double startY = Pointer.shown() ? Pointer.y() : point[1];
            Pointer.place(minecraft, startX, startY);
            if (drag) {
                AgentClientVanilla.press(screen, startX, startY, 0);
                Pointer.hold(true);
            }
            glide(dispatcher, reply, minecraft, drag ? screen : null, startX, startY, point[0], point[1], ticks, 1);
        });

        /*
         * Starts or stops a recording: a capture every `every` ticks into screenshots/<name>/, `downscale`
         * times smaller than the window on each side (2 by default), with the virtual pointer beside each
         * frame in frames.jsonl. `stop` answers once every frame is on disk.
         * tools/agent/make_gif.py turns the folder into a GIF.
         */
        dispatcher.register("client.record", (request, reply) -> {
            Minecraft minecraft = client();
            String action = request.choice("action", "start", "start", "stop");
            if (action.equals("start")) {
                int every = request.has("every") ? request.integer("every", 1, 20) : 2;
                int downscale = request.has("downscale") ? request.integer("downscale", 1, 8) : 2;
                String name = request.string("name", "recording");
                Recorder.start(dispatcher.queue().directory(), name, every, downscale);
                JsonObject result = new JsonObject();
                result.addProperty("recording", name);
                result.addProperty("every", every);
                result.addProperty("downscale", downscale);
                reply.ok(result);
                return;
            }
            whenRecorded(dispatcher, reply, Recorder.stop(minecraft), CAPTURE_ATTEMPTS * 4);
        });

        /* The open container's slots that hold something, by the index client.slot takes. */
        dispatcher.register("client.slots", (request, reply) -> reply.ok(slots(player(client()))));

        /*
         * A click on one slot of the open container, sent to the server as a screen sends it: a left or
         * right pick-up, a shift-click (quick_move) and so on. The slot is the menu's index, or with
         * `inventory` the player's own inventory index (container.N), wherever this menu put it.
         * Answered a few ticks later with every slot that holds something and what the cursor carries,
         * once the server has answered.
         */
        dispatcher.register("client.slot", (request, reply) -> {
            Minecraft minecraft = client();
            LocalPlayer player = player(minecraft);
            int slot = request.has("inventory") ? menuSlot(player, request.integer("inventory"))
                    : request.integer("slot", 0, player.containerMenu.slots.size() - 1);
            int button = request.has("button") ? request.integer("button", 0, 8) : 0;
            String action = request.choice("action", "pickup",
                    "pickup", "quick_move", "swap", "clone", "throw", "quick_craft", "pickup_all");
            AgentClientVanilla.clickSlot(minecraft, player, slot, button, action.toUpperCase(java.util.Locale.ROOT));
            dispatcher.defer(SLOT_TICKS, reply, () -> reply.ok(slots(player)));
        });

        /*
         * Switches the game's language without writing options.txt. Only the translations are loaded
         * again, not every resource the way the language screen does: the fonts already hold every
         * script, and a full reload in a world freed a font atlas that Jade's overlay drew from a frame
         * later, which crashed the client. So the next request already sees the new text.
         */
        dispatcher.register("client.language", (request, reply) -> {
            Minecraft minecraft = client();
            String code = request.string("code");
            minecraft.getLanguageManager().setSelected(code);
            minecraft.options.languageCode = code;
            minecraft.getLanguageManager().onResourceManagerReload(minecraft.getResourceManager());
            JsonObject result = new JsonObject();
            result.addProperty("language", minecraft.getLanguageManager().getSelected());
            reply.ok(result);
        });

        /*
         * How wide the game's font draws each text, in GUI pixels, the number a label compares against
         * the room it has. Keys are translated in the current language first, so with client.language
         * this measures every translation with the glyphs the game really uses.
         */
        dispatcher.register("client.textWidth", (request, reply) -> {
            Minecraft minecraft = client();
            JsonArray widths = new JsonArray();
            for (String key : request.has("keys") ? request.strings("keys") : List.<String>of()) {
                widths.add(width(minecraft, key, Component.translatable(key).getString()));
            }
            for (String text : request.has("texts") ? request.strings("texts") : List.<String>of()) {
                widths.add(width(minecraft, null, text));
            }
            JsonObject result = new JsonObject();
            result.addProperty("language", minecraft.getLanguageManager().getSelected());
            result.add("widths", widths);
            reply.ok(result);
        });

        /*
         * Dying and coming back is one of the checks, so it is one of the commands. Vanilla has only
         * one way in: the button on the death screen. A click at the coordinates that button happens to
         * be at is exactly the kind of assertion-about-a-picture the agent exists to avoid, so this
         * presses it the way the screen does, through the player's own connection.
         */
        dispatcher.register("client.respawn", (request, reply) -> {
            Minecraft minecraft = client();
            LocalPlayer player = player(minecraft);
            if (player.isAlive()) {
                throw new AgentException("client.respawn: " + player.getScoreboardName()
                        + " is alive; kill them first, with server.command 'kill' or client.command");
            }
            player.respawn();
            ClientVanilla.setScreen(minecraft, null);
            // Answered a few ticks later, not now: closing a screen takes effect on the client's next
            // pass, and the respawn itself is a round trip to the server, so an answer written here
            // would report the death screen still open and the player still dead.
            dispatcher.defer(RESPAWN_TICKS, reply, () -> {
                JsonObject result = new JsonObject();
                result.addProperty("screen", screenName(minecraft));
                result.addProperty("alive", minecraft.player != null && minecraft.player.isAlive());
                reply.ok(result);
            });
        });

        /*
         * Leaving and rejoining is one of the checks, so it is one of the commands. The client answers
         * before it disconnects, because the queue it would answer into belongs to this process and the
         * process survives either way.
         */
        dispatcher.register("client.disconnect", (request, reply) -> {
            Minecraft minecraft = client();
            if (minecraft.level == null) throw new AgentException("client.disconnect: no world is loaded");
            reply.ok();
            AgentClientVanilla.leaveWorld(minecraft);
        });

        dispatcher.register("client.connect", (request, reply) -> {
            Minecraft minecraft = client();
            String address = request.string("address", "localhost:25565");
            if (minecraft.level != null) {
                throw new AgentException("client.connect: a world is already loaded; disconnect first");
            }
            reply.ok();
            //? if >=1.20.5 {
            ServerData data = new ServerData("agent", address, ServerData.Type.OTHER);
            net.minecraft.client.gui.screens.ConnectScreen.startConnecting(
                    new TitleScreen(), minecraft,
                    net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(address),
                    data, false, null);
            //?} else {
            /*ServerData data = new ServerData("agent", address, false);
            net.minecraft.client.gui.screens.ConnectScreen.startConnecting(
                    new TitleScreen(), minecraft,
                    net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(address),
                    data, false);
            *///?}
        });
    }

    /** Waits for a capture to reach disk, then runs {@code ready}. */
    /**
     * The point a request names, in GUI pixels: `x` and `y` measured `from` the centre (the default),
     * the corner, or the top or bottom edge with x still from the centre. Without `x` and `y`, where the
     * virtual pointer is, when it is shown. Measured against the open screen, or the window without one.
     */
    private static double[] point(AgentRequest request, Minecraft minecraft) {
        if (!request.has("x") && !request.has("y") && Pointer.shown()) {
            return new double[] {Pointer.x(), Pointer.y()};
        }
        Screen screen = AgentClientVanilla.screen(minecraft);
        double width = screen != null ? screen.width : minecraft.getWindow().getGuiScaledWidth();
        double height = screen != null ? screen.height : minecraft.getWindow().getGuiScaledHeight();
        String from = request.choice("from", "centre", "centre", "corner", "top", "bottom");
        double x = request.decimal("x", 0.0F) + (from.equals("corner") ? 0.0 : width / 2.0);
        double y = request.decimal("y", 0.0F) + switch (from) {
            case "centre" -> height / 2.0;
            case "bottom" -> height;
            default -> 0.0;
        };
        return new double[] {x, y};
    }

    /** One tick of client.mouse's glide: step {@code step} of {@code ticks}, answering after the last. */
    private static void glide(AgentDispatcher dispatcher, AgentReply reply, Minecraft minecraft, Screen dragged,
                              double fromX, double fromY, double toX, double toY, int ticks, int step) {
        double lastX = Pointer.x();
        double lastY = Pointer.y();
        double t = ticks == 0 ? 1.0 : (double) step / ticks;
        // Minimum jerk, the profile a hand pointing at something follows: it speeds up and slows down
        // smoothly, with no jolt at either end.
        double eased = t * t * t * (10.0 - 15.0 * t + 6.0 * t * t);
        double x = fromX + (toX - fromX) * eased;
        double y = fromY + (toY - fromY) * eased;
        Pointer.place(minecraft, x, y);
        if (dragged != null) AgentClientVanilla.drag(dragged, x, y, 0, x - lastX, y - lastY);
        if (step < ticks) {
            dispatcher.defer(1, reply, () ->
                    glide(dispatcher, reply, minecraft, dragged, fromX, fromY, toX, toY, ticks, step + 1));
            return;
        }
        if (dragged != null) {
            AgentClientVanilla.release(dragged, x, y, 0);
            Pointer.hold(false);
        }
        Screen screen = AgentClientVanilla.screen(minecraft);
        JsonObject result = new JsonObject();
        result.addProperty("x", x);
        result.addProperty("y", y);
        result.addProperty("ticks", ticks);
        result.addProperty("dragged", dragged != null);
        result.addProperty("target", screen == null ? null
                : screen.getChildAt(x, y).map(child -> child.getClass().getName()).orElse(null));
        reply.ok(result);
    }

    /** Answers client.record's stop once every capture it started has reached disk. */
    private static void whenRecorded(AgentDispatcher dispatcher, AgentReply reply, JsonObject result,
                                     int attemptsLeft) {
        dispatcher.defer(CAPTURE_POLL_TICKS, reply, () -> {
            if (Recorder.finished()) {
                result.addProperty("failed", Recorder.failed());
                result.addProperty("firstFailure", Recorder.firstFailure());
                reply.ok(result);
            } else if (attemptsLeft <= 1) {
                reply.fail("the recording's last frames did not reach disk in time");
            } else {
                whenRecorded(dispatcher, reply, result, attemptsLeft - 1);
            }
        });
    }

    private static void whenCaptured(AgentDispatcher dispatcher, AgentReply reply, Frames.Capture capture,
                                     Runnable ready) {
        waitForCapture(dispatcher, reply, capture, CAPTURE_ATTEMPTS, ready);
    }

    private static void waitForCapture(AgentDispatcher dispatcher, AgentReply reply, Frames.Capture capture,
                                       int attemptsLeft, Runnable ready) {
        dispatcher.defer(CAPTURE_POLL_TICKS, reply, () -> {
            String failure = capture.failure().get();
            if (failure != null) {
                reply.fail("the capture failed: " + failure);
                return;
            }
            if (capture.finished() && Files.isRegularFile(capture.file())) {
                Frames.invalidate();
                ready.run();
                return;
            }
            if (attemptsLeft <= 1) {
                reply.fail("the capture did not reach " + capture.file() + " within "
                        + CAPTURE_ATTEMPTS * CAPTURE_POLL_TICKS + " ticks");
                return;
            }
            waitForCapture(dispatcher, reply, capture, attemptsLeft - 1, ready);
        });
    }

    private static JsonObject frame(Minecraft minecraft, Frames.Capture capture) {
        BufferedImage image = Frames.image(capture.file());
        JsonObject result = new JsonObject();
        result.addProperty("file", capture.file().toAbsolutePath().toString());
        result.addProperty("width", image.getWidth());
        result.addProperty("height", image.getHeight());
        result.addProperty("guiScale", AgentClientVanilla.guiScale(minecraft));
        result.addProperty("message", capture.message().get());
        return result;
    }

    private static JsonObject samples(Minecraft minecraft, Path file, List<int[]> points, boolean gui) {
        BufferedImage image = Frames.image(file);
        double scale = AgentClientVanilla.guiScale(minecraft);
        JsonArray samples = new JsonArray();
        for (int[] point : points) {
            int frameX = gui ? (int) Math.round((point[0] + 0.5) * scale) : point[0];
            int frameY = gui ? (int) Math.round((point[1] + 0.5) * scale) : point[1];
            JsonObject sample = new JsonObject();
            sample.addProperty("x", point[0]);
            sample.addProperty("y", point[1]);
            sample.addProperty("frameX", frameX);
            sample.addProperty("frameY", frameY);
            if (frameX < 0 || frameY < 0 || frameX >= image.getWidth() || frameY >= image.getHeight()) {
                sample.addProperty("error", "outside the " + image.getWidth() + "x" + image.getHeight() + " frame");
            } else {
                int argb = image.getRGB(frameX, frameY);
                sample.addProperty("argb", Frames.hex(argb));
                sample.addProperty("alpha", (argb >>> 24) & 0xFF);
                sample.addProperty("red", (argb >> 16) & 0xFF);
                sample.addProperty("green", (argb >> 8) & 0xFF);
                sample.addProperty("blue", argb & 0xFF);
            }
            samples.add(sample);
        }
        JsonObject result = new JsonObject();
        result.addProperty("file", file.toAbsolutePath().toString());
        result.addProperty("width", image.getWidth());
        result.addProperty("height", image.getHeight());
        result.addProperty("guiScale", scale);
        result.addProperty("space", gui ? "gui" : "frame");
        result.add("points", samples);
        return result;
    }

    /**
     * The points to sample, written either as {@code [x, y]} or as <code>{"x": .., "y": ..}</code>.
     * Both spellings turn up in hand-written scripts, and neither is worth an error.
     */
    private static List<int[]> points(AgentRequest request) {
        List<int[]> points = new ArrayList<>();
        for (JsonElement element : request.list("points")) {
            if (element.isJsonArray() && element.getAsJsonArray().size() == 2) {
                JsonArray pair = element.getAsJsonArray();
                points.add(new int[] {pair.get(0).getAsInt(), pair.get(1).getAsInt()});
            } else if (element.isJsonObject() && element.getAsJsonObject().has("x")) {
                JsonObject object = element.getAsJsonObject();
                points.add(new int[] {object.get("x").getAsInt(), object.get("y").getAsInt()});
            } else {
                throw new AgentException("client.pixels: a point is [x, y] or {\"x\": .., \"y\": ..}, got "
                        + element);
            }
        }
        if (points.isEmpty()) throw new AgentException("client.pixels: 'points' needs at least one point");
        return points;
    }

    /** One recorded draw of the bar, with the droplet rectangles worked out from it. */
    private static JsonElement bar(HudRecord.Bar drawn) {
        if (drawn == null) return JsonNull.INSTANCE;
        JsonObject result = new JsonObject();
        result.addProperty("left", drawn.left());
        result.addProperty("top", drawn.top());
        result.addProperty("right", drawn.right());
        result.addProperty("bottom", drawn.top() + HudRecord.ICON_SIZE);
        result.addProperty("width", HudRecord.BAR_WIDTH);
        result.addProperty("height", HudRecord.ICON_SIZE);
        result.addProperty("thirst", drawn.thirst());
        result.addProperty("quenched", drawn.quenched());
        result.addProperty("exhaustion", ServerProbes.round(drawn.exhaustion()));
        result.addProperty("overlay", drawn.overlay());
        result.addProperty("exhaustionStrip", drawn.exhaustionStrip());
        result.addProperty("shake", drawn.shake());
        result.addProperty("parched", drawn.parched());
        result.addProperty("upsetStomach", drawn.upsetStomach());
        result.addProperty("drawnMsAgo", drawn.ageMillis());
        // Droplet 0 is the rightmost, the way the HUD draws them.
        JsonArray droplets = new JsonArray();
        for (int i = 0; i < HudRecord.ICONS; i++) {
            JsonObject droplet = new JsonObject();
            int x = drawn.right() - i * HudRecord.ICON_STRIDE - HudRecord.ICON_SIZE;
            droplet.addProperty("index", i);
            droplet.addProperty("left", x);
            droplet.addProperty("top", drawn.top());
            droplet.addProperty("centreX", x + HudRecord.ICON_SIZE / 2);
            droplet.addProperty("centreY", drawn.top() + HudRecord.ICON_SIZE / 2);
            droplets.add(droplet);
        }
        result.add("droplets", droplets);
        return result;
    }

    private static JsonElement row(HudRecord.SpriteRow drawn) {
        if (drawn == null) return JsonNull.INSTANCE;
        JsonObject result = new JsonObject();
        result.addProperty("left", drawn.left());
        result.addProperty("top", drawn.top());
        result.addProperty("right", drawn.right());
        result.addProperty("bottom", drawn.bottom());
        result.addProperty("width", drawn.width());
        result.addProperty("height", drawn.height());
        result.addProperty("sprites", drawn.sprites());
        result.addProperty("drawnMsAgo", drawn.ageMillis());
        return result;
    }

    private static JsonObject movement(Minecraft minecraft) {
        LocalPlayer player = player(minecraft);
        JsonObject result = new JsonObject();
        result.addProperty("sprinting", player.isSprinting());
        result.addProperty("sneaking", player.isShiftKeyDown());
        result.addProperty("x", ServerProbes.round(player.getX()));
        result.addProperty("y", ServerProbes.round(player.getY()));
        result.addProperty("z", ServerProbes.round(player.getZ()));
        result.add("thirst", ServerProbes.state(ThirstManager.get(player)));
        return result;
    }

    /**
     * The stack a tooltip is asked about: one built from an item id, or, with {@code slot}, the one the
     * player is actually holding. An id alone cannot carry components, and a tooltip line that comes
     * from one - what a jar holds, a waterskin's servings, a grade - only exists on a real stack.
     */
    private static ItemStack stack(AgentRequest request, LocalPlayer player) {
        if (request.has("slot")) {
            String slot = request.string("slot");
            if (slot.equals("mainhand")) return player.getMainHandItem();
            if (slot.equals("offhand")) return player.getOffhandItem();
            try {
                return player.getInventory().getItem(Integer.parseInt(slot));
            } catch (NumberFormatException e) {
                throw new AgentException("client.tooltip: 'slot' is mainhand, offhand or an inventory "
                        + "index, not '" + slot + "'");
            }
        }
        String name = request.string("item");
        Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(name))
                .orElseThrow(() -> new AgentException("client.tooltip: no item called '" + name + "'"));
        return new ItemStack(item, request.integer("count", 1));
    }

    private static JsonObject slots(LocalPlayer player) {
        JsonArray filled = new JsonArray();
        for (int index = 0; index < player.containerMenu.slots.size(); index++) {
            net.minecraft.world.inventory.Slot slot = player.containerMenu.slots.get(index);
            if (!slot.hasItem()) continue;
            JsonObject entry = item(slot.getItem());
            entry.addProperty("slot", index);
            entry.addProperty("kind", slot.getClass().getName());
            if (slot.container == player.getInventory()) entry.addProperty("inventory", slot.getContainerSlot());
            filled.add(entry);
        }
        JsonObject result = new JsonObject();
        result.addProperty("menu", player.containerMenu.getClass().getName());
        result.addProperty("size", player.containerMenu.slots.size());
        result.add("carried", item(player.containerMenu.getCarried()));
        result.add("slots", filled);
        return result;
    }

    private static int menuSlot(LocalPlayer player, int inventorySlot) {
        for (int index = 0; index < player.containerMenu.slots.size(); index++) {
            net.minecraft.world.inventory.Slot slot = player.containerMenu.slots.get(index);
            if (slot.container == player.getInventory() && slot.getContainerSlot() == inventorySlot) return index;
        }
        throw new AgentException("client.slot: the open menu has no slot for inventory slot " + inventorySlot);
    }

    private static JsonObject item(ItemStack stack) {
        JsonObject entry = new JsonObject();
        entry.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        entry.addProperty("count", stack.getCount());
        return entry;
    }

    private static JsonObject width(Minecraft minecraft, String key, String text) {
        JsonObject entry = new JsonObject();
        if (key != null) entry.addProperty("key", key);
        entry.addProperty("text", text);
        entry.addProperty("width", minecraft.font.width(text));
        return entry;
    }

    private static String screenName(Minecraft minecraft) {
        Screen screen = AgentClientVanilla.screen(minecraft);
        return screen == null ? null : screen.getClass().getName();
    }

    private static Minecraft client() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) throw new AgentException("there is no client in this process");
        return minecraft;
    }

    private static LocalPlayer player(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            throw new AgentException("no player on this client; it is at " + screenName(minecraft));
        }
        return player;
    }
}
