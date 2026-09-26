package com.wwsf.multiblock;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.Material;
import org.bukkit.block.Block;

import com.wwsf.artillery.ArtillerySpec;
import com.wwsf.artillery.TrajectoryMode;
import com.wwsf.artillery.WeaponRole;

public final class TrenchCannonDefinition implements CannonDefinition {

    private final String displayName;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final Material triggerMaterial;
    private final int triggerX;
    private final int triggerY;
    private final int triggerZ;
    private final int barrelTipX;
    private final int barrelTipY;
    private final int barrelTipZ;
    public final Map<Character, Material> palette;
    public final Map<Integer, String[]> layerRows;
    private final String onCompleteEvent;
    private volatile int lastRotation;
    private volatile Location lastOrigin;

    public TrenchCannonDefinition(
        @Nonnull String displayName,
        int sizeX, int sizeY, int sizeZ,
        @Nonnull Material triggerMaterial,
        int triggerX, int triggerY, int triggerZ,
        int barrelTipX, int barrelTipY, int barrelTipZ,
        @Nonnull Map<Character, Material> palette,
        @Nonnull Map<Integer, String[]> layerRows,
        @Nonnull String onCompleteEvent
    ) {
        this.displayName = displayName;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.triggerMaterial = triggerMaterial;
        this.triggerX = triggerX;
        this.triggerY = triggerY;
        this.triggerZ = triggerZ;
        this.barrelTipX = barrelTipX;
        this.barrelTipY = barrelTipY;
        this.barrelTipZ = barrelTipZ;
        this.palette = Collections.unmodifiableMap(palette);
        this.layerRows = Collections.unmodifiableMap(layerRows);
        this.onCompleteEvent = onCompleteEvent;
    }

    @Override
    public @Nonnull String getId() { return "trench_cannon"; }

    @Override
    public @Nonnull String getName() { return displayName; }

    @Override
    public synchronized boolean isValid(@Nonnull Block trigger) {
        TrenchCannonValidator.Result result = TrenchCannonValidator.validate(this, trigger);
        if (result.isValid()) {
            lastRotation = result.getRotation();
            lastOrigin = result.getOrigin();
        }
        return result.isValid();
    }

    @Override
    public @Nonnull String getMissingBlocksMessage(@Nonnull Block trigger) {
        TrenchCannonValidator.Result result = TrenchCannonValidator.validate(this, trigger);
        if (result.isValid()) {
            return "&aStructure complete!";
        }
        StringBuilder msg = new StringBuilder("&cMissing/incorrect blocks for &f" + getName() + "&c:\n");
        for (TrenchCannonValidator.BlockMismatch m : result.getMismatches()) {
            msg.append("&7- ").append(m.describe()).append("\n");
        }
        return msg.toString();
    }

    @Override
    public int getLastRotation() { return lastRotation; }

    @Override
    public synchronized @Nonnull Location getBarrelTip(@Nonnull Location origin, int rotation) {
        int[] rotated = TrenchCannonValidator.rotate(barrelTipX, barrelTipZ, rotation);
        return new Location(origin.getWorld(),
            origin.getBlockX() + rotated[0],
            origin.getBlockY() + barrelTipY,
            origin.getBlockZ() + rotated[1]);
    }

    @Override
    public synchronized @Nonnull Vector getBarrelDirection(int rotation) {
        int[] rotated = TrenchCannonValidator.rotate(1, 0, rotation);
        return new Vector(rotated[0], 0, rotated[1]).normalize();
    }

    @Override
    public @Nonnull ArtillerySpec getWeaponSpec() {
            return new ArtillerySpec(
                getId(), Material.DARK_OAK_FENCE_GATE, "&7" + displayName,
            new String[]{"High-arc mortar — lobs shells over cover."},
            null, WeaponRole.MORTAR, TrajectoryMode.ARC,
             70, 1, 2.4, 12, 0,
             4.0, null
        );
    }

    public int getSizeX() { return sizeX; }
    public int getSizeY() { return sizeY; }
    public int getSizeZ() { return sizeZ; }

    @Nonnull
    public Material getTriggerMaterial() { return triggerMaterial; }

    public int getTriggerX() { return triggerX; }
    public int getTriggerY() { return triggerY; }
    public int getTriggerZ() { return triggerZ; }

    @Override
    public int triggerOffsetX() { return triggerX; }

    @Override
    public int triggerOffsetY() { return triggerY; }

    @Override
    public int triggerOffsetZ() { return triggerZ; }

    @Nonnull
    public Material getMaterialAt(int x, int y, int z) {
        String[] rows = layerRows.get(y);
        if (rows == null || z < 0 || z >= rows.length) {
            return Material.AIR;
        }
        String row = rows[z];
        if (row == null || x < 0 || x >= row.length()) {
            return Material.AIR;
        }
        char c = row.charAt(x);
        Material mat = palette.get(c);
        return mat != null ? mat : Material.AIR;
    }
}