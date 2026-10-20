package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.entity.*;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameSpawnEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** One temporary, non-scoring defender per ranked battle. Threats come from events, never world scans. */
@Mod.EventBusSubscriber(modid = DomesticationMod.MODID)
public final class RankedRefereeService {
    public static final String MARKER = "TLRankedRefereeBattle";
    private static final Map<UUID, State> BATTLES = new HashMap<>();
    private static final Map<UUID, State> BODIES = new HashMap<>();
    private static final class State {
        final UUID battle;
        final ServerLevel level;
        final TameArenaRegistry.ArenaPoint post;
        final Map<UUID, Mob> threats = new LinkedHashMap<>();
        final Set<Entity> effects = Collections.newSetFromMap(new WeakHashMap<>());
        Mob body;
        TameData data;
        long nextSpawn, nextDecision, nextPath, nextAttack, returningSince;
        State(UUID battle, ServerLevel level, TameArenaRegistry.ArenaPoint post) { this.battle=battle;this.level=level;this.post=post; }
        Vec3 position() { return new Vec3(post.x(),post.y(),post.z()); }
    }
    private RankedRefereeService() {}
    public static boolean isReferee(Entity entity) { return entity instanceof Mob && entity.getPersistentData().hasUUID(MARKER); }
    public static boolean temporary(Entity entity) { return entity != null && entity.getPersistentData().hasUUID(MARKER); }
    public static TameData data(Entity entity) { State state=entity==null?null:BODIES.get(entity.getUUID());return state==null?null:state.data; }
    public static Mob body(UUID battle) { State state=BATTLES.get(battle);return state==null?null:state.body; }

