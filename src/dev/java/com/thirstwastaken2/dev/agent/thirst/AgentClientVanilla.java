package com.thirstwastaken2.dev.agent.thirst;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.io.File;
import java.util.function.Consumer;

/**
 * The client vanilla calls the agent makes whose shape moved between Minecraft versions, and that the
 * mod itself has no reason to have a seam for.
 *
 * <p>It is the same idea as {@code client/platform/ClientVanilla} and deliberately not part of it: the
 * mod's seam carries what the mod needs, and widening it so a development tool can take a screenshot
 * would put API in the published jar that no player ever reaches. Whatever the mod already has a seam
 * for — whether the HUD is hidden, opening a screen — is called through {@code ClientVanilla} rather
 * than copied here.
 */
final class AgentClientVanilla {
    private AgentClientVanilla() { }

    /** The screen currently open, or null for none. 26.2 moved the field onto the GUI. */
    static Screen screen(Minecraft minecraft) {
        //? if >=26.2 {
        return minecraft.gui.screen();
        //?} else {
        /*return minecraft.screen;
        *///?}
    }

    /**
     * How many framebuffer pixels one GUI pixel is. The HUD rectangles the agent reads are in GUI
     * pixels and framebuffer samples are in physical ones, so every conversion between the two goes
     * through this. 1.21.11 turned the scale from a fraction into a whole number, which widens to the
     * same answer and needs no branch.
     */
    static double guiScale(Minecraft minecraft) {
        return minecraft.getWindow().getGuiScale();
    }

    /**
     * The native handle of the game window, for the few things no Minecraft call covers: maximising a
     * driven client's window. 1.21.11 renamed the accessor from {@code getWindow} to {@code handle}, and
     * 26.3 changed what it points at, from a GLFW window to an SDL one; see {@link ClientWindow#open}.
     */
    static long windowHandle(Minecraft minecraft) {
        //? if >1.21.1 {
        return minecraft.getWindow().handle();
        //?} else {
        /*return minecraft.getWindow().getWindow();
        *///?}
    }

    /**
     * Presses and releases a mouse button over {@code screen} at GUI coordinates, the two calls the
     * mouse handler makes, and answers whether the press was taken.
     */
    static boolean click(Screen screen, double x, double y, int button) {
        boolean taken = press(screen, x, y, button);
        release(screen, x, y, button);
        return taken;
    }

    /**
     * Presses a mouse button over {@code screen} and leaves it down, for a drag. 1.21.9 wrapped the
     * position and the button into one event.
     */
    static boolean press(Screen screen, double x, double y, int button) {
        //? if >1.21.1 {
        return screen.mouseClicked(event(x, y, button), false);
        //?} else {
        /*return screen.mouseClicked(x, y, mouseButton(button));
        *///?}
    }

    /** Releases a button {@link #press} left down. */
    static void release(Screen screen, double x, double y, int button) {
        //? if >1.21.1 {
        screen.mouseReleased(event(x, y, button));
        //?} else {
        /*screen.mouseReleased(x, y, mouseButton(button));
        *///?}
    }

    /** Moves the pointer by {@code dx}, {@code dy} to {@code x}, {@code y} with a button held, as a drag. */
    static void drag(Screen screen, double x, double y, int button, double dx, double dy) {
        //? if >1.21.1 {
        screen.mouseDragged(event(x, y, button), dx, dy);
        //?} else {
        /*screen.mouseDragged(x, y, mouseButton(button), dx, dy);
        *///?}
    }

    //? if >1.21.1 {
    private static net.minecraft.client.input.MouseButtonEvent event(double x, double y, int button) {
        return new net.minecraft.client.input.MouseButtonEvent(x, y,
                new net.minecraft.client.input.MouseButtonInfo(mouseButton(button), 0));
    }
    //?}

    /**
     * The game's number for a mouse button, from the 0 left, 1 right, 2 middle a script writes.
     *
     * <p>26.3 moved the client off GLFW and onto SDL, which numbers the buttons from one: left became
     * 1 and right 3, so a button the caller meant as left arrived as right and {@code
     * AbstractWidget.isValidClickButton} refused it. Reading the numbers out of {@code InputConstants}
     * rather than passing the caller's through keeps one meaning on every node; anything past middle
     * is passed on as it was given.
     */
    private static int mouseButton(int button) {
        return switch (button) {
            case 0 -> com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
            case 1 -> com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;
            case 2 -> com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_MIDDLE;
            default -> button;
        };
    }

    /**
     * Clicks a slot of the open container the way a screen does, through the game mode, which sends the
     * click to the server. {@code action} is the constant's name, {@code PICKUP} or {@code QUICK_MOVE}
     * for instance; 26.1 renamed {@code ClickType} to {@code ContainerInput} and kept the names.
     */
    static void clickSlot(Minecraft minecraft, Player player, int slot, int button, String action) {
        int containerId = player.containerMenu.containerId;
        //? if >=26.1 {
        minecraft.gameMode.handleContainerInput(containerId, slot, button,
                net.minecraft.world.inventory.ContainerInput.valueOf(action), player);
        //?} else {
        /*minecraft.gameMode.handleInventoryMouseClick(containerId, slot, button,
                net.minecraft.world.inventory.ClickType.valueOf(action), player);
        *///?}
    }

