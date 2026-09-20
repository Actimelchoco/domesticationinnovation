package com.github.alexthe668.domesticationinnovation.server.tameslevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/** Pure numeric planning. No Minecraft objects, inventories, registries or world access. */
final class FoodDistributionPlanner {
    record Food(int slot, int type, int count) { }
    record Target(long saturation, int freeSlots, int[] partialCapacity, int[] nutrition) {
        Target { partialCapacity = partialCapacity.clone(); nutrition = nutrition.clone(); }
    }
    record Snapshot(int[] stackSizes, List<Food> foods, List<Target> targets) {
        Snapshot { stackSizes = stackSizes.clone(); foods = List.copyOf(foods); targets = List.copyOf(targets); }
    }
    record Allocation(int slot, int type, int count) { }
    record Plan(List<List<Allocation>> targets) {
        Plan { targets = targets.stream().map(List::copyOf).toList(); }
    }
    private static final class State {
        final int index;
        final int[] partial;
        long saturation;
        int freeSlots;
        State(int index, Target target) {
            this.index = index;
            partial = target.partialCapacity().clone();
            saturation = target.saturation();
            freeSlots = target.freeSlots();
        }
        int capacity(int type, int size) { return partial[type] + freeSlots * size; }
        void reserve(int type, int size, int count) {
            int remaining = count - Math.min(count, partial[type]);
            partial[type] = Math.max(0, partial[type] - count);
            if (remaining > 0) {
                int slots = (remaining + size - 1) / size;
                freeSlots -= slots;
                partial[type] += slots * size - remaining;
            }
        }
    }
    private FoodDistributionPlanner() { }

    static Plan plan(Snapshot snapshot) {
        List<State> states = new ArrayList<>();
        List<List<Allocation>> result = new ArrayList<>();
        for (int i = 0; i < snapshot.targets().size(); i++) {
            states.add(new State(i, snapshot.targets().get(i)));
            result.add(new ArrayList<>());
        }
        for (Food food : snapshot.foods()) {
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
            int type = food.type();
            int size = snapshot.stackSizes()[type];
            int[] assigned = new int[states.size()];
            PriorityQueue<State> queue = new PriorityQueue<>(Comparator.<State>comparingLong(s -> s.saturation).thenComparingInt(s -> s.index));
            for (State state : states) {
                if (snapshot.targets().get(state.index).nutrition()[type] > 0 && state.capacity(type, size) > 0) queue.add(state);
            }
            int remaining = food.count();
            while (remaining > 0 && !queue.isEmpty()) {
                if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
                State state = queue.remove();
                int nutrition = snapshot.targets().get(state.index).nutrition()[type];
                long untilNext = queue.isEmpty() ? remaining : Math.max(1L,
                        (queue.peek().saturation - state.saturation + nutrition - 1) / nutrition);
                int count = (int) Math.min(Math.min(remaining, state.capacity(type, size)), untilNext);
                assigned[state.index] += count;
                state.reserve(type, size, count);
                state.saturation += (long) count * nutrition;
                remaining -= count;
                if (state.capacity(type, size) > 0) queue.add(state);
            }
            for (int i = 0; i < assigned.length; i++) {
                if (assigned[i] > 0) result.get(i).add(new Allocation(food.slot(), type, assigned[i]));
            }
        }
        return new Plan(result);
    }
}
