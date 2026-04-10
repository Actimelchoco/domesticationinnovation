import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

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

public class CrittersAndCompanionsDragonflyJarPatcher {
    private static final String TARGET_CLASS = "com/github/eterdelta/crittersandcompanions/client/model/geo/DragonflyModel.class";
    private static final String TARGET_METHOD = "getTextureResource";
    private static final String TARGET_DESC = "(Lcom/github/eterdelta/crittersandcompanions/entity/DragonflyEntity;)Lnet/minecraft/resources/ResourceLocation;";
    private static final String PATCH_MARKER = "domesticationinnovation/dragonfly_armor_guard";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: CrittersAndCompanionsDragonflyJarPatcher <input-jar> <output-jar>");
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
        boolean changed = false;
        for (MethodNode method : node.methods) {
            if (TARGET_METHOD.equals(method.name) && TARGET_DESC.equals(method.desc)) {
                rewriteTextureMethod(node.name, method);
                changed = true;
            }
        }
        if (!changed) {
            throw new IllegalStateException("Did not find expected dragonfly texture method in " + node.name);
        }
        ClassWriter writer = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static void rewriteTextureMethod(String owner, MethodNode method) {
        method.instructions.clear();
        method.tryCatchBlocks.clear();
        method.localVariables.clear();
        method.visitCode();

        Label useDefaultIfEmpty = new Label();
        Label useDragonflyArmor = new Label();
        Label useDragonflyArmorItem = new Label();

        method.visitLabel(useDefaultIfEmpty);
        method.visitVarInsn(Opcodes.ALOAD, 1);
        method.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/github/eterdelta/crittersandcompanions/entity/DragonflyEntity",
                "getArmor",
                "()Lnet/minecraft/world/item/ItemStack;",
                false
        );
        method.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/world/item/ItemStack",
                "m_41619_",
                "()Z",
                false
        );
        method.visitJumpInsn(Opcodes.IFEQ, useDragonflyArmor);
        visitDefaultTextureReturn(method);

        method.visitLabel(useDragonflyArmor);
        method.visitVarInsn(Opcodes.ALOAD, 1);
        method.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/github/eterdelta/crittersandcompanions/entity/DragonflyEntity",
                "getArmor",
                "()Lnet/minecraft/world/item/ItemStack;",
                false
        );
        method.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/world/item/ItemStack",
                "m_41720_",
                "()Lnet/minecraft/world/item/Item;",
                false
        );
        method.visitInsn(Opcodes.DUP);
        method.visitTypeInsn(Opcodes.INSTANCEOF, "com/github/eterdelta/crittersandcompanions/item/DragonflyArmorItem");
        method.visitJumpInsn(Opcodes.IFNE, useDragonflyArmorItem);
        method.visitInsn(Opcodes.POP);
        visitDefaultTextureReturn(method);

        method.visitLabel(useDragonflyArmorItem);
        method.visitLdcInsn(PATCH_MARKER);
        method.visitInsn(Opcodes.POP);
        method.visitTypeInsn(Opcodes.CHECKCAST, "com/github/eterdelta/crittersandcompanions/item/DragonflyArmorItem");
        method.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/github/eterdelta/crittersandcompanions/item/DragonflyArmorItem",
                "getTexture",
                "()Lnet/minecraft/resources/ResourceLocation;",
                false
        );
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private static void visitDefaultTextureReturn(MethodVisitor method) {
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitVarInsn(Opcodes.ALOAD, 1);
        method.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "software/bernie/geckolib/model/DefaultedEntityGeoModel",
                "getTextureResource",
                "(Lsoftware/bernie/geckolib/core/animatable/GeoAnimatable;)Lnet/minecraft/resources/ResourceLocation;",
                false
        );
        method.visitInsn(Opcodes.ARETURN);
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
