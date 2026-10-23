import org.objectweb.asm.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Patch only the four audited active-index operations; preserve stack frames and all other entries. */
public final class PatchAuraChunkIndex {
    private static final String MAP="it/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap";
    private static final String GUARD="de/ellpeck/naturesaura/compat/AuraChunkIndexGuard";
    private static final Map<String,String> TARGETS=Map.of(
            "de/ellpeck/naturesaura/chunk/AuraChunk.class","addOrRemoveAsActive",
            "de/ellpeck/naturesaura/events/CommonEvents.class","onChunkUnload",
            "de/ellpeck/naturesaura/Helper.class","getAuraChunksWithSpotsInArea");
    public static void main(String[] args)throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("input.jar output.jar compiled-guard.class");
        Path input=Path.of(args[0]),output=Path.of(args[1]);
        if(input.toRealPath().equals(output.toAbsolutePath().normalize()))throw new IllegalArgumentException("Input must stay read-only");
        Map<String,Integer> changes=new TreeMap<>();
        try(var original=new ZipFile(input.toFile());var result=new ZipOutputStream(Files.newOutputStream(output))){
            var entries=original.entries();
            while(entries.hasMoreElements()){
                var entry=entries.nextElement();if(entry.isDirectory())continue;
                byte[] bytes=original.getInputStream(entry).readAllBytes();
                if(TARGETS.containsKey(entry.getName())){
                    var writer=new ClassWriter(0);
                    new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9,writer){
                        @Override public MethodVisitor visitMethod(int access,String name,String desc,String signature,String[] exceptions){
                            var visitor=super.visitMethod(access,name,desc,signature,exceptions);
                            if(!name.equals(TARGETS.get(entry.getName())))return visitor;
                            return new MethodVisitor(Opcodes.ASM9,visitor){
                                @Override public void visitMethodInsn(int opcode,String owner,String method,String descriptor,boolean iface){
                                    if(opcode==Opcodes.INVOKEVIRTUAL&&owner.equals(MAP)&&Set.of("get","put","remove").contains(method)){
                                        String expected=method.equals("put")?"(JLjava/lang/Object;)Ljava/lang/Object;":"(J)Ljava/lang/Object;";
                                        if(!descriptor.equals(expected))throw new IllegalStateException("Unexpected signature "+descriptor);
                                        changes.merge(entry.getName()+":"+method,1,Integer::sum);
                                        super.visitMethodInsn(Opcodes.INVOKESTATIC,GUARD,method,"(L"+MAP+";"+descriptor.substring(1),false);
                                    }else super.visitMethodInsn(opcode,owner,method,descriptor,iface);
                                }
                            };
                        }
                    },0);
                    bytes=writer.toByteArray();
                }
                result.putNextEntry(new ZipEntry(entry.getName()));result.write(bytes);result.closeEntry();
            }
            result.putNextEntry(new ZipEntry(GUARD+".class"));result.write(Files.readAllBytes(Path.of(args[2])));result.closeEntry();
        }
        var expected=Map.of("de/ellpeck/naturesaura/chunk/AuraChunk.class:put",1,
                "de/ellpeck/naturesaura/chunk/AuraChunk.class:remove",1,
                "de/ellpeck/naturesaura/events/CommonEvents.class:remove",1,
                "de/ellpeck/naturesaura/Helper.class:get",1);
        if(!changes.equals(expected)){Files.delete(output);throw new IllegalStateException("Unexpected patch sites: "+changes);}
        System.out.println("Patched four audited operations: "+changes);
    }
}
