package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.github.alexthe668.domesticationinnovation.server.item.DIItemRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.PrimitiveMobsCompat;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

@net.minecraftforge.fml.common.Mod("primitive_compat_tests")
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="primitive_compat_tests")
public class PrimitiveFuseRegression {
    static class Fixture {
        final net.minecraft.server.level.ServerLevel level;
        Fixture(net.minecraft.server.level.ServerLevel level){this.level=level;}
        net.minecraft.server.level.ServerLevel getLevel(){return level;}
        net.minecraft.core.BlockPos absolutePos(net.minecraft.core.BlockPos p){return p.offset(0,-59,0);}
        void assertTrue(boolean value,String text){if(!value)throw new IllegalStateException(text);}
        void succeed(){}
    }
    @SubscribeEvent public static void run(net.minecraftforge.event.server.ServerStartedEvent event){
        var logger=org.apache.logging.log4j.LogManager.getLogger("PrimitiveFuseRegression");
        try{
            var h=new Fixture(event.getServer().overworld());
            naturalFestiveFuseSurvivesAndRespectsCooldown(h);
            wildAndSittingFestiveDoNotGetCombatFuse(h);
            actualPrimitivePetsAcceptOwnerCollars(h);
            otherCreeperBlastsPreserveBlocks(h);
            explosionDamageAndLearnedRadiusScale(h);
            logger.info("PRIMITIVE_FUSE_TESTS_PASS: native fuse, survival, cooldown, repeat blast, wild/sitting exclusion, five species collars, rocket launch/landing, support fuse, thrown TNT, block protection, native damage scaling, learned radius/damage/friendly protection");
        }catch(Throwable error){logger.error("PRIMITIVE_FUSE_TESTS_FAIL",error);}
        finally{event.getServer().halt(false);}
    }
    static void explosionDamageAndLearnedRadiusScale(Fixture h)throws Exception {
        var level=h.getLevel();
        for(String name:List.of("rocket_creeper","support_creeper","festive_creeper")){
            var pet=create(h,name,true);var data=new TameData(pet,TameEntityAdapter.ownerUuid(pet),false);
            data.bonusDamage=4;data.bonusKnockback=2;TameRegistry.register(data);
            try{
                var damage=new net.minecraftforge.event.entity.living.LivingHurtEvent(pet,level.damageSources().explosion(pet,pet),10);
                PrimitiveMobsCompat.scaleNativeBlast(damage);
                h.assertTrue(damage.getAmount()==14,name+" native damage adds earned attack damage once");
                h.assertTrue(PrimitiveMobsCompat.petBlastRadius(pet,3)==5,name+" native strength includes earned knockback");
                var melee=new net.minecraftforge.event.entity.living.LivingHurtEvent(pet,level.damageSources().mobAttack(pet),10);
                PrimitiveMobsCompat.scaleNativeBlast(melee);h.assertTrue(melee.getAmount()==10,"melee and learned-ability damage are not boosted twice");
            }finally{TameRegistry.remove(pet.getUUID());pet.discard();}
        }
        var minionType=ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("mutantmonsters:creeper_minion"));
        if(minionType!=null){
            var minion=(net.minecraft.world.entity.TamableAnimal)minionType.create(level);minion.setTame(true);minion.setOwnerUUID(UUID.randomUUID());
            minion.setPos(2,-57,2);level.addFreshEntity(minion);var md=new TameData(minion,minion.getOwnerUUID(),false);md.bonusDamage=4;md.bonusKnockback=2;TameRegistry.register(md);
            try{
                var sync=com.github.alexthe668.domesticationinnovation.server.CommonProxy.class.getDeclaredMethod("enforceNoGriefMutantCreeperMinion",Entity.class);sync.setAccessible(true);
                float before=((Number)minion.getClass().getMethod("getExplosionRadius").invoke(minion)).floatValue();
                sync.invoke(null,minion);sync.invoke(null,minion);
                float after=((Number)minion.getClass().getMethod("getExplosionRadius").invoke(minion)).floatValue();
                h.assertTrue(Math.abs(after-before-2)<.001,"minion knockback radius is synchronized once without accumulating");
                float base=10;
                try{var patch=Class.forName("fuzs.mutantmonsters.compat.DITameCreeperMinionCompat");base=((Number)patch.getMethod("adjustExplosionDamage",Object.class,Object.class,float.class).invoke(null,minion,new Object(),base)).floatValue();}catch(ClassNotFoundException ignored){}
                var hurt=new net.minecraftforge.event.entity.living.LivingHurtEvent(minion,level.damageSources().explosion(minion,minion),base);
                PrimitiveMobsCompat.scaleNativeBlast(hurt);h.assertTrue(hurt.getAmount()==14,"minion damage bonus applies exactly once with or without the legacy patch");
            }finally{TameRegistry.remove(minion.getUUID());minion.discard();}
        }
        var owner=UUID.randomUUID();var wolf=EntityType.WOLF.create(level);wolf.setTame(true);wolf.setOwnerUUID(owner);wolf.setNoAi(true);wolf.setPos(0,-57,0);level.addFreshEntity(wolf);
        var data=new TameData(wolf,owner,false);data.abilities.add("creeper_explosion");data.abilityLevels.put("creeper_explosion",4);data.bonusKnockback=2;
        data.mode=TameMode.MONSTER_HUNTER.id();TameRegistry.register(data);
        var near=EntityType.HUSK.create(level);near.setPos(5.5,-57,0);near.setNoAi(true);level.addFreshEntity(near);
        var outside=EntityType.HUSK.create(level);outside.setPos(6.5,-57,0);outside.setNoAi(true);level.addFreshEntity(outside);
        var friendly=EntityType.WOLF.create(level);friendly.setTame(true);friendly.setOwnerUUID(owner);friendly.setPos(2,-57,0);level.addFreshEntity(friendly);
        try{
            var events=com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameAbilityEvents.class;
            h.assertTrue(Math.abs(com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameAbilityEvents.creeperExplosionRadius(data,4)-5.9)<.0001,"learned radius includes level and knockback");
            float low=com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameAbilityEvents.offensiveAbilityCastDamage(data,"creeper_explosion",4);data.bonusDamage=20;
            h.assertTrue(com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameAbilityEvents.offensiveAbilityCastDamage(data,"creeper_explosion",4)>low,"learned damage scales with attack bonus");
            var cast=events.getDeclaredMethod("handleCreeperExplosion",net.minecraft.server.level.ServerLevel.class,LivingEntity.class,TameData.class,LivingEntity.class,long.class);cast.setAccessible(true);
            float nearHp=near.getHealth(),outsideHp=outside.getHealth(),friendlyHp=friendly.getHealth();
            cast.invoke(null,level,wolf,data,near,10000L);
            h.assertTrue(near.getHealth()<nearHp,"learned blast reaches an enemy beyond the old fixed three-block query");
            h.assertTrue(outside.getHealth()==outsideHp,"learned blast obeys its real spherical range");
            h.assertTrue(friendly.getHealth()==friendlyHp&&friendly.getDeltaMovement().lengthSqr()==0,"learned blast does not damage or shove friendly tames");
            data.bonusKnockback=1e9;h.assertTrue(com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameAbilityEvents.creeperExplosionRadius(data,Integer.MAX_VALUE)==32,"extreme stats cannot create an unbounded entity query");
        }finally{TameRegistry.remove(wolf.getUUID());wolf.discard();near.discard();outside.discard();friendly.discard();}
    }
    static void otherCreeperBlastsPreserveBlocks(Fixture h)throws Exception {
        var level=h.getLevel();
        for(String name:List.of("rocket_creeper","support_creeper","festive_creeper")){
            var pet=create(h,name,true);
            var blocks=new ArrayList<net.minecraft.core.BlockPos>();
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
                var p=pet.blockPosition().offset(x,-1,z);blocks.add(p);
                level.setBlock(p,net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState(),3);
            }
            var blasts=new Blasts(pet);MinecraftForge.EVENT_BUS.register(blasts);
            Entity target=null;
            try{
                if(name.equals("rocket_creeper")){
                    var enemy=EntityType.HUSK.create(level);target=enemy;
                    enemy.setPos(pet.getX()+2,pet.getY(),pet.getZ());enemy.setNoAi(true);level.addFreshEntity(enemy);pet.setTarget(enemy);
                    for(int i=0;i<17;i++){pet.setOnGround(true);pet.setSwellDir(1);pet.tick();}
                    h.assertTrue((boolean)pet.getClass().getMethod("isLaunched").invoke(pet),"native rocket tick launches toward its enemy");
                    pet.setHealth(1);pet.causeFallDamage(8,1,level.damageSources().fall());
                    h.assertTrue(pet.getHealth()==1,"rocket landing blast survives even at one HP");
                }else if(name.equals("support_creeper")){
                    pet.setHealth(pet.getMaxHealth()/4);pet.ignite();
                    for(int i=0;i<32;i++)pet.tick();
                }else{
                    var type=Class.forName("com.misanthropy.primitive_mobs.entity.projectile.PrimitiveTnt");
                    var tnt=(Entity)type.getConstructor(net.minecraft.world.level.Level.class,double.class,double.class,double.class,LivingEntity.class,float.class,int.class)
                        .newInstance(level,pet.getX(),pet.getY(),pet.getZ(),pet,3F,1);
                    tnt.tick();
                }
                h.assertTrue(blasts.count==1,name+" native attack produces its blast");
                h.assertTrue(pet.isAlive()&&!pet.isRemoved(),name+" survives its attack");
                for(var p:blocks)h.assertTrue(level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.GLASS),name+" preserves nearby fragile glass");
            }finally{
                MinecraftForge.EVENT_BUS.unregister(blasts);pet.discard();if(target!=null)target.discard();
                for(var p:blocks)level.setBlock(p,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            }
        }
    }
    public static class Blasts {
        final Entity source; int count;
        Blasts(Entity source) { this.source=source; }
        @SubscribeEvent public void blast(ExplosionEvent.Detonate event) {
            if(event.getExplosion().getDirectSourceEntity()==source) count++;
        }
    }
    static Creeper create(Fixture h,String type,boolean tame) {
        var pet=(Creeper)ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("primitive_mobs",type)).create(h.getLevel());
        var pos=h.absolutePos(new net.minecraft.core.BlockPos(2,2,2));
        pet.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        pet.setNoAi(true);pet.setNoGravity(true);h.getLevel().addFreshEntity(pet);
        if(tame){var owner=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"FuseOwner"));PrimitiveMobsCompat.tame(pet,owner);}
        return pet;
    }
    public static void naturalFestiveFuseSurvivesAndRespectsCooldown(Fixture h) {
        var pet=create(h,"festive_creeper",true);var target=EntityType.HUSK.create(h.getLevel());
        target.setPos(pet.getX()+2,pet.getY(),pet.getZ());target.setNoAi(true);h.getLevel().addFreshEntity(target);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
        target.setHealth(1000);pet.setTarget(target);var blasts=new Blasts(pet);MinecraftForge.EVENT_BUS.register(blasts);
        try{
            for(int i=0;i<35;i++)pet.tick();
            h.assertTrue(blasts.count==1,"native FestiveCreeper.tick reaches a close-range blast without reflection or ignition");
            h.assertTrue(pet.isAlive()&&!pet.isRemoved()&&!pet.isIgnited(),"festive creeper survives and resets its fuse");
            for(int i=0;i<50;i++)pet.tick();
            h.assertTrue(blasts.count==1,"three-second cooldown prevents another blast");
            for(int i=0;i<45;i++)pet.tick();
            h.assertTrue(blasts.count>=2,"combat fuse can trigger again after cooldown");
        }finally{MinecraftForge.EVENT_BUS.unregister(blasts);pet.discard();target.discard();}h.succeed();
    }
    public static void wildAndSittingFestiveDoNotGetCombatFuse(Fixture h) {
        for(boolean tame:List.of(false,true)){
            var pet=create(h,"festive_creeper",tame);var target=EntityType.HUSK.create(h.getLevel());
            target.setPos(pet.getX()+2,pet.getY(),pet.getZ());target.setNoAi(true);h.getLevel().addFreshEntity(target);pet.setTarget(target);
            if(tame)PrimitiveMobsCompat.setCommand(pet,1);
            var blasts=new Blasts(pet);MinecraftForge.EVENT_BUS.register(blasts);
            try{for(int i=0;i<40;i++)pet.tick();h.assertTrue(blasts.count==0,"wild/sitting creepers keep their existing behavior");}
            finally{MinecraftForge.EVENT_BUS.unregister(blasts);pet.discard();target.discard();}
        }h.succeed();
    }
    public static void actualPrimitivePetsAcceptOwnerCollars(Fixture h) {
        for(String name:List.of("festive_creeper","support_creeper","rocket_creeper","baby_spider","chameleon")){
            var pet=(LivingEntity)ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("primitive_mobs",name)).create(h.getLevel());
            var owner=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"CollarOwner"));
            pet.setPos(Vec3.atBottomCenterOf(h.absolutePos(new net.minecraft.core.BlockPos(2,2,2))));
            h.getLevel().addFreshEntity(pet);PrimitiveMobsCompat.tame((Mob)pet,owner);
            var data=new TameData(pet,owner.getUUID(),false);TameRegistry.register(data);
            try{owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(DIItemRegistry.COLLAR_TAG.get()));
                MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteractSpecific(owner,InteractionHand.MAIN_HAND,pet,Vec3.ZERO));
                h.assertTrue(TameableUtils.hasCollar(pet)&&data.hasCollarTag,"actual Primitive Mobs collar applies: "+name);
            }finally{TameRegistry.remove(pet.getUUID());pet.discard();}
        }h.succeed();
    }
}
