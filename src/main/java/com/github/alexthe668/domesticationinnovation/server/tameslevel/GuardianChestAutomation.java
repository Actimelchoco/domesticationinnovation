package com.github.alexthe668.domesticationinnovation.server.tameslevel;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.block.DIBlockRegistry;
import com.github.alexthe668.domesticationinnovation.server.block.DrumBlockEntity;
import com.github.alexthe668.domesticationinnovation.server.item.DIItemRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandlerModifiable;
import java.util.*;

/** Shared controllers use the loaded drum index. One setup and one tame operation per tick. */
@Mod.EventBusSubscriber(modid=DomesticationMod.MODID)
public final class GuardianChestAutomation {
    public static final String PAPER="TLGuardianPaper";
    private record Key(ServerLevel level, BlockPos pos) {}
    private record Config(UUID owner, String group) {}
    private record Inspection(Set<Config> configs, String problem, int labels, int unresolved) {}
    private record Member(Config config, UUID token) {}
    private static final class Setup {
        final Key key;
        LoadedChestDrums.Chest chest;
        Set<Config> configs=Set.of();
        boolean powered;
        Set<String> protectPlayers=Set.of();
        int nextProtectCheck=Integer.MIN_VALUE;
        boolean protectOnline;
        final Map<UUID,Member> members=new LinkedHashMap<>();
        Setup(Key key){this.key=key;}
    }
    private record Action(Setup setup, Config config, UUID identity, UUID token, boolean deploy, int expires) {}
    private record Manual(UUID caller,UUID owner,UUID identity,String group,boolean deploy,int expires) {}
    private static final Map<Key,Setup> SETUPS=new LinkedHashMap<>();
    private static final Map<UUID,Set<Setup>> ACTIVITY_LINKS=new HashMap<>();

    /** Indexed by tame; inventory contents are only read by the throttled controller scan. */
    public static boolean keepsActive(TameData data){
        if(data==null||!data.hasHome||data.rosterBodyguard)return false;
        var setups=ACTIVITY_LINKS.get(data.tlId!=null?data.tlId:data.uuid);
        if(setups==null)return false;
        for(Setup setup:setups){
            if(!setup.powered||setup.chest.blockEntity().isRemoved()||setup.protectPlayers.isEmpty())continue;
            int now=setup.key.level.getServer().getTickCount();
            if(now>=setup.nextProtectCheck){
                setup.nextProtectCheck=now+20;
                setup.protectOnline=setup.protectPlayers.stream().anyMatch(name->setup.key.level.getServer().getPlayerList().getPlayerByName(name)!=null);
            }
            if(setup.protectOnline)return true;
        }
        return false;
    }

    private static void unlinkActivity(Setup setup,UUID identity){
        var links=ACTIVITY_LINKS.get(identity);if(links==null)return;
        links.remove(setup);if(links.isEmpty())ACTIVITY_LINKS.remove(identity);
    }
    private static final Map<UUID,Action> AUTHORIZED=new HashMap<>();
    private static final Map<Key,LoadedChestDrums.Chest> SCANS=new LinkedHashMap<>(), DIRTY=new LinkedHashMap<>();
    private static final Map<Key,BlockPos> BLOCKERS=new HashMap<>();
    private static final Map<UUID,Action> ACTIONS=new LinkedHashMap<>();
    private static final Map<UUID,UUID> LATEST=new HashMap<>();
    private static final Map<UUID,Manual> MANUAL=new LinkedHashMap<>();
    private GuardianChestAutomation() {}

