package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.entity.ModifedToBeTameable;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class ImprintedOwnerRegression {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    @GameTest(template="empty")
    public static void firstOwnerSurvivesTransferAndSave(GameTestHelper h){
        UUID first=UUID.randomUUID(),second=UUID.randomUUID();
        var wolf=EntityType.WOLF.create(h.getLevel());TameEntityAdapter.setOwner(wolf,first);
        wolf.moveTo(h.absolutePos(new BlockPos(1,1,1)),0,0);h.getLevel().addFreshEntity(wolf);
        try{
            TameData data=TameRegistry.get(wolf.getUUID());
            check(first.equals(data.imprintedOwnerUUID()),"initial owner captured");
            TameEntityAdapter.setOwner(wolf,second);TameRegistry.transferOwnership(data.uuid,second);TameRegistry.bindEntityToData(wolf,data);
            check(second.equals(data.ownerUUID)&&first.equals(data.imprintedOwnerUUID()),"transfer changes current owner only");
            check(first.equals(TameData.fromTag(data.toTag()).imprintedOwnerUUID()),"registry save preserves imprint");
            CompoundTag saved=new CompoundTag();wolf.save(saved);
            var restored=(LivingEntity)EntityType.loadEntityRecursive(saved,h.getLevel(),entity->entity);
            try{check(first.equals(new TameData(restored,second,false).imprintedOwnerUUID()),"entity snapshot preserves original owner even when registry is rebuilt");}
            finally{restored.discard();}
        }finally{TameRegistry.remove(wolf.getUUID());wolf.discard();}h.succeed();
    }
    @GameTest(template="empty")
    public static void interfaceTameRetainsItsOriginalOwner(GameTestHelper h){
        UUID first=UUID.randomUUID(),second=UUID.randomUUID();var frog=EntityType.FROG.create(h.getLevel());
        var modified=(ModifedToBeTameable)frog;modified.setTame(true);modified.setTameOwnerUUID(first);
        var data=new TameData(frog,first,true);TameRegistry.register(data);
        try{
            TameEntityAdapter.setOwner(frog,second);TameRegistry.transferOwnership(data.uuid,second);TameRegistry.bindEntityToData(frog,data);
            check(first.equals(data.imprintedOwnerUUID()),"interface imprint remains first owner");
            check(first.equals(frog.getPersistentData().getUUID(TameData.IMPRINTED_OWNER_TAG)),"interface entity stores imprint");
            check(second.equals(modified.getTameOwnerUUID()),"current ownership still transfers normally");
        }finally{TameRegistry.remove(data.uuid);frog.discard();}h.succeed();
    }
    @GameTest(template="empty")
    public static void becomingWildDoesNotForgetTheImprint(GameTestHelper h){
        UUID first=UUID.randomUUID();var wolf=EntityType.WOLF.create(h.getLevel());TameEntityAdapter.setOwner(wolf,first);
        wolf.moveTo(h.absolutePos(new BlockPos(1,1,1)),0,0);h.getLevel().addFreshEntity(wolf);
        try{
            TameData data=TameRegistry.get(wolf.getUUID());TameLimitService.release(h.getLevel().getServer(),data);
            check(wolf.getOwnerUUID()==null,"wild entity loses current ownership");
            check(first.equals(TameLimitService.wildData(wolf).imprintedOwnerUUID()),"wild ability record retains imprint");
            check(first.equals(wolf.getPersistentData().getUUID(TameData.IMPRINTED_OWNER_TAG)),"wild entity retains imprint");
        }finally{TameRegistry.remove(wolf.getUUID());wolf.discard();}h.succeed();
    }
    @GameTest(template="empty")
    public static void legacyRecordsUseKnownOwnerWithoutOverwritingRememberedOwner(GameTestHelper h){
        UUID first=UUID.randomUUID(),current=UUID.randomUUID();CompoundTag tag=new CompoundTag();
        tag.putUUID("uuid",UUID.randomUUID());tag.putUUID("ownerUUID",current);
        TameData legacy=TameData.fromTag(tag);TameRegistry.register(legacy);
        try{check(current.equals(legacy.imprintedOwnerUUID()),"legacy record uses first owner known to this version");}
        finally{TameRegistry.remove(legacy.uuid);}
        CompoundTag snapshot=new CompoundTag(),forge=new CompoundTag();forge.putUUID(TameData.IMPRINTED_OWNER_TAG,first);
        snapshot.put("ForgeData",forge);tag.put("entitySnapshot",snapshot);
        TameData recovered=TameData.fromTag(tag);TameRegistry.register(recovered);
        try{check(first.equals(recovered.imprintedOwnerUUID()),"snapshot imprint takes precedence over current owner");}
        finally{TameRegistry.remove(recovered.uuid);}h.succeed();
    }
}
