package com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class TameClassRoller {
    private static final Random RANDOM = new Random();

    private TameClassRoller() {
    }

    static TameClass roll(ClassWeightConfig config) {
        List<WeightedClass> options = new ArrayList<>();
        if (config != null && !config.classRollWeights().isEmpty()) {
            for (Map.Entry<TameClass, Double> entry : config.classRollWeights().entrySet()) {
                options.add(new WeightedClass(entry.getKey(), entry.getValue()));
            }
        } else {
            for (TameClass tameClass : TameClass.values()) {
                options.add(new WeightedClass(tameClass, tameClass.rarity().weight()));
            }
        }
        TameClass rolled = pickWeighted(options);
        return rolled == null ? TameClass.ORDINARY : rolled;
    }

    private static TameClass pickWeighted(List<WeightedClass> options) {
        double total = 0.0D;
        for (WeightedClass option : options) {
            if (option.weight > 0.0D) {
                total += option.weight;
            }
        }
        if (total <= 0.0D) {
            return null;
        }
        double roll = RANDOM.nextDouble() * total;
        double cursor = 0.0D;
        for (WeightedClass option : options) {
            if (option.weight <= 0.0D) {
                continue;
            }
            cursor += option.weight;
            if (roll <= cursor) {
                return option.tameClass;
            }
        }
        return options.isEmpty() ? null : options.get(options.size() - 1).tameClass;
    }

    private record WeightedClass(TameClass tameClass, double weight) {
    }
}
