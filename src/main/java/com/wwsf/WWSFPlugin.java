package com.wwsf;

import java.util.logging.Level;

import javax.annotation.Nonnull;

import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import com.oildrills.OilDrillsPlugin;
import com.wwsf.ammo.AmmoRegistry;
import com.wwsf.artillery.AimingManager;
import com.wwsf.artillery.ArtilleryCombatListener;
import com.wwsf.artillery.SpentCasingListener;
import com.wwsf.commands.WwsfCommand;
import com.wwsf.fortifications.FortificationsRegistry;
import com.wwsf.fortifications.SteelBlastResistanceListener;
import com.wwsf.fortifications.SteelRecipeOverride;
import com.wwsf.gas.GasGrenadeListener;
import com.wwsf.gas.GasGrenadeRegistry;
import com.wwsf.gas.GasImmunityCommand;
import com.wwsf.gas.GasImmunityConfig;
import com.wwsf.items.MultiblockInteractListener;
import com.wwsf.items.TriggerInteractListener;
import com.wwsf.items.TriggerItemListener;
import com.wwsf.integration.HowitzerArtilleryBridge;
import com.wwsf.integration.HowitzerRecipeRegistry;
import com.wwsf.integration.GasDrumCraftCleanupListener;
import com.wwsf.multiblock.MultiblockPlaceListener;
import com.wwsf.setup.CustomModelDataRegistry;
import com.wwsf.setup.ElectricityCategory;
import com.wwsf.setup.FortificationsCategory;
import com.wwsf.setup.GunsCategory;
import com.wwsf.setup.MachinesCategory;
import com.wwsf.setup.ResearchRegistry;
import com.wwsf.setup.ResourcesCategory;
import com.wwsf.setup.WWSFCategory;
import com.wwsf.setup.WWSFItems;
import com.wwsf.vehicle.VehicleArtilleryRegistry;
import com.wwsf.vehicle.VehicleSyncTask;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;

public class WWSFPlugin extends JavaPlugin implements SlimefunAddon {

    private static WWSFPlugin instance;

    private AimingManager aimingManager;
    private NamespacedKey ownerKey;
    private GasImmunityCommand gasImmunityCommand;
    private OilDrillsPlugin oilDrills;
    private HowitzerArtilleryBridge howitzerBridge;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("WWSF onEnable starting...");
        saveDefaultConfig();
        ownerKey = new NamespacedKey(this, "wwsf-owner");
        howitzerBridge = new HowitzerArtilleryBridge(this);

        if (getServer().getPluginManager().getPlugin("Slimefun") == null) {
            getLogger().severe("Slimefun not found! WWSF requires Slimefun — disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register every Slimefun item during plugin startup. Delaying this by
        // 20 ticks allowed Slimefun to finish loading block storage before it
        // knew WWSF2's IDs, which made placed oil machines look vanilla after
        // a restart.
        getLogger().info("Slimefun found. Initializing WWSF during startup...");
        initialize();
    }

    private void initialize() {
        try {
            getLogger().info("WWSF initialize starting...");

            CustomModelDataRegistry.registerAll(this);
            FortificationsCategory.register(this);
            ElectricityCategory.register(this);
            WWSFCategory.register(this);
            ResourcesCategory.register(this);
            MachinesCategory.register(this);
            GunsCategory.register(this);
            getLogger().info("All categories registered.");

            AmmoRegistry.register(this);
            new com.wwsf.ammo.MachineGunAmmoItem(this).register(this);
            FortificationsRegistry.register(this);
            WWSFItems.registerAll(this);
            getLogger().info("All item registries completed.");

            com.wwsf.multiblock.MarkerManager.loadFromDisk();
            com.wwsf.multiblock.MarkerManager.startLabelProximityUpdater();
            getServer().getScheduler().runTaskTimer(
                this,
                com.wwsf.multiblock.MarkerManager::saveToDisk,
                6000L,
                6000L
            );
            getLogger().info("Multiblock markers loaded and autosave scheduled.");

            aimingManager = new AimingManager(this);
            getServer().getPluginManager().registerEvents(new ArtilleryCombatListener(this), this);
            getServer().getPluginManager().registerEvents(new SpentCasingListener(this), this);
            getLogger().info("Artillery combat initialized.");

            ResearchRegistry.registerAll(this);
            getLogger().info("WWSF research (Vault-gated unlocks) registered.");

            oilDrills = new OilDrillsPlugin(this);
            oilDrills.enable();

            // The Gas Bomb and HowitzerArtillery's survival items are published
            // after the oil items so the Gas Bottle they both consume exists.
            GasGrenadeRegistry.register(this);
            HowitzerRecipeRegistry.register(this);

            GasImmunityConfig.load();
            registerCommandsAndListeners();
            enableMovecraftBridge();

            getServer().getScheduler().runTaskTimer(
                this,
                com.wwsf.artillery.MachineGunHeatManager::tick,
                0L,
                2L
            );
            getLogger().info("WWSF enabled.");
        } catch (Throwable error) {
            getLogger().log(Level.SEVERE, "Failed to enable WWSF", error);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void registerCommandsAndListeners() {
        PluginCommand wwsfCommand = getCommand("wwsf");
        if (wwsfCommand == null) {
            throw new IllegalStateException("Command 'wwsf' is missing from plugin.yml");
        }

        WwsfCommand commandExecutor = new WwsfCommand(this);
        wwsfCommand.setExecutor(commandExecutor);
        gasImmunityCommand = commandExecutor.getGasImmunityCommand();

        getServer().getPluginManager().registerEvents(gasImmunityCommand, this);
        getServer().getPluginManager().registerEvents(new MultiblockPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new TriggerInteractListener(), this);
        getServer().getPluginManager().registerEvents(new TriggerItemListener(), this);
        getServer().getPluginManager().registerEvents(new MultiblockInteractListener(), this);
        getServer().getPluginManager().registerEvents(new GasGrenadeListener(this), this);
        getServer().getPluginManager().registerEvents(new GasDrumCraftCleanupListener(this), this);
        getServer().getPluginManager().registerEvents(new SteelBlastResistanceListener(), this);
    }

    private void enableMovecraftBridge() {
        if (getServer().getPluginManager().getPlugin("Movecraft") == null) {
            getLogger().info("Movecraft not present — static artillery only.");
            return;
        }

        getLogger().info("Movecraft detected — vehicle artillery support enabled.");
        VehicleSyncTask.start(this);
    }

    @Override
    public void onDisable() {
        SteelRecipeOverride.uninstall();
        if (oilDrills != null) {
            oilDrills.disable();
        }
        if (aimingManager != null) {
            aimingManager.clearAll();
        }
        com.wwsf.multiblock.MarkerManager.saveToDisk();
        if (gasImmunityCommand != null) {
            gasImmunityCommand.cleanup();
        }
        VehicleArtilleryRegistry.clear();
        getLogger().info("WWSF disabled.");
    }

    public static WWSFPlugin getInstance() {
        return instance;
    }

    public AimingManager getAimingManager() {
        return aimingManager;
    }

    @Nonnull
    public NamespacedKey getOwnerKey() {
        return ownerKey;
    }

    @Nonnull
    public HowitzerArtilleryBridge getHowitzerBridge() {
        return howitzerBridge;
    }

    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    @Override
    public String getBugTrackerURL() {
        return "";
    }
}