    private static Entity parent(Entity entity) {
        return entity instanceof Projectile projectile ? projectile.getOwner()
                : entity instanceof EvokerFangs fangs ? fangs.getOwner()
                : entity instanceof PrimedTnt tnt ? tnt.getOwner()
                : entity instanceof AreaEffectCloud cloud ? cloud.getOwner()
                : entity instanceof ChainLightningEntity lightning ? lightning.getCreatorEntity()
                : entity instanceof OwnableEntity owned ? owned.getOwner() : null;
    }
    private static UUID sourceBattle(Entity entity) {
        for(int depth=0;entity!=null&&depth<8;depth++) {
            if(temporary(entity))return entity.getPersistentData().getUUID(MARKER);
            Entity next=parent(entity);if(next==entity)break;entity=next;
        }
        return null;
    }
    public static boolean blocks(Entity target, Entity source) {
        if(target==null||target.level().isClientSide)return false;
        if(isReferee(target))return true;
        UUID battle=sourceBattle(source);
        if(battle==null)return false;
        State state=BATTLES.get(battle);
        return state==null||!TameDuelManager.isRankedBattleActive(battle)||!state.threats.containsKey(target.getUUID())||!validThreat(state,target);
    }
    public static boolean blocks(Entity target, DamageSource source) {
        return source!=null&&(blocks(target,source.getEntity())||blocks(target,source.getDirectEntity()));
    }
    private static boolean validThreat(State state,Entity entity) {
        return entity instanceof Mob mob && entity instanceof Enemy && mob.isAlive()&&!mob.isRemoved()
                && !isReferee(mob)&&!TameEntityAdapter.isTame(mob)&&!TameDuelManager.isEntityInDuel(mob.getUUID())
                && !(mob instanceof OwnableEntity owned && owned.getOwnerUUID()!=null)
                && mob.level()==state.level && mob.position().distanceToSqr(state.position())<=128*128;
    }
    public static boolean alert(Mob hostile,LivingEntity participant) {
        if(!(participant.level() instanceof ServerLevel level))return false;
        UUID battle=TameDuelManager.rankedBattleId(participant.getUUID());if(battle==null)return false;
        State state=BATTLES.get(battle);
        if(state==null) {
            TameArenaRegistry.init(level.getServer());
            var arena=TameArenaRegistry.getArena(TameRegistry.getRankedArenaName());
            var post=arena==null?null:arena.refereeSpot();
            if(post==null||!post.isValid()||!post.dimensionId().equals(level.dimension().location().toString()))return false;
            state=new State(battle,level,post);
            if(!validThreat(state,hostile))return false;
            BATTLES.put(battle,state);
        }
        if(!validThreat(state,hostile))return false;
        if(state.threats.size()<64)state.threats.put(hostile.getUUID(),hostile);
        if(state.body==null&&level.getGameTime()>=state.nextSpawn)spawn(state);
        return true;
    }
    public static List<TameData> candidates() {
        Map<UUID,TameData> unique=new HashMap<>();
        for(UUID id:TameRegistry.getRankedParticipants()) {
            TameData data=TameRegistry.get(id);if(data==null)data=TameRegistry.getByTlId(id);
            if(data==null||data.dead||TameDuelManager.ignoresDuelCalls(data.uuid)||TameDuelManager.isEntityInDuel(data.uuid)||TameDuelManager.isEntityInDuel(data.tlId)
                    ||TameRegistry.getRankedForbiddenTameTypes().contains(data.type))continue;
            unique.put(data.ensureTlId(),data);
        }
        return unique.values().stream().sorted(Comparator.comparingInt((TameData data)->data.duelMmr).reversed()
                .thenComparing(data->data.tlId.toString())).toList();
    }
    private static void spawn(State state) {
        state.nextSpawn=state.level.getGameTime()+200;
        if(!state.level.hasChunkAt(BlockPos.containing(state.position())))return;
        for(TameData source:candidates()) {
            CompoundTag snapshot=source.entitySnapshot.copy();
            if(source.bewereagerState.getBoolean("Active")&&source.bewereagerState.contains("Wolf",10))snapshot=source.bewereagerState.getCompound("Wolf").copy();
            LivingEntity live=TameEntityAdapter.findLoaded(state.level.getServer(),source.uuid,source.tlId);
            if(live!=null) { snapshot=new CompoundTag();live.save(snapshot); }
            snapshot.remove("Passengers");
            Entity created=EntityType.loadEntityRecursive(snapshot,state.level,entity->entity);
            if(!(created instanceof Mob mob)||!TameEntityAdapter.isSupported(mob))continue;
            if(TameRegistry.isTameTypeDisabled(mob)||TameRegistry.isSummonType(mob))continue;
            mob.setUUID(UUID.randomUUID());
            var attributes=mob.getAttributes().save();
            if(mob instanceof TamableAnimal animal){animal.setOwnerUUID(null);animal.setTame(false);}
            else if(mob instanceof ModifedToBeTameable modified){modified.setTameOwnerUUID(null);modified.setTame(false);}
            mob.getAttributes().load(attributes);mob.setHealth(mob.getMaxHealth());mob.deathTime=0;mob.setNoAi(false);
            mob.getPersistentData().remove(TameLimitService.WILD);mob.getPersistentData().remove("TLReleasedHunterData");
            mob.getPersistentData().remove(TameData.IMPRINTED_OWNER_TAG);
            TameData.syncTlIdToEntity(mob,UUID.randomUUID());mob.getPersistentData().putUUID(MARKER,state.battle);
            mob.setInvulnerable(true);mob.setPersistenceRequired();mob.setCanPickUpLoot(false);
            mob.goalSelector.removeAllGoals(goal->true);mob.targetSelector.removeAllGoals(goal->true);mob.setTarget(null);
            mob.moveTo(state.post.x(),state.post.y(),state.post.z(),state.post.yRot(),state.post.xRot());
            mob.setCustomName(Component.literal(source.name+" [Referee]").withStyle(ChatFormatting.GOLD));mob.setCustomNameVisible(true);
            TameData data=TameData.fromTag(source.toTag());data.uuid=mob.getUUID();data.tlId=TameData.getTlId(mob);data.ownerUUID=null;
            data.mode=TameMode.MONSTER_HUNTER.id();data.movementOrder=1;data.hasHome=false;data.stored=false;data.dead=false;
            data.rosterBodyguard=false;data.bewereagerState=new CompoundTag();data.cooldowns.clear();
            state.body=mob;state.data=data;BODIES.put(mob.getUUID(),state);pose(mob,true);
            TameSpawnEvents.beginTameReconstruction();
            boolean added;
            try{added=state.level.addFreshEntity(mob);}finally{TameSpawnEvents.endTameReconstruction();}
            if(added)return;
            BODIES.remove(mob.getUUID());mob.discard();state.body=null;state.data=null;
        }
    }
    private static void pose(Mob mob,boolean sitting) {
        if(mob instanceof TamableAnimal animal){animal.setOrderedToSit(sitting);animal.setInSittingPose(sitting);}
        if(mob instanceof com.github.alexthe666.citadel.server.entity.IComandableMob commandable)commandable.setCommand(sitting?1:0);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||BATTLES.isEmpty())return;
        for(State state:new ArrayList<>(BATTLES.values())) {
            if(!TameDuelManager.isRankedBattleActive(state.battle)){end(state.battle);continue;}
            long now=state.level.getGameTime();
            Mob mob=state.body;
            if(mob==null){if(!state.threats.isEmpty()&&now>=state.nextSpawn)spawn(state);continue;}
            if(mob.isRemoved()||!mob.isAlive()){BODIES.remove(mob.getUUID());state.body=null;continue;}
            if(now>=state.nextDecision) {
                state.nextDecision=now+10;
                mob.getSensing().tick();
                state.threats.values().removeIf(threat->!validThreat(state,threat));
                Mob target=state.threats.values().stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
                mob.setTarget(target);
                if(target!=null){pose(mob,false);state.data.movementOrder=2;state.returningSince=0;}
                else if(mob.position().distanceToSqr(state.position())<=1.5*1.5) {
                    mob.getNavigation().stop();mob.moveTo(state.post.x(),state.post.y(),state.post.z(),state.post.yRot(),state.post.xRot());
                    pose(mob,true);state.data.movementOrder=1;state.returningSince=0;
                }else{pose(mob,false);state.data.movementOrder=2;if(state.returningSince==0)state.returningSince=now;}
            }
            LivingEntity target=mob.getTarget();
            if(target!=null&&validThreat(state,target)) {
                mob.getLookControl().setLookAt(target,30,30);
                if(now>=state.nextPath){mob.getNavigation().moveTo(target,1.15);state.nextPath=now+20;}
                double reach=Math.max(2.5,mob.getBbWidth()+target.getBbWidth());
                if(now>=state.nextAttack&&mob.distanceToSqr(target)<=reach*reach&&mob.getSensing().hasLineOfSight(target)) {
                    state.nextAttack=now+20;mob.swing(InteractionHand.MAIN_HAND);mob.doHurtTarget(target);
                }
            }else if(state.data.movementOrder!=1) {
                if(now>=state.nextPath){mob.getNavigation().moveTo(state.post.x(),state.post.y(),state.post.z(),1.0);state.nextPath=now+20;}
                if(state.returningSince>0&&now-state.returningSince>=200) {
                    mob.getNavigation().stop();mob.moveTo(state.post.x(),state.post.y(),state.post.z(),state.post.yRot(),state.post.xRot());
                    pose(mob,true);state.data.movementOrder=1;state.returningSince=0;
                }
            }
            if(state.data.movementOrder!=1){mob.getNavigation().tick();mob.getMoveControl().tick();mob.getLookControl().tick();mob.getJumpControl().tick();}
        }
    }
    public static void end(UUID battle) {
        State state=BATTLES.remove(battle);if(state==null)return;
        if(state.body!=null){BODIES.remove(state.body.getUUID());state.body.discard();}
        for(Entity effect:state.effects)if(effect!=null)effect.discard();
        state.threats.clear();state.effects.clear();
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void target(LivingChangeTargetEvent event) {
        LivingEntity target=event.getNewTarget();
        if(isReferee(event.getEntity())){if(target!=null&&blocks(target,event.getEntity()))event.setNewTarget(null);return;}
        if(target!=null&&event.getEntity() instanceof Mob mob&&alert(mob,target)) {
            event.setNewTarget(body(TameDuelManager.rankedBattleId(target.getUUID())));
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void attack(LivingAttackEvent event) {
        if(blocks(event.getEntity(),event.getSource())){event.setCanceled(true);return;}
        Entity source=event.getSource().getEntity();if(source==null)source=parent(event.getSource().getDirectEntity());
        if(source instanceof Mob mob&&alert(mob,event.getEntity()))event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void impact(ProjectileImpactEvent event) {
        if(event.getRayTraceResult() instanceof EntityHitResult hit) {
            if(blocks(hit.getEntity(),event.getProjectile()))event.setCanceled(true);
            else if(hit.getEntity() instanceof LivingEntity living&&event.getProjectile().getOwner() instanceof Mob mob&&alert(mob,living))event.setCanceled(true);
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void explosion(ExplosionEvent.Detonate event) {
        DamageSource source=event.getExplosion().getDamageSource();
        if(source!=null&&(sourceBattle(source.getEntity())!=null||sourceBattle(source.getDirectEntity())!=null))event.getExplosion().getToBlow().clear();
        event.getAffectedEntities().removeIf(target->blocks(target,event.getExplosion().getDamageSource()));
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void join(EntityJoinLevelEvent event) {
        if(event.getLevel().isClientSide)return;
        Entity entity=event.getEntity();UUID battle=sourceBattle(entity);
        if(battle==null)return;
        State state=BATTLES.get(battle);
        if(state==null||isReferee(entity)&&BODIES.get(entity.getUUID())!=state){entity.discard();event.setCanceled(true);return;}
        if(!isReferee(entity)){entity.getPersistentData().putUUID(MARKER,battle);state.effects.add(entity);}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void drops(LivingDropsEvent event){if(isReferee(event.getEntity()))event.setCanceled(true);}
    @SubscribeEvent public static void xp(LivingExperienceDropEvent event){if(isReferee(event.getEntity()))event.setDroppedExperience(0);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void interact(PlayerInteractEvent.EntityInteract event){if(isReferee(event.getTarget()))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific event){if(isReferee(event.getTarget()))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void tame(AnimalTameEvent event){if(isReferee(event.getAnimal()))event.setCanceled(true);}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){for(UUID battle:new ArrayList<>(BATTLES.keySet()))end(battle);}
}
