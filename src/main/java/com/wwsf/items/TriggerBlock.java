package com.wwsf.items;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import com.wwsf.WWSFPlugin;
import com.wwsf.artillery.ArtilleryDefinition;
import com.wwsf.artillery.ArtillerySpec;
import com.wwsf.multiblock.CannonDefinition;
import com.wwsf.util.Messages;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

import java.util.ArrayList;
import java.util.List;

public class TriggerBlock extends AbstractArtilleryBlock {

    private final CannonDefinition cannonDefinition;
    // All registered cannon definitions, so the trigger can be matched even if the block
    // reverts to a vanilla tripwire hook (Slimefun state lost).
    private static final List<CannonDefinition> DEFINITIONS = new ArrayList<>();

    public TriggerBlock(ItemGroup category, ArtilleryDefinition definition, CannonDefinition cannonDefinition) {
        super(category, definition);
        this.cannonDefinition = cannonDefinition;
        if (!DEFINITIONS.contains(cannonDefinition)) {
            DEFINITIONS.add(cannonDefinition);
        }
    }

    @Nonnull
    public CannonDefinition getCannonDefinition() {
        return cannonDefinition;
    }

    @Nullable
    public CannonDefinition findMatchingDefinition(@Nonnull Block core) {
        return cannonDefinition.isValid(core) ? cannonDefinition : null;
    }

    /**
     * Finds a matching cannon definition for any tripwire hook (including one that has reverted
     * to a vanilla block) by validating the multiblock structure around it.
     */
    @Nullable
    public static CannonDefinition findMatching(@Nonnull Block core) {
        for (CannonDefinition def : DEFINITIONS) {
            if (def.isValid(core)) {
                return def;
            }
        }
        return null;
    }

    @Override
    public void preRegister() {
    }

    @Override
    protected void handleRightClick(PlayerRightClickEvent event) {
    }

    @Nullable
    public static TriggerBlock getTriggerAt(@Nonnull Block block) {
        SlimefunItem item = BlockStorage.check(block.getLocation());
        if (item instanceof TriggerBlock artillery) {
            return artillery;
        }
        return null;
    }
}
