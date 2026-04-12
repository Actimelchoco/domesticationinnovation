import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class MutantMonstersCreeperMinionJarPatcher {
    private static final String HELPER_OWNER = "fuzs/mutantmonsters/compat/DITameCreeperMinionCompat";
    private static final String PATCH_MARKER = HELPER_OWNER;
    private static final String CREEPER_CLASS = "fuzs/mutantmonsters/world/entity/CreeperMinion.class";
    private static final String EXPLOSION_CLASS = "fuzs/mutantmonsters/world/level/MutatedExplosion.class";
    private static final String TICK_METHOD = "m_8119_";
    private static final String TICK_DESC = "()V";
    private static final String EXPLODE_METHOD = "m_46061_";
    private static final String EXPLODE_DESC = "()V";
    private static final String HURT_METHOD = "m_6469_";
    private static final String HURT_DESC = "(Lnet/minecraft/world/damagesource/DamageSource;F)Z";

    private static final Map<String, String> INJECTED_CLASSES = Map.of(
            "fuzs/mutantmonsters/compat/DITameCreeperMinionCompat.class",
            "fuzs/mutantmonsters/compat/DITameCreeperMinionCompat.class"
    );

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 3) {
            throw new IllegalArgumentException("Usage: MutantMonstersCreeperMinionJarPatcher <input-jar> <output-jar> [compiled-classes-dir]");
        }
        Path compiledClasses = args.length == 3 ? Path.of(args[2]) : Path.of("build/tmp/mutant_patch_classes");
        patchJar(Path.of(args[0]), Path.of(args[1]), compiledClasses);
    }

    private static void patchJar(Path input, Path output, Path compiledClasses) throws IOException {
        Files.createDirectories(output.toAbsolutePath().getParent());
        try (JarFile jarFile = new JarFile(input.toFile());
             OutputStream fileOut = Files.newOutputStream(output);
             JarOutputStream jarOut = new JarOutputStream(fileOut)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                JarEntry outEntry = new JarEntry(entry.getName());
                jarOut.putNextEntry(outEntry);
                try (InputStream in = jarFile.getInputStream(entry)) {
                    byte[] bytes = in.readAllBytes();
                    if (CREEPER_CLASS.equals(entry.getName())) {
                        bytes = patchCreeperMinion(bytes);
                    } else if (EXPLOSION_CLASS.equals(entry.getName())) {
                        bytes = patchExplosion(bytes);
                    }
                    jarOut.write(bytes);
                }
                jarOut.closeEntry();
            }
            for (Map.Entry<String, String> injected : INJECTED_CLASSES.entrySet()) {
                Path classFile = compiledClasses.resolve(injected.getValue());
                if (!Files.exists(classFile)) {
                    throw new IllegalStateException("Missing compiled injected class: " + classFile);
                }
                JarEntry outEntry = new JarEntry(injected.getKey());
                jarOut.putNextEntry(outEntry);
                jarOut.write(Files.readAllBytes(classFile));
                jarOut.closeEntry();
            }
        }
    }

    private static byte[] patchCreeperMinion(byte[] classBytes) {
        if (containsMarker(classBytes)) {
            return classBytes;
        }
        ClassNode node = new ClassNode();
        new ClassReader(classBytes).accept(node, ClassReader.EXPAND_FRAMES);
        boolean changed = false;
        for (MethodNode method : node.methods) {
            if (TICK_METHOD.equals(method.name) && TICK_DESC.equals(method.desc)) {
                changed |= patchCreeperTick(method);
            }
        }
        if (!changed) {
            throw new IllegalStateException("Failed to patch CreeperMinion");
        }
        ClassWriter writer = new SafeClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static byte[] patchExplosion(byte[] classBytes) {
        if (containsMarker(classBytes)) {
            return classBytes;
        }
        ClassNode node = new ClassNode();
        new ClassReader(classBytes).accept(node, ClassReader.EXPAND_FRAMES);
        boolean changed = false;
        for (MethodNode method : node.methods) {
            if (EXPLODE_METHOD.equals(method.name) && EXPLODE_DESC.equals(method.desc)) {
                changed |= patchExplosionDamage(method);
            }
        }
        if (!changed) {
            throw new IllegalStateException("Failed to patch MutatedExplosion");
        }
        ClassWriter writer = new SafeClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static boolean patchCreeperTick(MethodNode method) {
        InsnList prologue = new InsnList();
        prologue.add(new VarInsnNode(Opcodes.ALOAD, 0));
        prologue.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                HELPER_OWNER,
                "enforceTamedSettings",
                "(Ljava/lang/Object;)V",
                false
        ));
        method.instructions.insert(prologue);

        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode methodInsn)) {
                continue;
            }
            if (!"canExplodeContinuously".equals(methodInsn.name) || !"()Z".equals(methodInsn.desc)) {
                continue;
            }
            AbstractInsnNode next = nextMeaningful(methodInsn);
            if (!(next instanceof JumpInsnNode jumpInsn) || jumpInsn.getOpcode() != Opcodes.IFNE) {
                continue;
            }
            InsnList guard = new InsnList();
            guard.add(new VarInsnNode(Opcodes.ALOAD, 0));
            guard.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    HELPER_OWNER,
                    "shouldDiscardAfterExplosion",
                    "(Ljava/lang/Object;)Z",
                    false
            ));
            guard.add(new JumpInsnNode(Opcodes.IFEQ, jumpInsn.label));
            method.instructions.insert(jumpInsn, guard);
            return true;
        }
        return false;
    }

    private static boolean patchExplosionDamage(MethodNode method) {
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode methodInsn)) {
                continue;
            }
            if (methodInsn.getOpcode() != Opcodes.INVOKEVIRTUAL
                    || !HURT_METHOD.equals(methodInsn.name)
                    || !HURT_DESC.equals(methodInsn.desc)) {
                continue;
            }
            InsnList injected = new InsnList();
            injected.add(new VarInsnNode(Opcodes.ALOAD, 2));
            injected.add(new VarInsnNode(Opcodes.ALOAD, 23));
            injected.add(new VarInsnNode(Opcodes.FLOAD, 28));
            injected.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    HELPER_OWNER,
                    "adjustExplosionDamage",
                    "(Ljava/lang/Object;Ljava/lang/Object;F)F",
                    false
            ));
            injected.add(new VarInsnNode(Opcodes.FSTORE, 28));
            method.instructions.insertBefore(insn, injected);
            return true;
        }
        return false;
    }

    private static AbstractInsnNode nextMeaningful(AbstractInsnNode insn) {
        AbstractInsnNode current = insn.getNext();
        while (current != null) {
            int type = current.getType();
            if (type != AbstractInsnNode.LABEL && type != AbstractInsnNode.FRAME && type != AbstractInsnNode.LINE) {
                return current;
            }
            current = current.getNext();
        }
        return null;
    }

    private static boolean containsMarker(byte[] classBytes) {
        return new String(classBytes, StandardCharsets.ISO_8859_1).contains(PATCH_MARKER);
    }

    private static final class SafeClassWriter extends ClassWriter {
        private SafeClassWriter(int flags) {
            super(flags);
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            return "java/lang/Object";
        }
    }
}
