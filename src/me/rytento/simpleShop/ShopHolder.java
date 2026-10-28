package me.rytento.simpleShop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class ShopHolder implements InventoryHolder {
    private final String shopName;
    private final int page;
    private final boolean mainMenu;

    public ShopHolder(String shopName, int page, boolean mainMenu) {
        this.shopName = shopName;
        this.page = page;
        this.mainMenu = mainMenu;
    }

    public String getShopName() { return shopName; }
    public int getPage() { return page; }
    public boolean isMainMenu() { return mainMenu; }
    @Override public Inventory getInventory() { return null; }
}
