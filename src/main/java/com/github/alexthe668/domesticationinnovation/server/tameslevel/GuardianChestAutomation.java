package com.github.alexthe668.domesticationinnovation.server.tameslevel;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.block.DIBlockRegistry;
import com.github.alexthe668.domesticationinnovation.server.block.DrumBlockEntity;
import com.github.alexthe668.domesticationinnovation.server.item.DIItemRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Only indexed, loaded drums are inspected. One setup scan and one tame operation per tick. */
@Mod.EventBusSubscriber(modid=DomesticationMod.MODID)
public final class GuardianChestAutomation {
    public static final String PAPER="TLGuardianPaper";
    private record Key(ServerLevel level, BlockPos pos) {}
    private record Config(UUID owner, String group) {}
    private static final class Setup {
        final Key key; LoadedChestDrums.Chest chest; Config config; boolean powered;
        final Map<UUID,UUID> members=new LinkedHashMap<>();
        Setup(Key key){this.key=key;}
    }
    private record Action(Setup setup, UUID identity, UUID token, boolean deploy, int expires) {}
    private static final Map<Key,Setup> SETUPS=new LinkedHashMap<>();
    private static final Map<UUID,Action> AUTHORIZED=new HashMap<>();
    private static final Map<Key,LoadedChestDrums.Chest> SCANS=new LinkedHashMap<>();
    private static final Map<Key,LoadedChestDrums.Chest> DIRTY=new LinkedHashMap<>();
    private static final Map<UUID,Action> ACTIONS=new LinkedHashMap<>();
    private static final Map<UUID,UUID> LATEST=new HashMap<>();
    private record Manual(UUID owner,UUID identity,String group,int expires) {}
    private static final Map<UUID,Manual> MANUAL=new LinkedHashMap<>();

