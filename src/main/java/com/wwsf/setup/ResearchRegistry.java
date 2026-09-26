package com.wwsf.setup;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

import org.bukkit.NamespacedKey;

import com.wwsf.WWSFPlugin;
import com.wwsf.config.ItemConfigHelper;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Assigns a Slimefun {@link Research} to every WWSF item so that nothing can be
 * crafted/used until the player unlocks it.
 *
 * <p>Each research carries both an experience-level cost and a Vault currency
 * ("money") cost. When the server enables money-based unlocks in Slimefun's
 * config ({@code researches.use-money-unlock: true}, requires the Vault
 * dependency + an economy plugin), players must pay the currency cost to
 * research — i.e. WWSF items become Vault-gated.</p>
 *
 * <p>Costs are config-driven (project convention): global defaults live in
 * {@code config.yml} under {@code research.*}, and any item can override them
 * via {@code items.<id>.research-level-cost} / {@code items.<id>.research-currency-cost}.
 * Slimefun additionally persists the resolved values into its own
 * {@code researches.yml} for live server-side tuning.</p>
 */
public final class ResearchRegistry {

    private ResearchRegistry() {
    }

    /**
     * Base numeric research ID. Kept high to avoid clashing with core Slimefun
     * research IDs. Each WWSF research gets a unique sequential ID from here.
     */
    private static final int ID_BASE = 41_500;

    public static void registerAll(WWSFPlugin plugin) {
        if (!readUnitedConfigFlag("isResearchingEnabled", true)) {
            plugin.getLogger().info("Slimefun researching is disabled globally — WWSF items will NOT require research.");
            return;
        }

        int defaultLevelCost = plugin.getConfig().getInt("research.default-level-cost", 10);
        double defaultCurrencyCost = plugin.getConfig().getDouble("research.default-currency-cost", 500.0);

        int id = ID_BASE;
        int count = 0;

        for (SlimefunItem item : Slimefun.getRegistry().getAllSlimefunItems()) {
            if (item == null || !plugin.equals(item.getAddon())) {
                continue;
            }

            // Don't clobber a research an item may have declared for itself.
            if (item.hasResearch()) {
                continue;
            }

            String itemId = item.getId();
            int levelCost = ItemConfigHelper.getInt(itemId, "research-level-cost", defaultLevelCost);
            double currencyCost = ItemConfigHelper.getDouble(itemId, "research-currency-cost", defaultCurrencyCost);

            NamespacedKey key = new NamespacedKey(plugin, "research_" + itemId.toLowerCase(Locale.ROOT));

            Research research = new Research(key, id++, item.getItemName(), levelCost);
            setCurrencyCostIfSupported(plugin, research, currencyCost);
            research.addItems(item);
            research.register();
            count++;
        }

        plugin.getLogger().info("Registered Vault-gated research for " + count + " WWSF items.");
        if (!readUnitedConfigFlag("isUseMoneyUnlock", false)) {
            plugin.getLogger().info("Note: Slimefun 'researches.use-money-unlock' is false — WWSF research currently"
                + " costs experience levels. Set it to true (with Vault + an economy plugin) for Vault money costs.");
        }
    }

    /**
     * Slimefun United adds a config manager and currency research costs that are not present in
     * the published Slimefun API. Reflection preserves those features when United is installed.
     */
    private static boolean readUnitedConfigFlag(String methodName, boolean fallback) {
        try {
            Method getConfigManager = Slimefun.class.getMethod("getConfigManager");
            Object configManager = getConfigManager.invoke(null);
            Method flag = configManager.getClass().getMethod(methodName);
            return (boolean) flag.invoke(configManager);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassCastException ex) {
            return fallback;
        }
    }

    private static void setCurrencyCostIfSupported(WWSFPlugin plugin, Research research, double cost) {
        try {
            Research.class.getMethod("setCurrencyCost", double.class).invoke(research, cost);
        } catch (NoSuchMethodException ex) {
            plugin.getLogger().fine("Standard Slimefun detected; currency research costs are unavailable.");
        } catch (IllegalAccessException | InvocationTargetException ex) {
            plugin.getLogger().warning("Could not set research currency cost: " + ex.getMessage());
        }
    }
}
