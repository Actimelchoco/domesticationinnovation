import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class CrittersAndCompanionsDragonflyServerJarPatcher {
    private static final String TARGET_CLASS = "com/github/eterdelta/crittersandcompanions/entity/DragonflyEntity.class";
    private static final String TARGET_METHOD = "m_8061_";
    private static final String TARGET_DESC = "(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V";
    private static final String PATCH_MARKER = "domesticationinnovation/server_dragonfly_armor_guard";
    private static final String TICK_METHOD = "m_8024_";
    private static final String TICK_DESC = "()V";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: CrittersAndCompanionsDragonflyServerJarPatcher <input-jar> <output-jar>");
        }
        patchJar(Path.of(args[0]), Path.of(args[1]));
    }

    private static void patchJar(Path input, Path output) throws IOException {
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
                        bytes = patchClass(bytes);
                    }
                    jarOut.write(bytes);
                }
                jarOut.closeEntry();
            }
        }
    }

    private static byte[] patchClass(byte[] classBytes) {
        if (new String(classBytes, StandardCharsets.ISO_8859_1).contains(PATCH_MARKER)) {
            return classBytes;
        }
        ClassNode node = new ClassNode();
        new ClassReader(classBytes).accept(node, 0);
        boolean tickPatched = false;
        for (MethodNode method : node.methods) {
            if (TARGET_METHOD.equals(method.name) && TARGET_DESC.equals(method.desc)) {
                throw new IllegalStateException("DragonflyEntity already defines " + TARGET_METHOD + TARGET_DESC);
            }
            if (TICK_METHOD.equals(method.name) && TICK_DESC.equals(method.desc)) {
                patchTickMethod(method);
                tickPatched = true;
            }
        }
        if (!tickPatched) {
            throw new IllegalStateException("DragonflyEntity missing expected tick method " + TICK_METHOD + TICK_DESC);
        }
        node.methods.add(createGuardedSetItemSlotOverride());
        ClassWriter writer = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static void patchTickMethod(MethodNode method) {
        InsnList injected = new InsnList();
        LabelNode skipSanitize = new LabelNode();

        injected.add(new VarInsnNode(Opcodes.ALOAD, 0));
        injected.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "com/github/eterdelta/crittersandcompanions/entity/DragonflyEntity",
                "getArmor",
                "()Lnet/minecraft/world/item/ItemStack;",
                false
        ));
        injected.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/world/item/ItemStack",
                "m_41619_",
                "()Z",
                false
        ));
        injected.add(new JumpInsnNode(Opcodes.IFNE, skipSanitize));
        injected.add(new VarInsnNode(Opcodes.ALOAD, 0));
        injected.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "com/github/eterdelta/crittersandcompanions/entity/DragonflyEntity",
                "getArmor",
                "()Lnet/minecraft/world/item/ItemStack;",
                false
        ));
        injected.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/world/item/ItemStack",
                "m_41720_",
                "()Lnet/minecraft/world/item/Item;",
                false
        ));
        injected.add(new TypeInsnNode(
                Opcodes.INSTANCEOF,
                "com/github/eterdelta/crittersandcompanions/item/DragonflyArmorItem"
        ));
        injected.add(new JumpInsnNode(Opcodes.IFNE, skipSanitize));
        injected.add(new VarInsnNode(Opcodes.ALOAD, 0));
        injected.add(new FieldInsnNode(
                Opcodes.GETSTATIC,
                "net/minecraft/world/item/ItemStack",
                "f_41583_",
                "Lnet/minecraft/world/item/ItemStack;"
        ));
        injected.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "com/github/eterdelta/crittersandcompanions/entity/DragonflyEntity",
                "setArmor",
                "(Lnet/minecraft/world/item/ItemStack;)V",
                false
        ));
        injected.add(new LdcInsnNode(PATCH_MARKER));
        injected.add(new InsnNode(Opcodes.POP));
        injected.add(skipSanitize);

        method.instructions.insert(injected);
    }

    private static MethodNode createGuardedSetItemSlotOverride() {
        MethodNode method = new MethodNode(
                Opcodes.ACC_PUBLIC,
                TARGET_METHOD,
                TARGET_DESC,
                null,
                null
        );
        InsnList insns = method.instructions;
        LabelNode callSuperWithOriginal = new LabelNode();

        insns.add(new VarInsnNode(Opcodes.ALOAD, 1));
        insns.add(new FieldInsnNode(
                Opcodes.GETSTATIC,
                "net/minecraft/world/entity/EquipmentSlot",
                "CHEST",
                "Lnet/minecraft/world/entity/EquipmentSlot;"
        ));
        insns.add(new JumpInsnNode(Opcodes.IF_ACMPNE, callSuperWithOriginal));

        insns.add(new VarInsnNode(Opcodes.ALOAD, 2));
        insns.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/world/item/ItemStack",
                "m_41619_",
                "()Z",
                false
        ));
        insns.add(new JumpInsnNode(Opcodes.IFNE, callSuperWithOriginal));

        insns.add(new VarInsnNode(Opcodes.ALOAD, 2));
        insns.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/world/item/ItemStack",
                "m_41720_",
                "()Lnet/minecraft/world/item/Item;",
                false
        ));
        insns.add(new TypeInsnNode(
                Opcodes.INSTANCEOF,
                "com/github/eterdelta/crittersandcompanions/item/DragonflyArmorItem"
        ));
        insns.add(new JumpInsnNode(Opcodes.IFNE, callSuperWithOriginal));

        insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        insns.add(new VarInsnNode(Opcodes.ALOAD, 1));
        insns.add(new FieldInsnNode(
                Opcodes.GETSTATIC,
                "net/minecraft/world/item/ItemStack",
                "f_41583_",
                "Lnet/minecraft/world/item/ItemStack;"
        ));
        insns.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                "net/minecraft/world/entity/TamableAnimal",
                TARGET_METHOD,
                TARGET_DESC,
                false
        ));
        insns.add(new LdcInsnNode(PATCH_MARKER));
        insns.add(new InsnNode(Opcodes.POP));
        insns.add(new InsnNode(Opcodes.RETURN));

        insns.add(callSuperWithOriginal);
        insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        insns.add(new VarInsnNode(Opcodes.ALOAD, 1));
        insns.add(new VarInsnNode(Opcodes.ALOAD, 2));
        insns.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                "net/minecraft/world/entity/TamableAnimal",
                TARGET_METHOD,
                TARGET_DESC,
                false
        ));
        insns.add(new InsnNode(Opcodes.RETURN));
        method.maxLocals = 3;
        method.maxStack = 3;
        return method;
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
