package com.oildrills;

import org.bukkit.block.BlockFace;

final class ActiveDrillData {

    private final OilDrillTier tier;
    private long nextExtractAtMs;
    private boolean enabled;
    private BlockFace facing;
    private int fuelCredit;
    private long lastWarningAtMs;

    ActiveDrillData(OilDrillTier tier, long nextExtractAtMs) {
        this(tier, nextExtractAtMs, false, null);
    }

    ActiveDrillData(OilDrillTier tier, long nextExtractAtMs, boolean enabled) {
        this(tier, nextExtractAtMs, enabled, null);
    }

    ActiveDrillData(OilDrillTier tier, long nextExtractAtMs, boolean enabled, BlockFace facing) {
        this(tier, nextExtractAtMs, enabled, facing, 0);
    }

    ActiveDrillData(
        OilDrillTier tier,
        long nextExtractAtMs,
        boolean enabled,
        BlockFace facing,
        int fuelCredit
    ) {
        this.tier = tier;
        this.nextExtractAtMs = nextExtractAtMs;
        this.enabled = enabled;
        this.facing = facing;
        this.fuelCredit = Math.max(0, fuelCredit);
    }

    OilDrillTier tier() {
        return tier;
    }

    boolean isEnabled() {
        return enabled;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    BlockFace facing() {
        return facing;
    }

    void setFacing(BlockFace facing) {
        this.facing = facing;
    }

    int fuelCredit() {
        return fuelCredit;
    }

    void addFuelCredit(int amount) {
        if (amount <= 0) {
            return;
        }
        fuelCredit = (int) Math.min(Integer.MAX_VALUE, (long) fuelCredit + amount);
    }

    int spendFuelCredit(int amount) {
        int spent = Math.min(Math.max(0, amount), fuelCredit);
        fuelCredit -= spent;
        return spent;
    }

    boolean isReady() {
        return System.currentTimeMillis() >= nextExtractAtMs;
    }

    void scheduleNext(long delayMs) {
        nextExtractAtMs = System.currentTimeMillis() + delayMs;
    }

    long nextExtractAtMs() {
        return nextExtractAtMs;
    }

    long secondsUntilNext() {
        return Math.max(0, (nextExtractAtMs - System.currentTimeMillis()) / 1000);
    }

    boolean shouldWarn(long intervalMs) {
        long now = System.currentTimeMillis();
        if (now - lastWarningAtMs < intervalMs) {
            return false;
        }
        lastWarningAtMs = now;
        return true;
    }
}
