package me.rytento.simpleShop;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;

public final class ShopListener implements Listener {
    private final Main plugin;
    private final ShopManager shopManager;

    public ShopListener(Main plugin) {
        this.plugin = plugin;
        this.shopManager = plugin.getShopManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof ShopHolder holder)) return;
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory() != top) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        if (holder.isMainMenu()) {
            String shop = meta.getPersistentDataContainer().get(shopManager.getOpensShopKey(), PersistentDataType.STRING);
            if (shop != null) shopManager.openShop(player, shop);
            return;
        }

        if (clicked.getType().name().equals("ARROW")) {
            if (event.getSlot() == ShopManager.PREV_PAGE_SLOT && holder.getPage() > 1) {
                shopManager.openShop(player, holder.getShopName(), holder.getPage() - 1);
                return;
            }
            if (event.getSlot() == ShopManager.NEXT_PAGE_SLOT) {
                shopManager.openShop(player, holder.getShopName(), holder.getPage() + 1);
                return;
            }
        }

        String itemId = meta.getPersistentDataContainer().get(shopManager.getItemIdKey(), PersistentDataType.STRING);
        if (itemId == null) return;
        buyItem(player, holder.getShopName(), itemId);
    }

    private void buyItem(Player player, String shopName, String itemId) {
        FileConfiguration config = plugin.getConfig();
        String path = "shops." + shopName + ".items." + itemId;
        if (!config.contains(path + ".material") || !config.contains(path + ".cost")) return;
        double price = config.getDouble(path + ".cost", -1);
        if (price < 0) {
            player.sendMessage(ShopManager.color("&cPreço inválido para esse item."));
            return;
        }
        if (Main.getEconomy() == null) {
            player.sendMessage(ShopManager.color("&cA economia não está disponível."));
            return;
        }
        if (Main.getEconomy().getBalance(player) < price) {
            player.sendMessage(ShopManager.color("&cVocê precisa de $" + format(price) + " para comprar este item."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        ItemStack purchased = shopManager.createPurchasedItem(shopName, itemId);
        EconomyResponse response = Main.getEconomy().withdrawPlayer(player, price);
        if (!response.transactionSuccess()) {
            player.sendMessage(ShopManager.color("&cNão foi possível concluir o pagamento."));
            return;
        }
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(purchased);
        for (ItemStack leftover : leftovers.values()) player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        player.sendMessage(ShopManager.color("&aCompra realizada! &fVocê comprou " + displayName(path) + " &apor $" + format(price) + "."));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    private String displayName(String path) {
        String name = plugin.getConfig().getString(path + ".name");
        if (name == null || name.isBlank()) name = plugin.getConfig().getString(path + ".material", "item");
        return ChatColor.stripColor(ShopManager.color(name));
    }

    private String format(double value) {
        if (value == Math.rint(value)) return String.format(java.util.Locale.US, "%.0f", value);
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
