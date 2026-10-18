package com.github.alexthe668.domesticationinnovation.test;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.claimfire.ClaimDisplayProtection;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.*;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class ClaimDisplayRegression {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static BlockPos claim(GameTestHelper h)throws Exception{
        var level=h.getLevel();var source=level.getServer().createCommandSourceStack().withPermission(4);
        var team=FTBTeamsAPI.api().getManager().createServerTeam(source,"Displays"+UUID.randomUUID().toString().substring(0,8),"",Color4I.WHITE);
        var p=h.absolutePos(new BlockPos(64,2,64));var pos=new BlockPos((p.getX()>>4)*16,p.getY(),(p.getZ()>>4)*16);
        check(FTBChunksAPI.api().getManager().getOrCreateData(team).claim(source,new ChunkDimPos(level,pos),false).isSuccess(),"claim fixture");return pos;
    }
    private static void unclaim(ServerLevel level,BlockPos pos){
        var source=level.getServer().createCommandSourceStack().withPermission(4);var chunk=FTBChunksAPI.api().getManager().getChunk(new ChunkDimPos(level,pos));
        if(chunk!=null)chunk.getTeamData().unclaim(source,new ChunkDimPos(level,pos),false);
    }
    private static ItemFrame frame(ServerLevel level,BlockPos pos,boolean glow){
        level.setBlockAndUpdate(pos.north(),Blocks.STONE.defaultBlockState());
        ItemFrame frame=glow?new GlowItemFrame(level,pos,Direction.SOUTH):new ItemFrame(level,pos,Direction.SOUTH);
        frame.setItem(new ItemStack(Items.DIAMOND));level.addFreshEntity(frame);return frame;
    }
    private static ArmorStand stand(ServerLevel level,BlockPos pos){
        var stand=EntityType.ARMOR_STAND.create(level);stand.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);level.addFreshEntity(stand);return stand;
    }
    @GameTest(template="empty",batch="claim-displays")
    public static void allNonMeleeDamageAndWrongToolsAreBlocked(GameTestHelper h)throws Exception{
        var level=h.getLevel();var pos=claim(h);var frame=frame(level,pos,true);var stand=stand(level,pos);
        var player=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"DisplayDamage"));
        var arrow=new Arrow(level,player);var zombie=EntityType.ZOMBIE.create(level);
        try{
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SWORD));
            for(DamageSource damage:List.of(level.damageSources().generic(),level.damageSources().onFire(),level.damageSources().inFire(),
                    level.damageSources().lava(),level.damageSources().fall(),level.damageSources().lightningBolt(),
                    level.damageSources().explosion(player,player),level.damageSources().arrow(arrow,player),level.damageSources().mobAttack(zombie))){
                check(!frame.hurt(damage,1000)&&!frame.isRemoved()&&frame.getItem().is(Items.DIAMOND),"frame and contents immune to "+damage.getMsgId());
                check(!stand.hurt(damage,1000)&&!stand.isRemoved(),"armor stand immune to "+damage.getMsgId());
            }
            player.getAbilities().instabuild=true;
            for(Item tool:List.of(Items.IRON_AXE,Items.IRON_PICKAXE,Items.STICK,Items.BOW)){
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(tool));
                check(!frame.hurt(level.damageSources().playerAttack(player),100)&&frame.getItem().is(Items.DIAMOND),"even creative wrong-tool hit cannot pop frame contents");
                check(!stand.hurt(level.damageSources().playerAttack(player),100)&&!stand.isRemoved(),"creative wrong-tool hit cannot break stand");
            }
            player.getAbilities().instabuild=false;
        }finally{player.getAbilities().instabuild=false;unclaim(level,pos);frame.discard();stand.discard();arrow.discard();zombie.discard();}h.succeed();
    }
    @GameTest(template="empty",batch="claim-displays")
    public static void handAndSwordKeepVanillaRemoval(GameTestHelper h)throws Exception{
        var level=h.getLevel();var pos=claim(h);var player=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"DisplayRemove"));List<Entity> fixtures=new ArrayList<>();
        try{
            for(ItemStack hand:List.of(ItemStack.EMPTY,new ItemStack(Items.IRON_SWORD))){
                player.setItemInHand(InteractionHand.MAIN_HAND,hand);
                var frame=frame(level,pos,false);fixtures.add(frame);var stand=stand(level,pos);fixtures.add(stand);
                DamageSource damage=level.damageSources().playerAttack(player);
                check(!ClaimDisplayProtection.blockDamage(frame,damage),"direct hand/sword hit allowed");
                frame.hurt(damage,1);check(frame.getItem().isEmpty()&&!frame.isRemoved(),"first allowed hit pops frame item normally");
                frame.hurt(damage,1);check(frame.isRemoved(),"second allowed hit removes empty frame");
                stand.hurt(damage,100);stand.hurt(damage,100);check(stand.isRemoved(),"allowed melee removes armor stand using vanilla rules");
            }
            var outside=frame(level,pos.west(),false);fixtures.add(outside);
            outside.hurt(level.damageSources().explosion(player,player),100);
            check(outside.isRemoved(),"unclaimed frame retains explosion damage");
        }finally{unclaim(level,pos);fixtures.forEach(Entity::discard);}h.succeed();
    }
    @GameTest(template="empty",batch="claim-displays")
    public static void supportMovementAndExplosionListsCannotBreakDisplays(GameTestHelper h)throws Exception{
        var level=h.getLevel();var pos=claim(h);var frame=frame(level,pos,true);var stand=stand(level,pos);var cow=EntityType.COW.create(level);
        try{
            level.setBlockAndUpdate(pos.north(),Blocks.AIR.defaultBlockState());
            check(frame.survives(),"claimed frame survives backing-block removal");
            for(int i=0;i<120;i++)frame.tick();
            frame.move(MoverType.PISTON,new Vec3(1,0,0));frame.push(1,0,0);
            check(!frame.isRemoved()&&frame.getItem().is(Items.DIAMOND),"support checks, piston movement and pushing preserve frame and contents");
            List<Entity> affected=new ArrayList<>(List.of(frame,stand,cow));
            ClaimDisplayProtection.onExplosion(new ExplosionEvent.Detonate(level,new Explosion(level,null,pos.getX(),pos.getY(),pos.getZ(),4F,false,Explosion.BlockInteraction.KEEP),affected));
            check(!affected.contains(frame)&&!affected.contains(stand)&&affected.contains(cow),"all explosions skip claimed displays without making animals immortal");
            unclaim(level,pos);check(!frame.survives(),"unclaim immediately restores support requirements");
            for(int i=0;i<120&&!frame.isRemoved();i++)frame.tick();
            check(frame.isRemoved(),"unclaimed unsupported frame breaks normally");
        }finally{unclaim(level,pos);frame.discard();stand.discard();cow.discard();}h.succeed();
    }
}