    /** Toggles the same HUD-hidden state as F1 without depending on a physical keyboard event. */
    static void toggleHud(Minecraft minecraft) {
        // 26.2 moved the flag into Hud and made the toggle its public operation.
        //? if >=26.2 {
        minecraft.gui.hud.toggle();
        //?} else {
        /*minecraft.options.hideGui = !minecraft.options.hideGui;
        *///?}
    }

    /**
     * Writes the framebuffer to {@code <directory>/screenshots/<name>} and calls {@code done} with
     * vanilla's own message once the file is on disk — which is not in this tick: from 1.21.11 the
     * pixels are read back from the GPU asynchronously.
     *
     * <p>A PNG rather than pixels held in memory, on purpose. It is one call whose shape barely moved
     * across four Minecraft versions, the samples are then taken with {@code javax.imageio} on any of
     * them, and the same file is the evidence a person looks at afterwards.
     */
    static void screenshot(Minecraft minecraft, File directory, String name, Consumer<Component> done) {
        RenderTarget target = mainTarget(minecraft);
        // 1.21.11 added the downscale factor; 1 is the framebuffer's own size on every version.
        //? if >1.21.1 {
        Screenshot.grab(directory, name, target, 1, done);
        //?} else {
        /*Screenshot.grab(directory, name, target, done);
        *///?}
    }

    /** What {@link #readFrame} hands over: the frame's pixels as ARGB, top row first. */
    interface FramePixels {
        void accept(int width, int height, int[] argb);
    }

    /**
     * Reads the main target back and hands its pixels to {@code done}, on the render thread, without
     * writing a file: a recording writes its many frames itself, smaller, on a thread of its own.
     * From 1.21.11 the readback is asynchronous and {@code done} runs a frame or so later; on 1.21.1
     * it runs before this returns. 1.21.2 turned {@code NativeImage}'s pixels from ABGR into ARGB.
     */
    static void readFrame(Minecraft minecraft, FramePixels done) {
        RenderTarget target = mainTarget(minecraft);
        //? if >1.21.1 {
        Screenshot.takeScreenshot(target, image -> {
            try (image) {
                done.accept(image.getWidth(), image.getHeight(), image.getPixels());
            }
        });
        //?} else {
        /*try (com.mojang.blaze3d.platform.NativeImage image = Screenshot.takeScreenshot(target)) {
            int[] pixels = image.getPixelsRGBA();
            for (int i = 0; i < pixels.length; i++) {
                int abgr = pixels[i];
                pixels[i] = (abgr & 0xFF00FF00) | (abgr & 0xFF) << 16 | (abgr >> 16 & 0xFF);
            }
            done.accept(image.getWidth(), image.getHeight(), pixels);
        }
        *///?}
    }

    /**
     * Leaves the world or server this client is in for the title screen, as the pause screen's
     * "Save and Quit to Title" does. With an integrated server the call returns once that server has
     * saved and stopped, so its world can be copied or opened again straight after.
     *
     * <p>The level has to be disconnected first: that is what tells an integrated server its owner left,
     * and it halts on that. {@code Minecraft.disconnect} alone waits for a server that was never asked to
     * stop, rendering frames forever without a tick. 1.21.11 made the pair one call.
     */
    static void leaveWorld(Minecraft minecraft) {
        //? if >=1.21.11 {
        minecraft.disconnectFromWorld(Component.translatable("multiplayer.status.quitting"));
        //?} else {
        /*minecraft.level.disconnect();
        clearLevel(minecraft);
        *///?}
    }

    /** What is left of leaving once the level is disconnected, before 1.21.11. 1.20.5 renamed the call. */
    private static void clearLevel(Minecraft minecraft) {
        //? if >=1.20.5 {
        minecraft.disconnect(new net.minecraft.client.gui.screens.TitleScreen(), false);
        //?} else {
        /*minecraft.clearLevel(new net.minecraft.client.gui.screens.TitleScreen());
        *///?}
    }

    /**
     * Opens the singleplayer world in {@code saves/<folder>}, as the world list's Play button does,
     * landing on the title screen if the game refuses it. The world may still be loading when this
     * returns. 1.20.5 renamed {@code loadLevel} to {@code openWorld} and turned the screen to go back to
     * into a callback.
     */
    static void openWorld(Minecraft minecraft, String folder) {
        //? if >=1.20.5 {
        minecraft.createWorldOpenFlows().openWorld(folder,
                () -> com.thirstwastaken2.client.platform.ClientVanilla.setScreen(minecraft,
                        new net.minecraft.client.gui.screens.TitleScreen()));
        //?} else {
        /*minecraft.createWorldOpenFlows().loadLevel(new net.minecraft.client.gui.screens.TitleScreen(), folder);
        *///?}
    }

    /** The target the world and the GUI are drawn into. 26.2 moved it from the client onto its game renderer. */
    private static RenderTarget mainTarget(Minecraft minecraft) {
        //? if >=26.2 {
        return minecraft.gameRenderer.mainRenderTarget();
        //?} else {
        /*return minecraft.getMainRenderTarget();
        *///?}
    }
}
