package me.rytento.simpleShop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    private static Economy economy;
    private ShopManager shopManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!setupEconomy()) {
            getLogger().severe("Vault ou um plugin de economia não foi encontrado. Desativando SimpleShop.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        shopManager = new ShopManager(this);
        getCommand("shop").setExecutor(new ShopCommand(this, shopManager));
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);
        getLogger().info("Kart Shop ativado.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> registration = getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null) return false;
        economy = registration.getProvider();
        return economy != null;
    }

    public static Economy getEconomy() { return economy; }
    public ShopManager getShopManager() { return shopManager; }
}