    private static Key key(LoadedChestDrums.Chest chest){return new Key((ServerLevel)chest.blockEntity().getLevel(),chest.pos());}
    public static boolean isPaper(ItemStack stack){return stack.is(DIItemRegistry.DEED_OF_OWNERSHIP.get())&&stack.hasTag()&&stack.getTag().contains(PAPER,10);}
    public static TameData paperTame(ItemStack stack){
        if(!stack.is(DIItemRegistry.DEED_OF_OWNERSHIP.get())||!stack.hasTag())return null;
        CompoundTag tag=isPaper(stack)?stack.getTag().getCompound(PAPER):stack.getTag();
        UUID id=tag.hasUUID("Tame")?tag.getUUID("Tame"):tag.hasUUID("BoundEntity")?tag.getUUID("BoundEntity"):null;
        if(id==null)return null;
        TameData data=TameRegistry.getByTlId(id);return data==null?TameRegistry.get(id):data;
    }
    public static boolean bindPaper(ItemStack stack,UUID owner,String group){
        if(owner==null||group==null||group.isBlank()||!stack.is(DIItemRegistry.DEED_OF_OWNERSHIP.get())||!stack.hasTag())return false;
        if(!isPaper(stack)&&!stack.getTag().getBoolean("HasBoundEntity"))return false;
        TameData data=paperTame(stack);
        if(data==null||!owner.equals(data.ownerUUID))return false;
        if(isPaper(stack)){
            CompoundTag old=stack.getTag().getCompound(PAPER);
            if(!old.hasUUID("Owner")||!owner.equals(old.getUUID("Owner"))||!group.equals(old.getString("Group")))return false;
        }
        CompoundTag binding=new CompoundTag();binding.putUUID("Owner",owner);binding.putUUID("Tame",data.ensureTlId());binding.putString("Group",group);
        stack.getOrCreateTag().put(PAPER,binding);
        stack.getTag().remove("HasBoundEntity");stack.getTag().remove("BoundEntity");stack.getTag().remove("BoundEntityName");
        stack.setHoverName(Component.literal(group+": "+data.name));return true;
    }
    private static boolean matches(ItemStack stack,Config config,UUID identity){
        if(!isPaper(stack))return false;
        CompoundTag tag=stack.getTag().getCompound(PAPER);TameData data=paperTame(stack);
        return tag.hasUUID("Owner")&&tag.hasUUID("Tame")&&config.owner.equals(tag.getUUID("Owner"))
                &&config.group.equals(tag.getString("Group"))&&identity.equals(tag.getUUID("Tame"))
                &&data!=null&&config.owner.equals(data.ownerUUID);
    }
    public static BlockPos foundationCenter(LoadedChestDrums.Chest chest){
        return chest.drumPos().getY()<chest.pos().getY()?chest.drumPos().below():chest.pos().below();
    }
    public static int foundationCount(LoadedChestDrums.Chest chest){
        ServerLevel level=(ServerLevel)chest.blockEntity().getLevel();int count=0;BlockPos base=foundationCenter(chest);
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            BlockPos pos=base.offset(x,0,z);var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
            if(chunk==null)continue;
            var entity=chunk.getBlockEntity(pos);
            if(entity!=null&&!entity.isRemoved()&&LoadedChestDrums.inventory(entity,net.minecraft.core.Direction.UP)!=null)count++;
        }
        return count;
    }
    private static boolean hasIntent(LoadedChestDrums.Chest chest){
        for(int slot=0;slot<chest.inventory().getSlots();slot++){
            ItemStack stack=chest.inventory().getStackInSlot(slot);
            if(isPaper(stack)||stack.is(DIBlockRegistry.DRUM.get().asItem())&&stack.hasCustomHoverName()&&stack.getHoverName().getString().contains(":"))return true;
        }return false;
    }
    private static Inspection inspect(LoadedChestDrums.Chest chest){
        if(!(chest.blockEntity().getLevel() instanceof ServerLevel level)||chest.blockEntity().isRemoved())return new Inspection(Set.of(),"Control inventory is unloaded or removed.",0,0);
        var chunk=level.getChunkSource().getChunkNow(chest.drumPos().getX()>>4,chest.drumPos().getZ()>>4);
        if(chunk==null||!(chunk.getBlockEntity(chest.drumPos()) instanceof DrumBlockEntity))return new Inspection(Set.of(),"Place a drum immediately above or below the control inventory.",0,0);
        int base=foundationCount(chest);
        if(base!=9)return new Inspection(Set.of(),"Base has "+base+"/9 inventories. Put a flat 3x3 inventory layer centered at "+foundationCenter(chest).toShortString()+".",0,0);
        var server=level.getServer();Map<String,UUID> owners=new HashMap<>();Set<UUID> checked=new HashSet<>();List<String[]> labels=new ArrayList<>();
        for(var player:server.getPlayerList().getPlayers())owners.put(player.getGameProfile().getName().toLowerCase(Locale.ROOT),player.getUUID());
        for(int slot=0;slot<chest.inventory().getSlots();slot++){
            ItemStack stack=chest.inventory().getStackInSlot(slot);TameData data=paperTame(stack);
            if(data!=null&&data.ownerUUID!=null&&checked.add(data.ownerUUID)){
                var online=server.getPlayerList().getPlayer(data.ownerUUID);
                var profile=online!=null?online.getGameProfile():server.getProfileCache()==null?null:server.getProfileCache().get(data.ownerUUID).orElse(null);
                if(profile!=null)owners.put(profile.getName().toLowerCase(Locale.ROOT),profile.getId());
            }
            if(!stack.is(DIBlockRegistry.DRUM.get().asItem())||!stack.hasCustomHoverName())continue;
            String name=stack.getHoverName().getString();int split=name.indexOf(':');if(split<1)continue;
            String owner=name.substring(0,split).trim().toLowerCase(Locale.ROOT),group=name.substring(split+1).trim().toLowerCase(Locale.ROOT);
            if(owner.equals("protect"))continue;
            if(!owner.isBlank()&&!group.isBlank()&&group.length()<=128)labels.add(new String[]{owner,group});
        }
        Set<Config> configs=new LinkedHashSet<>();int unresolved=0;
        for(String[] label:labels){UUID owner=owners.get(label[0]);if(owner==null)unresolved++;else configs.add(new Config(owner,label[1]));}
        if(labels.isEmpty())return new Inspection(Set.of(),"Add a drum named '<owner>:<deployment group>' and bound adoption papers to this inventory.",0,0);
        if(configs.isEmpty())return new Inspection(Set.of(),"Named drum owners are not resolved. Add their bound tame papers; the owners may need to join once to cache their profiles.",labels.size(),unresolved);
        return new Inspection(Set.copyOf(configs),"",labels.size(),unresolved);
    }
    private static Config configForPaper(ItemStack stack,Set<Config> configs){
        TameData data=paperTame(stack);if(data==null||data.ownerUUID==null)return null;
        if(isPaper(stack)){
            CompoundTag binding=stack.getTag().getCompound(PAPER);
            for(Config config:configs)if(config.owner.equals(data.ownerUUID)&&binding.hasUUID("Owner")&&config.owner.equals(binding.getUUID("Owner"))&&config.group.equals(binding.getString("Group")))return config;
            return null;
        }
        if(!stack.getTag().getBoolean("HasBoundEntity"))return null;
        List<Config> matching=configs.stream().filter(config->config.owner.equals(data.ownerUUID)).toList();
        if(matching.size()==1)return matching.get(0);
        List<Config> anchored=matching.stream().filter(config->data.guardianSetAnchors.containsKey(config.group)).toList();
        return anchored.size()==1?anchored.get(0):null;
    }
    private static boolean storePaper(LoadedChestDrums.Chest chest,int slot,Config config){
        ItemStack old=chest.inventory().getStackInSlot(slot).copy(),replacement=old.copy();
        if(!bindPaper(replacement,config.owner,config.group))return false;
        if(ItemStack.isSameItemSameTags(old,replacement))return true;
        if(chest.inventory() instanceof IItemHandlerModifiable inventory){
            try{inventory.setStackInSlot(slot,replacement);chest.blockEntity().setChanged();}
            catch(RuntimeException ignored){return false;}
        }else{
            // Generic handlers have no setStackInSlot. Deeds are single items;
            // never mutate the stack returned by getStackInSlot in place.
            var inventory=chest.inventory();
            if(old.getCount()!=1||!ItemStack.matches(inventory.extractItem(slot,1,true),old))return false;
            ItemStack extracted=inventory.extractItem(slot,1,false);
            if(!ItemStack.matches(extracted,old)){
                returnPaper(chest,slot,extracted);return false;
            }
            ItemStack rejected=inventory.insertItem(slot,replacement,false);
            if(!rejected.isEmpty()){
                returnPaper(chest,slot,extracted);return false;
            }
            chest.blockEntity().setChanged();
        }
        return matches(chest.inventory().getStackInSlot(slot),config,paperTame(replacement).ensureTlId());
    }
    private static void returnPaper(LoadedChestDrums.Chest chest,int slot,ItemStack stack){
        if(stack.isEmpty())return;
        ItemStack remainder=chest.inventory().insertItem(slot,stack,false);
        if(!remainder.isEmpty())remainder=net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(chest.inventory(),remainder,false);
        if(!remainder.isEmpty()&&chest.blockEntity().getLevel() instanceof ServerLevel level)
            level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level,chest.pos().getX()+.5,chest.pos().getY()+1,chest.pos().getZ()+.5,remainder));
    }
    private static boolean powered(LoadedChestDrums.Chest chest){ServerLevel level=(ServerLevel)chest.blockEntity().getLevel();return level.hasNeighborSignal(chest.pos())||level.hasNeighborSignal(chest.drumPos());}
    private static boolean hasPaper(Setup setup,Config config,UUID identity){
        for(int slot=0;slot<setup.chest.inventory().getSlots();slot++)if(matches(setup.chest.inventory().getStackInSlot(slot),config,identity))return true;
        return false;
    }
    public static boolean authorized(UUID token){
        Action action=AUTHORIZED.get(token);if(action==null||!token.equals(LATEST.get(action.identity)))return false;
        Setup setup=action.setup;if(SETUPS.get(setup.key)!=setup||setup.chest.blockEntity().isRemoved()||BLOCKERS.containsKey(setup.key))return false;
        var chunk=setup.key.level.getChunkSource().getChunkNow(setup.key.pos.getX()>>4,setup.key.pos.getZ()>>4);
        return chunk!=null&&chunk.getBlockEntity(setup.key.pos)==setup.chest.blockEntity()&&inspect(setup.chest).configs.contains(action.config)
                &&powered(setup.chest)==action.deploy&&hasPaper(setup,action.config,action.identity);
    }
    private static void retire(UUID token){Action action=AUTHORIZED.remove(token);if(action!=null){LATEST.remove(action.identity,token);ACTIONS.remove(action.identity,action);}}
    private static void invalidate(Setup setup){for(var entry:setup.members.entrySet()){retire(entry.getValue().token);unlinkActivity(setup,entry.getKey());}setup.members.clear();}
    public static String status(LoadedChestDrums.Status status){
        if(!status.active())return "Inactive: overlaps chest x drum at "+status.blocker().toShortString()+".";
        var chest=status.chest();Inspection inspection=inspect(chest);if(!inspection.problem.isBlank())return "Inactive: "+inspection.problem;
        Set<UUID> linked=new HashSet<>();int unbound=0,rejected=0,missing=0,bodyguards=0,inactive=0;
        for(int slot=0;slot<chest.inventory().getSlots();slot++){
            ItemStack stack=chest.inventory().getStackInSlot(slot);if(!stack.is(DIItemRegistry.DEED_OF_OWNERSHIP.get()))continue;
            Config config=configForPaper(stack,inspection.configs);TameData data=paperTame(stack);
            if(config==null||data==null){if(!isPaper(stack)&&(!stack.hasTag()||!stack.getTag().getBoolean("HasBoundEntity")))unbound++;else rejected++;continue;}
            UUID id=data.tlId!=null?data.tlId:data.uuid;if(!linked.add(id))continue;
            if(!data.guardianSetAnchors.containsKey(config.group))missing++;
            if(data.rosterBodyguard)bodyguards++;if(data.dead||data.stored)inactive++;
        }
        return "Ready; power "+(powered(chest)?"ON":"OFF")+"; "+inspection.configs.size()+" owner/group label(s), "+linked.size()+" linked tame(s)"
                +"; missing posts "+missing+", bodyguards "+bodyguards+", dead/stored "+inactive+", unbound papers "+unbound+", unmatched/ambiguous papers "+rejected
                +(inspection.unresolved>0?", unresolved owner labels "+inspection.unresolved:"")+".";
    }
    public static void scan(LoadedChestDrums.Chest chest){
        if(!(chest.blockEntity().getLevel() instanceof ServerLevel level))return;
        Key key=key(chest);Setup setup=SETUPS.get(key);Inspection inspection=inspect(chest);
        if(BLOCKERS.containsKey(key)||!inspection.problem.isBlank()){
            if(setup!=null){invalidate(setup);SETUPS.remove(key);}return;
        }
        boolean fresh=setup==null;
        if(fresh){setup=new Setup(key);SETUPS.put(key,setup);}
        if(setup.chest!=null&&setup.chest.blockEntity()!=chest.blockEntity()){invalidate(setup);fresh=true;}
        setup.chest=chest;setup.configs=inspection.configs;boolean on=powered(chest),edge=!fresh&&on!=setup.powered;setup.powered=on;
        Set<String> protect=new HashSet<>();
        for(int slot=0;slot<chest.inventory().getSlots();slot++){
            var stack=chest.inventory().getStackInSlot(slot);
            if(!stack.is(DIBlockRegistry.DRUM.get().asItem())||!stack.hasCustomHoverName())continue;
            String name=stack.getHoverName().getString().trim();int split=name.indexOf(':');
            if(split>0&&name.substring(0,split).trim().equalsIgnoreCase("protect")){
                String player=name.substring(split+1).trim();if(!player.isBlank())protect.add(player);
            }
        }
        if(!protect.equals(setup.protectPlayers)){setup.protectPlayers=Set.copyOf(protect);setup.nextProtectCheck=Integer.MIN_VALUE;setup.protectOnline=false;}
        Map<UUID,Config> present=new LinkedHashMap<>();Set<UUID> conflicts=new HashSet<>();
        for(int slot=0;slot<chest.inventory().getSlots();slot++){
            ItemStack stack=chest.inventory().getStackInSlot(slot);Config config=configForPaper(stack,setup.configs);if(config==null)continue;
            if(!storePaper(chest,slot,config))continue;
            TameData data=paperTame(chest.inventory().getStackInSlot(slot));UUID id=data.ensureTlId();
            Config old=present.putIfAbsent(id,config);if(old!=null&&!old.equals(config))conflicts.add(id);
        }
        for(UUID conflict:conflicts)present.remove(conflict);
        for(UUID old:new ArrayList<>(setup.members.keySet()))if(!present.containsKey(old)||!setup.members.get(old).config.equals(present.get(old))){retire(setup.members.remove(old).token);unlinkActivity(setup,old);}
        for(var entry:present.entrySet()){
            UUID id=entry.getKey();Config config=entry.getValue();boolean added=!setup.members.containsKey(id);
            ACTIVITY_LINKS.computeIfAbsent(id,ignored->Collections.newSetFromMap(new IdentityHashMap<>())).add(setup);
            if(edge||added){
                Member old=setup.members.remove(id);if(old!=null)retire(old.token);UUID token=UUID.randomUUID();setup.members.put(id,new Member(config,token));
                TameData data=TameRegistry.getByTlId(id);
                if((edge||on)&&data!=null&&!data.rosterBodyguard&&!data.dead&&!data.stored&&(!on||data.guardianSetAnchors.containsKey(config.group))){
                    Action action=new Action(setup,config,id,token,on,level.getServer().getTickCount()+1200);AUTHORIZED.put(token,action);LATEST.put(id,token);ACTIONS.put(id,action);
                }
            }
        }

    }
    public static void refresh(ServerLevel level,List<LoadedChestDrums.Status> statuses){
        Set<BlockPos> loaded=new HashSet<>();
        for(var status:statuses){Key key=new Key(level,status.chest().pos());loaded.add(key.pos);SCANS.put(key,status.chest());
            if(status.active())BLOCKERS.remove(key);else{BLOCKERS.put(key,status.blocker());Setup old=SETUPS.remove(key);if(old!=null)invalidate(old);}}
        for(var iterator=SETUPS.entrySet().iterator();iterator.hasNext();){var entry=iterator.next();if(entry.getKey().level==level&&!loaded.contains(entry.getKey().pos)){invalidate(entry.getValue());iterator.remove();}}
        BLOCKERS.keySet().removeIf(key->key.level==level&&!loaded.contains(key.pos));
    }
    public static List<TameData> feedingMembers(LoadedChestDrums.Chest chest){
        if(!(chest.blockEntity().getLevel() instanceof ServerLevel))return List.of();
        Setup setup=SETUPS.get(key(chest));if(setup==null||setup.chest.blockEntity()!=chest.blockEntity())return List.of();
        Set<Config> configs=inspect(chest).configs;List<TameData> result=new ArrayList<>();
        for(var entry:setup.members.entrySet()){
            TameData data=TameRegistry.getByTlId(entry.getKey());Config config=entry.getValue().config;
            if(configs.contains(config)&&data!=null&&config.owner.equals(data.ownerUUID)&&data.hasHome&&!data.rosterBodyguard&&!data.dead&&!data.stored&&hasPaper(setup,config,entry.getKey()))result.add(data);
        }return result;
    }
    public static boolean handlesRedstone(net.minecraft.world.level.Level world,BlockPos drumPos){
        if(!(world instanceof ServerLevel level))return false;
        var chest=LoadedChestDrums.chestAtDrum(level,drumPos);if(chest==null)return false;Key key=key(chest);
        Setup setup=SETUPS.get(key);if(setup!=null){DIRTY.put(key,chest);return true;}
        // Invalid controllers must not accidentally issue an ordinary broad drum movement command.
        return hasIntent(chest);
    }
    @SubscribeEvent public static void neighbors(BlockEvent.NeighborNotifyEvent event){
        if(SETUPS.isEmpty()||!(event.getLevel() instanceof ServerLevel level))return;
        for(var direction:event.getNotifiedSides()){
            BlockPos pos=event.getPos().relative(direction);
            for(BlockPos candidate:List.of(pos,pos.above(),pos.below())){Setup setup=SETUPS.get(new Key(level,candidate));if(setup!=null)DIRTY.put(setup.key,setup.chest);}
        }
    }
    public static void removeDrum(ServerLevel level,BlockPos drum){
        for(BlockPos pos:List.of(drum.above(),drum.below())){
            Key key=new Key(level,pos);Setup setup=SETUPS.get(key);
            if(setup!=null&&setup.chest.drumPos().equals(drum)){invalidate(setup);SETUPS.remove(key);}
            SCANS.remove(key);DIRTY.remove(key);BLOCKERS.remove(key);
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var scans=DIRTY.isEmpty()?SCANS:DIRTY;
        if(!scans.isEmpty()){var iterator=scans.entrySet().iterator();var entry=iterator.next();var chest=entry.getValue();Key key=entry.getKey();iterator.remove();SCANS.remove(key);scan(chest);}
        if(!ACTIONS.isEmpty()){
            var iterator=ACTIONS.values().iterator();Action action=iterator.next();iterator.remove();if(!authorized(action.token))return;
            TameData data=TameRegistry.getByTlId(action.identity);
            if(data==null||data.dead||data.rosterBodyguard||!action.config.owner.equals(data.ownerUUID))return;
            if(!TameCommands.guardianAutomationMove(event.getServer(),data,action.config.group,action.deploy,action.token)
                    &&event.getServer().getTickCount()<action.expires)ACTIONS.put(action.identity,action);
        }else if(!MANUAL.isEmpty()){
            var iterator=MANUAL.values().iterator();Manual manual=iterator.next();iterator.remove();TameData data=TameRegistry.getByTlId(manual.identity);
            if(data==null||!manual.owner.equals(data.ownerUUID)||data.rosterBodyguard||data.dead)return;
            boolean done=TameCommands.guardianAutomationMove(event.getServer(),data,manual.group,manual.deploy,null);
            if(!done&&event.getServer().getTickCount()<manual.expires)MANUAL.put(manual.identity,manual);
            else{var caller=event.getServer().getPlayerList().getPlayer(manual.caller);if(caller!=null)caller.sendSystemMessage(Component.literal((done?"Guardian request processed for ":"Guardian request timed out for ")+data.name+"."));}
        }
    }
    private static void requestManual(ServerPlayer player,ItemStack stack,boolean deploy){
        TameData data=paperTame(stack);var binding=stack.getTag().getCompound(PAPER);
        if(data==null||!binding.hasUUID("Owner")||!binding.getUUID("Owner").equals(data.ownerUUID)){player.sendSystemMessage(Component.literal("This guardian paper no longer matches the tame's owner."));return;}
        if(data.rosterBodyguard){player.sendSystemMessage(Component.literal("Bodyguards cannot be overridden by guardian papers."));return;}
        if(data.dead||data.stored){player.sendSystemMessage(Component.literal("This tame is dead or stored and cannot be moved by a guardian paper."));return;}
        String group=binding.getString("Group");
        if(deploy&&!data.guardianSetAnchors.containsKey(group)){player.sendSystemMessage(Component.literal("Right click the ground with this paper to set its guardian post first."));return;}
        if(!deploy&&!data.hasHome){player.sendSystemMessage(Component.literal(data.name+" is not assigned as a guardian."));return;}
        if(player.getCooldowns().isOnCooldown(DIItemRegistry.DEED_OF_OWNERSHIP.get()))return;
        player.getCooldowns().addCooldown(DIItemRegistry.DEED_OF_OWNERSHIP.get(),10);
        UUID id=data.ensureTlId();UUID previous=LATEST.remove(id);if(previous!=null)retire(previous);ACTIONS.remove(id);
        MANUAL.put(id,new Manual(player.getUUID(),data.ownerUUID,id,group,deploy,player.server.getTickCount()+1200));
        player.sendSystemMessage(Component.literal((deploy?"Guardian deployment queued for ":"Guardian release/tphome queued for ")+data.name+"."));
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void usePaper(PlayerInteractEvent.RightClickBlock event){
        if(!isPaper(event.getItemStack()))return;event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        if(player.isShiftKeyDown()){requestManual(player,event.getItemStack(),true);return;}
        TameData data=paperTame(event.getItemStack());var binding=event.getItemStack().getTag().getCompound(PAPER);
        if(data==null||!binding.hasUUID("Owner")||!binding.getUUID("Owner").equals(data.ownerUUID)){player.sendSystemMessage(Component.literal("This guardian paper no longer matches its tame."));return;}
        TameCommands.setGuardianPaperPost(data,binding.getString("Group"),player.serverLevel(),event.getPos().relative(event.getFace()));
        player.sendSystemMessage(Component.literal("Guardian post for "+data.name+" in '"+binding.getString("Group")+"' set."));
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void useInAir(PlayerInteractEvent.RightClickItem event){
        if(!isPaper(event.getItemStack())||!event.getEntity().isShiftKeyDown())return;
        event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
        if(event.getEntity() instanceof ServerPlayer player)requestManual(player,event.getItemStack(),true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST,receiveCanceled=true) public static void punchBlock(PlayerInteractEvent.LeftClickBlock event){
        if(!isPaper(event.getItemStack()))return;event.setCanceled(true);
        if(event.getEntity() instanceof ServerPlayer player&&event.getHand()==InteractionHand.MAIN_HAND)requestManual(player,event.getItemStack(),false);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST,receiveCanceled=true) public static void punchEntity(AttackEntityEvent event){
        if(!isPaper(event.getEntity().getMainHandItem()))return;event.setCanceled(true);
        if(event.getEntity() instanceof ServerPlayer player)requestManual(player,player.getMainHandItem(),false);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void protectBlock(BlockEvent.BreakEvent event){if(isPaper(event.getPlayer().getMainHandItem()))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void blockAdoption(PlayerInteractEvent.EntityInteract event){if(isPaper(event.getItemStack())){event.setCanceled(true);event.setCancellationResult(InteractionResult.FAIL);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void blockAdoptionSpecific(PlayerInteractEvent.EntityInteractSpecific event){if(isPaper(event.getItemStack())){event.setCanceled(true);event.setCancellationResult(InteractionResult.FAIL);}}
    @SubscribeEvent public static void unload(net.minecraftforge.event.level.LevelEvent.Unload event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        for(var iterator=SETUPS.entrySet().iterator();iterator.hasNext();){var entry=iterator.next();if(entry.getKey().level==level){invalidate(entry.getValue());iterator.remove();}}
        SCANS.keySet().removeIf(key->key.level==level);DIRTY.keySet().removeIf(key->key.level==level);BLOCKERS.keySet().removeIf(key->key.level==level);
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event){ACTIVITY_LINKS.clear();SETUPS.clear();AUTHORIZED.clear();SCANS.clear();DIRTY.clear();BLOCKERS.clear();ACTIONS.clear();LATEST.clear();MANUAL.clear();}
}
