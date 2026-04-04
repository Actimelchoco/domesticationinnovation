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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class LegendaryMonstersDuelJarPatcher {
    private static final String HELPER_OWNER = "com/github/alexthe668/domesticationinnovation/server/tameslevel/compat/LegendaryMonstersDuelCompat";
    private static final String HELPER_NAME = "shouldAllowFriendlyFireInDuel";
    private static final String HELPER_DESC = "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)Z";

    private static final Map<String, String> TARGET_CLASSES = Map.of(
            "net/miauczel/legendary_monsters/entity/AnimatedMonster/Mobs/Pets/FHauntedGuardEntity.class", "m_269323_",
            "net/miauczel/legendary_monsters/entity/AnimatedMonster/Mobs/Pets/FLivingArmorEntity.class", "m_269323_"
    );

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: LegendaryMonstersDuelJarPatcher <input-jar> <output-jar>");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        patchJar(input, output);
    }

    private static void patchJar(Path input, Path output) throws IOException {
        Files.createDirectories(output.getParent());
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
        }
    }

    private static byte[] patchClass(byte[] classBytes, String ownerMethodName) {
        ClassNode node = new ClassNode();
        new ClassReader(classBytes).accept(node, 0);
        boolean changed = false;
        for (MethodNode method : node.methods) {
            if (!"AreaAttack".equals(method.name)) {
                continue;
            }
            changed |= patchAreaAttack(method, ownerMethodName);
        }
        if (!changed) {
            throw new IllegalStateException("Did not patch expected AreaAttack method in " + node.name);
        }
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static boolean patchAreaAttack(MethodNode method, String ownerMethodName) {
        boolean changed = false;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof JumpInsnNode jump) || jump.getOpcode() != Opcodes.IF_ACMPEQ) {
                continue;
            }
            AbstractInsnNode firstPrev = previousMeaningful(insn.getPrevious());
            AbstractInsnNode secondPrev = previousMeaningful(firstPrev == null ? null : firstPrev.getPrevious());
            AbstractInsnNode thirdPrev = previousMeaningful(secondPrev == null ? null : secondPrev.getPrevious());
            if (!(firstPrev instanceof MethodInsnNode firstMethod) || !(secondPrev instanceof VarInsnNode secondVar) || !(thirdPrev instanceof MethodInsnNode thirdMethod)) {
                continue;
            }
            if (!ownerMethodName.equals(firstMethod.name) || !ownerMethodName.equals(thirdMethod.name)) {
                continue;
            }
            if (secondVar.getOpcode() != Opcodes.ALOAD || secondVar.var != 0) {
                continue;
            }
            LabelNode allowDamage = new LabelNode();
            InsnList injected = new InsnList();
            injected.add(new JumpInsnNode(Opcodes.IF_ACMPNE, allowDamage));
            injected.add(new VarInsnNode(Opcodes.ALOAD, 0));
            injected.add(new VarInsnNode(Opcodes.ALOAD, 9));
            injected.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER_OWNER, HELPER_NAME, HELPER_DESC, false));
            injected.add(new JumpInsnNode(Opcodes.IFNE, allowDamage));
            injected.add(new JumpInsnNode(Opcodes.GOTO, jump.label));
            injected.add(allowDamage);
            method.instructions.insert(insn, injected);
            method.instructions.remove(insn);
            changed = true;
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
}
