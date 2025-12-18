package com.shaolian.uhc;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.placeholder.PlaceholderManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class TabSidebar extends PlaceholderExpansion {

    private final JavaPlugin plugin;
    private final String serverIP;

    public TabSidebar(JavaPlugin plugin, String serverIP) {
        this.plugin = plugin;
        this.serverIP = serverIP;
        startSidebarUpdateTask();
    }

    public void startSidebarUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateSidebar(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // 每秒更新一次
    }

    private void updateSidebar(Player player) {
//        // 使用PlaceholderAPI解析占位符
//        String ip = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, "%shaolian_ip%");
//        String coords = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, "%shaolian_coords%");
//        String world = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, "%shaolian_world%");

        // 使用注册的占位符更新侧边栏
        String ip = "%shaolian_ip%";
        String coords = "%shaolian_coords%";
        String world = "%shaolian_world%";

        // 这里需要根据TAB的API来实际更新侧边栏内容
        // 你可能需要使用TAB的API来设置这些占位符
    }



    public String formatCoordinates(Player player) {
        return String.format("%d, %d, %d",
                (int) player.getLocation().getX(),
                (int) player.getLocation().getY(),
                (int) player.getLocation().getZ());
    }

    @Override
    public boolean register() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            return super.register();
        }
        return false;
    }



    @Override
    public String getIdentifier() {
        return "shaolian";
    }

    @Override
    public String getAuthor() {
        return "YourName";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";

        switch (identifier) {
            case "ip":
                return "§aIP: " + serverIP;
            case "coords":
                return String.format("§b坐标: %d, %d, %d",
                        (int) player.getLocation().getX(),
                        (int) player.getLocation().getY(),
                        (int) player.getLocation().getZ());
            case "world":
                return "§c世界: " + player.getWorld().getName();
            default:
                return null;
        }
    }
}