    private GuardianChestAutomation() {}
    /** Suppress the ordinary drum's broad movement command for an automation controller. */
    public static boolean handlesRedstone(net.minecraft.world.level.Level world,BlockPos drumPos){
        if(!(world instanceof ServerLevel level))return false;
        Key key=new Key(level,drumPos.above());Setup setup=SETUPS.get(key);
        if(setup!=null){DIRTY.put(key,setup.chest);return true;}
        var chunk=level.getChunkSource().getChunkNow(drumPos.getX()>>4,drumPos.getZ()>>4);
        if(chunk==null||!(chunk.getBlockEntity(key.pos) instanceof ChestBlockEntity chest))return false;
        var inventory=chest.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).orElse(null);
        return inventory!=null&&configuration(level,new LoadedChestDrums.Chest(chest,inventory,null,0))!=null;
    }
    @SubscribeEvent public static void neighbors(net.minecraftforge.event.level.BlockEvent.NeighborNotifyEvent event){
        if(SETUPS.isEmpty()||!(event.getLevel() instanceof ServerLevel level))return;
        for(var direction:event.getNotifiedSides()){
            BlockPos pos=event.getPos().relative(direction);
            Setup setup=SETUPS.get(new Key(level,pos));if(setup==null)setup=SETUPS.get(new Key(level,pos.above()));
            if(setup!=null)DIRTY.put(setup.key,setup.chest);
        }
    }
    public static boolean isPaper(ItemStack stack){return stack.is(DIItemRegistry.DEED_OF_OWNERSHIP.get())&&stack.hasTag()&&stack.getTag().contains(PAPER,10);}
    public static TameData paperTame(ItemStack stack){
        if(!stack.hasTag())return null;
        CompoundTag tag=isPaper(stack)?stack.getTag().getCompound(PAPER):stack.getTag();
        UUID id=tag.hasUUID("Tame")?tag.getUUID("Tame"):tag.hasUUID("BoundEntity")?tag.getUUID("BoundEntity"):null;
        if(id==null)return null;
        TameData data=TameRegistry.getByTlId(id);return data==null?TameRegistry.get(id):data;
    }
    /** Conversion is permanent; the original deed binding cannot transfer ownership afterwards. */
    public static boolean bindPaper(ItemStack stack,UUID owner,String group){
        if(!stack.is(DIItemRegistry.DEED_OF_OWNERSHIP.get())||!stack.hasTag()||group==null||group.isBlank())return false;
        if(!isPaper(stack)&&!stack.getTag().getBoolean("HasBoundEntity"))return false;
        TameData data=paperTame(stack);
        if(data==null||!owner.equals(data.ownerUUID))return false;
        if(isPaper(stack)){
            CompoundTag binding=stack.getTag().getCompound(PAPER);
            if(!binding.hasUUID("Owner")||!owner.equals(binding.getUUID("Owner"))||!group.equals(binding.getString("Group")))return false;
        }
        CompoundTag binding=new CompoundTag();binding.putUUID("Owner",owner);binding.putUUID("Tame",data.ensureTlId());binding.putString("Group",group);
        stack.getOrCreateTag().put(PAPER,binding);
        stack.getTag().remove("HasBoundEntity");stack.getTag().remove("BoundEntity");stack.getTag().remove("BoundEntityName");
        stack.setHoverName(Component.literal(group+": "+data.name));
        return true;
    }
    private static boolean matches(ItemStack stack,Config config,UUID identity){
        if(!isPaper(stack))return false;
        CompoundTag tag=stack.getTag().getCompound(PAPER);
        TameData data=paperTame(stack);
        return tag.hasUUID("Owner")&&tag.hasUUID("Tame")&&config.owner.equals(tag.getUUID("Owner"))
                &&config.group.equals(tag.getString("Group"))&&identity.equals(tag.getUUID("Tame"))
                &&data!=null&&config.owner.equals(data.ownerUUID);
    }
    private static boolean foundation(ServerLevel level,BlockPos chest){
        BlockPos base=chest.below(2);
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            BlockPos pos=base.offset(x,0,z);
            var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
            if(chunk==null||!(chunk.getBlockEntity(pos) instanceof ChestBlockEntity))return false;
        }
        return true;
    }
    private static Config configuration(ServerLevel level,LoadedChestDrums.Chest chest){
        if(!(chest.blockEntity() instanceof ChestBlockEntity)||!foundation(level,chest.pos())||chest.blockEntity().isRemoved())return null;
        var drumChunk=level.getChunkSource().getChunkNow(chest.pos().getX()>>4,chest.pos().getZ()>>4);
        if(drumChunk==null||!(drumChunk.getBlockEntity(chest.pos().below()) instanceof DrumBlockEntity drum)||drum.getPlacerUUID()==null)return null;
        // Resolve only the placer, never a network/name lookup on the server tick.
        var server=level.getServer();var player=server.getPlayerList().getPlayer(drum.getPlacerUUID());
        var profile=player==null?(server.getProfileCache()==null?null:server.getProfileCache().get(drum.getPlacerUUID()).orElse(null)):player.getGameProfile();
        if(profile==null)return null;
        Config found=null;
        for(int slot=0;slot<chest.inventory().getSlots();slot++){
            ItemStack stack=chest.inventory().getStackInSlot(slot);
            if(!stack.is(DIBlockRegistry.DRUM.get().asItem())||!stack.hasCustomHoverName())continue;
            String name=stack.getHoverName().getString();int split=name.indexOf(':');
            if(split<1||!profile.getName().equalsIgnoreCase(name.substring(0,split).trim()))continue;
            String group=name.substring(split+1).trim().toLowerCase(Locale.ROOT);if(group.isBlank()||group.length()>128)continue;
            Config next=new Config(profile.getId(),group);
            if(found!=null&&!found.equals(next))return null; // Ambiguous labels never issue commands.
            found=next;
        }
        return found;
    }
    private static boolean powered(Setup setup){return setup.key.level.hasNeighborSignal(setup.key.pos)||setup.key.level.hasNeighborSignal(setup.key.pos.below());}
    private static boolean hasPaper(Setup setup,UUID identity){
        for(int slot=0;slot<setup.chest.inventory().getSlots();slot++)if(matches(setup.chest.inventory().getStackInSlot(slot),setup.config,identity))return true;
        return false;
    }
    public static boolean authorized(UUID token){
        Action action=AUTHORIZED.get(token);if(action==null||!token.equals(LATEST.get(action.identity)))return false;
        Setup setup=action.setup;
        if(SETUPS.get(setup.key)!=setup||setup.chest.blockEntity().isRemoved()||setup.config==null)return false;
        var chunk=setup.key.level.getChunkSource().getChunkNow(setup.key.pos.getX()>>4,setup.key.pos.getZ()>>4);
        return chunk!=null&&chunk.getBlockEntity(setup.key.pos)==setup.chest.blockEntity()
                &&setup.config.equals(configuration(setup.key.level,setup.chest))&&powered(setup)==action.deploy&&hasPaper(setup,action.identity);
    }
    private static void retire(UUID token){
        Action action=AUTHORIZED.remove(token);if(action!=null){LATEST.remove(action.identity,token);ACTIONS.remove(action.identity,action);}
    }
    private static void invalidate(Setup setup){for(UUID token:setup.members.values())retire(token);setup.members.clear();}
    public static void removeDrum(ServerLevel level,BlockPos drum){
        Key key=new Key(level,drum.above());Setup setup=SETUPS.remove(key);if(setup!=null)invalidate(setup);
        SCANS.remove(key);DIRTY.remove(key);
    }
    public static void scan(LoadedChestDrums.Chest chest){
        if(!(chest.blockEntity().getLevel() instanceof ServerLevel level))return;
        Key key=new Key(level,chest.pos());Setup setup=SETUPS.get(key);Config config=configuration(level,chest);
        if(config==null){if(setup!=null){invalidate(setup);SETUPS.remove(key);}return;}
        boolean fresh=setup==null;
        if(fresh){setup=new Setup(key);SETUPS.put(key,setup);}
        if(!config.equals(setup.config)||setup.chest!=null&&setup.chest.blockEntity()!=chest.blockEntity()){invalidate(setup);fresh=true;}
        setup.chest=chest;setup.config=config;boolean on=powered(setup);boolean edge=!fresh&&on!=setup.powered;setup.powered=on;
        Set<UUID> present=new LinkedHashSet<>();boolean changed=false;
        for(int slot=0;slot<chest.inventory().getSlots();slot++){
            ItemStack stack=chest.inventory().getStackInSlot(slot);
            if(!stack.is(DIItemRegistry.DEED_OF_OWNERSHIP.get()))continue;
            boolean wasPaper=isPaper(stack);
            if(!bindPaper(stack,config.owner,config.group))continue;
            changed|=!wasPaper;
            UUID id=stack.getTag().getCompound(PAPER).getUUID("Tame");present.add(id);
        }
        for(UUID old:new ArrayList<>(setup.members.keySet()))if(!present.contains(old))retire(setup.members.remove(old));
        for(UUID id:present){
            boolean added=!setup.members.containsKey(id);
            if(edge||added){
                UUID old=setup.members.remove(id);if(old!=null)retire(old);
                UUID token=UUID.randomUUID();setup.members.put(id,token);
                if(edge||on){Action action=new Action(setup,id,token,on,level.getServer().getTickCount()+1200);AUTHORIZED.put(token,action);LATEST.put(id,token);ACTIONS.put(id,action);}
            }
        }
        if(changed)chest.blockEntity().setChanged();
    }
    /** Refresh from the existing loaded-drum index, without discovering blocks or entities. */
    public static void refresh(ServerLevel level,List<LoadedChestDrums.Status> statuses){
        Set<BlockPos> current=new HashSet<>();
        for(var status:statuses)if(status.active()){current.add(status.chest().pos());SCANS.put(new Key(level,status.chest().pos()),status.chest());}
        for(var iterator=SETUPS.entrySet().iterator();iterator.hasNext();){var entry=iterator.next();
            if(entry.getKey().level==level&&!current.contains(entry.getKey().pos)){invalidate(entry.getValue());iterator.remove();}}
    }
    /** Includes unloaded guardians: food is stored in their registry inventory without loading chunks. */
    public static List<TameData> feedingMembers(LoadedChestDrums.Chest chest){
        if(!(chest.blockEntity().getLevel() instanceof ServerLevel level))return List.of();
        Setup setup=SETUPS.get(new Key(level,chest.pos()));
        if(setup==null||setup.chest.blockEntity()!=chest.blockEntity()||!setup.config.equals(configuration(level,chest)))return List.of();
        List<TameData> result=new ArrayList<>();
        for(UUID id:setup.members.keySet()){
            TameData data=TameRegistry.getByTlId(id);
            if(data!=null&&setup.config.owner.equals(data.ownerUUID)&&data.hasHome&&!data.rosterBodyguard&&!data.dead&&!data.stored&&hasPaper(setup,id))result.add(data);
        }
        return result;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        Map<Key,LoadedChestDrums.Chest> scans=DIRTY.isEmpty()?SCANS:DIRTY;
        if(!scans.isEmpty()){var iterator=scans.entrySet().iterator();var entry=iterator.next();var chest=entry.getValue();Key key=entry.getKey();iterator.remove();SCANS.remove(key);scan(chest);}
        if(ACTIONS.isEmpty()){
            if(!MANUAL.isEmpty()){var iterator=MANUAL.values().iterator();Manual manual=iterator.next();iterator.remove();
                TameData data=TameRegistry.getByTlId(manual.identity);
                if(data!=null&&manual.owner.equals(data.ownerUUID)&&!data.rosterBodyguard&&!data.dead
                        &&!TameCommands.guardianAutomationMove(event.getServer(),data,manual.group,true,null)
                        &&event.getServer().getTickCount()<manual.expires)MANUAL.put(manual.identity,manual);
            }
            return;
        }
        var iterator=ACTIONS.values().iterator();Action action=iterator.next();iterator.remove();
        if(!authorized(action.token))return;
        TameData data=TameRegistry.getByTlId(action.identity);
        if(data==null||data.dead||data.rosterBodyguard||!action.setup.config.owner.equals(data.ownerUUID))return;
        if(!TameCommands.guardianAutomationMove(event.getServer(),data,action.setup.config.group,action.deploy,action.token)
                &&event.getServer().getTickCount()<action.expires)ACTIONS.put(action.identity,action);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void usePaper(PlayerInteractEvent.RightClickBlock event){
        if(!isPaper(event.getItemStack()))return;
        event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        var binding=event.getItemStack().getTag().getCompound(PAPER);TameData data=paperTame(event.getItemStack());
        if(data==null||!binding.hasUUID("Owner")||!player.getUUID().equals(binding.getUUID("Owner"))||!player.getUUID().equals(data.ownerUUID)){
            player.sendSystemMessage(Component.literal("Only this tame's owner can use its guardian paper."));return;
        }
        String group=binding.getString("Group");
        if(player.isShiftKeyDown()){
            requestManual(player,data,group);
        }else{
            TameCommands.setGuardianPaperPost(data,group,player.serverLevel(),event.getPos().relative(event.getFace()));
            player.sendSystemMessage(Component.literal("Guardian post for "+data.name+" in '"+group+"' set."));
        }
    }
    private static void requestManual(ServerPlayer player,TameData data,String group){
        if(data.rosterBodyguard){player.sendSystemMessage(Component.literal("Release this bodyguard before deploying it as a guardian."));return;}
        if(!data.guardianSetAnchors.containsKey(group)){player.sendSystemMessage(Component.literal("Right click a block with this paper to set its guardian post first."));return;}
        if(player.getCooldowns().isOnCooldown(DIItemRegistry.DEED_OF_OWNERSHIP.get()))return;
        player.getCooldowns().addCooldown(DIItemRegistry.DEED_OF_OWNERSHIP.get(),10);
        MANUAL.put(data.ensureTlId(),new Manual(player.getUUID(),data.tlId,group,player.server.getTickCount()+1200));
        player.sendSystemMessage(Component.literal("Guardian deployment queued for "+data.name+"."));
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void useInAir(PlayerInteractEvent.RightClickItem event){
        if(!isPaper(event.getItemStack())||!event.getEntity().isShiftKeyDown())return;
        event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        var binding=event.getItemStack().getTag().getCompound(PAPER);TameData data=paperTame(event.getItemStack());
        if(data!=null&&binding.hasUUID("Owner")&&player.getUUID().equals(binding.getUUID("Owner"))&&player.getUUID().equals(data.ownerUUID))requestManual(player,data,binding.getString("Group"));
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void blockAdoption(PlayerInteractEvent.EntityInteract event){if(isPaper(event.getItemStack())){event.setCanceled(true);event.setCancellationResult(InteractionResult.FAIL);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void blockAdoptionSpecific(PlayerInteractEvent.EntityInteractSpecific event){if(isPaper(event.getItemStack())){event.setCanceled(true);event.setCancellationResult(InteractionResult.FAIL);}}
    @SubscribeEvent public static void unload(net.minecraftforge.event.level.LevelEvent.Unload event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        for(var iterator=SETUPS.entrySet().iterator();iterator.hasNext();){var entry=iterator.next();if(entry.getKey().level==level){invalidate(entry.getValue());iterator.remove();}}
        SCANS.keySet().removeIf(key->key.level==level);DIRTY.keySet().removeIf(key->key.level==level);
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event){SETUPS.clear();AUTHORIZED.clear();SCANS.clear();DIRTY.clear();ACTIONS.clear();LATEST.clear();MANUAL.clear();}
}
