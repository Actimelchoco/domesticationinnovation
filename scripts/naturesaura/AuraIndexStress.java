import de.ellpeck.naturesaura.compat.AuraChunkIndexGuard;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.*;
import java.util.concurrent.*;

/** Standalone reproduction using the same map and audited call signatures as chunk loading. */
public final class AuraIndexStress {
    private static final int THREADS=16,KEYS=8192;
    private static boolean guarded;
    private static Object put(Long2ObjectOpenHashMap<Object> map,long key,Object value){return guarded?AuraChunkIndexGuard.put(map,key,value):map.put(key,value);}
    private static Object get(Long2ObjectOpenHashMap<Object> map,long key){return guarded?AuraChunkIndexGuard.get(map,key):map.get(key);}
    private static Object remove(Long2ObjectOpenHashMap<Object> map,long key){return guarded?AuraChunkIndexGuard.remove(map,key):map.remove(key);}
    interface Work {void run(int worker)throws Exception;}
    private static void parallel(Work work)throws Exception {
        var start=new CyclicBarrier(THREADS);
        var pool=Executors.newFixedThreadPool(THREADS,task->{var thread=new Thread(task);thread.setDaemon(true);return thread;});
        List<Future<?>> tasks=new ArrayList<>();
        for(int i=0;i<THREADS;i++){final int worker=i;tasks.add(pool.submit(()->{start.await();work.run(worker);return null;}));}
        try {for(var task:tasks)task.get(15,TimeUnit.SECONDS);} finally {pool.shutdownNow();}
    }
    public static void main(String[] args)throws Exception {
        guarded=args.length>0&&args[0].equals("guarded");
        long start=System.nanoTime();
        for(int round=0;round<(guarded?6:1);round++){
            var map=new Long2ObjectOpenHashMap<Object>(2);
            parallel(worker->{
                for(int n=0;n<KEYS;n++){
                    long key=((long)worker<<32)|n;
                    put(map,key,key);
                    if(!Objects.equals(get(map,key),key))throw new AssertionError("concurrent insertion/read lost key "+key);
                }
            });
            if(map.size()!=THREADS*KEYS)throw new AssertionError("growth lost entries: "+map.size());
            parallel(worker->{
                for(int n=0;n<KEYS;n++){
                    long key=((long)worker<<32)|n;
                    if(!Objects.equals(remove(map,key),key))throw new AssertionError("removal lost key "+key);
                    put(map,key,~key);
                    if(!Objects.equals(get(map,key),~key))throw new AssertionError("mixed workload lost key "+key);
                }
            });
            for(int worker=0;worker<THREADS;worker++)for(int n=0;n<KEYS;n++){
                long key=((long)worker<<32)|n;if(!Objects.equals(get(map,key),~key))throw new AssertionError("final content mismatch");
            }
            parallel(worker->{for(int n=0;n<KEYS;n++)remove(map,((long)worker<<32)|n);});
            if(!map.isEmpty())throw new AssertionError("unload left stale entries");
        }
        System.out.printf("PASS guarded=%s threads=%d entries/round=%d elapsed=%.3fs%n",guarded,THREADS,THREADS*KEYS,(System.nanoTime()-start)/1E9);
    }
}
