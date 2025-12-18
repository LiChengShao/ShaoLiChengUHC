package com.shaolian.uhc.items;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class Tim implements Listener {
    private final JavaPlugin plugin;

    public Tim(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public ItemStack createTimSummoner() {
        ItemStack timSummoner = new ItemStack(Material.EMERALD);
        ItemMeta meta = timSummoner.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Tim召唤物");
            timSummoner.setItemMeta(meta);
        }
        return timSummoner;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Tim召唤物")) {
            event.setCancelled(true);
            Location spawnLocation = player.getLocation().add(player.getLocation().getDirection().multiply(2));
            Villager tim = (Villager) player.getWorld().spawnEntity(spawnLocation, EntityType.VILLAGER);
            tim.setAI(false); // 禁止移动
            tim.setCustomName(ChatColor.GREEN + "Tim");
            tim.setCustomNameVisible(true);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Villager) {
            Villager villager = (Villager) event.getRightClicked();
            if (villager.getCustomName() != null && villager.getCustomName().equals(ChatColor.GREEN + "Tim")) {
                Player player = event.getPlayer();
                ItemStack itemInHand = player.getInventory().getItemInMainHand();

                if (itemInHand != null && (itemInHand.getType() == Material.BOW || itemInHand.getType().toString().endsWith("_SWORD") || itemInHand.getType().toString().endsWith("_CHESTPLATE"))) {
                    ItemStack upgradedItem = upgradeItem(itemInHand);
                    player.getInventory().setItemInMainHand(null); // 移除玩家手中的物品

                    new BukkitRunnable() {
                        int rotations = 0;

                        @Override
                        public void run() {
                            if (rotations < 3) {
                                villager.setRotation(villager.getLocation().getYaw() + 120, villager.getLocation().getPitch());
                                rotations++;
                            } else {
                                player.getWorld().dropItemNaturally(villager.getLocation(), upgradedItem);
                                this.cancel();
                            }
                        }
                    }.runTaskTimer(plugin, 0L, 20L); // 每秒旋转一次
                }
            }
        }
    }

    private ItemStack upgradeItem(ItemStack item) {
        ItemStack upgradedItem = new ItemStack(item.getType());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            upgradedItem.setItemMeta(meta);
        }

        // 升级物品类型
        switch (item.getType()) {
            case BOW:
                upgradedItem.addEnchantment(Enchantment.POWER, item.getEnchantmentLevel(Enchantment.POWER) + 1);
                break;
            case WOODEN_SWORD:
                upgradedItem.setType(Material.STONE_SWORD);
                break;
            case STONE_SWORD:
                upgradedItem.setType(Material.IRON_SWORD);
                break;
            case IRON_SWORD:
                upgradedItem.setType(Material.DIAMOND_SWORD);
                break;
            case DIAMOND_SWORD:
                upgradedItem.setType(Material.NETHERITE_SWORD);
                break;
            case LEATHER_CHESTPLATE:
                upgradedItem.setType(Material.IRON_CHESTPLATE);
                break;
            case IRON_CHESTPLATE:
                upgradedItem.setType(Material.DIAMOND_CHESTPLATE);
                break;
            case DIAMOND_CHESTPLATE:
                upgradedItem.setType(Material.NETHERITE_CHESTPLATE);
                break;
            default:
                break;
        }

        return upgradedItem;
    }
}
