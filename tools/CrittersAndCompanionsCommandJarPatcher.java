import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
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

public class CrittersAndCompanionsCommandJarPatcher {
    private static final String TARGET_CLASS = "com/github/eterdelta/crittersandcompanions/entity/DragonflyEntity.class";
    private static final String GOAL_METHOD = "m_8099_";
    private static final String GOAL_DESC = "()V";
    private static final String INTERACT_METHOD = "m_6071_";
    private static final String INTERACT_DESC = "(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;";
    private static final String TICK_METHOD = "m_8024_";
    private static final String TICK_DESC = "()V";
    private static final String PATCH_MARKER = "com/github/eterdelta/crittersandcompanions/compat/DIServerPetCommandCompat";
    private static final String FOLLOW_OWNER_GOAL = "net/minecraft/world/entity/ai/goal/FollowOwnerGoal";
    private static final String PATCHED_GOAL = "com/github/eterdelta/crittersandcompanions/entity/ai/goal/DICommandedFollowOwnerGoal";
    private static final String HELPER_OWNER = "com/github/eterdelta/crittersandcompanions/compat/DIServerPetCommandCompat";

    private static final Map<String, String> INJECTED_CLASSES = Map.of(
            "com/github/eterdelta/crittersandcompanions/compat/DIServerPetCommandCompat.class",
            "com/github/eterdelta/crittersandcompanions/compat/DIServerPetCommandCompat.class",
            "com/github/eterdelta/crittersandcompanions/entity/ai/goal/DICommandedFollowOwnerGoal.class",
            "com/github/eterdelta/crittersandcompanions/entity/ai/goal/DICommandedFollowOwnerGoal.class"
    );

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 3) {
            throw new IllegalArgumentException("Usage: CrittersAndCompanionsCommandJarPatcher <input-jar> <output-jar> [compiled-classes-dir]");
        }
        Path compiledClasses = args.length == 3 ? Path.of(args[2]) : Path.of("build/tmp/critters_patch_classes");
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
                    if (TARGET_CLASS.equals(entry.getName())) {
                        bytes = patchDragonflyEntity(bytes);
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

    private static byte[] patchDragonflyEntity(byte[] classBytes) {
        if (new String(classBytes, StandardCharsets.ISO_8859_1).contains(PATCH_MARKER)) {
            return classBytes;
        }
        ClassNode node = new ClassNode();
        new ClassReader(classBytes).accept(node, 0);
        boolean goalPatched = false;
        boolean interactPatched = false;
        boolean tickPatched = false;
        for (MethodNode method : node.methods) {
            if (GOAL_METHOD.equals(method.name) && GOAL_DESC.equals(method.desc)) {
                patchGoalRegistration(method);
                goalPatched = true;
            } else if (INTERACT_METHOD.equals(method.name) && INTERACT_DESC.equals(method.desc)) {
                patchInteract(method);
                interactPatched = true;
            } else if (TICK_METHOD.equals(method.name) && TICK_DESC.equals(method.desc)) {
                patchTick(method);
                tickPatched = true;
            }
        }
        if (!goalPatched || !interactPatched || !tickPatched) {
            throw new IllegalStateException("Failed to patch DragonflyEntity completely");
        }
        ClassWriter writer = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static void patchGoalRegistration(MethodNode method) {
        boolean newPatched = false;
        boolean initPatched = false;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof TypeInsnNode typeInsn && typeInsn.getOpcode() == Opcodes.NEW && FOLLOW_OWNER_GOAL.equals(typeInsn.desc)) {
                typeInsn.desc = PATCHED_GOAL;
                newPatched = true;
            } else if (insn instanceof MethodInsnNode methodInsn
                    && methodInsn.getOpcode() == Opcodes.INVOKESPECIAL
                    && FOLLOW_OWNER_GOAL.equals(methodInsn.owner)
                    && "<init>".equals(methodInsn.name)) {
                methodInsn.owner = PATCHED_GOAL;
                initPatched = true;
            }
        }
        if (!newPatched || !initPatched) {
            throw new IllegalStateException("Failed to replace DragonflyEntity follow goal");
        }
    }

    private static void patchInteract(MethodNode method) {
        boolean patched = false;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode methodInsn)) {
                continue;
            }
            if (methodInsn.getOpcode() != Opcodes.INVOKEVIRTUAL
                    || !"com/github/eterdelta/crittersandcompanions/entity/DragonflyEntity".equals(methodInsn.owner)
                    || !"m_21839_".equals(methodInsn.name)
                    || !"(Z)V".equals(methodInsn.desc)) {
                continue;
            }
            InsnList injected = new InsnList();
            injected.add(new VarInsnNode(Opcodes.ALOAD, 0));
            injected.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    HELPER_OWNER,
                    "syncManualCommand",
                    "(Ljava/lang/Object;)V",
                    false
            ));
            method.instructions.insert(insn, injected);
            patched = true;
            break;
        }
        if (!patched) {
            throw new IllegalStateException("Failed to patch DragonflyEntity interaction toggle");
        }
    }

    private static void patchTick(MethodNode method) {
        InsnList injected = new InsnList();
        injected.add(new VarInsnNode(Opcodes.ALOAD, 0));
        injected.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                HELPER_OWNER,
                "tickSync",
                "(Ljava/lang/Object;)V",
                false
        ));
        method.instructions.insert(injected);
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
