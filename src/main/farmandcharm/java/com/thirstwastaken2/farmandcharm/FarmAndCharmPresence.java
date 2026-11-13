package com.thirstwastaken2.farmandcharm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Whether Farm & Charm, and Candlelight beside it, still have each class and method a mixin targets.
 *
 * <p>Names no class of either mod, no Minecraft class and no loader, so the mixin plugin can ask before
 * anything a mixin targets is loaded: every answer is a resource lookup or a read of a class file,
 * neither of which loads a class. Both loaders compile this directory, which is why it probes the
 * classpath rather than asking a mod list. Candlelight's Large Cooking Pot is one more target here,
 * absent when Candlelight is, which skips that target alone.
 *
 * <p>A block's {@code useItemOn} is Minecraft's method, so a Fabric jar, in intermediary, calls it
 * {@code method_55765}; the plugin asks for either name.
 */
public final class FarmAndCharmPresence {
    private static final Logger LOGGER = LoggerFactory.getLogger("thirstwastaken2");

    private static final Map<String, Boolean> TARGETS = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> METHODS = new ConcurrentHashMap<>();

    private FarmAndCharmPresence() { }

    /** Whether {@code targetClassName}, a binary name as Mixin passes it, is on the classpath. */
    public static boolean hasTarget(String targetClassName) {
        return TARGETS.computeIfAbsent(targetClassName, name ->
                FarmAndCharmPresence.class.getClassLoader().getResource(name.replace('.', '/') + ".class") != null);
    }

    /**
     * Whether {@code targetClassName} declares a method called any of {@code names}: read off the class
     * file, which never loads the class. A Minecraft override is listed under its Mojang name, for NeoForge
     * and a Fabric development run, and its intermediary name, for a Fabric player's game. Logged when the
     * class is there but none of them is, which is an upstream change the integration has to follow, with
     * {@code consequence}, what the player loses.
     */
    public static boolean hasMethod(String targetClassName, String consequence, String... names) {
        for (String name : names) {
            if (declaresMethod(targetClassName, name)) return true;
        }
        if (hasTarget(targetClassName)) {
            LOGGER.warn("{} has no {}, so it is not a version ThirstWasTaken2 supports; {}",
                    targetClassName, String.join(" or ", names), consequence);
        }
        return false;
    }

    /** {@link #hasMethod}, without the warning, for a target whose method may be gone on purpose. */
    public static boolean declaresMethod(String targetClassName, String method) {
        if (!hasTarget(targetClassName)) return false;
        return METHODS.computeIfAbsent(targetClassName + '#' + method, key -> readMethod(targetClassName, method));
    }

    /**
     * Every method {@code targetClassName} calls from the bodies of its methods named any of
     * {@code methods}, as {@code owner.name} with the owner slashed: what a hook in those methods can sit
     * on. Read off the class file, never loading the class; empty when it cannot be read.
     */
    public static Set<String> callsIn(String targetClassName, List<String> methods) {
        Set<String> calls = new HashSet<>();
        String resource = targetClassName.replace('.', '/') + ".class";
        try (InputStream in = FarmAndCharmPresence.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) return calls;
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    if (!methods.contains(name)) return null;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String called, String desc, boolean isInterface) {
                            calls.add(owner + '.' + called);
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        } catch (IOException e) {
            LOGGER.warn("Could not read {} to look for what it calls", targetClassName, e);
        }
        return calls;
    }

    private static boolean readMethod(String targetClassName, String method) {
        String resource = targetClassName.replace('.', '/') + ".class";
        try (InputStream in = FarmAndCharmPresence.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) return false;
            boolean[] found = {false};
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    if (name.equals(method)) found[0] = true;
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return found[0];
        } catch (IOException e) {
            LOGGER.warn("Could not read {} to look for {}; leaving that part of the integration off", targetClassName, method, e);
            return false;
        }
    }
}
