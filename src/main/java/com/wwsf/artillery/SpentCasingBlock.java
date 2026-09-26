package com.wwsf.artillery;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.util.Vector;

/**
 * Places the spent large-shell casing left by a successful artillery shot.
 *
 * <p>The combined resource pack reserves powered Piglin notes 11-13 for the
 * HE and smoke casing models. Placement follows the exact-state
 * NoteBlock pattern used by the sandbag carrier and never overwrites a block.</p>
 */
public final class SpentCasingBlock {

    static final int HE_NOTE = 11;
    static final int SMOKE_NOTE = 12;

    private static final int[][] FALLBACK_OFFSETS = {
        {0, 2},
        {0, -2},
        {2, 0},
        {-2, 0},
        {0, 3},
        {0, -3},
        {3, 0},
        {-3, 0}
    };

    private SpentCasingBlock() {
    }

    public static boolean place(
        @Nonnull Location cannon,
        @Nullable Vector barrelDirection,
        @Nonnull ShellType shellType
    ) {
        if (cannon.getWorld() == null) {
            return false;
        }

        for (int[] offset : candidateOffsets(barrelDirection)) {
            Block candidate = cannon.getBlock().getRelative(offset[0], 0, offset[1]);
            if (!candidate.isEmpty()) {
                continue;
            }
            candidate.setBlockData(createBlockData(shellType), false);
            return true;
        }
        return false;
    }

    @Nonnull
    static BlockData createBlockData(@Nonnull ShellType shellType) {
        NoteBlock data = (NoteBlock) Material.NOTE_BLOCK.createBlockData();
        data.setInstrument(Instrument.PIGLIN);
        data.setNote(new Note(noteFor(shellType)));
        data.setPowered(true);
        return data;
    }

    static int noteFor(@Nonnull ShellType shellType) {
        return switch (shellType) {
            case SMOKE -> SMOKE_NOTE;
            default -> HE_NOTE;
        };
    }

    static boolean isCasing(@Nonnull Block block) {
        return casingNote(block) >= 0;
    }

    static int casingNote(@Nonnull Block block) {
        if (!(block.getBlockData() instanceof NoteBlock noteBlock)
            || noteBlock.getInstrument() != Instrument.PIGLIN
            || !noteBlock.isPowered()) {
            return -1;
        }
        int note = noteBlock.getNote().getId();
        return note >= HE_NOTE && note <= SMOKE_NOTE ? note : -1;
    }

    static void restore(@Nonnull Block block, int note) {
        if (block.getType() != Material.NOTE_BLOCK
            || note < HE_NOTE
            || note > SMOKE_NOTE) {
            return;
        }
        NoteBlock data = (NoteBlock) Material.NOTE_BLOCK.createBlockData();
        data.setInstrument(Instrument.PIGLIN);
        data.setNote(new Note(note));
        data.setPowered(true);
        block.setBlockData(data, false);
    }

    static List<int[]> candidateOffsets(@Nullable Vector barrelDirection) {
        if (barrelDirection == null
            || !Double.isFinite(barrelDirection.getX())
            || !Double.isFinite(barrelDirection.getZ())
            || Math.abs(barrelDirection.getX()) + Math.abs(barrelDirection.getZ()) < 0.001) {
            return List.of(FALLBACK_OFFSETS);
        }

        int forwardX = Math.abs(barrelDirection.getX()) >= Math.abs(barrelDirection.getZ())
            ? (barrelDirection.getX() >= 0 ? 1 : -1)
            : 0;
        int forwardZ = forwardX == 0 ? (barrelDirection.getZ() >= 0 ? 1 : -1) : 0;
        int rightX = -forwardZ;
        int rightZ = forwardX;

        List<int[]> offsets = new ArrayList<>(8);
        offsets.add(new int[]{rightX * 2, rightZ * 2});
        offsets.add(new int[]{rightX * 3, rightZ * 3});
        offsets.add(new int[]{-rightX * 2, -rightZ * 2});
        offsets.add(new int[]{-rightX * 3, -rightZ * 3});
        offsets.add(new int[]{-forwardX + rightX * 2, -forwardZ + rightZ * 2});
        offsets.add(new int[]{-forwardX - rightX * 2, -forwardZ - rightZ * 2});
        offsets.add(new int[]{-forwardX * 2, -forwardZ * 2});
        offsets.add(new int[]{-forwardX * 3, -forwardZ * 3});
        return offsets;
    }
}
