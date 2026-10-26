package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.gametest.*;
import java.util.*;
import java.util.stream.Collectors;

@GameTestHolder("domesticationinnovation") @PrefixGameTestTemplate(false)
public class DuelSelectionPhaseRegression {
    @SuppressWarnings("unchecked") static Map<UUID,Object> preparations() throws Exception {
        var f=TameCommands.class.getDeclaredField("SIMPLE_DUEL_PREPARATION_BY_PLAYER");f.setAccessible(true);return (Map<UUID,Object>)f.get(null);
    }
    static Set<String> commands(GameTestHelper h,ServerPlayer player){
        var node=h.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("tames").getChild("duel");
        return node.getChildren().stream().filter(n->n.canUse(player.createCommandSourceStack())).map(n->n.getName()).collect(Collectors.toSet());
    }
    @GameTest(template="empty") public static void selectionOnlyShowsUsefulActionsAndReadyUpdates(GameTestHelper h)throws Exception{
        try(var f=new DuelFollowerRegression.Fixture(h)){
            var type=Class.forName(TameCommands.class.getName()+"$SimpleDuelPreparation");var ctor=type.getDeclaredConstructor(UUID.class,UUID.class,long.class);ctor.setAccessible(true);
            Object preparation=ctor.newInstance(f.a.getUUID(),f.b.getUUID(),System.currentTimeMillis()+60000);preparations().put(f.a.getUUID(),preparation);preparations().put(f.b.getUUID(),preparation);
            try{
                h.assertTrue(commands(h,f.a).equals(Set.of("selection","start","add","addMyself","invite","addNearby","cancel")),"empty selection shows only useful editing/start/cancel commands: "+commands(h,f.a));
                int result=h.getLevel().getServer().getCommands().performPrefixedCommand(f.a.createCommandSourceStack(),"tames duel addMyself");h.assertTrue(result>0,"actual selection edit accepted");
                h.assertTrue(!commands(h,f.a).contains("addMyself")&&commands(h,f.a).contains("remove"),"selected player cannot add themselves twice; remove now available");
                h.getLevel().getServer().getCommands().performPrefixedCommand(f.a.createCommandSourceStack(),"tames duel start");
                h.assertTrue(!commands(h,f.a).contains("start")&&commands(h,f.b).contains("start"),"already-ready player does not need another start; opponent still does");
                h.getLevel().getServer().getCommands().performPrefixedCommand(f.b.createCommandSourceStack(),"tames duel addMyself");
                h.assertTrue(!commands(h,f.a).contains("start"),"opponent edits must not reset own ready state");
                h.getLevel().getServer().getCommands().performPrefixedCommand(f.a.createCommandSourceStack(),"tames duel remove myself");
                h.assertTrue(commands(h,f.a).contains("start")&&commands(h,f.a).contains("addMyself")&&!commands(h,f.a).contains("remove"),"own edit refreshes needed actions and resets own readiness");
            }finally{preparations().remove(f.a.getUUID());preparations().remove(f.b.getUUID());}
        }h.succeed();
    }
    @GameTest(template="empty") public static void battleOnlyShowsForfeitAndInfo(GameTestHelper h){
        try(var f=new DuelFollowerRegression.Fixture(h)){f.start(Set.of(f.a.getUUID()),Set.of(f.b.getUUID()));h.assertTrue(commands(h,f.a).equals(Set.of("ff","info")),"active battle hides all selection commands: "+commands(h,f.a));}h.succeed();
    }
}
