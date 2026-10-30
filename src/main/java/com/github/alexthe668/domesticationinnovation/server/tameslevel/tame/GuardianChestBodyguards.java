package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.ai.memory.*;
import net.minecraft.world.entity.player.Player;
import java.util.*;

/** Chest rules are indexed once; every eligible guardian may protect its assigned online player. */
public final class GuardianChestBodyguards {
    public record Rule(UUID owner, String group, String player) { }
    private record Assignment(Object source, Rule rule, UUID player) { }
    private static final Map<Object,Map<UUID,Rule>> SOURCES=new IdentityHashMap<>();
    private static final Map<UUID,Assignment> ACTIVE=new HashMap<>();
    private static final Set<UUID> RETURNING=new HashSet<>();
    private static int nextRefresh=Integer.MIN_VALUE;
    private GuardianChestBodyguards() { }
    private static UUID id(TameData data){return data.tlId!=null?data.tlId:data.uuid;}

    public static void replace(Object source,Map<UUID,Rule> rules){
        if(rules.isEmpty())SOURCES.remove(source);else SOURCES.put(source,Map.copyOf(rules));
    }
    public static void remove(Object source){SOURCES.remove(source);}
    private static boolean eligible(TameData data,Rule rule){
        if(data==null||!data.hasHome||data.rosterBodyguard||data.dead||data.stored||!Objects.equals(data.ownerUUID,rule.owner))return false;
        var post=data.guardianSetAnchors.get(rule.group);
        return post!=null&&Objects.equals(post.getString("dimension"),data.homeDimension)
                &&post.getInt("x")==data.homeX&&post.getInt("y")==data.homeY&&post.getInt("z")==data.homeZ;
    }
    public static UUID protectedPlayerId(TameData data){
        if(data==null)return null;
        Assignment assignment=ACTIVE.get(id(data));if(assignment==null)return null;
        var rules=SOURCES.get(assignment.source);
        if(rules==null||!assignment.rule.equals(rules.get(id(data)))||!eligible(data,assignment.rule))return null;
        return assignment.player;
    }
    public static ServerPlayer protectedPlayer(MinecraftServer server,TameData data){
        UUID player=protectedPlayerId(data);
        return player==null?null:server.getPlayerList().getPlayer(player);
    }
    public static void refresh(MinecraftServer server){
        int now=server.getTickCount();if(now<nextRefresh)return;nextRefresh=now+20;
        Map<String,ServerPlayer> online=new HashMap<>();
        for(var player:server.getPlayerList().getPlayers())online.put(player.getGameProfile().getName().toLowerCase(Locale.ROOT),player);
        Map<UUID,Assignment> candidates=new HashMap<>();Set<UUID> conflicts=new HashSet<>();
        // Sources are few and bounded by loaded controllers, not the global tame registry.
        for(var source:SOURCES.entrySet())for(var entry:source.getValue().entrySet()){
            UUID id=entry.getKey();Rule rule=entry.getValue();TameData data=TameRegistry.getByTlId(id);
            if(!eligible(data,rule))continue;
            var player=online.get(rule.player.toLowerCase(Locale.ROOT));if(player==null)continue;
            var assignment=new Assignment(source.getKey(),rule,player.getUUID());
            var previous=candidates.putIfAbsent(id,assignment);
            if(previous!=null&&!previous.player.equals(assignment.player))conflicts.add(id);
        }
        Map<UUID,Assignment> selected=new HashMap<>();
        for(var entry:candidates.entrySet()){
            if(!conflicts.contains(entry.getKey()))selected.put(entry.getKey(),entry.getValue());
        }
        for(UUID old:ACTIVE.keySet())if(!selected.containsKey(old)||!ACTIVE.get(old).player.equals(selected.get(old).player))RETURNING.add(old);
        ACTIVE.clear();ACTIVE.putAll(selected);RETURNING.removeAll(selected.keySet());
        RETURNING.removeIf(identity->{var data=TameRegistry.getByTlId(identity);return data==null||!data.hasHome||data.dead||data.stored||data.rosterBodyguard||data.guardianResting;});
    }
    public static boolean hasAssignment(MinecraftServer server,TameData data){return protectedPlayer(server,data)!=null;}
    public static boolean protects(UUID attacker,Entity target){
        if(attacker==null||target==null||target.getServer()==null)return false;
        var data=TameRegistry.get(attacker);var player=protectedPlayer(target.getServer(),data);
        return player!=null&&(player.getUUID().equals(target.getUUID())||player.getUUID().equals(
                com.github.alexthe668.domesticationinnovation.server.tameslevel.events.FriendlyOwnerProtection.owner(target)));
    }
    public static boolean isReturning(TameData data){return RETURNING.contains(id(data));}
    public static void returned(TameData data){RETURNING.remove(id(data));}

