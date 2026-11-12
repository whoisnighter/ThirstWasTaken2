package com.thirstwastaken2.littlelogistics;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies {@code thirstwastaken2.littlelogistics.mixins.json} only where Little Logistics is installed, and
 * each mixin only while its target still declares the method it injects into. Each answer is a resource
 * lookup or a read of a class file, neither of which loads a class, so nothing of the mod is named before
 * it is known to be there.
 */
public final class LittleLogisticsMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("thirstwastaken2");
    private static final String PACKAGE = "dev.murad.shipping.";
    /** A class every build of Little Logistics has. */
    private static final String MARKER = "dev/murad/shipping/ShippingMod.class";

    /** The method each target must still declare. */
    private static final Map<String, List<String>> NEEDS = Map.of(
            PACKAGE + "entity.custom.train.wagon.FluidTankCarEntity", List.of("interact"),
            PACKAGE + "entity.custom.vessel.barge.FluidTankBargeEntity", List.of("doInteract"),
            // NeoForge's own class: it changes nothing outside the two clicks above, and the marker keeps it to where the mod is.
            "net.neoforged.neoforge.fluids.FluidUtil", List.of("getFluidHandler"));

    @Override
    public void onLoad(String mixinPackage) { }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        ClassLoader loader = LittleLogisticsMixinPlugin.class.getClassLoader();
        if (loader.getResource(MARKER) == null) return false;
        List<String> methods = NEEDS.get(targetClassName);
        if (methods == null) return false;
        for (String method : methods) {
            if (!declares(loader, targetClassName, method)) return false;
        }
        return true;
    }

    private static boolean declares(ClassLoader loader, String targetClassName, String method) {
        try (InputStream in = loader.getResourceAsStream(targetClassName.replace('.', '/') + ".class")) {
            if (in == null) {
                LOGGER.warn("{} is missing, so this Little Logistics is not a version ThirstWasTaken2 supports; "
                        + "its water may lose its grade there", targetClassName);
                return false;
            }
            boolean[] found = {false};
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    if (name.equals(method)) found[0] = true;
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            if (!found[0]) {
                LOGGER.warn("{} has no {}, so it is not a version ThirstWasTaken2 supports; "
                        + "its water may lose its grade there", targetClassName, method);
            }
            return found[0];
        } catch (IOException e) {
            LOGGER.warn("Could not read {} to look for {}; leaving that part of the integration off", targetClassName, method, e);
            return false;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
