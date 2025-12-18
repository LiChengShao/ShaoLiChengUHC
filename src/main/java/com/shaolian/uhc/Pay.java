package com.shaolian.uhc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;

import java.util.Arrays;


public class Pay implements Listener{
    private Main plugin;


    public Pay(Main plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public ItemStack createPayItem() {
        ItemStack emerald = new ItemStack(Material.EMERALD);
        ItemMeta meta = emerald.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6为爱发电");
            meta.setLore(Arrays.asList("§7你的鼓励是我更新的最大动力(⋟﹏⋞)"));
            meta.setUnbreakable(true);
            // 添加虚拟附魔效果
            meta.addEnchant(Enchantment.EFFICIENCY, 1, true);
            // 隐藏附魔信息
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            emerald.setItemMeta(meta);
        }
        return emerald;
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.EMERALD) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equals("§6为爱发电")) {
                event.setCancelled(true);
                Player player = event.getPlayer();
                openPayGUI(player);

            }
        }
    }


    private void openPayGUI(Player player) {
        Inventory gui = plugin.getServer().createInventory(null, 27, "§6投喂作者");

        //辣条腐肉
        ItemStack flesh = new ItemStack(Material.ROTTEN_FLESH);
        ItemMeta meta1 = flesh.getItemMeta();
        if (meta1 != null) {
            meta1.setDisplayName("§d投喂一袋辣条");
            meta1.setLore(Arrays.asList("§7作者不挑食~", ""));
            flesh.setItemMeta(meta1);
        }

        //小蛋糕
        ItemStack cake = new ItemStack(Material.CAKE);
        ItemMeta meta2 = cake.getItemMeta();
        if (meta2 != null) {
            meta2.setDisplayName("§d投喂一块小蛋糕");
            meta2.setLore(Arrays.asList("§7难过的时候不想听大道理", "§7只想吃小蛋糕"));
            cake.setItemMeta(meta2);
        }

        gui.setItem(11, flesh);
        gui.setItem(15, cake);

        player.openInventory(gui);
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§6投喂作者")) {
            event.setCancelled(true); // 防止玩家移动物品
            Player player = (Player) event.getWhoClicked();

            if (event.getSlot() == 11) {
                player.sendMessage(ChatColor.LIGHT_PURPLE +  "非常感谢你!");

                //在这里添加一个方法，实现玩家输入/minepay buy 首充礼包  的功能
                // 执行购买命令
                player.performCommand("minepay buy 首充礼包");
            }
            if (event.getSlot() == 15) {
                player.sendMessage(ChatColor.DARK_PURPLE + "超级无敌非常感谢你!!!");

                //在这里添加一个方法，实现玩家输入/minepay buy 测试金币  的功能
                // 执行购买命令
                player.performCommand("minepay buy 测试金币");
            }
        }
    }






}
