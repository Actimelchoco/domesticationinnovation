import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;

public class ClassInspector {
    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 3) {
            throw new IllegalArgumentException("Usage: ClassInspector <jar-or-class-file> [class-entry] [method-name]");
        }
        byte[] bytes;
        String methodName = null;
        if (args[0].endsWith(".class")) {
            bytes = Files.readAllBytes(Path.of(args[0]));
            if (args.length == 2) {
                methodName = args[1];
            }
        } else {
            bytes = readJarEntry(Path.of(args[0]), args[1]);
            if (args.length == 3) {
                methodName = args[2];
            }
        }
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        System.out.println("Class: " + node.name);
        for (FieldNode field : node.fields) {
            System.out.println("Field: " + field.name + " " + field.desc);
        }
        for (MethodNode method : node.methods) {
            if (methodName != null && !method.name.equals(methodName)) {
                continue;
            }
            System.out.println("Method: " + method.name + method.desc);
            if (methodName == null) {
                continue;
            }
            for (AbstractInsnNode insn : method.instructions) {
                Printer printer = new Textifier();
                TraceMethodVisitor visitor = new TraceMethodVisitor(printer);
                insn.accept(visitor);
                StringWriter sw = new StringWriter();
                printer.print(new PrintWriter(sw));
                System.out.print(sw);
            }
        }
    }

    private static byte[] readJarEntry(Path jarPath, String entryName) throws Exception {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            return jar.getInputStream(jar.getJarEntry(entryName)).readAllBytes();
        }
    }
}
