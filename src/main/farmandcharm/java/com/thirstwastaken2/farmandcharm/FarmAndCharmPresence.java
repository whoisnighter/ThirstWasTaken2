package com.thirstwastaken2.farmandcharm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
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
 * <p>Only the mods' own method names are checked: {@code useItemOn} is Minecraft's, and is named in
 * intermediary in a Fabric jar, so a block's mixin asks only for its class.
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
     * Whether {@code targetClassName} declares a method called {@code method}: read off the class file,
     * which never loads the class. Logged when the class is there but the method is not, which is an
     * upstream change the integration has to follow.
     */
    public static boolean hasMethod(String targetClassName, String method) {
        boolean found = declaresMethod(targetClassName, method);
        if (!found && hasTarget(targetClassName)) {
            LOGGER.warn("{} has no {}, so it is not a version ThirstWasTaken2 supports; "
                    + "sea water may cook there", targetClassName, method);
        }
        return found;
    }

    /** {@link #hasMethod}, without the warning, for a target whose method may be gone on purpose. */
    public static boolean declaresMethod(String targetClassName, String method) {
        if (!hasTarget(targetClassName)) return false;
        return METHODS.computeIfAbsent(targetClassName + '#' + method, key -> readMethod(targetClassName, method));
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
