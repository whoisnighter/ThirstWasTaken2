package com.thirstwastaken2.sophisticated;

import com.thirstwastaken2.sophisticated.platform.ModFiles;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
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
 * Whether the Sophisticated Core installed is one the integration was compiled against, and whether it
 * still has each class and method a mixin targets.
 *
 * <p>Names no Sophisticated class and no Minecraft class, so the mixin plugin can ask before anything
 * is loaded. It reads FML's list of discovered mod files, which is complete before any mixin config is
 * read, and the class files themselves, which never loads them.
 */
public final class SophisticatedPresence {
    public static final String MOD_ID = "sophisticatedcore";

    private static final Logger LOGGER = LoggerFactory.getLogger("thirstwastaken2");

    private static final Map<String, Boolean> TARGETS = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> METHODS = new ConcurrentHashMap<>();

    private static volatile Boolean present;

    private SophisticatedPresence() { }

    public static boolean isPresent() {
        Boolean known = present;
        if (known == null) {
            ModFileInfo core = LoadingModList.get().getModFileById(MOD_ID);
            // A class only the generation of Core this node's fluid code targets has, IFluidHandler on
            // 1.21.1 or the transfer API from 1.21.11. Looked up in the mod's own jar, so asking does not
            // load it.
            known = core != null && ModFiles.contains(core, SophisticatedGeneration.MARKER);
            if (core != null && !known) {
                LOGGER.warn("Sophisticated Core is installed but is not a version ThirstWasTaken2 supports; "
                        + "water quality through its upgrades is disabled");
            }
            present = known;
        }
        return known;
    }

    /** Whether {@code className}, dotted or slashed, is on the classpath. */
    public static boolean hasClass(String className) {
        String slashed = className.replace('.', '/');
        return TARGETS.computeIfAbsent(slashed, name ->
                SophisticatedPresence.class.getClassLoader().getResource(name + ".class") != null);
    }

    /** Whether {@code className} declares a method called {@code method}, read off its class file. */
    public static boolean declaresMethod(String className, String method) {
        if (!hasClass(className)) return false;
        return METHODS.computeIfAbsent(className.replace('.', '/'), SophisticatedPresence::readMethods).contains(method);
    }

    /**
     * Every method {@code className} calls from the bodies of its methods named any of {@code methods}, as
     * {@code owner.name} with the owner slashed: what a hook in those methods can sit on. Read off the class
     * file, never loading the class; empty when it cannot be read.
     */
    public static Set<String> callsIn(String className, List<String> methods) {
        Set<String> calls = new HashSet<>();
        try (InputStream in = open(className)) {
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
            LOGGER.warn("Could not read {} to look for what it calls", className, e);
        }
        return calls;
    }

    private static Set<String> readMethods(String className) {
        Set<String> names = new HashSet<>();
        try (InputStream in = open(className)) {
            if (in == null) return names;
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    names.add(name);
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        } catch (IOException e) {
            LOGGER.warn("Could not read {} to look for its methods; leaving that part of the integration off", className, e);
        }
        return names;
    }

    private static InputStream open(String className) {
        return SophisticatedPresence.class.getClassLoader().getResourceAsStream(className.replace('.', '/') + ".class");
    }
}
