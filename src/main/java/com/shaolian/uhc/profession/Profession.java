package com.shaolian.uhc.profession;

import com.shaolian.uhc.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.Listener;
import java.util.HashMap;


public class Profession implements Listener  {

    public static ItemStack createItems() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (GameManager.isGameFinalRunning) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;
        if(item.equals(createItems())){
            openKitGUI(player);
        }
    }

    public void openKitGUI(Player player) {
        // 创建一个GUI，包含所有职业的物品
        Inventory gui = Bukkit.createInventory(null, 36, "职业选择");
        // 添加职业物品

    }


    public void addItemsSheShou(Player player) {
        // 创建要添加的物品
        ItemStack[] items = {
                new ItemStack(Material.STRING, 5),
                new ItemStack(Material.FEATHER, 7)
        };

        // 添加物品并处理剩余部分
        for (ItemStack item : items) {
            // 尝试添加物品到玩家背包，并处理无法放入的部分
            player.getInventory().addItem(item).values().forEach(leftover ->
                    player.getWorld().dropItem(player.getLocation(), leftover)
            );
        }
    }

    public void addItemsQingJia(Player player) {
        ItemStack[] items = {
                new ItemStack(Material.LEATHER_HELMET, 1),
                new ItemStack(Material.LEATHER_CHESTPLATE, 1),
                new ItemStack(Material.LEATHER_LEGGINGS, 1),
                new ItemStack(Material.LEATHER_BOOTS, 1)
        };

        for (ItemStack item : items) {
            player.getInventory().addItem(item).values().forEach(leftover ->
                    player.getWorld().dropItem(player.getLocation(), leftover)
            );
        }
    }

    public void addItemsShengTaiXueJia(Player player) {
        ItemStack[] items = {
                new ItemStack(Material.VINE, 18),
                new ItemStack(Material.LILY_PAD, 32),
                new ItemStack(Material.SUGAR_CANE, 8)
        };

        for (ItemStack item : items) {
            player.getInventory().addItem(item).values().forEach(leftover ->
                    player.getWorld().dropItem(player.getLocation(), leftover)
            );
        }
    }

    public void addItemsFuMoShi(Player player) {
        ItemStack[] items = {
                new ItemStack(Material.BOOK, 3),
                new ItemStack(Material.EXPERIENCE_BOTTLE, 13),
                new ItemStack(Material.LAPIS_LAZULI, 12)
        };

        for (ItemStack item : items) {
            player.getInventory().addItem(item).values().forEach(leftover ->
                    player.getWorld().dropItem(player.getLocation(), leftover)
            );
        }
    }

    public void addItemsNongMing(Player player) {
        ItemStack[] items = {
                new ItemStack(Material.IRON_HOE, 1),
                new ItemStack(Material.MELON_SLICE, 2),
                new ItemStack(Material.CARROT, 2),
                new ItemStack(Material.BONE_MEAL, 3)
        };

        for (ItemStack item : items) {
            player.getInventory().addItem(item).values().forEach(leftover ->
                    player.getWorld().dropItem(player.getLocation(), leftover)
            );
        }
    }

    public void addItemsXunMaShi(Player player) {
        ItemStack[] items = {
                new ItemStack(Material.LEATHER, 9),
                new ItemStack(Material.WHEAT, 7),
                new ItemStack(Material.STRING, 3)
        };

        for (ItemStack item : items) {
            player.getInventory().addItem(item).values().forEach(leftover ->
                    player.getWorld().dropItem(player.getLocation(), leftover)
            );
        }
    }

    public void addItemsTuFu(Player player) {
        ItemStack[] items = {
                new ItemStack(Material.BEEF, 7),
                new ItemStack(Material.CARROT, 8),
                new ItemStack(Material.APPLE, 3)
        };

        for (ItemStack item : items) {
            player.getInventory().addItem(item).values().forEach(leftover ->
                    player.getWorld().dropItem(player.getLocation(), leftover)
            );
        }
    }

    public void addItemsWuShi(Player player) {
        ItemStack[] items = {
                    new ItemStack(Material.BONE, 3),
                new ItemStack(Material.SLIME_BALL, 3),
                new ItemStack(Material.GUNPOWDER, 1),
                new ItemStack(Material.SPIDER_EYE, 1),
        };

        for (ItemStack item : items) {
            player.getInventory().addItem(item).values().forEach(leftover ->
                    player.getWorld().dropItem(player.getLocation(), leftover)
            );
        }
    }

    public void addItemsTanXianJia(Player player) {
        ItemStack[] items = {
                new ItemStack(Material.STONE_PICKAXE, 1),
                new ItemStack(Material.STONE_SWORD, 1),
                new ItemStack(Material.STONE_AXE, 1),
                new ItemStack(Material.STONE_SHOVEL, 1),
        };

        for (ItemStack item : items) {
            player.getInventory().addItem(item).values().forEach(leftover ->
                    player.getWorld().dropItem(player.getLocation(), leftover)
            );
        }
    }

    public void addItemsXianJinShi(Player player) {
        ItemStack[] items = {
                new ItemStack(Material.PISTON, 6),
                new ItemStack(Material.PISTON_HEAD, 6),
                new ItemStack(Material.REDSTONE, 20),
                new ItemStack(Material.POINTED_DRIPSTONE, 6),
                new ItemStack(Material.HONEY_BLOCK, 4),
                new ItemStack(Material.SLIME_BLOCK, 4),
                new ItemStack(Material.OBSERVER, 4),
                new ItemStack(Material.SCULK_SENSOR, 4),
                new ItemStack(Material.SCULK_SENSOR, 4),
                new ItemStack(createEnchantedStoneShovel())
        };

        for (ItemStack item : items) {
            player.getInventory().addItem(item).values().forEach(leftover ->
                    player.getWorld().dropItem(player.getLocation(), leftover)
            );
        }
    }

    public ItemStack createEnchantedStoneShovel() {
       // 创建一个石头铲子，数量为1
       ItemStack enchantedStoneShovel = new ItemStack(Material.STONE_SHOVEL, 1);
       ItemMeta meta = enchantedStoneShovel.getItemMeta();
       if (meta != null) {
           meta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
           enchantedStoneShovel.setItemMeta(meta);
       }
         return enchantedStoneShovel;
   }



}