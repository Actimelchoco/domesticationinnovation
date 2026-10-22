package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.block.DIBlockRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.*;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class GuardianChestBodyguardRegression {
    static void refresh(GameTestHelper h)throws Exception{
        var field=GuardianChestBodyguards.class.getDeclaredField("nextRefresh");field.setAccessible(true);field.setInt(null,Integer.MIN_VALUE);
        GuardianChestBodyguards.refresh(h.getLevel().getServer());
    }
    static ItemStack drum(String rule){var stack=new ItemStack(DIBlockRegistry.DRUM.get().asItem());stack.setHoverName(Component.literal(rule));return stack;}
    @GameTest(template="empty") public static void realDrumSwitchesOnlineGuardianWithoutChangingItsDuty(GameTestHelper h)throws Exception {
        try(var f=new GuardianChestRegression.Fixture(h)){
            var protectedPlayer=SharedGuardianRegression.observer(f,UUID.randomUUID(),"ChestProtected",new ArrayList<>());
            protectedPlayer.setPos(f.post.getX()+4.5,f.post.getY(),f.post.getZ()+.5);
            f.container.setItem(2,drum("bodyguard ChestWolf:ChestProtected"));f.power(true);f.tick();refresh(h);
            h.assertTrue(!GuardianChestBodyguards.hasAssignment(h.getLevel().getServer(),f.data),"offline protected player leaves a normal guardian");
            var online=GuardianActivationRegression.online(h);online.add(protectedPlayer);SharedGuardianRegression.players(h).put(protectedPlayer.getUUID(),protectedPlayer);
            try {
                refresh(h);
                h.assertTrue(GuardianChestBodyguards.protectedPlayer(h.getLevel().getServer(),f.data)==protectedPlayer,"real drum resolves linked tame and online protected player");
                h.assertTrue(TameRegistry.isProtectedAttackTarget(f.wolf,protectedPlayer),"escort target is protected from own guardian abilities");
                var arrow=new net.minecraft.world.entity.projectile.Arrow(h.getLevel(),f.wolf);
                h.assertTrue(com.github.alexthe668.domesticationinnovation.server.tameslevel.events.FriendlyOwnerProtection.blocks(protectedPlayer,arrow),"guardian projectile damage cannot hurt protected player");
                h.assertTrue(com.github.alexthe668.domesticationinnovation.server.tameslevel.events.FriendlyOwnerProtection.blocks(protectedPlayer,h.getLevel().damageSources().explosion(f.wolf,f.wolf)),"guardian explosion damage cannot hurt protected player");
                int x=f.data.homeX,y=f.data.homeY,z=f.data.homeZ;f.data.bodyguardRange=1;
                f.wolf.tickCount=Math.floorMod(-f.wolf.getId(),10);GuardianChestBodyguards.tick(f.wolf,f.data);
                h.assertTrue(f.wolf.distanceToSqr(protectedPlayer)<1,"guardian follows protected player instead of its owner/post");
                h.assertTrue(f.data.hasHome&&!f.data.rosterBodyguard&&f.data.movementOrder==3&&f.data.mode==TameMode.MONSTER_HUNTER.id()
                        &&f.data.homeX==x&&f.data.homeY==y&&f.data.homeZ==z,"guardian identity, mode and post remain intact");
                f.container.setItem(2,ItemStack.EMPTY);f.scan();refresh(h);
                h.assertTrue(!GuardianChestBodyguards.hasAssignment(h.getLevel().getServer(),f.data),"removing drum ends escort");
                GuardianChestBodyguards.tick(f.wolf,f.data);
                h.assertTrue(f.wolf.blockPosition().equals(f.post),"former escort returns to guardian post");
                f.container.setItem(2,drum("bodyguard ChestWolf:ChestProtected"));f.scan();refresh(h);
                h.assertTrue(GuardianChestBodyguards.hasAssignment(h.getLevel().getServer(),f.data),"reinserted rule activates without another deployment");
                f.container.setItem(1,ItemStack.EMPTY);f.scan();refresh(h);
                h.assertTrue(!GuardianChestBodyguards.hasAssignment(h.getLevel().getServer(),f.data),"removed tame paper revokes escort");
                f.container.setItem(1,f.paper);f.scan();refresh(h);
                h.assertTrue(GuardianChestBodyguards.hasAssignment(h.getLevel().getServer(),f.data),"restored paper restores eligible escort");
                f.power(false);refresh(h);
                h.assertTrue(!GuardianChestBodyguards.hasAssignment(h.getLevel().getServer(),f.data),"power OFF revokes escort");
            } finally {online.remove(protectedPlayer);SharedGuardianRegression.players(h).remove(protectedPlayer.getUUID());}
        }h.succeed();
    }
    @GameTest(template="empty") public static void strongestThreeAcrossControllersAreSeparateFromNormalBodyguards(GameTestHelper h)throws Exception {
        try(var f=new GuardianChestRegression.Fixture(h)){
            var player=SharedGuardianRegression.observer(f,UUID.randomUUID(),"ThreeProtected",new ArrayList<>());
            var online=GuardianActivationRegression.online(h);online.add(player);SharedGuardianRegression.players(h).put(player.getUUID(),player);
            List<LivingEntity> bodies=new ArrayList<>();List<TameData> guards=new ArrayList<>();Object first=new Object(),second=new Object();
            var normalRoster=BodyguardService.roster(player);UUID normalId=UUID.randomUUID();normalRoster.bodyguards.add(normalId);
            try {
                Map<UUID,GuardianChestBodyguards.Rule> a=new HashMap<>(),b=new HashMap<>();
                for(int i=0;i<5;i++){
                    var wolf=EntityType.WOLF.create(h.getLevel());TameEntityAdapter.setOwner(wolf,f.owner.getUUID());wolf.setNoAi(true);h.getLevel().addFreshEntity(wolf);bodies.add(wolf);
                    var data=new TameData(wolf);data.level=10*(i+1);data.hasHome=true;data.movementOrder=3;data.homeDimension=h.getLevel().dimension().location().toString();
                    data.homeX=f.post.getX();data.homeY=f.post.getY();data.homeZ=f.post.getZ();data.guardianSetAnchors.put("night",f.data.guardianSetAnchors.get("night").copy());TameRegistry.register(data);guards.add(data);
                    (i<2?a:b).put(data.ensureTlId(),new GuardianChestBodyguards.Rule(f.owner.getUUID(),"night","ThreeProtected"));
                }
                GuardianChestBodyguards.replace(first,a);GuardianChestBodyguards.replace(second,b);refresh(h);
                for(int i=0;i<5;i++)h.assertTrue(GuardianChestBodyguards.hasAssignment(h.getLevel().getServer(),guards.get(i))==(i>=2),"global cap chooses highest three levels across controllers");
                h.assertTrue(normalRoster.bodyguards.contains(normalId)&&normalRoster.bodyguards.size()==1,"normal bodyguard roster is independent");
                guards.get(0).level=60;refresh(h);
                h.assertTrue(GuardianChestBodyguards.hasAssignment(h.getLevel().getServer(),guards.get(0))&&!GuardianChestBodyguards.hasAssignment(h.getLevel().getServer(),guards.get(2)),"level change promotes stronger guardian and demotes weakest winner");
                online.remove(player);SharedGuardianRegression.players(h).remove(player.getUUID());refresh(h);
                for(var data:guards)h.assertTrue(!GuardianChestBodyguards.hasAssignment(h.getLevel().getServer(),data)&&data.hasHome&&!data.rosterBodyguard,"logout leaves all tames as guardians");
            } finally {online.remove(player);SharedGuardianRegression.players(h).remove(player.getUUID());normalRoster.bodyguards.remove(normalId);for(var data:guards)TameRegistry.remove(data.uuid);for(var body:bodies)body.discard();}
        }h.succeed();
    }
    @GameTest(template="empty") public static void wolfAndInterfaceGuardianDefendProtectedPlayer(GameTestHelper h)throws Exception {
        try(var f=new GuardianChestRegression.Fixture(h)){
            var player=SharedGuardianRegression.observer(f,UUID.randomUUID(),"DefendProtected",new ArrayList<>());
            player.setPos(f.post.getX()+.5,f.post.getY(),f.post.getZ()+.5);
            var online=GuardianActivationRegression.online(h);online.add(player);SharedGuardianRegression.players(h).put(player.getUUID(),player);
            var fox=EntityType.FOX.create(h.getLevel());TameEntityAdapter.setOwner(fox,f.owner.getUUID());fox.setNoAi(true);fox.setPos(player.position());h.getLevel().addFreshEntity(fox);
            var foxData=new TameData(fox,f.owner.getUUID(),false);foxData.hasHome=true;foxData.movementOrder=3;foxData.homeDimension=h.getLevel().dimension().location().toString();foxData.homeX=f.post.getX();foxData.homeY=f.post.getY();foxData.homeZ=f.post.getZ();foxData.guardianSetAnchors.put("night",f.data.guardianSetAnchors.get("night").copy());TameRegistry.register(foxData);
            var enemy=EntityType.ZOMBIE.create(h.getLevel());enemy.setNoAi(true);enemy.setPos(player.getX()+2,player.getY(),player.getZ());h.getLevel().addFreshEntity(enemy);
            try {
                f.power(true);f.tick();
                var rule=new GuardianChestBodyguards.Rule(f.owner.getUUID(),"night","DefendProtected");
                GuardianChestBodyguards.replace(new Object(),Map.of(f.data.ensureTlId(),rule,foxData.ensureTlId(),rule));refresh(h);
                player.setLastHurtByMob(enemy);
                for(var body:List.of(f.wolf,fox)){
                    var data=body==fox?foxData:f.data;body.tickCount=Math.floorMod(-body.getId(),10);
                    GuardianChestBodyguards.tick(body,data);h.assertTrue(body.getTarget()==enemy&&body.getLastHurtByMob()==enemy,"vanilla and interface guardians defend protected player's attacker and wake retaliation AI");
                }
                f.data.homeX++;refresh(h);
                h.assertTrue(!GuardianChestBodyguards.hasAssignment(h.getLevel().getServer(),f.data),"deployment outside the chest's saved group cannot escort");
            } finally {online.remove(player);SharedGuardianRegression.players(h).remove(player.getUUID());TameRegistry.remove(foxData.uuid);fox.discard();enemy.discard();}
        }h.succeed();
    }
}

