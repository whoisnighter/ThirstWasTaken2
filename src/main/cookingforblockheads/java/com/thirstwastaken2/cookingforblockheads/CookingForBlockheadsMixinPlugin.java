package com.thirstwastaken2.cookingforblockheads;

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
import java.util.Set;

/**
 * Applies {@code thirstwastaken2.cookingforblockheads.mixins.json} only where Cooking for Blockheads is
 * installed, and each mixin only to the targets that exist: the Sink's block, block entity and tank, and
 * whichever of Balm's fluid wrappers this version of Balm has. The click mixin also needs the Sink to still
 * declare the method it hooks. Balm is installed without Cooking for Blockheads by other mods, and its
 * wrappers are left alone then.
 *
 * <p>Names no class of the mod, no Minecraft class and no loader: the target is looked up as a resource
 * and its methods read off the class file, neither of which loads a class, so this can be asked before
 * anything the mixin targets is loaded.
 */
public final class CookingForBlockheadsMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("thirstwastaken2");

    /**
     * The Sink's click, under every name a jar may give it: {@code useItemOn} from 1.20.5 and {@code use}
     * before, as Mojang names them (NeoForge, a Fabric dev run, 26.1 and later), in intermediary (a Fabric
     * player's game up to 1.21.11) and in SRG (Forge 1.20.1).
     */
    private static final List<String> USE = List.of("useItemOn", "method_55765", "use", "method_9534", "m_6227_");

    @Override
    public void onLoad(String mixinPackage) { }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    /** The Sink's block, present wherever Cooking for Blockheads is, on every version. */
    private static final String SINK_BLOCK = "net/blay09/mods/cookingforblockheads/block/SinkBlock.class";

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        ClassLoader loader = CookingForBlockheadsMixinPlugin.class.getClassLoader();
        if (loader.getResource(SINK_BLOCK) == null) return false;
        String resource = targetClassName.replace('.', '/') + ".class";
        if (!mixinClassName.endsWith(".SinkBlockMixin")) return loader.getResource(resource) != null;
        try (InputStream in = loader.getResourceAsStream(resource)) {
            if (in == null) return false;
            boolean[] found = {false};
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    if (USE.contains(name)) found[0] = true;
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            if (!found[0]) {
                LOGGER.warn("{} has no {}, so it is not a version ThirstWasTaken2 supports; its Sink may take sea "
                        + "water and give ungraded water", targetClassName, USE.get(0));
            }
            return found[0];
        } catch (IOException e) {
            LOGGER.warn("Could not read {}; leaving the Cooking for Blockheads Sink as it is", targetClassName, e);
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
