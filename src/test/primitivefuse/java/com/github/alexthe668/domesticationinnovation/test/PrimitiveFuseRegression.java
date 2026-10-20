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
            logger.info("PRIMITIVE_FUSE_TESTS_PASS: native fuse, survival, cooldown, repeat blast, wild/sitting exclusion, five species collars, rocket launch/landing, support fuse, thrown TNT, block protection");
        }catch(Throwable error){logger.error("PRIMITIVE_FUSE_TESTS_FAIL",error);}
        finally{event.getServer().halt(false);}
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
