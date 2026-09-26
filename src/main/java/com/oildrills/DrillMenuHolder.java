package com.oildrills;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
public class DrillMenuHolder implements InventoryHolder {

    private final Location drillLocation;
    private Inventory inventory;

    public DrillMenuHolder(Location drillLocation) {
        this.drillLocation = drillLocation;
    }

    public Location getDrillLocation() {
        return drillLocation;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
