package com.github.alexthe668.domesticationinnovation.server.tameslevel;

import java.util.ArrayList;
import java.util.List;

public final class FoodDistributionPlannerTests {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        List<FoodDistributionPlanner.Food> foods = new ArrayList<>();
        for (int i = 0; i < 3125; i++) foods.add(new FoodDistributionPlanner.Food(i, 0, 64));
        List<FoodDistributionPlanner.Target> targets = new ArrayList<>();
        for (int i = 0; i < 40; i++) targets.add(new FoodDistributionPlanner.Target(0, 18, new int[]{0}, new int[]{1600}));
        var snapshot = new FoodDistributionPlanner.Snapshot(new int[]{64}, foods, targets);
        long start = System.nanoTime();
        var plan = FoodDistributionPlanner.plan(snapshot);
        int total = 0;
        for (var target : plan.targets()) {
            int count = target.stream().mapToInt(FoodDistributionPlanner.Allocation::count).sum();
            check(count == 1152, "Every tame limited to eighteen stacks");
            total += count;
        }
        check(total == 46080, "200,000 source items cannot exceed target capacity");
        check(snapshot.targets().get(0).freeSlots() == 18 && snapshot.targets().get(0).partialCapacity()[0] == 0,
                "Snapshot remains unchanged");
        check(FoodDistributionPlanner.plan(snapshot).equals(plan), "Snapshot reusable with independent planner state");
        var shared = FoodDistributionPlanner.plan(new FoodDistributionPlanner.Snapshot(new int[]{64, 64},
                List.of(new FoodDistributionPlanner.Food(0, 0, 1), new FoodDistributionPlanner.Food(1, 1, 64)),
                List.of(new FoodDistributionPlanner.Target(0, 1, new int[]{0, 0}, new int[]{1, 1}))));
        check(shared.targets().get(0).stream().mapToInt(FoodDistributionPlanner.Allocation::count).sum() == 1,
                "Different foods share the same free slot budget");
        var partial = FoodDistributionPlanner.plan(new FoodDistributionPlanner.Snapshot(new int[]{64},
                List.of(new FoodDistributionPlanner.Food(0, 0, 100)),
                List.of(new FoodDistributionPlanner.Target(0, 0, new int[]{3}, new int[]{1}))));
        check(partial.targets().get(0).get(0).count() == 3, "Partial stack capacity remains usable when slots full");
        var rejected = FoodDistributionPlanner.plan(new FoodDistributionPlanner.Snapshot(new int[]{64},
                List.of(new FoodDistributionPlanner.Food(0, 0, 100)),
                List.of(new FoodDistributionPlanner.Target(0, 18, new int[]{0}, new int[]{0}))));
        check(rejected.targets().get(0).isEmpty(), "Incompatible foods never allocated");
        System.out.println("FOOD_DISTRIBUTION_PLANNER_PASS 200000 items / 40 tames; elapsed ms=" + (System.nanoTime() - start) / 1_000_000.0);
    }
}
