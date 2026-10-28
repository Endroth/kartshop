package me.rytento.simpleShop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ShopManager {
    public static final int PREV_PAGE_SLOT = 18;
    public static final int NEXT_PAGE_SLOT = 26;
    private final Main plugin;
    private final NamespacedKey opensShopKey;
    private final NamespacedKey itemIdKey;

    public ShopManager(Main plugin) {
        this.plugin = plugin;
        this.opensShopKey = new NamespacedKey(plugin, "opens_shop");
        this.itemIdKey = new NamespacedKey(plugin, "shop_item_id");
    }

    public NamespacedKey getOpensShopKey() { return opensShopKey; }
    public NamespacedKey getItemIdKey() { return itemIdKey; }

    public void openMainMenu(Player player) {
        FileConfiguration config = plugin.getConfig();
        int size = normalizeSize(config.getInt("main_menu.size", 27));
        String title = color(config.getString("main_menu.title", "&0Kart Shop"));
        Inventory inventory = Bukkit.createInventory(new ShopHolder(null, 1, true), size, title);
        ConfigurationSection items = config.getConfigurationSection("main_menu.items");
        if (items != null) {
            for (String id : items.getKeys(false)) {
                String path = "main_menu.items." + id;
                int slot = config.getInt(path + ".slot", -1);
                if (slot < 0 || slot >= size) continue;
                ItemStack item = createDisplayItem(path, config);
                ItemMeta meta = item.getItemMeta();
                if (meta != null && config.contains(path + ".opens_shop")) {
                    meta.getPersistentDataContainer().set(opensShopKey, PersistentDataType.STRING, config.getString(path + ".opens_shop"));
                    item.setItemMeta(meta);
                }
                inventory.setItem(slot, item);
            }
        }
        player.openInventory(inventory);
    }

    public void openShop(Player player, String shopName) { openShop(player, shopName, 1); }

    public void openShop(Player player, String shopName, int page) {
        FileConfiguration config = plugin.getConfig();
        String root = "shops." + shopName;
        ConfigurationSection shop = config.getConfigurationSection(root);
        if (shop == null) {
            player.sendMessage(color("&cLoja não encontrada: &f" + shopName));
            return;
        }
        int size = normalizeSize(config.getInt(root + ".size", 27));
        Inventory inventory = Bukkit.createInventory(new ShopHolder(shopName, page, false), size,
                color(config.getString(root + ".title", "&0Loja")) + (hasMultiplePages(shopName) ? color(" &7- Página " + page) : ""));
        ConfigurationSection items = config.getConfigurationSection(root + ".items");
        if (items != null) {
            for (String id : items.getKeys(false)) {
                String path = root + ".items." + id;
                int itemPage = config.getInt(path + ".page", 1);
                if (itemPage != page) continue;
                int slot = config.getInt(path + ".slot", -1);
                if (slot < 0 || slot >= size) continue;
                ItemStack item = createShopItem(path, config);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, id);
                    item.setItemMeta(meta);
                }
                inventory.setItem(slot, item);
            }
        }
        int maxPage = getMaxPage(shopName);
        if (maxPage > 1) {
            if (page > 1) inventory.setItem(PREV_PAGE_SLOT, named(Material.ARROW, "&ePágina anterior"));
            if (page < maxPage) inventory.setItem(NEXT_PAGE_SLOT, named(Material.ARROW, "&ePróxima página"));
        }
        player.openInventory(inventory);
    }

    public boolean hasMultiplePages(String shopName) { return getMaxPage(shopName) > 1; }

    private int getMaxPage(String shopName) {
        ConfigurationSection items = plugin.getConfig().getConfigurationSection("shops." + shopName + ".items");
        int max = 1;
        if (items != null) for (String id : items.getKeys(false)) max = Math.max(max, plugin.getConfig().getInt("shops." + shopName + ".items." + id + ".page", 1));
        return max;
    }

    public ItemStack createPurchasedItem(String shopName, String itemId) {
        String path = "shops." + shopName + ".items." + itemId;
        FileConfiguration config = plugin.getConfig();
        Material material = Material.matchMaterial(config.getString(path + ".material", "STONE"));
        if (material == null) material = Material.STONE;
        int amount = Math.max(1, Math.min(64, config.getInt(path + ".amount", 1)));
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (config.contains(path + ".name")) meta.setDisplayName(color(config.getString(path + ".name")));
            List<String> lore = config.getStringList(path + ".lore");
            if (!lore.isEmpty()) meta.setLore(color(lore));
            item.setItemMeta(meta);
        }
        applyEnchantments(item, config.getStringList(path + ".enchantments"));
        String stored = config.getString(path + ".stored_enchantment");
        if (stored != null && !stored.isBlank() && item.getItemMeta() instanceof EnchantmentStorageMeta bookMeta) {
            String[] parts = stored.split(":");
            if (parts.length == 2) {
                Enchantment enchantment = findEnchantment(parts[0]);
                try {
                    int level = Integer.parseInt(parts[1]);
                    if (enchantment != null && level > 0) {
                        bookMeta.addStoredEnchant(enchantment, level, true);
                        item.setItemMeta(bookMeta);
                    }
                } catch (NumberFormatException ignored) { }
            }
        }
        return item;
    }

    private ItemStack createShopItem(String path, FileConfiguration config) {
        ItemStack item = createDisplayItem(path, config);
        applyEnchantments(item, config.getStringList(path + ".enchantments"));
        String stored = config.getString(path + ".stored_enchantment");
        if (stored != null && !stored.isBlank() && item.getItemMeta() instanceof EnchantmentStorageMeta meta) {
            String[] parts = stored.split(":");
            if (parts.length == 2) {
                Enchantment enchantment = findEnchantment(parts[0]);
                try {
                    if (enchantment != null) {
                        meta.addStoredEnchant(enchantment, Integer.parseInt(parts[1]), true);
                        item.setItemMeta(meta);
                    }
                } catch (NumberFormatException ignored) { }
            }
        }
        return item;
    }

    private ItemStack createDisplayItem(String path, FileConfiguration config) {
        Material material = Material.matchMaterial(config.getString(path + ".material", "PAPER"));
        if (material == null) material = Material.PAPER;
        ItemStack item = new ItemStack(material, Math.max(1, Math.min(64, config.getInt(path + ".amount", 1))));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (config.contains(path + ".name")) meta.setDisplayName(color(config.getString(path + ".name")));
            List<String> lore = config.getStringList(path + ".lore");
            if (!lore.isEmpty()) meta.setLore(color(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void applyEnchantments(ItemStack item, List<String> definitions) {
        for (String definition : definitions) {
            String[] parts = definition.split(":");
            if (parts.length != 2) continue;
            Enchantment enchantment = findEnchantment(parts[0]);
            try {
                int level = Integer.parseInt(parts[1]);
                if (enchantment != null && level > 0) item.addUnsafeEnchantment(enchantment, level);
            } catch (NumberFormatException ignored) { }
        }
    }

    private Enchantment findEnchantment(String rawName) {
        String key = rawName.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        try { return Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key)); }
        catch (NoSuchMethodError | IllegalArgumentException ignored) { return null; }
    }

    private ItemStack named(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(color(name)); item.setItemMeta(meta); }
        return item;
    }

    private int normalizeSize(int size) {
        if (size < 9) return 9;
        if (size > 54) return 54;
        return ((size + 8) / 9) * 9;
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
    private List<String> color(List<String> lines) {
        List<String> colored = new ArrayList<>();
        for (String line : lines) colored.add(color(line));
        return colored;
    }
}
