package com.quickshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class QuickShopPlugin extends JavaPlugin implements Listener {
    private Economy economy;
    private ShopRegistry registry;
    private DataStore data;
    private QuickBuyGui gui;
    private Menus menus;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "prices.yml").exists()) saveResource("prices.yml", false);

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().severe("No Vault economy provider found! Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        economy = rsp.getProvider();

        registry = new ShopRegistry(this);
        loadPrices();
        data = new DataStore(this);
        gui = new QuickBuyGui(this);
        menus = new Menus(this);

        Bukkit.getPluginManager().registerEvents(gui, this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(), this);
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, () -> data.save(), 20L * 30, 20L * 30);
    }

    @Override
    public void onDisable() {
        if (data != null) data.save();
    }

    private void loadPrices() {
        registry.load(YamlConfiguration.loadConfiguration(new File(getDataFolder(), "prices.yml")));
    }

    public Economy economy() { return economy; }
    public ShopRegistry registry() { return registry; }
    public DataStore data() { return data; }
    public QuickBuyGui gui() { return gui; }
    public Menus menus() { return menus; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("shop") && args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("quickshop.reload")) { sender.sendMessage("No permission."); return true; }
            reloadConfig();
            loadPrices();
            sender.sendMessage("QuickShop prices reloaded.");
            return true;
        }
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
        if (cmd.getName().equalsIgnoreCase("shop")) {
            gui.open(p);
        } else if (cmd.getName().equalsIgnoreCase("shards")) {
            p.sendMessage(Component.text("Shards: ", NamedTextColor.GRAY)
                    .append(Component.text(Messages.fmt(data.shards(p)), Messages.BLUE)));
        }
        return true;
    }

    @EventHandler
    public void onKill(EntityDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (dead instanceof ArmorStand) return;
        Player killer = dead.getKiller();
        if (killer == null || killer.equals(dead)) return;
        data.addShards(killer, getConfig().getInt("shards-per-kill", 10));
    }
}
