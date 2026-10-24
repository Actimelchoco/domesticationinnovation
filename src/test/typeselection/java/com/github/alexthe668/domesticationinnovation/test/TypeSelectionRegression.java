package com.github.alexthe668.domesticationinnovation.test;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class TypeSelectionRegression {
    static Object call(String name,Class<?> type,Object arg)throws Exception{
        var method=TameCommands.class.getDeclaredMethod(name,type);method.setAccessible(true);return method.invoke(null,arg);
    }
    @GameTest(template="empty") public static void moddedShortCompletionWorksInAdminCommand(GameTestHelper h)throws Exception{
        var keys=ForgeRegistries.ENTITY_TYPES.getKeys();
        ResourceLocation id=keys.stream().filter(k->!k.getNamespace().equals("minecraft")&&keys.stream().filter(o->o.getPath().equals(k.getPath())).count()==1).findFirst().orElseThrow();
        var suggestions=(List<?>)call("shortTypeSuggestions",Collection.class,List.of(id.toString(),"minecraft:wolf"));
        h.assertTrue(suggestions.contains(id.getPath())&&!suggestions.contains(id.toString()),"modded completion uses just typeId");
        h.assertTrue(id.equals(call("normalizeEntityTypeId",String.class,id.getPath())),"short modded name resolves to actual registry type");
        h.assertTrue(id.equals(call("normalizeEntityTypeId",String.class,id.toString())),"full names still work");
        var source=h.getLevel().getServer().createCommandSourceStack();
        var disable=TameCommands.class.getDeclaredMethod("adminDisableTameType",net.minecraft.commands.CommandSourceStack.class,String.class);disable.setAccessible(true);
        var remove=TameCommands.class.getDeclaredMethod("adminRemoveTameTypeDisable",net.minecraft.commands.CommandSourceStack.class,String.class);remove.setAccessible(true);
        boolean wasDisabled=TameRegistry.isTameTypeDisabled(id.toString());
        try{
            if(wasDisabled) TameRegistry.removeDisabledTameType(id.toString());
            h.assertTrue((int)disable.invoke(null,source,id.getPath())==1,"admin command accepts short modded completion");
            h.assertTrue(TameRegistry.getDisabledTameTypes().contains(id.toString()),"setting stores canonical namespaced type");
            h.assertTrue((int)remove.invoke(null,source,id.getPath())==1,"short name removes same setting");
        }finally{TameRegistry.removeDisabledTameType(id.toString());if(wasDisabled)TameRegistry.addDisabledTameType(id.toString());}
        boolean wasSummon=TameRegistry.getSummonTypes().contains(id.toString());
        try{
            TameRegistry.removeSummonType(id.toString());
            int added=h.getLevel().getServer().getCommands().performPrefixedCommand(source,"tames admin isSummon add "+id.getPath());
            h.assertTrue(added==1&&TameRegistry.getSummonTypes().contains(id.toString()),"actual summon command parses short modded ID without minecraft prefix");
            int removed=h.getLevel().getServer().getCommands().performPrefixedCommand(source,"tames admin isSummon remove "+id.getPath());
            h.assertTrue(removed==1&&!TameRegistry.getSummonTypes().contains(id.toString()),"actual summon remove accepts short ID");
        }finally{TameRegistry.removeSummonType(id.toString());if(wasSummon)TameRegistry.addSummonType(id.toString());}
        h.succeed();
    }
    @GameTest(template="empty") public static void duplicatePathsRemainUnambiguous(GameTestHelper h)throws Exception{
        String a="fixture_a:shared_type_test",b="fixture_b:shared_type_test";
        try{
            TameRegistry.addDisabledTameType(a);TameRegistry.addDisabledTameType(b);
            var suggestions=(List<?>)call("shortTypeSuggestions",Collection.class,List.of(a,b));
            h.assertTrue(suggestions.contains(a)&&suggestions.contains(b)&&!suggestions.contains("shared_type_test"),"duplicates retain mod prefixes");
            h.assertTrue("".equals(call("resolveCommandTypeId",String.class,"shared_type_test")),"ambiguous short name cannot silently choose a mod");
            h.assertTrue(a.equals(call("resolveCommandTypeId",String.class,a)),"full name selects exact duplicate");
        }finally{TameRegistry.removeDisabledTameType(a);TameRegistry.removeDisabledTameType(b);}
        h.succeed();
    }
}
