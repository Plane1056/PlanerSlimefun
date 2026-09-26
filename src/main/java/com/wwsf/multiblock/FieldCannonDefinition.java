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

public final class FieldCannonDefinition implements CannonDefinition {

    private final String displayName;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final Material anchorMaterial;
    private final int anchorX;
    private final int anchorY;
    private final int anchorZ;
    private final int barrelTipX;
    private final int barrelTipY;
    private final int barrelTipZ;
    public final Map<Character, Material> palette;
    public final Map<Integer, String[]> layerRows;
    private final String onCompleteEvent;
    private volatile int lastRotation;
    private volatile Location lastOrigin;

    public FieldCannonDefinition(
        @Nonnull String displayName,
        int sizeX, int sizeY, int sizeZ,
        @Nonnull Material anchorMaterial,
        int anchorX, int anchorY, int anchorZ,
        int barrelTipX, int barrelTipY, int barrelTipZ,
        @Nonnull Map<Character, Material> palette,
        @Nonnull Map<Integer, String[]> layerRows,
        @Nonnull String onCompleteEvent
    ) {
        this.displayName = displayName;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.anchorMaterial = anchorMaterial;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
        this.barrelTipX = barrelTipX;
        this.barrelTipY = barrelTipY;
        this.barrelTipZ = barrelTipZ;
        this.palette = Collections.unmodifiableMap(palette);
        this.layerRows = Collections.unmodifiableMap(layerRows);
        this.onCompleteEvent = onCompleteEvent;
    }

    @Override
    public @Nonnull String getId() { return "field_cannon"; }

    @Override
    public @Nonnull String getName() { return displayName; }

    @Override
    public synchronized boolean isValid(@Nonnull Block core) {
        com.wwsf.multiblock.FieldCannonValidator.Result result = com.wwsf.multiblock.FieldCannonValidator.validate(this, core);
        if (result.isValid()) {
            lastRotation = result.getRotation();
            lastOrigin = result.getOrigin();
        }
        return result.isValid();
    }

    @Override
    public @Nonnull String getMissingBlocksMessage(@Nonnull Block trigger) {
        com.wwsf.multiblock.FieldCannonValidator.Result result = com.wwsf.multiblock.FieldCannonValidator.validate(this, trigger);
        if (result.isValid()) {
            return "&aStructure complete!";
        }
        StringBuilder msg = new StringBuilder("&cMissing/incorrect blocks for &f" + getName() + "&c:\n");
        for (com.wwsf.multiblock.FieldCannonValidator.BlockMismatch m : result.getMismatches()) {
            msg.append("&7- ").append(m.describe()).append("\n");
        }
        return msg.toString();
    }

    @Override
    public int getLastRotation() { return lastRotation; }

    @Override
    public synchronized @Nonnull Location getBarrelTip(@Nonnull Location origin, int rotation) {
        int[] rotated = com.wwsf.multiblock.FieldCannonValidator.rotate(barrelTipX, barrelTipZ, rotation);
        return new Location(origin.getWorld(),
            origin.getBlockX() + rotated[0],
            origin.getBlockY() + barrelTipY,
            origin.getBlockZ() + rotated[1]);
    }

    @Override
    public synchronized @Nonnull Vector getBarrelDirection(int rotation) {
        int[] rotated = com.wwsf.multiblock.FieldCannonValidator.rotate(1, 0, rotation);
        return new Vector(rotated[0], 0, rotated[1]).normalize();
    }

    @Override
    public @Nonnull ArtillerySpec getWeaponSpec() {
            return new ArtillerySpec(
                getId(), Material.DARK_OAK_FENCE_GATE, "&8" + displayName,
            new String[]{"Long-range flat-trajectory field gun with slight drop."},
            null, WeaponRole.CANNON, TrajectoryMode.DIRECT,
             90, 1, 3.0, 0, 0,
             6.0, null
        );
    }

    @Nonnull
    public String getDisplayName() { return displayName; }

    public int getSizeX() { return sizeX; }
    public int getSizeY() { return sizeY; }
    public int getSizeZ() { return sizeZ; }

    @Nonnull
    public Material getAnchorMaterial() { return anchorMaterial; }

    @Nonnull
    @Override
    public Material getTriggerMaterial() { return anchorMaterial; }

    public int getAnchorX() { return anchorX; }
    public int getAnchorY() { return anchorY; }
    public int getAnchorZ() { return anchorZ; }

    @Override
    public int triggerOffsetX() { return anchorX; }

    @Override
    public int triggerOffsetY() { return anchorY; }

    @Override
    public int triggerOffsetZ() { return anchorZ; }

    @Nonnull
    public Map<Character, Material> getPalette() { return palette; }

    @Nullable
    public String[] getLayerRows(int y) { return layerRows.get(y); }

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

    @Nonnull
    public String getOnCompleteEvent() { return onCompleteEvent; }
}