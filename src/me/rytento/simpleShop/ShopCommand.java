package me.rytento.simpleShop;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ShopCommand implements CommandExecutor {
    private static final int[] ITEM_SLOTS = {9, 10, 11, 12, 13, 14, 15, 16, 17};
    private final Main plugin;
    private final ShopManager shopManager;

    public ShopCommand(Main plugin, ShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("simpleshop.reload")) {
                sender.sendMessage(ShopManager.color("&cVocê não tem permissão."));
                return true;
            }
            plugin.reloadConfig();
            sender.sendMessage(ShopManager.color("&aConfiguração do Kart Shop recarregada."));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("add")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Este comando só pode ser usado por um jogador.");
                return true;
            }
            if (!player.hasPermission("simpleshop.admin")) {
                player.sendMessage(ShopManager.color("&cVocê não tem permissão."));
                return true;
            }
            addHeldItem(player, args);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando só pode ser usado por um jogador.");
            return true;
        }
        if (args.length == 0) shopManager.openMainMenu(player);
        else if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("ajuda")) sendHelp(player);
        else shopManager.openShop(player, args[0]);
        return true;
    }

    private void addHeldItem(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ShopManager.color("&cUso: /shop add <categoria> <preço>"));
            player.sendMessage(ShopManager.color("&7Segure o item que deseja adicionar na mão principal."));
            return;
        }

        String shopName = args[1].toLowerCase(Locale.ROOT);
        if (!shopName.matches("[a-z0-9_-]+")) {
            player.sendMessage(ShopManager.color("&cNome de categoria inválido. Use letras, números, _ ou -."));
            return;
        }
        String root = "shops." + shopName;
        if (!plugin.getConfig().isConfigurationSection(root)) {
            player.sendMessage(ShopManager.color("&cCategoria não encontrada: &f" + shopName));
            player.sendMessage(ShopManager.color("&7Crie a categoria em config.yml antes de adicionar itens."));
            return;
        }

        double price;
        try {
            price = Double.parseDouble(args[2].replace(',', '.'));
            if (!Double.isFinite(price) || price < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            player.sendMessage(ShopManager.color("&cPreço inválido. Exemplo: /shop add pvp 80"));
            return;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            player.sendMessage(ShopManager.color("&cSegure o item que deseja adicionar na mão principal."));
            return;
        }

        String itemsPath = root + ".items";
        int slot = -1;
        int page = 1;
        // Usa os slots 9 a 17, deixando os slots 18 e 26 para a navegação.
        for (int candidatePage = 1; candidatePage <= 100; candidatePage++) {
            for (int candidateSlot : ITEM_SLOTS) {
                boolean occupied = false;
                if (plugin.getConfig().isConfigurationSection(itemsPath)) {
                    for (String existingId : plugin.getConfig().getConfigurationSection(itemsPath).getKeys(false)) {
                        String existingPath = itemsPath + "." + existingId;
                        if (plugin.getConfig().getInt(existingPath + ".page", 1) == candidatePage
                                && plugin.getConfig().getInt(existingPath + ".slot", -1) == candidateSlot) {
                            occupied = true;
                            break;
                        }
                    }
                }
                if (!occupied) {
                    page = candidatePage;
                    slot = candidateSlot;
                    break;
                }
            }
            if (slot != -1) break;
        }
        if (slot == -1) {
            player.sendMessage(ShopManager.color("&cA loja está cheia (limite de 100 páginas)."));
            return;
        }

        String baseId = held.getType().name().toLowerCase(Locale.ROOT);
        String id = baseId;
        int suffix = 2;
        while (plugin.getConfig().contains(itemsPath + "." + id)) id = baseId + "_" + suffix++;

        String path = itemsPath + "." + id;
        plugin.getConfig().set(path + ".material", held.getType().name());
        plugin.getConfig().set(path + ".amount", Math.max(1, held.getAmount()));
        plugin.getConfig().set(path + ".slot", slot);
        plugin.getConfig().set(path + ".page", page);
        plugin.getConfig().set(path + ".cost", price);

        ItemMeta heldMeta = held.getItemMeta();
        String displayName = heldMeta != null && heldMeta.hasDisplayName()
                ? heldMeta.getDisplayName()
                : prettyName(held.getType());
        plugin.getConfig().set(path + ".name", displayName);
        List<String> lore = new ArrayList<>();
        lore.add(ShopManager.color("&7Preço: $" + format(price)));
        plugin.getConfig().set(path + ".lore", lore);

        if (heldMeta != null && !heldMeta.getEnchants().isEmpty()) {
            List<String> enchantments = new ArrayList<>();
            for (Map.Entry<Enchantment, Integer> entry : heldMeta.getEnchants().entrySet()) {
                enchantments.add(entry.getKey().getKey().getKey().toUpperCase(Locale.ROOT) + ":" + entry.getValue());
            }
            plugin.getConfig().set(path + ".enchantments", enchantments);
        }

        plugin.saveConfig();
        player.sendMessage(ShopManager.color("&aItem adicionado à loja &f" + shopName + "&a por $" + format(price) + "!"));
        player.sendMessage(ShopManager.color("&7ID: &f" + id + " &7| Página: &f" + page + " &7| Slot: &f" + slot));
        player.sendMessage(ShopManager.color("&eUse &f/shop " + shopName + " &epara ver a loja atualizada."));
    }

    private void sendHelp(Player player) {
        player.sendMessage(ShopManager.color("&6&lKart Shop — Comandos"));
        player.sendMessage(ShopManager.color("&e/shop &7- Abre o menu principal"));
        player.sendMessage(ShopManager.color("&e/shop <categoria> &7- Abre uma loja"));
        player.sendMessage(ShopManager.color("&e/shop add <categoria> <preço> &7- Adiciona o item da sua mão"));
        player.sendMessage(ShopManager.color("&e/shop reload &7- Recarrega o config.yml"));
    }

    private String prettyName(Material material) {
        String[] words = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private String format(double value) {
        if (value == Math.rint(value)) return String.format(Locale.US, "%.0f", value);
        return String.format(Locale.US, "%.2f", value);
    }
}