    /** Called from the existing loaded-tame tick; heavy work is staggered to ten ticks. */
    public static boolean tick(PathfinderMob tame,TameData data){
        var server=tame.getServer();if(server==null)return false;
        UUID identity=id(data);ServerPlayer player=protectedPlayer(server,data);
        if(player==null){
            if(RETURNING.contains(identity)){
                if(!data.hasHome||data.dead||data.stored||data.rosterBodyguard)RETURNING.remove(identity);
                else if((tame.tickCount+tame.getId())%10==0&&TameCommands.returnChestBodyguardToPost(tame,data))RETURNING.remove(identity);
                return RETURNING.contains(identity);
            }
            return false;
        }
        if((tame.tickCount+tame.getId())%10!=0)return true;
        if(data.guardianResting||data.hungerForcedSit)return true;
        double range=Math.max(1,Math.min(22,data.bodyguardRange));
        if(tame.level()!=player.level()||tame.distanceToSqr(player)>range*range){
            tame.setTarget(null);TameCommands.followChestBodyguardPlayer(tame,data,player);return true;
        }
        LivingEntity target=tame.getTarget();
        if(!valid(tame,player,target)){
            target=player.getLastHurtByMob();
            if(!valid(tame,player,target))target=player.getLastHurtMob();
            if(!valid(tame,player,target)){
                target=null;double best=Double.MAX_VALUE;
                for(var candidate:tame.level().getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(16),
                        e->e instanceof Enemy&&valid(tame,player,e))){
                    double distance=player.distanceToSqr(candidate);if(distance<best){best=distance;target=candidate;}
                }
            }
            if(tame.getTarget()!=target){
                tame.setTarget(target);
                // Some native combat goals only wake on a retaliation marker.
                if(target!=null&&tame.getTarget()==target)tame.setLastHurtByMob(target);
            }
        }
        if(target==null&&tame.distanceToSqr(player)>4){
            if(tame.getBrain().checkMemory(MemoryModuleType.WALK_TARGET,MemoryStatus.REGISTERED))
                tame.getBrain().setMemory(MemoryModuleType.WALK_TARGET,new WalkTarget(player,1.25F,2));
            tame.getNavigation().moveTo(player,1.25D);
        }
        return true;
    }
    private static boolean valid(PathfinderMob tame,ServerPlayer player,LivingEntity target){
        if(target==null)return false;
        UUID targetOwner=com.github.alexthe668.domesticationinnovation.server.tameslevel.events.FriendlyOwnerProtection.owner(target);
        if(player.getUUID().equals(targetOwner)||Objects.equals(TameEntityAdapter.ownerUuid(tame),targetOwner)&&targetOwner!=null)return false;
        return target!=null&&target!=tame&&target!=player&&target.isAlive()&&!target.isRemoved()
                &&target.level()==tame.level()&&(!(target instanceof Player other)||!other.isCreative()&&!other.isSpectator())
                &&!TameRegistry.isProtectedAttackTarget(tame,target);
    }
    public static void clear(){SOURCES.clear();ACTIVE.clear();RETURNING.clear();nextRefresh=Integer.MIN_VALUE;}
}
