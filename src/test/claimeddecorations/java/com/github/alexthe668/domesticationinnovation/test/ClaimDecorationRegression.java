package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.entity.ModifedToBeTameable;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.ClaimDecorationProtection;
import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.decoration.*;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class ClaimDecorationRegression {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static BlockPos claim(GameTestHelper h)throws Exception{
        var level=h.getLevel();var source=level.getServer().createCommandSourceStack().withPermission(4);
        var team=FTBTeamsAPI.api().getManager().createServerTeam(source,"Decor"+UUID.randomUUID().toString().substring(0,8),"",Color4I.WHITE);
        BlockPos pos=h.absolutePos(new BlockPos(64,2,64));pos=new BlockPos((pos.getX()>>4)*16,pos.getY(),(pos.getZ()>>4)*16);
        check(FTBChunksAPI.api().getManager().getOrCreateData(team).claim(source,new ChunkDimPos(level,pos),false).isSuccess(),"claim fixture");return pos;
    }
    private static void unclaim(ServerLevel level,BlockPos pos){
        var source=level.getServer().createCommandSourceStack().withPermission(4);var chunk=FTBChunksAPI.api().getManager().getChunk(new ChunkDimPos(level,pos));
        if(chunk!=null)chunk.getTeamData().unclaim(source,new ChunkDimPos(level,pos),false);
    }
    private static Wolf wolf(ServerLevel level,BlockPos pos,boolean tamed){
        Wolf wolf=EntityType.WOLF.create(level);wolf.setTame(tamed);if(tamed)wolf.setOwnerUUID(UUID.randomUUID());wolf.setPos(pos.getX()-.5,pos.getY(),pos.getZ()+.5);return wolf;
    }
    private static Entity decoration(ServerLevel level,BlockPos pos,EntityType<?> type){
        Entity e;
        if(type==EntityType.ITEM_FRAME)e=new ItemFrame(level,pos,Direction.SOUTH);
        else if(type==EntityType.GLOW_ITEM_FRAME)e=new GlowItemFrame(level,pos,Direction.SOUTH);
        else if(type==EntityType.PAINTING)e=new Painting(level,pos,Direction.SOUTH,level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.PAINTING_VARIANT).getHolderOrThrow(PaintingVariants.KEBAB));
        else {e=type.create(level);e.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);}
        if(e instanceof ItemFrame frame)frame.setItem(new ItemStack(Items.DIAMOND));
        level.setBlockAndUpdate(pos.north(),Blocks.STONE.defaultBlockState());level.addFreshEntity(e);return e;
    }
    @GameTest(template="empty",batch="claim-decorations")
    public static void protectedTypesAndUnclaimedControls(GameTestHelper h)throws Exception{
        var level=h.getLevel();var pos=claim(h);var tame=wolf(level,pos,true);List<Entity> fixtures=new ArrayList<>();
        try{
            for(EntityType<?> type:List.of(EntityType.ARMOR_STAND,EntityType.ITEM_FRAME,EntityType.GLOW_ITEM_FRAME,EntityType.PAINTING,EntityType.BOAT,EntityType.CHEST_MINECART,EntityType.END_CRYSTAL)){
                Entity target=decoration(level,pos,type);fixtures.add(target);
                var source=level.damageSources().explosion(tame,tame);
                check(!target.hurt(source,100)&&!target.isRemoved(),"claimed "+type+" survives tame explosion damage");
                if(target instanceof ItemFrame frame)check(frame.getItem().is(Items.DIAMOND),"frame contents stay attached");
            }
            Entity outside=decoration(level,pos.west(),EntityType.ITEM_FRAME);fixtures.add(outside);
            outside.hurt(level.damageSources().mobAttack(tame),1);
            check(((ItemFrame)outside).getItem().isEmpty(),"unclaimed frame keeps normal damage behavior");
            var stand=(ArmorStand)fixtures.get(0);unclaim(level,pos);
            stand.hurt(level.damageSources().explosion(tame,tame),100);
            check(stand.isRemoved(),"unclaim takes effect immediately without stale claim cache");
        }finally{unclaim(level,pos);fixtures.forEach(Entity::discard);tame.discard();}h.succeed();
    }
    @GameTest(template="empty",batch="claim-decorations")
    public static void projectileInterfaceAndWildPlayerControls(GameTestHelper h)throws Exception{
        var level=h.getLevel();var pos=claim(h);var tame=wolf(level,pos,true);var wild=wolf(level,pos,false);
        var frame=(ItemFrame)decoration(level,pos,EntityType.ITEM_FRAME);var arrow=new Arrow(level,tame);
        var fox=EntityType.FOX.create(level);var pet=(ModifedToBeTameable)fox;pet.setTame(true);pet.setTameOwnerUUID(tame.getOwnerUUID());
        try{
            check(!frame.hurt(level.damageSources().arrow(arrow,null),10)&&frame.getItem().is(Items.DIAMOND),"direct-only projectile resolves tame owner");
            check(!frame.hurt(level.damageSources().mobAttack(fox),1)&&frame.getItem().is(Items.DIAMOND),"interface tame cannot pop claimed frame contents");
            check(!ClaimDecorationProtection.blockDamage(frame,level.damageSources().mobAttack(wild)),"wild mobs retain existing FTB behavior");
            var player=FakePlayerFactory.get(level,new GameProfile(tame.getOwnerUUID(),"DecorationOwner"));
            check(!ClaimDecorationProtection.blockDamage(frame,level.damageSources().playerAttack(player)),"player removal is not blocked by the tame rule");
            frame.hurt(level.damageSources().mobAttack(wild),1);check(frame.getItem().isEmpty(),"ordinary wild damage remains unchanged");
        }finally{unclaim(level,pos);frame.discard();tame.discard();wild.discard();arrow.discard();fox.discard();}h.succeed();
    }
    @GameTest(template="empty",batch="claim-decorations")
    public static void explosionFilteringKeepsAnimalsAndUnclaimedEntities(GameTestHelper h)throws Exception{
        var level=h.getLevel();var pos=claim(h);var tame=wolf(level,pos,true);
        Entity protectedFrame=decoration(level,pos,EntityType.GLOW_ITEM_FRAME),outside=decoration(level,pos.west(),EntityType.ITEM_FRAME);
        var cow=EntityType.COW.create(level);cow.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        try{
            List<Entity> affected=new ArrayList<>(List.of(protectedFrame,outside,cow));
            var explosion=new Explosion(level,tame,pos.getX(),pos.getY(),pos.getZ(),4F,false,Explosion.BlockInteraction.KEEP);
            ClaimDecorationProtection.onExplosion(new ExplosionEvent.Detonate(level,explosion,affected));
            check(!affected.contains(protectedFrame)&&affected.contains(outside)&&affected.contains(cow),"only claimed props removed from tame explosion damage list");
            List<Entity> normal=new ArrayList<>(List.of(protectedFrame));
            ClaimDecorationProtection.onExplosion(new ExplosionEvent.Detonate(level,new Explosion(level,null,pos.getX(),pos.getY(),pos.getZ(),4F,false,Explosion.BlockInteraction.KEEP),normal));
            check(normal.contains(protectedFrame),"ownerless explosions keep existing protection rules");
        }finally{unclaim(level,pos);protectedFrame.discard();outside.discard();cow.discard();tame.discard();}h.succeed();
    }
}
