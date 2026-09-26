package com.oildrills;

import java.util.concurrent.ThreadLocalRandom;

public enum OilRarity {
    DRY_VOID(0, 49, "§8§lDry Void", 0.35),
    TRACE(50, 99, "§7§lTrace Pockets", 0.25),
    STABLE(100, 249, "§e§lStable Reservoir", 0.20),
    ABUNDANT(250, 399, "§a§lAbundant Well", 0.15),
    MASSIVE(400, 500, "§6§lMassive Field", 0.05);

    private final int min;
    private final int max;
    private final String label;
    private final double weight;

    OilRarity(int min, int max, String label, double weight) {
        this.min = min;
        this.max = max;
        this.label = label;
        this.weight = weight;
    }

    public String getLabel() {
        return label;
    }

    public int rollBarrels() {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public static OilRarity fromBarrels(int barrels) {
        if (barrels < TRACE.min) return DRY_VOID;
        if (barrels < STABLE.min) return TRACE;
        if (barrels < ABUNDANT.min) return STABLE;
        if (barrels < MASSIVE.min) return ABUNDANT;
        return MASSIVE;
    }

    public static OilRarity rollRandom() {
        double roll = ThreadLocalRandom.current().nextDouble();
        double cumulative = 0;
        for (OilRarity rarity : values()) {
            cumulative += rarity.weight;
            if (roll < cumulative) {
                return rarity;
            }
        }
        return MASSIVE;
    }
}
