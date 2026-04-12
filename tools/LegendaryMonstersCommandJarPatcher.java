import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
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

public class LegendaryMonstersCommandJarPatcher {
    private static final String HELPER_OWNER = "net/miauczel/legendary_monsters/compat/DIServerPetCommandCompat";
    private static final String PATCHED_GOAL = "net/miauczel/legendary_monsters/entity/ai/goal/DICommandedFollowOwnerGoal";
    private static final String PATCH_MARKER = HELPER_OWNER;
    private static final String GOAL_METHOD = "m_8099_";
    private static final String GOAL_DESC = "()V";
    private static final String INTERACT_METHOD = "m_6071_";
    private static final String INTERACT_DESC = "(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;";
    private static final String SIT_METHOD = "m_21839_";
    private static final String SIT_DESC = "(Z)V";
    private static final String WALK_KEY = "legendary_monsters.message.pet_walk_enable";
    private static final String SIT_KEY = "legendary_monsters.message.pet_sit_enable";
    private static final String DI_FOLLOW_KEY = "message.domesticationinnovation.command_2";
    private static final String DI_SIT_KEY = "message.domesticationinnovation.command_1";

    private static final Map<String, String> TARGET_CLASSES = Map.of(
            "net/miauczel/legendary_monsters/entity/AnimatedMonster/Mobs/Pets/FLivingArmorEntity.class",
            "net/miauczel/legendary_monsters/entity/AnimatedMonster/Mobs/Pets/FLivingArmorEntity$2",
            "net/miauczel/legendary_monsters/entity/AnimatedMonster/Mobs/Pets/FHauntedGuardEntity.class",
            "net/miauczel/legendary_monsters/entity/AnimatedMonster/Mobs/Pets/FHauntedGuardEntity$3",
            "net/miauczel/legendary_monsters/entity/AnimatedMonster/Mobs/Pets/MossyGolemEntity.class",
            "net/miauczel/legendary_monsters/entity/AnimatedMonster/Mobs/Pets/MossyGolemEntity$4"
    );

    private static final Map<String, String> INJECTED_CLASSES = Map.of(
            "net/miauczel/legendary_monsters/compat/DIServerPetCommandCompat.class",
            "net/miauczel/legendary_monsters/compat/DIServerPetCommandCompat.class",
            "net/miauczel/legendary_monsters/entity/ai/goal/DICommandedFollowOwnerGoal.class",
            "net/miauczel/legendary_monsters/entity/ai/goal/DICommandedFollowOwnerGoal.class"
    );

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 3) {
            throw new IllegalArgumentException("Usage: LegendaryMonstersCommandJarPatcher <input-jar> <output-jar> [compiled-classes-dir]");
        }
        Path compiledClasses = args.length == 3 ? Path.of(args[2]) : Path.of("build/tmp/legendary_patch_classes");
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
                    if (TARGET_CLASSES.containsKey(entry.getName())) {
                        bytes = patchClass(bytes, TARGET_CLASSES.get(entry.getName()));
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

    private static byte[] patchClass(byte[] classBytes, String originalGoalOwner) {
        if (containsMarker(classBytes) && !originalGoalOwner.contains("$")) {
            return classBytes;
        }
        ClassNode node = new ClassNode();
        new ClassReader(classBytes).accept(node, ClassReader.EXPAND_FRAMES);
        boolean changed = false;
        for (MethodNode method : node.methods) {
            if (GOAL_METHOD.equals(method.name) && GOAL_DESC.equals(method.desc)) {
                changed |= patchGoalRegistration(method, originalGoalOwner);
            } else if (INTERACT_METHOD.equals(method.name) && INTERACT_DESC.equals(method.desc)) {
                changed |= patchInteract(method, node.name);
            } else if (SIT_METHOD.equals(method.name) && SIT_DESC.equals(method.desc)) {
                changed |= patchActionBarKeys(method);
            }
        }
        if (!changed && !originalGoalOwner.contains("$")) {
            throw new IllegalStateException("Failed to patch " + node.name);
        }
        ClassWriter writer = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static boolean containsMarker(byte[] classBytes) {
        return new String(classBytes, StandardCharsets.ISO_8859_1).contains(PATCH_MARKER);
    }

    private static boolean patchGoalRegistration(MethodNode method, String originalGoalOwner) {
        boolean newPatched = false;
        boolean initPatched = false;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof TypeInsnNode typeInsn && typeInsn.getOpcode() == Opcodes.NEW && originalGoalOwner.equals(typeInsn.desc)) {
                typeInsn.desc = PATCHED_GOAL;
                newPatched = true;
            } else if (insn instanceof MethodInsnNode methodInsn
                    && methodInsn.getOpcode() == Opcodes.INVOKESPECIAL
                    && originalGoalOwner.equals(methodInsn.owner)
                    && "<init>".equals(methodInsn.name)) {
                VarInsnNode syntheticOuterLoad = findSecondPreviousAloadZero(methodInsn);
                methodInsn.owner = PATCHED_GOAL;
                methodInsn.desc = "(Lnet/minecraft/world/entity/TamableAnimal;DFFZ)V";
                if (syntheticOuterLoad != null) {
                    method.instructions.remove(syntheticOuterLoad);
                }
                initPatched = true;
            }
        }
        return newPatched && initPatched;
    }

    private static boolean patchInteract(MethodNode method, String ownerName) {
        boolean patched = false;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode methodInsn)) {
                continue;
            }
            if (methodInsn.getOpcode() != Opcodes.INVOKEVIRTUAL
                    || !ownerName.equals(methodInsn.owner)
                    || !SIT_METHOD.equals(methodInsn.name)
                    || !SIT_DESC.equals(methodInsn.desc)) {
                continue;
            }
            InsnList injected = new InsnList();
            injected.add(new VarInsnNode(Opcodes.ALOAD, 1));
            injected.add(new VarInsnNode(Opcodes.ALOAD, 0));
            injected.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    HELPER_OWNER,
                    "syncManualCommand",
                    "(Ljava/lang/Object;Ljava/lang/Object;)V",
                    false
            ));
            method.instructions.insert(insn, injected);
            patched = true;
        }
        return patched;
    }

    private static boolean patchActionBarKeys(MethodNode method) {
        boolean changed = false;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof LdcInsnNode ldc) || !(ldc.cst instanceof String value)) {
                continue;
            }
            if (SIT_KEY.equals(value)) {
                ldc.cst = DI_SIT_KEY;
                changed = true;
            } else if (WALK_KEY.equals(value)) {
                ldc.cst = DI_FOLLOW_KEY;
                changed = true;
            }
        }
        return changed;
    }

    private static AbstractInsnNode previousMeaningful(AbstractInsnNode insn) {
        AbstractInsnNode current = insn;
        while (current != null) {
            int type = current.getType();
            if (type != AbstractInsnNode.LABEL && type != AbstractInsnNode.FRAME && type != AbstractInsnNode.LINE) {
                return current;
            }
            current = current.getPrevious();
        }
        return null;
    }

    private static VarInsnNode findSecondPreviousAloadZero(AbstractInsnNode insn) {
        int seen = 0;
        for (AbstractInsnNode current = previousMeaningful(insn.getPrevious()); current != null; current = previousMeaningful(current.getPrevious())) {
            if (current instanceof VarInsnNode varInsn && varInsn.getOpcode() == Opcodes.ALOAD && varInsn.var == 0) {
                seen++;
                if (seen == 2) {
                    return varInsn;
                }
            }
        }
        return null;
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
