package com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.rewards;

public class WeightedReward {

    public LevelReward reward;
    public double weight;

    public WeightedReward(LevelReward reward, double weight) {
        this.reward = reward;
        this.weight = weight;
    }
}
