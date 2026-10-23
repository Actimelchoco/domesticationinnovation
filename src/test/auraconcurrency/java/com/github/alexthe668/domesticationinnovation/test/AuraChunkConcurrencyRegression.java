package com.github.alexthe668.domesticationinnovation.test;

import de.ellpeck.naturesaura.Helper;
import de.ellpeck.naturesaura.api.NaturesAuraAPI;
import de.ellpeck.naturesaura.api.misc.ILevelData;
import de.ellpeck.naturesaura.chunk.AuraChunk;
import de.ellpeck.naturesaura.events.CommonEvents;
import de.ellpeck.naturesaura.misc.LevelData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.gametest.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@GameTestHolder("domesticationinnovation") @PrefixGameTestTemplate(false)
public class AuraChunkConcurrencyRegression {
    @GameTest(template="empty",timeoutTicks=1200)
    public static void parallelRealChunkDeserializationQueriesAndUnloadsPreserveAura(GameTestHelper h)throws Exception {
        var level=h.getLevel();var data=(LevelData)ILevelData.getLevelData(level);
        int threads=12,count=768;
        List<LevelChunk> chunks=new ArrayList<>();List<AuraChunk> aura=new ArrayList<>();List<CompoundTag> saved=new ArrayList<>();
        // Detached chunks exercise real capability state without loading or generating world regions.
        for(int i=0;i<count;i++){
            var chunk=new LevelChunk(level,new ChunkPos(1000000+i,1000000));chunks.add(chunk);
            aura.add(new AuraChunk(chunk,NaturesAuraAPI.TYPE_OVERWORLD));
            var spots=new ListTag();spots.add(new AuraChunk.DrainSpot(new BlockPos(chunk.getPos().getMinBlockX()+2,64,chunk.getPos().getMinBlockZ()+2),123+i).serializeNBT());
            var tag=new CompoundTag();tag.put("drain_spots",spots);saved.add(tag);
        }
        var pool=Executors.newFixedThreadPool(threads,task->{var thread=new Thread(task,"aura-test-worker");thread.setDaemon(true);return thread;});
        var events=new CommonEvents();
        try {
            for(int round=0;round<4;round++){
                var barrier=new CyclicBarrier(threads);List<Future<?>> tasks=new ArrayList<>();
                for(int t=0;t<threads;t++){final int worker=t;tasks.add(pool.submit(()->{
                    barrier.await();
                    for(int i=worker;i<count;i+=threads){
                        var current=aura.get(i);var chunk=chunks.get(i);var pos=new BlockPos(chunk.getPos().getMinBlockX()+2,64,chunk.getPos().getMinBlockZ()+2);
                        current.deserializeNBT(saved.get(i).copy());
                        var seen=new AtomicInteger();Helper.getAuraChunksWithSpotsInArea(level,pos,0,a->{if(a==current)seen.incrementAndGet();});
                        if(seen.get()!=1)throw new AssertionError("loaded chunk not visible through real aura area lookup");
                        events.onChunkUnload(new ChunkEvent.Unload(chunk));
                        current.deserializeNBT(saved.get(i).copy());
                        if(!current.serializeNBT().equals(saved.get(i)))throw new AssertionError("aura NBT changed during round-trip");
                    }
                    return null;
                }));}
                for(var task:tasks)task.get(15,TimeUnit.SECONDS);
                for(int i=0;i<count;i++)h.assertTrue(data.auraChunksWithSpots.get(chunks.get(i).getPos().toLong())==aura.get(i),"all concurrently restored chunks remain indexed");
            }
            for(int i=0;i<count;i++){
                aura.get(i).deserializeNBT(new CompoundTag());
                h.assertTrue(!data.auraChunksWithSpots.containsKey(chunks.get(i).getPos().toLong()),"empty aura chunk removes its index entry");
            }
        } finally {
            pool.shutdownNow();pool.awaitTermination(5,TimeUnit.SECONDS);
            for(var chunk:chunks)events.onChunkUnload(new ChunkEvent.Unload(chunk));
        }
        h.succeed();
    }
}
